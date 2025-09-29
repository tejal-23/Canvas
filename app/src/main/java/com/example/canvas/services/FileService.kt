package com.example.canvas.services


import android.content.Context
import com.example.canvas.models.WhiteboardState
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileService(private val context: Context) {
    private val json = Json { prettyPrint = true }

    fun saveState(state: WhiteboardState): File {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val name = "whiteboard_${formatter.format(Date())}.json"
        val dir = context.getExternalFilesDir("whiteboards") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, name)
        file.writeText(json.encodeToString(state))
        return file
    }

    fun loadState(file: File): WhiteboardState {
        val text = file.readText()
        return json.decodeFromString(text)
    }

    fun listSavedFiles(): List<File> {
        val dir = context.getExternalFilesDir("whiteboards") ?: context.filesDir
        return dir.listFiles()?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }
}
