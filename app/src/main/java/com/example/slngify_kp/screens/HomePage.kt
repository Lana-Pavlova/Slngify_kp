package com.example.slngify_kp.screens

import android.os.Bundle
import com.example.slngify_kp.ui.theme.MyTheme
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.slngify_kp.R
import com.example.slngify_kp.WordOfTheDayWidget
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.CircularProgressIndicator
import com.google.firebase.firestore.AggregateSource

data class WordOfTheDay(
    val word: String = "",
    val definition: String = "",
    val examples: String = "",
    val translation: String = "",
    val addedDate: Long = 0
)

class HomePageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                NavigationComponent()
            }
        }
    }
}


@Composable
fun NavigationComponent() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomePageScreen(navController) }
        composable("lessonsList") { LessonsScreen(navController) }
        composable("sectionsList") { SectionsScreen(navController) }
        composable("dictionaryPage") { DictionaryScreen(navController) }
        composable("profilePage") { ProfilePage(navController) }
        composable("lessonDetail/{lessonTitle}") { backStackEntry ->
            val lessonTitle = backStackEntry.arguments?.getString("lessonTitle")
            if (lessonTitle != null) {
                LessonDetailScreen(navController, lessonTitle)
            } else {
                Text("Ошибка: заголовок урока не найден")
            }
        }
        composable("sectionDetail/{sectionTitle}") { backStackEntry ->
            val sectionTitle = backStackEntry.arguments?.getString("sectionTitle")
            if (sectionTitle != null) {
                SectionDetailScreen(navController, sectionTitle)
            } else {
                Text("Ошибка: заголовок раздела не найден")
            }
        }
    }
}
suspend fun fetchRandomWordOfTheDay(): WordOfTheDay? {
    return try {
        val totalDocs = Firebase.firestore.collection("wordOfTheDay")
            .count()
            .get(AggregateSource.SERVER)
            .await()
            .count

        if (totalDocs == 0L) return null // Handle empty collection

        // Efficient random selection:
        val randomIndex = (0 until totalDocs).random()

        val querySnapshot = Firebase.firestore.collection("wordOfTheDay")
            .orderBy("word") // order by any field
            .startAt(randomIndex.toInt()) // start at a random position
            .limit(1)
            .get()
            .await()

        querySnapshot.documents.firstOrNull()?.toObject(WordOfTheDay::class.java)
    } catch (e: Exception) {
        println("Error fetching random word: ${e.message}")
        null
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageScreen(navController: NavController) {
    var wordOfTheDay by remember { mutableStateOf<WordOfTheDay?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            wordOfTheDay = fetchRandomWordOfTheDay()
        } catch (e: Exception) {
            error = e.message ?: "An unexpected error occurred."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slangify") },
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
                if (isLoading) {
                    CircularProgressIndicator()
                } else if (error != null) {
                    Text("Error: $error")
                } else {
                    wordOfTheDay?.let { word ->
                        Text(
                            text = "✨ Слово дня: ${word.word} ✨\n${word.definition}\nПеревод: ${word.translation}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp)
                        )
                        if (word.examples.isNotBlank()) {
                            Text("Примеры: ${word.examples}", style = MaterialTheme.typography.bodyMedium)
                        }
                    } ?: run {
                        Text("No word found.")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MainMenuButton("Словарь", R.drawable.dictionary) {
                        navController.navigate("dictionaryPage") // Переход на страницу словаря
                    }
                    MainMenuButton("Уроки", R.drawable.lessons) {
                    navController.navigate("lessonsList") // Переход на страницу уроков
                }
                    MainMenuButton("Практика", R.drawable.practice){
                        navController.navigate("sectionsList") // Переход на страницу заданий
                }
                    MainMenuButton("Личный кабинет", R.drawable.profile) {
                        navController.navigate("profilePage") // Переход на страницу профиля
                    }
                }
            }
        }
    )
}

@Composable
fun MainMenuButton(text: String, iconRes: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            modifier = Modifier.size(64.dp)
        )
        Text(text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    MyTheme {
        WordOfTheDayWidget()
        val navController = rememberNavController()
        HomePageScreen(navController)
    }
}