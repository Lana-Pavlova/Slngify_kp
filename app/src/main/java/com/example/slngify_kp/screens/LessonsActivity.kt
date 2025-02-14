package com.example.slngify_kp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.slngify_kp.R
import com.example.slngify_kp.ui.theme.MyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Модели данных
data class LessonSection(
    val content: String?,
    val imageUrl: String?
)
data class Lesson(
    val lessonTitle: String,
    val practiceSectionId: String,
    val previousLessonId: String?,
    val id: String
)
data class LessonListItem(
    val id: String,
    val lessonTitle: String,
    val sectionCount: Int,
    val previousLessonId: String? = null,
    val isCompleted: Boolean = false
)
enum class LessonFilter {
    ALL, COMPLETED, NOT_COMPLETED
}

class LessonsViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private val _lessonList = MutableStateFlow<List<LessonListItem>>(emptyList())
    val lessonList: StateFlow<List<LessonListItem>> = _lessonList

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _userProgress = MutableStateFlow(UserProgress())
    val userProgress: StateFlow<UserProgress> = _userProgress

    private val _currentFilter = MutableStateFlow(LessonFilter.ALL)
    val currentFilter: StateFlow<LessonFilter> = _currentFilter

    init {
        loadLessons()
        auth.currentUser?.uid?.let { loadUserProgress(it) }
    }

    private fun loadLessons() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val documents = firestore.collection("lessons").get().await()
                val lessons = mutableListOf<LessonListItem>()
                for (document in documents) {
                    val sectionA = document.get("sectionA") as? Map<*, *>
                    val lessonTitle = sectionA?.get("content") as? String ?: "No title"
                    val sectionIds = document.get("sectionIds") as? List<String> ?: emptyList()
                    val sectionCount = sectionIds.size
                    val previousLessonId = document.getString("previousLessonId")
                    val lessonId = document.getString("lessonId") ?: ""
                    val isCompleted = _userProgress.value.completedLessons.contains(lessonId)

                    lessons.add(
                        LessonListItem(
                            id = lessonId,
                            lessonTitle = lessonTitle,
                            sectionCount = sectionCount,
                            previousLessonId = previousLessonId,
                            isCompleted = isCompleted
                        )
                    )
                }
                _lessonList.value = filterLessons(lessons)
            } catch (e: Exception) {
                Log.e("LessonsViewModel", "Error getting lessons", e)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun filterLessons(lessons: List<LessonListItem>): List<LessonListItem> {
        return when (_currentFilter.value) {
            LessonFilter.ALL -> lessons
            LessonFilter.COMPLETED -> lessons.filter { it.isCompleted }
            LessonFilter.NOT_COMPLETED -> lessons.filter { !it.isCompleted }
        }
    }

    private fun loadUserProgress(userId: String) {
        viewModelScope.launch {
            try {
                val userDocument = firestore.collection("users").document(userId).get().await()
                val completedIds = userDocument.get("completedLessonIds") as? List<String> ?: emptyList()
                val completedSections = userDocument.get("completedSections") as? List<String> ?: emptyList()
                _userProgress.value = UserProgress(completedLessons = completedIds, completedSections = completedSections)
                loadLessons()
                Log.d("LessonsViewModel", "user progress loaded completed sections: $completedSections")
            } catch (e: Exception) {
                Log.e("LessonsViewModel", "Error load user progress", e)
            }
        }
    }

    fun setFilter(filter: LessonFilter) {
        _currentFilter.value = filter
        loadLessons()
    }

    fun markLessonCompleted(lessonId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val currentCompleted = _userProgress.value.completedLessons.toMutableList()

            if (!currentCompleted.contains(lessonId)) {
                currentCompleted.add(lessonId)
            }

            try {
                firestore.collection("users").document(userId)
                    .update("completedLessonIds", currentCompleted.toList()).await()
                _userProgress.value = UserProgress(completedLessons = currentCompleted)
                loadLessons()
            } catch (e: Exception) {
                Log.e("LessonsViewModel", "Error mark lesson completed", e)
            }
        }
    }
}
class LessonViewModel(private val lessonDocumentId: String) : ViewModel() {

    private val firestore = Firebase.firestore

    private val _lesson = MutableStateFlow<Lesson?>(null)
    val lesson: StateFlow<Lesson?> = _lesson

    private val _sections = MutableStateFlow<List<Pair<String, LessonSection>>>(emptyList())
    val sections: StateFlow<List<Pair<String, LessonSection>>> = _sections

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error


    init {
        loadLesson()
    }

    private fun loadLesson() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                Log.d("LessonViewModel", "Загрузка урока с ID: $lessonDocumentId")
                val document = firestore.collection("lessons").document(lessonDocumentId).get().await()
                val sectionA = document.get("sectionA") as? Map<*, *>
                val lessonTitle = sectionA?.get("content") as? String ?: "Урок"
                val practiceSectionId = document.getString("practiceSectionId") ?: ""
                val previousLessonId = document.getString("previousLessonId")
                val lessonId = document.getString("lessonId") ?: ""
                Log.d("LessonViewModel", "Данные урока: $lessonTitle")

                _lesson.value = Lesson(
                    lessonTitle = lessonTitle,
                    practiceSectionId = practiceSectionId,
                    previousLessonId = previousLessonId,
                    id = lessonId
                )
                loadSections(document)
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки урока: ${e.message}"
                Log.e("LessonViewModel", "Error getting lesson", e)
            } finally {
                _loading.value = false
            }
        }
    }
    private fun loadSections(document: com.google.firebase.firestore.DocumentSnapshot) {
        viewModelScope.launch {
            val lessonSections = mutableListOf<Pair<String, LessonSection>>()
            _error.value = null
            try {
                val sectionA = document.get("sectionA") as? Map<*, *>
                if (sectionA != null) {
                    val contentA = sectionA["content"] as? String
                    val imageUrlA = sectionA["imageUrl"] as? String
                    lessonSections.add(Pair("sectionA", LessonSection(contentA, imageUrlA)))
                    Log.d("LessonViewModel", "Секция sectionA: content = $contentA, image = $imageUrlA")
                }
                var sectionIndex = 'B' // Начинаем с sectionB
                while (true) {
                    val sectionKey = "section$sectionIndex"
                    val sectionData = document.get(sectionKey) as? Map<*, *>
                    if (sectionData == null) {
                        break // Если sectionKey не найден, заканчиваем цикл
                    }
                    val content = sectionData["content"] as? String
                    val imageUrl = sectionData["imageUrl"] as? String
                    lessonSections.add(Pair(sectionKey, LessonSection(content, imageUrl)))
                    Log.d("LessonViewModel", "Секция $sectionKey: content = $content, image = $imageUrl")
                    sectionIndex++ // Переходим к следующей секции
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки секции: ${e.message}"
                Log.e("LessonViewModel", "Error getting section", e)
            }
            _sections.value = lessonSections
            Log.d("LessonViewModel", "Загружено секций: $lessonSections, общее число: ${_sections.value.size}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonsScreen(navController: NavHostController) {
    val viewModel: LessonsViewModel = viewModel()
    val lessonList by viewModel.lessonList.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val userProgress by viewModel.userProgress.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()

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
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading...")
            }
        } else {
            Column(modifier = Modifier.padding(paddingValues)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterButton(text = "Все", currentFilter = currentFilter, onClick = { viewModel.setFilter(LessonFilter.ALL) })
                    FilterButton(text = "Пройденные", currentFilter = currentFilter, onClick = { viewModel.setFilter(LessonFilter.COMPLETED) })
                    FilterButton(text = "Не пройденные", currentFilter = currentFilter, onClick = { viewModel.setFilter(LessonFilter.NOT_COMPLETED) })
                }
                LazyColumn {
                    items(lessonList) { lessonItem ->
                        LessonListItemView(lessonItem = lessonItem, navController = navController, userProgress = userProgress)
                    }
                }
            }

        }
    }
}

@Composable
fun FilterButton(text: String, currentFilter: LessonFilter, onClick: () -> Unit) {
    Button(onClick = onClick,
        colors = if (currentFilter.name == text.uppercase())
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        else
            ButtonDefaults.buttonColors()) {
        Text(text = text)
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(lessonDocumentId: String, navController: NavHostController) {
    val viewModel: LessonViewModel = viewModel { LessonViewModel(lessonDocumentId) }
    val lessonsViewModel: LessonsViewModel = viewModel()

    val lesson by viewModel.lesson.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val scrollState = rememberScrollState()
    val error by viewModel.error.collectAsState()
    val practiceSectionId = lesson?.practiceSectionId


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lesson?.lessonTitle ?: "Урок") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        if (loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ошибка: $error", color = Color.Red)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                sections.forEach { (sectionKey, lessonSection) ->
                    lessonSection.content?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 16.sp
                        )
                    }
                    lessonSection.imageUrl?.let { imageUrl ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            var imageLoading by remember { mutableStateOf(true) }
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .size(Size.ORIGINAL)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        navController.navigate("imageViewer/$imageUrl")
                                    },
                                onSuccess = { imageLoading = false },
                                onError = {
                                    Log.d("ImageViewerScreen", "Error loading image: $imageUrl")
                                    imageLoading = false // Укажем, что загрузка завершена с ошибкой
                                }
                            )
                            if (imageLoading) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                if(practiceSectionId?.isNotEmpty() == true){
                    Button(onClick = {
                        navController.navigate("sectionDetail/$practiceSectionId?lessonId=${lesson?.id}")
                    }) {
                        Text("Перейти к практике")
                    }
                }

                Button(onClick = {
                    lesson?.id?.let { lessonId ->
                        lessonsViewModel.markLessonCompleted(lessonId)
                        navController.popBackStack()
                    }

                }) {
                    Text("Завершить урок")
                }
            }
        }
    }
}

@Composable
fun LessonListItemView(lessonItem: LessonListItem, navController: NavHostController, userProgress: UserProgress) {
    val isUnlocked = lessonItem.previousLessonId == null || userProgress.completedLessons.contains(lessonItem.previousLessonId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        enabled = isUnlocked,
        onClick = {
            if (isUnlocked) {
                navController.navigate("lessonDetail/${lessonItem.id}")
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = lessonItem.lessonTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )
            }
            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Заблокировано",
                    modifier = Modifier
                        .padding(16.dp)
                )
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

