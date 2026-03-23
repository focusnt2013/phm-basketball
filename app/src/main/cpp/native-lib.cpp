#include <jni.h>
#include <string>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <android/log.h>
#include <sys/stat.h>
#include <cstring>

#define LOG_TAG "GPIO_JNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define GPIO_SUCCESS 0
#define GPIO_ERROR   -1

bool gpio_is_exported(int gpio_num) {
    char path[64];
    struct stat st;
    snprintf(path, sizeof(path), "/sys/class/gpio/gpio%d", gpio_num);
    if (stat(path, &st) == 0 && S_ISDIR(st.st_mode)) {
        return true;
    }
    return false;
}

int gpio_export(int gpio_num) {
    if (gpio_is_exported(gpio_num)) {
        return GPIO_SUCCESS;
    }

    int fd;
    char buf[64];
    ssize_t len;

    fd = open("/sys/class/gpio/export", O_WRONLY);
    if (fd < 0) {
        LOGE("打开export失败: %s", strerror(errno));
        return GPIO_ERROR;
    }

    len = snprintf(buf, sizeof(buf), "%d", gpio_num);
    if (write(fd, buf, len) < 0) {
        LOGE("导出GPIO %d失败: %s", gpio_num, strerror(errno));
        close(fd);
        return GPIO_ERROR;
    }

    close(fd);
    LOGD("导出GPIO %d成功", gpio_num);
    return GPIO_SUCCESS;
}

int gpio_unexport(int gpio_num) {
    if (!gpio_is_exported(gpio_num)) {
        return GPIO_SUCCESS;
    }

    int fd;
    char buf[64];
    ssize_t len;

    fd = open("/sys/class/gpio/unexport", O_WRONLY);
    if (fd < 0) {
        LOGE("打开unexport失败: %s", strerror(errno));
        return GPIO_ERROR;
    }

    len = snprintf(buf, sizeof(buf), "%d", gpio_num);
    if (write(fd, buf, len) < 0) {
        LOGE("取消导出GPIO %d失败: %s", gpio_num, strerror(errno));
        close(fd);
        return GPIO_ERROR;
    }

    close(fd);
    LOGD("取消导出GPIO %d成功", gpio_num);
    return GPIO_SUCCESS;
}

int gpio_set_direction(int gpio_num, bool is_output) {
    if (!gpio_is_exported(gpio_num)) {
        LOGE("GPIO %d 未导出，无法设置方向", gpio_num);
        return GPIO_ERROR;
    }

    int fd;
    char path[64];
    const char* dir_str = is_output ? "out" : "in";

    snprintf(path, sizeof(path), "/sys/class/gpio/gpio%d/direction", gpio_num);
    fd = open(path, O_WRONLY);
    if (fd < 0) {
        LOGE("打开direction失败(GPIO%d): %s", gpio_num, strerror(errno));
        return GPIO_ERROR;
    }

    if (write(fd, dir_str, strlen(dir_str)) < 0) {
        LOGE("设置GPIO%d方向为%s失败: %s", gpio_num, dir_str, strerror(errno));
        close(fd);
        return GPIO_ERROR;
    }

    close(fd);
    LOGD("设置GPIO%d方向为%s成功", gpio_num, dir_str);
    return GPIO_SUCCESS;
}

int gpio_read_value(int gpio_num, int* value) {
    if (!gpio_is_exported(gpio_num)) {
        LOGE("GPIO %d 未导出，无法读取值", gpio_num);
        return GPIO_ERROR;
    }

    if (value == nullptr) {
        LOGE("value指针为空");
        return GPIO_ERROR;
    }

    char path[64];
    char buf[4];
    int fd;

    snprintf(path, sizeof(path), "/sys/class/gpio/gpio%d/value", gpio_num);
    fd = open(path, O_RDONLY);
    if (fd < 0) {
        LOGE("打开value失败(GPIO%d): %s", gpio_num, strerror(errno));
        return GPIO_ERROR;
    }

    memset(buf, 0, sizeof(buf));
    if (read(fd, buf, sizeof(buf) - 1) < 0) {
        LOGE("读取GPIO%d值失败: %s", gpio_num, strerror(errno));
        close(fd);
        return GPIO_ERROR;
    }

    *value = atoi(buf);
    close(fd);
    LOGD("读取GPIO%d值: %d", gpio_num, *value);
    return GPIO_SUCCESS;
}

int gpio_write_value(int gpio_num, int value) {
    if (!gpio_is_exported(gpio_num)) {
        LOGE("GPIO %d 未导出，无法写入值", gpio_num);
        return GPIO_ERROR;
    }

    if (value != 0 && value != 1) {
        LOGE("GPIO值非法：%d（仅支持0/1）", value);
        return GPIO_ERROR;
    }

    char path[64];
    char buf[2];
    int fd;

    snprintf(path, sizeof(path), "/sys/class/gpio/gpio%d/value", gpio_num);
    fd = open(path, O_WRONLY);
    if (fd < 0) {
        LOGE("打开value失败(GPIO%d): %s", gpio_num, strerror(errno));
        return GPIO_ERROR;
    }

    snprintf(buf, sizeof(buf), "%d", value);
    if (write(fd, buf, strlen(buf)) < 0) {
        LOGE("写入GPIO%d值%d失败: %s", gpio_num, value, strerror(errno));
        close(fd);
        return GPIO_ERROR;
    }

    close(fd);
    LOGD("写入GPIO%d值%d成功", gpio_num, value);
    return GPIO_SUCCESS;
}

extern "C" {
JNIEXPORT jint JNICALL
Java_com_smartbasketball_app_gpio_GpioJniWrapper_nativeExportGPIO(
        JNIEnv* env,
        jobject /* this */,
        jint gpio_num) {
    return (jint)gpio_export((int)gpio_num);
}

JNIEXPORT jint JNICALL
Java_com_smartbasketball_app_gpio_GpioJniWrapper_nativeUnexportGPIO(
        JNIEnv* env,
        jobject /* this */,
        jint gpio_num) {
    return (jint)gpio_unexport((int)gpio_num);
}

JNIEXPORT jint JNICALL
Java_com_smartbasketball_app_gpio_GpioJniWrapper_nativeSetGpioDirection(
        JNIEnv* env,
        jobject /* this */,
        jint gpio_num,
        jboolean is_output) {
    return (jint)gpio_set_direction((int)gpio_num, (bool)is_output);
}

JNIEXPORT jint JNICALL
Java_com_smartbasketball_app_gpio_GpioJniWrapper_nativeReadGpioValue(
        JNIEnv* env,
        jobject /* this */,
        jint gpio_num) {
    int value = -1;
    if (gpio_read_value((int)gpio_num, &value) == GPIO_SUCCESS) {
        return (jint)value;
    }
    return -1;
}

JNIEXPORT jint JNICALL
Java_com_smartbasketball_app_gpio_GpioJniWrapper_nativeWriteGpioValue(
        JNIEnv* env,
        jobject /* this */,
        jint gpio_num,
        jint value) {
    return (jint)gpio_write_value((int)gpio_num, (int)value);
}
}

