package com.loteriavip.app.presentation.screens.live

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loteriavip.app.domain.model.ResultCategory
import com.loteriavip.app.presentation.components.LiveResultCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveResultsScreen(
    viewModel: LiveResultsViewModel = viewModel()
) {
    val filteredResults by viewModel.filteredResults.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }
                            val dateString = sdf.format(Date(millis))
                            viewModel.setDate(dateString)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7F6))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            HeaderSection(onDateClick = { showDatePicker = true })

            // Date filter banner if active
            selectedDate?.let { date ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .background(Color(0xFFD8F3EC), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resultados del: $date",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF005943)
                    )
                    Text(
                        text = "Volver a hoy",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        modifier = Modifier.clickable { viewModel.clearDate() }
                    )
                }
            }

            // Category Selector
            CategorySelector(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setCategory(it) }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF005943))
                }
            } else {
                // Results List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredResults) { result ->
                        LiveResultCard(
                            result = result,
                            onFavoriteClick = { viewModel.toggleFavorite(result.id) }
                        )
                    }
                }
            }
        }

        // Floating Action Pill (Bottom Navigation)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            FloatingNavigationPill()
        }
    }
}

@Composable
private fun HeaderSection(onDateClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Menu, contentDescription = null, tint = Color(0xFF005943))
        Text(
            text = "Lotería VIP",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF005943)
        )
        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = "Buscar por fecha",
            tint = Color(0xFF005943),
            modifier = Modifier.clickable { onDateClick() }
        )
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: ResultCategory,
    onCategorySelected: (ResultCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ResultCategory.entries.forEach { category ->
            val isSelected = selectedCategory == category
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCategorySelected(category) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color(0xFF005943) else Color.White,
                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Text(
                    text = when (category) {
                        ResultCategory.LOTERIA -> "Loterías"
                        ResultCategory.LOTTO -> "Lottos"
                        ResultCategory.AMERICANA -> "Americanas"
                    },
                    modifier = Modifier.padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun FloatingNavigationPill() {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF005943),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.height(64.dp).width(280.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillItem(Icons.Default.Home, "Inicio", isSelected = true)
            PillItem(Icons.Default.Favorite, "Favoritos", isSelected = false)
            PillItem(Icons.Default.Menu, "Más", isSelected = false)
        }
    }
}

@Composable
private fun PillItem(icon: ImageVector, label: String, isSelected: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { /* Navigate */ }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        if (isSelected) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .size(4.dp)
                    .background(Color(0xFFFFD700), androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
