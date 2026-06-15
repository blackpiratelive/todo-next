package com.todonext.planify.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.todonext.planify.ui.theme.AccentBlue

@Composable
fun AnimatedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color = AccentBlue,
    modifier: Modifier = Modifier
) {
    val animatedBorderColor by animateColorAsState(
        targetValue = if (checked) accentColor else accentColor.copy(alpha = 0.5f),
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )

    val animatedFillColor by animateColorAsState(
        targetValue = if (checked) accentColor else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "fillColor"
    )

    val animatedCheckColor by animateColorAsState(
        targetValue = if (checked) Color.White else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "checkColor"
    )

    Canvas(
        modifier = modifier
            .size(24.dp)
            .semantics {
                contentDescription = if (checked) "Checked" else "Unchecked"
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 18.dp),
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        val strokeWidth = 2.dp.toPx()
        val radius = size.minDimension / 2f
        val center = this.center

        // Fill circle
        drawCircle(
            color = animatedFillColor,
            radius = radius,
            center = center
        )

        // Border circle
        drawCircle(
            color = animatedBorderColor,
            radius = radius - strokeWidth / 2f,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // Checkmark
        if (checked || animatedCheckColor != Color.Transparent) {
            val checkPath = Path().apply {
                val scale = size.minDimension / 24f
                moveTo(center.x - 4.5f * scale, center.y + 0.5f * scale)
                lineTo(center.x - 1.5f * scale, center.y + 3.5f * scale)
                lineTo(center.x + 5f * scale, center.y - 3.5f * scale)
            }
            drawPath(
                path = checkPath,
                color = animatedCheckColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
