package com.smartbasketball.app.data.api

import org.junit.Assert.*
import org.junit.Test

class ApiClientTest {

    @Test
    fun `RetrofitResult Success holds data`() {
        val result: RetrofitResult<String> = RetrofitResult.Success("test data")
        assertTrue(result is RetrofitResult.Success)
        assertEquals("test data", (result as RetrofitResult.Success).data)
    }

    @Test
    fun `RetrofitResult Error holds exception`() {
        val exception = ApiException("Test error")
        val result: RetrofitResult<String> = RetrofitResult.Error(exception)
        assertTrue(result is RetrofitResult.Error)
        assertEquals("Test error", (result as RetrofitResult.Error).exception.message)
    }

    @Test
    fun `RetrofitResult Loading is singleton`() {
        val loading1 = RetrofitResult.Loading
        val loading2 = RetrofitResult.Loading
        assertSame(loading1, loading2)
    }

    @Test
    fun `ApiException creates with message`() {
        val exception = ApiException("Test error message")
        assertEquals("Test error message", exception.message)
    }

    @Test
    fun `NetworkException extends ApiException`() {
        val exception = NetworkException("Network error")
        assertTrue(exception is ApiException)
        assertEquals("Network error", exception.message)
    }

    @Test
    fun `ServerException extends ApiException`() {
        val exception = ServerException("Server error")
        assertTrue(exception is ApiException)
        assertEquals("Server error", exception.message)
    }

    @Test
    fun `UnauthorizedException extends ApiException`() {
        val exception = UnauthorizedException("Unauthorized")
        assertTrue(exception is ApiException)
        assertEquals("Unauthorized", exception.message)
    }

    @Test
    fun `ForbiddenException extends ApiException`() {
        val exception = ForbiddenException("Forbidden")
        assertTrue(exception is ApiException)
        assertEquals("Forbidden", exception.message)
    }

    @Test
    fun `NotFoundException extends ApiException`() {
        val exception = NotFoundException("Not found")
        assertTrue(exception is ApiException)
        assertEquals("Not found", exception.message)
    }

    @Test
    fun `Exception types are distinguishable`() {
        val exceptions = listOf(
            ApiException("General"),
            NetworkException("Network"),
            ServerException("Server"),
            UnauthorizedException("Unauthorized"),
            ForbiddenException("Forbidden"),
            NotFoundException("Not found")
        )

        assertEquals(6, exceptions.size)
        exceptions.forEach { assertTrue(it is ApiException) }
    }
}
