package com.bluetutor.android.feature.practice.data

import android.content.Context

suspend fun <T> withRecoveredReport(
    context: Context,
    reportId: String,
    block: suspend (resolvedReportId: String) -> T,
): T {
    val resolvedReportId = PracticeLocalCache.readReportAlias(context, reportId) ?: reportId
    return try {
        block(resolvedReportId)
    } catch (error: Exception) {
        if (!shouldRecoverMissingReport(error)) {
            throw error
        }
        val cachedDetail = PracticeLocalCache.readReportDetail(context, reportId)
            ?: PracticeLocalCache.readReportDetail(context, resolvedReportId)
            ?: throw error
        val ingestResult = MistakesApiClient.ingestCachedReport(cachedDetail)
        PracticeLocalCache.saveReportAlias(context, reportId, ingestResult.reportId)
        block(ingestResult.reportId)
    }
}

fun shouldRecoverMissingReport(error: Throwable): Boolean {
    val message = error.message.orEmpty()
    return message.contains("不存在") ||
        message.contains("找不到") ||
        message.contains("not found", ignoreCase = true)
}
