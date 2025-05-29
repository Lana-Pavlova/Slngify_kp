package com.example.slngify_kp.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.slngify_kp.ui.theme.MyTheme
import com.example.slngify_kp.widget.WordOfTheDayWidget
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class WordOfTheDay(
    val definition : String? = null,
    val examples : String? = null,
    val translation : String? = null,
    val word : String? = null
)
class HomePageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyTheme {
                NavigationComponent()
            }
        }
        WordOfTheDayWidget.updateWidget(this)

    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationComponent() {
    val navController = rememberNavController()
    var selectedItem = remember { mutableStateOf("") }

    val shouldShowBottomBar = remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar.value) {
                BottomNavigationBar(
                    navController = navController,
                    selectedItem = selectedItem.value,
                    onItemSelected = { route -> selectedItem.value = route }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                shouldShowBottomBar.value = false
                selectedItem.value = ""
                HomePageScreen(navController)
            }
            composable("lessonsList") {
                shouldShowBottomBar.value = true
                selectedItem.value = ""
                LessonsScreen(navController)
            }
            composable("sectionsList") {
                shouldShowBottomBar.value = true
                selectedItem.value = ""
                SectionsScreen(navController)
            }
            composable("dictionaryPage") {
                shouldShowBottomBar.value = true
                selectedItem.value = ""
                DictionaryScreen(navController)
            }
            composable("profilePage") {
                shouldShowBottomBar.value = false
                selectedItem.value = ""
                ProfilePage(navController = navController)
            }
            composable("lessonDetail/{lessonDocumentId}") { backStackEntry ->
                shouldShowBottomBar.value = false
                selectedItem.value = ""
                val lessonDocumentId = backStackEntry.arguments?.getString("lessonDocumentId") ?: ""
                LessonScreen(lessonDocumentId = lessonDocumentId, navController = navController)
            }
            composable("sectionDetail/{sectionId}") { backStackEntry ->
                shouldShowBottomBar.value = false
                selectedItem.value = ""
                val sectionId = backStackEntry.arguments?.getString("sectionId") ?: ""
                SectionDetailScreen(sectionId = sectionId, navController = navController)
            }
            composable("imageViewer/{imageUrl}") { backStackEntry ->
                shouldShowBottomBar.value = false
                selectedItem.value = ""
                val imageUrl = backStackEntry.arguments?.getString("imageUrl") ?: ""
                ImageViewerScreen(imageUrl = imageUrl)
            }
        }
    }
}

suspend fun fetchRandomWordOfTheDay(): WordOfTheDay? {
    return try {
        val querySnapshot = Firebase.firestore.collection("wordOfTheDay")
            .get()
            .await()

        if (querySnapshot.isEmpty) return null

        val randomIndex = (0 until querySnapshot.size()).random()
        querySnapshot.documents[randomIndex].toObject(WordOfTheDay::class.java)
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
    var isWordAvailable by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            wordOfTheDay = fetchRandomWordOfTheDay()
            isWordAvailable = wordOfTheDay != null
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
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                        CircularProgressIndicator()
                    }
                }
                else if (error != null) {
                    Text(text = "Error: $error", style = MaterialTheme.typography.bodyMedium)
                }
                else if (!isWordAvailable) {
                    Text("No word found.")
                }
                else {
                    wordOfTheDay?.let { word ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
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
                        ) {
                            if (!word.word.isNullOrBlank()) {
                                Text(
                                    text = "✨ Слово дня: ${word.word} ✨",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (!word.definition.isNullOrBlank()) {
                                Text(
                                    text = "${word.definition}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (!word.translation.isNullOrBlank()) {
                                Text(
                                    text = "Перевод: ${word.translation}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (!word.examples.isNullOrBlank()) {
                                Text(
                                    text = "Примеры: ${word.examples}",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MainMenuButton(
                        text = "Словарь",
                        iconRes = R.drawable.dictionary,
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(8.dp)
                    ) {
                        navController.navigate("dictionaryPage")
                    }
                    MainMenuButton(
                        text = "Уроки",
                        iconRes = R.drawable.lessons,
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(8.dp)
                    ) {
                        navController.navigate("lessonsList")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MainMenuButton(
                        text = "Практика",
                        iconRes = R.drawable.practice,
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(8.dp)
                    ) {
                        navController.navigate("sectionsList")
                    }
                    MainMenuButton(
                        text = "Личный кабинет",
                        iconRes = R.drawable.profile,
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(8.dp)
                    ) {
                        navController.navigate("profilePage")
                    }
                }
            }
        }
    )
}

@Composable
fun MainMenuButton(
    text: String,
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
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
        val navController = rememberNavController()
        HomePageScreen(navController)
    }
}