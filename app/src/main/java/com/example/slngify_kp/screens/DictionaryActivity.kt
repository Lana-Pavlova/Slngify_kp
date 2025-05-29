package com.example.slngify_kp.screens

import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.slngify_kp.R
import com.example.slngify_kp.ui.theme.MyTheme
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// добавить view model для добавление слов в "избранное"
class DictionaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()
                    DictionaryScreen(navController)
                }
            }
        }
    }
}
data class SlangTerm(
    val term: String,
    val translation: String,
    val example: String? = null,
    val definition: String? = null
)

class DictionaryViewModel : ViewModel() {
    private val _slangTerms = MutableStateFlow<List<SlangTerm>>(emptyList())
    val slangTerms: StateFlow<List<SlangTerm>> = _slangTerms.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSlangTerms()
    }

    private fun loadSlangTerms() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _slangTerms.value = fetchWordTranslations()
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun getFilteredSlangTerms(): List<SlangTerm> {
        val searchTerm = _searchText.value.lowercase()
        return _slangTerms.value.filter {
            it.term.lowercase().contains(searchTerm) || it.definition?.lowercase()?.contains(searchTerm) ?: false
        }
    }

    suspend fun fetchWordTranslations(): List<SlangTerm> {
        return try {
            val querySnapshot = Firebase.firestore.collection("wordOfTheDay").get().await()
            querySnapshot.documents.mapNotNull { document ->
                SlangTerm(
                    term = document.getString("word") ?: "",
                    translation = document.getString("translation") ?: "",
                    example = document.getString("examples"),
                    definition = document.getString("definition")
                )
            }
        } catch (e: Exception) {
            println("Error fetching word translations: ${e.message}")
            _error.value = e.message ?: "An unexpected error occurred."
            emptyList()
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChange,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
        },
        placeholder = { Text("Search", color = Color.Gray) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = Color.LightGray,
            unfocusedBorderColor = Color.LightGray
        ),
    )
}
@Composable
fun SlangTermList(slangTerms: List<SlangTerm>) {
    LazyColumn {
        val grouped = slangTerms.groupBy { it.term.first().uppercaseChar() }
        grouped.forEach { (letter, terms) ->
            item {
                Text(text = letter.toString(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
            }
            items(terms) { term ->
                SlangTermItem(term = term)
            }
        }
    }
}
@Composable
fun SlangTermItem(term: SlangTerm) {
    var showExampleDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable {
                if (term.example != null) { // Проверяем, есть ли пример
                    showExampleDialog = true
                }
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = term.term, style = MaterialTheme.typography.titleMedium, color = Color.Black)
            Text(text = term.translation ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }

    if (showExampleDialog) {
        ExampleDialog(
            term = term,
            onDismiss = { showExampleDialog = false }
        )
    }
}

@Composable
fun ExampleDialog(term: SlangTerm, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(shape = RoundedCornerShape(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Example for ${term.term}:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = term.example ?: "No example available.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { onDismiss() }) {
                    Text("Close")
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(navController: NavController) {
    val viewModel: DictionaryViewModel = viewModel()
    val searchText by viewModel.searchText.collectAsState()
    val slangTerms by viewModel.slangTerms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Словарь") },
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
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            SearchBar(
                searchText = searchText,
                onSearchTextChange = viewModel::onSearchTextChange,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (error != null) {
                Text("Error: $error")
            } else {
                SlangTermList(slangTerms = viewModel.getFilteredSlangTerms())
            }
        }
    }
}
@Preview
@Composable
fun DictionaryScreenPreview () {
    MyTheme {
        DictionaryScreen(rememberNavController())
    }
}