package com.example.lenovo.stationoff.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
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
import com.example.lenovo.stationoff.ui.MainActivity
import com.example.lenovo.stationoff.util.FileUtils
import com.example.lenovo.stationoff.util.HanziToPinyin
import com.example.lenovo.stationoff.util.JsonParser
import com.example.lenovo.stationoff.util.LocationNotification
import com.iflytek.cloud.*
import com.orhanobut.logger.Logger
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.io.File
import java.util.concurrent.TimeUnit

class LocationAudioService : Service() {
    private val mBinder: ServiceBinder by lazy {
        ServiceBinder()
    }
    //存放录音的文件路径
    private val RECORD_PATH_FILE: String = Environment.getExternalStorageDirectory().absolutePath + "/StationOffRecord/"
    // 引擎类型
    private val mEngineType = SpeechConstant.TYPE_CLOUD
    // 语音听写对象
    private lateinit var mIat: SpeechRecognizer
    //录音的对象
    private var disposes = ArrayList<Disposable>()
    //目的地名字拼音
    private var stationNames = ArrayList<String>()
    //目的地中文
    private lateinit var stationName: String
    //录音
    private lateinit var audioRecorder: AudioRecorder

    private var stopTime1: Long = 0L
    private var stopTime2: Long = 0L
    //本地监听识别超时的对象
    private var timeOutDisposable: Disposable? = null

    private lateinit var mContext: Context

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
        MyAMapLocation(mContext)
    }

    private lateinit var serviceListener: ServiceListener

    interface ServiceListener {
        fun showMsg(msg: String)
        fun locationOk(msg: String)
        fun contentText(msg: String?)
        fun stationOff()
        fun shakeAndNotice()
    }

    fun myStartForeground() {
        startForeground(MainActivity.LOCATION_NOTI_ID, LocationNotification.buildNotification(mContext))
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    /**
     * 开始录音监听
     *
     * 录音5秒后停止
     */
    fun startAudio(m: Long) {
        serviceListener.contentText("正在录音监听...")
        Logger.e("开始录音")
        audioRecorder.startRecord()
        val disposable = Observable.timer(m, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    stopTime1 = System.currentTimeMillis()
                    audioRecorder.stop()
                    serviceListener.contentText("解析中...")
                }
        disposes.add(disposable)
    }

    private var ret = 0 // 函数调用返回值

    /**
     * 执行音频流识别操作
     *
     * 识别超时3秒，取消识别，重新录音
     */
    @SuppressLint("CheckResult")
    private fun executeStream(file: File) {
        timeOutDisposable = Observable.timer(3, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Logger.e("超时 主动结束")
                    if (mIat.isListening) {
                        mIat.cancel()
                        startAudio(5L)
                    }
                }
        disposes.add(timeOutDisposable!!)
        // 设置音频来源为外部文件
        mIat.setParameter(SpeechConstant.AUDIO_SOURCE, "-1")

        //TODO 这里会导致音乐播放器暂停
        ret = mIat.startListening(mRecognizerListener)
        if (ret != ErrorCode.SUCCESS) {
            serviceListener.showMsg("识别失败,错误码：$ret")
        } else {
            val audioData = FileUtils.readFile4Bytes(file)
            if (null != audioData) {
                // 一次（也可以分多次）写入音频文件数据，数据格式必须是采样率为8KHz或16KHz（本地识别只支持16K采样率，云端都支持），
                // 位长16bit，单声道的wav或者pcm
                // 写入8KHz采样的音频时，必须先调用setParameter(SpeechConstant.SAMPLE_RATE, "8000")设置正确的采样率
                // 注：当音频过长，静音部分时长超过VAD_EOS将导致静音后面部分不能识别。
                // 音频切分方法：FucUtil.splitBuffer(byte[] buffer,int length,int spsize);
                mIat.writeAudio(audioData, 0, audioData.size)
            } else {
                mIat.cancel()
                serviceListener.showMsg("读取音频流失败")
            }
        }
    }

    /**
     * 语音识别监听器。
     *
     */
    private val mRecognizerListener = object : RecognizerListener {
        override fun onBeginOfSpeech() {
        }

        override fun onError(error: SpeechError) {
            Logger.e("error：" + error.getPlainDescription(true))
            stopTime2 = System.currentTimeMillis()
            serviceListener.contentText("error：" + error.getPlainDescription(true) + " 解析时间：" + (stopTime2 - stopTime1))
            startAudio(5L)
        }

        override fun onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Logger.e("结束说话 等待结果...")
            timeOutDisposable!!.dispose()
            serviceListener.contentText(null)
        }

        override fun onResult(results: RecognizerResult, isLast: Boolean) {
            Logger.e(results.resultString)
            stopTime2 = System.currentTimeMillis()

            //{"sn":1,"ls":false,"bg":0,"ed":0,"ws":[{"bg":1,"cw":[{"sc":0.0,"w":"原来"}]}]}
            val str = JsonParser.parseIatResult(results.resultString) ?: return

            serviceListener.contentText(str + "\n" + " 解析时间：" + (stopTime2 - stopTime1))
            var audioPy = getHanziPy(str)
            //比较结果
            val b = contrast(audioPy, stationNames)
            if (b) {
                serviceListener.contentText("马上到站了，赶快下车！")
                serviceListener.stationOff()
                mBinder.stopAll()
                //震动和通知
                serviceListener.shakeAndNotice()
            } else {
                startAudio(5L)
            }
        }

        override fun onVolumeChanged(volume: Int, data: ByteArray) {
        }

        override fun onEvent(p0: Int, p1: Int, p2: Int, obj: Bundle?) {
        }
    }

    /**
     * 转换文字拼音
     */
    fun getHanziPy(str: String): ArrayList<String> {
        val pys = arrayListOf<String>()
        val list = HanziToPinyin.getInstance().get(str)
        for (i in list.indices) {
            val token = list[i]
            pys.add(token.target)
        }
        return pys
    }

    /**
     * 比较拼音结果
     * 需要优化 识别到的文字与输入的站名匹配字数 》一半
     *          比如：输入站名5位 识别的文字大于等于3 位
     */
    fun contrast(audioPy: ArrayList<String>, stationNames: ArrayList<String>): Boolean {
        Logger.e("audioPy：$audioPy  stationNames:$stationNames")
        //识别的拼音
        val audioPySize = audioPy.size
        //目的地的拼音
        val stationPySize = stationNames.size
        //目的地的一半 四舍五入
        val halfStation = Math.round(stationPySize / 2.0)
        Logger.e("audioPySize：$audioPySize  stationPySize:$stationPySize  halfStation:$halfStation")

        if (audioPySize > stationPySize) {
            audioPy.removeAll(stationNames)
            val newSize = audioPy.size
            Logger.e("(pySize-newSize):" + (audioPySize - newSize))
            if ((audioPySize - newSize) >= halfStation) {
                return true
            }
        } else {
            val list = ArrayList<String>(stationNames)
            list.removeAll(audioPy)
            val newSize = list.size
            Logger.e("(stationSize-newSize):" + (stationPySize - newSize))
            if ((stationPySize - newSize) >= halfStation) {
                return true
            }
        }
        return false
    }

    /**
     * 返回讯飞初始化是否成功
     */
    private val mInitListener = InitListener { code ->
        if (code != ErrorCode.SUCCESS) {
            serviceListener.showMsg("初始化失败，错误码：$code")
        }
    }

    @SuppressLint("WrongConstant")
    fun stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(MainActivity.SERVICE_FOREGROUND_ID)
        }
    }

    /**
     * 返回定位 和录音的数据
     */
    inner class ServiceBinder : Binder(), BusStationSearch.OnBusStationSearchListener, DistanceSearch.OnDistanceSearchListener,
            AMapLocationListener, AudioRecorder.RecorderListen {

        fun startForeground() {
            myStartForeground()
        }

        fun setListener(listener: ServiceListener) {
            serviceListener = listener
        }

        /**
         * 讯飞识别的参数设置
         */
        fun setParam(context: Context) {
            mContext = context
            // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
            mIat = SpeechRecognizer.createRecognizer(context, mInitListener)
            // 清空参数
            mIat.setParameter(SpeechConstant.PARAMS, null)
            // 设置听写引擎
            mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType)
            // 设置返回结果格式
            mIat.setParameter(SpeechConstant.RESULT_TYPE, "json")
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn")
            mIat.setParameter(SpeechConstant.NET_TIMEOUT, "2000")
            //此处用于设置dialog中不显示错误码信息
            mIat.setParameter("view_tips_plain", "false")
        }

        /**
         * 初始化定位
         */
        fun initLocation() {
            myAMapLocation.setListener(this, this, this)
        }

        /**
         * 初始化录音
         */
        fun initAudio() {
            //设置最后转码的录音格式：wav
            audioRecorder = AudioRecorder(WavEncoder())
            audioRecorder.setRecorderListen(this)
        }

        /**
         * 开始监听
         *
         * todo 这里可优化为 service 定位和录音  通过 startForegroundService 把service置为前台 bindservice绑定
         *
         */
        fun startLocation() {
            serviceListener.contentText("正在定位监听...")
            isUserStart = true

            val intent = Intent(mContext, LocationAudioService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mContext.startForegroundService(intent)
            }
            myAMapLocation.startLocation()

//        LocationNotification.showNotification(context, myAMapLocation.mLocationClient)
        }

        //目的地的拼音
        fun stationPy(text: String) {
            stationName = text
            stationNames = getHanziPy(text)
        }

        /**
         * 返回公交站的信息
         */
        override fun onBusStationSearched(p0: BusStationResult?, p1: Int) {
            Logger.e(if (p1 == 1000) "获取公交信息成功" else "获取公交信息失败")
            if (p0 != null && p1 == 1000) {
                //可优化供用户选择
                if (p0.busStations.size > 0) {
                    stationLonLat = p0.busStations[0].latLonPoint
                }
            }
        }

        /**
         * 返回距离信息
         */
        @SuppressLint("CheckResult")
        override fun onDistanceSearched(p0: DistanceResult?, p1: Int) {
            if (p1 == 1000) "获取距离成功" else "获取距离失败"
            if (p1 == 1000 && p0 != null) {
                if (p0.distanceResults.size > 0) {
                    Logger.e("两地距离:" + p0.distanceResults[0].distance)
                    //如果小于20米，停止定位，开启录音，收听提示
                    if (p0.distanceResults[0].distance <= locationRange) {
                        myAMapLocation.stopLocation()

                        serviceListener.locationOk(locationRange.toString())
                        startAudio(5)
                    }
                }
            }
        }

        /**
         * 返回定位信息
         */
        @SuppressLint("CheckResult")
        override fun onLocationChanged(location: AMapLocation?) {
            Logger.e(location.toString())
            //点击第一次定位，保存城市code，并查询目的地的信息
            if (isUserStart) {
                isUserStart = false
                if (location != null) {
                    cityCode = location.cityCode
                }
                myAMapLocation.getStationNameLoc(stationName, cityCode)
                return
            }
            if (location != null) {
                //当前位置坐标
                nowLatLon = LatLonPoint(location.latitude, location.longitude)

                //计算两个位置的距离
                myAMapLocation.getDistanceSearch(stationLonLat, nowLatLon)
            }
        }

        override fun recorderStop(filePath: String?) {
            Logger.e("filePath:$filePath")
            executeStream(File(filePath))
        }

        fun cancelLocationNotifi() {
            LocationNotification.cancelNotification()
        }

        fun destroy() {
            myAMapLocation.mLocationClient.onDestroy()
        }

        /**
         * 点击停止
         */
        fun stopAll() {
            //取消前台
            stopForeground()
            //清除通知栏
            cancelLocationNotifi()
            //取消站点信息监听
            myAMapLocation.cancelStationNameLoc()
            //取消距离计算监听
            myAMapLocation.cancelDistanceSearch()
            //停止定位
            if (myAMapLocation.mLocationClient.isStarted) {
                myAMapLocation.stopLocation()
            }
            //取消语音识别
            if (mIat.isListening) {
                mIat.cancel()
            }
            //取消录音
            for (dis in disposes) {
                if (!dis.isDisposed)
                    dis.dispose()
            }
            disposes.clear()
            //释放录音
            audioRecorder.release()
            //刪除所有的录音文件
            FileUtils.deleteFile("$RECORD_PATH_FILE/wav/")
            FileUtils.deleteFile("$RECORD_PATH_FILE/pcm/")
        }
    }


}
