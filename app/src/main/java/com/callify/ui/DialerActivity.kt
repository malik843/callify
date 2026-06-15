package com.callify.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.callify.MainActivity

/**
 * Transparent alias Activity required for [android.app.role.RoleManager.ROLE_DIALER] eligibility.
 *
 * Android requires that a default dialer app declares an Activity with an [Intent.ACTION_DIAL]
 * intent filter. This activity fulfils that requirement and immediately forwards to [MainActivity]
 * which hosts the actual DialPad tab.
 */
class DialerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
        finish()
    }
}
