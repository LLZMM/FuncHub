package com.lzm.funchub.features.todo

import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ernestoyaquello.dragdropswipe.DragDropSwipeDefaults
import com.ernestoyaquello.dragdropswipe.DragDropSwipeLazyColumn
import com.ernestoyaquello.dragdropswipe.DraggableSwipeableItemContent
import com.ernestoyaquello.dragdropswipe.SwipeDirection
import com.lzm.funchub.features.todo.data.Todo
import com.valentinilk.shimmer.shimmer
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnKonfettiListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.text.SimpleDateFormat
import java.util.*

enum class TodoFilter { ALL, ACTIVE, COMPLETED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(onBack: () -> Unit) {
    val viewModel: TodoViewModel = viewModel()
    val allTodos by viewModel.todos.collectAsState()
    var filter by remember { mutableStateOf(TodoFilter.ALL) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var showConfetti by remember { mutableStateOf(false) }

    val displayTodos = remember(filter, allTodos) {
        when (filter) {
            TodoFilter.ALL -> allTodos
            TodoFilter.ACTIVE -> allTodos.filter { !it.isCompleted }
            TodoFilter.COMPLETED -> allTodos.filter { it.isCompleted }
        }
    }
    val doneCount = allTodos.count { it.isCompleted }
    val totalCount = allTodos.size
    val progress = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f

    // 全部完成触发彩带
    LaunchedEffect(progress) {
        if (progress == 1f && totalCount > 0) showConfetti = true
    }

    val confettiColors = listOf(
        AndroidColor.RED, AndroidColor.BLUE, AndroidColor.GREEN,
        AndroidColor.YELLOW, AndroidColor.MAGENTA, AndroidColor.CYAN
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("待办事项") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("新增") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ── 进度卡片 + 微光效果 ──
                if (totalCount > 0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shimmer(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "完成进度",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "$doneCount / $totalCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(MaterialTheme.shapes.small),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            if (progress == 1f) {
                                Spacer(Modifier.height(4.dp))
                                Text("全部完成！🎉", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // ── 筛选标签 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filter == TodoFilter.ALL,
                        onClick = { filter = TodoFilter.ALL },
                        label = { Text("全部") }
                    )
                    FilterChip(
                        selected = filter == TodoFilter.ACTIVE,
                        onClick = { filter = TodoFilter.ACTIVE },
                        label = { Text("进行中") }
                    )
                    FilterChip(
                        selected = filter == TodoFilter.COMPLETED,
                        onClick = { filter = TodoFilter.COMPLETED },
                        label = { Text("已完成") }
                    )
                }

                // ── 列表（拖拽排序 + 侧滑删除） ──
                if (displayTodos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (filter) {
                                    TodoFilter.COMPLETED -> Icons.Default.CheckCircle
                                    TodoFilter.ACTIVE -> Icons.Default.TaskAlt
                                    TodoFilter.ALL -> Icons.Default.Inbox
                                },
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                when (filter) {
                                    TodoFilter.COMPLETED -> "还没有已完成的任务"
                                    TodoFilter.ACTIVE -> "所有任务都完成啦！"
                                    TodoFilter.ALL -> "点击下方按钮添加你的第一个任务"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val listState = rememberLazyListState()

                    DragDropSwipeLazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        onItemDragStart = { _ -> },
                        onItemDragEnd = { fromIndex, toIndex ->
                            viewModel.moveTodo(fromIndex, toIndex, filter)
                        },
                        onItemSwiped = { index, direction ->
                            if (direction == SwipeDirection.EndToStart) {
                                viewModel.deleteTodo(displayTodos[index].id)
                            } else if (direction == SwipeDirection.StartToEnd) {
                                viewModel.toggleTodo(displayTodos[index].id)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        dragDropSwipeDefaults = DragDropSwipeDefaults(
                            swipeStartDirection = SwipeDirection.EndToStart,
                            swipeEndDirection = SwipeDirection.StartToEnd,
                            swipeStartIcon = Icons.Default.Delete,
                            swipeEndIcon = Icons.Default.Check,
                            swipeStartBackgroundColor = MaterialTheme.colorScheme.errorContainer,
                            swipeEndBackgroundColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        items(displayTodos, key = { it.id }) { todo ->
                            DraggableSwipeableItemContent {
                                TodoCard(
                                    todo = todo,
                                    onToggle = { viewModel.toggleTodo(todo.id) },
                                    onDelete = { viewModel.deleteTodo(todo.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 彩带庆祝特效 ──
        if (showConfetti) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = listOf(
                    Party(
                        angle = 270,
                        spread = 90,
                        speed = 0.5f,
                        maxSpeed = 2f,
                        timeToLive = 2000,
                        position = Position.Relative(0.5, 0.0),
                        emitter = Emitter(duration = 2000).max(200),
                        colors = confettiColors
                    )
                ),
                onKonfettiListener = object : OnKonfettiListener {
                    override fun onKonfettiEnded() {
                        showConfetti = false
                    }
                }
            )
        }
    }

    // ── 新增对话框 ──
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newTitle = "" },
            icon = { Icon(Icons.Default.EditNote, null) },
            title = { Text("新建任务") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("任务内容") },
                    placeholder = { Text("比如：下班买菜") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addTodo(newTitle)
                    showAddDialog = false
                    newTitle = ""
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newTitle = "" }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun TodoCard(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (todo.isCompleted)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (todo.isCompleted)
                        TextDecoration.LineThrough else TextDecoration.None,
                    color = if (todo.isCompleted)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                todo.dueDate?.let { due ->
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formatDueDate(due),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatDueDate(epochMillis: Long): String {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val tomorrow = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
    }
    val dueCal = Calendar.getInstance().apply { timeInMillis = epochMillis }
    dueCal.set(Calendar.HOUR_OF_DAY, 0); dueCal.set(Calendar.MINUTE, 0)

    return when {
        dueCal.timeInMillis == today.timeInMillis -> "今天截止"
        dueCal.timeInMillis == tomorrow.timeInMillis -> "明天截止"
        else -> SimpleDateFormat("M月d日", Locale.CHINESE).format(Date(epochMillis))
    }
}
