package com.example.slngify_kp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

data class Section(
    val sectionId: String = "",
    val lessonId: String = "",
    val sectionTitle: String = "",
    val sectionDescription: String = "",
    val questions: List<Question> = emptyList(),
    val previousSectionId: String? = null

)

data class Question(
    val type: String = "",
    val text: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val imageUrl: String = ""

)


class SectionsViewModel: ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid

    private val _sectionList = MutableStateFlow<List<Section>>(emptyList())
    val sectionList: StateFlow<List<Section>> = _sectionList

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _completedSectionIds = MutableStateFlow<Set<String>>(emptySet())
    val completedSectionIds : StateFlow<Set<String>> = _completedSectionIds
    init {
        loadSections()
        userId?.let { loadCompletedSectionIds(it) }
    }


    private fun loadSections() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = firestore.collection("sections").get().await()
                val sections = mutableListOf<Section>()
                for (document in result) {
                    val sectionId = document.id
                    val lessonId = document.getString("lessonId") ?: ""
                    val sectionTitle = document.getString("sectionTitle") ?: ""
                    val sectionDescription = document.getString("sectionDescription") ?: ""
                    val questionsData = document.get("questions") as? List<Map<*, *>> ?: emptyList()
                    val previousSectionId = document.getString("previousSectionId")


                    val questions = questionsData.map { questionData ->
                        Question(
                            type = questionData["type"] as? String ?: "",
                            text = questionData["text"] as? String ?: "",
                            options = questionData["options"] as? List<String> ?: emptyList(),
                            correctAnswer = questionData["correctAnswer"] as? String ?: "",
                            imageUrl = questionData["imageUrl"] as? String ?: ""
                        )
                    }
                    val isUnlocked = previousSectionId == null || _completedSectionIds.value.contains(previousSectionId)
                    sections.add(
                        Section(
                            sectionId,
                            lessonId,
                            sectionTitle,
                            sectionDescription,
                            questions,
                            previousSectionId = previousSectionId
                        )
                    )
                    Log.d("SectionsScreen", "sectionId: $sectionId")
                    Log.d("SectionsScreen", "sectionTitle: $sectionTitle")
                    Log.d("SectionsScreen", "lessonId: $lessonId")
                }
                _sectionList.value = sections
            } catch (exception: Exception) {
                Log.e("SectionsScreen", "Error getting sections", exception)
            } finally {
                _loading.value = false
            }
        }
    }
    private fun loadCompletedSectionIds(userId: String) {
        viewModelScope.launch {
            val userDocument = firestore.collection("users").document(userId).get().await()
            val completedIds = userDocument.get("completedSectionIds") as? List<String> ?: emptyList()
            _completedSectionIds.value = completedIds.toSet()
        }
    }
    fun markSectionCompleted(sectionId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val currentCompleted = _completedSectionIds.value.toMutableSet()

            if (!currentCompleted.contains(sectionId)) {
                currentCompleted.add(sectionId)
            }

            try {
                firestore.collection("users").document(userId)
                    .update("completedSectionIds", currentCompleted.toList()).await()
                _completedSectionIds.value = currentCompleted
            } catch (e: Exception) {
                Log.e("SectionsViewModel", "Error mark lesson completed", e)
            }
        }
    }
}

class SectionDetailViewModel(private val sectionId: String) : ViewModel() {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()


    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions

    private val _selectedAnswers = MutableStateFlow(mutableMapOf<Int, String>())
    val selectedAnswers: StateFlow<MutableMap<Int, String>> = _selectedAnswers

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex

    private val _showResult = MutableStateFlow(false)
    val showResult: StateFlow<Boolean> = _showResult

    private val _numberOfCorrectAnswers = MutableStateFlow(0)
    val numberOfCorrectAnswers: StateFlow<Int> = _numberOfCorrectAnswers

    private val _incorrectQuestions = MutableStateFlow<List<Int>>(emptyList())
    val incorrectQuestions: StateFlow<List<Int>> = _incorrectQuestions

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _lessonIdForUpdate = MutableStateFlow<String?>(null)
    val lessonIdForUpdate: StateFlow<String?> = _lessonIdForUpdate


    init {
        loadSection()
    }
    private fun loadSection(){
        viewModelScope.launch {
            _loading.value = true
            try {
                val document = firestore.collection("sections").document(sectionId).get().await()
                Log.d("SectionDetailScreen", "Document ${document.data}")
                if(document.data != null){
                    val questionsData = document.get("questions") as? List<Map<*, *>> ?: emptyList()
                    val questionList = questionsData.map { questionData ->
                        Question(
                            type = questionData["type"] as? String ?: "",
                            text = questionData["text"] as? String ?: "",
                            options = questionData["options"] as? List<String> ?: emptyList(),
                            correctAnswer = questionData["correctAnswer"] as? String ?: "",
                            imageUrl = questionData["imageUrl"] as? String ?: ""
                        )
                    }
                    _questions.value = questionList
                    val lessonId = document.getString("lessonId")
                    _lessonIdForUpdate.value = lessonId
                    Log.d("SectionDetailScreen", "questions: $questions")
                }

            } catch (e: Exception) {
                Log.e("SectionDetailScreen", "Error getting questions", e)
            } finally {
                _loading.value = false
            }

        }
    }

    fun onAnswerSelected(selectedAnswer: String) {
        _selectedAnswers.value[_currentQuestionIndex.value] = selectedAnswer
        Log.d("SectionDetailScreen", "Selected answer for question ${_currentQuestionIndex.value}: $selectedAnswer")
    }

    fun onNextQuestion() {
        if (_currentQuestionIndex.value < _questions.value.size - 1){
            _currentQuestionIndex.value++
        } else {
            calculateResults(
                selectedAnswers = _selectedAnswers.value,
                questions = _questions.value,
                onResult = { correctAnswers, incorrects ->
                    _numberOfCorrectAnswers.value = correctAnswers
                    _showResult.value = true
                    _incorrectQuestions.value = incorrects
                }
            )
        }
    }

    fun onRetryQuestion(index: Int){
        _currentQuestionIndex.value = index
        _showResult.value = false
    }

    private fun calculateResults(
        selectedAnswers: MutableMap<Int, String>,
        questions: List<Question>,
        onResult: (Int, List<Int>) -> Unit
    ) {
        var correctAnswers = 0
        val incorrectQuestions = mutableListOf<Int>()
        for ((index, question) in questions.withIndex()) {
            val selectedAnswer = selectedAnswers[index]
            if (selectedAnswer == question.correctAnswer) {
                correctAnswers++
            } else{
                incorrectQuestions.add(index)
            }
        }
        onResult(correctAnswers, incorrectQuestions)
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionsScreen(navController: NavHostController) {
    val viewModel: SectionsViewModel = viewModel()
    val sectionList by viewModel.sectionList.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val completedSectionIds by viewModel.completedSectionIds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Разделы практики") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.back),
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
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(sectionList) { section ->
                    val isUnlocked = section.previousSectionId == null || completedSectionIds.contains(section.previousSectionId)
                    SectionListItem(section = section, navController = navController, isUnlocked = isUnlocked)
                }
            }
        }
    }
}


@Composable
fun SectionListItem(section: Section, navController: NavHostController, isUnlocked: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable (enabled = isUnlocked) {
                if(isUnlocked){
                    navController.navigate("sectionDetail/${section.sectionId}")
                }
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box (modifier = Modifier.fillMaxSize()){
            Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                Text(text = section.sectionTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = section.sectionDescription, style = MaterialTheme.typography.bodyMedium)
            }
            if (!isUnlocked){
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ){
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = "Locked")
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(sectionId: String, navController: NavHostController, lessonIdForSection: String? = null) {
    val viewModel: SectionDetailViewModel = viewModel { SectionDetailViewModel(sectionId)}

    val questions by viewModel.questions.collectAsState()
    val selectedAnswers by viewModel.selectedAnswers.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val showResult by viewModel.showResult.collectAsState()
    val numberOfCorrectAnswers by viewModel.numberOfCorrectAnswers.collectAsState()
    val incorrectQuestions by viewModel.incorrectQuestions.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val lessonIdForUpdate by viewModel.lessonIdForUpdate.collectAsState()



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Практика") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.back),
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ){
                CircularProgressIndicator()
            }
        }
        else if (questions.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center){
                Text("No Questions")
            }

        } else if (showResult) {
            ResultScreen(
                numberOfQuestions = questions.size,
                numberOfCorrectAnswers = numberOfCorrectAnswers,
                navController = navController,
                sectionId = sectionId,
                modifier = Modifier.padding(paddingValues),
                incorrectQuestions = incorrectQuestions,
                questions = questions,
                onRetryQuestion = {index ->
                    viewModel.onRetryQuestion(index)
                },
                lessonId = lessonIdForUpdate,

                )
        } else {
            QuestionScreen(
                question = questions[currentQuestionIndex],
                questionIndex = currentQuestionIndex,
                onAnswerSelected = { selectedAnswer ->
                    viewModel.onAnswerSelected(selectedAnswer)
                },
                onNextQuestion = {
                    viewModel.onNextQuestion()
                },
                selectedAnswer = selectedAnswers[currentQuestionIndex] ?: "",
                navController = navController,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun ResultScreen(
    numberOfQuestions: Int,
    numberOfCorrectAnswers: Int,
    navController: NavHostController,
    sectionId: String,
    modifier: Modifier = Modifier,
    incorrectQuestions: List<Int>,
    questions: List<Question>,
    onRetryQuestion: (Int) -> Unit,
    lessonId: String?
) {
    val progress = if (numberOfQuestions > 0) numberOfCorrectAnswers.toFloat() / numberOfQuestions else 0f
    val sectionsViewModel : SectionsViewModel = viewModel()
    val lessonViewModel: LessonViewModel = viewModel { lessonId?.let { LessonViewModel(it) } ?: LessonViewModel("") }
    val lessonsViewModel: LessonsViewModel = viewModel()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Результат",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Text(
            "Правильных ответов: $numberOfCorrectAnswers из $numberOfQuestions"
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        CircularProgressIndicator(
            progress = progress,
            modifier = Modifier.size(100.dp)
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        if (incorrectQuestions.isNotEmpty()) {
            Text(
                text = "Ошибки в вопросах:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            incorrectQuestions.forEach { index ->
                Button(onClick = { onRetryQuestion(index) }) {
                    Text(text = "Вопрос ${index + 1}")
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (numberOfCorrectAnswers == numberOfQuestions)
                "Отлично, все ответы верны!"
            else if (numberOfCorrectAnswers >= numberOfQuestions / 2)
                "Хороший результат, но есть куда расти."
            else
                "Похоже, что тебе нужно повторить материал."
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            navController.popBackStack()
        }) {
            Text("Вернуться к списку разделов")
        }
        LaunchedEffect(Unit) {
            lessonId?.let {
                lessonsViewModel.markLessonCompleted(it)
            }
            sectionsViewModel.markSectionCompleted(sectionId)
        }
    }
}
@Composable
fun QuestionScreen(
    question: Question,
    questionIndex : Int,
    onAnswerSelected: (String) -> Unit,
    onNextQuestion: () -> Unit,
    selectedAnswer: String,
    navController: NavHostController,
    modifier : Modifier = Modifier
){
    var currentSelectedAnswer by remember { mutableStateOf(selectedAnswer) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Вопрос ${questionIndex + 1}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (!question.imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        layout(placeable.width, placeable.height) {
                            if (placeable.width > 0 && placeable.height > 0 ) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                    }
            ){
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(question.imageUrl)
                        .crossfade(true)
                        .size(Size.ORIGINAL)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            navController.navigate("imageViewer/${question.imageUrl}")
                        },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(text = question.text, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        when (question.type) {
            "multiple_choice" -> {
                question.options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (option == currentSelectedAnswer),
                                onClick = {
                                    currentSelectedAnswer = option
                                    onAnswerSelected(option)
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (option == currentSelectedAnswer), onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            "input" -> {
                OutlinedTextField(
                    value = currentSelectedAnswer,
                    onValueChange = {
                        currentSelectedAnswer = it
                        onAnswerSelected(it)
                    },
                    label = { Text("Ваш ответ") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            else -> {
                Text(text = "Unknown question type")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (currentSelectedAnswer.isNotBlank()){
                    onNextQuestion()
                } else{
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Выберите или введите ответ",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            enabled = currentSelectedAnswer.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Дальше")
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(16.dp) )
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(imageUrl: String, navController: NavHostController) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offset += pan
                }
            }
    ) {
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
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}


@Preview(showBackground = true)
@Composable
fun SectionsScreenPreview() {
    MyTheme {
        SectionsScreen(rememberNavController())
    }
}