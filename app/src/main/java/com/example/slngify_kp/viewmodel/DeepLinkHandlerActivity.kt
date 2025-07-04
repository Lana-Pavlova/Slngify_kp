package com.example.slngify_kp.viewmodel

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class DeepLinkHandlerActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authViewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        handleIntent(intent)

        observeEmailChangeConfirmationResult()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        authViewModel.resetEmailChangeConfirmationState()

        if (intent != null && intent.action == Intent.ACTION_VIEW) {
            val uri: Uri? = intent.data
            if (uri != null) {
                val oobCode = uri.getQueryParameter("oobCode")

                if (oobCode != null) {
                    Log.d("DeepLinkHandler", "Received oobCode: $oobCode")
                    // Передаем только oobCode в ViewModel для обработки
                    authViewModel.handleEmailChangeConfirmation(oobCode)

                } else {
                    Log.w("DeepLinkHandler", "Deep link does not contain oobCode.")
                    showErrorAndFinish("Неверная ссылка.")
                }
            } else {
                Log.w("DeepLinkHandler", "Intent data is null.")
                showErrorAndFinish("Неверная ссылка.")
            }
        } else {
            Log.w("DeepLinkHandler", "Intent action is not ACTION_VIEW.")
            showErrorAndFinish("Неверная ссылка.")
        }
    }

    private fun observeEmailChangeConfirmationResult() {
        lifecycleScope.launch {
            authViewModel.emailChangeConfirmationResult.collect { result ->
                when (result) {
                    is ResultState.Loading -> {
                        Log.d("DeepLinkHandler", "Applying action code - Loading...")
                    }
                    is ResultState.Success -> {
                        // Email успешно подтвержден и изменен
                        val newEmail = result.data
                        Log.d("DeepLinkHandler", "Email change confirmed successfully to $newEmail!")
                        showSuccessAndFinish("Ваш email успешно изменен на $newEmail!")
                    }
                    is ResultState.Error -> {
                        // Произошла ошибка при подтверждении
                        val errorMessage = result.message
                        Log.e("DeepLinkHandler", "Error applying action code: $errorMessage")
                        showErrorAndFinish("Не удалось подтвердить изменение email: $errorMessage")
                    }
                    is ResultState.Idle -> {
                    }
                }
            }
        }
    }

    // Вспомогательные функции для отображения результата и завершения
    private fun showSuccessAndFinish(message: String) {
        // TODO: Реализовать показ сообщения пользователю, например, Toast или AlertDialog
        Log.i("DeepLinkHandler", "Success: $message")
        finish()
    }

    private fun showErrorAndFinish(message: String) {
        // TODO: Реализовать показ сообщения об ошибке пользователю
        Log.e("DeepLinkHandler", "Error: $message")
        finish()
    }
}

sealed class ResultState<out T> {
    object Idle : ResultState<Nothing>()
    object Loading : ResultState<Nothing>()
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(val message: String) : ResultState<Nothing>()
}