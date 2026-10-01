package com.nocturne.player.extensions

import android.content.Context
import android.os.PowerManager
import com.nocturne.player.constants.TabletUiKey
import com.nocturne.player.utils.dataStore
import com.nocturne.player.utils.get


fun Context.supportsWideScreen() : Boolean {
    val config = resources.configuration
    return config.screenWidthDp >= 600
}

fun Context.isTablet() : Boolean {
    val config = resources.configuration
    return config.smallestScreenWidthDp >= 600
}

/**
 * If screen is large enough to support tablet UI mode.
 * Current screen must be at least 600dp.
 */
fun Context.tabMode(): Boolean {
    val config = resources.configuration
    val isTablet = config.smallestScreenWidthDp >= 600
    return (dataStore.get(TabletUiKey, isTablet)) && config.screenWidthDp >= 600
}

fun Context.isPowerSaver(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isPowerSaveMode
}