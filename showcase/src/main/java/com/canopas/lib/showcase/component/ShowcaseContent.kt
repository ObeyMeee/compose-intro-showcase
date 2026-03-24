package com.canopas.lib.showcase.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

sealed class TargetShape {
    object Circle : TargetShape()
    data class RoundedRectangle(val cornerRadius: Dp = 8.dp) : TargetShape()
}

@Composable
fun ShowcasePopup(
    state: IntroShowcaseState,
    dismissOnClickOutside: Boolean,
    onShowCaseCompleted: () -> Unit,
) {
    state.currentTarget?.let {
        if (it.coordinates.isAttached) {
            ShowcaseWindow {
                ShowcaseContent(
                    target = it,
                    dismissOnClickOutside = dismissOnClickOutside
                ) {
                    state.currentTargetIndex++
                    if (state.currentTarget == null) {
                        onShowCaseCompleted()
                    }
                }
            }
        }
    }
}

@Composable
internal fun ShowcaseContent(
    target: IntroShowcaseTargets,
    dismissOnClickOutside: Boolean,
    onShowcaseCompleted: () -> Unit
) {

    val targetCords = target.coordinates
    val targetRect = targetCords.boundsInWindow()

    var dismissShowcaseRequest by remember(target) { mutableStateOf(false) }

    val maxDimension =
        max(targetCords.size.width.absoluteValue, targetCords.size.height.absoluteValue)

    val targetRadius = maxDimension / 2f + target.style.targetPaddingPx

    val animationSpec = infiniteRepeatable<Float>(
        animation = tween(2000, easing = FastOutLinearInEasing),
        repeatMode = RepeatMode.Restart,
    )

    var outerOffset by remember(target) {
        mutableStateOf(Offset(0f, 0f))
    }

    var outerRadius by remember(target) {
        mutableFloatStateOf(0f)
    }

    val outerAnimatable = remember { Animatable(0.6f) }
    val outerAlphaAnimatable = remember(target) { Animatable(0f) }

    val animatables = remember(target) {
        listOf(
            Animatable(0f),
            Animatable(0f)
        )
    }

    LaunchedEffect(target) {
        outerAnimatable.snapTo(0.6f)

        outerAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    LaunchedEffect(target) {
        outerAlphaAnimatable.animateTo(
            targetValue = target.style.backgroundAlpha,
            animationSpec = tween(
                durationMillis = 500,
                easing = FastOutSlowInEasing,
            ),
        )
    }

    LaunchedEffect(dismissShowcaseRequest) {
        if (dismissShowcaseRequest) {
            launch {
                outerAlphaAnimatable.animateTo(
                    0f,
                    animationSpec = tween(
                        durationMillis = 200
                    )
                )
            }
            launch {
                outerAnimatable.animateTo(
                    targetValue = 0.6f,
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = FastOutSlowInEasing,
                    )
                )
            }
            delay(350)
            onShowcaseCompleted()
        }
    }

    animatables.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * 1000L)
            animatable.animateTo(
                targetValue = 1f, animationSpec = animationSpec
            )
        }
    }

    val dys = animatables.map { it.value }

    Box(
        modifier = Modifier
            .alpha(outerAlphaAnimatable.value)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(target) {
                    detectTapGestures { tapOffset ->
                        if (targetRect.contains(tapOffset)) {
                            target.onTargetClick()
                            dismissShowcaseRequest = true
                        }
                    }
                }
                .let {
                    if (dismissOnClickOutside) {
                        it.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { dismissShowcaseRequest = true }
                    } else it
                }
                .graphicsLayer(alpha = 0.99f)
        ) {
            drawCircle(
                color = target.style.backgroundColor,
                center = outerOffset,
                radius = outerRadius * outerAnimatable.value,
                alpha = target.style.backgroundAlpha
            )

            dys.forEach { dy ->
                when (target.style.targetShape) {
                    is TargetShape.Circle -> {
                        drawCircle(
                            color = target.style.targetColor,
                            radius = maxDimension * dy * 2f,
                            center = targetRect.center,
                            alpha = 1 - dy
                        )
                    }

                    is TargetShape.RoundedRectangle -> {
                        val shape = target.style.targetShape
                        val expansion = maxDimension * dy * 2f
                        drawRoundRect(
                            color = target.style.targetColor,
                            topLeft = Offset(
                                targetRect.left - expansion,
                                targetRect.top - expansion
                            ),
                            size = androidx.compose.ui.geometry.Size(
                                targetRect.width + expansion * 2,
                                targetRect.height + expansion * 2
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                shape.cornerRadius.toPx()
                            ),
                            alpha = 1 - dy
                        )
                    }
                }
            }

            when (target.style.targetShape) {
                is TargetShape.Circle -> {
                    drawCircle(
                        color = target.style.targetColor,
                        radius = targetRadius,
                        center = targetRect.center,
                        blendMode = BlendMode.Xor
                    )
                }

                is TargetShape.RoundedRectangle -> {
                    val shape = target.style.targetShape
                    drawRoundRect(
                        color = target.style.targetColor,
                        topLeft = Offset(
                            targetRect.left - 40f,
                            targetRect.top - 40f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            targetRect.width + 80f,
                            targetRect.height + 80f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                            shape.cornerRadius.toPx()
                        ),
                        blendMode = BlendMode.Xor
                    )
                }
            }
        }

        ShowCaseText(target, targetRect, targetRadius) { textCoords ->
            val contentRect = textCoords.boundsInWindow()
            val outerRect = getOuterRect(contentRect, targetRect)
            outerOffset = outerRect.center
            outerRadius = getOuterRadius(outerRect) + targetRadius
        }
    }
}


@Composable
private fun ShowCaseText(
    currentTarget: IntroShowcaseTargets,
    boundsInParent: Rect,
    targetRadius: Float,
    updateContentCoordinates: (LayoutCoordinates) -> Unit
) {
    var contentOffsetY by remember(currentTarget) { mutableFloatStateOf(0f) }

    val contentGap = currentTarget.style.targetPaddingPx

    Box(
        content = currentTarget.content,
        modifier = Modifier
            .offset(y = with(LocalDensity.current) {
                contentOffsetY.toDp()
            })
            .onGloballyPositioned {
                updateContentCoordinates(it)
                val contentHeight = it.size.height

                val possibleTop = boundsInParent.center.y - targetRadius - contentHeight

                contentOffsetY = if (possibleTop > 0) {
                    possibleTop
                } else {
                    boundsInParent.bottom + contentGap
                }
            }
            .padding(16.dp)
    )
}

private fun getOuterRect(contentRect: Rect, targetRect: Rect): Rect {

    val topLeftX = min(contentRect.topLeft.x, targetRect.topLeft.x)
    val topLeftY = min(contentRect.topLeft.y, targetRect.topLeft.y)
    val bottomRightX = max(contentRect.bottomRight.x, targetRect.bottomRight.x)
    val bottomRightY = max(contentRect.bottomRight.y, targetRect.bottomRight.y)

    return Rect(topLeftX, topLeftY, bottomRightX, bottomRightY)
}

private fun getOuterRadius(outerRect: Rect): Float {
    val d = sqrt(
        outerRect.height.toDouble().pow(2.0)
                + outerRect.width.toDouble().pow(2.0)
    ).toFloat()

    return (d / 2f)
}

data class IntroShowcaseTargets(
    val index: Int,
    val coordinates: LayoutCoordinates,
    val style: ShowcaseStyle = ShowcaseStyle.Default,
    val content: @Composable BoxScope.() -> Unit,
    val onTargetClick: () -> Unit = {}
)

class ShowcaseStyle(
    val backgroundColor: Color = Color.Black,
    /*@FloatRange(from = 0.0, to = 1.0)*/
    val backgroundAlpha: Float = DEFAULT_BACKGROUND_RADIUS,
    val targetColor: Color = Color.White,
    val targetShape: TargetShape = TargetShape.Circle,
    val targetPadding: Dp = 0.dp,
) {

    val targetPaddingPx: Float
        @Composable
        get() = with(LocalDensity.current) { targetPadding.toPx() }

    fun copy(
        backgroundColor: Color = this.backgroundColor,
        /*@FloatRange(from = 0.0, to = 1.0)*/
        backgroundAlpha: Float = this.backgroundAlpha,
        targetColor: Color = this.targetColor,
        targetShape: TargetShape = this.targetShape,
        targetPadding: Dp = this.targetPadding,
    ): ShowcaseStyle {

        return ShowcaseStyle(
            backgroundColor = backgroundColor,
            backgroundAlpha = backgroundAlpha,
            targetColor = targetColor,
            targetShape = targetShape,
            targetPadding = targetPadding,
        )
    }

    companion object {
        private const val DEFAULT_BACKGROUND_RADIUS = 0.9f

        /**
         * Constant for default text style.
         */
        @Stable
        val Default = ShowcaseStyle()
    }
}
