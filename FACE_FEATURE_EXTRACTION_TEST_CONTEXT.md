# 人脸特征提取一致性测试 - 完整上下文

## 调测环境

### 设备信息
- **设备ID**: 9479443a67f54d2c
- **连接方式**: USB调试
- **命令**: `adb -s 9479443a67f54d2c logcat -d | grep "SmartBasketball"`

### 查看日志
```bash
# 实时查看日志
adb -s 9479443a67f54d2c logcat -d | grep "SmartBasketball"

# 清除日志后重新测试
adb -s 9479443a67f54d2c logcat -c
```

### 关键日志标签
- `ScrfdDetector` - SCRFD模型加载和检测
- `extractFaceFeature` - 人脸特征提取
- `generateEmbedding` - 特征向量生成
- `相似度:` - 相似度计算结果

---

## 调测记录

### 2026-03-06 18:46 - SCRFD检测器Shape错误

**问题**: SCRFD检测器加载成功，但检测时报错：
```
Shape [1, 3, 640, 640], requires 1228800 elements but the buffer has 4915200 elements.
```

**根因**: `ScrfdDetector.kt:113` 创建了ByteBuffer但传给了需要FloatBuffer的OnnxTensor

**修复**:
```kotlin
// 错误
val inputTensor = OnnxTensor.createTensor(ortEnvironment!!, inputBuffer, ...)
// 正确
val inputTensor = OnnxTensor.createTensor(ortEnvironment!!, inputBuffer.asFloatBuffer(), ...)
```

**状态**: 已修复，待测试

---

## 测试目的

验证512维ONNX模型(buffalo_l/w600k_r50)能否解决192维TFLite模型的bug：多个不同用户之间的相似度高达81%以上。

## 核心问题

**现象**: 不同人的特征相似度高达75%-97%，无法区分不同用户

**期望**: 不同人之间的相似度应该很低（如<30%），同一人不同照片的相似度应该很高（如>80%）

## 测试用户

```kotlin
val testUserNames = listOf(
    "陈阳", "李榕洲", "刘老师", "谢君浩", "戴熙妍", 
    "李承凯", "李晨浩", "许成韬", "江逸朋", "姜立哲"
)
```

## 模型信息

- 模型文件: `buffalo_l/w600k_r50.onnx` (174MB)
- 输入: `(1, 3, 112, 112)` - NCHW格式
- 输出: 512维特征向量
- 标准模板关键点 (112x112):
  - 左眼: (30, 30)
  - 右眼: (82, 30)
  - 鼻子: (56, 58)
  - 嘴角左: (38, 82)
  - 嘴角右: (74, 82)

---

## 测试结果汇总

### 测试1: 简单缩放（无对齐）
- **裁剪**: 原始人脸区域，无扩展
- **对齐**: 无
- **结果**: 
  - 陈阳 vs 姜立哲 = 95.73%
  - 李榕洲 vs 许成韬 = 97.10%
  - 李晨浩 vs 许成韬 = 96.54%
  - **相似度范围: 84%-97%** ❌

### 测试2: 3点对齐（双眼+鼻子）
- **裁剪**: 原始人脸区域，无扩展
- **对齐**: 仿射变换 (3点)
- **结果**:
  - 陈阳 vs 谢君浩 = 80.25%
  - 李榕洲 vs 谢君浩 = 79.02%
  - 谢君浩 vs 许成韬 = 82.39%
  - **相似度范围: 79%-95%** ❌ (略有改善)

### 测试3: 5点对齐 + 3点后备 + 裁剪扩展10%
- **裁剪**: 人脸区域扩展10%
- **对齐**: 5点仿射变换，失败时回退3点
- **结果**:
  - 5点对齐全部失败（嘴角检测不到）
  - 回退到3点对齐
  - 李榕洲 vs 谢君浩 = 78.19%
  - 谢君浩 vs 姜立哲 = 81.58%
  - **相似度范围: 78%-95%** ❌

### 测试4: 禁用对齐（简单缩放）+ 裁剪扩展10%
- **裁剪**: 人脸区域扩展10%
- **对齐**: 禁用（直接简单缩放）
- **结果**:
  - 李晨浩 vs 许成韬 = 97.22%
  - 李承凯 vs 许成韬 = 95.23%
  - 陈阳 vs 李晨浩 = 95.16%
  - 刘老师 vs 姜立哲 = 70.69%
  - **相似度范围: 70%-97%** ❌

---

## 结论

1. **对齐有帮助但不够** - 3点对齐让部分相似度从93%降到79%，但整体仍然偏高
2. **禁用对齐后相似度仍然高** - 说明问题不只在对齐上
3. **5点对齐失败** - ML Kit未能检测到嘴角关键点
4. **可能的其他原因**:
   - 测试照片都是**蓝底证件照**，背景过于相似
   - 预处理参数可能不正确
   - 模型本身问题（需要使用专门的人脸预处理库）

---

## 尝试过的修复

| 修复 | 状态 | 说明 |
|------|------|------|
| ONNX输出缓冲区复制 | ✅ | 避免缓冲区复用 |
| normalize()创建新数组 | ✅ | 避免修改原数组 |
| HTTP设置no-cache | ✅ | 避免缓存问题 |
| 裁剪扩展 | ✅ | 添加10%扩展 |
| BGR→RGB | ✅ | 通道顺序修正 |
| [0,1]→[-1,1] | ✅ | 归一化修正 |
| ImageNet均值预处理 | ❌ | 不是根本原因 |
| 3点仿射对齐 | ⚠️ | 略有改善，不够 |
| 5点仿射对齐 | ❌ | 嘴角检测不到 |

---

## 相关代码

| 文件 | 作用 |
|------|------|
| `ScrfdDetector.kt` | SCRFD人脸检测器，加载det_10g.onnx模型 |
| `FaceRecognitionManager.kt:180-250` | 测试逻辑，提取10用户特征并计算相似度 |
| `FaceFeatureManager.kt:430-530` | 人脸检测、裁剪、SCRFD调用 |
| `FaceEmbeddingManager.kt:252-387` | ONNX推理、预处理、5点对齐 |

## SCRFD检测器

- **模型文件**: `det_10g.onnx` (在assets/models/目录下)
- **输入尺寸**: 640x640
- **输出**: 人脸框(bbox) + 5点关键点(kps)
- **5点关键点**: 左眼、右眼、鼻尖、嘴角左、嘴角右
- **调用流程**:
  1. `FaceFeatureManager.extractFaceFeature()` 加载SCRFD模型
  2. 调用 `scrfdDetector.detect(bitmap)` 检测人脸
  3. 获取 `scrfdFace.kps` 5点关键点
  4. 调用 `faceEmbeddingManager.generateEmbeddingWithScrfd()` 进行5点对齐

---

## SCRFD检测器工作流程

### 完整流程
1. **输入**: 原图（保持比例缩放，较长边≤640）
2. **SCRFD检测**: 检测人脸框(bbox)和5点关键点(kps)
3. **人脸裁剪**: 根据bbox裁剪人脸区域
4. **5点对齐**: 使用5点关键点进行仿射变换，得到112x112人脸
5. **特征提取**: 112x112人脸图输入到 buffalo_l 模型
6. **输出**: 512维特征向量

### 2026-03-06 22:20 - SCRFD优化记录

**问题1**: score值太低（最高0.1），检测不到人脸
- 根因: ONNX输出是logits，需要sigmoid激活
- 修复: 添加sigmoid处理 `score = 1 / (1 + exp(-rawScore))`
- 阈值从0.5降到0.02

**问题2**: 强制resize造成拉伸
- 根因: 强制缩放到640x640
- 修复: 保持原图比例，较长边缩放到640

**问题3**: 移除ML Kit后备
- SCRFD失败则抛出异常终止测试

---

## MediaPipe方案测试（2026-03-07）

### 方案5: MediaPipe Tasks Vision 0.10.8 FaceLandmarker

**背景**: 
- ONNX Runtime在Rockchip NPU上无法加速，推理4秒+
- 尝试使用MediaPipe Tasks Vision替代

**实现**:
- 使用 `FaceLandmarker` 提取468个人脸关键点
- 从关键点坐标(x,y,z)生成256维特征向量

**结果**:
- 相似度范围: 97%-100% ❌ 更差

**代码**:
- `FaceEmbeddingManager.kt` - 改用FaceLandmarker
- 模型: `face_landmarker.task`

**问题分析**:
- FaceLandmarker只提供468个关键点坐标
- 直接用关键点坐标作为特征无法区分不同人脸
- MediaPipe Tasks Vision 0.10.8 没有直接的FaceEmbedder API

---

## 最终结论

### 尝试过的所有方案

| 方案 | 状态 | 问题 |
|------|------|------|
| ScrfdDetector + ArcFace (ONNX) | ❌ | 检测4秒+，无法NPU加速 |
| MobileFaceNet | ❌ | 相似度85%+，无法区分 |
| MediaPipe FaceLandmarker | ❌ | 关键点坐标不是embedding |

### 核心问题

1. **ONNX方案性能问题**: Rockchip NPU不支持标准ONNX Runtime加速
2. **特征质量问题**: 无论使用哪种方案，相似度都过高，无法区分不同人

### 可能的原因

1. **测试数据问题**: 10个用户的证件照可能过于相似（蓝底证件照）
2. **缺少人脸对齐**: 没有使用Face Alignment对齐直接提取特征效果差
3. **模型问题**: 需要专门的Face Embedding模型配合Face Alignment

### 下一步建议

1. 确认测试照片是否为不同的人
2. 尝试添加人脸对齐(Face Alignment)预处理
3. 考虑使用专业的Face Embedding模型（如MobileFaceNet + Face Alignment）
4. 或使用其他厂商的人脸识别SDK
