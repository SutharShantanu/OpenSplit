package com.opensplit.data.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.opensplit.OpenSplitApp

/** Result of an [AppUpdater.install] session: shows Android's confirm prompt or reports failure. */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val updater = (context.applicationContext as? OpenSplitApp)?.container?.appUpdater ?: return
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirm?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            // On success the app process is replaced; nothing to do.
            PackageInstaller.STATUS_SUCCESS -> Unit
            PackageInstaller.STATUS_FAILURE_ABORTED -> updater.onInstallFailed("Update cancelled")
            else -> updater.onInstallFailed(
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)?.let { "Install failed: $it" }
                    ?: "Install failed (code $status)"
            )
        }
    }
}
