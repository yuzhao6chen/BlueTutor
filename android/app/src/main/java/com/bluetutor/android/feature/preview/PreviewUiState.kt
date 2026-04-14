package com.bluetutor.android.feature.preview

data class PreviewUiState(
    val userName: String,
    val streakDays: Int,
    val weeklyPreviewedLessons: Int,
    val weeklyGoalCurrent: Int,
    val weeklyGoalTarget: Int,
    val quickTopics: List<PreviewQuickTopicUiModel>,
    val quickEntries: List<PreviewQuickEntryUiModel>,
    val recommendedLessons: List<PreviewRecommendedLessonUiModel>,
    val uploadCard: PreviewUploadCardUiModel,
)

data class PreviewQuickTopicUiModel(
    val id: Int,
    val emoji: String,
    val label: String,
)

enum class PreviewQuickEntryTone {
    Sky,
    Warm,
}

data class PreviewQuickEntryUiModel(
    val id: Int,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val tone: PreviewQuickEntryTone,
)

data class PreviewRecommendedLessonUiModel(
    val id: Int,
    val tag: String,
    val grade: String,
    val title: String,
    val description: String,
    val masteryPercent: Int,
)

enum class PreviewUploadStage {
    Idle,
    Processing,
    Success,
}

data class PreviewUploadCardUiModel(
    val stage: PreviewUploadStage,
    val title: String,
    val description: String,
    val fileName: String?,
    val helperText: String?,
)

fun previewMockUiState(
    uploadStage: PreviewUploadStage,
    uploadedFileName: String?,
): PreviewUiState {
    val uploadCard = when (uploadStage) {
        PreviewUploadStage.Idle -> PreviewUploadCardUiModel(
            stage = uploadStage,
            title = "上传讲义 / 教材",
            description = "AI 自动提取知识点，生成专属预习计划，智能追问引导。",
            fileName = null,
            helperText = null,
        )

        PreviewUploadStage.Processing -> PreviewUploadCardUiModel(
            stage = uploadStage,
            title = "AI 正在分析讲义",
            description = "文档已接收，正在提取知识点和重点公式。",
            fileName = uploadedFileName,
            helperText = null,
        )

        PreviewUploadStage.Success -> PreviewUploadCardUiModel(
            stage = uploadStage,
            title = "上传成功！🎉",
            description = "讲义已经完成初步整理，可以开始今日预习。",
            fileName = uploadedFileName,
            helperText = null,
        )
    }

    return PreviewUiState(
        userName = "小明同学",
        streakDays = 7,
        weeklyPreviewedLessons = 3,
        weeklyGoalCurrent = 3,
        weeklyGoalTarget = 5,
        quickTopics = listOf(
            PreviewQuickTopicUiModel(1, "📐", "图形面积"),
            PreviewQuickTopicUiModel(2, "🔢", "分数运算"),
            PreviewQuickTopicUiModel(3, "🚂", "行程问题"),
            PreviewQuickTopicUiModel(4, "📊", "数据统计"),
            PreviewQuickTopicUiModel(5, "🧮", "方程"),
        ),
        quickEntries = listOf(
            PreviewQuickEntryUiModel(1, "📷", "拍照提问", "一步步引导你想", PreviewQuickEntryTone.Sky),
            PreviewQuickEntryUiModel(2, "📚", "选知识点", "系统性预习", PreviewQuickEntryTone.Warm),
        ),
        recommendedLessons = listOf(
            PreviewRecommendedLessonUiModel(
                id = 1,
                tag = "应用题",
                grade = "六年级下册",
                title = "行程问题基础",
                description = "理解速度、时间和路程的关系，掌握线段图画法。",
                masteryPercent = 42,
            ),
            PreviewRecommendedLessonUiModel(
                id = 2,
                tag = "计算",
                grade = "六年级上册",
                title = "分数乘除法",
                description = "理解分数乘法意义，掌握基础计算方法。",
                masteryPercent = 75,
            ),
        ),
        uploadCard = uploadCard,
    )
}