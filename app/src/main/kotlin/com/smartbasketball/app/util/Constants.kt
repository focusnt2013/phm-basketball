package com.smartbasketball.app.util

object Constants {

    // Game Settings
    object Game {
        const val DEFAULT_COUNTDOWN_TIME = 60
        const val DEFAULT_FIXED_BALL_COUNT = 20
        const val COUNTDOWN_STEP = 15
        const val BALL_COUNT_STEP = 5
        const val MIN_COUNTDOWN_TIME = 30
        const val MAX_COUNTDOWN_TIME = 180
        const val MIN_BALL_COUNT = 10
        const val MAX_BALL_COUNT = 50
    }

    // Timeout Settings
    object Timeout {
        const val STANDBY_TIMEOUT = 30
        const val LEADERBOARD_TIMEOUT = 30
        const val FACE_DETECT_TIMEOUT = 5
        const val GESTURE_HOLD_DURATION = 500L
        const val NETWORK_TIMEOUT = 30_000L
        const val SYNC_INTERVAL = 5
    }

    // Recognition Settings
    object Recognition {
        const val FACE_THRESHOLD = 0.85f
        const val GESTURE_THRESHOLD = 0.85f
        const val FACE_DETECTION_INTERVAL = 100L
        const val GESTURE_DETECTION_INTERVAL = 100L
    }

    // Ball Detection Settings
    object BallDetection {
        const val DEFAULT_DEBOUNCE_MS = 50L
        const val STUCK_THRESHOLD_MS = 1000L
        const val DEFAULT_BAUDRATE = 9600
        const val SERIAL_READ_INTERVAL_MS = 10L
    }

    // Camera Settings
    object Camera {
        const val PREVIEW_WIDTH = 640
        const val PREVIEW_HEIGHT = 480
        const val PREVIEW_FPS = 30
        const val FRAME_INTERVAL_MS = 100L / PREVIEW_FPS
    }

    // USB Settings
    object USB {
        const val DEVICE_OPEN_TIMEOUT = 2500L
        const val PERMISSION_REQUEST_CODE = 1001
    }

    // Network Settings
    object Network {
        const val BASE_URL = "https://your-server.com/"
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 30L
        const val WRITE_TIMEOUT = 30L
        const val MAX_RETRY_COUNT = 3
        const val CACHE_SIZE = 10 * 1024 * 1024L
    }

    // Data Settings
    object Data {
        const val MAX_PENDING_RECORDS = 100
        const val MAX_ERROR_LOGS = 10
        const val SYNC_WIFI_ONLY = true
    }

    // Storage Keys
    object Storage {
        const val KEY_SCHOOL_ID = "school_id"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_DEFAULT_MODE = "default_mode"
        const val KEY_COUNTDOWN_TIME = "countdown_time"
        const val KEY_FIXED_BALL_COUNT = "fixed_ball_count"
        const val KEY_FACE_THRESHOLD = "face_threshold"
        const val KEY_GESTURE_THRESHOLD = "gesture_threshold"
        const val KEY_BALL_DEBOUNCE_MS = "ball_debounce_ms"
        const val KEY_STANDBY_TIMEOUT = "standby_timeout"
        const val KEY_LEADERBOARD_TIMEOUT = "leaderboard_timeout"
        const val KEY_SYNC_INTERVAL = "sync_interval"
        const val KEY_SYNC_WIFI_ONLY = "sync_wifi_only"
        const val KEY_VOLUME = "volume"
        const val KEY_SPEECH_RATE = "speech_rate"
    }

    // Intent Actions
    object Intent {
        const val ACTION_USB_PERMISSION = "com.smartbasketball.app.USB_PERMISSION"
        const val ACTION_DEVICE_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        const val ACTION_DEVICE_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
    }

    // Broadcast Actions
    object Broadcast {
        const val ACTION_FACE_DETECTED = "com.smartbasketball.app.FACE_DETECTED"
        const val ACTION_GAME_STARTED = "com.smartbasketball.app.GAME_STARTED"
        const val ACTION_GAME_ENDED = "com.smartbasketball.app.GAME_ENDED"
        const val ACTION_BALL_DETECTED = "com.smartbasketball.app.BALL_DETECTED"
        const val ACTION_SYNC_COMPLETED = "com.smartbasketball.app.SYNC_COMPLETED"
    }

    // Extras Keys
    object Extras {
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_GAME_MODE = "game_mode"
        const val KEY_GAME_SCORE = "game_score"
        const val KEY_GAME_DURATION = "game_duration"
        const val KEY_RECORD_ID = "record_id"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_DEVICE_PATH = "device_path"
    }

    // File Names
    object Files {
        const val ERROR_LOG_DIR = "error_logs"
        const val CRASH_LOG_FILE = "crash_log.txt"
        const val PERFORMANCE_LOG_FILE = "performance_log.txt"
    }

    // API Endpoints
    object API {
        const val FACE_SYNC = "api/user/face/sync"
        const val FACE_ADD = "api/user/face/add"
        const val FACE_DELETE = "api/user/face/delete"
        const val GAME_RECORD_UPLOAD = "api/game/record/upload"
        const val GAME_RECORDS_SYNC = "api/game/records/sync"
        const val VIDEO_UPLOAD = "api/video/upload"
        const val LEADERBOARD_CONFIG = "api/config/leaderboard"
        const val DEVICE_CONFIG = "api/config/device"
        const val DEVICE_STATUS = "api/device/status"
    }

    // URL Templates
    object URLs {
        const val LEADERBOARD_SCENE_TEMPLATE = "https://school.xixiti.com/rank_basketball.htm?school_id=%s&mode=scene"
        const val LEADERBOARD_GAME_TEMPLATE = "https://school.xixiti.com/rank_basketball.htm?school_id=%s&user_id=%s&mode=game"
    }

    // Animation Durations
    object Animation {
        const val SHORT = 150L
        const val MEDIUM = 300L
        const val LONG = 500L
        const val EXTRA_LONG = 800L
    }

    // Format Patterns
    object Format {
        const val DATE_TIME = "yyyy-MM-dd HH:mm:ss"
        const val TIME = "HH:mm:ss"
        const val DATE = "yyyy-MM-dd"
        const val TIMESTAMP = "yyyyMMddHHmmss"
    }
}
