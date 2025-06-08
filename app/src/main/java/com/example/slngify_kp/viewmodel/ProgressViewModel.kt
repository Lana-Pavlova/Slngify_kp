package com.example.slngify_kp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


data class UserProgress(
    val completedLessonIds: List<String> = emptyList(),
    val completedTasks: List<String> = emptyList(),
    val achievements: List<AchievementData> = emptyList(),
    val completedSectionIds: List<String> = emptyList()
)

data class AchievementData(
    val name: String,
    val icon: String
)

data class Lesson(
    val id: String,
    val title: String,
    var isCompleted: Boolean = false
)

data class Section(
    val id: String,
    val title: String,
    var isCompleted: Boolean = false
)
data class TestResult(
    val correctAnswers: Int,
    val incorrectAnswers: Int
)
class ProgressViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userId = auth.currentUser?.uid ?: "default_user"

    private val _userProgress = MutableStateFlow(UserProgress())
    val userProgress: StateFlow<UserProgress> = _userProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _lessonList = MutableStateFlow<List<Lesson>>(emptyList())
    val lessonList: StateFlow<List<Lesson>> = _lessonList.asStateFlow()

    private val _sectionList = MutableStateFlow<List<Section>>(emptyList())
    val sectionList: StateFlow<List<Section>> = _sectionList.asStateFlow()

    private val _totalLessons = MutableStateFlow(0)
    val totalLessons: StateFlow<Int> = _totalLessons.asStateFlow()

    private val _newLessonsCompleted = MutableStateFlow(0)
    val newLessonsCompleted: StateFlow<Int> = _newLessonsCompleted.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            loadUserProgress()
            loadTotalLessons()
            loadLessons()
            loadSections()
        }
    }

    private suspend fun loadUserProgress() {
        Log.d("ProgressViewModel", "loadUserProgress called")
        try {
            val userDocument = firestore.collection("users").document(userId).get().await()
            Log.d("ProgressViewModel", "User document loaded successfully")

            val completedLessonIds = userDocument.get("completedLessonIds") as? List<String> ?: emptyList()
            val completedSectionIds = userDocument.get("completedSectionIds") as? List<String> ?: emptyList()
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
            _userProgress.value = UserProgress(completedLessonIds = completedLessonIds, completedSectionIds = completedSectionIds, achievements = achievements)
            _newLessonsCompleted.value = completedLessonIds.size
            Log.d("ProgressViewModel", "New lessons completed: ${_newLessonsCompleted.value}")
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load user progress: ${e.message}"
            Log.e("ProgressViewModel", "Failed to load user progress: ${e.message}")
        }
    }

    private suspend fun loadTotalLessons() {
        try {
            val result = firestore.collection("lessons").get().await()
            val lessons = result.documents.mapNotNull { document ->
                val lessonId = document.id
                val lessonTitle = document.getString("title") ?: ""
                val isCompleted = _userProgress.value.completedLessonIds.contains(lessonId)
                Lesson(id = lessonId, title = lessonTitle, isCompleted = isCompleted)
            }
            _lessonList.value = lessons
            _totalLessons.value = lessons.size
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load lessons: ${e.message}"
            _totalLessons.value = 0
        }
    }

    private suspend fun loadLessons() {
        try {
            val snapshot = firestore.collection("lessons").get().await()
            val lessons = snapshot.documents.map { document ->
                val lessonId = document.id
                val lessonTitle = document.getString("title") ?: ""
                val isCompleted = _userProgress.value.completedLessonIds.contains(lessonId)
                Lesson(id = lessonId, title = lessonTitle, isCompleted = isCompleted)
            }
            _lessonList.value = lessons
        } catch (e: Exception) {
            Log.e("ProgressViewModel", "Error loading lessons", e)
        }
    }

    private suspend fun loadSections() {
        try {
            val snapshot = firestore.collection("sections").get().await()
            val sections = snapshot.documents.map { document ->
                val sectionId = document.id
                val sectionTitle = document.getString("title") ?: ""
                val isCompleted = _userProgress.value.completedSectionIds.contains(sectionId)
                Section(id = sectionId, title = sectionTitle, isCompleted = isCompleted)
            }
            _sectionList.value = sections
        } catch (e: Exception) {
            Log.e("ProgressViewModel", "Error loading sections", e)
        }
    }
    fun completeLessonAndAwardAchievement(lessonId: String?, sectionId: String, resultText: String) {
        viewModelScope.launch {
            // Выдаем достижение
            awardAchievement(sectionId)

            // Отмечаем урок как пройденный, если результат хороший
            if (resultText != "Похоже, что тебе нужно повторить материал." && lessonId != null) {
                Log.d("ProgressViewModel", "Marking lesson completed: lessonId = $lessonId")
                markLessonCompleted(lessonId)
            } else {
                Log.d("ProgressViewModel", "Not marking lesson completed: resultText = $resultText, lessonId = $lessonId")
            }
        }
    }
    fun awardAchievement(sectionId: String) {
        Log.d("ProgressViewModel", "Starting awardAchievement for sectionId: $sectionId") // Начало

        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: run {
                Log.w("ProgressViewModel", "User not authenticated")
                return@launch // Прерываем корутину, если пользователь не аутентифицирован
            }

            Log.d("ProgressViewModel", "User ID: $userId")

            try {
                val achievementId = getAchievementIdForSection(sectionId)
                Log.d("ProgressViewModel", "Achievement ID for sectionId $sectionId: $achievementId")
                if (achievementId == null) {
                    Log.w("ProgressViewModel", "No achievement found for sectionId: $sectionId")
                    return@launch
                }

                val userDocument = firestore.collection("users").document(userId).get().await()
                Log.d("ProgressViewModel", "User document retrieved")

                val achievementIds = userDocument.get("achievementIds") as? List<String> ?: emptyList()
                Log.d("ProgressViewModel", "Existing achievement IDs: $achievementIds")

                if (achievementId !in achievementIds) {
                    Log.d("ProgressViewModel", "Awarding achievement: $achievementId")
                    firestore.collection("users").document(userId)
                        .update("achievementIds", FieldValue.arrayUnion(achievementId)).await()
                    Log.d("ProgressViewModel", "Achievement ID added to Firestore")

                    // Получаем данные достижения
                    val achievementDocument = firestore.collection("achievements").document(achievementId).get().await()
                    if (!achievementDocument.exists()) {
                        Log.e("ProgressViewModel", "Achievement document not found for achievementId: $achievementId")
                        return@launch
                    }
                    val name = achievementDocument.getString("name") ?: ""
                    val icon = achievementDocument.getString("icon") ?: ""
                    val achievementData = AchievementData(name, icon)
                    val currentAchievements = _userProgress.value.achievements.toMutableList()
                    currentAchievements.add(achievementData) // Добавляем achievementData
                    _userProgress.value = _userProgress.value.copy(achievements = currentAchievements)

                    Log.d("ProgressViewModel", "Achievement awarded successfully")
                } else {
                    Log.d("ProgressViewModel", "Achievement already awarded: $achievementId")
                }
            } catch (e: Exception) {
                Log.e("ProgressViewModel", "Error awarding achievement", e) // Выводим ошибку
            }
            Log.d("ProgressViewModel", "Finished awardAchievement") // Конец
        }
    }
    private suspend fun getAchievementIdForSection(sectionId: String): String? {
        return try {
            val querySnapshot = firestore.collection("achievements")
                .whereEqualTo("sectionId", sectionId)
                .get()
                .await()
            if (querySnapshot.documents.isNotEmpty()) {
                querySnapshot.documents[0].id
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error getting achievementId for sectionId: $sectionId", e)
            null
        }
    }
    fun markLessonCompleted(lessonId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                firestore.collection("users").document(userId)
                    .update(
                        "completedLessonIds", FieldValue.arrayUnion(lessonId)
                    )
                    .await()

                _userProgress.value = _userProgress.value.copy(
                    completedLessonIds = _userProgress.value.completedLessonIds + lessonId
                )

                _lessonList.value = _lessonList.value.map { lesson ->
                    if (lesson.id == lessonId) {
                        lesson.copy(isCompleted = true)
                    } else {
                        lesson
                    }
                }
                loadUserProgress()
            } catch (e: Exception) {
                Log.e("ProgressViewModel", "Error mark lesson completed", e)
            }
        }
    }
}