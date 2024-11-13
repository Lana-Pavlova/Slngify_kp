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
import com.example.slngify_kp.screens.HomePage
import com.example.slngify_kp.ui.theme.Slngify_kp


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Slngify_kp {
                HomePage()  // HomePage как основной интерфейс
            }
        }
    }
}