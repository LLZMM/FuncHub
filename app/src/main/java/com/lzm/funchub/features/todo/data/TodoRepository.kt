package com.lzm.funchub.features.todo.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TodoRepository {
    private var file: File? = null
    private val _todos = MutableStateFlow<List<Todo>>(emptyList())
    val todos: StateFlow<List<Todo>> = _todos

    private var nextId = 1L

    fun init(context: Context) {
        if (file != null) return
        file = File(context.filesDir, "todos.json")
        _todos.value = loadFromFile()
        nextId = (_todos.value.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun add(title: String) {
        val todo = Todo(id = nextId++, title = title)
        _todos.update { it + todo }
        saveToFile()
    }

    fun toggle(id: Long) {
        _todos.update { list ->
            list.map { if (it.id == id) it.copy(isCompleted = !it.isCompleted) else it }
        }
        saveToFile()
    }

    fun delete(id: Long) {
        _todos.update { list -> list.filter { it.id != id } }
        saveToFile()
    }

    fun move(fromIndex: Int, toIndex: Int) {
        _todos.update { list ->
            val mutable = list.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            mutable
        }
        saveToFile()
    }

    private fun loadFromFile(): List<Todo> {
        return try {
            val f = file ?: return emptyList()
            if (!f.exists()) return emptyList()
            val json = f.readText().trim()
            if (json.isEmpty()) return emptyList()
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Todo(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    isCompleted = obj.getBoolean("isCompleted"),
                    dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate"))
                        obj.getLong("dueDate") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToFile() {
        try {
            val f = file ?: return
            val array = JSONArray()
            _todos.value.forEach { todo ->
                array.put(JSONObject().apply {
                    put("id", todo.id)
                    put("title", todo.title)
                    put("isCompleted", todo.isCompleted)
                    put("dueDate", todo.dueDate ?: JSONObject.NULL)
                    put("createdAt", todo.createdAt)
                })
            }
            f.writeText(array.toString())
        } catch (_: Exception) {}
    }
}
