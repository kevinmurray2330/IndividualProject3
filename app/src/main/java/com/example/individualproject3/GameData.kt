package com.example.individualproject3

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// Data Models

data class User(val name: String, val pass: String, val isParent: Boolean)
data class GameLog(val childName: String, val level: String, val success: Boolean, val timestamp: Long)

// Level Data

data class LevelData(
    val id: Int,
    val difficulty: Int, // 1 or 2
    val gridRows: Int,
    val gridCols: Int,
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int,
    val obstacles: List<Pair<Int, Int>>
)

object GameContent {
    // 2 Levels, 3 Games (Stages) each.
    val levels = listOf(
        // Difficulty 1
        LevelData(1, 1, 5, 5, 0, 0, 0, 4, listOf(1 to 0, 1 to 1, 1 to 2, 1 to 3, 1 to 4)), // Top row path
        LevelData(2, 1, 5, 5, 2, 0, 2, 4, listOf(0 to 0, 1 to 0, 3 to 0, 4 to 0)), // Middle row
        LevelData(3, 1, 4, 4, 0, 0, 3, 3, listOf(0 to 1, 1 to 0, 2 to 3, 3 to 2)), // Diagonal-ish
        // Difficulty 2
        LevelData(4, 2, 6, 6, 0, 0, 5, 5, listOf(0 to 1, 0 to 2, 1 to 1, 2 to 2, 3 to 3, 4 to 4)),
        LevelData(5, 2, 6, 6, 5, 0, 0, 5, listOf(4 to 0, 4 to 1, 4 to 2, 2 to 3, 2 to 4, 2 to 5)),
        LevelData(6, 2, 5, 5, 2, 2, 4, 4, listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4))
    )
}

object FileHelper {
    private const val USERS_FILE = "users.txt"
    private const val LOGS_FILE = "logs.txt"

    fun saveUser(context: Context, user: User) {
        val file = File(context.filesDir, USERS_FILE)
        file.appendText("${user.name},${user.pass},${user.isParent}\n")
    }

    fun loadUsers(context: Context): List<User> {
        val file = File(context.filesDir, USERS_FILE)
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size == 3) User(parts[0], parts[1], parts[2].toBoolean()) else null
        }
    }

    fun logProgress(context: Context, log: GameLog) {
        val file = File(context.filesDir, LOGS_FILE)
        file.appendText("${log.childName},${log.level},${log.success},${log.timestamp}\n")
    }

    fun loadLogs(context: Context): List<GameLog> {
        val file = File(context.filesDir, LOGS_FILE)
        if (!file.exists()) return emptyList()
        return file.readLines().mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size == 4) GameLog(parts[0], parts[1], parts[2].toBoolean(), parts[3].toLong()) else null
        }
    }
}