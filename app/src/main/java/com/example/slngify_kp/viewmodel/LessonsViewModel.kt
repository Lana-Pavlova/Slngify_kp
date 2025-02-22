package com.example.slngify_kp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.slngify_kp.screens.UserProgress
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LessonsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val userId = "user_id" // Замени на реальный ID пользователя

    private val _userProgress = MutableStateFlow(UserProgress())
    val userProgress: StateFlow<UserProgress> = _userProgress

    private val _lessonList = MutableStateFlow<List<String>>(emptyList())
    val lessonList: StateFlow<List<String>> = _lessonList

    // Состояния для количества пройденных и общего количества уроков
    private val _newLessonsCompleted = MutableStateFlow(0)
    val newLessonsCompleted: StateFlow<Int> = _newLessonsCompleted.asStateFlow()

    private val _totalLessons = MutableStateFlow(0)
    val totalLessons: StateFlow<Int> = _totalLessons.asStateFlow()

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
        // Загружаем данные о прогрессе пользователя из Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val completedLessons =
                        (document.get("completedLessons") as? List<String>) ?: emptyList()
                    val achievements =
                        (document.get("achievements") as? List<Long>)?.map { it.toInt() }
                            ?: emptyList() // Добавили загрузку достижений
                    _userProgress.value = UserProgress(
                        completedLessons = completedLessons,
                        achievements = achievements
                    )
                    _newLessonsCompleted.value = completedLessons.size
                } else {
                    // Обработка ошибки: документ не найден
                    _newLessonsCompleted.value = 0
                }
            }
            .addOnFailureListener { exception ->
                // Обработка ошибки: не удалось загрузить данные
                _newLessonsCompleted.value = 0
                // TODO: Handle the error appropriately, e.g., log it or show a message to the user
            }
    }

    private suspend fun loadTotalLessons() {
        // Загружаем список уроков из Firestore
        db.collection("lessons")
            .get()
            .addOnSuccessListener { result ->
                val lessons = result.documents.map { it.id }
                _lessonList.value = lessons
                _totalLessons.value = lessons.size
            }
            .addOnFailureListener { exception ->
                // Обработка ошибки: не удалось загрузить данные
                _totalLessons.value = 0
                // TODO: Handle the error appropriately, e.g., log it or show a message to the user
            }
    }
}