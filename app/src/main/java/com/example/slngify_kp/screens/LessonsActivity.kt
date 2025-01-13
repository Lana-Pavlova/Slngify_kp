package com.example.slngify_kp.screens

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.slngify_kp.R
import com.example.slngify_kp.ui.theme.MyTheme
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
data class LessonSection(
    val content: String?,
    val imageUrl: String?
)
data class Lesson(
    val lessonTitle: String, // Заголовок из sectionA
    val sections: List<Pair<String, LessonSection>>,
    val sectionIds : List<String>?,
    val practiceSectionId: String = ""

)
data class LessonListItem(
    val id: String,
    val lessonTitle: String, // Используем content из sectionA
    val sectionCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(navController: NavHostController) {
    var lessonList by remember { mutableStateOf<List<LessonListItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        db.collection("lessons")
            .get()
            .addOnSuccessListener { documents ->
                val lessons = mutableListOf<LessonListItem>()
                for (document in documents) {
                    val sectionA = document.get("sectionA") as? Map<*, *>
                    val lessonTitle = sectionA?.get("content") as? String ?: "No title"
                    val sectionIds = document.get("sectionIds") as? List<String> ?: emptyList()
                    val sectionCount = sectionIds.size
                    lessons.add(
                        LessonListItem(
                            id = document.id,
                            lessonTitle = lessonTitle,
                            sectionCount = sectionCount
                        )
                    )
                }
                lessonList = lessons.toList()
            }
            .addOnFailureListener { e ->
                Log.e("LessonsScreen", "Error getting lessons", e)
            }
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(lessonList) { lessonItem ->
            LessonListItemView(lessonItem = lessonItem, navController = navController)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Список уроков") },
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
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(lessonList) { lessonItem ->
                LessonListItemView(lessonItem = lessonItem, navController = navController)
            }
        }
    }
}
@Composable
fun LessonListItemView(lessonItem: LessonListItem, navController: NavHostController){
    Card(modifier = Modifier.padding(8.dp)
        .clickable {
            navController.navigate("lessonDetail/${lessonItem.id}")
        }) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Text(text = "Lesson: ${lessonItem.lessonTitle}")
            Text(text = "Sections: ${lessonItem.sectionCount}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(lessonDocumentId: String, navController: NavHostController) {
    var lesson by remember { mutableStateOf<Lesson?>(null) }
    val scrollState = rememberScrollState()
    var lessonTitle by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(lessonDocumentId) {
        val db = Firebase.firestore
        db.collection("lessons").document(lessonDocumentId)
            .get()
            .addOnSuccessListener { document ->
                val sectionA = document.get("sectionA") as? Map<*, *>
                val lessonTitle = sectionA?.get("content") as? String ?: "Урок"
                val practiceSectionId = document.getString("practiceSectionId") ?: ""
                Log.d("LessonScreen", "lessonTitle: $lessonTitle")
                Log.d("LessonScreen", "practiceSectionId: $practiceSectionId") // <-- Added log

                val lessonSections = mutableListOf<Pair<String, LessonSection>>()
                val sectionIds = document.get("sectionIds") as? List<String> ?: emptyList()
                Log.d("LessonScreen", "sectionIds: $sectionIds")
                if (sectionIds.isNotEmpty()) {
                    for (sectionId in sectionIds) {
                        val sectionData = document.get(sectionId) as? Map<*, *>
                        val content = sectionData?.get("content") as? String
                        val imageUrl = sectionData?.get("imageUrl") as? String
                        lessonSections.add(Pair(sectionId, LessonSection(content, imageUrl)))
                    }
                } else {
                    document.data?.forEach { (key, value) ->
                        if (key.startsWith("section") && key != "sectionA") {
                            val sectionData = value as? Map<*, *>
                            val content = sectionData?.get("content") as? String
                            val imageUrl = sectionData?.get("imageUrl") as? String
                            lessonSections.add(Pair(key, LessonSection(content, imageUrl)))
                        }
                    }
                }

                lesson = Lesson(
                    lessonTitle = lessonTitle ?: "Урок",
                    sections = lessonSections,
                    sectionIds = sectionIds,
                    practiceSectionId = practiceSectionId
                )
            }
            .addOnFailureListener { e ->
                Log.e("LessonScreen", "Error getting lesson", e)
            }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lessonTitle ?: "Урок") },
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)

        ) {

            lesson?.let { lessonData ->
                Text(
                    text = lessonData.lessonTitle,
                    modifier = Modifier.padding(bottom = 16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                lessonData.sections.forEach { (_, section) ->
                    section.content?.let {
                        Text(
                            text = it,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    section.imageUrl?.let {
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .padding(horizontal = 8.dp)
                                .height(200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                if (!lessonData.practiceSectionId.isNullOrBlank()) {
                    Button(
                        onClick = {
                            if (!lessonData.practiceSectionId.isNullOrBlank()){
                                navController.navigate("sectionDetail/${lessonData.practiceSectionId}")
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = "Перейти к практике")
                    }
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun LessonsScreenPreview() {
    MyTheme {
        LessonsScreen(rememberNavController())
    }
}