package com.muse.companion.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Reports whether the device currently has validated internet. Used to gate online tools/UI. */
class ConnectivityMonitor(private val context: Context) {
    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
