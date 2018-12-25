package com.example.lenovo.stationoff.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import com.example.lenovo.stationoff.R

class MainPresenter(private val mView: MainView) {
    private lateinit var notification: Notification
    private lateinit var mContext: Context
    //震动
    private val vibrator: Vibrator by lazy {
        mContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    //通知
    private val manager: NotificationManager by lazy {
        mContext.getSystemService(AppCompatActivity.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * 初始化notification
     */
    fun initNotification(context: Context){
        this.mContext=context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "StationOff"
            val channelName = "到站啦"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            channel.setBypassDnd(true)//设置可绕过  请勿打扰模式
            manager.createNotificationChannel(channel)
        }
        val intent = Intent(context, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(context, 1, intent, 0)

        notification = NotificationCompat.Builder(context, "StationOff")
                .setContentTitle("到站啦")
                .setContentText("到站啦，准备下车吧！")
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
                .setAutoCancel(true)
                .setContentIntent(pIntent)
                .setFullScreenIntent(pIntent, true)
                .build()
    }

    fun showNotification(){
        manager.notify(MainActivity.AUDIO_NOTI_ID, notification)
    }

    /**
     * 开始震动
     */
    fun startShake(){
        // 判断手机硬件是否有振动器
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder().setFlags(AudioAttributes.USAGE_GAME).build()
                val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(1000, 1000), 0)
                vibrator.vibrate(vibrationEffect, audioAttributes)
            } else {
                vibrator.vibrate(longArrayOf(1000, 1000), 0)
            }
        }
    }

    /**
     * 停止震动和通知
     */
    fun showStop(){
        vibrator.cancel()
        manager.cancel(MainActivity.AUDIO_NOTI_ID)
    }
}