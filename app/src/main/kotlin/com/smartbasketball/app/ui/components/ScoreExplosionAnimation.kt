package com.smartbasketball.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartbasketball.app.ui.theme.BasketballOrange
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ScoreExplosionAnimation(
    score: Int,
    enabled: Boolean = true,
    onAnimationEnd: () -> Unit = {}
) {
    if (!enabled) {
        SimpleScoreDisplay(score = score)
        return
    }

    ExplosionEffect(score = score, onAnimationEnd = onAnimationEnd)
}

@Composable
private fun SimpleScoreDisplay(score: Int) {
    val configuration = LocalConfiguration.current
    val density = configuration.densityDpi.toFloat()
    val fontSize = (configuration.screenHeightDp * 0.5f * density).sp
    
    Text(
        text = "$score",
        fontSize = fontSize,
        color = BasketballOrange,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun ExplosionEffect(
    score: Int,
    onAnimationEnd: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = configuration.densityDpi.toFloat()
    val baseFontSize = configuration.screenHeightDp * 0.5f * density  // 转换为像素
    
    var animationStarted by remember { mutableStateOf(false) }
    var particles by remember { mutableStateOf(emptyList<Particle>()) }
    var shockwaveRadius by remember { mutableFloatStateOf(0f) }
    var shockwaveAlpha by remember { mutableFloatStateOf(0f) }
    var shakeOffset by remember { mutableStateOf(Offset.Zero) }

    // 动画时长配置
    val explosionDuration = 2000
    val particleDuration = 1200

    // 数字爆炸动画 (0.0s - 0.8s)
    val numberScale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.1f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = Spring.StiffnessLow
        ),
        finishedListener = { },
        label = "numberScale"
    )

    // 脉冲动画 (0.8s - 2.5s，循环)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // 粒子系统状态
    LaunchedEffect(Unit) {
        animationStarted = true
        // 生成粒子
        particles = generateParticles(80)
    }

    // 冲击波动画
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(200)
        shockwaveRadius = 0f
        shockwaveAlpha = 1f
        // 冲击波扩散 - 使用固定大数值覆盖大屏幕
        val maxRadius = 2000f  // 足够覆盖大屏幕
        val step = maxRadius / 50f
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 800) {
            shockwaveRadius += step
            shockwaveAlpha -= 0.015f
            kotlinx.coroutines.delay(16)
        }
        shockwaveAlpha = 0f
    }

    // 屏幕震动动画
    LaunchedEffect(Unit) {
        val shakeDuration = 300
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < shakeDuration) {
            shakeOffset = Offset(
                x = Random.nextFloat() * 20 - 10,
                y = Random.nextFloat() * 20 - 10
            )
            kotlinx.coroutines.delay(16)
        }
        shakeOffset = Offset.Zero
    }

    // 动画结束回调
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        onAnimationEnd()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 冲击波
        if (shockwaveAlpha > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    radius = shockwaveRadius,
                    center = Offset(size.width / 2, size.height / 2),
                    color = Color.White.copy(alpha = shockwaveAlpha * 0.5f),
                    style = Stroke(width = 8f)
                )
            }
        }

        // 粒子
        Canvas(modifier = Modifier.fillMaxSize()) {
            val currentTime = System.currentTimeMillis()
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            particles.forEach { particle ->
                val progress = (currentTime % particleDuration).toFloat() / particleDuration
                val age = progress * particle.lifetime

                if (age < particle.lifetime) {
                    val x = centerX + particle.velocityX * age * 0.5f
                    val y = centerY + particle.velocityY * age * 0.5f + age * age * 0.1f
                    val alpha = (1 - age / particle.lifetime).coerceIn(0f, 1f)
                    val scale = (1 + age * 0.5f).coerceIn(0.5f, 2f)

                    drawParticle(
                        x = x,
                        y = y,
                        size = particle.size * scale,
                        color = particle.color.copy(alpha = alpha),
                        rotation = particle.rotation + age * particle.rotationSpeed,
                        shape = particle.shape
                    )
                }
            }
        }

        // 数字（带脉冲动画和发光效果）
        Box(contentAlignment = Alignment.Center) {
            // 发光背景
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = BasketballOrange.copy(alpha = 0.3f),
                    radius = this.size.minDimension * 0.4f
                )
            }
            // 数字在最上层
            Text(
                text = "$score",
                fontSize = (baseFontSize * numberScale * pulseScale).sp,
                color = BasketballOrange,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun generateParticles(count: Int): List<Particle> {
    val colors = listOf(
        Color(0xFFFF0000), // 红
        Color(0xFFFF8800), // 橙
        Color(0xFFFFFF00), // 黄
        Color(0xFF00FF00), // 绿
        Color(0xFF00AAFF), // 蓝
        Color(0xFFAA00FF)  // 紫
    )

    val shapes = listOf(ParticleShape.CIRCLE, ParticleShape.STAR, ParticleShape.TRIANGLE)

    return List(count) {
        val angle = Random.nextFloat() * 2 * Math.PI.toFloat()
        val speed = Random.nextFloat() * 3000 + 1500  // 增加到1500-4500像素/秒

        Particle(
            x = 0f,
            y = 0f,
            velocityX = cos(angle) * speed,
            velocityY = sin(angle) * speed,
            size = Random.nextFloat() * 40 + 20,  // 增加到20-60像素
            color = colors.random(),
            shape = shapes.random(),
            rotation = Random.nextFloat() * 360,
            rotationSpeed = Random.nextFloat() * 360 - 180,
            lifetime = Random.nextFloat() * 0.5f + 0.5f
        )
    }
}

private fun DrawScope.drawParticle(
    x: Float,
    y: Float,
    size: Float,
    color: Color,
    rotation: Float,
    shape: ParticleShape
) {
    when (shape) {
        ParticleShape.CIRCLE -> {
            drawCircle(
                radius = size,
                color = color,
                center = Offset(x, y)
            )
        }
        ParticleShape.TRIANGLE -> {
            rotate(rotation, Offset(x, y)) {
                drawPath(
                    path = Path().apply {
                        moveTo(x, y - size)
                        lineTo(x + size, y + size)
                        lineTo(x - size, y + size)
                        close()
                    },
                    color = color
                )
            }
        }
        ParticleShape.STAR -> {
            rotate(rotation, Offset(x, y)) {
                drawPath(
                    path = createStarPath(x, y, size, size / 2, 5),
                    color = color
                )
            }
        }
    }
}

private fun createStarPath(
    cx: Float,
    cy: Float,
    outerRadius: Float,
    innerRadius: Float,
    points: Int
): Path {
    val path = Path()
    val angleStep = Math.PI.toFloat() / points

    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = i * angleStep - Math.PI.toFloat() / 2
        val x = cx + cos(angle) * radius
        val y = cy + sin(angle) * radius

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

data class Particle(
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val color: Color,
    val shape: ParticleShape,
    val rotation: Float,
    val rotationSpeed: Float,
    val lifetime: Float
)

enum class ParticleShape {
    CIRCLE, STAR, TRIANGLE
}
