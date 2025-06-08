package com.example.slngify_kp.screens

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.slngify_kp.R
import com.example.slngify_kp.ui.theme.AppTypography
import com.example.slngify_kp.ui.theme.MyTheme
import com.example.slngify_kp.ui.theme.lightScheme
import com.example.slngify_kp.viewmodel.AchievementData
import com.example.slngify_kp.viewmodel.AuthViewModel
import com.example.slngify_kp.viewmodel.ProgressViewModel
import com.example.slngify_kp.viewmodel.UserProgress
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore


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
fun ProfilePage(navController: NavHostController) {
    val viewModel: ProgressViewModel = viewModel()
    val authViewModel: AuthViewModel = viewModel()
    val userProgress by viewModel.userProgress.collectAsState()
    val lessonList by viewModel.lessonList.collectAsState()

    val newLessonsCompleted by viewModel.newLessonsCompleted.collectAsState()
    val totalLessons by viewModel.totalLessons.collectAsState()

    Log.d("ProfilePage", "newLessonsCompleted: $newLessonsCompleted, totalLessons: $totalLessons")

    var showAuthorInfo by remember { mutableStateOf(false) }
    var showAuthForm by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }

    val userId = Firebase.auth.currentUser?.uid
    val context = LocalContext.current

    var selectedItem by remember { mutableStateOf("profilePage") }

    val userNameState = remember { mutableStateOf("") }

    LaunchedEffect(key1 = userId) {
        if (userId != null) {
            val userDocRef = Firebase.firestore.collection("users").document(userId)
            userDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        userNameState.value = document.getString("displayName") ?: ""
                    } else {
                        Log.d("Firebase", "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d("Firebase", "get failed with ", exception)
                }
        }
    }

    MyAppTheme {
        Scaffold(
            bottomBar = {
                BottomNavigationBar(
                    navController = navController,
                    selectedItem = selectedItem,
                    onItemSelected = { screen -> selectedItem = screen }
                )
            }
        ) { padding ->
            ProfileContent(
                paddingValues = padding,
                userName = userNameState.value,
                showAuthorInfo = showAuthorInfo,
                showAuthForm = showAuthForm,
                isRegistering = isRegistering,
                lessonList = lessonList,
                userProgress = userProgress,
                onShowAuthorInfoChanged = { showAuthorInfo = it },
                onShowAuthFormChanged = { showAuthForm = it },
                onIsRegisteringChanged = { isRegistering = it },
                onUserNameChanged = { userNameState.value = it },
                onSignOutClick = { authViewModel.signOutUser(navController) },
                context = context,
                newLessonsCompleted = newLessonsCompleted,
                totalLessons = totalLessons
            )
        }
    }
}
@Composable
fun ProfileContent(
    paddingValues: PaddingValues,
    userName: String,
    showAuthorInfo: Boolean,
    showAuthForm: Boolean,
    isRegistering: Boolean,
    lessonList: List<*>,
    userProgress: UserProgress,
    onShowAuthorInfoChanged: (Boolean) -> Unit,
    onShowAuthFormChanged: (Boolean) -> Unit,
    onIsRegisteringChanged: (Boolean) -> Unit,
    onUserNameChanged: (String) -> Unit,
    onSignOutClick: () -> Unit,
    context: Context,
    newLessonsCompleted: Int,
    totalLessons: Int
) {
    Log.d("ProfileContent", "newLessonsCompleted: $newLessonsCompleted, totalLessons: $totalLessons")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProfileHeader(userName = userName)
        Spacer(modifier = Modifier.height(16.dp))

        AuthorInfoSection(
            showAuthorInfo = showAuthorInfo,
            userName = userName,
            onShowAuthorInfoChanged = onShowAuthorInfoChanged,
            onUserNameChanged = onUserNameChanged,
            context = context
        )

        AuthSection(
            showAuthForm = showAuthForm,
            isRegistering = isRegistering,
            onShowAuthFormChanged = onShowAuthFormChanged,
            onIsRegisteringChanged = onIsRegisteringChanged
        )

        StatisticsSection(
            newLessonsCompleted = newLessonsCompleted,
            totalLessons = totalLessons
        )

        Spacer(modifier = Modifier.height(16.dp))

        AchievementsSection(achievements = userProgress.achievements)

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSignOutClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = "Выйти")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
@Composable
fun ProfileHeader(userName: String) {
    // Получаем email из Auth
    val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: "Email не найден"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.goose),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = userName, style = MaterialTheme.typography.headlineMedium)
        Text(text = userEmail, style = MaterialTheme.typography.bodyLarge)
    }
}


@Composable
fun StatisticsSection(newLessonsCompleted: Int, totalLessons: Int) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Статистика",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatisticCard(
                icon = Icons.Default.Check,
                count = newLessonsCompleted,
                label = "Пройденные уроки"
            )
            StatisticCard(
                icon = Icons.Default.Check,
                count = totalLessons,
                label = "Всего уроков"
            )
        }
    }
}

@Composable
fun StatisticCard(icon: ImageVector, count: Int, label: String) {
    Card(
        modifier = Modifier.size(150.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize()
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count.toString(), style = MaterialTheme.typography.titleLarge)
            Text(text = label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
@Composable
fun AchievementsSection(achievements: List<AchievementData>) {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Достижения",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            achievements.forEach { achievement ->
                AchievementButton(achievement = achievement)
            }
        }
    }
}

@Composable
fun AchievementButton(achievement: AchievementData) {
    OutlinedButton(
        onClick = { /* TODO: Handle achievement click */ },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = achievement.name)
            Spacer(modifier = Modifier.width(4.dp))
            Image(
                painter = rememberAsyncImagePainter(achievement.icon),
                contentDescription = "Иконка достижения",
                modifier = Modifier.size(40.dp) // Задаем размер картинки
            )
        }
    }
}


@Composable
fun AuthForm(onRegisterClick: () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading by authViewModel.loginLoading.collectAsState(false)
    val errorMessage = authViewModel.loginError.collectAsState(initial = null)
    val errorMessageValue = errorMessage.value
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Почта") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (errorMessageValue != null) {
                Text(
                    text = errorMessageValue,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = {
                            authViewModel.loginUser(email, password) { success, message ->
                                if (success) {
                                    Toast.makeText(context, "Вход выполнен", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Войти")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onRegisterClick) {
                        Text("Создать аккаунт", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}
@Composable
fun AuthSection(
    showAuthForm: Boolean,
    isRegistering: Boolean,
    onShowAuthFormChanged: (Boolean) -> Unit,
    onIsRegisteringChanged: (Boolean) -> Unit
) {
    Button(
        onClick = { onShowAuthFormChanged(!showAuthForm) },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Text("Зарегистрироваться или Авторизоваться")
    }
    if (showAuthForm) {
        if (isRegistering) {
            RegistrationForm(onRegisterComplete = { onIsRegisteringChanged(false) })
        } else {
            AuthForm(onRegisterClick = { onIsRegisteringChanged(true) })
        }
    }
}

@Composable
fun AuthorInfo(
    initialName: String,
    viewModel: AuthViewModel,
    onNameChanged: (String) -> Unit,
    onSave: (Boolean) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var newEmail by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    val loading by viewModel.updateDataLoading.collectAsState()
    val success by viewModel.updateDataSuccess.collectAsState()
    val error by viewModel.updateDataError.collectAsState()

    val context = LocalContext.current

    val email = viewModel.userEmail.collectAsState().value

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isEditing
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "Почта: $email")

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("Новый email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = isEditing
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = isEditing,
                visualTransformation = PasswordVisualTransformation(),
                isError = newEmail.isNotEmpty() && password.isEmpty()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isEditing) {
                    Button(
                        onClick = {
                            // Сначала обновляем имя (если оно изменилось)
                            if (name != initialName) {
                                viewModel.updateNameProfile(name) { isNameUpdateSuccessful ->
                                    if (isNameUpdateSuccessful) {
                                        onNameChanged(name) // Обновляем UI
                                        // Теперь обновляем email (если он указан)
                                        if (newEmail.isNotEmpty()) {
                                            viewModel.changeEmail(newEmail, password) { isEmailChangeSuccessful ->
                                                if (isEmailChangeSuccessful) {
                                                    Toast.makeText(context, "Имя и email успешно обновлены!", Toast.LENGTH_SHORT).show()
                                                    onSave(true)
                                                } else {
                                                    Toast.makeText(context, "Ошибка при обновлении email", Toast.LENGTH_SHORT).show()
                                                    onSave(false)
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Имя успешно обновлено!", Toast.LENGTH_SHORT).show()
                                            onSave(true)
                                        }
                                    } else {
                                        Toast.makeText(context, "Ошибка при обновлении имени", Toast.LENGTH_SHORT).show()
                                        onSave(false)
                                    }
                                }
                            } else if (newEmail.isNotEmpty()) { // Если имя не изменилось, обновляем только email
                                viewModel.changeEmail(newEmail, password) { isEmailChangeSuccessful ->
                                    if (isEmailChangeSuccessful) {
                                        Toast.makeText(context, "Email успешно обновлен!", Toast.LENGTH_SHORT).show()
                                        onSave(true)
                                    } else {
                                        Toast.makeText(context, "Ошибка при обновлении email", Toast.LENGTH_SHORT).show()
                                        onSave(false)
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Ничего не изменилось!", Toast.LENGTH_SHORT).show()
                                onSave(true)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        enabled = !loading
                    ) {
                        Text("Сохранить")
                    }
                } else {
                    Button(
                        onClick = { isEditing = true },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("Редактировать")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                viewModel.resendEmailVerification { success, message ->
                    if (success) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Отправить письмо с подтверждением повторно")
            }
            if (loading) {
                CircularProgressIndicator()
            }
            if (error != null) {
                Text(text = error ?: "Неизвестная ошибка", color = Color.Red)
            }
            if (success) {
                Text(text = "Данные успешно обновлены!", color = Color.Green)
            }
        }
    }
}
@Composable
fun AuthorInfoSection(
    showAuthorInfo: Boolean,
    userName: String,
    onShowAuthorInfoChanged: (Boolean) -> Unit,
    onUserNameChanged: (String) -> Unit,
    context: Context
) {
    val authViewModel: AuthViewModel = viewModel()
    val isLoading by authViewModel.updateDataLoading.collectAsState(false)
    val nameState = remember { mutableStateOf(userName) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { onShowAuthorInfoChanged(!showAuthorInfo) },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text("Сведения об авторе")
        }

        if (showAuthorInfo) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                AuthorInfo(
                    initialName = nameState.value,
                    viewModel = authViewModel,
                    onNameChanged = { newName ->
                        nameState.value = newName
                        onUserNameChanged(newName)
                    },
                    onSave = { isSuccessful ->
                    }
                )
            }
        }
    }
}

@Composable
fun RegistrationForm(onRegisterComplete: () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading by authViewModel.registrationLoading.collectAsState(false)
    val errorMessage = authViewModel.registrationError.collectAsState(initial = null)
    val errorMessageValue = errorMessage.value
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Имя") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Почта") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (errorMessageValue != null) {
                Text(
                    text = errorMessageValue,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        authViewModel.registerUser(name, email, password) { success, message ->
                            if (success) {
                                Toast.makeText(context, "Аккаунт создан", Toast.LENGTH_SHORT).show()
                                onRegisterComplete()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Зарегистрироваться")
                }
            }
        }
    }
}

@Composable
fun MyAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightScheme,
        typography = AppTypography,
        content = content
    )
}

@Preview(showBackground = true)
@Composable
fun ProfilePagePreview() {
    MyTheme {
        val navController = rememberNavController()
        ProfilePage(navController)
    }
}