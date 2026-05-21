package com.lzm.funchub.features.todo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.lzm.funchub.registry.FeatureEntry

object TodoFeature : FeatureEntry {
    override val id: String = "todo"
    override val name: String = "待办事项"
    override val icon: ImageVector = Icons.Default.CheckCircle
    override val route: String = "todo"

    @Composable
    override fun Screen(onBack: () -> Unit) {
        TodoScreen(onBack = onBack)
    }
}
