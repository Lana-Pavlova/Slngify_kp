package com.example.slngify_kp.screens

import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import com.example.slngify_kp.R
import com.example.slngify_kp.ui.theme.MyTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch


data class Section(
    val sectionId: String = "",
    val lessonId: String = "",
    val sectionTitle: String = "",
    val sectionDescription: String = "",
    val questions: List<Question> = emptyList()
)

data class Question(
    val type: String = "",
    val text: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String = "",
    val imageUrl: String = ""
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionsScreen(navController: NavHostController) {
    var sectionList by remember { mutableStateOf<List<Section>>(emptyList()) }

    LaunchedEffect(Unit) {
        val db = Firebase.firestore
        db.collection("sections")
            .get()
            .addOnSuccessListener { result ->
                val sections = mutableListOf<Section>()
                for (document in result) {
                    val sectionId = document.id
                    val lessonId = document.getString("lessonId") ?: ""
                    val sectionTitle = document.getString("sectionTitle") ?: ""
                    val sectionDescription = document.getString("sectionDescription") ?: ""
                    val questionsData = document.get("questions") as? List<Map<*, *>> ?: emptyList()

                    val questions = questionsData.map { questionData ->
                        Question(
                            type = questionData["type"] as? String ?: "",
                            text = questionData["text"] as? String ?: "",
                            options = questionData["options"] as? List<String> ?: emptyList(),
                            correctAnswer = questionData["correctAnswer"] as? String ?: "",
                            imageUrl = questionData["imageUrl"] as? String ?: ""
                        )
                    }

                    sections.add(Section(sectionId, lessonId, sectionTitle, sectionDescription, questions))

                    Log.d("SectionsScreen", "sectionId: $sectionId")
                    Log.d("SectionsScreen", "sectionTitle: $sectionTitle")
                    Log.d("SectionsScreen", "lessonId: $lessonId")
                }
                sectionList = sections
            }
            .addOnFailureListener { exception ->
                Log.e("SectionsScreen", "Error getting sections", exception)
            }
    }

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
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(sectionList) { section ->
                SectionListItem(section = section, navController = navController)
            }
        }
    }
}


@Composable
fun SectionListItem(section: Section, navController: NavHostController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                navController.navigate("sectionDetail/${section.sectionId}")
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = section.sectionTitle, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = section.sectionDescription, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectionDetailScreen(sectionId: String, navController: NavHostController) {
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    var numberOfCorrectAnswers by remember { mutableStateOf(0) }
    var incorrectQuestions by remember { mutableStateOf<List<Int>>(emptyList()) }


    LaunchedEffect(sectionId) {
        val db = Firebase.firestore
        db.collection("sections").document(sectionId)
            .get()
            .addOnSuccessListener { document ->
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
                    questions = questionList
                    Log.d("SectionDetailScreen", "questions: $questions")
                }

            }
            .addOnFailureListener { e ->
                Log.e("SectionDetailScreen", "Error getting questions", e)
            }
    }

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
        if (questions.isEmpty()) {
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues), contentAlignment = Alignment.Center){
                CircularProgressIndicator()
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
                    currentQuestionIndex = index
                    showResult = false
                }
            )
        }
        else{
            QuestionScreen(
                question = questions[currentQuestionIndex],
                questionIndex = currentQuestionIndex,
                onAnswerSelected = { selectedAnswer ->
                    selectedAnswers[currentQuestionIndex] = selectedAnswer
                    Log.d("SectionDetailScreen", "Selected answer for question $currentQuestionIndex: $selectedAnswer")
                },
                onNextQuestion = {
                    if (currentQuestionIndex < questions.size - 1){
                        currentQuestionIndex++
                    } else {
                        calculateResults(
                            selectedAnswers = selectedAnswers,
                            questions = questions,
                            onResult = { correctAnswers, incorrects ->
                                numberOfCorrectAnswers = correctAnswers
                                showResult = true
                                incorrectQuestions = incorrects
                            }
                        )
                    }
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
    incorrectQuestions : List<Int>,
    questions: List<Question>,
    onRetryQuestion: (Int) -> Unit
) {

    val progress = if (numberOfQuestions > 0 ) numberOfCorrectAnswers.toFloat() / numberOfQuestions else 0f
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
        if (incorrectQuestions.isNotEmpty()){
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
        Button(onClick = { navController.navigateUp() }) {
            Text("Вернуться к списку разделов")
        }
        LaunchedEffect(Unit) {
            updateUserProgress(sectionId, numberOfCorrectAnswers, numberOfQuestions )
        }
    }

}
private fun calculateResults(
    selectedAnswers: Map<Int, String>,
    questions: List<Question>,
    onResult : (Int, List<Int>) -> Unit
){
    var numberOfCorrectAnswers = 0
    val incorrectQuestions = mutableListOf<Int>()
    questions.forEachIndexed { index, question ->
        if (selectedAnswers[index] == question.correctAnswer) {
            numberOfCorrectAnswers++
            Log.d("SectionDetailScreen", "Correct answer for question $index")
        } else {
            Log.d("SectionDetailScreen", "Incorrect answer for question $index. Selected: ${selectedAnswers[index]}, Correct: ${question.correctAnswer}")
            incorrectQuestions.add(index)
        }
    }
    onResult(numberOfCorrectAnswers, incorrectQuestions)
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
    var inputAnswer by remember { mutableStateOf(selectedAnswer) }
    var answerSelected by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var currentSelectedAnswer by remember { mutableStateOf(selectedAnswer) }

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
                            navController.navigate("imageViewer/$question.imageUrl")
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
                                    answerSelected = true
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
                    value = inputAnswer,
                    onValueChange = {
                        inputAnswer = it
                        onAnswerSelected(it)
                        answerSelected = true
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
                if (answerSelected || inputAnswer.isNotBlank()) {
                    onNextQuestion()
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Выберите или введите ответ",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
            },
            enabled = answerSelected || inputAnswer.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Дальше")
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(16.dp) )
    }
}


private fun updateUserProgress(
    sectionId: String,
    correctAnswers: Int,
    numberOfQuestions: Int
) {
    val userId = Firebase.auth.currentUser?.uid ?: "testUser"
    val db = Firebase.firestore
    val userRef = db.collection("users").document(userId)

    db.runTransaction { transaction ->
        val snapshot = transaction.get(userRef)
        val userProgress = snapshot.toObject(UserProgress::class.java) ?: UserProgress()
        val updatedSectionProgress = userProgress.sectionProgress.toMutableMap().apply {
            this[sectionId] = correctAnswers
        }


        val completedSections = userProgress.completedSections.toMutableList()

        if (correctAnswers == numberOfQuestions) {
            if (!completedSections.contains(sectionId)){
                completedSections.add(sectionId)
            }
        }

        val updatedProgress =  userProgress.copy(
            completedSections = completedSections.toList(),
            sectionProgress = updatedSectionProgress
        )
        transaction.set(userRef, updatedProgress)
        null
    }.addOnSuccessListener {
        Log.d("SectionDetailScreen", "Transaction success")
    }
        .addOnFailureListener{ exception ->
            Log.e("SectionDetailScreen", "Transaction failed: ", exception)
        }

}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(imageUrl: String, navController: NavHostController) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageLoaded by remember { mutableStateOf(false) }

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
        if(imageLoaded) {
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
        } else {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
            )
        }

    }

    LaunchedEffect(imageUrl){
        imageLoaded = true
    }
}


@Preview(showBackground = true)
@Composable
fun SectionsScreenPreview() {
    MyTheme {
        SectionsScreen(rememberNavController())
    }
}