package com.example.myhealth.presentation.loginregister

/**
 * Extremely simple in-memory auth store. Replace with DataStore/Room/remote later.
 */
object FakeAuthStore {
    private val users: MutableMap<String, Pair<String, String>> = mutableMapOf()
    // email -> (hashedPwdOrPlain, name)
    var currentUserEmail: String? = null
        private set

    fun register(name: String, email: String, password: String): Boolean {
        if (email in users) return false
        users[email] = password to name
        currentUserEmail = email
        return true
    }

    fun login(email: String, password: String): Boolean {
        val saved = users[email] ?: return false
        val ok = saved.first == password
        if (ok) currentUserEmail = email
        return ok
    }

    fun logout() { currentUserEmail = null }
}
