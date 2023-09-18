package com.example.relive

import android.content.Context
import android.content.SharedPreferences

class SessionManager(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences("MySession", Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun login() {
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.apply()
    }

    fun logout() {
        editor.putBoolean(KEY_IS_LOGGED_IN, false)
        editor.apply()
    }
}
