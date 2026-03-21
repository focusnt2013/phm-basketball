package com.smartbasketball.app.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

private val TestBgColor = Color(0xFF1A1A1A)

private val fixedUserNames = listOf(
    "陈阳", "李榕洲", "刘老师", "谢君浩", "戴熙妍",
    "李承凯", "李晨浩", "许成韬", "江逸朋", "姜立哲"
)

@Composable
fun TestFaceImagesScreen(
    testFaces: List<Pair<String, String>>,
    testFaceOriginalUrls: List<Pair<String, String>> = emptyList(),
    testFaceData: List<Triple<String, FloatArray, Float>> = emptyList(),
    similarityMatrix: List<List<Float>> = emptyList(),
    testExtractTimes: Map<String, Long> = emptyMap(),
    testLogs: String = "",
    onBack: () -> Unit
) {
    val facePaths = remember(testFaces) {
        testFaces.associate { it.first to it.second }
    }

    val originalUrls = remember(testFaceOriginalUrls) {
        testFaceOriginalUrls.associate { it.first to it.second }
    }

    // 检查是否只测试了1个用户
    val isSingleUserTest = testFaces.size == 1 && testExtractTimes.size == 1

    val isComplete = if (isSingleUserTest) {
        testExtractTimes.isNotEmpty()
    } else {
        similarityMatrix.size == 10 && similarityMatrix.all { it.size == 10 }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TestBgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A2A2A))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, modifier = Modifier.padding(0.dp)) {
                Text(text = "← 返回", fontSize = 16.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isSingleUserTest) "人脸特征提取测试 (1x1)" else "人脸相似度矩阵 (10x10)",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (testLogs.isNotEmpty()) {
                Text(
                    text = testLogs,
                    fontSize = 12.sp,
                    color = if (testLogs.contains("失败") || testLogs.contains("错误")) Color.Red 
                           else if (testLogs.contains("成功") || testLogs.contains("完成")) Color.Green 
                           else Color.Yellow,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(2f),
                    textAlign = TextAlign.End
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!isComplete) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (colIdx in 0..10) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(1.dp, Color.Gray)
                            .background(Color(0xFF3A3A3A))
                            .padding(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (colIdx == 0) {
                            Text("用户", fontSize = 14.sp, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        } else {
                            val name = fixedUserNames[colIdx - 1]
                            // 列标题显示原图
                            val originalUrl = originalUrls[name]
                            if (originalUrl != null && originalUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = originalUrl,
                                    contentDescription = "$name 原图",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .border(1.dp, Color.Gray),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                // 如果没有原图，显示裁剪图
                                val path = facePaths[name]
                                if (path != null && path.isNotEmpty()) {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = "$name 裁剪图",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .border(1.dp, Color.Gray),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            for (rowIdx in 0 until 10) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (colIdx in 0..10) {
                        val isFirstCol = colIdx == 0
                        val isDiagonal = rowIdx == colIdx - 1
                        
                        val similarity = if (!isFirstCol && isComplete && 
                            rowIdx < similarityMatrix.size && colIdx - 1 < similarityMatrix[rowIdx].size) {
                            similarityMatrix[rowIdx][colIdx - 1]
                        } else -1f

                        val cellColor = when {
                            isDiagonal -> Color(0xFF4CAF50)
                            similarity < 0 -> Color(0xFF2A2A2A)
                            similarity > 0.7f -> Color(0xFFE53935)
                            similarity > 0.5f -> Color(0xFFFF9800)
                            similarity > 0.3f -> Color(0xFFFFEB3B)
                            else -> Color(0xFF4CAF50)
                        }

                        if (isFirstCol) {
                            val name = fixedUserNames[rowIdx]
                            val extractTime = testExtractTimes[name]
                            val isSuccess = extractTime != null && extractTime > 0
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(1.dp, Color.Gray)
                                    .background(if (isSuccess) Color(0xFF3A3A3A) else Color(0xFF5A3A3A))
                                    .padding(2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isSuccess) {
                                    val seconds = (extractTime ?: 0) / 1000.0
                                    Text(
                                        text = String.format("%.1fs ✓", seconds),
                                        fontSize = 11.sp,
                                        color = Color(0xFF00FF00),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else if (extractTime != null && extractTime == 0L) {
                                    Text(
                                        text = "处理中...",
                                        fontSize = 10.sp,
                                        color = Color(0xFFFFAA00),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text(
                                        text = "等待中...",
                                        fontSize = 10.sp,
                                        color = Color(0xFFAAAAAA),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // 判断是否是单用户测试模式的对角线位置
                            val isDiagonalCell = isSingleUserTest && rowIdx == colIdx - 1
                            
                            if (isSingleUserTest) {
                                // 单用户测试模式：对角线显示裁剪图，其他显示-
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(1.dp, Color.Gray)
                                        .background(Color(0xFF2A2A2A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isDiagonalCell) {
                                        val name = fixedUserNames[rowIdx]
                                        val path = facePaths[name]
                                        if (path != null && path.isNotEmpty()) {
                                            AsyncImage(
                                                model = File(path),
                                                contentDescription = "$name 裁剪图",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .fillMaxHeight(),
                                                contentScale = ContentScale.Fit
                                            )
                                        } else {
                                            Text("-", fontSize = 16.sp, color = Color.White)
                                        }
                                    } else {
                                        Text("-", fontSize = 14.sp, color = Color.Gray)
                                    }
                                }
                            } else {
                                // 完整矩阵模式
                                val similarity = if (!isFirstCol && isComplete && 
                                    rowIdx < similarityMatrix.size && colIdx - 1 < similarityMatrix[rowIdx].size) {
                                    similarityMatrix[rowIdx][colIdx - 1]
                                } else -1f

                                val cellColor = when {
                                    isDiagonal -> Color(0xFF4CAF50)
                                    similarity < 0 -> Color(0xFF2A2A2A)
                                    similarity > 0.7f -> Color(0xFFE53935)
                                    similarity > 0.5f -> Color(0xFFFF9800)
                                    similarity > 0.3f -> Color(0xFFFFEB3B)
                                    else -> Color(0xFF4CAF50)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(1.dp, Color.Gray)
                                        .background(cellColor.copy(alpha = if (similarity < 0) 0.3f else 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (similarity >= 0) {
                                        Text(
                                            text = String.format("%.0f%%", similarity * 100),
                                            fontSize = 16.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2A2A2A))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(color = Color(0xFF4CAF50), label = "本人")
            LegendItem(color = Color(0xFFE53935), label = ">70%")
            LegendItem(color = Color(0xFFFF9800), label = "50-70%")
            LegendItem(color = Color(0xFFFFEB3B), label = "30-50%")
            LegendItem(color = Color(0xFF4CAF50), label = "<30%")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color.copy(alpha = 0.5f))
                .border(1.dp, color)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(text = label, fontSize = 9.sp, color = Color.White)
    }
}
