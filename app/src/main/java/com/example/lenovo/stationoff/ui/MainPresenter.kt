package com.example.lenovo.stationoff.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationListener
import com.amap.api.services.busline.BusStationResult
import com.amap.api.services.busline.BusStationSearch
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.DistanceResult
import com.amap.api.services.route.DistanceSearch
import com.example.lenovo.stationoff.location.MyAMapLocation
import com.example.lenovo.stationoff.recorder.AudioRecorder
import com.example.lenovo.stationoff.recorder.WavEncoder
import com.example.lenovo.stationoff.service.LocationAudioService
import com.example.lenovo.stationoff.util.FileUtils
import com.example.lenovo.stationoff.util.HanziToPinyin
import com.example.lenovo.stationoff.util.JsonParser
import com.iflytek.cloud.*
import com.orhanobut.logger.Logger
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.File
import java.util.concurrent.TimeUnit


class MainPresenter(private val mView: MainView) {
    //存放录音的文件路径
    private val RECORD_PATH_FILE: String = Environment.getExternalStorageDirectory().absolutePath + "/StationOffRecord/"
    // 引擎类型
    private val mEngineType = SpeechConstant.TYPE_CLOUD
    // 语音听写对象
    private lateinit var mIat: SpeechRecognizer

    private var disposes = ArrayList<Disposable>()
    //目的地名字拼音
    private var stationNames = ArrayList<String>()
    //目的地中文
    private lateinit var stationName: String
    //录音
    private lateinit var audioRecorder: AudioRecorder

    private var stopTime1: Long = 0L
    private var stopTime2: Long = 0L

    private var timeOutDisposable: Disposable? = null

    private lateinit var context: Context

    //城市code
    lateinit var cityCode: String
    //当前定位的经纬度
    lateinit var nowLatLon: LatLonPoint
    //目的地的经纬度
    lateinit var stationLonLat: LatLonPoint
    //是否手动点击开始
    var isUserStart: Boolean = false
    //距离范围默认20米
    private val locationRange: Int = 2000

    private val myAMapLocation: MyAMapLocation by lazy {
        MyAMapLocation(context)
    }
}