package com.smartbasketball.app.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.smartbasketball.app.ui.game.viewmodel.ShotAnimation
import com.smartbasketball.app.ui.theme.BasketballOrange
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ShotAnimationView(
    animation: ShotAnimation,
    modifier: Modifier = Modifier
) {
    val isMade = animation == ShotAnimation.MADE
    
    // 动画进度 0 -> 1
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearEasing
        ),
        label = "shotAnimation"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            // 绘制篮筐
            val rimY = centerY - 80f
            val rimWidth = 120f
            
            // 篮筐（半圆弧）
            drawArc(
                color = Color.Red,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - rimWidth / 2, rimY - 10f),
                size = androidx.compose.ui.geometry.Size(rimWidth, 20f),
                style = Stroke(width = 6f)
            )
            
            // 篮板
            drawRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(centerX - rimWidth / 2 - 10f, rimY - 80f),
                size = androidx.compose.ui.geometry.Size(rimWidth + 20f, 80f)
            )
            
            // 绘制篮球轨迹
            val ballRadius = 20f
            val startX = centerX - 150f
            val startY = centerY + 150f
            
            if (isMade) {
                // 进球轨迹：抛物线进网
                val t = progress
                val x = startX + (centerX - startX) * t
                // 抛物线轨迹
                val y = startY - (startY - rimY) * t - 100 * sin(t * PI).toFloat()
                
                // 只在有效范围内绘制
                if (t <= 1f) {
                    // 篮球
                    drawCircle(
                        color = BasketballOrange,
                        radius = ballRadius,
                        center = Offset(x, y)
                    )
                    // 篮球纹路
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.3f),
                        radius = ballRadius,
                        center = Offset(x, y),
                        style = Stroke(width = 2f)
                    )
                }
                
                // 进网效果（最后阶段）
                if (t > 0.7f) {
                    val netAlpha = ((t - 0.7f) / 0.3f).coerceIn(0f, 1f)
                    // 篮网
                    for (i in 0..5) {
                        val netX = centerX - 40f + i * 16f
                        drawLine(
                            color = Color.White.copy(alpha = netAlpha * 0.8f),
                            start = Offset(centerX - rimWidth / 2 + 10f + i * 20f, rimY),
                            end = Offset(netX, rimY + 60f),
                            strokeWidth = 2f
                        )
                    }
                }
            } else {
                // 未进轨迹：碰到框弹开
                val t = progress.coerceIn(0f, 1f)
                val x: Float
                val y: Float
                
                if (t < 0.6f) {
                    // 前60%：抛物线到篮筐位置
                    val t1 = t / 0.6f
                    x = startX + (centerX + 30f - startX) * t1
                    y = startY - (startY - rimY + 20f) * t1 - 80 * sin(t1 * PI).toFloat()
                } else {
                    // 后40%：弹开
                    val t2 = (t - 0.6f) / 0.4f
                    x = centerX + 30f + t2 * 100f
                    y = rimY + 20f + t2 * 80f
                }
                
                // 篮球
                drawCircle(
                    color = BasketballOrange,
                    radius = ballRadius,
                    center = Offset(x, y)
                )
                // 篮球纹路
                drawCircle(
                    color = Color.Black.copy(alpha = 0.3f),
                    radius = ballRadius,
                    center = Offset(x, y),
                    style = Stroke(width = 2f)
                )
                
                // 碰撞星星效果
                if (t >= 0.5f && t < 0.7f) {
                    val starAlpha = 1f - (t - 0.5f) / 0.2f
                    drawCircle(
                        color = Color.Yellow.copy(alpha = starAlpha),
                        radius = 8f,
                        center = Offset(centerX + 30f, rimY + 10f)
                    )
                }
            }
        }
    }
}

@Composable
fun ShotAnimationOverlay(
    animation: ShotAnimation?,
    modifier: Modifier = Modifier
) {
    if (animation != null) {
        ShotAnimationView(
            animation = animation,
            modifier = modifier
        )
    }
}
