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
    val registrationSuccess = MutableStateFlow(false)
    val loginLoading = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)
    val loginSuccess = MutableStateFlow(false)
    val updateDataLoading = MutableStateFlow(false)
    val updateDataError = MutableStateFlow<String?>(null)
    val updateDataSuccess = MutableStateFlow(false)
    val verificationEmailSent = MutableStateFlow(false) //  для отслеживания отправки письма с подтверждением

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

        viewModelScope.launch {
            val user = auth.currentUser ?: run {
                updateDataLoading.value = false
                updateDataError.value = "User not authenticated"
                onComplete(false)
                return@launch
            }

            try {
                // 1. Re-authenticate user
                val credential = EmailAuthProvider.getCredential(user.email!!, password)
                user.reauthenticate(credential).await()

                // 2. Update email
                user.updateEmail(newEmail).await()

                // 3. Send verification email
                user.sendEmailVerification().await()
                updateDataError.value = "Письмо с подтверждением отправлено на ваш новый email. Пожалуйста, подтвердите его."
                onComplete(true)

            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e("AuthViewModel", "Invalid credentials", e)
                updateDataError.value = "Неверный пароль."
                onComplete(false)
            } catch (e: FirebaseAuthException) {
                Log.e("AuthViewModel", "Firebase Auth error", e)
                updateDataError.value = "Ошибка: ${e.message}"
                onComplete(false)
            }
            catch (e: Exception) {
                Log.e("AuthViewModel", "Error changing email", e)
                updateDataError.value = "Ошибка при смене email: ${e.message}"
                onComplete(false)
            } finally {
                updateDataLoading.value = false
            }
        }
    }
    fun registerUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        registrationLoading.value = true
        registrationError.value = null
        registrationSuccess.value = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result.user
                    user?.let {
                        it.sendEmailVerification()
                            .addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    val userDocRef = db.collection("users").document(it.uid)
                                    userDocRef.set(
                                        mapOf(
                                            "email" to email,
                                            "displayName" to "",
                                            "isEmailVerified" to false
                                        )
                                    ).addOnCompleteListener { firestoreTask ->
                                        registrationLoading.value = false
                                        if (firestoreTask.isSuccessful) {
                                            registrationSuccess.value = true
                                            onComplete(true, "Регистрация успешна. Пожалуйста, подтвердите свой email.")
                                        } else {
                                            registrationError.value = firestoreTask.exception?.message
                                                ?: "Failed to add user data"
                                            registrationSuccess.value = false
                                            onComplete(
                                                false,
                                                firestoreTask.exception?.message ?: "Failed to add user data"
                                            )
                                        }
                                    }
                                } else {
                                    registrationLoading.value = false
                                    registrationError.value = verificationTask.exception?.message
                                        ?: "Failed to send verification email"
                                    registrationSuccess.value = false
                                    onComplete(
                                        false,
                                        verificationTask.exception?.message ?: "Failed to send verification email"
                                    )
                                }
                            }
                    }
                } else {
                    registrationLoading.value = false
                    registrationError.value = task.exception?.message ?: "Registration failed"
                    registrationSuccess.value = false
                    val errorMessage = task.exception?.message ?: "Регистрация не удалась"
                    onComplete(false, errorMessage)
                }
            }
    }
    // Функция логина
    fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        loginLoading.value = true
        loginError.value = null
        loginSuccess.value = false

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
    fun updateUserData(
        name: String,
        email: String,
        updateName: Boolean,
        updateEmail: Boolean,
        password: String,
        onComplete: (Boolean) -> Unit
    ) {
        updateDataLoading.value = true
        updateDataError.value = null
        updateDataSuccess.value = false
        verificationEmailSent.value = false

        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: run {
                updateDataLoading.value = false
                updateDataError.value = "User not authenticated"
                onComplete(false)
                return@launch
            }

            // Обновление email
            if (updateEmail) {
                if (!isEmailUnique(email)) {
                    updateDataLoading.value = false
                    updateDataError.value = "Данный email уже используется"
                    onComplete(false)
                    return@launch
                }

                val isEmailUpdateSuccessful = updateFirebaseAuthProfile(email, password)
                if (!isEmailUpdateSuccessful) {
                    updateDataLoading.value = false
                    onComplete(false)
                    return@launch
                }

                // Показываем пользователю сообщение о необходимости подтверждения email
                updateDataError.value = "Мы отправили письмо для подтверждения на $email. Пожалуйста, проверьте вашу почту и перейдите по ссылке в письме, чтобы завершить смену адреса."
                verificationEmailSent.value = true
                updateDataLoading.value = false
                onComplete(false)
                return@launch
            }

            // Обновление имени
            if (updateName) {
                val isNameUpdateSuccessful = updateNameProfile(name)
                if (!isNameUpdateSuccessful) {
                    updateDataLoading.value = false
                    onComplete(false)
                    return@launch
                }
            }

            // Обновление данных в Firestore
            if (updateName || updateEmail) { // Проверяем, нужно ли обновлять Firestore
                val isFirestoreUpdateSuccessful = updateFirestore(userId, email, name)

                updateDataLoading.value = false
                updateDataSuccess.value = isFirestoreUpdateSuccessful
                onComplete(isFirestoreUpdateSuccessful)
            } else {
                updateDataLoading.value = false
                updateDataSuccess.value = true // Если ничего не обновлялось, считаем, что все прошло успешно
                onComplete(true)
            }
        }
    }

    // Функция для проверки уникальности email
    private suspend fun isEmailUnique(email: String): Boolean {
        return try {
            val result = auth.fetchSignInMethodsForEmail(email).await()
            result.signInMethods?.isEmpty() ?: true
        } catch (e: FirebaseAuthException) {
            // Проверяем код ошибки
            if (e.errorCode == "ERROR_INVALID_EMAIL") {
                // Если email имеет неверный формат, считаем его уникальным
                return true
            } else {
                // Если произошла другая ошибка, сообщаем об этом
                Log.e("AuthViewModel", "Error checking email uniqueness", e)
                return false
            }
        } catch (e: Exception) {
            // Обрабатываем другие возможные исключения
            Log.e("AuthViewModel", "Error checking email uniqueness", e)
            return false
        }
    }
    // Функция для обновления профиля Firebase Authentication
    private suspend fun updateFirebaseAuthProfile(email: String, password: String): Boolean {
        val user = auth.currentUser ?: return false

        return try {
            // ре-аутентификация
            val credential = EmailAuthProvider.getCredential(user.email!!, password)
            user.reauthenticate(credential).await()

            // подтверждение нвоой почты после верификации
            user.verifyBeforeUpdateEmail(email).await()

            verificationEmailSent.value = true
            updateDataError.value = "Пожалуйста, проверьте вашу новую почту для подтверждения."

            true
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            Log.e("AuthViewModel", "Re-authentication required", e)
            updateDataError.value = "Требуется повторная аутентификация. Пожалуйста, введите свой пароль еще раз."
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
    private suspend fun updateNameProfile(name: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            if (name.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profileUpdates).await()
            }
            true
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error updating name profile", e)
            updateDataError.value = "Ошибка при обновлении имени: ${e.message}"
            false
        }
    }
    // Функция для обновления данных в Firestore
    private suspend fun updateFirestore(uid: String, email: String, name: String): Boolean {
        return try {
            val userRef = db.collection("users").document(uid)
            val updates = mutableMapOf<String, Any?>()
            if (email.isNotBlank()) {
                updates["email"] = email
            }
            if (name.isNotBlank()) {
                updates["displayName"] = name
            } else {
                updates["displayName"] = null // Разрешаем установить displayName в null
            }
            userRef.update(updates as Map<String, Any>).await()
            true
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error updating Firestore", e)
            updateDataError.value = "Ошибка при обновлении данных (Firestore): ${e.message}"
            false
        }
    }
}
