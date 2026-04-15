package com.bluetutor.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bluetutor.android.feature.practice.PracticeRoute
import com.bluetutor.android.feature.preview.PreviewRoute
import com.bluetutor.android.feature.profile.ProfileRoute
import com.bluetutor.android.feature.solve.SolveRoute

@Composable
fun BluetutorNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = BluetutorDestination.Preview.route,
        modifier = modifier,
    ) {
        composable(BluetutorDestination.Preview.route) {
            PreviewRoute()
        }
        composable(BluetutorDestination.Solve.route) {
            SolveRoute()
        }
        composable(BluetutorDestination.Practice.route) {
            PracticeRoute()
        }
        composable(BluetutorDestination.Profile.route) {
            ProfileRoute()
        }
    }
}