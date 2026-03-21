package com.smartbasketball.app.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartbasketball.app.data.local.UserEntity
import com.smartbasketball.app.ui.theme.BasketballOrange
import kotlinx.coroutines.delay

@Composable
fun RecognitionSuccessDialog(
    user: UserEntity,
    confidence: Float,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    var showButtons by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.5f,
        animationSpec = tween(500),
        label = "scale"
    )
    
    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        delay(300)
        showButtons = true
    }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .alpha(if (showContent) 1f else 0f)
                .background(
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFF4CAF50), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 64.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "识别成功",
                    fontSize = 36.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = user.name,
                    fontSize = 32.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = user.title,
                    fontSize = 20.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "置信度: ${(confidence * 100).toInt()}%",
                    fontSize = 16.sp,
                    color = BasketballOrange
                )

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "是否确认开始投篮？",
                    fontSize = 24.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = showButtons,
                    enter = fadeIn() + scaleIn()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            ),
                            modifier = Modifier
                                .width(160.dp)
                                .height(56.dp)
                        ) {
                            Text(
                                text = "取消",
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        Button(
                            onClick = onConfirm,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BasketballOrange
                            ),
                            modifier = Modifier
                                .width(160.dp)
                                .height(56.dp)
                        ) {
                            Text(
                                text = "开始投篮",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecognitionFailedOverlay(
    message: String,
    onDismiss: () -> Unit
) {
    var showContent by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.5f,
        animationSpec = tween(300),
        label = "scale"
    )
    
    LaunchedEffect(Unit) {
        showContent = true
        delay(2000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .alpha(if (showContent) 1f else 0f)
                .background(
                    color = Color.Red.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Text(
                text = message,
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}
