package com.lzm.funchub

import android.app.Application
import com.lzm.funchub.features.todo.TodoFeature
import com.lzm.funchub.features.todo.data.TodoRepository
import com.lzm.funchub.registry.FeatureRegistry

class CCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TodoRepository.init(this)
        FeatureRegistry.register(TodoFeature)
    }
}
