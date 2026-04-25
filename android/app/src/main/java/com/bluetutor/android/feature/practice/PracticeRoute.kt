package com.bluetutor.android.feature.practice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bluetutor.android.feature.practice.component.PracticeDetailScreen
import com.bluetutor.android.feature.practice.component.PracticeDialogueScreen
import com.bluetutor.android.feature.practice.component.PracticeDialogueSessionScreen
import com.bluetutor.android.feature.practice.component.PracticeHomeScreen
import com.bluetutor.android.feature.practice.component.PracticeRecommendationPracticeScreen
import com.bluetutor.android.feature.practice.component.PracticeRecommendationScreen
import com.bluetutor.android.feature.practice.component.PracticeRedoScreen
import com.bluetutor.android.feature.practice.component.PracticeRedoSessionScreen
import com.bluetutor.android.feature.practice.component.PracticeTimelineScreen

@Composable
fun PracticeRoute(
    modifier: Modifier = Modifier,
    onBottomBarVisibilityChange: (Boolean) -> Unit = {},
) {
    var currentDestination by remember { mutableStateOf<PracticeDestination>(PracticeDestination.Home) }
    val navigationStack = remember { mutableListOf<PracticeDestination>(PracticeDestination.Home) }

    val onNavigate: (PracticeDestination) -> Unit = { dest ->
        navigationStack.add(dest)
        currentDestination = dest
    }

    val onBack: () -> Unit = {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.lastIndex)
            currentDestination = navigationStack.last()
        }
    }

    when (val dest = currentDestination) {
        is PracticeDestination.Home -> PracticeHomeScreen(
            onNavigate = onNavigate,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
        is PracticeDestination.Timeline -> PracticeTimelineScreen(
            initialStatus = dest.initialStatus,
            initialKnowledgeTag = dest.initialKnowledgeTag,
            onNavigate = onNavigate,
            onBack = onBack,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
        is PracticeDestination.Detail -> PracticeDetailScreen(
            reportId = dest.reportId,
            onNavigate = onNavigate,
            onBack = onBack,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
        is PracticeDestination.Lecture -> PracticeDetailScreen(
            reportId = dest.reportId,
            onNavigate = onNavigate,
            onBack = onBack,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
        is PracticeDestination.Redo -> PracticeRedoScreen(
            reportId = dest.reportId,
            onNavigate = onNavigate,
            onBack = onBack,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
        is PracticeDestination.RedoSession -> PracticeRedoSessionScreen(
            sessionId = dest.sessionId,
            onNavigate = onNavigate,
            onBack = onBack,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
        is PracticeDestination.Dialogue -> PracticeDialogueScreen(
            reportId = dest.reportId,
            onNavigate = onNavigate,
            onBack = onBack,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
        is PracticeDestination.DialogueSession -> PracticeDialogueSessionScreen(
            sessionId = dest.sessionId,
            onNavigate = onNavigate,
            onBack = onBack,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
        is PracticeDestination.Recommendation -> PracticeRecommendationScreen(
            reportId = dest.reportId,
            recommendationType = dest.type,
            onNavigate = onNavigate,
            onBack = onBack,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
        is PracticeDestination.RecommendationPractice -> PracticeRecommendationPracticeScreen(
            recommendationId = dest.recommendationId,
            onNavigate = onNavigate,
            onBack = onBack,
            onBottomBarVisibilityChange = onBottomBarVisibilityChange,
            modifier = modifier,
        )
    }
}
