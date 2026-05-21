package com.lzm.funchub.registry

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

interface FeatureEntry {
    val id: String
    val name: String
    val icon: ImageVector
    val route: String
        get() = id

    @Composable
    fun Screen(onBack: () -> Unit = {})
}
