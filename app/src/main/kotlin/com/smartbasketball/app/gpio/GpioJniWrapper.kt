package com.smartbasketball.app.gpio

import com.smartbasketball.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream

class GpioJniWrapper {

    private var hasRootPermission = false

    init {
        AppLogger.d("GpioJniWrapper: 初始化GPIO JNI封装")
    }

    suspend fun requestRootPermission(): Boolean = withContext(Dispatchers.IO) {
        if (hasRootPermission) {
            AppLogger.d("GpioJniWrapper: 已有ROOT权限")
            return@withContext true
        }

        AppLogger.d("GpioJniWrapper: 请求ROOT权限...")
        
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)

            os.writeBytes("chmod 777 /sys/class/gpio/export\n")
            os.writeBytes("chmod 777 /sys/class/gpio/unexport\n")
            os.writeBytes("chmod -R 777 /sys/class/gpio/\n")
            os.writeBytes("exit\n")
            os.flush()

            val exitCode = process.waitFor()
            os.close()
            process.destroy()

            if (exitCode == 0) {
                hasRootPermission = true
                AppLogger.d("GpioJniWrapper: ROOT权限获取成功")
                true
            } else {
                AppLogger.e("GpioJniWrapper: ROOT权限获取失败，exitCode=$exitCode")
                false
            }
        } catch (e: Exception) {
            AppLogger.e("GpioJniWrapper: ROOT权限获取异常: ${e.message}")
            false
        }
    }

    fun hasRootPermission(): Boolean = hasRootPermission

    fun exportGpio(gpioNum: Int): Int {
        return try {
            val result = nativeExportGPIO(gpioNum)
            AppLogger.d("GpioJniWrapper: exportGPIO($gpioNum) = $result")
            result
        } catch (e: Exception) {
            AppLogger.e("GpioJniWrapper: exportGPIO异常: ${e.message}")
            -1
        }
    }

    fun unexportGpio(gpioNum: Int): Int {
        return try {
            val result = nativeUnexportGPIO(gpioNum)
            AppLogger.d("GpioJniWrapper: unexportGPIO($gpioNum) = $result")
            result
        } catch (e: Exception) {
            AppLogger.e("GpioJniWrapper: unexportGPIO异常: ${e.message}")
            -1
        }
    }

    fun setGpioDirection(gpioNum: Int, isOutput: Boolean): Int {
        return try {
            val result = nativeSetGpioDirection(gpioNum, isOutput)
            AppLogger.d("GpioJniWrapper: setGpioDirection($gpioNum, $isOutput) = $result")
            result
        } catch (e: Exception) {
            AppLogger.e("GpioJniWrapper: setGpioDirection异常: ${e.message}")
            -1
        }
    }

    fun readGpioValue(gpioNum: Int): Int {
        return try {
            val value = nativeReadGpioValue(gpioNum)
            if (value >= 0) {
                AppLogger.v("GpioJniWrapper: readGpioValue($gpioNum) = $value")
            } else {
                AppLogger.w("GpioJniWrapper: readGpioValue($gpioNum) 失败，返回 $value")
            }
            value
        } catch (e: Exception) {
            AppLogger.e("GpioJniWrapper: readGpioValue异常: ${e.message}")
            -1
        }
    }

    fun writeGpioValue(gpioNum: Int, value: Int): Int {
        return try {
            val result = nativeWriteGpioValue(gpioNum, value)
            AppLogger.d("GpioJniWrapper: writeGpioValue($gpioNum, $value) = $result")
            result
        } catch (e: Exception) {
            AppLogger.e("GpioJniWrapper: writeGpioValue异常: ${e.message}")
            -1
        }
    }

    companion object {
        private const val TAG = "GpioJniWrapper"

        init {
            System.loadLibrary("smartbasketballgpio")
        }

        @JvmStatic
        private external fun nativeExportGPIO(gpioNum: Int): Int

        @JvmStatic
        private external fun nativeUnexportGPIO(gpioNum: Int): Int

        @JvmStatic
        private external fun nativeSetGpioDirection(gpioNum: Int, isOutput: Boolean): Int

        @JvmStatic
        private external fun nativeReadGpioValue(gpioNum: Int): Int

        @JvmStatic
        private external fun nativeWriteGpioValue(gpioNum: Int, value: Int): Int
    }
}
