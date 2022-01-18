package com.example.testapplication.views

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.google.android.material.math.MathUtils.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@ExperimentalMaterialApi
@Composable
fun <T> CardStack(
    modifier: Modifier = Modifier,
    items: MutableList<T>,
    maxElements: Int = 3,
    content: @Composable (T) -> Unit
) {
    var selectedIndex by remember {
        mutableStateOf(items.size - 1)
    }
    val config = LocalConfiguration.current
    val screenWidth = with(LocalDensity.current) {
        config.screenWidthDp.dp.toPx()
    }
    val scope = rememberCoroutineScope()

    val dragManager =
        rememberDragManager(
            size = items.size,
            screenWidth = screenWidth,
            scope = scope,
            maxElements = maxElements
        )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            LaunchedEffect(key1 = Unit, block = {
                dragManager.setBoxWidth(with(density) { maxWidth.value.dp.toPx() })
            })
            items
                .asReversed()
                .forEachIndexed { index, item ->
                    if (dragManager.listOfDragState.isNotEmpty()) {
                        val swipeState = dragManager.listOfDragState[index]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleY = swipeState.scale.value
                                    scaleX = swipeState.scale.value
                                    translationX = swipeState.offsetX.value
                                    translationY = swipeState.offsetY.value
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            dragManager.onDragEnd(
                                                index = index,
                                                selectedIndex = selectedIndex
                                            ) {
                                                selectedIndex -= 1
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            val original = Offset(
                                                x = swipeState.offsetX.value,
                                                y = swipeState.offsetY.value
                                            )
                                            val summed = original + dragAmount
                                            change.consumePositionChange()
                                            if (dragAmount.x > 0) {
                                                dragManager.swipeRight(
                                                    index = index,
                                                    dragAmountX = summed.x
                                                )
                                            }
                                            dragManager.performDrag(
                                                dragAmountY = summed.y,
                                                dragAmountX = summed.x,
                                                dragIndex = index,
                                                selectedIndex = selectedIndex
                                            )
                                        }
                                    )
                                },
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                content(item)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(alpha = swipeState.opacity.value)
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(percent = 10)
                                    )
                            )
                        }
                    }
                }
        }
    }
}

@Composable
fun rememberDragManager(size: Int, screenWidth: Float, scope: CoroutineScope, maxElements: Int) =
    remember {
        DragManager(
            size = size,
            screenWidth = screenWidth,
            scope = scope,
            maxElements = maxElements
        )
    }

open class DragManager(
    val size: Int,
    private val screenWidth: Float,
    private val scope: CoroutineScope,
    private val maxElements: Int
) {

    var listOfDragState: List<DragState> = listOf()
        private set

    private val maxCards = maxElements

    private var scale: List<Float> = listOf()

    private var opacity: List<Float> = listOf()

    private var offsetX: List<Float> = listOf()

    private var boxWidth: Float = 0f

    init {
        //Initializing drag states for all the cards
        initList()
        initProperty()
        initView()
    }

    fun setBoxWidth(boxWidth: Float) {
        Log.d("TAG", "CALLED!!!")
        this.boxWidth = boxWidth
        initProperty()
        initView()
    }

    private fun initView() {
        scope.launch {
            listOfDragState.asReversed().mapIndexed { index, dragState ->
                when {
                    index >= maxCards -> {
                        return@mapIndexed
                    }
                    else -> dragState.snap(
                        scaleP = scale[index],
                        opacityP = opacity[index],
                        offsetXP = offsetX[index]
                    )
                }
            }
        }
    }

    private fun initProperty() {
        val scale = mutableListOf<Float>()
        val opacity = mutableListOf<Float>()
        val offsetX = mutableListOf<Float>()
        val scaleGap = CARD_STACK_SCALE_FACTOR
        val opacityGap = CARD_STACK_OPACITY_GAP
        val offsetGap = scaleGap * boxWidth
        for (i in 0 until maxCards) {
            scale.add(1f - i * scaleGap)
            opacity.add(i * opacityGap)
            offsetX.add(i * offsetGap)
        }
        this.scale = scale
        this.opacity = opacity
        this.offsetX = offsetX
    }

    /***
     * Initializing the list of drag state
     */
    private fun initList() {
        listOfDragState =
            List(size = size) { DragState(index = it, screenWidth = screenWidth, scope = scope) }
    }

    fun performDrag(dragAmountX: Float, dragAmountY: Float, dragIndex: Int, selectedIndex: Int) =
        scope.launch {
            if (dragIndex != selectedIndex) return@launch
            if (dragAmountX > 0) return@launch
            launch {
                //Only the top item should be removed from deck otherwise it will not respond
                val dragState = listOfDragState[dragIndex]
                dragState.drag(dragAmountX = dragAmountX, dragAmountY = dragAmountY)
            }
            for ((counter, i) in (dragIndex - 1 downTo (dragIndex - maxCards + 1).coerceAtLeast(0)).withIndex()) {
                val startIndex = counter + 1
                val endIndex = counter
                val dragFraction = abs(dragAmountX).div(screenWidth / 2).coerceIn(0f, 1f)
                val scaleP =
                    lerp(scale[startIndex], scale[endIndex], abs(dragFraction)) //[1f,0.8f,0.6f]
                val opacityP =
                    lerp(opacity[startIndex], opacity[endIndex], abs(dragFraction)) //[0f,0.2f,0.4f]
                val offsetXP = lerp(offsetX[startIndex], offsetX[endIndex], abs(dragFraction)) //[]
                launch {
                    listOfDragState[i].snap(
                        scaleP = scaleP,
                        opacityP = opacityP,
                        offsetXP = offsetXP
                    )
                }
            }
        }

    fun onDragEnd(index: Int, selectedIndex: Int, onDismiss: () -> Unit) {
        if (index != selectedIndex) return
        val swipeState = listOfDragState[index]
        when {
            abs(swipeState.offsetX.targetValue) < screenWidth / 2 -> {
                swipeState
                    .positionToCenter() {
                        returnToEquilibium(index = index)
                    }
            }
            abs(swipeState.offsetX.targetValue) > 0 -> swipeState
                .animateOutsideOfScreen() {
                    if (index - maxCards >= 0) {
                        scope.launch {
                            val lastElement = listOfDragState[index - maxCards]
                            lastElement.animateTo(
                                scaleP = scale[maxCards - 1],
                                opacityP = opacity[maxCards - 1],
                                offsetXP = offsetX[maxCards - 1]
                            )
                        }
                    }
                }
                .invokeOnCompletion {
                    onDismiss()
                }
            abs(swipeState.offsetX.targetValue) < 0 -> {
                swipeState.positionToCenter() {
                    returnToEquilibium(index = index)
                }
            }
        }
    }

    private suspend fun returnToEquilibium(index: Int) = scope.launch {
        for ((counter, i) in (index - 1 downTo (index - maxCards + 1).coerceAtLeast(0)).withIndex()) {
            listOfDragState[i].animateTo(
                scaleP = scale[counter + 1],
                opacityP = opacity[counter + 1],
                offsetXP = offsetX[counter + 1]
            )
        }
    }

    fun swipeRight(index: Int, dragAmountX: Float) = scope.launch {
        if (dragAmountX < 0) return@launch
        val prevItemIndex = (index - 1).coerceAtLeast(0)
    }

    fun swipeLeft(index: Int){

    }
}

enum class DragDirection {
    Forward,
    Reverse
}

open class DragState(
    val index: Int,
    val screenWidth: Float,
    private val scope: CoroutineScope
) {


    var relativeOffsetX = Animatable(0f)
        private set

    var opacity = Animatable(1f)
        private set

    var offsetX = Animatable(0f)
        private set
    var offsetY = Animatable(0f)
        private set
    var scale = Animatable(0f)
        private set

    init {

    }

    suspend fun drag(dragAmountX: Float, dragAmountY: Float) {
        offsetX.snapTo(dragAmountX)
    }

    fun positionToCenter(onParallel: suspend () -> Unit = {}) = scope.launch {
        launch { offsetX.animateTo(0f) }
        launch { offsetY.animateTo(0f) }
        launch { onParallel() }
    }

    fun animateOutsideOfScreen(onParallel: suspend () -> Unit = {}) = scope.launch {
        launch {
            offsetX.animateTo(-screenWidth)
        }
        launch {
            onParallel()
        }
    }

    suspend fun snap(scaleP: Float, opacityP: Float, offsetXP: Float) = scope.launch {
        launch { scale.snapTo(scaleP) }
        launch { opacity.snapTo(opacityP) }
        launch { offsetX.snapTo(offsetXP) }
    }

    suspend fun animateTo(scaleP: Float, opacityP: Float, offsetXP: Float) = scope.launch {
        launch { scale.animateTo(scaleP) }
        launch { opacity.animateTo(opacityP) }
        launch { offsetX.animateTo(offsetXP) }
    }

}
