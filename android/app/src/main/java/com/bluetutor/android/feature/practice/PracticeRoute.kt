package com.bluetutor.android.feature.practice

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.bluetutor.android.feature.practice.component.PracticeDetailScreen
import com.bluetutor.android.feature.practice.component.PracticeDialogueScreen
import com.bluetutor.android.feature.practice.component.PracticeHomeScreen
import com.bluetutor.android.feature.practice.component.PracticeRecommendationPracticeScreen
import com.bluetutor.android.feature.practice.component.PracticeRecommendationScreen
import com.bluetutor.android.feature.practice.component.PracticeRedoScreen
import com.bluetutor.android.feature.practice.component.PracticeTimelineScreen

@Composable
fun PracticeRoute(
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
) {
    val backStack = remember {
        mutableStateListOf<PracticeDestination>(PracticeDestination.Home)
    }

    val currentDestination = backStack.last()

    fun navigate(destination: PracticeDestination) {
        backStack.add(destination)
    }

    fun popBack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        popBack()
    }

    when (val destination = currentDestination) {
        PracticeDestination.Home -> {
            PracticeHomeScreen(
                onNavigate = ::navigate,
                onBottomBarVisibilityChange = onBottomBarVisibilityChange,
                modifier = modifier,
            )
        }

        is PracticeDestination.Timeline -> {
            PracticeTimelineScreen(
                initialStatus = destination.initialStatus,
                initialKnowledgeTag = destination.initialKnowledgeTag,
                onNavigate = ::navigate,
                onBack = ::popBack,
                onBottomBarVisibilityChange = onBottomBarVisibilityChange,
                modifier = modifier,
            )
        }

        is PracticeDestination.Detail -> {
            PracticeDetailScreen(
                reportId = destination.reportId,
                onNavigate = ::navigate,
                onBack = ::popBack,
                onBottomBarVisibilityChange = onBottomBarVisibilityChange,
                modifier = modifier,
            )
        }

        is PracticeDestination.Redo -> {
            PracticeRedoScreen(
                reportId = destination.reportId,
                onNavigate = ::navigate,
                onBack = ::popBack,
                onBottomBarVisibilityChange = onBottomBarVisibilityChange,
                modifier = modifier,
            )
        }

        is PracticeDestination.Dialogue -> {
            PracticeDialogueScreen(
                reportId = destination.reportId,
                onNavigate = ::navigate,
                onBack = ::popBack,
                onBottomBarVisibilityChange = onBottomBarVisibilityChange,
                modifier = modifier,
            )
        }

        is PracticeDestination.Recommendation -> {
            PracticeRecommendationScreen(
                reportId = destination.reportId,
                recommendationType = destination.recommendationType,
                onNavigate = ::navigate,
                onBack = ::popBack,
                onBottomBarVisibilityChange = onBottomBarVisibilityChange,
                modifier = modifier,
            )
        }

        is PracticeDestination.RecommendationPractice -> {
            PracticeRecommendationPracticeScreen(
                recommendationId = destination.recommendationId,
                onNavigate = ::navigate,
                onBack = ::popBack,
                onBottomBarVisibilityChange = onBottomBarVisibilityChange,
                modifier = modifier,
            )
        }
    }
}