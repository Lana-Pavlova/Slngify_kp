package com.example.slngify_kp.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.ActionCodeResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    var auth: FirebaseAuth = Firebase.auth
    var db: FirebaseFirestore = Firebase.firestore

    val registrationLoading = MutableStateFlow(false)
    val registrationError = MutableStateFlow<String?>(null)
    val loginLoading = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)
    val loginSuccess = MutableStateFlow(false)
    val updateDataLoading = MutableStateFlow(false)
    val updateDataError = MutableStateFlow<String?>(null)
    val updateDataSuccess = MutableStateFlow(false)
    val verificationEmailSent = MutableStateFlow(false) //  для отслеживания отправки письма с подтверждением

    private val _userEmail = MutableStateFlow(Firebase.auth.currentUser?.email ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _emailChangeConfirmationResult = MutableStateFlow<ResultState<String>>(ResultState.Idle)
    val emailChangeConfirmationResult: StateFlow<ResultState<String>> = _emailChangeConfirmationResult.asStateFlow()
    // Функция для обработки кода действия из ссылки
    fun handleEmailChangeConfirmation(oobCode: String) {
        if (_emailChangeConfirmationResult.value is ResultState.Loading) {
            Log.d("AuthViewModel", "Already processing email change confirmation.")
            return
        }

        _emailChangeConfirmationResult.value = ResultState.Loading // Устанавливаем состояние загрузки

        viewModelScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()

                val actionCodeResult = auth.checkActionCode(oobCode).await()
                Log.d("AuthViewModel", "Action code checked successfully. Operation: ${actionCodeResult.operation}")

                if (actionCodeResult.operation != ActionCodeResult.VERIFY_BEFORE_CHANGE_EMAIL ) {
                    val errorMsg = "Неверный тип операции для кода действия."
                    Log.e("AuthViewModel", errorMsg)
                    _emailChangeConfirmationResult.value = ResultState.Error(errorMsg)
                    return@launch // Прерываем выполнение
                }

                // Обновляем локальный объект пользователя, чтобы получить новый email и статус верификации.
                auth.currentUser?.reload()?.await()
                Log.d("AuthViewModel", "Local user object reloaded.")

                val user = auth.currentUser
                val newEmail = user?.email

                if (newEmail != null && user.isEmailVerified) { // Проверяем также статус верификации
                    Log.d("AuthViewModel", "New email from reloaded user: $newEmail. Verified: ${user.isEmailVerified}")

                    val userId = user.uid
                    val isFirestoreUpdated = updateFirestoreEmail(userId, newEmail, user.isEmailVerified) // Передаем новый email и статус верификации

                    if (isFirestoreUpdated) {
                        Log.d("AuthViewModel", "Firestore email updated successfully.")
                        _emailChangeConfirmationResult.value = ResultState.Success(newEmail)
                    } else {
                        val errorMsg = "Email изменен в Auth, но не удалось обновить Firestore."
                        Log.e("AuthViewModel", errorMsg)
                        _emailChangeConfirmationResult.value = ResultState.Error(newEmail)
                    }

                } else if (newEmail != null && !user.isEmailVerified){
                    val errorMsg = "Email изменен на $newEmail, но статус верификации не 'true'. Что-то пошло не так."
                    Log.e("AuthViewModel", errorMsg)
                    val userId = user.uid
                    updateFirestoreEmail(userId, newEmail, user.isEmailVerified)
                    _emailChangeConfirmationResult.value = ResultState.Error(errorMsg)
                }
                else {
                    val errorMsg = "Пользователь или его email равен null после reload()."
                    Log.e("AuthViewModel", errorMsg)
                    _emailChangeConfirmationResult.value = ResultState.Error(errorMsg)
                }


            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error checking action code or reloading user", e)
                val errorMessage = when (e.message) {
                    "Invalid action code." -> "Неверный или устаревший код действия."
                    "Action code has expired." -> "Срок действия кода действия истек."
                    else -> e.message ?: "Неизвестная ошибка."
                }
                _emailChangeConfirmationResult.value = ResultState.Error(errorMessage)
            }
        }
    }

    // Функция для обновления email и статуса верификации в Firestore
    private suspend fun updateFirestoreEmail(userId: String, email: String, isEmailVerified: Boolean): Boolean {

        try {
            val userRef = db.collection("users").document(userId)

            val updates = mapOf(
                "email" to email,
                "isEmailVerified" to isEmailVerified
            )

            userRef.update(updates).await()

            Log.d("AuthViewModel", "Firestore email and verification status updated successfully for user: $userId")
            return true // Сообщаем об успехе

        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error updating Firestore for user: $userId", e)

            return false // Сообщаем о неудаче
        }
    }

    fun resetEmailChangeConfirmationState() {
        _emailChangeConfirmationResult.value = ResultState.Idle
        Log.d("AuthViewModel", "Email change confirmation state reset.")
    }


    fun changeEmail(newEmail: String, password: String, onComplete: (Boolean) -> Unit) {
        updateDataLoading.value = true
        updateDataError.value = null
        updateDataSuccess.value = false

        viewModelScope.launch {
            val user = auth.currentUser ?: run {
                updateDataLoading.value = false
                updateDataError.value = "User not authenticated"
                onComplete(false)
                return@launch
            }

            try {
                // Обновляем email в Firebase Authentication с подтверждением
                val isUpdateSuccessful = updateFirebaseAuthProfile(newEmail, password)
                if (isUpdateSuccessful) {
                    updateDataLoading.value = false
                    updateDataSuccess.value = true
                    onComplete(true)
                } else {
                    updateDataLoading.value = false
                    onComplete(false)
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e("AuthViewModel", "Invalid credentials", e)
                updateDataLoading.value = false
                updateDataError.value = "Неверный пароль."
                onComplete(false)
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                Log.e("AuthViewModel", "Recent login required", e)
                updateDataLoading.value = false
                updateDataError.value = "Требуется повторный вход в аккаунт. Пожалуйста, выйдите и войдите снова."
                onComplete(false)
            } catch (e: FirebaseAuthUserCollisionException) {
                Log.e("AuthViewModel", "Email already in use", e)
                updateDataLoading.value = false
                updateDataError.value = "Этот email уже используется."
                onComplete(false)
            } catch (e: FirebaseAuthException) {
                Log.e("AuthViewModel", "Firebase Auth error", e)
                updateDataLoading.value = false
                updateDataError.value = "Ошибка Firebase: ${e.message}"
                onComplete(false)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error changing email", e)
                updateDataLoading.value = false
                updateDataError.value = "Ошибка при смене email: ${e.message}"
                onComplete(false)
            } finally {
                updateDataLoading.value = false
            }
        }
    }

    fun registerUser(name: String, email: String, password: String, onComplete: (Boolean, String) -> Unit) {
        registrationLoading.value = true
        registrationError.value = null
        viewModelScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user
                if (user != null) {
                    // Обновляем displayName пользователя
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user.updateProfile(profileUpdates).await()

                    user.sendEmailVerification().await()

                    // Создаем запись о пользователе в Firestore
                    createUserInFirestore(user.uid, name)

                    registrationLoading.value = false
                    onComplete(true, "Регистрация прошла успешно. Проверьте свой email для подтверждения.")
                } else {
                    registrationLoading.value = false
                    onComplete(false, "Не удалось создать пользователя.")
                }
            } catch (e: FirebaseAuthException) {
                registrationLoading.value = false
                registrationError.value = "Ошибка регистрации: ${e.message}"
                onComplete(false, "Ошибка регистрации: ${e.message}")
            } catch (e: Exception) {
                registrationLoading.value = false
                registrationError.value = "Неизвестная ошибка: ${e.message}"
                onComplete(false, "Неизвестная ошибка: ${e.message}")
            }
        }
    }
    private fun createUserInFirestore(uid: String, name: String) {
        val user = hashMapOf(
            "displayName" to name,
            "completedLessonIds" to 0,
            "completedSectionIds" to 0,
            "achievementIds" to 0
        )

        db.collection("users")
            .document(uid)
            .set(user)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "User added to Firestore")
            }
            .addOnFailureListener { e ->
                Log.w("AuthViewModel", "Error adding user to Firestore", e)
            }
    }
    // Функция логина
    fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        loginLoading.value = true
        loginError.value = null
        loginSuccess.value = false

        if (email.isBlank() || password.isBlank()) {
            loginLoading.value = false
            loginError.value = "Пожалуйста, заполните все поля."
            onComplete(false, "Пожалуйста, заполните все поля.")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loginLoading.value = false
                if (task.isSuccessful) {
                    loginSuccess.value = true
                    Log.d("AuthViewModel", "User login successful")
                    onComplete(true, null)
                } else {
                    loginError.value = task.exception?.message ?: "Login failed"
                    loginSuccess.value = false
                    val errorMessage = task.exception?.message ?: "Ошибка входа"
                    Log.e("AuthViewModel", "User login failed: $errorMessage")
                    onComplete(false, errorMessage)
                }
            }
    }
    // Функция для повторной отправки письма с подтверждением
    fun resendEmailVerification(onComplete: (Boolean, String?) -> Unit) {
        auth.currentUser?.let { user ->
            user.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onComplete(true, "Письмо с подтверждением отправлено повторно. Проверьте свою почту.")
                    } else {
                        onComplete(false, task.exception?.message ?: "Не удалось отправить письмо с подтверждением.")
                    }
                }
        } ?: run {
            onComplete(false, "Пользователь не аутентифицирован.")
        }
    }
    // Функция выхода
    fun signOutUser(navController: NavController) {
        auth.signOut()
        navController.navigate("profilePage") {
            popUpTo(0)
        }
    }

    // Функция обновления данных пользователя
    fun updateNameProfile(
        name: String,
        onComplete: (Boolean) -> Unit
    ) {
        updateDataLoading.value = true
        updateDataError.value = null
        updateDataSuccess.value = false

        viewModelScope.launch {
            val user = auth.currentUser ?: run {
                updateDataLoading.value = false
                updateDataError.value = "User not authenticated"
                onComplete(false)
                return@launch
            }

            try {
                if (name.isNotBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                    user.updateProfile(profileUpdates).await()

                    // Обновляем displayName в Firestore
                    updateDisplayNameInFirestore(user.uid, name)
                }
                updateDataLoading.value = false
                updateDataSuccess.value = true
                onComplete(true)

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating name profile", e)
                updateDataLoading.value = false
                updateDataError.value = "Ошибка при обновлении имени: ${e.message}"
                onComplete(false)
            }
        }
    }

    private fun updateDisplayNameInFirestore(uid: String, displayName: String) {
        db.collection("users")
            .document(uid)
            .update("displayName", displayName)
            .addOnSuccessListener {
                Log.d("AuthViewModel", "displayName updated in Firestore")
            }
            .addOnFailureListener { e ->
                Log.w("AuthViewModel", "Error updating displayName in Firestore", e)
            }
    }

    // Функция для обновления профиля Firebase Authentication
    private suspend fun updateFirebaseAuthProfile(email: String, password: String): Boolean {
        val user = auth.currentUser ?: return false

        return try {
            // ре-аутентификация
            val credential = EmailAuthProvider.getCredential(user.email!!, password)
            user.reauthenticate(credential).await()

            // подтверждение новой почты после верификации
            user.verifyBeforeUpdateEmail(email).await()

            verificationEmailSent.value = true
            updateDataError.value = "Пожалуйста, проверьте вашу новую почту для подтверждения."

            true
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Log.e("AuthViewModel", "Re-authentication required", e)
            updateDataError.value =
                "Требуется повторная аутентификация. Пожалуйста, введите свой пароль еще раз."
            false
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.e("AuthViewModel", "Invalid email format", e)
            updateDataError.value = "Неверный формат email."
            false
        } catch (e: FirebaseAuthUserCollisionException) {
            Log.e("AuthViewModel", "Email already in use", e)
            updateDataError.value = "Этот email уже используется."
            false
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error updating Firebase Auth profile", e)
            updateDataError.value = "Ошибка при обновлении профиля: ${e.message}"
            false
        }
    }
}
