package com.example.slngify_kp.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.slngify_kp.R
import androidx.navigation.compose.rememberNavController


class DictionaryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()
                    DictionaryScreen(navController) // Передаем navController
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Словарь") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.back), // Убедитесь, что иконка существует
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        content = { paddingValues ->
            var query by remember { mutableStateOf("") }
            val dictionary = listOf(
                "Привет" to "Hello",
                "Мир" to "World",
                "Книга" to "Book",
                "Кошка" to "Cat",
                "Собака" to "Dog",
                "Сленг" to "Slang",
                "Компьютер" to "Computer",
                "Телефон" to "Phone"
            )

            val filteredDictionary = dictionary.filter {
                it.first.contains(query, ignoreCase = true) || it.second.contains(query, ignoreCase = true)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Поиск") },
                    placeholder = { Text("Введите слово") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(filteredDictionary) { item ->
                        DictionaryItem(wordPair = item)
                        Divider()
                    }
                }
            }
        }
    )
}

@Composable
fun DictionaryItem(wordPair: Pair<String, String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = wordPair.first, style = MaterialTheme.typography.bodyLarge)
        Text(text = wordPair.second, style = MaterialTheme.typography.bodyLarge)
    }
}
