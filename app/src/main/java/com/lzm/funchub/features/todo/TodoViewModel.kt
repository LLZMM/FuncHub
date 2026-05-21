package com.lzm.funchub.features.todo

import androidx.lifecycle.ViewModel
import com.lzm.funchub.features.todo.data.TodoRepository

class TodoViewModel : ViewModel() {
    val todos = TodoRepository.todos

    fun addTodo(title: String) {
        if (title.isBlank()) return
        TodoRepository.add(title.trim())
    }

    fun toggleTodo(id: Long) {
        TodoRepository.toggle(id)
    }

    fun deleteTodo(id: Long) {
        TodoRepository.delete(id)
    }

    fun moveTodo(fromIndex: Int, toIndex: Int, filter: TodoFilter) {
        if (filter != TodoFilter.ALL) return
        TodoRepository.move(fromIndex, toIndex)
    }
}
