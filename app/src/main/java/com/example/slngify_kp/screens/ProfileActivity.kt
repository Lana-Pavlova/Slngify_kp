package com.example.slngify_kp.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.slngify_kp.R
import com.example.slngify_kp.ui.theme.MyTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Patterns

class ProfilePageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                NavigationComponent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(navController: NavController) {
    var showAuthorInfo by remember { mutableStateOf(false) }
    var showAuthForm by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Личный кабинет") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.back),
                            contentDescription = "Назад"
                        )
                    }
                },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfileHeader()
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showAuthorInfo = !showAuthorInfo }) {
                    Text("Сведения об авторе")
                }
                if (showAuthorInfo) {
                    AuthorInfo()
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showAuthForm = !showAuthForm }) {
                    Text("Зарегистрироваться или Авторизоваться")
                }
                if (showAuthForm) {
                    if (isRegistering) {
                        RegistrationForm()
                    } else {
                        AuthForm(onRegisterClick = { isRegistering = true })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                StatisticsSection()
                Spacer(modifier = Modifier.height(16.dp))
//                ProgressSection()
                Spacer(modifier = Modifier.height(16.dp))
                RatingSection()
            }
        }
    )
}
@Composable
fun ProfileHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.your_profile_picture),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Иван Иванов", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("ivan@example.com", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun AuthorInfo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Person,
                contentDescription = "Имя",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Имя: Иван Иванов", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Email,
                contentDescription = "Email",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Почта: ivan@example.com", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AuthForm(onRegisterClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Почта") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { /* TODO: Handle login */ },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Войти")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onRegisterClick){
            Text("Создать аккаунт")
        }
    }
}
@Composable
fun RegistrationForm() {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val auth = Firebase.auth
    val scope = rememberCoroutineScope()

    val isFormValid = firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() &&
            password.isNotBlank() && password == confirmPassword &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        TextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth(),
            isError = firstName.isBlank(),
            supportingText = { if (firstName.isBlank()) Text("Поле не может быть пустым", color = MaterialTheme.colorScheme.error) else null }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Фамилия") },
            modifier = Modifier.fillMaxWidth(),
            isError = lastName.isBlank(),
            supportingText = { if (lastName.isBlank()) Text("Поле не может быть пустым", color = MaterialTheme.colorScheme.error) else null }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Почта") },
            modifier = Modifier.fillMaxWidth(),
            isError = !Patterns.EMAIL_ADDRESS.matcher(email).matches(),
            supportingText = { if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) Text("Неверный формат почты", color = MaterialTheme.colorScheme.error) else null }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            isError = password.isBlank(),
            visualTransformation = PasswordVisualTransformation(),
            supportingText = { if (password.isBlank()) Text("Поле не может быть пустым", color = MaterialTheme.colorScheme.error) else null }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Подтверждение пароля") },
            modifier = Modifier.fillMaxWidth(),
            isError = confirmPassword.isBlank() || confirmPassword != password,
            visualTransformation = PasswordVisualTransformation(),
            supportingText = { if (confirmPassword.isBlank() || confirmPassword != password) Text("Пароли не совпадают", color = MaterialTheme.colorScheme.error) else null }
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch {
                    if (isFormValid) {
                        try {
                            val userCredential = auth.createUserWithEmailAndPassword(email, password).await()
                            val user = userCredential.user
                            val userDocRef = Firebase.firestore.collection("users").document(user?.uid!!)
                            userDocRef.set(mapOf(
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "email" to email,
                                "password" to password
                            )).await()
                            showError = false
                            errorMessage = ""
                        } catch (e: Exception) {
                            showError = true
                            errorMessage = e.message ?: "Registration failed"
                        }
                    } else {
                        showError = true
                        errorMessage = "Please fill in all fields correctly"
                    }
                }
            },
            enabled = isFormValid,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Зарегистрироваться")
        }
        if (showError) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }
}
@Composable
fun RatingSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(text = "Рейтинг пользователей", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Топ пользователей
        val topUsers = listOf(
            "1. Иван Иванов - 1500 очков",
            "2. Анна Смирнова - 1400 очков",
            "3. Петр Петров - 1300 очков"
        )
        topUsers.forEach { user ->
            Text(text = user, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Ваша позиция: 5-е место", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun StatisticsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(text = "Ваш прогресс", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // Пример круговой диаграммы
        CircularProgressIndicator(
            progress = 0.5f,
            modifier = Modifier.size(100.dp),
            strokeWidth = 8.dp
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Выполнено 10 из 20 заданий", style = MaterialTheme.typography.bodyMedium)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilePagePreview() {
    MyTheme {
        val navController = rememberNavController() // Создаем фиктивный NavController
        ProfilePage(navController) // Передаем navController в ProfilePage
    }
}

