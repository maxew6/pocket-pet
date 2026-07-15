package com.pocketpet.core.designsystem.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.pocketpet.core.designsystem.theme.Honey
import com.pocketpet.core.designsystem.theme.PetInnerEar
import com.pocketpet.core.designsystem.theme.SoftCoral
import com.pocketpet.core.model.Accessory
import com.pocketpet.core.model.PetAppearance
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws the default cream cat entirely with Canvas primitives — no raster assets. The curved
 * parts (ears, tail) reuse [Path] instances allocated once via `remember` and rebuilt (never
 * re-allocated) every frame, which is what keeps this cheap enough to run continuously.
 *
 * Callers must constrain size via [modifier] (e.g. `Modifier.size(96.dp)`); this composable does
 * not impose a default size, so it works equally well in the overlay and in a settings preview.
 */
@Composable
fun PetCanvas(renderState: PetRenderState, appearance: PetAppearance, modifier: Modifier = Modifier) {
    val furColor = appearance.colorTone.toFurColor()
    val leftEarPath = remember { Path() }
    val rightEarPath = remember { Path() }
    val tailPath = remember { Path() }

    Canvas(
        modifier = modifier
            .alpha(appearance.opacity)
            .scale(appearance.scale),
    ) {
        drawPetScene(renderState, appearance, furColor, leftEarPath, rightEarPath, tailPath)
    }
}

private fun DrawScope.drawPetScene(
    r: PetRenderState,
    appearance: PetAppearance,
    furColor: Color,
    leftEarPath: Path,
    rightEarPath: Path,
    tailPath: Path,
) {
    val unit = size.minDimension
    val centerX = size.width / 2f
    val groundY = size.height * 0.86f
    val bounceOffsetPx = r.bodyOffsetYDp.dp.toPx()

    drawShadow(centerX, groundY, unit, r.shadowScale)

    val bodyWidth = unit * 0.70f * r.squash
    val bodyHeight = unit * 0.60f * r.stretch
    val bodyCenterY = groundY - bodyHeight / 2f + bounceOffsetPx
    val bodyCenter = Offset(centerX, bodyCenterY)

    drawTail(tailPath, bodyCenter, bodyWidth, bodyHeight, r.tailPhase, furColor)
    drawPaws(bodyCenter, bodyWidth, bodyHeight, r.pawPhase, furColor)

    rotate(degrees = r.bodyRotationDegrees, pivot = bodyCenter) {
        drawOval(
            color = furColor,
            topLeft = Offset(bodyCenter.x - bodyWidth / 2f, bodyCenter.y - bodyHeight / 2f),
            size = Size(bodyWidth, bodyHeight),
        )
        // Belly highlight: a softer, smaller oval near the bottom-front of the body.
        drawOval(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(bodyCenter.x - bodyWidth * 0.28f, bodyCenter.y + bodyHeight * 0.02f),
            size = Size(bodyWidth * 0.56f, bodyHeight * 0.42f),
        )

        drawEars(leftEarPath, rightEarPath, bodyCenter, bodyWidth, bodyHeight, r.earRotationDegrees, furColor)
        drawFace(r, bodyCenter, bodyWidth, bodyHeight)
        drawWhiskers(bodyCenter, bodyWidth, bodyHeight)
        drawAccessory(appearance.accessory, bodyCenter, bodyWidth, bodyHeight)
    }

    if (r.showFoodBowl) drawFoodBowl(centerX, groundY, unit)
    if (r.showZzz) drawZzz(bodyCenter, bodyWidth, bodyHeight)
    if (r.showEnergyParticles) drawEnergyParticles(bodyCenter, unit)
    if (r.heartBurstProgress > 0.01f) drawHearts(bodyCenter, unit, r.heartBurstProgress)
    if (r.confettiBurstProgress > 0.01f) drawConfetti(bodyCenter, unit, r.confettiBurstProgress)
}

private fun DrawScope.drawShadow(centerX: Float, groundY: Float, unit: Float, shadowScale: Float) {
    drawOval(
        color = Color.Black.copy(alpha = 0.14f),
        topLeft = Offset(centerX - (unit * 0.32f * shadowScale), groundY - unit * 0.04f),
        size = Size(unit * 0.64f * shadowScale, unit * 0.09f * shadowScale),
    )
}

private fun DrawScope.drawTail(
    path: Path,
    bodyCenter: Offset,
    bodyWidth: Float,
    bodyHeight: Float,
    tailPhase: Float,
    furColor: Color,
) {
    val sway = (tailPhase - 0.5f) * bodyWidth * 0.5f
    val base = Offset(bodyCenter.x + bodyWidth * 0.38f, bodyCenter.y + bodyHeight * 0.20f)
    val mid = Offset(base.x + bodyWidth * 0.32f, base.y - bodyHeight * 0.35f + sway * 0.3f)
    val tip = Offset(base.x + bodyWidth * 0.30f + sway, base.y - bodyHeight * 0.75f)

    path.reset()
    path.moveTo(base.x, base.y)
    path.quadraticTo(mid.x, mid.y, tip.x, tip.y)
    drawPath(
        path = path,
        color = furColor,
        style = Stroke(width = bodyHeight * 0.16f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawPaws(
    bodyCenter: Offset,
    bodyWidth: Float,
    bodyHeight: Float,
    pawPhase: Float,
    furColor: Color,
) {
    val pawY = bodyCenter.y + bodyHeight * 0.44f - (pawPhase * bodyHeight * 0.05f)
    val pawWidth = bodyWidth * 0.20f
    val pawHeight = bodyHeight * 0.16f
    listOf(-1f, 1f).forEach { side ->
        drawOval(
            color = furColor,
            topLeft = Offset(bodyCenter.x + side * bodyWidth * 0.20f - pawWidth / 2f, pawY),
            size = Size(pawWidth, pawHeight),
        )
    }
}

private fun DrawScope.drawEars(
    leftPath: Path,
    rightPath: Path,
    bodyCenter: Offset,
    bodyWidth: Float,
    bodyHeight: Float,
    earRotationDegrees: Float,
    furColor: Color,
) {
    val earBaseY = bodyCenter.y - bodyHeight * 0.42f
    val earHeight = bodyHeight * 0.34f
    val earHalfWidth = bodyWidth * 0.16f

    listOf(-1f, 1f).forEach { side ->
        val path = if (side < 0) leftPath else rightPath
        val baseX = bodyCenter.x + side * bodyWidth * 0.30f
        val tip = Offset(baseX + side * earHalfWidth * 0.3f, earBaseY - earHeight)
        val pivot = Offset(baseX, earBaseY)

        path.reset()
        path.moveTo(baseX - earHalfWidth, earBaseY)
        path.lineTo(tip.x, tip.y)
        path.lineTo(baseX + earHalfWidth, earBaseY)
        path.close()

        rotate(degrees = earRotationDegrees * side, pivot = pivot) {
            drawPath(path = path, color = furColor)
            val innerPath = Path().apply {
                moveTo(baseX - earHalfWidth * 0.45f, earBaseY - earHeight * 0.06f)
                lineTo(baseX + side * earHalfWidth * 0.15f, earBaseY - earHeight * 0.68f)
                lineTo(baseX + earHalfWidth * 0.45f, earBaseY - earHeight * 0.06f)
                close()
            }
            drawPath(path = innerPath, color = PetInnerEar)
        }
    }
}

private fun DrawScope.drawFace(r: PetRenderState, bodyCenter: Offset, bodyWidth: Float, bodyHeight: Float) {
    val eyeY = bodyCenter.y - bodyHeight * 0.06f
    val eyeSpacing = bodyWidth * 0.22f
    val eyeRadius = bodyWidth * 0.13f
    val eyeOpen = r.eyeOpenness.coerceIn(0.05f, 1f)

    listOf(-1f, 1f).forEach { side ->
        val eyeCenter = Offset(bodyCenter.x + side * eyeSpacing, eyeY)
        drawEyebrow(r, eyeCenter, eyeRadius, side)

        drawOval(
            color = Color.White,
            topLeft = Offset(eyeCenter.x - eyeRadius, eyeCenter.y - eyeRadius * eyeOpen),
            size = Size(eyeRadius * 2f, eyeRadius * 2f * eyeOpen),
        )
        if (eyeOpen > 0.15f) {
            val pupilOffset = Offset(r.pupilOffsetX * eyeRadius * 0.4f, r.pupilOffsetY * eyeRadius * 0.35f)
            drawCircle(
                color = Color(0xFF3A2E26),
                radius = eyeRadius * 0.52f * eyeOpen,
                center = eyeCenter + pupilOffset,
            )
            drawCircle(
                color = Color.White,
                radius = eyeRadius * 0.16f,
                center = eyeCenter + pupilOffset + Offset(-eyeRadius * 0.18f, -eyeRadius * 0.18f),
            )
        } else {
            drawLine(
                color = Color(0xFF3A2E26),
                start = Offset(eyeCenter.x - eyeRadius * 0.7f, eyeCenter.y),
                end = Offset(eyeCenter.x + eyeRadius * 0.7f, eyeCenter.y),
                strokeWidth = eyeRadius * 0.22f,
                cap = StrokeCap.Round,
            )
        }

        if (r.tearProgress > 0.02f) {
            val tearDrop = Offset(eyeCenter.x, eyeCenter.y + eyeRadius * (1.1f + r.tearProgress * 0.9f))
            drawOval(
                color = Color(0xFF7FB6E0).copy(alpha = 0.85f),
                topLeft = Offset(tearDrop.x - eyeRadius * 0.16f, tearDrop.y - eyeRadius * 0.22f),
                size = Size(eyeRadius * 0.32f, eyeRadius * 0.44f * r.tearProgress.coerceIn(0.3f, 1f)),
            )
        }
    }

    drawMouth(r, bodyCenter, bodyWidth, bodyHeight)
}

private fun DrawScope.drawEyebrow(r: PetRenderState, eyeCenter: Offset, eyeRadius: Float, side: Float) {
    val browY = eyeCenter.y - eyeRadius * 1.5f
    val tilt = when (r.mouthExpression) {
        MouthExpression.Concerned -> -0.5f * side
        MouthExpression.Sad -> -0.3f * side
        MouthExpression.Happy -> 0.25f * side
        else -> 0f
    }
    drawLine(
        color = Color(0xFF3A2E26).copy(alpha = 0.55f),
        start = Offset(eyeCenter.x - eyeRadius * 0.6f, browY + tilt * eyeRadius * 0.3f),
        end = Offset(eyeCenter.x + eyeRadius * 0.6f, browY - tilt * eyeRadius * 0.3f),
        strokeWidth = eyeRadius * 0.16f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawMouth(r: PetRenderState, bodyCenter: Offset, bodyWidth: Float, bodyHeight: Float) {
    val mouthCenter = Offset(bodyCenter.x, bodyCenter.y + bodyHeight * 0.14f)
    val mouthWidth = bodyWidth * 0.16f
    val color = Color(0xFF3A2E26).copy(alpha = 0.7f)
    when (r.mouthExpression) {
        MouthExpression.Happy -> drawArc(
            color = color,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(mouthCenter.x - mouthWidth, mouthCenter.y - mouthWidth * 0.6f),
            size = Size(mouthWidth * 2f, mouthWidth * 1.2f),
            style = Stroke(width = mouthWidth * 0.22f, cap = StrokeCap.Round),
        )
        MouthExpression.Sad -> drawArc(
            color = color,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(mouthCenter.x - mouthWidth, mouthCenter.y - mouthWidth * 0.2f),
            size = Size(mouthWidth * 2f, mouthWidth * 1.2f),
            style = Stroke(width = mouthWidth * 0.22f, cap = StrokeCap.Round),
        )
        MouthExpression.Open -> drawOval(
            color = color,
            topLeft = Offset(mouthCenter.x - mouthWidth * 0.35f, mouthCenter.y - mouthWidth * 0.1f),
            size = Size(mouthWidth * 0.7f, mouthWidth * 0.8f),
        )
        MouthExpression.Concerned, MouthExpression.Neutral -> drawLine(
            color = color,
            start = Offset(mouthCenter.x - mouthWidth * 0.5f, mouthCenter.y),
            end = Offset(mouthCenter.x + mouthWidth * 0.5f, mouthCenter.y),
            strokeWidth = mouthWidth * 0.2f,
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawWhiskers(bodyCenter: Offset, bodyWidth: Float, bodyHeight: Float) {
    val whiskerY = bodyCenter.y + bodyHeight * 0.06f
    val color = Color(0xFF3A2E26).copy(alpha = 0.28f)
    listOf(-1f, 1f).forEach { side ->
        for (i in 0..2) {
            val yOffset = (i - 1) * bodyHeight * 0.05f
            drawLine(
                color = color,
                start = Offset(bodyCenter.x + side * bodyWidth * 0.30f, whiskerY + yOffset),
                end = Offset(bodyCenter.x + side * bodyWidth * 0.52f, whiskerY + yOffset * 1.4f),
                strokeWidth = bodyWidth * 0.008f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawFoodBowl(centerX: Float, groundY: Float, unit: Float) {
    val bowlWidth = unit * 0.34f
    val bowlHeight = unit * 0.14f
    val bowlCenter = Offset(centerX + unit * 0.46f, groundY - bowlHeight * 0.3f)
    drawOval(
        color = SoftCoral,
        topLeft = Offset(bowlCenter.x - bowlWidth / 2f, bowlCenter.y - bowlHeight / 2f),
        size = Size(bowlWidth, bowlHeight),
    )
    drawOval(
        color = Honey,
        topLeft = Offset(bowlCenter.x - bowlWidth * 0.36f, bowlCenter.y - bowlHeight * 0.30f),
        size = Size(bowlWidth * 0.72f, bowlHeight * 0.5f),
    )
}

private fun DrawScope.drawZzz(bodyCenter: Offset, bodyWidth: Float, bodyHeight: Float) {
    val base = Offset(bodyCenter.x + bodyWidth * 0.42f, bodyCenter.y - bodyHeight * 0.48f)
    val color = Color(0xFF3A2E26).copy(alpha = 0.5f)
    listOf(0.55f, 0.8f, 1.05f).forEachIndexed { index, scaleStep ->
        val zOffset = Offset(base.x + index * bodyWidth * 0.10f, base.y - index * bodyHeight * 0.14f)
        val z = bodyWidth * 0.09f * scaleStep
        val path = Path().apply {
            moveTo(zOffset.x - z, zOffset.y - z * 0.6f)
            lineTo(zOffset.x + z, zOffset.y - z * 0.6f)
            lineTo(zOffset.x - z, zOffset.y + z * 0.6f)
            lineTo(zOffset.x + z, zOffset.y + z * 0.6f)
        }
        drawPath(path = path, color = color, style = Stroke(width = z * 0.28f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawEnergyParticles(bodyCenter: Offset, unit: Float) {
    val color = Color(0xFFE8C784).copy(alpha = 0.8f)
    for (i in 0..3) {
        val angle = (i / 4f) * 2 * PI.toFloat()
        val radius = unit * 0.42f
        val point = Offset(
            bodyCenter.x + cos(angle) * radius,
            bodyCenter.y + sin(angle) * radius * 0.7f,
        )
        drawCircle(color = color, radius = unit * 0.02f, center = point)
    }
}

private fun DrawScope.drawHearts(bodyCenter: Offset, unit: Float, progress: Float) {
    val color = SoftCoral.copy(alpha = (1f - progress).coerceIn(0f, 1f))
    val riseY = -progress * unit * 0.5f
    listOf(-0.22f, 0f, 0.22f).forEachIndexed { index, xFrac ->
        val delay = index * 0.15f
        val local = (progress - delay).coerceIn(0f, 1f)
        if (local <= 0f) return@forEachIndexed
        val center = Offset(
            bodyCenter.x + unit * xFrac,
            bodyCenter.y - unit * 0.5f + riseY * local,
        )
        drawHeart(center, unit * 0.06f, color.copy(alpha = color.alpha * (1f - local * 0.5f)))
    }
}

private fun DrawScope.drawHeart(center: Offset, size: Float, color: Color) {
    drawCircle(color = color, radius = size * 0.55f, center = center + Offset(-size * 0.5f, -size * 0.25f))
    drawCircle(color = color, radius = size * 0.55f, center = center + Offset(size * 0.5f, -size * 0.25f))
    val trianglePath = Path().apply {
        moveTo(center.x - size, center.y - size * 0.1f)
        lineTo(center.x + size, center.y - size * 0.1f)
        lineTo(center.x, center.y + size * 1.1f)
        close()
    }
    drawPath(path = trianglePath, color = color)
}

private fun DrawScope.drawConfetti(bodyCenter: Offset, unit: Float, progress: Float) {
    val colors = listOf(SoftCoral, Honey, Color(0xFF7C9473), Color(0xFFA6616B))
    for (i in 0 until 10) {
        val angle = (i / 10f) * 2 * PI.toFloat()
        val distance = unit * 0.55f * progress
        val point = Offset(
            bodyCenter.x + cos(angle) * distance,
            bodyCenter.y - unit * 0.3f + sin(angle) * distance * 0.6f - (progress * unit * 0.2f),
        )
        drawCircle(
            color = colors[i % colors.size].copy(alpha = (1f - progress).coerceIn(0f, 1f)),
            radius = unit * 0.016f,
            center = point,
        )
    }
}

private fun DrawScope.drawAccessory(accessory: Accessory, bodyCenter: Offset, bodyWidth: Float, bodyHeight: Float) {
    when (accessory) {
        Accessory.None -> Unit
        Accessory.Hat -> {
            val hatY = bodyCenter.y - bodyHeight * 0.58f
            drawOval(
                color = Color(0xFF3A2E26).copy(alpha = 0.85f),
                topLeft = Offset(bodyCenter.x - bodyWidth * 0.26f, hatY),
                size = Size(bodyWidth * 0.52f, bodyHeight * 0.10f),
            )
            val conePath = Path().apply {
                moveTo(bodyCenter.x - bodyWidth * 0.16f, hatY + bodyHeight * 0.02f)
                lineTo(bodyCenter.x + bodyWidth * 0.16f, hatY + bodyHeight * 0.02f)
                lineTo(bodyCenter.x, hatY - bodyHeight * 0.22f)
                close()
            }
            drawPath(path = conePath, color = Color(0xFF3A2E26).copy(alpha = 0.85f))
        }
        Accessory.Glasses -> {
            val eyeY = bodyCenter.y - bodyHeight * 0.06f
            val eyeSpacing = bodyWidth * 0.22f
            val lensRadius = bodyWidth * 0.15f
            val strokeColor = Color(0xFF3A2E26).copy(alpha = 0.8f)
            listOf(-1f, 1f).forEach { side ->
                drawCircle(
                    color = strokeColor,
                    radius = lensRadius,
                    center = Offset(bodyCenter.x + side * eyeSpacing, eyeY),
                    style = Stroke(width = bodyWidth * 0.015f),
                )
            }
            drawLine(
                color = strokeColor,
                start = Offset(bodyCenter.x - eyeSpacing + lensRadius, eyeY),
                end = Offset(bodyCenter.x + eyeSpacing - lensRadius, eyeY),
                strokeWidth = bodyWidth * 0.012f,
            )
        }
        Accessory.Scarf -> {
            val scarfY = bodyCenter.y + bodyHeight * 0.28f
            drawOval(
                color = SoftCoral,
                topLeft = Offset(bodyCenter.x - bodyWidth * 0.34f, scarfY),
                size = Size(bodyWidth * 0.68f, bodyHeight * 0.14f),
            )
            drawOval(
                color = SoftCoral,
                topLeft = Offset(bodyCenter.x + bodyWidth * 0.06f, scarfY + bodyHeight * 0.08f),
                size = Size(bodyWidth * 0.14f, bodyHeight * 0.30f),
            )
        }
    }
}
