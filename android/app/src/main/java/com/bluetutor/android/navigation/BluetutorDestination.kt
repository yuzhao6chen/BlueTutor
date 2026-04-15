package com.bluetutor.android.navigation

sealed class BluetutorDestination(val route: String) {
    object Preview : BluetutorDestination("preview")
    object Solve : BluetutorDestination("solve")
    object Practice : BluetutorDestination("practice")
    object Profile : BluetutorDestination("profile")
}