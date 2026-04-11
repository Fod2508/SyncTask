package com.phuc.synctask.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.phuc.synctask.R
import java.util.concurrent.atomic.AtomicInteger

class SyncTaskMessagingService : FirebaseMessagingService() {

    private val notificationIdGenerator = AtomicInteger(0)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Lưu token lên Realtime Database cho user hiện tại (nếu đã đăng nhập)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val dbRef = FirebaseDatabase.getInstance().reference
            dbRef.child("users").child(currentUser.uid).child("fcmToken").setValue(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notification = message.notification
        if (notification != null) {
            val title = notification.title ?: "Thông báo mới"
            val body = notification.body ?: "Bạn có cập nhật mới từ nhóm."
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "group_task_channel"
        val channelName = "Công việc Nhóm"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Yêu cầu cho Android 8.0 trở lên (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo về các nhiệm vụ nhóm."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo thông báo
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            // Lấy icon ứng dụng mặc định, nếu không có thì dùng launcher icon
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        // Hiển thị thông báo với ID độc lập
        notificationManager.notify(notificationIdGenerator.incrementAndGet(), notificationBuilder.build())
    }
}
