package persistence

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import models.Player
import models.PlayerProgress
import models.Stats
import java.io.File

class ProgressManager {
    private val saveFile = File(System.getProperty("user.home"), ".async-adventure-save.json")
    private val json = Json { prettyPrint = true }

    fun saveProgress(player: Player) {
        try {
            val progress = player.toProgress()
            val jsonString = json.encodeToString(progress)
            saveFile.writeText(jsonString)
        } catch (e: Exception) {
            println("Failed to save progress: ${e.message}")
        }
    }

    fun loadProgress(): PlayerProgress? {
        return try {
            if (saveFile.exists()) {
                val jsonString = saveFile.readText()
                json.decodeFromString<PlayerProgress>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            println("Failed to load progress: ${e.message}")
            null
        }
    }

    fun hasSaveFile(): Boolean {
        return saveFile.exists()
    }

    fun deleteSave() {
        if (saveFile.exists()) {
            saveFile.delete()
        }
    }
}
