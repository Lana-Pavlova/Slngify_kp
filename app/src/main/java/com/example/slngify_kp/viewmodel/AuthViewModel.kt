package com.example.slngify_kp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.util.Log
import androidx.navigation.NavController
import com.google.firebase.auth.UserProfileChangeRequest



// добавить автоматическое обновление страницы


class AuthViewModel : ViewModel() {

    var auth: FirebaseAuth = Firebase.auth
    var db: FirebaseFirestore = Firebase.firestore

    val registrationLoading = MutableLiveData<Boolean>()
    val registrationError = MutableLiveData<String?>()
    val registrationSuccess = MutableLiveData<Boolean>()
    val loginLoading = MutableLiveData<Boolean>()
    val loginError = MutableLiveData<String?>()
    val loginSuccess = MutableLiveData<Boolean>()
    val updateDataLoading = MutableLiveData<Boolean>()
    val updateDataError = MutableLiveData<String?>()
    val updateDataSuccess = MutableLiveData<Boolean>()

    // Функция регистрации
    fun registerUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        registrationLoading.value = true
        registrationError.value = null
        registrationSuccess.value = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result.user
                    user?.let {
                        val userDocRef = db.collection("users").document(it.uid)
                        userDocRef.set(mapOf(
                            "email" to email,
                        )).addOnCompleteListener { firestoreTask ->
                            registrationLoading.value = false
                            if (firestoreTask.isSuccessful) {
                                registrationSuccess.value = true
                                onComplete(true, null)
                            } else {
                                registrationError.value = firestoreTask.exception?.message ?: "Failed to add user data"
                                registrationSuccess.value = false
                                onComplete(false, firestoreTask.exception?.message ?: "Failed to add user data")
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
        onComplete: (Boolean, String?) -> Unit
    ) {
        updateDataLoading.value = true
        updateDataError.value = null
        updateDataSuccess.value = false
        val user = auth.currentUser

        user?.let {
            isEmailUnique(email) { isUnique ->
                if (isUnique || !updateEmail) {
                    if (updateEmail) {
                        it.updateEmail(email).addOnCompleteListener { taskEmail ->
                            if (taskEmail.isSuccessful) {
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build()

                                if (updateName) {
                                    it.updateProfile(profileUpdates).addOnCompleteListener { taskName ->
                                        if (taskName.isSuccessful) {
                                            updateFirestore(it.uid, email, name) { firestoreSuccess, firestoreMessage ->
                                                updateDataLoading.value = false
                                                if (firestoreSuccess) {
                                                    updateDataSuccess.value = true
                                                    onComplete(true, null)
                                                } else {
                                                    updateDataError.value = firestoreMessage
                                                    updateDataSuccess.value = false
                                                    onComplete(false, firestoreMessage)
                                                }
                                            }
                                        } else {
                                            updateDataLoading.value = false
                                            updateDataError.value = "Ошибка при обновлении имени: ${taskName.exception?.message}"
                                            updateDataSuccess.value = false
                                            onComplete(false, "Ошибка при обновлении имени: ${taskName.exception?.message}")
                                        }
                                    }
                                } else {
                                    updateFirestore(it.uid, email, name) { firestoreSuccess, firestoreMessage ->
                                        updateDataLoading.value = false
                                        if (firestoreSuccess) {
                                            updateDataSuccess.value = true
                                            onComplete(true, null)
                                        } else {
                                            updateDataError.value = firestoreMessage
                                            updateDataSuccess.value = false
                                            onComplete(false, firestoreMessage)
                                        }
                                    }
                                }
                            } else {
                                updateDataLoading.value = false
                                updateDataError.value = "Ошибка при обновлении почты: ${taskEmail.exception?.message}"
                                updateDataSuccess.value = false
                                onComplete(false, "Ошибка при обновлении почты: ${taskEmail.exception?.message}")
                            }
                        }
                    } else {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        if (updateName) {
                            it.updateProfile(profileUpdates).addOnCompleteListener { taskName ->
                                if (taskName.isSuccessful) {
                                    updateFirestore(it.uid, email, name) { firestoreSuccess, firestoreMessage ->
                                        updateDataLoading.value = false
                                        if (firestoreSuccess) {
                                            updateDataSuccess.value = true
                                            onComplete(true, null)
                                        } else {
                                            updateDataError.value = firestoreMessage
                                            updateDataSuccess.value = false
                                            onComplete(false, firestoreMessage)
                                        }
                                    }
                                } else {
                                    updateDataLoading.value = false
                                    updateDataError.value = "Ошибка при обновлении имени: ${taskName.exception?.message}"
                                    updateDataSuccess.value = false
                                    onComplete(false, "Ошибка при обновлении имени: ${taskName.exception?.message}")
                                }
                            }
                        } else {
                            updateDataLoading.value = false
                            updateDataSuccess.value = true
                            onComplete(true, null)
                        }
                    }
                } else {
                    updateDataLoading.value = false
                    updateDataError.value = "Данный email уже используется"
                    updateDataSuccess.value = false
                    onComplete(false, "Данный email уже используется")
                }
            }
        }
    }

    // Функция проверки уникальности email
    private fun isEmailUnique(email: String, onResult: (Boolean) -> Unit) {
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val querySnapshot = task.result
                    onResult(querySnapshot.isEmpty)
                } else {
                    onResult(false)
                }
            }
    }

    // Функция обновления данных в Firestore
    private fun updateFirestore(userId: String, email: String, name: String, onComplete: (Boolean, String?) -> Unit) {
        val userDocRef = db.collection("users").document(userId)
        userDocRef.update(mapOf(
            "email" to email,
            "name" to name
        )).addOnCompleteListener { firestoreTask ->
            if (firestoreTask.isSuccessful) {
                onComplete(true, null)
            } else {
                onComplete(false, firestoreTask.exception?.message ?: "Failed to update Firestore")
            }
        }
    }
}
