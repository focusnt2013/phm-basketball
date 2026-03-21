package com.smartbasketball.app.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Context Extensions
val Context.screenWidth: Int
    get() = (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay.width

val Context.screenHeight: Int
    get() = (getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay.height

val Context.screenDensity: Float
    get() = resources.displayMetrics.density

val Context.isLandscape: Boolean
    get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

// DateTime Extensions
fun Long.toFormattedDateTime(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.toFormattedTime(pattern: String = "HH:mm:ss"): String {
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(this))
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diff = now - this

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
        diff < TimeUnit.HOURS.toMillis(1) -> "${diff / TimeUnit.MINUTES.toMillis(1)}分钟前"
        diff < TimeUnit.DAYS.toMillis(1) -> "${diff / TimeUnit.HOURS.toMillis(1)}小时前"
        diff < TimeUnit.DAYS.toMillis(7) -> "${diff / TimeUnit.DAYS.toMillis(1)}天前"
        else -> toFormattedDateTime()
    }
}

fun Int.toFormattedDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun Long.toDurationString(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}

// Number Extensions
fun Float.toPercentString(): String = "${(this * 100).toInt()}%"

fun Float.toAccuracyString(): String = String.format("%.1f%%", this * 100)

fun Float.clamp(min: Float, max: Float): Float = when {
    this < min -> min
    this > max -> max
    else -> this
}

fun Int.clamp(min: Int, max: Int): Int = when {
    this < min -> min
    this > max -> max
    else -> this
}

// Collection Extensions
fun <T> List<T>.safeGet(index: Int): T? = if (index in indices) this[index] else null

fun <T, R> List<T>.mapNotNullNull(transform: (T) -> R?): List<R> {
    return mapNotNull(transform)
}

fun <T> List<T>.secondOrNull(): T? = safeGet(1)
fun <T> List<T>.thirdOrNull(): T? = safeGet(2)

// String Extensions
fun String.isValidName(): Boolean = this.isNotBlank() && this.length in 1..50

fun String.truncate(maxLength: Int, suffix: String = "..."): String {
    return if (this.length > maxLength) {
        this.take(maxLength - suffix.length) + suffix
    } else {
        this
    }
}

// Compose Extensions
@Composable
fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}

@Composable
fun Float.toDp(): Dp {
    return with(LocalDensity.current) { this@toDp.dp }
}

@Composable
fun rememberScreenWidth(): Int {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp
}

@Composable
fun rememberScreenHeight(): Int {
    val configuration = LocalConfiguration.current
    return configuration.screenHeightDp
}

@Composable
fun rememberIsLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

// Device Info
fun getDeviceInfo(): String {
    return buildString {
        append("Brand: ${Build.BRAND}")
        append(", Model: ${Build.MODEL}")
        append(", SDK: ${Build.VERSION.SDK_INT}")
        append(", Android: ${Build.VERSION.RELEASE}")
    }
}

fun getDeviceId(): String {
    return "Android_${Build.MODEL}_${Build.BOARD}_${Build.ID}"
}

// Bitmask utilities
fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag

fun Int.addFlag(flag: Int): Int = this or flag

fun Int.removeFlag(flag: Int): Int = this and flag.inv()

// Result utilities
inline fun <T, R> T.runCatching(block: T.() -> R): Result<R> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Timer utilities
inline fun measureTimeMillis(block: () -> Unit): Long {
    val start = System.currentTimeMillis()
    block()
    return System.currentTimeMillis() - start
}

inline fun measureTimeNanos(block: () -> Unit): Long {
    val start = System.nanoTime()
    block()
    return System.nanoTime() - start
}

// ID generation
fun generateId(): String = UUID.randomUUID().toString()

fun generateTimestampId(): String = "${System.currentTimeMillis()}_${(0..9999).random()}"

// Locale utilities
fun getCurrentLocale(): Locale {
    return Locale.getDefault()
}

fun formatPhoneNumber(phone: String): String {
    return phone.replace(Regex("[^0-9+]"), "")
}
