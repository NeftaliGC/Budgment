package com.nintech.budgment.data.repository

import com.nintech.budgment.data.local.dao.UserDao
import com.nintech.budgment.data.local.entity.UserEntity
import com.nintech.budgment.data.local.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject


enum class AuthError { USERNAME_TAKEN, INVALID_CREDENTIALS, EMPTY_FIELDS, PASSWORDS_DO_NOT_MATCH }

sealed class AuthResult {
    data class Success(val userId: String) : AuthResult()
    data class Error(val reason: AuthError) : AuthResult()
}

interface AuthRepository {
    suspend fun register(name: String, username: String, email: String?, password: String, confirmPassword: String): AuthResult
    suspend fun login(username: String, password: String): AuthResult
    /** Comprueba la contraseña del usuario [userId] sin cambiar la sesión. */
    suspend fun verifyPassword(userId: String, password: String): Boolean
}

class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : AuthRepository {

    override suspend fun register(
        name: String, username: String, email: String?, password: String, confirmPassword: String
    ): AuthResult {
        if (name.isBlank() || username.isBlank() || password.isBlank()) {
            return AuthResult.Error(AuthError.EMPTY_FIELDS)
        }
        if (password != confirmPassword) {
            return AuthResult.Error(AuthError.PASSWORDS_DO_NOT_MATCH)
        }
        if (userDao.getByUsername(username) != null) {
            return AuthResult.Error(AuthError.USERNAME_TAKEN)
        }
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            username = username,
            email = email?.ifBlank { null },
            passwordHash = PasswordHasher.hash(password),
            defaultCurrency = "MXN"
        )
        userDao.insert(user)
        return AuthResult.Success(user.id)
    }

    override suspend fun login(username: String, password: String): AuthResult {
        if (username.isBlank() || password.isBlank()) return AuthResult.Error(AuthError.EMPTY_FIELDS)
        val user = userDao.getByUsername(username) ?: return AuthResult.Error(AuthError.INVALID_CREDENTIALS)
        return if (PasswordHasher.verify(password, user.passwordHash)) {
            AuthResult.Success(user.id)
        } else {
            AuthResult.Error(AuthError.INVALID_CREDENTIALS)
        }
    }

    override suspend fun verifyPassword(userId: String, password: String): Boolean {
        if (password.isBlank()) return false
        val user = userDao.getById(userId) ?: return false
        return withContext(Dispatchers.Default) { PasswordHasher.verify(password, user.passwordHash) }
    }
}
