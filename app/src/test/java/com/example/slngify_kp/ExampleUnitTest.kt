package com.example.slngify_kp

import org.junit.Test

import org.junit.Assert.*
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.slngify_kp.viewmodel.AuthViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

fun <T> createMock(clazz: Class<T>): T = mock(clazz)
@RunWith(MockitoJUnitRunner::class)
@ExperimentalCoroutinesApi
class AuthViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun registerUser_successScenario() = runTest  {
        // Arrange
        val email = "test2@example.com"
        val password = "password123"

        // Создание моков
        val firebaseAuth: FirebaseAuth = createMock(FirebaseAuth::class.java)
        val firebaseFirestore: FirebaseFirestore = createMock(FirebaseFirestore::class.java)
        val authResultTask: Task<AuthResult> = createMock(Task::class.java as Class<Task<AuthResult>>)
        val firebaseUser: FirebaseUser = createMock(FirebaseUser::class.java)
        val authResult: AuthResult = createMock(AuthResult::class.java)
        val firestoreTask: Task<Void> = createMock(Task::class.java as Class<Task<Void>>)
        val documentReference: DocumentReference = createMock(DocumentReference::class.java)
        val collectionReference: CollectionReference = createMock(CollectionReference::class.java)

        // Настройка поведения моков
        doReturn(authResultTask).`when`(firebaseAuth).createUserWithEmailAndPassword(email, password)
        `when`(authResultTask.isSuccessful).thenReturn(true)
        `when`(authResultTask.result).thenReturn(authResult)
        `when`(authResult.user).thenReturn(firebaseUser)
        `when`(firebaseUser.uid).thenReturn("testUid")

        doReturn(collectionReference).`when`(firebaseFirestore).collection("users")
        doReturn(documentReference).`when`(collectionReference).document("testUid")
        doReturn(firestoreTask).`when`(documentReference).set(any())
        `when`(firestoreTask.isSuccessful).thenReturn(true)

        // Создание ViewModel и внедрение зависимостей
        val authViewModel = AuthViewModel()
        authViewModel.auth = firebaseAuth
        authViewModel.db = firebaseFirestore

        // Act
        var successResult: Boolean? = null
        var errorMessage: String? = null
        authViewModel.registerUser(email, password) { success, message ->
            successResult = success
            errorMessage = message
        }

        // Assert
        verify(firebaseAuth).createUserWithEmailAndPassword(email, password)
        verify(collectionReference).document("testUid")
        verify(documentReference).set(any())

        assertEquals(true, successResult)
        assertNull(errorMessage)
    }

    // ... Другие тесты
}