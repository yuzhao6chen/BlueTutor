package com.bluetutor.android.feature.solve.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SolveHistoryEntryUiModel(
    val sessionId: String,
    val title: String,
    val subtitle: String,
    val statusTag: String,
    val updatedAtMillis: Long,
    val isSolved: Boolean,
)

data class SolveSessionCacheSnapshot(
    val sessionId: String,
    val problemText: String,
    val parsedKnownConditions: List<String>,
    val goal: String,
    val referenceAnswer: String,
    val dialogueHistory: List<GuideDialogueMessage>,
    val thinkingTree: Map<String, GuideThinkingNodeResult>,
    val currentStuckNodeId: String?,
    val stuckCount: Int,
    val lastUpdatedNodeId: String?,
    val isSolved: Boolean,
    val draftInput: String,
    val updatedAtMillis: Long,
)

object SolveLocalCache {
    private const val preferencesName = "solve_local_cache_v1"
    private const val historyKey = "solve_history"

    fun readSessionCache(context: Context, sessionId: String): SolveSessionCacheSnapshot? {
        val rawValue = context
            .getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(sessionKey(sessionId), null)
            ?: return null

        return runCatching {
            val root = JSONObject(rawValue)
            val dialogueHistoryJson = root.optJSONArray("dialogueHistory") ?: JSONArray()
            val thinkingTreeJson = root.optJSONArray("thinkingTree") ?: JSONArray()
            SolveSessionCacheSnapshot(
                sessionId = root.optString("sessionId").ifBlank { sessionId },
                problemText = root.optString("problemText"),
                parsedKnownConditions = readStringList(root.optJSONArray("parsedKnownConditions")),
                goal = root.optString("goal"),
                referenceAnswer = root.optString("referenceAnswer"),
                dialogueHistory = buildList {
                    for (index in 0 until dialogueHistoryJson.length()) {
                        val item = dialogueHistoryJson.optJSONObject(index) ?: continue
                        add(
                            GuideDialogueMessage(
                                role = item.optString("role"),
                                content = item.optString("content"),
                            ),
                        )
                    }
                },
                thinkingTree = buildMap {
                    for (index in 0 until thinkingTreeJson.length()) {
                        val item = thinkingTreeJson.optJSONObject(index) ?: continue
                        val nodeId = item.optString("nodeId").ifBlank { continue }
                        put(
                            nodeId,
                            GuideThinkingNodeResult(
                                nodeId = nodeId,
                                content = item.optString("content"),
                                status = item.optString("status"),
                                parentId = item.optString("parentId").ifBlank { null },
                                errorHistory = readStringList(item.optJSONArray("errorHistory")),
                                children = readStringList(item.optJSONArray("children")),
                            ),
                        )
                    }
                },
                currentStuckNodeId = root.optString("currentStuckNodeId").ifBlank { null },
                stuckCount = root.optInt("stuckCount", 0),
                lastUpdatedNodeId = root.optString("lastUpdatedNodeId").ifBlank { null },
                isSolved = root.optBoolean("isSolved", false),
                draftInput = root.optString("draftInput"),
                updatedAtMillis = root.optLong("updatedAtMillis", 0L),
            )
        }.getOrNull()
    }

    fun saveSessionCache(context: Context, snapshot: SolveSessionCacheSnapshot) {
        val root = JSONObject()
            .put("sessionId", snapshot.sessionId)
            .put("problemText", snapshot.problemText)
            .put("goal", snapshot.goal)
            .put("referenceAnswer", snapshot.referenceAnswer)
            .put("currentStuckNodeId", snapshot.currentStuckNodeId)
            .put("stuckCount", snapshot.stuckCount)
            .put("lastUpdatedNodeId", snapshot.lastUpdatedNodeId)
            .put("isSolved", snapshot.isSolved)
            .put("draftInput", snapshot.draftInput)
            .put("updatedAtMillis", snapshot.updatedAtMillis)
            .put(
                "parsedKnownConditions",
                JSONArray().apply { snapshot.parsedKnownConditions.forEach(::put) },
            )
            .put(
                "dialogueHistory",
                JSONArray().apply {
                    snapshot.dialogueHistory.takeLast(30).forEach { message ->
                        put(
                            JSONObject()
                                .put("role", message.role)
                                .put("content", message.content),
                        )
                    }
                },
            )
            .put(
                "thinkingTree",
                JSONArray().apply {
                    snapshot.thinkingTree.values.forEach { node ->
                        put(
                            JSONObject()
                                .put("nodeId", node.nodeId)
                                .put("content", node.content)
                                .put("status", node.status)
                                .put("parentId", node.parentId)
                                .put("errorHistory", JSONArray().apply { node.errorHistory.forEach(::put) })
                                .put("children", JSONArray().apply { node.children.forEach(::put) }),
                        )
                    }
                },
            )

        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(sessionKey(snapshot.sessionId), root.toString())
            .apply()
    }

    fun readHistory(context: Context): List<SolveHistoryEntryUiModel> {
        val rawValue = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .getString(historyKey, null)
            ?: return emptyList()

        return runCatching {
            val itemsJson = JSONArray(rawValue)
            buildList {
                for (index in 0 until itemsJson.length()) {
                    val item = itemsJson.optJSONObject(index) ?: continue
                    add(
                        SolveHistoryEntryUiModel(
                            sessionId = item.optString("sessionId"),
                            title = item.optString("title"),
                            subtitle = item.optString("subtitle"),
                            statusTag = item.optString("statusTag"),
                            updatedAtMillis = item.optLong("updatedAtMillis", 0L),
                            isSolved = item.optBoolean("isSolved", false),
                        ),
                    )
                }
            }.filter { it.sessionId.isNotBlank() }
                .sortedByDescending { it.updatedAtMillis }
        }.getOrElse { emptyList() }
    }

    fun upsertHistoryEntry(context: Context, entry: SolveHistoryEntryUiModel) {
        val nextEntries = buildList {
            add(entry)
            readHistory(context)
                .filterNot { it.sessionId == entry.sessionId }
                .forEach(::add)
        }.take(30)

        val historyJson = JSONArray().apply {
            nextEntries.forEach { item ->
                put(
                    JSONObject()
                        .put("sessionId", item.sessionId)
                        .put("title", item.title)
                        .put("subtitle", item.subtitle)
                        .put("statusTag", item.statusTag)
                        .put("updatedAtMillis", item.updatedAtMillis)
                        .put("isSolved", item.isSolved),
                )
            }
        }

        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit()
            .putString(historyKey, historyJson.toString())
            .apply()
    }

    private fun readStringList(array: JSONArray?): List<String> = buildList {
        if (array == null) return@buildList
        for (index in 0 until array.length()) {
            val value = array.optString(index).trim()
            if (value.isNotEmpty()) {
                add(value)
            }
        }
    }

    private fun sessionKey(sessionId: String): String = "solve_session_$sessionId"
}