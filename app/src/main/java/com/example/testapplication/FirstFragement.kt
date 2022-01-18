package com.example.testapplication

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.viewpager2.widget.ViewPager2
import com.example.testapplication.views.CardStack
import com.google.accompanist.flowlayout.FlowRow
import kotlin.math.roundToInt


class FirstFragement : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val list = listOf(10)
        val view = inflater.inflate(R.layout.fragment_first_fragement, container, false)

        val listOfViewModel = listOf(
            ViewModel(align = CardAlign.RIGHT,color = Color.Red),
            ViewModel(align = CardAlign.LEFT,color = Color.Blue),
            ViewModel(align = CardAlign.RIGHT,color = Color.Green),
            ViewModel(align = CardAlign.LEFT,color = Color.Gray),
            ViewModel(align = CardAlign.CENTRE,color = Color.Red)
        )


        return view
    }

    @ExperimentalMaterialApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            CardStack(items = mutableListOf(Color.Red,Color.DarkGray,Color.Blue,Color.Black,Color.Magenta,Color.Red))
        }
    }
    
    private fun listOf(count:Int):MutableList<String>{
        val mutableList = mutableListOf<String>()
        for(i in 0 until count) mutableList.add("SOME $i")
        return mutableList
    }

    private fun findViewPager(view:ListComposeView):ViewPager2? {
        var rootView: ViewParent? = view
        var viewPager:ViewPager2? = null
        while (rootView != null && (rootView is ViewPager2).not()) {
            rootView = rootView.parent

            if (rootView is ViewPager2) {
                viewPager = rootView
            }
        }
        return viewPager
    }
}

enum class CardAlign{
    LEFT,
    RIGHT,
    CENTRE
}

data class ViewModel(val align: CardAlign,val color: Color)

@Composable
fun AdaptiveRow(listOfModel:List<ViewModel>){
    val spanCount = 2
    val totalSize = listOfModel.size
    val fraction = 1f.div(spanCount)
    //Span count closest multiple should be my threshold value for choosing whether it will be in the same line or in a orphan state
    val orphanThreshold = totalSize.coerceAtMost(maximumValue = totalSize.div(spanCount)*spanCount)
    val orphanFraction = 1f.div(totalSize - orphanThreshold)
    val leftSide = 0 until spanCount.div(2) //
    val rightSide = if(spanCount%2 == 0) spanCount.div(2) until spanCount else spanCount.div(2)+1 until spanCount
    val center = if(spanCount%2 == 0) -1 else spanCount.div(2)
    FlowRow(modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()){
        listOfModel.forEachIndexed { index, viewModel ->
            //It will determine the relative value of the index wrt a specific row
            val indexInRange = index%spanCount
            CardImage(
                modifier = Modifier.fillMaxWidth(fraction = if(index < orphanThreshold) fraction else orphanFraction),
                color = viewModel.color,
                align = when {
                    index >= orphanThreshold && orphanFraction == 1f -> CardAlign.CENTRE
                    indexInRange in leftSide -> CardAlign.RIGHT
                    indexInRange in rightSide -> CardAlign.LEFT
                    indexInRange == center && center != -1 -> CardAlign.CENTRE
                    else -> CardAlign.CENTRE
                }
            )
        }
    }
}

@Composable
fun CardImage(modifier:Modifier,color: Color,align: CardAlign = CardAlign.CENTRE){
    Row(modifier = modifier
        .padding(5.dp),
        horizontalArrangement = when(align){
            CardAlign.CENTRE -> Arrangement.Center
            CardAlign.LEFT -> Arrangement.Start
            CardAlign.RIGHT -> Arrangement.End
        }
    ){
        Box(modifier = Modifier
            .background(color = color)
            .size(100.dp))
    }
}




fun Float.roundTo(n : Int) : Float {
    return "%.${n}f".format(this).toFloat()
}




enum class States {
    EXPANDED,
    COLLAPSED
}

@Composable
fun rememberTimeOut() = remember{
    val isShowing = mutableStateOf(true)
    Handler(Looper.getMainLooper()).postDelayed({
        isShowing.value = false
    },3000)
    isShowing
}

@ExperimentalMaterialApi
@Composable
fun BottomSheet(modifier: Modifier = Modifier){
    val swipeableState = rememberSwipeableState(initialValue = States.EXPANDED, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
    val lazyListState = rememberLazyListState()

    BoxWithConstraints(modifier = modifier){
        val constraintsScope = this
        val maxHeight = with(LocalDensity.current) {
            constraintsScope.maxHeight.toPx()
        }
        val nestedScrollConnection = object : NestedScrollConnection{
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                Log.d("AVAILABLE","$available")
                val delta = available.y
                return if (delta < 0) {
                    val value = swipeableState.performDrag(delta).toOffset()
                    //Log.d("PERFORMED","$value")
                    value
                } else {
                    Offset.Zero
                }
            }
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                Log.d("AVAILABLE CONSUMED","${available.y} - ${consumed.y}")
                return swipeableState.performDrag(delta).toOffset()
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (available.y < 0 && lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0) {
                    swipeableState.performFling(available.y)
                    available
                } else {
                    Velocity.Zero
                }
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                Log.d("VELOCITY","available - ${available.y} consumed - ${consumed.y}")
                swipeableState.performFling(velocity = available.y)
                return super.onPostFling(consumed, available)
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .swipeable(
                    state = swipeableState,
                    orientation = Orientation.Vertical,
                    anchors = mapOf(
                        0f to States.EXPANDED,
                        maxHeight to States.COLLAPSED,
                    ),
                    thresholds = { _, _ -> FixedThreshold(constraintsScope.maxHeight / 2f) }
                )
                .offset {
                    IntOffset(
                        x = 0,
                        y = swipeableState.offset.value.roundToInt()
                    )
                }
                .nestedScroll(nestedScrollConnection)
                .background(color = Color.Red)
        ){

            LazyColumn(state = lazyListState){
                items(count = 100){index ->
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        text = "SOME $index"
                    )
                }
            }
        }
    }
}

fun Float.toOffset():Offset{
    return Offset(x = 0f,y = this)
}