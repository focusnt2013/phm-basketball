package com.smartbasketball.app.ui.components

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.smartbasketball.app.ui.theme.BasketballOrange
import com.smartbasketball.app.ui.theme.DarkBackground

@Composable
fun LeaderboardScreen(
    schoolId: String,
    userId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "运动排行榜",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isLoading) {
                LinearProgressIndicator(
                    progress = loadProgress / 100f,
                    modifier = Modifier
                        .width(100.dp)
                        .height(4.dp),
                    color = BasketballOrange
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LeaderboardWebView(
                schoolId = schoolId,
                userId = userId,
                onPageStarted = {
                    isLoading = true
                    loadProgress = 0
                    errorMessage = null
                },
                onPageProgressChanged = { progress ->
                    loadProgress = progress
                },
                onPageFinished = { success ->
                    isLoading = false
                    if (!success) {
                        errorMessage = "页面加载失败"
                    }
                },
                onError = { error ->
                    errorMessage = error
                }
            )

            if (errorMessage != null) {
                ErrorContent(
                    message = errorMessage!!,
                    onRetry = { }
                )
            }

            if (isLoading && loadProgress > 0) {
                LoadingContent(progress = loadProgress)
            }
        }
    }
}

@Composable
fun LeaderboardWebView(
    schoolId: String,
    userId: String?,
    onPageStarted: () -> Unit,
    onPageProgressChanged: (Int) -> Unit,
    onPageFinished: (Boolean) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val url = remember(schoolId, userId) {
        buildString {
            append("https://school.xixiti.com/rank_basketball.htm")
            append("?school_id=$schoolId")
            userId?.let { append("&user_id=$it") }
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    builtInZoomControls = false
                    displayZoomControls = false
                    cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onPageProgressChanged(newProgress)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        onPageStarted()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        onPageFinished(true)
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        onError(description ?: "加载错误")
                        onPageFinished(false)
                    }
                }

                loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}

@Composable
fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "加载失败",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BasketballOrange
                )
            ) {
                Text("重新加载")
            }
        }
    }
}

@Composable
fun LoadingContent(
    progress: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = BasketballOrange,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "正在加载榜单... $progress%",
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun SceneRankContent(
    onFaceDetected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "运动排行榜",
                fontSize = 56.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "学校榜单",
                fontSize = 36.sp,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(60.dp))

            PulsingText(
                text = "请面向摄像头，人脸识别登录",
                fontSize = 28.sp,
                color = BasketballOrange
            )
        }
    }
}

@Composable
fun PulsingText(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "scale"
    )

    Text(
        text = text,
        fontSize = fontSize,
        color = color,
        modifier = modifier
    )
}
