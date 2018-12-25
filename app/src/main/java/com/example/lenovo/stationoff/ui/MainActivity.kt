package com.example.lenovo.stationoff.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.os.*
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.example.lenovo.stationoff.R
import com.example.lenovo.stationoff.service.LocationAudioService
import com.orhanobut.logger.Logger
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_main_layout.*


class MainActivity : AppCompatActivity(), MainView, LocationAudioService.ServiceListener {
    companion object {
        const val AUDIO_NOTI_ID = 10
        const val LOCATION_NOTI_ID = 11
        const val SERVICE_FOREGROUND_ID = 12
    }
    private val mPresenter: MainPresenter by lazy {
        MainPresenter(this)
    }
    //是否开启
    private var isBtnSelect = false
    //是否到站
    private var isStationOff = false

    private val rxPermissions: RxPermissions by lazy {
        RxPermissions(this)
    }
    //权限是否给完
    private var isPermissionOk = true

    private var mServiceBinder: LocationAudioService.ServiceBinder? = null

    private var serviceIntent:Intent?=null

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mServiceBinder = service as LocationAudioService.ServiceBinder
            mServiceBinder!!.setListener(this@MainActivity)
            // 设置讯飞参数
            mServiceBinder!!.setParam(this@MainActivity)
            //设置为前台
            mServiceBinder!!.startForeground(this@MainActivity)
            //初始化定位
            mServiceBinder!!.initLocation()
            //舒适化录音
            mServiceBinder!!.initAudio()
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServiceBinder = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_layout)

        getPermissions()

        initView()
        mPresenter.initNotification(this)

        //使用服务进行定位和录音
        serviceIntent = Intent(this@MainActivity, LocationAudioService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
            Logger.e("startForegroundService")
        }
        bindService(serviceIntent, mConnection, BIND_AUTO_CREATE)
    }

    /**
     * 获取权限 存储，录音，定位
     */
    @SuppressLint("CheckResult")
    private fun getPermissions() {
        isPermissionOk = true
        rxPermissions
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION)
                .subscribe { granted ->
                    if (granted) { // Always true pre-M
                        Logger.e("granted")
                    } else {
                        Logger.e("！granted")
                        isPermissionOk = false
                        show("请打开权限")
                    }
                }
    }

    private fun initView() {
        img_clear.setOnClickListener { v ->
            if (!isBtnSelect)
                edit_station.text = null
        }
        btn.setOnClickListener { v ->
            if (!isPermissionOk) {
                getPermissions()
                return@setOnClickListener
            }

            //这里是到站后，用户点击停止
            if (isStationOff) {
                isStationOff = false
                changView()
                mPresenter.showStop()
                mServiceBinder!!.cancelLocationNotifi()
                tv_location.visibility = View.GONE
                return@setOnClickListener
            }

            val textStr = edit_station.text.toString().trim()
            if (textStr.isEmpty()) {
                show("请输入站名")
                return@setOnClickListener
            }
            if (textStr.length < 2) {
                show("站名至少两个字")
                return@setOnClickListener
            }
            if (!isBtnSelect) {
                mServiceBinder!!.stationPy(edit_station.text.toString())
                mServiceBinder!!.startLocation()
                //设置为前台
                mServiceBinder!!.startForeground(this@MainActivity)
            } else {
                tv_content.text = "停止"
                mServiceBinder!!.stopAll()
            }
            tv_location.visibility = View.GONE
            changView()
        }
    }

    private fun show(str: String) {
        Toast.makeText(this@MainActivity, str, Toast.LENGTH_SHORT).show()
    }

    /**
     * 点击开启按钮动作
     * 改变ui和一些状态
     * open:是否开启（改变前）
     */
    override fun changView() {
        if (!isBtnSelect) {
            isBtnSelect = true
            btn.background = ContextCompat.getDrawable(this, R.drawable.circle_select)
            btn.text = getString(R.string.listen)
            edit_station.isEnabled = false
        } else {
            isBtnSelect = false
            btn.background = ContextCompat.getDrawable(this, R.drawable.circle_unselect)
            btn.text = getString(R.string.start)
            edit_station.isEnabled = true
        }
    }

    /**
     * 显示返回数据
     */
    override fun contentText(text: String?) {
        tv_content.text = text
    }

    /**
     * 显示范围提醒文字
     */
    override fun locationOk(str: String) {
        tv_location.visibility = View.VISIBLE
        tv_location.text = getString(R.string.location_str, str)
    }

    /**
     * 到站了
     *
     */
    override fun stationOff() {
        isStationOff = true
        btn.background = ContextCompat.getDrawable(this, R.drawable.circle_station_off_select)
        btn.text = "关闭"
    }

    override fun showMsg(msg: String) {
        show(msg)
    }

    /**
     * 开启震动
     */
    override fun shakeAndNotice() {
        mPresenter.showNotification()
        mPresenter.startShake()
    }

    //重写返回键
    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        mServiceBinder!!.stopAll()
        mServiceBinder!!.destroy()
        unbindService(mConnection)
    }
}

