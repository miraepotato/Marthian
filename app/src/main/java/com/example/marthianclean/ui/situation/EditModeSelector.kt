package com.example.marthianclean.ui.situation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditModeSelector(
    onDispatchSelected: () -> Unit,
    onDisasterSelected: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp)
            .background(Color.Black)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Edit Mode",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        SelectorButton(
            title = "Dispatch Unit Configuration",
            subtitle = "Configure responding units",
            onClick = onDispatchSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        SelectorButton(
            title = "Disaster Information",
            subtitle = "Edit incident details",
            onClick = onDisasterSelected
        )
    }
}

@Composable
private fun SelectorButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFFFF9800),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}
