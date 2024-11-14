package com.example.slngify_kp.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.slngify_kp.R
import com.example.slngify_kp.ui.theme.MyTheme

class PracticeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "sectionsList") {
                    composable("sectionsList") { SectionsScreen(navController) }
                    composable("sectionDetail/{sectionTitle}") { backStackEntry ->
                        val sectionTitle = backStackEntry.arguments?.getString("sectionTitle")
                        if (sectionTitle != null) {
                            LessonDetailScreen(navController, sectionTitle)
                        } else {
                            Text("Ошибка: заголовок задания не найден")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionsScreen(navController: NavController) {
    val sections = listOf("Сленг в музыке", "Сленг в фильмах", "Сленг в социальных сетях")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Разделы практики") },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(sections) { section ->
                    SectionItem(section, onClick = {
                        navController.navigate("sectionDetail/${section}")
                    })
                }
            }
        }
    )
}

@Composable
fun SectionItem(sectionTitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            text = sectionTitle,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(navController: NavController, sectionTitle: String) {
    val sectionQuestions = when (sectionTitle) {
        "Сленг в музыке" -> listOf(
            Question(
                text = "Что означает 'to drop the mic'?",
                options = listOf("Петь громко", "Покинуть сцену", "Завершить выступление", "Начать рэп-баттл"),
                correctAnswer = "Завершить выступление"
            )
        )
        "Сленг в фильмах" -> listOf(
            Question(
                text = "Что значит 'blockbuster'?",
                options = listOf("Популярный фильм", "Фильм ужасов", "Чёрно-белый фильм", "Документальный фильм"),
                correctAnswer = "Популярный фильм"
            )
        )
        "Сленг в социальных сетях" -> listOf(
            Question(
                text = "Что означает 'FOMO'?",
                options = listOf("Страх пропустить что-то интересное", "Знаменитость", "Мем", "Хештег"),
                correctAnswer = "Страх пропустить что-то интересное"
            )
        )
        else -> emptyList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sectionTitle) },
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
            ) {
                sectionQuestions.forEach { question ->
                    QuestionItem(question)
                }
            }
        }
    )
}

@Composable
fun QuestionItem(question: Question) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }

    Column {
        Text(question.text, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        question.options.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (selectedOption == option),
                        onClick = { selectedOption = option }
                    )
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedOption == option),
                    onClick = { selectedOption = option }
                )
                Text(option, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { showResult = true }) {
            Text("Проверить ответ")
        }
        if (showResult) {
            val message = if (selectedOption == question.correctAnswer) {
                "Правильно!"
            } else {
                "Неправильно. Правильный ответ: ${question.correctAnswer}"
            }
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

data class Question(
    val text: String,
    val options: List<String>,
    val correctAnswer: String
)

@Preview(showBackground = true)
@Composable
fun SectionsScreenPreview() {
    MyTheme {
        SectionsScreen(rememberNavController())
    }
}