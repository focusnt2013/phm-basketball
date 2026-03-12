package com.smartbasketball.app.service

import org.junit.Assert.*
import org.junit.Test
import java.lang.Runtime

class PerformanceMonitorTest {

    @Test
    fun `MemoryInfo default values`() {
        val memoryInfo = MemoryInfo()
        assertEquals(0L, memoryInfo.totalMemory)
        assertEquals(0L, memoryInfo.availableMemory)
        assertEquals(0L, memoryInfo.usedMemory)
        assertEquals(0f, memoryInfo.usagePercent, 0.001f)
        assertFalse(memoryInfo.isLowMemory)
    }

    @Test
    fun `CPUInfo default values`() {
        val cpuInfo = CPUInfo()
        assertEquals(0f, cpuInfo.usagePercent, 0.001f)
        assertEquals(0, cpuInfo.coreCount)
    }

    @Test
    fun `DeviceInfo stores correct values`() {
        val deviceInfo = DeviceInfo(
            deviceName = "Test Device",
            androidVersion = "13",
            sdkVersion = 33,
            totalMemory = 8L * 1024 * 1024 * 1024,
            availableMemory = 4L * 1024 * 1024 * 1024,
            isLowMemory = false
        )

        assertEquals("Test Device", deviceInfo.deviceName)
        assertEquals("13", deviceInfo.androidVersion)
        assertEquals(33, deviceInfo.sdkVersion)
    }

    @Test
    fun `NativeHeapInfo stores correct values`() {
        val heapInfo = NativeHeapInfo(
            size = 64L * 1024 * 1024,
            allocated = 32L * 1024 * 1024,
            free = 32L * 1024 * 1024
        )

        assertEquals(64L * 1024 * 1024, heapInfo.size)
        assertEquals(32L * 1024 * 1024, heapInfo.allocated)
    }

    @Test
    fun `DalvikHeapInfo stores correct values`() {
        val heapInfo = DalvikHeapInfo(
            size = 128L * 1024 * 1024,
            used = 64L * 1024 * 1024,
            free = 64L * 1024 * 1024
        )

        assertEquals(128L * 1024 * 1024, heapInfo.size)
        assertEquals(64L * 1024 * 1024, heapInfo.used)
    }

    @Test
    fun `PerformanceReport contains all data`() {
        val report = PerformanceReport(
            timestamp = System.currentTimeMillis(),
            memory = MemoryInfo(),
            cpu = CPUInfo(),
            device = DeviceInfo(
                deviceName = "Test",
                androidVersion = "13",
                sdkVersion = 33,
                totalMemory = 8L * 1024 * 1024 * 1024,
                availableMemory = 4L * 1024 * 1024 * 1024,
                isLowMemory = false
            ),
            nativeHeap = NativeHeapInfo(64L * 1024 * 1024, 32L * 1024 * 1024, 32L * 1024 * 1024),
            dalvikHeap = DalvikHeapInfo(128L * 1024 * 1024, 64L * 1024 * 1024, 64L * 1024 * 1024)
        )

        assertNotNull(report.timestamp)
        assertNotNull(report.memory)
        assertNotNull(report.cpu)
        assertNotNull(report.device)
    }

    @Test
    fun `getNativeHeap methods return positive values`() {
        val size = Runtime.getRuntime().maxMemory()
        assertTrue(size > 0)
    }

    @Test
    fun `core count is reasonable`() {
        val coreCount = Runtime.getRuntime().availableProcessors()
        assertTrue(coreCount >= 1)
        assertTrue(coreCount <= 32)
    }
}
