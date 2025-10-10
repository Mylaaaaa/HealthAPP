package com.example.myhealth.presentation.loginregister

import androidx.compose.runtime.mutableStateOf

/**
 * Super-simple in-memory auth. Replace with Firebase/Room later if needed.
 * loggedIn is observable for Compose so RootApp can react to logout/login.
 */
object FakeAuthStore {
    // email -> (name, password)
    private val users = mutableMapOf<String, Triple<String, String, Long>>() // name, password, createdAt
    private var currentEmail: String? = null

    // Observable auth flag (Compose will recompose when this changes)
    val loggedIn = mutableStateOf(false)

    val currentUserEmail: String?
        get() = currentEmail

    fun currentUserName(): String? = currentEmail?.let { users[it]?.first }

    fun register(name: String, email: String, password: String): Boolean {
        if (users.containsKey(email)) return false
        users[email] = Triple(name, password, System.currentTimeMillis())
        currentEmail = email
        loggedIn.value = true
        return true
    }

    fun login(email: String, password: String): Boolean {
        val entry = users[email] ?: return false
        val ok = entry.second == password
        if (ok) {
            currentEmail = email
            loggedIn.value = true
        }
        return ok
    }

    fun logout() {
        currentEmail = null
        loggedIn.value = false
    }
}
