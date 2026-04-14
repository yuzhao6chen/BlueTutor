package com.bluetutor.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.School
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainTab(
    val destination: BluetutorDestination,
    val label: String,
    val icon: ImageVector,
) {
    Preview(BluetutorDestination.Preview, "预习", Icons.Filled.School),
    Solve(BluetutorDestination.Solve, "引导解题", Icons.Filled.PhotoCamera),
    Practice(BluetutorDestination.Practice, "错题练习", Icons.Filled.AssignmentTurnedIn),
    Profile(BluetutorDestination.Profile, "我的", Icons.Filled.Person),
}