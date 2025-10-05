package at.mcbabo.authenticator.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object MotionConstants {
    const val DEFAULT_MOTION_DURATION: Int = 300
    val DEFAULT_SLIDE_DISTANCE: Dp = 30.dp
}

const val INITIAL_OFFSET = 0.10f

private const val PROGRESS_THRESHOLD = 0.35f

private val Int.ForOutgoing: Int
    get() = (this * PROGRESS_THRESHOLD).toInt()

private val Int.ForIncoming: Int
    get() = this - this.ForOutgoing

fun materialSharedAxisXIn(
    initialOffsetX: (fullWidth: Int) -> Int,
    durationMillis: Int = MotionConstants.DEFAULT_MOTION_DURATION
): EnterTransition = slideInHorizontally(
    animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
    initialOffsetX = initialOffsetX
) + fadeIn(
    animationSpec = tween(
        durationMillis = durationMillis.ForIncoming,
        delayMillis = durationMillis.ForOutgoing,
        easing = LinearOutSlowInEasing
    )
)

fun materialSharedAxisXOut(
    targetOffsetX: (fullWidth: Int) -> Int,
    durationMillis: Int = MotionConstants.DEFAULT_MOTION_DURATION
): ExitTransition = slideOutHorizontally(
    animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
    targetOffsetX = targetOffsetX
) + fadeOut(
    animationSpec = tween(
        durationMillis = durationMillis.ForOutgoing,
        delayMillis = 0,
        easing = FastOutLinearInEasing
    )
)

inline fun <reified T : Any> NavGraphBuilder.animatedComposable(
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) = composable<T>(
    enterTransition = {
        materialSharedAxisXIn(initialOffsetX = { (it * INITIAL_OFFSET).toInt() })
    },
    exitTransition = {
        materialSharedAxisXOut(targetOffsetX = { -(it * INITIAL_OFFSET).toInt() })
    },
    popEnterTransition = {
        materialSharedAxisXIn(initialOffsetX = { -(it * INITIAL_OFFSET).toInt() })
    },
    popExitTransition = {
        materialSharedAxisXOut(targetOffsetX = { (it * INITIAL_OFFSET).toInt() })
    },
    content = content
)
