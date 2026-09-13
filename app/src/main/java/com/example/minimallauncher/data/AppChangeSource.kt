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
 * The broadcasts that can invalidate the launcher's app list.
 *
 * Extracted as plain constants so the wiring is reviewable and unit-testable
 * without a device.
 */
object AppChangeActions {

    /** Package broadcasts that add, remove, update or change a launchable app. */
    val PACKAGE_ACTIONS: List<String> = listOf(
        Intent.ACTION_PACKAGE_ADDED,
        Intent.ACTION_PACKAGE_REMOVED,
        Intent.ACTION_PACKAGE_CHANGED,
        Intent.ACTION_PACKAGE_REPLACED,
    )

    /**
     * A locale change re-sorts the list, because `AppRepository` orders apps with
     * `Collator.getInstance(Locale.getDefault())`.
     */
    const val LOCALE_ACTION: String = Intent.ACTION_LOCALE_CHANGED
}

/**
 * Signals that the set of installed apps may have changed.
 *
 * Abstracted so the ViewModel never touches `Context` and tests can emit change
 * events deterministically instead of relying on real broadcasts.
 */
interface AppChangeSource {

    /** Emits once per relevant change. Never completes. */
    val changes: Flow<Unit>
}

/**
 * [AppChangeSource] backed by broadcasts.
 *
 * Package actions are delivered on a filter that **requires** the `package` data
 * scheme (that is how the framework scopes them to a specific package), while
 * `ACTION_LOCALE_CHANGED` carries no data at all — a filter with a scheme would
 * never match it. They therefore need two registrations, and consequently two
 * receiver instances so each can be unregistered exactly once.
 *
 * Registration lifetime is tied to collection: `callbackFlow` registers on collect
 * and `awaitClose` unregisters, which replaces the old manual `onCleared`
 * unregistration and removes the chance of leaking a receiver.
 */
class PackageChangeSource(private val context: Context) : AppChangeSource {

    override val changes: Flow<Unit> = callbackFlow {
        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val localeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }

        val packageFilter = IntentFilter().apply {
            AppChangeActions.PACKAGE_ACTIONS.forEach(::addAction)
            addDataScheme("package")
        }
        val localeFilter = IntentFilter(AppChangeActions.LOCALE_ACTION)

        ContextCompat.registerReceiver(
            context,
            packageReceiver,
            packageFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            context,
            localeReceiver,
            localeFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        awaitClose {
            context.unregisterReceiver(packageReceiver)
            context.unregisterReceiver(localeReceiver)
        }
    }
}
