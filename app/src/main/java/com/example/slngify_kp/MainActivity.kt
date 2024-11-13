package com.example.slngify_kp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.slngify_kp.screens.RegisterScreen
import com.example.slngify_kp.screens.SignInScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Composable
fun MyApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.SignIn) }
    var emailFromRegistration by remember { mutableStateOf("") }

    when (currentScreen) {
        is Screen.SignIn -> {
            SignInScreen(
                onSignInClick = { email, password ->
                    // Handle sign-in logic, e.g., authenticate user
                },
                onRegisterClick = { initialEmail ->
                    emailFromRegistration = initialEmail
                    currentScreen = Screen.Register
                },
                onForgotPasswordClick = {
                    // Handle forgot password logic
                }
            )
        }

        is Screen.Register -> {
            RegisterScreen(
                initialEmail = emailFromRegistration,
                onRegister = { email, password ->
                    // Handle successful registration
                    emailFromRegistration = email
                    currentScreen = Screen.SignIn
                },
                onBackClick = {
                    currentScreen = Screen.SignIn
                }
            )
        }
    }
}

sealed class Screen {
    object SignIn : Screen()
    object Register : Screen()
}

