package com.smartbasketball.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartbasketball.app.domain.model.GameSession
import com.smartbasketball.app.ui.theme.*

@Composable
fun GameScoreDisplay(
    madeBalls: Int,
    totalBalls: Int,
    accuracy: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            BasketballOrange.copy(alpha = 0.3f),
                            BasketballOrange.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$madeBalls",
                    fontSize = 72.sp,
                    color = BasketballOrange,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ $totalBalls",
                    fontSize = 36.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AccuracyBar(accuracy = accuracy)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "命中率 ${(accuracy * 100).toInt()}%",
            fontSize = 28.sp,
            color = when {
                accuracy >= 0.7f -> SuccessGreen
                accuracy >= 0.5f -> WarningYellow
                else -> ErrorRed
            },
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AccuracyBar(
    accuracy: Float,
    modifier: Modifier = Modifier
) {
    val animatedAccuracy by animateFloatAsState(
        targetValue = accuracy.coerceIn(0f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "accuracy"
    )

    Column(
        modifier = modifier.fillMaxWidth(0.7f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedAccuracy)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                ErrorRed,
                                WarningYellow,
                                SuccessGreen
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun TimerDisplay(
    timeInSeconds: Int,
    modifier: Modifier = Modifier
) {
    val minutes = timeInSeconds / 60
    val seconds = timeInSeconds % 60

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (timeInSeconds <= 10) ErrorRed.copy(alpha = 0.2f)
                    else SportsBlue.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$seconds",
                fontSize = 48.sp,
                color = if (timeInSeconds <= 10) ErrorRed else SportsBlue,
                fontWeight = FontWeight.Bold
            )
        }

        if (minutes > 0) {
            Text(
                text = " : $minutes",
                fontSize = 36.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = " 秒",
            fontSize = 24.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun BallsRemainingDisplay(
    remaining: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "剩余",
            fontSize = 24.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "$remaining",
                fontSize = 64.sp,
                color = if (remaining <= 5) ErrorRed else BasketballOrange,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " / $total 球",
                fontSize = 28.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        BallsIndicator(remaining = remaining, total = total)
    }
}

@Composable
fun BallsIndicator(
    remaining: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(total) { index ->
            val isRemaining = index < remaining
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRemaining) BasketballOrange
                        else Color.Gray.copy(alpha = 0.3f)
                    )
            )
        }
    }
}

@Composable
fun GameModeBadge(
    mode: String,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) BasketballOrange else Color.Gray.copy(alpha = 0.3f)
    ) {
        Text(
            text = mode,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun UserBadge(
    name: String,
    role: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = if (role != null) SportsBlue.copy(alpha = 0.2f) else WarningYellow.copy(alpha = 0.2f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "👤",
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                fontSize = 18.sp,
                color = Color.White
            )
            if (role != null) {
                Text(
                    text = " ($role)",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(
    text: String,
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            fontSize = 16.sp,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 12f,
    color: Color = BasketballOrange,
    backgroundColor: Color = Color.Gray.copy(alpha = 0.3f)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "progress"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val stroke = strokeWidth.dp.toPx()
            val radius = (size.minDimension - stroke) / 2

            drawCircle(
                color = backgroundColor,
                radius = radius,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
            )

            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
                topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            )
        }
    }
}

@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = BasketballOrange
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier
            .size(16.dp * scale)
            .clip(CircleShape)
            .background(color)
    )
}
