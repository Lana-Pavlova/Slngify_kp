package com.example.slngify_kp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.slngify_kp.ui.theme.Slngify_kp

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashScreen()
        }

        lifecycleScope.launch {
            delay(3000) // Задержка в 3 секунды
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish() // Закрываем SplashActivity
        }
    }
}

@Composable
fun SplashScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Slangify",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 16.dp) // Отступ снизу
            )
            Image(
                painter = painterResource(id = R.drawable.goose),
                contentDescription = "Splash Image",
                modifier = Modifier
                    .size(128.dp) // Размер изображения
                    .clip(CircleShape) // Обрезаем изображение в круг
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    Slngify_kp {
        SplashScreen()
    }
}
