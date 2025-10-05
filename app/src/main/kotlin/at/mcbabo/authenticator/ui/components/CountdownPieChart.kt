package at.mcbabo.authenticator.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CountdownPieChart(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    val percentage = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        ),
        label = "pie_animation"
    )

    Canvas(
        modifier = modifier.size(size)
    ) {
        val canvasSize = size.toPx()
        val radius = canvasSize / 2
        val center = Offset(canvasSize / 2, canvasSize / 2)

        if (animatedPercentage > 0f) {
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = -(animatedPercentage * 360f),
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }
    }
}
