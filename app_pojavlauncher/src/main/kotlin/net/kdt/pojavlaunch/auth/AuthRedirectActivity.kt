package net.kdt.pojavlaunch.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import net.kdt.pojavlaunch.ui.vojo.VojoActivity

@AndroidEntryPoint
class AuthRedirectActivity : ComponentActivity() {
    @Inject lateinit var accounts: AccountRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = intent.dataString
        intent.data = null
        if (savedInstanceState == null && callback != null) accounts.acceptRedirect(callback)
        // Never forward the callback/code to another Activity or a saved Bundle.
        startActivity(Intent(this, VojoActivity::class.java).putExtra("vojo.openAccounts", true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
