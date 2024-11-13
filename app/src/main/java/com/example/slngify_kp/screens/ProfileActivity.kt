package com.example.slngify_kp.screens

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfilePage()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage() {

    var showAuthorInfo by remember { mutableStateOf(false) }
    var showAuthForm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Личный кабинет") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(onClick = { showAuthorInfo = !showAuthorInfo }) {
                    Text("Сведения об авторе")
                }
                if (showAuthorInfo) {
                    Text(
                        text = "Имя: Иван Иванов\nПочта: ivan@example.com",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Button(onClick = { showAuthForm = !showAuthForm }) {
                    Text("Зарегистрироваться или Авторизоваться")
                }
                if (showAuthForm) {
                    AuthForm()
                }

                Spacer(modifier = Modifier.height(16.dp))

                StatisticsSection()

                Spacer(modifier = Modifier.height(16.dp))

                ProgressSection()

                Spacer(modifier = Modifier.height(16.dp))

                RatingSection()
            }
        }
    )
}

@Composable
fun AuthForm() {
    Column {
        TextField(
            value = "",
            onValueChange = {},
            label = { Text("Имя пользователя") },
            modifier = Modifier.fillMaxWidth()
        )
        TextField(
            value = "",
            onValueChange = {},
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { /* TODO: Handle login */ }, modifier = Modifier.align(Alignment.End)) {
            Text("Войти")
        }
    }
}

@Composable
fun StatisticsSection() {
    Text(
        text = "Статистика по выполненным заданиям: 10 из 20",
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun ProgressSection() {
    Text(text = "Прогресс обучения", style = MaterialTheme.typography.bodyLarge)
    LinearProgressIndicator(progress = 0.5f, modifier = Modifier.fillMaxWidth())
}

@Composable
fun RatingSection() {
    Text(text = "Рейтинг пользователей", style = MaterialTheme.typography.bodyLarge)
    val users = listOf(
        "Иван Иванов - 1500 очков",
        "Анна Смирнова - 1400 очков",
        "Петр Петров - 1300 очков"
    )
    users.forEach { user ->
        Text(text = user, style = MaterialTheme.typography.bodyMedium)
    }
}