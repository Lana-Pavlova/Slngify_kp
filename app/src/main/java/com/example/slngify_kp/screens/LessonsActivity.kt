package com.example.slngify_kp.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import android.net.Uri

class LessonsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "lessonsList") {
                    composable("lessonsList") { LessonsScreen(navController) }
                    composable("lessonDetail/{lessonTitle}") { backStackEntry ->
                        val lessonTitle = backStackEntry.arguments?.getString("lessonTitle")
                        if (lessonTitle != null) {
                            LessonDetailScreen(navController, lessonTitle)
                        } else {
                            Text("Ошибка: заголовок урока не найден")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(navController: NavController) {
    val lessons = listOf(
        "Урок 1: Основы сленга",
        "Урок 2: Современные фразы",
        "Урок 3: Жаргон в популярных медиа"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Изучение сленга") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.back), // Убедитесь, что иконка существует
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
                items(lessons) { lesson ->
                    LessonItem(lesson, onClick = {
                        navController.navigate("lessonDetail/${Uri.encode(lesson)}")
                    })
                }
            }
        }
    )
}

@Composable
fun LessonItem(lessonTitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Text(
            text = lessonTitle,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonDetailScreen(navController: NavController, lessonTitle: String) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lessonTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.back), // Убедитесь, что иконка существует, иначе используйте Icons.Default.ArrowBack
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
                Text(
                    text = "Добро пожаловать в $lessonTitle!",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Этот урок познакомит вас с современными выражениями и фразами, которые часто встречаются в повседневной речи.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Здесь можно добавить дополнительный контент, например, изображения, видео и т.д.
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LessonsScreenPreview() {
    MyTheme {
        LessonsScreen(rememberNavController())
    }
}