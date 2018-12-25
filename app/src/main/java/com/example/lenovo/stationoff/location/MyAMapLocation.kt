package com.example.lenovo.stationoff.location

import android.content.Context
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.amap.api.services.busline.BusStationQuery
import com.amap.api.services.busline.BusStationSearch
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.route.DistanceSearch


/**
 * 定位，公交站信息，计算距离
 *
 * 1，定位一次，拿到城市code
 * 2，再获取输入的站名的信息
 * 3，开启定位
 * 4，计算距离，小于10米，终止定位，开启录音
 */
class MyAMapLocation(context: Context) {

    private var mContext:Context = context
    private var busSearchListener:BusStationSearch.OnBusStationSearchListener?=null
    private var distanceSearchListener:DistanceSearch.OnDistanceSearchListener?=null
    private var locationListener: AMapLocationListener?=null
    private var busStationSearch:BusStationSearch?=null

    //初始化定位
    val mLocationClient: AMapLocationClient by lazy {
        AMapLocationClient(mContext)
    }

    fun setListener(busSearchListener:BusStationSearch.OnBusStationSearchListener,distanceSearchListener: DistanceSearch.OnDistanceSearchListener,locationListener: AMapLocationListener){
        this.busSearchListener=busSearchListener
        this.distanceSearchListener=distanceSearchListener
        this.locationListener=locationListener
    }

    //AMapLocationClientOption对象
    private val mLocationOption: AMapLocationClientOption by lazy {
        AMapLocationClientOption()
    }
    //距离测量对象
    private val distanceSearch : DistanceSearch by lazy {
        DistanceSearch(mContext)
    }

    private val distanceQuery:DistanceSearch.DistanceQuery by lazy {
        DistanceSearch.DistanceQuery()
    }

    /**
     * 开启定位
     */
    fun startLocation(){
        //Hight_Accuracy，高精度模式。
        mLocationOption.locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
        //设置定位间隔,单位毫秒,默认为2000ms，最低1000ms。
        mLocationOption.interval = 5000
        mLocationOption.isOnceLocation=false
        //设置是否返回地址信息（默认返回地址信息）
        mLocationOption.isNeedAddress = true
        //单位是毫秒，默认30000毫秒，建议超时时间不要低于8000毫秒。
        mLocationOption.httpTimeOut = 8000
        //关闭缓存机制
        mLocationOption.isLocationCacheEnable = false
        mLocationClient.setLocationOption(mLocationOption)
        mLocationClient.setLocationListener(locationListener)
        mLocationClient.startLocation()
    }

    /**
     * 关闭定位
     */
    fun stopLocation(){
        if(mLocationClient.isStarted){
            mLocationClient.unRegisterLocationListener(locationListener)
            mLocationClient.stopLocation()
        }
    }

    /**
     * 获取站名的经纬度
     *
     */
    fun getStationNameLoc(name:String,cityCode:String){
        // 第一个参数表示公交站点名，第二个参数表示所在城市名或者城市区号
        val busStationQuery: BusStationQuery by lazy {
            BusStationQuery(name, cityCode)
        }
        busStationSearch = BusStationSearch(mContext, busStationQuery)
        busStationSearch!!.setOnBusStationSearchListener(busSearchListener)// 设置查询结果的监听

        busStationSearch!!.searchBusStationAsyn()
    }

    /**
     * 取消站名的监听
     */
    fun cancelStationNameLoc(){
        if(busStationSearch!=null){
            busStationSearch!!.setOnBusStationSearchListener(null)
        }
    }

    /**
     * 计算两个坐标的距离
     */
    fun getDistanceSearch(start:LatLonPoint ,dest:LatLonPoint){
        distanceSearch.setDistanceSearchListener(distanceSearchListener)
        //设置起点和终点，其中起点支持多个
        val latLonPoints = ArrayList<LatLonPoint>()
        latLonPoints.add(start)
        distanceQuery.origins = latLonPoints
        distanceQuery.destination = dest
        //设置测量方式，支持直线和驾车
        distanceQuery.type = DistanceSearch.TYPE_DISTANCE
        distanceSearch.calculateRouteDistanceAsyn(distanceQuery)
    }

    /**
     * 取消计算坐标
     */
    fun cancelDistanceSearch(){
        distanceSearch.setDistanceSearchListener(null)
    }

}
