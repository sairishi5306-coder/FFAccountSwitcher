package com.ffswitcher.app

import android.content.Context

/**
 * Tiny SharedPreferences wrapper - stores the two file paths and the
 * current account index so everything survives an app/service restart.
 */
object Prefs {
    private const val NAME = "ff_switcher_prefs"
    private const val KEY_ACCOUNTS_PATH = "accounts_path"
    private const val KEY_CACHE_PATH = "cache_path"
    private const val KEY_INDEX = "current_index"

    fun getAccountsPath(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_ACCOUNTS_PATH, "") ?: ""

    fun getCachePath(ctx: Context): String =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getString(KEY_CACHE_PATH, "") ?: ""

    fun savePaths(ctx: Context, accountsPath: String, cachePath: String) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_ACCOUNTS_PATH, accountsPath)
            .putString(KEY_CACHE_PATH, cachePath)
            .apply()
    }

    fun getIndex(ctx: Context): Int =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).getInt(KEY_INDEX, 0)

    fun saveIndex(ctx: Context, index: Int) {
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_INDEX, index)
            .apply()
    }
}
