package com.example.slngify_kp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.slngify_kp.R

@Composable
fun BottomNavigationBar(navController: NavController, selectedItem: String, onItemSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavigationButton(
            navController = navController,
            route = "home",
            selected = selectedItem == "home",
            icon = R.drawable.home,
            onItemSelected = onItemSelected
        )

        NavigationButton(
            navController = navController,
            route = "dictionaryPage",
            selected = selectedItem == "dictionaryPage",
            icon = R.drawable.dictionary,
            onItemSelected = onItemSelected
        )

        NavigationButton(
            navController = navController,
            route = "lessonsList",
            selected = selectedItem == "lessonsList",
            icon = R.drawable.lessons,
            onItemSelected = onItemSelected
        )
        NavigationButton(
            navController = navController,
            route = "sectionsList",
            selected = selectedItem == "sectionsList",
            icon = R.drawable.practice,
            onItemSelected = onItemSelected
        )
        NavigationButton(
            navController = navController,
            route = "profilePage",
            selected = selectedItem == "profilePage",
            icon = R.drawable.profile,
            onItemSelected = onItemSelected
        )
    }
}

@Composable
fun NavigationButton(
    navController: NavController,
    route: String,
    selected: Boolean,
    icon: Int,
    onItemSelected: (String) -> Unit
) {
    Button(
        onClick = {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId)
                launchSingleTop = true
            }
            onItemSelected(route)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.size(75.dp) // Задаем размер кнопки
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null, // Убираем описание
            modifier = Modifier.size(70.dp), // Задаем размер иконки
            tint = Color.Unspecified
        )
    }
}

