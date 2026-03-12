#!/bin/bash

# ========================================
#  智智慧投篮机 Android 构建脚本
# ========================================

# 设置项目路径
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "========================================"
echo "  智智慧投篮机 Android 构建脚本"
echo "========================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 函数定义
print_step() {
    echo -e "${BLUE}[$1/6]${NC} $2"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# 检查函数
check_command() {
    if command -v $1 &> /dev/null; then
        print_success "$1 已安装"
        return 0
    else
        print_warning "$1 未安装"
        return 1
    fi
}

# 检查环境
print_step "1" "检查构建环境..."
echo "项目路径: $PROJECT_DIR"

# 检查Java
if ! check_command java; then
    echo "请安装 JDK 17 或更高版本"
    echo "下载地址: https://adoptium.net/"
    exit 1
fi

java -version 2>&1 | head -1

# 检查Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/opt/android-sdk" ]; then
        export ANDROID_HOME="/opt/android-sdk"
    else
        print_warning "ANDROID_HOME 未设置"
        echo "请设置 ANDROID_HOME 环境变量或安装 Android SDK"
        echo "下载地址: https://developer.android.com/studio"
    fi
fi

if [ -n "$ANDROID_HOME" ]; then
    print_success "ANDROID_HOME: $ANDROID_HOME"
fi

# 检查Gradle
if [ -f "gradlew" ]; then
    print_success "Gradle Wrapper 已找到"
else
    print_error "Gradle Wrapper 未找到"
    echo "正在生成..."
    gradle wrapper --gradle-version 8.2
fi

if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    print_success "Gradle Wrapper JAR 已找到"
else
    print_error "Gradle Wrapper JAR 未找到"
    exit 1
fi

echo ""

# 清理构建
print_step "2" "清理构建..."
./gradlew clean --no-daemon
if [ $? -ne 0 ]; then
    print_error "清理失败"
    exit 1
fi
print_success "清理完成"

echo ""

# 运行代码检查
print_step "3" "运行代码检查..."
./gradlew lint --no-daemon
if [ $? -ne 0 ]; then
    print_warning "Lint检查发现问题，但继续构建..."
fi
print_success "代码检查完成"

echo ""

# 运行单元测试
print_step "4" "运行单元测试..."
./gradlew testDebugUnitTest --no-daemon
if [ $? -ne 0 ]; then
    print_error "单元测试失败"
    exit 1
fi
print_success "单元测试通过"

echo ""

# 构建调试版本
print_step "5" "构建调试版本..."
./gradlew assembleDebug --no-daemon
if [ $? -ne 0 ]; then
    print_error "调试版本构建失败"
    exit 1
fi
print_success "调试版本构建成功"

echo ""

# 构建发布版本
print_step "6" "构建发布版本..."
./gradlew assembleRelease --no-daemon
if [ $? -ne 0 ]; then
    print_error "发布版本构建失败"
    exit 1
fi
print_success "发布版本构建成功"

echo ""
echo "========================================"
echo "  构建完成！
echo "========================================"
echo ""
echo "输出文件:"
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo -e "  ${GREEN}调试版本:${NC} app/build/outputs/apk/debug/app-debug.apk"
fi
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    echo -e "  ${GREEN}发布版本:${NC} app/build/outputs/apk/release/app-release.apk"
fi
echo ""
echo "后续步骤:"
echo "  1. 运行 'adb install app/build/outputs/apk/debug/app-debug.apk' 安装调试版本"
echo "  2. 配置服务器地址: 修改 app/src/main/kotlin/.../NetworkModule.kt 中的 BASE_URL"
echo "  3. 集成百度AI SDK: 按照 SDK 文档配置 API Key"
echo "  4. 集成MediaPipe: 添加手势识别模型文件到 assets 目录"
echo ""
