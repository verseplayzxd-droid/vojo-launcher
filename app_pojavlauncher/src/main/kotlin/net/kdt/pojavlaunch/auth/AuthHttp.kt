package net.kdt.pojavlaunch.auth

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection
import net.kdt.pojavlaunch.BuildConfig

@Singleton
class AuthHttp @Inject constructor() : AuthTransport {
    @Volatile private var active: HttpsURLConnection? = null
    fun cancel() { active?.disconnect() }

    override fun exchange(service: AuthService, payload: String?, form: Boolean, bearer: String?): String {
        val address = when (service) {
            AuthService.MICROSOFT -> AuthProtocol.TOKEN
            AuthService.XBOX -> AuthProtocol.XBOX
            AuthService.XSTS -> AuthProtocol.XSTS
            AuthService.MINECRAFT -> AuthProtocol.MINECRAFT
            AuthService.ENTITLEMENTS -> AuthProtocol.ENTITLEMENTS
            AuthService.PROFILE -> AuthProtocol.PROFILE
        }
        val connection = URL(address).openConnection() as HttpsURLConnection
        try {
            active = connection
            connection.connectTimeout = 15_000; connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false; connection.useCaches = false
            connection.requestMethod = if (payload == null) "GET" else "POST"
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "VojoLauncher/${BuildConfig.VERSION_NAME} (https://github.com/verseplayzxd-droid/vojo-launcher)")
            if (bearer != null) {
                if (service != AuthService.ENTITLEMENTS && service != AuthService.PROFILE) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
                connection.setRequestProperty("Authorization", "Bearer $bearer")
            }
            if (payload != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", if (form) "application/x-www-form-urlencoded" else "application/json")
                val bytes = payload.toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val output = ByteArrayOutputStream()
            stream?.use {
                val buffer = ByteArray(4096)
                while (true) {
                    val count = it.read(buffer)
                    if (count == -1) break
                    if (output.size() + count > 131_072) throw AuthFailure(AuthProblem.INVALID_RESPONSE)
                    output.write(buffer, 0, count)
                }
            }
            val body = output.toString("UTF-8")
            if (status !in 200..299) throw AuthProtocol.failure(service, status, body)
            return body
        } catch (failure: AuthFailure) { throw failure }
        catch (_: IOException) { throw AuthFailure(AuthProblem.OFFLINE) }
        catch (_: Exception) { throw AuthFailure(AuthProblem.SERVICE_UNAVAILABLE) }
        finally { active = null; connection.disconnect() }
    }
}
