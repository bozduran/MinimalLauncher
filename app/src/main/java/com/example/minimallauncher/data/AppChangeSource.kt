package com.example.minimallauncher.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Signals that the set of installed apps may have changed.
 *
 * Abstracted so the ViewModel never touches `Context` and tests can emit change
 * events deterministically instead of relying on real broadcasts.
 */
interface AppChangeSource {

    /** Emits once per relevant package change. Never completes. */
    val changes: Flow<Unit>
}

/**
 * [AppChangeSource] backed by package-change broadcasts.
 *
 * Registration lifetime is tied to collection: `callbackFlow` registers on
 * collect and `awaitClose` unregisters when the collector is cancelled. That
 * replaces the previous manual `onCleared` unregistration and removes the chance
 * of leaking a receiver or double-unregistering it.
 */
class PackageChangeSource(private val context: Context) : AppChangeSource {

    override val changes: Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        awaitClose { context.unregisterReceiver(receiver) }
    }
}
