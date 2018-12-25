package com.example.lenovo.stationoff

import android.app.Application
import com.example.lenovo.stationoff.util.LogcatHelper
import com.iflytek.cloud.SpeechUtility
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger



class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //讯飞语音
        SpeechUtility.createUtility(this@MyApplication, "appid=5c1b8c29")
        Logger.addLogAdapter(AndroidLogAdapter())
        LogcatHelper.getInstance(this).start()
    }
}
