package com.example.lenovo.stationoff.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.amap.api.location.AMapLocationClient
import com.example.lenovo.stationoff.R
import com.example.lenovo.stationoff.ui.MainActivity


class LocationNotification{

    companion object {
        private const val NOTIFICATION_CHANNEL_NAME = "BackgroundLocation"
        private var notificationManager: NotificationManager? = null
        var isCreateChannel = false

        fun cancelNotification(){
            if(notificationManager!=null)
                notificationManager!!.cancel(MainActivity.LOCATION_NOTI_ID)
            notificationManager!!.cancelAll()
        }

        fun buildNotification(context:Context): Notification? {
            val builder: Notification.Builder?
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                //Android O上对Notification进行了修改，如果设置的targetSDKVersion>=26建议使用此种方式创建通知栏
                if (null == notificationManager) {
                    notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
                }
                val channelId = context.packageName
                if (!isCreateChannel) {
                    val notificationChannel = NotificationChannel(channelId,
                            NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
                    notificationChannel.setShowBadge(true) //是否在久按桌面图标时显示此渠道的通知
                    notificationManager!!.createNotificationChannel(notificationChannel)
                    isCreateChannel = true
                }
                builder = Notification.Builder(context, channelId)
            } else {
                builder = Notification.Builder(context)
            }
            builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("到站提醒")
                    .setContentText("正在后台运行")
                    .setWhen(System.currentTimeMillis())
            return builder.build()
        }

    }

}

