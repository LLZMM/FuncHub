# 今天吃什么轮盘 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a new “今天吃什么” feature that lets the user spin a warm cartoon-style meal wheel for lunch or dinner, manage meal options, mark favorites, and temporarily exclude meals for today.

**Architecture:** Add one isolated feature package under `features/mealwheel`, following the existing `FeatureEntry` + `FeatureRegistry` pattern. Business rules live in `MealWheelRepository`, UI state lives in `MealWheelViewModel`, and Compose UI lives in `MealWheelScreen`. Persistence uses a local `meal_options.json` file, matching the lightweight Todo repository style.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Compose Canvas animation, StateFlow/ViewModel, local JSON file persistence, JUnit 4 unit tests.

---

## File Structure

Create or modify these files only:

- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/data/MealOption.kt`
  - Defines `MealType` and `MealOption`.
- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/data/MealWheelRepository.kt`
  - Owns default meals, JSON persistence, list mutations, candidate filtering, and weighted random selection.
- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelViewModel.kt`
  - Owns screen state and forwards user actions to the repository.
- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelFeature.kt`
  - Exposes the feature through `FeatureEntry`.
- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelScreen.kt`
  - Renders the wheel, meal type switch, candidate summary, result dialog, and management sheet.
- Modify: `app/src/main/java/com/lzm/funchub/CCApplication.kt`
  - Initializes the repository and registers `MealWheelFeature`.
- Create: `app/src/test/java/com/lzm/funchub/features/mealwheel/data/MealWheelRepositoryTest.kt`
  - Tests filtering, exclusion, weighting, and empty draw behavior.

Do not modify Todo code, navigation code, build files, or unrelated UI.

---

### Task 1: Add MealOption model and failing repository rule tests

**Files:**
- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/data/MealOption.kt`
- Create: `app/src/test/java/com/lzm/funchub/features/mealwheel/data/MealWheelRepositoryTest.kt`

- [ ] **Step 1: Create the meal model**

Create `app/src/main/java/com/lzm/funchub/features/mealwheel/data/MealOption.kt`:

```kotlin
package com.lzm.funchub.features.mealwheel.data

enum class MealType {
    LUNCH,
    DINNER
}

data class MealOption(
    val id: Long = 0L,
    val name: String,
    val forLunch: Boolean = true,
    val forDinner: Boolean = true,
    val isFavorite: Boolean = false,
    val excludedDate: String? = null
)
```

- [ ] **Step 2: Write failing tests for filtering and weighted selection**

Create `app/src/test/java/com/lzm/funchub/features/mealwheel/data/MealWheelRepositoryTest.kt`:

```kotlin
package com.lzm.funchub.features.mealwheel.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealWheelRepositoryTest {
    @Test
    fun availableOptions_filtersByMealTypeAndTodayExclusion() {
        val today = "2026-05-22"
        val options = listOf(
            MealOption(id = 1, name = "黄焖鸡米饭", forLunch = true, forDinner = false),
            MealOption(id = 2, name = "火锅", forLunch = false, forDinner = true),
            MealOption(id = 3, name = "拉面", forLunch = true, forDinner = true, excludedDate = today),
            MealOption(id = 4, name = "炒饭", forLunch = true, forDinner = true, excludedDate = "2026-05-21")
        )

        val lunch = MealWheelRepository.availableOptions(options, MealType.LUNCH, today)
        val dinner = MealWheelRepository.availableOptions(options, MealType.DINNER, today)

        assertEquals(listOf("黄焖鸡米饭", "炒饭"), lunch.map { it.name })
        assertEquals(listOf("火锅", "炒饭"), dinner.map { it.name })
    }

    @Test
    fun weightedOptions_duplicatesFavoriteMealsOnce() {
        val normal = MealOption(id = 1, name = "盖浇饭", isFavorite = false)
        val favorite = MealOption(id = 2, name = "烤肉饭", isFavorite = true)

        val weighted = MealWheelRepository.weightedOptions(listOf(normal, favorite))

        assertEquals(3, weighted.size)
        assertEquals(1, weighted.count { it.id == normal.id })
        assertEquals(2, weighted.count { it.id == favorite.id })
    }

    @Test
    fun drawOption_returnsNullWhenNoCandidatesRemain() {
        val today = "2026-05-22"
        val options = listOf(
            MealOption(id = 1, name = "麻辣烫", forLunch = true, forDinner = true, excludedDate = today)
        )

        val result = MealWheelRepository.drawOption(
            options = options,
            mealType = MealType.LUNCH,
            today = today,
            random = Random(0)
        )

        assertNull(result)
    }

    @Test
    fun drawOption_onlyReturnsEligibleMeals() {
        val today = "2026-05-22"
        val options = listOf(
            MealOption(id = 1, name = "午餐便当", forLunch = true, forDinner = false),
            MealOption(id = 2, name = "晚餐火锅", forLunch = false, forDinner = true),
            MealOption(id = 3, name = "今日不吃", forLunch = true, forDinner = true, excludedDate = today)
        )

        repeat(20) { seed ->
            val result = MealWheelRepository.drawOption(
                options = options,
                mealType = MealType.LUNCH,
                today = today,
                random = Random(seed)
            )

            assertEquals("午餐便当", result?.name)
        }
    }

    @Test
    fun defaultOptions_includeMealsForLunchAndDinner() {
        val defaults = MealWheelRepository.defaultOptions()

        assertTrue(defaults.any { it.forLunch })
        assertTrue(defaults.any { it.forDinner })
        assertTrue(defaults.size >= 10)
        assertEquals(defaults.size, defaults.map { it.id }.distinct().size)
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run from `FuncHub/`:

```bash
./gradlew :app:testDebugUnitTest --tests "com.lzm.funchub.features.mealwheel.data.MealWheelRepositoryTest"
```

Expected: FAIL because `MealWheelRepository` is not defined.

- [ ] **Step 4: Commit the failing tests and model**

```bash
git add app/src/main/java/com/lzm/funchub/features/mealwheel/data/MealOption.kt app/src/test/java/com/lzm/funchub/features/mealwheel/data/MealWheelRepositoryTest.kt
git commit -m "test: define meal wheel selection rules"
```

---

### Task 2: Implement MealWheelRepository core rules and persistence

**Files:**
- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/data/MealWheelRepository.kt`
- Test: `app/src/test/java/com/lzm/funchub/features/mealwheel/data/MealWheelRepositoryTest.kt`

- [ ] **Step 1: Implement repository rules and local JSON storage**

Create `app/src/main/java/com/lzm/funchub/features/mealwheel/data/MealWheelRepository.kt`:

```kotlin
package com.lzm.funchub.features.mealwheel.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

object MealWheelRepository {
    private const val FILE_NAME = "meal_options.json"

    private var file: File? = null
    private val _options = MutableStateFlow<List<MealOption>>(emptyList())
    val options: StateFlow<List<MealOption>> = _options

    private var nextId = 1L

    fun init(context: Context) {
        if (file != null) return
        val mealFile = File(context.filesDir, FILE_NAME)
        file = mealFile
        val loaded = loadInitialOptions(mealFile)
        _options.value = loaded
        nextId = (loaded.maxOfOrNull { it.id } ?: 0L) + 1L
        if (!mealFile.exists()) {
            saveToFile()
        }
    }

    fun defaultOptions(): List<MealOption> {
        return listOf(
            MealOption(id = 1L, name = "黄焖鸡米饭", forLunch = true, forDinner = true),
            MealOption(id = 2L, name = "兰州拉面", forLunch = true, forDinner = true),
            MealOption(id = 3L, name = "麻辣烫", forLunch = true, forDinner = true),
            MealOption(id = 4L, name = "盖浇饭", forLunch = true, forDinner = false),
            MealOption(id = 5L, name = "炒饭", forLunch = true, forDinner = true),
            MealOption(id = 6L, name = "轻食沙拉", forLunch = true, forDinner = false),
            MealOption(id = 7L, name = "云吞面", forLunch = true, forDinner = true),
            MealOption(id = 8L, name = "烤肉饭", forLunch = true, forDinner = true, isFavorite = true),
            MealOption(id = 9L, name = "火锅", forLunch = false, forDinner = true),
            MealOption(id = 10L, name = "烧烤", forLunch = false, forDinner = true),
            MealOption(id = 11L, name = "饺子", forLunch = true, forDinner = true),
            MealOption(id = 12L, name = "寿司", forLunch = true, forDinner = true)
        )
    }

    fun availableOptions(
        options: List<MealOption>,
        mealType: MealType,
        today: String
    ): List<MealOption> {
        return options.filter { option ->
            val matchesMealType = when (mealType) {
                MealType.LUNCH -> option.forLunch
                MealType.DINNER -> option.forDinner
            }
            matchesMealType && option.excludedDate != today
        }
    }

    fun weightedOptions(options: List<MealOption>): List<MealOption> {
        return options.flatMap { option ->
            List(if (option.isFavorite) 2 else 1) { option }
        }
    }

    fun drawOption(
        options: List<MealOption>,
        mealType: MealType,
        today: String,
        random: Random = Random.Default
    ): MealOption? {
        val weighted = weightedOptions(availableOptions(options, mealType, today))
        if (weighted.isEmpty()) return null
        return weighted[random.nextInt(weighted.size)]
    }

    fun add(name: String) {
        val cleanedName = name.trim()
        if (cleanedName.isBlank()) return
        val option = MealOption(id = nextId++, name = cleanedName)
        _options.update { it + option }
        saveToFile()
    }

    fun delete(id: Long) {
        _options.update { options -> options.filterNot { it.id == id } }
        saveToFile()
    }

    fun updateMealTypes(id: Long, forLunch: Boolean, forDinner: Boolean) {
        _options.update { options ->
            options.map { option ->
                if (option.id == id) {
                    option.copy(forLunch = forLunch, forDinner = forDinner)
                } else {
                    option
                }
            }
        }
        saveToFile()
    }

    fun toggleFavorite(id: Long) {
        _options.update { options ->
            options.map { option ->
                if (option.id == id) option.copy(isFavorite = !option.isFavorite) else option
            }
        }
        saveToFile()
    }

    fun excludeForToday(id: Long, today: String = todayString()) {
        _options.update { options ->
            options.map { option ->
                if (option.id == id) option.copy(excludedDate = today) else option
            }
        }
        saveToFile()
    }

    fun todayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
    }

    private fun loadInitialOptions(mealFile: File): List<MealOption> {
        if (!mealFile.exists()) return defaultOptions()
        return try {
            decodeOptions(mealFile.readText())
        } catch (_: Exception) {
            defaultOptions()
        }
    }

    private fun decodeOptions(json: String): List<MealOption> {
        if (json.trim().isEmpty()) return emptyList()
        val array = JSONArray(json)
        return (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            MealOption(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                forLunch = obj.optBoolean("forLunch", true),
                forDinner = obj.optBoolean("forDinner", true),
                isFavorite = obj.optBoolean("isFavorite", false),
                excludedDate = if (obj.has("excludedDate") && !obj.isNull("excludedDate")) {
                    obj.getString("excludedDate")
                } else {
                    null
                }
            )
        }
    }

    private fun encodeOptions(options: List<MealOption>): String {
        val array = JSONArray()
        options.forEach { option ->
            array.put(JSONObject().apply {
                put("id", option.id)
                put("name", option.name)
                put("forLunch", option.forLunch)
                put("forDinner", option.forDinner)
                put("isFavorite", option.isFavorite)
                put("excludedDate", option.excludedDate ?: JSONObject.NULL)
            })
        }
        return array.toString()
    }

    private fun saveToFile() {
        val mealFile = file ?: return
        try {
            mealFile.writeText(encodeOptions(_options.value))
        } catch (_: Exception) {
        }
    }
}
```

- [ ] **Step 2: Run repository tests**

Run from `FuncHub/`:

```bash
./gradlew :app:testDebugUnitTest --tests "com.lzm.funchub.features.mealwheel.data.MealWheelRepositoryTest"
```

Expected: PASS.

- [ ] **Step 3: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit repository implementation**

```bash
git add app/src/main/java/com/lzm/funchub/features/mealwheel/data/MealWheelRepository.kt
git commit -m "feat: add meal wheel repository"
```

---

### Task 3: Add MealWheelViewModel state and actions

**Files:**
- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelViewModel.kt`

- [ ] **Step 1: Create ViewModel and UI state**

Create `app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelViewModel.kt`:

```kotlin
package com.lzm.funchub.features.mealwheel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzm.funchub.features.mealwheel.data.MealOption
import com.lzm.funchub.features.mealwheel.data.MealType
import com.lzm.funchub.features.mealwheel.data.MealWheelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn


data class MealWheelUiState(
    val mealType: MealType = MealType.LUNCH,
    val options: List<MealOption> = emptyList(),
    val availableOptions: List<MealOption> = emptyList(),
    val isSpinning: Boolean = false,
    val selectedOption: MealOption? = null,
    val resultOption: MealOption? = null,
    val today: String = MealWheelRepository.todayString()
)

class MealWheelViewModel : ViewModel() {
    private val mealType = MutableStateFlow(MealType.LUNCH)
    private val isSpinning = MutableStateFlow(false)
    private val selectedOption = MutableStateFlow<MealOption?>(null)
    private val resultOption = MutableStateFlow<MealOption?>(null)

    val uiState: StateFlow<MealWheelUiState> = combine(
        MealWheelRepository.options,
        mealType,
        isSpinning,
        selectedOption,
        resultOption
    ) { options, currentMealType, spinning, selected, result ->
        val today = MealWheelRepository.todayString()
        MealWheelUiState(
            mealType = currentMealType,
            options = options,
            availableOptions = MealWheelRepository.availableOptions(options, currentMealType, today),
            isSpinning = spinning,
            selectedOption = selected,
            resultOption = result,
            today = today
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MealWheelUiState()
    )

    fun selectMealType(type: MealType) {
        mealType.value = type
        clearResult()
    }

    fun spin(): MealOption? {
        if (isSpinning.value) return null
        val state = uiState.value
        val selected = MealWheelRepository.drawOption(
            options = state.options,
            mealType = state.mealType,
            today = state.today
        ) ?: return null
        selectedOption.value = selected
        resultOption.value = null
        isSpinning.value = true
        return selected
    }

    fun finishSpin() {
        isSpinning.value = false
        resultOption.value = selectedOption.value
    }

    fun clearResult() {
        resultOption.value = null
    }

    fun addMeal(name: String) {
        MealWheelRepository.add(name)
    }

    fun deleteMeal(id: Long) {
        MealWheelRepository.delete(id)
    }

    fun updateMealTypes(id: Long, forLunch: Boolean, forDinner: Boolean) {
        MealWheelRepository.updateMealTypes(id, forLunch, forDinner)
    }

    fun toggleFavorite(id: Long) {
        MealWheelRepository.toggleFavorite(id)
    }

    fun excludeResultForToday() {
        val result = resultOption.value ?: return
        MealWheelRepository.excludeForToday(result.id, uiState.value.today)
        resultOption.value = null
        selectedOption.value = null
    }
}
```

- [ ] **Step 2: Run unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit ViewModel**

```bash
git add app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelViewModel.kt
git commit -m "feat: add meal wheel view model"
```

---

### Task 4: Add feature entry and register the meal wheel

**Files:**
- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelFeature.kt`
- Modify: `app/src/main/java/com/lzm/funchub/CCApplication.kt`

- [ ] **Step 1: Create FeatureEntry implementation**

Create `app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelFeature.kt`:

```kotlin
package com.lzm.funchub.features.mealwheel

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.lzm.funchub.registry.FeatureEntry

object MealWheelFeature : FeatureEntry {
    override val id: String = "meal_wheel"
    override val name: String = "今天吃什么"
    override val icon: ImageVector = Icons.Default.Fastfood
    override val route: String = "meal_wheel"

    @Composable
    override fun Screen(onBack: () -> Unit) {
        MealWheelScreen(onBack = onBack)
    }
}
```

- [ ] **Step 2: Update application registration**

Modify `app/src/main/java/com/lzm/funchub/CCApplication.kt` to exactly:

```kotlin
package com.lzm.funchub

import android.app.Application
import com.lzm.funchub.features.mealwheel.MealWheelFeature
import com.lzm.funchub.features.mealwheel.data.MealWheelRepository
import com.lzm.funchub.features.todo.TodoFeature
import com.lzm.funchub.features.todo.data.TodoRepository
import com.lzm.funchub.registry.FeatureRegistry

class CCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TodoRepository.init(this)
        MealWheelRepository.init(this)
        FeatureRegistry.register(TodoFeature)
        FeatureRegistry.register(MealWheelFeature)
    }
}
```

- [ ] **Step 3: Run compile to verify missing screen is the only blocker**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: FAIL because `MealWheelScreen` is not defined yet.

- [ ] **Step 4: Commit feature registration**

```bash
git add app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelFeature.kt app/src/main/java/com/lzm/funchub/CCApplication.kt
git commit -m "feat: register meal wheel feature"
```

---

### Task 5: Build the meal wheel Compose screen

**Files:**
- Create: `app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelScreen.kt`

- [ ] **Step 1: Create screen with wheel, animation, result dialog, and management sheet**

Create `app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelScreen.kt`:

```kotlin
package com.lzm.funchub.features.mealwheel

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzm.funchub.features.mealwheel.data.MealOption
import com.lzm.funchub.features.mealwheel.data.MealType

private val WheelColors = listOf(
    Color(0xFFFFB74D),
    Color(0xFFFF8A65),
    Color(0xFFFFD54F),
    Color(0xFFAED581),
    Color(0xFF4DD0E1),
    Color(0xFFBA68C8),
    Color(0xFFFFA726),
    Color(0xFFE57373)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealWheelScreen(onBack: () -> Unit) {
    val viewModel: MealWheelViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val wheelRotation = remember { Animatable(0f) }
    var rotationBase by remember { mutableFloatStateOf(0f) }
    var spinVersion by remember { mutableIntStateOf(0) }
    var showManageSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(spinVersion) {
        val selected = state.selectedOption
        val candidates = state.availableOptions
        if (spinVersion > 0 && state.isSpinning && selected != null && candidates.isNotEmpty()) {
            val target = targetRotationFor(selected, candidates, rotationBase)
            wheelRotation.snapTo(rotationBase)
            wheelRotation.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 2600, easing = FastOutSlowInEasing)
            )
            rotationBase = target % 360f
            viewModel.finishSpin()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今天吃什么") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showManageSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "管理餐食")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFF3E0),
                    titleContentColor = Color(0xFF5D4037),
                    navigationIconContentColor = Color(0xFF5D4037),
                    actionIconContentColor = Color(0xFF5D4037)
                )
            )
        },
        containerColor = Color(0xFFFFFBF5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3), Color(0xFFFFFBF5))
                    )
                )
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MealTypeSwitch(
                selected = state.mealType,
                onSelect = viewModel::selectMealType
            )
            Spacer(Modifier.height(18.dp))
            WheelCard(
                options = state.availableOptions,
                rotation = wheelRotation.value,
                isSpinning = state.isSpinning,
                onSpin = {
                    if (viewModel.spin() != null) {
                        spinVersion += 1
                    }
                }
            )
            Spacer(Modifier.height(18.dp))
            CandidateSummary(
                options = state.availableOptions,
                mealType = state.mealType,
                onManageClick = { showManageSheet = true }
            )
        }
    }

    state.resultOption?.let { result ->
        ResultDialog(
            result = result,
            onConfirm = viewModel::clearResult,
            onSpinAgain = {
                viewModel.clearResult()
                if (viewModel.spin() != null) {
                    spinVersion += 1
                }
            },
            onExcludeToday = viewModel::excludeResultForToday
        )
    }

    if (showManageSheet) {
        MealManageSheet(
            options = state.options,
            onDismiss = { showManageSheet = false },
            onAddClick = { showAddDialog = true },
            onDelete = viewModel::deleteMeal,
            onToggleLunch = { option ->
                viewModel.updateMealTypes(option.id, !option.forLunch, option.forDinner)
            },
            onToggleDinner = { option ->
                viewModel.updateMealTypes(option.id, option.forLunch, !option.forDinner)
            },
            onToggleFavorite = { option -> viewModel.toggleFavorite(option.id) }
        )
    }

    if (showAddDialog) {
        AddMealDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name ->
                viewModel.addMeal(name)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MealTypeSwitch(selected: MealType, onSelect: (MealType) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selected == MealType.LUNCH,
            onClick = { onSelect(MealType.LUNCH) },
            label = { Text("午饭") }
        )
        FilterChip(
            selected = selected == MealType.DINNER,
            onClick = { onSelect(MealType.DINNER) },
            label = { Text("晚饭") }
        )
    }
}

@Composable
private fun WheelCard(
    options: List<MealOption>,
    rotation: Float,
    isSpinning: Boolean,
    onSpin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(292.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▼",
                    color = Color(0xFFD84315),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                MealWheelCanvas(
                    options = options,
                    rotation = rotation,
                    modifier = Modifier
                        .size(260.dp)
                        .clip(CircleShape)
                )
                ElevatedButton(
                    onClick = onSpin,
                    enabled = options.isNotEmpty() && !isSpinning,
                    shape = CircleShape,
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(if (isSpinning) "转动中" else "开转")
                }
            }
        }
    }
}

@Composable
private fun MealWheelCanvas(
    options: List<MealOption>,
    rotation: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.background(Color(0xFFFFE0B2))) {
        val radius = size.minDimension / 2f
        val topLeft = Offset((size.width - radius * 2f) / 2f, (size.height - radius * 2f) / 2f)
        val arcSize = Size(radius * 2f, radius * 2f)
        val center = Offset(size.width / 2f, size.height / 2f)

        if (options.isEmpty()) {
            drawCircle(color = Color(0xFFFFCC80), radius = radius, center = center)
            drawContext.canvas.nativeCanvas.drawText(
                "先加几个好吃的",
                center.x,
                center.y,
                Paint().apply {
                    color = Color(0xFF6D4C41).toArgb()
                    textAlign = Paint.Align.CENTER
                    textSize = 16.dp.toPx()
                    isAntiAlias = true
                    isFakeBoldText = true
                }
            )
            return@Canvas
        }

        val sweep = 360f / options.size
        options.forEachIndexed { index, option ->
            val start = rotation - 90f + index * sweep
            drawArc(
                color = WheelColors[index % WheelColors.size],
                startAngle = start,
                sweepAngle = sweep - 1f,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            rotate(degrees = start + sweep / 2f, pivot = center) {
                drawContext.canvas.nativeCanvas.drawText(
                    option.name.take(6),
                    center.x + radius * 0.34f,
                    center.y + 5.dp.toPx(),
                    Paint().apply {
                        color = Color(0xFF4E342E).toArgb()
                        textAlign = Paint.Align.CENTER
                        textSize = 13.dp.toPx()
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                )
            }
        }
        drawCircle(color = Color.White.copy(alpha = 0.86f), radius = radius * 0.28f, center = center)
    }
}

@Composable
private fun CandidateSummary(
    options: List<MealOption>,
    mealType: MealType,
    onManageClick: () -> Unit
) {
    val mealName = if (mealType == MealType.LUNCH) "午饭" else "晚饭"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.82f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$mealName 当前可选：${options.size} 个",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5D4037)
                    )
                    Text(
                        text = if (options.isEmpty()) "今天选择太少啦，去加几个好吃的吧" else "不想吃的可以抽中后临时排除",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF8D6E63)
                    )
                }
                TextButton(onClick = onManageClick) {
                    Text("管理")
                }
            }
            if (options.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(options.take(8), key = { it.id }) { option ->
                        AssistChip(
                            onClick = {},
                            label = { Text(option.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultDialog(
    result: MealOption,
    onConfirm: () -> Unit,
    onSpinAgain: () -> Unit,
    onExcludeToday: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFF8A00)) },
        title = { Text("今天就吃") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = result.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF5D4037),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "选择困难先放一边，这个看起来很合适。",
                    textAlign = TextAlign.Center,
                    color = Color(0xFF8D6E63)
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("就吃这个") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSpinAgain) { Text("再抽一次") }
                TextButton(onClick = onExcludeToday) { Text("今天不吃") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealManageSheet(
    options: List<MealOption>,
    onDismiss: () -> Unit,
    onAddClick: () -> Unit,
    onDelete: (Long) -> Unit,
    onToggleLunch: (MealOption) -> Unit,
    onToggleDinner: (MealOption) -> Unit,
    onToggleFavorite: (MealOption) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("管理餐食", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("新增")
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(options, key = { it.id }) { option ->
                    MealOptionRow(
                        option = option,
                        onDelete = { onDelete(option.id) },
                        onToggleLunch = { onToggleLunch(option) },
                        onToggleDinner = { onToggleDinner(option) },
                        onToggleFavorite = { onToggleFavorite(option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MealOptionRow(
    option: MealOption,
    onDelete: () -> Unit,
    onToggleLunch: () -> Unit,
    onToggleDinner: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (option.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "更想吃",
                        tint = Color(0xFFFFA000)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = option.forLunch,
                    onClick = onToggleLunch,
                    label = { Text("午饭") }
                )
                FilterChip(
                    selected = option.forDinner,
                    onClick = onToggleDinner,
                    label = { Text("晚饭") }
                )
                Surface(
                    color = if (option.isFavorite) Color(0xFFFFECB3) else Color(0xFFECEFF1),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (option.isFavorite) "更想吃" else "普通",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMealDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增餐食") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("餐食名称") },
                placeholder = { Text("比如：牛肉面") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onAdd(name) }) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

private fun targetRotationFor(
    selected: MealOption,
    options: List<MealOption>,
    currentRotation: Float
): Float {
    val index = options.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)
    val sweep = 360f / options.size
    val selectedCenter = index * sweep + sweep / 2f
    val desiredRotation = -90f - selectedCenter
    val normalizedCurrent = ((currentRotation % 360f) + 360f) % 360f
    val normalizedTarget = ((desiredRotation % 360f) + 360f) % 360f
    val delta = (normalizedTarget - normalizedCurrent + 360f) % 360f
    return currentRotation + 1440f + delta
}
```

- [ ] **Step 2: Add missing collectAsState import if Android Studio does not auto-import**

Ensure `MealWheelScreen.kt` includes this import near the other runtime imports:

```kotlin
import androidx.compose.runtime.collectAsState
```

- [ ] **Step 3: Run Kotlin compilation**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. If it fails because `Fastfood` is unavailable in the current material icon artifact, replace both the import and usage in `MealWheelFeature.kt` with `Icons.Default.CheckCircle`, then rerun the command.

- [ ] **Step 4: Commit screen implementation**

```bash
git add app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelScreen.kt app/src/main/java/com/lzm/funchub/features/mealwheel/MealWheelFeature.kt
git commit -m "feat: add meal wheel screen"
```

---

### Task 6: Run full automated verification

**Files:**
- Verify only; no source edits expected after Task 5 unless commands fail.

- [ ] **Step 1: Run unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Build debug APK**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL and APK generated at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 3: Check git status**

```bash
git status --short --branch
```

Expected: only intentional meal wheel files are changed. The existing untracked `app/.gitignore` may still appear and should not be committed unless the user explicitly asks.

- [ ] **Step 4: Commit verification fixes if any were needed**

If Task 6 required source fixes, commit the exact files changed by those fixes:

```bash
git add app/src/main/java/com/lzm/funchub/features/mealwheel app/src/test/java/com/lzm/funchub/features/mealwheel app/src/main/java/com/lzm/funchub/CCApplication.kt
git commit -m "fix: verify meal wheel build"
```

If no fixes were needed, do not create an empty commit.

---

### Task 7: Manual UI verification

**Files:**
- Verify behavior in Android Studio or on an installed debug APK.

- [ ] **Step 1: Start the app on an emulator or connected device**

Use Android Studio Run, or install the debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected: app launches successfully.

- [ ] **Step 2: Verify home entry**

Expected:

- 首页 shows existing “待办事项”.
- 首页 also shows “今天吃什么”.
- Tapping “今天吃什么” opens the wheel page.

- [ ] **Step 3: Verify spinning flow**

Expected:

- The page starts on “午饭”.
- Current candidate count is greater than zero.
- Tapping “开转” disables the button while the wheel spins.
- The wheel slows down and opens a result dialog.
- “就吃这个” closes the result dialog.

- [ ] **Step 4: Verify “再抽一次”**

Expected:

- Tap “开转”.
- In the result dialog, tap “再抽一次”.
- The dialog closes and the wheel spins again.
- A new result dialog appears after the animation.

- [ ] **Step 5: Verify “今天不吃”**

Expected:

- Tap “开转”.
- Note the result name.
- Tap “今天不吃”.
- The result dialog closes.
- The noted result no longer appears in the current candidate chips for the current meal type.

- [ ] **Step 6: Verify meal management**

Expected:

- Tap the top-right settings icon.
- Bottom sheet opens.
- Tap “新增”, enter “测试餐食”, tap “添加”.
- “测试餐食” appears in the management list.
- Toggle 午饭 and 晚饭 chips; candidate count updates after closing the sheet.
- Toggle the star; the row changes between “普通” and “更想吃”.
- Delete “测试餐食”; it disappears from the list.

- [ ] **Step 7: Commit manual verification note only if source changed**

If manual verification uncovered and fixed source issues, commit those fixes:

```bash
git add app/src/main/java/com/lzm/funchub/features/mealwheel app/src/test/java/com/lzm/funchub/features/mealwheel app/src/main/java/com/lzm/funchub/CCApplication.kt
git commit -m "fix: polish meal wheel interactions"
```

If no source fixes were needed, do not create a commit.

---

## Self-Review

Spec coverage:

- Default meal pool: Task 2 `defaultOptions()` and repository initialization.
- Custom add/delete: Task 2 repository actions, Task 5 management sheet.
- Lunch/dinner tags: Task 1 model, Task 2 filtering, Task 5 chips.
- Simple favorite weighting: Task 1 tests, Task 2 weighted selection, Task 5 star toggle.
- Today exclusion: Task 1 tests, Task 2 `excludeForToday`, Task 5 result dialog.
- Result ceremony: Task 5 animated wheel and result dialog.
- Plugin registration: Task 4.
- Automated verification: Task 6.
- Manual UI verification: Task 7.

Placeholder scan: no placeholder markers, incomplete sections, or undefined task references remain.

Type consistency:

- `MealType`, `MealOption`, `MealWheelRepository`, `MealWheelUiState`, `MealWheelViewModel`, `MealWheelFeature`, and `MealWheelScreen` names are consistent across tasks.
- Repository methods called by the ViewModel are defined in Task 2 before Task 3 uses them.
- UI calls only ViewModel methods defined in Task 3.
