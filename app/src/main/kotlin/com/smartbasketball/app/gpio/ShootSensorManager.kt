package com.smartbasketball.app.gpio

import com.smartbasketball.app.util.AppLogger
import com.smartbasketball.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ShootSensorManager(
    private val gpioWrapper: GpioJniWrapper = GpioJniWrapper()
) {
    private var pollingJob: Job? = null
    private var isInitialized = false
    private var isListening = false

    private var lastGpioValue = -1
    private var lastTriggerTime = 0L
    private var firstTriggerTime = 0L
    private var hasFirstTrigger = false

    private var onShootCallback: ((Boolean) -> Unit)? = null

    private val gpioNum = Constants.GPIO.SHOOT_SENSOR_PIN
    private val pollingInterval = Constants.GPIO.POLLING_INTERVAL_MS
    private val debounceMs = Constants.GPIO.DEBOUNCE_MS
    private val secondTriggerWindow = Constants.GPIO.SECOND_TRIGGER_WINDOW_MS

    fun initialize(): Boolean {
        if (isInitialized) {
            AppLogger.d("ShootSensorManager: 已初始化")
            return true
        }

        AppLogger.d("ShootSensorManager: 开始初始化 GPIO $gpioNum")

        AppLogger.d("ShootSensorManager: 请求ROOT权限...")
        val hasRoot = runBlocking { gpioWrapper.requestRootPermission() }
        if (!hasRoot) {
            AppLogger.e("ShootSensorManager: ROOT权限获取失败，GPIO操作可能无法执行")
        }

        val exportResult = gpioWrapper.exportGpio(gpioNum)
        if (exportResult != 0) {
            AppLogger.e("ShootSensorManager: 导出GPIO失败")
            return false
        }

        val dirResult = gpioWrapper.setGpioDirection(gpioNum, false)
        if (dirResult != 0) {
            AppLogger.e("ShootSensorManager: 设置GPIO方向失败")
            gpioWrapper.unexportGpio(gpioNum)
            return false
        }

        lastGpioValue = gpioWrapper.readGpioValue(gpioNum)
        if (lastGpioValue < 0) {
            AppLogger.e("ShootSensorManager: 读取初始GPIO值失败")
            gpioWrapper.unexportGpio(gpioNum)
            return false
        }

        isInitialized = true
        AppLogger.d("ShootSensorManager: 初始化成功，初始电平=$lastGpioValue")
        return true
    }

    fun startListening(onShoot: (Boolean) -> Unit) {
        if (!isInitialized) {
            AppLogger.e("ShootSensorManager: 未初始化，无法开始监听")
            return
        }

        if (isListening) {
            AppLogger.d("ShootSensorManager: 已在监听中")
            return
        }

        onShootCallback = onShoot
        isListening = true
        hasFirstTrigger = false
        lastTriggerTime = 0L
        firstTriggerTime = 0L

        AppLogger.d("ShootSensorManager: 开始监听投篮感应器")

        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && isListening) {
                val currentValue = gpioWrapper.readGpioValue(gpioNum)

                if (currentValue >= 0) {
                    processGpioValue(currentValue)
                }

                delay(pollingInterval)
            }
        }
    }

    fun stopListening() {
        if (!isListening) {
            return
        }

        isListening = false
        pollingJob?.cancel()
        pollingJob = null
        hasFirstTrigger = false

        AppLogger.d("ShootSensorManager: 停止监听")
    }

    fun release() {
        stopListening()

        if (isInitialized) {
            gpioWrapper.unexportGpio(gpioNum)
            isInitialized = false
            AppLogger.d("ShootSensorManager: 释放资源")
        }
    }

    private fun processGpioValue(value: Int) {
        val now = System.currentTimeMillis()

        if (value != lastGpioValue) {
            val edgeType = if (value == 1 && lastGpioValue == 0) "上升沿" else "下降沿"
            AppLogger.v("ShootSensorManager: 电平变化 $lastGpioValue -> $value ($edgeType)")

            if (value == 1) {
                handleRisingEdge(now)
            }

            lastGpioValue = value
        }

        if (hasFirstTrigger) {
            val elapsed = now - firstTriggerTime
            if (elapsed > secondTriggerWindow) {
                AppLogger.d("ShootSensorManager: 超过${secondTriggerWindow}ms无二次触发，判定为未命中")
                hasFirstTrigger = false
                onShootCallback?.invoke(false)
            }
        }
    }

    private fun handleRisingEdge(timestamp: Long) {
        if (!hasFirstTrigger) {
            firstTriggerTime = timestamp
            hasFirstTrigger = true
            AppLogger.d("ShootSensorManager: 第一次触发，等待二次确认...")
        } else {
            val interval = timestamp - lastTriggerTime
            lastTriggerTime = timestamp

            if (interval < debounceMs) {
                AppLogger.v("ShootSensorManager: 间隔${interval}ms < 防抖${debounceMs}ms，忽略")
                return
            }

            AppLogger.d("ShootSensorManager: 二次触发，间隔${interval}ms，判定为命中！")
            hasFirstTrigger = false
            onShootCallback?.invoke(true)
        }
    }

    fun isReady(): Boolean = isInitialized
}
