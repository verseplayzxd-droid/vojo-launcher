@file:Suppress("DEPRECATION")
package net.kdt.pojavlaunch.auth

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import javax.security.auth.x500.X500Principal

class StoredAccount(val id: String, val name: String, val refreshToken: String,
    val minecraftToken: String, val expiresAtMs: Long, val verifiedAtMs: Long) {
    override fun toString() = "StoredAccount(<redacted>)"
}
class VaultContents(val clientId: String, val accounts: List<StoredAccount> = emptyList(),
    val selectedId: String? = null, val pending: PendingSignIn? = null) {
    override fun toString() = "VaultContents(<redacted>)"
    fun updated(accounts: List<StoredAccount> = this.accounts, selectedId: String? = this.selectedId,
        pending: PendingSignIn? = this.pending) = VaultContents(clientId, accounts, selectedId, pending)
}

@Singleton
class CredentialVault @Inject constructor(@ApplicationContext context: Context) {
    private val context = context.applicationContext
    // API 21 noBackupFilesDir plus manifest backup rules exclude both cloud and device transfer.
    private val directory = File(context.noBackupFilesDir, "vojo-auth")
    private val file = AtomicFile(File(directory, "accounts.v1"))
    private val wrapped = AtomicFile(File(directory, "wrapped-key.v1"))
    private val gson = Gson()
    private val aad = "Vojo credential vault v1".toByteArray(Charsets.UTF_8)
    private val aesAlias = "vojo.auth.aes.v1"
    private val rsaAlias = "vojo.auth.rsa.v1"
    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Synchronized fun read(): VaultContents? {
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) return null
        try {
            if (file.baseFile.length() > 1_048_576) throw AuthFailure(AuthProblem.STORAGE)
            val encrypted = file.openRead().use { it.readBytes() }
            if (encrypted.size > 1_048_576) throw AuthFailure(AuthProblem.STORAGE)
            val input = DataInputStream(ByteArrayInputStream(encrypted))
            if (input.readInt() != 0x564A4F31) throw AuthFailure(AuthProblem.STORAGE)
            val mode = input.readUnsignedByte()
            if (mode !in 1..2) throw AuthFailure(AuthProblem.STORAGE)
            val iv = ByteArray(12).also(input::readFully)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(mode, create = false), GCMParameterSpec(128, iv))
            cipher.updateAAD(aad + mode.toByte())
            val clear = cipher.doFinal(input.readBytes())
            val contents = try { gson.fromJson(String(clear, Charsets.UTF_8), VaultContents::class.java) }
                finally { clear.fill(0) }
            validate(contents)
            return contents
        } catch (_: Exception) { throw AuthFailure(AuthProblem.STORAGE) }
    }

    @Synchronized fun write(contents: VaultContents) {
        try {
            validate(contents)
            if (!directory.isDirectory && !directory.mkdirs()) throw AuthFailure(AuthProblem.STORAGE)
            val mode = if (Build.VERSION.SDK_INT >= 23) 2 else 1
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val key = encryptionKey(mode, create = true)
            if (mode == 2) cipher.init(Cipher.ENCRYPT_MODE, key)
            else cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, ByteArray(12).also(SecureRandom()::nextBytes)))
            cipher.updateAAD(aad + mode.toByte())
            val clear = gson.toJson(contents).toByteArray(Charsets.UTF_8)
            val encrypted = try { cipher.doFinal(clear) } finally { clear.fill(0) }
            val output = ByteArrayOutputStream()
            if (cipher.iv.size != 12) throw AuthFailure(AuthProblem.STORAGE)
            DataOutputStream(output).use { it.writeInt(0x564A4F31); it.writeByte(mode); it.write(cipher.iv); it.write(encrypted) }
            if (output.size() > 1_048_576) throw AuthFailure(AuthProblem.STORAGE)
            atomicWrite(file, output.toByteArray())
        } catch (_: Exception) { throw AuthFailure(AuthProblem.STORAGE) }
    }

    @Synchronized fun reset() {
        try {
            // Explicit user-confirmed reset only; never silently replace an unreadable vault.
            file.delete(); wrapped.delete()
            keyStore().apply { if (containsAlias(aesAlias)) deleteEntry(aesAlias); if (containsAlias(rsaAlias)) deleteEntry(rsaAlias) }
        } catch (_: Exception) { throw AuthFailure(AuthProblem.STORAGE) }
    }

    private fun validate(contents: VaultContents?) {
        if (contents == null || contents.clientId.isBlank() || contents.accounts.size > 20) throw AuthFailure(AuthProblem.STORAGE)
        if (contents.accounts.map { it.id }.distinct().size != contents.accounts.size) throw AuthFailure(AuthProblem.STORAGE)
        contents.accounts.forEach {
            if (!it.id.matches(Regex("[0-9a-f]{32}")) || !it.name.matches(Regex("[A-Za-z0-9_]{1,16}")) ||
                it.refreshToken.isBlank() || it.minecraftToken.isBlank() || it.expiresAtMs <= 0 || it.verifiedAtMs <= 0) throw AuthFailure(AuthProblem.STORAGE)
        }
        if (contents.selectedId != null && contents.accounts.none { it.id == contents.selectedId }) throw AuthFailure(AuthProblem.STORAGE)
        contents.pending?.let { if (it.verifier.length !in 43..128 || it.state.length < 32 || it.startedElapsedMs < 0) throw AuthFailure(AuthProblem.STORAGE) }
    }

    private fun atomicWrite(target: AtomicFile, bytes: ByteArray) {
        val stream = target.startWrite()
        try { stream.write(bytes); target.finishWrite(stream) }
        catch (e: Exception) { target.failWrite(stream); throw e }
    }

    private fun encryptionKey(mode: Int, create: Boolean): SecretKey {
        val store = keyStore()
        if (mode == 2) {
            if (Build.VERSION.SDK_INT < 23) throw AuthFailure(AuthProblem.STORAGE)
            if (!store.containsAlias(aesAlias)) {
                if (!create) throw AuthFailure(AuthProblem.STORAGE)
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                    init(KeyGenParameterSpec.Builder(aesAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256).setRandomizedEncryptionRequired(true).build())
                    generateKey()
                }
            }
            return store.getKey(aesAlias, null) as SecretKey
        }
        // API 21–22 cannot store AES keys in AndroidKeyStore. Wrap a random AES-256
        // data key with an AndroidKeyStore RSA key. Only the wrapped key reaches disk.
        if (!store.containsAlias(rsaAlias)) {
            if (!create || wrapped.baseFile.exists()) throw AuthFailure(AuthProblem.STORAGE)
            val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
            val end = Calendar.getInstance().apply { add(Calendar.YEAR, 30) }.time
            KeyPairGenerator.getInstance("RSA", "AndroidKeyStore").apply {
                initialize(KeyPairGeneratorSpec.Builder(context).setAlias(rsaAlias).setKeySize(2048)
                    .setSubject(X500Principal("CN=Vojo credential wrapping" )).setSerialNumber(BigInteger.ONE)
                    .setStartDate(start).setEndDate(end).build())
                generateKeyPair()
            }
        }
        if (!wrapped.baseFile.exists() && !File(wrapped.baseFile.path + ".bak").exists()) {
            if (!create) throw AuthFailure(AuthProblem.STORAGE)
            val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
            try {
                val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                cipher.init(Cipher.ENCRYPT_MODE, store.getCertificate(rsaAlias).publicKey)
                atomicWrite(wrapped, cipher.doFinal(bytes))
            } finally { bytes.fill(0) }
        }
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.DECRYPT_MODE, store.getKey(rsaAlias, null))
        val bytes = cipher.doFinal(wrapped.openRead().use { it.readBytes() })
        return try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
    }
}
