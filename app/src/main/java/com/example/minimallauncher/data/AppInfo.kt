package com.example.minimallauncher.data

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
) {
    val key: String get() = "$packageName/$activityName"
}
