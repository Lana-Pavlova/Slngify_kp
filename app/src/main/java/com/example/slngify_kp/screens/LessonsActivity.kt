package com.example.slngify_kp.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.slngify_kp.viewmodel.AchievementData
import com.example.slngify_kp.viewmodel.UserProgress
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.material3.*
import com.example.slngify_kp.viewmodel.YouTubeWebView


// Модели данных
data class LessonSection(
    val title: String? = null,
    val content: String? = null,
    val imageUrl: String? = null,
    val order: Int? = null,
    val videoUrl: String? = null,
    val tableData: List<TableRow>? = null,

    )
data class TableRow(
    val englishText: String?,
    val translation: String?
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
    val lessonList: StateFlow<List<LessonListItem>> = _lessonList.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _userProgress = MutableStateFlow(UserProgress())
    val userProgress: StateFlow<UserProgress> = _userProgress.asStateFlow()

    private val _currentFilter = MutableStateFlow(LessonFilter.ALL)
    val currentFilter: StateFlow<LessonFilter> = _currentFilter.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        Log.d("LessonsViewModel", "loadData called")
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            Log.d("LessonsViewModel", "UserId: $userId")
            if (userId == null) {
                Log.e("LessonsViewModel", "UserId is null, returning")
                return@launch
            }

            _loading.value = true

            try {
                val userProgress = loadUserProgress(userId)
                _userProgress.value = userProgress

                loadLessonsFlow(completedLessons = userProgress.completedLessons)
                    .collect { lessons ->
                        _lessonList.value = filterLessons(lessons, userProgress.completedLessons) // Вызываем filterLessons
                    }
            } catch (e: Exception) {
                Log.e("LessonsViewModel", "Error loading data", e)
            } finally {
                _loading.value = false
                Log.d("LessonsViewModel", "loadData finished")
            }
        }
    }

    private suspend fun loadUserProgress(userId: String): UserProgress {
        Log.d("LessonsViewModel", "loadUserProgress called")
        return try {
            val userDocument = firestore.collection("users").document(userId).get().await()
            val completedLessons = userDocument.get("completedLessonIds") as? List<String> ?: emptyList()
            val completedTasks = userDocument.get("completedTaskIds") as? List<String> ?: emptyList()
            val achievementIds = userDocument.get("achievementIds") as? List<String> ?: emptyList()

            val achievements = mutableListOf<AchievementData>()
            for (achievementId in achievementIds) {
                val achievementDocument = firestore.collection("achievements").document(achievementId).get().await()
                if (achievementDocument.exists()) {
                    val name = achievementDocument.getString("name") ?: ""
                    val icon = achievementDocument.getString("icon") ?: ""
                    achievements.add(AchievementData(name, icon))
                }
            }
            Log.d("LessonsViewModel", "loadUserProgress finished")
            UserProgress(completedLessons = completedLessons, completedTasks = completedTasks, achievements = achievements)
        } catch (e: Exception) {
            Log.e("LessonsViewModel", "Error getting user progress", e)
            UserProgress()
        }
    }

    private fun loadLessonsFlow(completedLessons: List<String>): kotlinx.coroutines.flow.Flow<List<LessonListItem>> = flow {
        Log.d("LessonsViewModel", "loadLessonsFlow called")
        _loading.value = true
        try {
            val documents = firestore.collection("lessons").get().await()
            Log.d("LessonsViewModel", "Number of lessons found: ${documents.size()}")
            val lessons = mutableListOf<LessonListItem>()

            for (document in documents) {
                val lessonId = document.id
                val lessonData = document.data ?: emptyMap<String, Any>()

                var lessonTitle = "No title"
                if (lessonData.containsKey("sectionA") && lessonData["sectionA"] is Map<*, *>) {
                    lessonTitle = (lessonData["sectionA"] as Map<*, *>).get("title") as? String ?: "No title"
                }

                val sectionCount = lessonData.count { it.key.startsWith("section") } // Подсчет секций
                val previousLessonId = document.getString("previousLessonId")

                val isCompleted = completedLessons.contains(lessonId)

                lessons.add(
                    LessonListItem(
                        id = lessonId,
                        lessonTitle = lessonTitle,
                        sectionCount = sectionCount,
                        previousLessonId = previousLessonId,
                        isCompleted = isCompleted
                    )
                )
                Log.d("LessonsViewModel", "Lesson added: $lessonId, title: $lessonTitle")
            }
            emit(lessons)
            Log.d("LessonsViewModel", "Lessons emitted: ${lessons.size}")
        } catch (e: Exception) {
            Log.e("LessonsViewModel", "Error getting lessons", e)
            emit(emptyList())
        } finally {
            _loading.value = false
            Log.d("LessonsViewModel", "loadLessonsFlow finished")
        }
    }

    private fun filterLessons(lessons: List<LessonListItem>, completedLessons: List<String>): List<LessonListItem> {
        return when (_currentFilter.value) {
            LessonFilter.ALL -> lessons
            LessonFilter.COMPLETED -> lessons.filter { completedLessons.contains(it.id) }
            LessonFilter.NOT_COMPLETED -> lessons.filter { !completedLessons.contains(it.id) }
        }
    }

    fun setFilter(filter: LessonFilter) {
        _currentFilter.value = filter
        loadData()
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
                val lessonTitle = document.getString("title") ?: "Урок"
                val practiceSectionId = document.getString("practiceSectionId") ?: ""
                val previousLessonId = document.getString("previousLessonId")
                val lessonId = document.id
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
                val data = document.data
                if (data != null) {
                    for ((key, value) in data) {
                        if (key.startsWith("section")) {
                            val sectionData = value as? Map<*, *>
                            if (sectionData != null) {
                                val content = sectionData["content"] as? String
                                val imageUrl = sectionData["imageUrl"] as? String
                                val videoUrl = sectionData["videoUrl"] as? String
                                val title = sectionData["title"] as? String
                                val order = (sectionData["order"] as? Number)?.toInt()

                                // Table Data
                                val tableData = sectionData["tableData"] as? List<Map<String, String>>
                                val tableDataList = if (tableData != null) {
                                    val list = mutableListOf<TableRow>()
                                    tableData.forEach { row ->
                                        val englishText = row["englishText"] as? String
                                        val translation = row["translation"] as? String
                                        list.add(TableRow(englishText, translation))
                                    }
                                    list
                                } else {
                                    null
                                }

                                lessonSections.add(
                                    Pair(
                                        key,
                                        LessonSection(
                                            title = title,
                                            content = content,
                                            imageUrl = imageUrl,
                                            videoUrl = videoUrl,
                                            tableData = tableDataList,
                                            order = order
                                        )
                                    )
                                )
                                Log.d(
                                    "LessonViewModel",
                                    "Секция $key: content = $content, image = $imageUrl, videoUrl = $videoUrl"
                                )
                            }
                        }
                    }
                    lessonSections.sortBy { it.second.order }
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки секции: ${e.message}"
                Log.e("LessonViewModel", "Error getting section", e)
            } finally {
                _sections.value = lessonSections
                Log.d(
                    "LessonViewModel",
                    "Загружено секций: $lessonSections, общее число: ${_sections.value.size}"
                )
            }
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

    Log.d("LessonsScreen", "LessonsScreen called")
    Log.d("LessonsScreen", "Loading: $loading, LessonList size: ${lessonList.size}")

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
            Log.d("LessonsScreen", "Loading indicator displayed")
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Loading...")
            }
        } else {
            Log.d("LessonsScreen", "Not loading, LessonList size: ${lessonList.size}")
            Column(modifier = Modifier.padding(paddingValues)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterButton(text = "Все", currentFilter = currentFilter, onClick = { viewModel.setFilter(LessonFilter.ALL) })
                    FilterButton(text = "Пройденные", currentFilter = currentFilter, onClick = { viewModel.setFilter(LessonFilter.COMPLETED) })
                    FilterButton(text = "Не пройденные", currentFilter = currentFilter, onClick = { viewModel.setFilter(LessonFilter.NOT_COMPLETED) })
                }
                if (lessonList.isEmpty()) {
                    Log.d("LessonsScreen", "No lessons to display")
                    Text("Нет уроков")
                } else {
                    Log.d("LessonsScreen", "Displaying lessons")
                    LazyColumn {
                        items(lessonList) { lessonItem ->
                            LessonListItemView(
                                lessonItem = lessonItem,
                                navController = navController,
                                userProgress = userProgress,
                                onLessonClick = {
                                    navController.navigate("lessonDetail/${lessonItem.id}")
                                }
                            )
                        }
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


@Composable
fun TableView(tableData: List<TableRow>?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // Отступы по краям таблицы
            .border(1.dp, Color.Gray) // Рамка вокруг таблицы
    ) {
        if (tableData != null) {
            Column(modifier = Modifier.padding(8.dp)) { // Отступы внутри таблицы
                // Заголовки таблицы
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Текст на английском", Modifier.weight(1f))
                    Text("Перевод", Modifier.weight(1f))
                }
                Divider()
                tableData.forEach { row ->
                    TableRowView(row = row)
                    Divider()
                }
            }
        } else {
            Text("Нет данных для отображения")
        }
    }
}
@Composable
fun TableRowView(row: TableRow) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(row.englishText ?: "", Modifier.weight(1f))
        Text(row.translation ?: "", Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(lessonDocumentId: String, navController: NavHostController) {
    val viewModel: LessonViewModel = viewModel { LessonViewModel(lessonDocumentId) }

    val lesson by viewModel.lesson.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val scrollState = rememberScrollState()
    val error by viewModel.error.collectAsState()
    val practiceSectionId = lesson?.practiceSectionId
    var youtubeError by remember { mutableStateOf<String?>(null) } //Состояние для ошибок YouTubeWebView

    val videoId = sections.firstOrNull { !it.second.videoUrl.isNullOrEmpty() }?.second?.videoUrl

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
                Text(
                    text = lesson?.lessonTitle ?: "Урок",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(16.dp)
                )
                if (!videoId.isNullOrEmpty()) {
                    YouTubeWebView(videoId = videoId, onError = { errorMessage ->
                        youtubeError = errorMessage
                    })
                    if (youtubeError != null) {
                        Text(text = "YouTube Error: ${youtubeError!!}", color = Color.Red)
                    }
                }

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
                                    imageLoading = false
                                }
                            )
                            if (imageLoading) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                    lessonSection.tableData?.let { tableData ->
                        TableView(tableData = tableData)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
                if (practiceSectionId?.isNotEmpty() == true) {
                    Button(onClick = {
                        navController.navigate("sectionDetail/$practiceSectionId?lessonId=${lesson?.id}")
                    }) {
                        Text("Перейти к практике")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonListItemView(lessonItem: LessonListItem, navController: NavHostController, userProgress: UserProgress, onLessonClick: () -> Unit) {
    val isUnlocked = lessonItem.previousLessonId == null || userProgress.completedLessons.contains(lessonItem.previousLessonId)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        onClick = {
            if (isUnlocked) {
                navController.navigate("lessonDetail/${lessonItem.id}")
            } else {
                scope.launch {
                    Toast.makeText(context, "Этот урок пока недоступен", Toast.LENGTH_SHORT).show()
                }
            }
        },
        border = if (!isUnlocked) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = lessonItem.lessonTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!isUnlocked) {  // Отображаем иконку замка, если урок *не* разблокирован
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Заблокировано",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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

