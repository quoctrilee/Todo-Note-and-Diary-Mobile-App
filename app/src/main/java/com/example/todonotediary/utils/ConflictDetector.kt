package com.example.todonotediary.utils

import android.util.Log

data class ConflictInfo(
    val entityId: String,
    val entityType: String, // "todo", "note", "diary"
    val localUpdatedAt: Long,
    val remoteUpdatedAt: Long,
    val hasConflict: Boolean
) {
    fun getConflictMessage(): String {
        return when {
            !hasConflict -> "No conflict detected"
            localUpdatedAt > remoteUpdatedAt -> "Local version is newer (local: ${formatTimestamp(localUpdatedAt)}, remote: ${formatTimestamp(remoteUpdatedAt)})"
            else -> "Remote version is newer (local: ${formatTimestamp(localUpdatedAt)}, remote: ${formatTimestamp(remoteUpdatedAt)})"
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        return java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
    }
}

object ConflictDetector {
    private const val TAG = "ConflictDetector"
    private const val CONFLICT_THRESHOLD_MS = 5000L // 5 seconds

    /**
     * Detect if there's a conflict between local and remote entities
     * A conflict occurs when both have been modified and timestamps differ significantly
     */
    fun detectConflict(
        entityId: String,
        entityType: String,
        localUpdatedAt: Long,
        remoteUpdatedAt: Long
    ): ConflictInfo {
        val timeDiff = kotlin.math.abs(localUpdatedAt - remoteUpdatedAt)
        val hasConflict = timeDiff > CONFLICT_THRESHOLD_MS

        if (hasConflict) {
            Log.w(TAG, "Conflict detected for $entityType:$entityId - Local: $localUpdatedAt, Remote: $remoteUpdatedAt, Diff: ${timeDiff}ms")
        }

        return ConflictInfo(
            entityId = entityId,
            entityType = entityType,
            localUpdatedAt = localUpdatedAt,
            remoteUpdatedAt = remoteUpdatedAt,
            hasConflict = hasConflict
        )
    }

    /**
     * Resolve conflict using Last-Write-Wins strategy
     * Returns true if remote should win, false if local should win
     */
    fun resolveConflict(conflictInfo: ConflictInfo): Boolean {
        if (!conflictInfo.hasConflict) {
            return true // No conflict, use remote
        }

        // Last-Write-Wins: newer timestamp wins
        val remoteWins = conflictInfo.remoteUpdatedAt >= conflictInfo.localUpdatedAt
        
        Log.d(TAG, "Conflict resolution for ${conflictInfo.entityType}:${conflictInfo.entityId} - ${if (remoteWins) "Remote" else "Local"} wins")
        
        return remoteWins
    }
}
