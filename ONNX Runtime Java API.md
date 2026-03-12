SmartJavaAI 在 Android 端会自动识别 assets目录，你只需要在配置中指定相对于 assets的路径即可。
一：加载人脸检测模型 (det_10g.onnx)
// 1. 创建检测器配置
DetectorModelConfig config = new DetectorModelConfig();
// 2. 设置模型类型为自定义 ONNX
config.setModelEnum(DetectorModelEnum.CUSTOM);
// 3. 关键：设置模型路径（assets 目录下的相对路径）
config.setModelPath("det_10g.onnx");
// 4. 设置模型参数（根据你的模型训练配置调整）
config.putCustomParam("width", 640);   // 输入图像宽度
config.putCustomParam("height", 640);  // 输入图像高度
config.putCustomParam("threshold", 0.5f); // 置信度阈值
// 5. 加载模型
DetectorModel model = DetectorModelFactory.getInstance().getModel(config);

二：加载人脸识别模型 (w600k_r50.onnx)
// 1). 创建人脸引擎
FaceEngine engine = new FaceEngine();
// 2). 创建配置
FaceModelConfig config = new FaceModelConfig();
// 3). 设置模型路径
config.setModelPath("w600k_r50.onnx");
// 4). 初始化引擎（加载模型）
engine.init(config);

三. 关键注意事项
路径格式：路径直接写文件名（如 "det_10g.onnx"），不要加 file://前缀或绝对路径。SmartJavaAI 会自动在 assets目录下查找。
模型兼容性：确保你的 ONNX 模型是由支持的框架（如 PyTorch、TensorFlow）导出，且算子版本与 SmartJavaAI 内置的 ONNX Runtime 兼容。
参数匹配：putCustomParam中的参数（如 width、height）必须与模型训练时的输入尺寸一致，否则会导致推理错误。
通过以上配置，SmartJavaAI 会自动从 assets目录加载并解析 ONNX 模型文件，无需手动处理文件流读取。