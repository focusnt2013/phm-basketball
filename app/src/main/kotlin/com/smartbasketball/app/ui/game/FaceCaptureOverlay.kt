package com.smartbasketball.app.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FaceCaptureOverlay(
    modifier: Modifier = Modifier,
    isRecognizing: Boolean = false,
    isSuccess: Boolean = false,
    matchProgress: Float = 0f,
    confidence: Float = 0f,
    userName: String? = null,
    userRole: String? = null,
    userTitle: String? = null
) {
    val targetProgress = if (isSuccess && confidence > 0f) confidence else matchProgress
    
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "progressAnimation"
    )
    
    val primaryColor = when {
        isSuccess -> Color(0xFF00FF88)
        isRecognizing -> Color(0xFF00FFFF)
        else -> Color(0xFF00FFFF)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenHeight = size.height
            val radius = screenHeight * 0.375f
            val center = Offset(size.width / 2, size.height / 2)
            
            val clipPath = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        offset = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                )
            }
            clipPath(clipPath, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.7f),
                    size = size
                )
            }
            
            drawCircle(
                color = primaryColor.copy(alpha = 0.8f),
                radius = radius,
                center = center,
                style = Stroke(width = 4f)
            )
            
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 12f)
            )

            // 识别成功时在圆圈中央显示提示
            if (isSuccess) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 24.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        alpha = 220
                        isFakeBoldText = true
                    }
                    drawText("请点头确认开始游戏", center.x, center.y, paint)
                }
            }
            
            val innerRadius = radius - 15f
            
            if (animatedProgress > 0f) {
                val progressSweep = animatedProgress * 360f
                
                drawArc(
                    color = primaryColor.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
                
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
                
                drawArc(
                    color = primaryColor.copy(alpha = 0.5f),
                    startAngle = -90f,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
                    size = Size(innerRadius * 2, innerRadius * 2),
                    style = Stroke(width = 14f, cap = StrokeCap.Round)
                )
            }
        }
        
        // 识别成功时显示用户信息（在圆圈上方）
        if (isSuccess && userName != null) {
            Text(
                text = "${userTitle ?: ""} ${userName}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp)
            )
            if (userRole != null) {
                Text(
                    text = "($userRole)",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 75.dp)
                )
            }
        }
    }
}
