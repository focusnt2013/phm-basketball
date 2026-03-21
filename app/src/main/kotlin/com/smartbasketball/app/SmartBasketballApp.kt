package com.smartbasketball.app

import android.app.Application
import com.smartbasketball.app.util.AppLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartBasketballApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        AppLogger.d("========== 应用启动 ==========")
    }
}
