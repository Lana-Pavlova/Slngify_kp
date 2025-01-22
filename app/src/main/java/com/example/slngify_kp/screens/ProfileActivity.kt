package com.example.slngify_kp.screens

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.slngify_kp.R
import com.example.slngify_kp.ui.theme.MyTheme
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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

data class UserProgress(
    val completedLessons: List<String> = emptyList(),
    val completedSections: List<String> = emptyList(),
    val sectionProgress: Map<String, Int> = emptyMap()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(navController: NavController) {
    var showAuthorInfo by remember { mutableStateOf(false) }
    var showAuthForm by remember { mutableStateOf(false) }
    var isRegistering by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("Your name") }
    var userEmail by remember { mutableStateOf("your@email.com") }
    val userId = Firebase.auth.currentUser?.uid
    val context = LocalContext.current
    val viewModel: LessonsViewModel = viewModel()
    val userProgress by viewModel.userProgress.collectAsState()
    val lessonList by viewModel.lessonList.collectAsState()

    LaunchedEffect(userId) {
        val auth = Firebase.auth
        val currentUser = auth.currentUser

        if (currentUser != null) {
            userName = currentUser.displayName ?: "Имя пользователя"
            userEmail = currentUser.email ?: "email"
        }
    }


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
                ProfileHeader(name = userName, email = userEmail)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showAuthorInfo = !showAuthorInfo }) {
                    Text("Сведения об авторе")
                }
                if (showAuthorInfo) {
                    AuthorInfo(
                        initialName = userName,
                        initialEmail = userEmail,
                        onSave = { name, email, updateName, updateEmail ->
                            updateUserData(name, email, updateName, updateEmail) { success, message ->
                                if (success) {
                                    userName = name
                                    userEmail = email
                                    Toast.makeText(context, "Данные успешно обновлены", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showAuthForm = !showAuthForm }) {
                    Text("Зарегистрироваться или Авторизоваться")
                }
                if (showAuthForm) {
                    if (isRegistering) {
                        RegistrationForm(onRegisterComplete = { isRegistering = false })
                    } else {
                        AuthForm(onRegisterClick = { isRegistering = true })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                if(lessonList.isNotEmpty()){
                    StatisticsSection(userProgress, lessonList.size)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                ProgressSection(userProgress = userProgress)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    signOutUser(navController)
                }) {
                    Text(text = "Выйти")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    )
}
@Composable
fun ProfileHeader(name : String, email : String) {
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
        Text(text = name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(text = email, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable
fun AuthorInfo(
    initialName: String,
    initialEmail: String,
    onSave: (String, String, Boolean, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var email by remember { mutableStateOf(initialEmail) }
    val context = LocalContext.current
    var updateName by remember { mutableStateOf(false) }
    var updateEmail by remember { mutableStateOf(false) }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Почта") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = updateName, onCheckedChange = { updateName = it })
            Text(text = "Изменить имя")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = updateEmail, onCheckedChange = { updateEmail = it })
            Text(text = "Изменить почту")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                onSave(name, email, updateName, updateEmail)
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Сохранить")
        }
    }
}

@Composable
fun AuthForm(onRegisterClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

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
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
        if (isLoading){
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    isLoading = true
                    loginUser(email, password) { success, message ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Вход выполнен", Toast.LENGTH_SHORT).show()
                        } else {
                            errorMessage = message
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Войти")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRegisterClick) {
                Text("Создать аккаунт")
            }
        }

    }
}
@Composable
fun RegistrationForm(onRegisterComplete: () -> Unit){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

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
        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
        if (isLoading){
            CircularProgressIndicator()
        }else {
            Button(
                onClick = {
                    isLoading = true
                    registerUser(email, password) { success, message ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "Аккаунт создан", Toast.LENGTH_SHORT).show()
                            onRegisterComplete()
                        } else {
                            errorMessage = message
                        }
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Зарегистрироваться")
            }
        }

    }
}
@Composable
fun StatisticsSection(userProgress: UserProgress, totalLessons: Int) {
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(userProgress, totalLessons){
        progress = if (totalLessons > 0) userProgress.completedLessons.size.toFloat() / totalLessons else 0f
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(text = "Ваш прогресс", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier.size(100.dp),
                strokeWidth = 8.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Пройдено ${userProgress.completedLessons.size} из $totalLessons уроков", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ProgressSection(userProgress: UserProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text("Прогресс", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
        Text("Пройдено разделов: ${userProgress.completedSections.size}", style = MaterialTheme.typography.bodyMedium)

        if (userProgress.completedLessons.isNotEmpty()) {
            Text("Пройденные уроки:", style = MaterialTheme.typography.titleMedium)
            userProgress.completedLessons.forEach { lessonId ->
                Text(text = "Урок $lessonId", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (userProgress.completedSections.isNotEmpty()) {
            Text("Пройденные разделы:", style = MaterialTheme.typography.titleMedium)
            userProgress.completedSections.forEach { sectionId ->
                Text(text = "Раздел $sectionId", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
//@Composable
//fun StatisticsSection() {
//    var userProgress by remember { mutableStateOf<UserProgress?>(null) }
//    var progress by remember { mutableStateOf(0f) }
//    var totalSections by remember { mutableStateOf(0) }
//    val userId = Firebase.auth.currentUser?.uid
//
//    LaunchedEffect(userId) {
//        if (userId != null) {
//            loadUserProgress() { progress ->
//                userProgress = progress
//            }
//            loadTotalSections() { total ->
//                totalSections = total
//            }
//        }
//    }
//
//    if (userProgress != null) {
//        val completedSections = userProgress!!.completedSections.size
//        val totalCorrectAnswers = userProgress!!.sectionProgress.values.sum()
//        progress = if (totalSections > 0) completedSections.toFloat() / totalSections else 0f
//
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
//                .padding(16.dp)
//        ) {
//            Text(text = "Ваш прогресс", style = MaterialTheme.typography.headlineSmall)
//            Spacer(modifier = Modifier.height(8.dp))
//            Box(contentAlignment = Alignment.Center) {
//                CircularProgressIndicator(
//                    progress = progress,
//                    modifier = Modifier.size(100.dp),
//                    strokeWidth = 8.dp,
//                    color = MaterialTheme.colorScheme.primary
//                )
//                Text(text = "${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleLarge)
//            }
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(text = "Пройдено $completedSections из $totalSections разделов", style = MaterialTheme.typography.bodyMedium)
//        }
//    }
//}
//@Composable
//fun ProgressSection(userProgress: UserProgress) {
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
//            .padding(16.dp)
//    ) {
//        Text("Прогресс", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
//
//        val totalCorrectAnswers = userProgress.sectionProgress.values.sum()
//        Text("Пройдено разделов: ${userProgress.completedSections.size}", style = MaterialTheme.typography.bodyMedium)
//        Text("Правильных ответов: $totalCorrectAnswers", style = MaterialTheme.typography.bodyMedium)
//        Spacer(modifier = Modifier.height(8.dp))
//
//        if (userProgress.completedLessons.isNotEmpty()) {
//            Text("Пройденные уроки:", style = MaterialTheme.typography.titleMedium)
//            userProgress.completedLessons.forEach { lessonId ->
//                Text(text = "Урок $lessonId", style = MaterialTheme.typography.bodyMedium)
//            }
//            Spacer(modifier = Modifier.height(8.dp))
//        }
//
//
//        if (userProgress.completedSections.isNotEmpty()) {
//            Text("Пройденные разделы:", style = MaterialTheme.typography.titleMedium)
//            userProgress.completedSections.forEach { sectionId ->
//                Text(text = "Раздел $sectionId", style = MaterialTheme.typography.bodyMedium)
//            }
//        }
//    }
//}
//fun loadUserProgress(onProgressLoaded: (UserProgress) -> Unit) {
//    val userId = Firebase.auth.currentUser?.uid ?: "testUser"
//    val db = Firebase.firestore
//    val userRef = db.collection("users").document(userId)
//
//    userRef.get()
//        .addOnSuccessListener { document ->
//            val userProgress = document.toObject(UserProgress::class.java) ?: UserProgress()
//            onProgressLoaded(userProgress)
//            Log.d("StatisticsSection", "User progress loaded successfully: $userProgress")
//        }
//        .addOnFailureListener { e ->
//            Log.e("StatisticsSection", "Error loading user progress", e)
//        }
//}




//private fun loadLessonTitles(lessonIds: List<String>, onTitlesLoaded: (Map<String, String>) -> Unit) {
//    if (lessonIds.isEmpty()) {
//        onTitlesLoaded(emptyMap())
//        return
//    }
//    val db = Firebase.firestore
//    val lessonRef = db.collection("lessons")
//    val lessonTitles = mutableMapOf<String, String>()
//
//    lessonRef.whereIn(FieldPath.documentId(), lessonIds).get()
//        .addOnSuccessListener { querySnapshot ->
//            for (document in querySnapshot.documents){
//                val lessonId = document.id
//                val lessonTitle = document.getString("lessonTitle")
//                if (lessonTitle != null) {
//                    lessonTitles[lessonId] = lessonTitle
//                }
//            }
//            onTitlesLoaded(lessonTitles)
//            Log.d("ProgressSection", "Lessons titles loaded successfully: $lessonTitles")
//
//        }.addOnFailureListener { e ->
//            Log.e("ProgressSection", "Error loading lessons", e)
//        }
//}

private fun loadTotalSections(onTotalLoaded: (Int) -> Unit) {
    val db = Firebase.firestore
    db.collection("sections")
        .get()
        .addOnSuccessListener { querySnapshot ->
            onTotalLoaded(querySnapshot.size())
            Log.d("StatisticsSection", "Total sections loaded successfully: ${querySnapshot.size()}")
        }
        .addOnFailureListener { e ->
            Log.e("StatisticsSection", "Error loading total sections", e)
        }
}
private fun registerUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = task.result.user
                user?.let {
                    val userDocRef = db.collection("users").document(it.uid)
                    userDocRef.set(mapOf(
                        "email" to email,
                    )).addOnCompleteListener {
                        if(it.isSuccessful){
                            onComplete(true, null)
                        } else {
                            onComplete(false, it.exception?.message ?: "Failed to add user data" )
                        }
                    }
                }

            } else {
                val errorMessage = task.exception?.message ?: "Ошибка регистрации"
                onComplete(false, errorMessage)
            }
        }
}

private fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
    val auth = Firebase.auth
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("ProfilePage", "User login successful")
                onComplete(true, null)
            } else {
                val errorMessage = task.exception?.message ?: "Ошибка входа"
                Log.e("ProfilePage", "User login failed: $errorMessage")
                onComplete(false, errorMessage)
            }
        }
}
private fun signOutUser(navController: NavController){
    val auth = Firebase.auth
    auth.signOut()
    navController.navigate("home"){
        popUpTo(0)
    }
}

private fun updateUserData(
    name: String,
    email: String,
    updateName: Boolean,
    updateEmail: Boolean,
    onComplete: (Boolean, String?) -> Unit
) {
    val auth = Firebase.auth
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
                                    // ... (rest of the updateUserData function)
                                }
                            } else {
                                // ... (handling the case without updating the name)
                            }
                        } else {
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
                                // ... (Update Firestore)
                            } else {
                                onComplete(false, "Ошибка при обновлении имени: ${taskName.exception?.message}")
                            }
                        }
                    } else {
                        onComplete(true, null)
                    }
                }
            } else {
                onComplete(false, "Данный email уже используется")
            }
        }
    }
}

private fun isEmailUnique(email: String, onResult: (Boolean) -> Unit) {
        val db = Firebase.firestore
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    val querySnapshot = task.result
                    onResult(querySnapshot.isEmpty)
                } else {
                    onResult(false)
                }
            }
    }

@Preview(showBackground = true)
@Composable
fun ProfilePagePreview() {
    MyTheme {
        val navController = rememberNavController()
        ProfilePage(navController)
    }
}

