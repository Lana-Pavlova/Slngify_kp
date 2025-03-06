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
    val completedLessons: List<String> = emptyList(),
    val completedTasks: List<String> = emptyList(),
    val achievements: List<AchievementData> = emptyList(),
    val completedSections: List<String> = emptyList() // Добавили completedSections

)
data class AchievementData(
    val name: String,
    val icon: String
)

class ProgressViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userId = auth.currentUser?.uid ?: "default_user_id" // Обработка отсутствия пользователя

    private val _userProgress = MutableStateFlow(UserProgress())
    val userProgress: StateFlow<UserProgress> = _userProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lessonList = MutableStateFlow<List<String>>(emptyList())
    val lessonList: StateFlow<List<String>> = _lessonList.asStateFlow()

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
        }
    }

    private suspend fun loadUserProgress() {
        try {
            val userDocument = firestore.collection("users").document(userId).get().await()

            val completedLessons = userDocument.get("completedLessonIds") as? List<String> ?: emptyList()
            val completedTasks = userDocument.get("completedTaskIds") as? List<String> ?: emptyList()
            val achievementIds = userDocument.get("achievementIds") as? List<String> ?: emptyList()

            // Загружаем информацию о каждом достижении по его ID
            val achievements = mutableListOf<AchievementData>()
            for (achievementId in achievementIds) {
                val achievementDocument = firestore.collection("achievements").document(achievementId).get().await()
                if (achievementDocument.exists()) {
                    val name = achievementDocument.getString("name") ?: ""
                    val icon = achievementDocument.getString("icon") ?: ""
                    achievements.add(AchievementData(name, icon))
                }
            }

            _userProgress.value = UserProgress(completedLessons = completedLessons, completedTasks = completedTasks, achievements = achievements)

        } catch (e: Exception) {
            _errorMessage.value = "Failed to load user progress: ${e.message}"
            // Log.e("MainViewModel", "Error loading user progress", e)
        }
    }
    private suspend fun loadTotalLessons() {
        try {
            val result = firestore.collection("lessons").get().await()
            val lessons = result.documents.map { it.id }
            _lessonList.value = lessons
            _totalLessons.value = lessons.size
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load lessons: ${e.message}"
            _totalLessons.value = 0
            // Log.e("MainViewModel", "Error loading lessons", e)
        }
    }

//    fun markLessonCompleted(lessonId: String) {
//        markItemCompleted(lessonId, "completedLessonIds") { currentProgress ->
//            currentProgress.copy(completedLessons = currentProgress.completedLessons + lessonId)
//        }
//        _newLessonsCompleted.value = _newLessonsCompleted.value + 1
//    }
//
//    fun markTaskCompleted(taskId: String) {
//        markItemCompleted(taskId, "completedTaskIds") { currentProgress ->
//            currentProgress.copy(completedTasks = currentProgress.completedTasks + taskId)
//        }
//    }

//    private fun markItemCompleted(
//        itemId: String,
//        fieldName: String,
//        updateProgress: (UserProgress) -> UserProgress
//    ) {
//        viewModelScope.launch {
//            try {
//                firestore.collection("users").document(userId)
//                    .update(fieldName, FieldValue.arrayUnion(itemId)).await()
//
//                _userProgress.value = updateProgress(_userProgress.value)
//
//            } catch (e: Exception) {
//                _errorMessage.value = "Failed to mark item completed: ${e.message}"
//                 Log.e("ProgressViewModel", "Error marking item completed", e)
//            }
//        }
//    }
    fun awardAchievement(sectionId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch

            try {
                // 1. Получаем achievementId на основе sectionId (из Firestore)
                val achievementId = getAchievementIdForSection(sectionId)
                if (achievementId == null) {
                    // Нет достижения для этой секции
                    Log.w("ProgressViewModel", "No achievement found for sectionId: $sectionId")
                    return@launch
                }

                // 2. Получаем данные о достижении из коллекции "achievements"
                val achievementDocument = firestore.collection("achievements").document(achievementId).get().await()
                if (!achievementDocument.exists()) {
                    Log.e("ProgressViewModel", "Achievement document not found for achievementId: $achievementId")
                    return@launch
                }

                val name = achievementDocument.getString("name") ?: ""
                val icon = achievementDocument.getString("icon") ?: ""
                val achievementData = AchievementData(name, icon)

                // 3. Добавляем ID достижения в массив достижений пользователя
                firestore.collection("users").document(userId)
                    .update("achievementIds", FieldValue.arrayUnion(achievementId)).await()

                // 4. Обновить _userProgress (оптимистичное обновление)
                val currentAchievements = _userProgress.value.achievements.toMutableList()
                currentAchievements.add(achievementData) // Добавляем achievementData
                _userProgress.value = _userProgress.value.copy(achievements = currentAchievements)

            } catch (e: Exception) {
                Log.e("ProgressViewModel", "Error awarding achievement", e)
            }
        }
    }
    private suspend fun getAchievementIdForSection(sectionId: String): String? {
        return try {
            // Ищем документ в коллекции "achievements", где sectionId соответствует заданному
            val querySnapshot = firestore.collection("achievements")
                .whereEqualTo("sectionId", sectionId)
                .get()
                .await()

            if (querySnapshot.documents.isNotEmpty()) {
                // Если найден документ, возвращаем его ID
                querySnapshot.documents[0].id
            } else {
                // Если документ не найден, возвращаем null
                null
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error getting achievementId for sectionId: $sectionId", e)
            null
        }
    }


}