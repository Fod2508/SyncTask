package com.phuc.synctask.util

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    
    // HƯỚNG DẪN: Thay YOUR_PROJECT_ID bằng Project ID của bạn
    // Bạn có thể lấy Project ID trong Firebase Console -> Project Settings.
    private const val FCM_API_URL = "https://fcm.googleapis.com/v1/projects/YOUR_PROJECT_ID/messages:send"
    
    // HƯỚNG DẪN TEST LẤY OAUTH2 TOKEN TẠM THỜI:
    // 1. Vào Google OAuth 2.0 Playground (https://developers.google.com/oauthplayground)
    // 2. Kéo xuống chọn "Firebase Cloud Messaging API v1" -> Chọn scope "https://www.googleapis.com/auth/cloud-platform" hoặc "https://www.googleapis.com/auth/firebase.messaging"
    // 3. Đăng nhập bằng tài khoản Google (tài khoản có quyền Owner đối với project Firebase hiện tại).
    // 4. Bấm "Exchange authorization code for tokens".
    // 5. Copy chuỗi "Access token" và dán vào biến dưới đây. Token này sống được 1 giờ.
    // LƯU Ý: Trong production, Server (Backend) là nơi tạo OAuth2 token từ Service Account JSON, Client không nên chứa logic này.
    private const val OAUTH2_TOKEN = "ya29.YOUR_TEMPORARY_OAUTH2_TOKEN_HERE"

    private val client = OkHttpClient()

    /**
     * Gửi Push Notification thông qua FCM HTTP v1 API.
     */
    fun sendPushNotification(targetToken: String, title: String, body: String) {
        if (targetToken.isEmpty()) return

        val jsonPayload = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", targetToken)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                })
            })
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(FCM_API_URL)
            // Header Authorization với Bearer Token
            .addHeader("Authorization", "Bearer $OAUTH2_TOKEN")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Gửi thông báo thất bại", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d(TAG, "Gửi thông báo thành công! targetToken=$targetToken")
                } else {
                    Log.e(TAG, "Gửi thông báo có lỗi: mã ${response.code}, message: ${response.message}\nBody: ${response.body?.string()}")
                }
            }
        })
    }
}
