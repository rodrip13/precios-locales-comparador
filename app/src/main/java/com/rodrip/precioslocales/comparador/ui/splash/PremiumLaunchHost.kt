package com.rodrip.precioslocales.comparador.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rodrip.precioslocales.comparador.R
import com.rodrip.precioslocales.comparador.ui.main.MainScreen
import com.rodrip.precioslocales.comparador.ui.stores.StoreViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

@Composable
fun PremiumLaunchHost(storeViewModel: StoreViewModel) {
	var introCompleted by rememberSaveable { mutableStateOf(false) }

	val contentAlpha by animateFloatAsState(
		targetValue = if (introCompleted) 1f else 0.78f,
		animationSpec = tween(durationMillis = 700, delayMillis = 120, easing = LinearOutSlowInEasing),
		label = "contentAlpha"
	)
	val contentScale by animateFloatAsState(
		targetValue = if (introCompleted) 1f else 0.975f,
		animationSpec = tween(durationMillis = 760, delayMillis = 120, easing = FastOutSlowInEasing),
		label = "contentScale"
	)

	Box(modifier = Modifier.fillMaxSize()) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.graphicsLayer {
					alpha = contentAlpha
					scaleX = contentScale
					scaleY = contentScale
				}
		) {
			MainScreen(storeViewModel = storeViewModel)
		}

		if (!introCompleted) {
			PremiumIntroOverlay(onFinished = { introCompleted = true })
		}
	}
}

@Composable
private fun PremiumIntroOverlay(onFinished: () -> Unit) {
	BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
		val density = LocalDensity.current
		val widthPx = with(density) { maxWidth.toPx() }
		val heightPx = with(density) { maxHeight.toPx() }

		val impactFx = 0.36f
		val impactFy = 0.38f
		val impactX = widthPx * impactFx
		val impactY = heightPx * impactFy

		val maxDistance = hypot(
			max(impactX, widthPx - impactX).toDouble(),
			max(impactY, heightPx - impactY).toDouble()
		).toFloat()

		val ballProgress = remember { Animatable(0f) }
		val splashProgress = remember { Animatable(0f) }
		val edgeChaos = remember { Animatable(0f) }
		val impactRing = remember { Animatable(0f) }
		val paintZoom = remember { Animatable(1f) }
		val iconAlpha = remember { Animatable(0f) }
		val iconScale = remember { Animatable(3.1f) }
		val overlayAlpha = remember { Animatable(1f) }

		val backgroundPrimary = colorResource(R.color.splash_background)
		val backgroundSecondary = colorResource(R.color.splash_background_secondary)
		val paintColor = colorResource(R.color.splash_accent)
		val paintHighlight = lerp(paintColor, Color.White, 0.28f)

		LaunchedEffect(Unit) {
			// Timeline total: ~2400ms
			ballProgress.animateTo(
				targetValue = 1f,
				animationSpec = tween(durationMillis = 430, easing = FastOutSlowInEasing)
			)

			splashProgress.animateTo(
				targetValue = 0.26f,
				animationSpec = tween(durationMillis = 170, easing = LinearOutSlowInEasing)
			)

			launch {
				impactRing.animateTo(
					targetValue = 1f,
					animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
				)
			}
			launch {
				edgeChaos.animateTo(
					targetValue = 1f,
					animationSpec = tween(durationMillis = 980, easing = LinearOutSlowInEasing)
				)
			}

			splashProgress.animateTo(
				targetValue = 1f,
				animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
			)

			launch {
				iconAlpha.animateTo(
					targetValue = 1f,
					animationSpec = tween(durationMillis = 360, delayMillis = 180, easing = LinearOutSlowInEasing)
				)
			}
			launch {
				iconScale.animateTo(
					targetValue = 1f,
					animationSpec = tween(durationMillis = 640, delayMillis = 120, easing = FastOutSlowInEasing)
				)
			}

			paintZoom.animateTo(
				targetValue = 3.35f,
				animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing)
			)

			overlayAlpha.animateTo(0f, tween(durationMillis = 140, easing = LinearOutSlowInEasing))
			onFinished()
		}

		val splashRadius = maxDistance * (0.08f + splashProgress.value * 1.12f)
		val ballStartX = -widthPx * 0.09f
		val ballStartY = -heightPx * 0.16f
		val ballX = ballStartX + (impactX - ballStartX) * ballProgress.value
		val ballY = ballStartY + (impactY - ballStartY) * ballProgress.value

		val ballVisible = ballProgress.value < 0.995f
		val cameraOrigin = TransformOrigin(impactFx, impactFy)

		Box(
			modifier = Modifier
				.fillMaxSize()
				.graphicsLayer {
					alpha = overlayAlpha.value
					scaleX = paintZoom.value
					scaleY = paintZoom.value
					transformOrigin = cameraOrigin
				}
				.background(
					brush = Brush.verticalGradient(
						colors = listOf(backgroundSecondary, backgroundPrimary)
					)
				)
		) {
			Canvas(modifier = Modifier.fillMaxSize()) {
				val ringAlpha = (1f - impactRing.value).coerceIn(0f, 1f)
				if (ringAlpha > 0f) {
					drawCircle(
						color = paintHighlight.copy(alpha = ringAlpha * 0.45f),
						radius = splashRadius * (0.42f + impactRing.value * 0.72f),
						center = Offset(impactX, impactY)
					)
				}

				drawCircle(
					color = paintColor,
					radius = splashRadius,
					center = Offset(impactX, impactY)
				)

				val irregularAmplitude = splashRadius * (0.24f * edgeChaos.value)
				repeat(18) { index ->
					val seed = (index + 1) / 18f
					val baseAngle = (seed * (2f * PI.toFloat())) + edgeChaos.value * 0.9f
					val noisyAngle = baseAngle + (sin(seed * 11.3f + edgeChaos.value * 2.7f) * 0.22f)
					val radialNoise = ((sin(seed * 7.6f + edgeChaos.value * 3.2f) + 1f) / 2f)
					val edgeDistance = splashRadius * (0.8f + radialNoise * 0.3f)
					val lobeNoise = ((sin(seed * 13.9f + edgeChaos.value * 4.1f) + 1f) / 2f)
					val lobeRadius = irregularAmplitude * (0.45f + lobeNoise * 0.85f)
					drawCircle(
						color = paintColor,
						radius = lobeRadius,
						center = Offset(
							x = impactX + cos(noisyAngle.toDouble()).toFloat() * edgeDistance,
							y = impactY + sin(noisyAngle.toDouble()).toFloat() * edgeDistance
						)
					)
				}

				drawCircle(
					color = paintHighlight.copy(alpha = 0.20f * splashProgress.value),
					radius = splashRadius * 0.58f,
					center = Offset(impactX - splashRadius * 0.12f, impactY - splashRadius * 0.18f)
				)

				val splashPulse = 1f - splashProgress.value
				if (splashPulse > 0f) {
					drawCircle(
						color = paintHighlight.copy(alpha = splashPulse * 0.35f),
						radius = splashRadius * (0.72f + splashProgress.value * 0.36f),
						center = Offset(impactX, impactY)
					)
				}

				repeat(9) { index ->
					val t = (index + 1) / 10f
					val drift = sin((index + 1) * 1.45f) * 0.19f
					val dropRadius = (widthPx * (0.007f + t * 0.016f)) * splashProgress.value
					val dx = widthPx * drift * (0.25f + splashProgress.value * 0.95f)
					val dy = heightPx * (0.03f + t * 0.28f) * splashProgress.value
					drawCircle(
						color = paintColor.copy(alpha = 0.82f * splashProgress.value),
						radius = dropRadius,
						center = Offset(impactX + dx, impactY + dy)
					)
				}

				if (ballVisible) {
					val ballRadius = widthPx * (0.055f + (1f - ballProgress.value) * 0.018f)
					drawCircle(
						brush = Brush.radialGradient(
							colors = listOf(paintHighlight, paintColor),
							center = Offset(ballX - ballRadius * 0.2f, ballY - ballRadius * 0.25f),
							radius = ballRadius * 1.3f
						),
						radius = ballRadius,
						center = Offset(ballX, ballY)
					)
				}
			}

			Image(
				painter = painterResource(id = R.mipmap.ic_launcher_cpr_foreground),
				contentDescription = null,
				modifier = Modifier
					.align(Alignment.Center)
					.size(106.dp)
					.graphicsLayer {
						alpha = iconAlpha.value
						scaleX = iconScale.value
						scaleY = iconScale.value
					},
				contentScale = ContentScale.Fit
			)
		}
	}
}









