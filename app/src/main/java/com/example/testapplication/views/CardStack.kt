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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs


/**
 * This card deck composable is responsible for the swipe enabled animating cards which will give user to swipe through the cards to the left
 * direction and it will swipe animating the view. One of it's core feature is to lazily load the view in to the view hierarchy.
 * @param modifier Modifier to style the container of the card deck
 * @param items items to show
 * @param maxElements Max cards needs to be shown
 * @param content Content then needs to be placed into the container
 * @author Debdut Saha
 */
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
    LaunchedEffect(key1 = Unit, block = {
        var i=1
        while (true) {

            delay(2000)
            dragManager.swipeLeft(items.size-i) {
               // selectedIndex--
            }
            i++
        }

    })

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.8f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            dragManager.onDragEnd(
                                index = selectedIndex,
                                selectedIndex = selectedIndex
                            ) {
                                selectedIndex = (selectedIndex - it).coerceIn(0, items.size - 1)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val swipeState = dragManager.listOfDragState[selectedIndex]
                            val original = Offset(
                                x = swipeState.offsetX.value,
                                y = swipeState.offsetY.value
                            )
                            val summed = original + dragAmount
                            change.consumePositionChange()
                            if (dragAmount.x > 0) {
                                dragManager.dragRight(
                                    index = selectedIndex,
                                    dragAmount = dragAmount
                                )
                            }
                            scope.launch {
                                dragManager.performDrag(
                                    dragAmountY = summed.y,
                                    dragAmountX = summed.x,
                                    dragIndex = selectedIndex,
                                    selectedIndex = selectedIndex
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            LaunchedEffect(key1 = Unit, block = {
                dragManager.setBoxWidth(with(density) { maxWidth.value.dp.toPx() })
            })
            val visibleIndexRange =
                (items.size downTo (selectedIndex - maxElements + 1).coerceAtLeast(0))
            items
                .asReversed()
                .forEachIndexed { index, item ->
                    if (dragManager.listOfDragState.isNotEmpty()) {
                        val swipeState = dragManager.listOfDragState[index]
                        if (index in visibleIndexRange) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleY = swipeState.scale.value
                                        scaleX = swipeState.scale.value
                                        translationX = swipeState.offsetX.value
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
}

/**
 * It's for remembering the dragManager object after recomposition
 */
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

    var isAnimationRunning = false
        private set

    private var isSwipingRight = false

    /**
     * When the object initialize the object
     * */
    init {
        //Initializing drag states for all the cards
        initList()
        initProperty()
        initView()
    }

    /**
     * It will set the box width
     * */
    fun setBoxWidth(boxWidth: Float) {
        this.boxWidth = boxWidth
        initProperty()
        initView()
    }

    /**
     * It will just initialize the views with the exact dimensions
     */
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

    /**
     * Initializing the helper properties to manipulate
     */
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

    /**
     * It's responsible for performing the drag of the card
     * @param dragAmountX X axis drag amount
     * @param dragAmountY Y axis drag amount
     * @param dragIndex dragging index by the user
     * @param selectedIndex which one is currently top of the deck
     */
    fun performDrag(dragAmountX: Float, dragAmountY: Float, dragIndex: Int, selectedIndex: Int) = scope.launch {
        if (dragIndex != selectedIndex) return@launch
        if (dragAmountX > 0){
            return@launch
        }
        if(isAnimationRunning) return@launch

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

    /**
     * When deck of card ends dragging then need to handle couple of things
     * @param index which card user wants to swipe
     * @param selectedIndex currently which card is in the top of the deck
     * @param onDismiss It will call after the manipulation
     */
    fun onDragEnd(index: Int, selectedIndex: Int, onDismiss: (Int) -> Unit) {
        if (index != selectedIndex) return
        val swipeState = listOfDragState[index]
        when {
            swipeState.offsetX.targetValue >= 0 -> {
                val prevIndex = (index + 1).coerceAtMost(size - 1)
                if (prevIndex == index + 1) {
                    isAnimationRunning = true
                    val prevState = listOfDragState[prevIndex]
                    prevState
                        .positionToCenter{
                            returnToEquilibrium(index = prevIndex)
                        }.invokeOnCompletion {
                            isAnimationRunning = false
                            onDismiss(-1)
                        }
                }
            }
            abs(swipeState.offsetX.targetValue) < screenWidth / 2 -> {
                isAnimationRunning = true
                swipeState
                    .positionToCenter() {
                        returnToEquilibrium(index = index)
                    }.invokeOnCompletion {
                        isAnimationRunning = false
                    }
            }
            abs(swipeState.offsetX.targetValue) > 0 && abs(swipeState.offsetX.targetValue) > screenWidth/2 -> {
                animateOutsideOfScreen(index = index, onDismiss = onDismiss)
            }
        }
    }

    /**
     * It's responsibility to returning the deck of cards to the equilibrium state in which state it belongs to
     * @param index Index of currently swiped card
     */
    private suspend fun returnToEquilibrium(index: Int) = scope.launch {
        for ((counter, i) in (index - 1 downTo (index - maxCards + 1).coerceAtLeast(0)).withIndex()) {
            listOfDragState[i].animateTo(
                scaleP = scale[counter + 1],
                opacityP = opacity[counter + 1],
                offsetXP = offsetX[counter + 1]
            )
        }
    }

    /**
     * It's the drag right gesture whenever user drags it right of the screen so that the last removed card should appear into the deck again
     * @param index Index of the current selected item
     * @param dragAmount Drag offset so that it will do the interpolation
     */
    fun dragRight(index: Int, dragAmount: Offset) = scope.launch {
        isSwipingRight = true
        if (dragAmount.x < 0) return@launch
        val prevItemIndex = (index + 1).coerceAtMost(size - 1)
        if (prevItemIndex == index + 1) {
            val item = listOfDragState[prevItemIndex]
            val itemOffset = Offset(x = item.offsetX.value, y = item.offsetY.value)
            val summed = itemOffset + dragAmount
            performDrag(
                dragAmountX = summed.x,
                dragAmountY = 0f,
                dragIndex = prevItemIndex,
                selectedIndex = prevItemIndex
            )
        }
        isSwipingRight = false
    }

    /**
     * It will give you the power to swiping left without interacting to the cards
     * @param index Index of the swiped
     * @param onDismiss It will be called after dismissal of the card
     */
    fun swipeLeft(index: Int,onDismiss: (Int) -> Unit = {}) = scope.launch {
        if(index < 0) return@launch
        else
        animateOutsideOfScreen(index = index, onDismiss = onDismiss)
    }

    /**
     * It will animate outside of screen with the particular index
     * @param index Index of the current swiped card
     * @param onDismiss After dismissal of the card if we need to manipulate some thing or not
     */
    private fun animateOutsideOfScreen(index: Int,onDismiss: (Int) -> Unit) = scope.launch{
        val state = listOfDragState[index]
        state.animateOutsideOfScreen()
        isAnimationRunning = true
        for ((counter, i) in (index - 1 downTo (index - maxCards + 1).coerceAtLeast(0)).withIndex()){
            launch {
                listOfDragState[i].animateTo(scaleP = scale[counter], opacityP = opacity[counter], offsetXP = offsetX[counter]){
                    if (index - maxCards >= 0) {
                        val lastElement = listOfDragState[index - maxCards]
                        lastElement.animateTo(
                            scaleP = scale[maxCards - 1],
                            opacityP = opacity[maxCards - 1],
                            offsetXP = offsetX[maxCards - 1]
                        )
                    }
                }.invokeOnCompletion {
                    isAnimationRunning = false
                    onDismiss(1)
                }
            }
        }
    }
}


open class DragState(
    val index: Int,
    val screenWidth: Float,
    private val scope: CoroutineScope
) {

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

    suspend fun animateTo(
        scaleP: Float,
        opacityP: Float,
        offsetXP: Float,
        onParallel: suspend () -> Unit = {}
    ) = scope.launch {
        launch { scale.animateTo(scaleP) }
        launch { opacity.animateTo(opacityP) }
        launch { offsetX.animateTo(offsetXP) }
        launch { onParallel() }
    }

}
