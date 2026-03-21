package com.smartbasketball.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolStorage @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREFS_NAME = "smart_basketball_prefs"
        private const val KEY_SCHOOL = "school"
        private const val KEY_TIMESTAMP = "synch_timestamp"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveSchool(schoolJson: JSONObject?) {
        if (schoolJson != null) {
            prefs.edit().putString(KEY_SCHOOL, schoolJson.toString()).apply()
        } else {
            prefs.edit().remove(KEY_SCHOOL).apply()
        }
    }
    
    fun getSchool(): JSONObject? {
        val json = prefs.getString(KEY_SCHOOL, null)
        return if (json != null) JSONObject(json) else null
    }
    
    fun isLoggedIn(): Boolean {
        val school = getSchool()
        return school != null && school.has("title")
    }
    
    fun getSchoolId(): String? {
        return getSchool()?.optString("_id")
    }
    
    fun getSchoolTitle(): String? {
        return getSchool()?.optString("title")
    }
    
    fun saveTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_TIMESTAMP, timestamp).apply()
    }
    
    fun getTimestamp(): Long {
        return prefs.getLong(KEY_TIMESTAMP, 0)
    }
    
    fun clearSchool() {
        prefs.edit().remove(KEY_SCHOOL).apply()
    }
}
