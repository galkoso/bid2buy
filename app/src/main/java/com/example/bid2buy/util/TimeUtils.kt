package com.example.bid2buy.util

import android.os.SystemClock
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private var offset: Long = 0
    private var isSynced = false
    suspend fun syncTime() = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.connect()
            
            val dateStr = connection.getHeaderField("Date")
            if (dateStr != null) {
                val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
                val serverTime = sdf.parse(dateStr)?.time ?: 0L
                if (serverTime != 0L) {
                    offset = serverTime - SystemClock.elapsedRealtime()
                    isSynced = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            offset = System.currentTimeMillis() - SystemClock.elapsedRealtime()
        }
    }
    
    fun currentTimeMillis(): Long {
        if (!isSynced) {
            synchronized(this) {
                if (!isSynced) {
                    offset = System.currentTimeMillis() - SystemClock.elapsedRealtime()
                    isSynced = true
                }
            }
        }
        return SystemClock.elapsedRealtime() + offset
    }
    
    fun now(): Timestamp {
        return Timestamp(Date(currentTimeMillis()))
    }
}
