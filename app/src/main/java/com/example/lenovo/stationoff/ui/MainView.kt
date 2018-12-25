package com.example.lenovo.stationoff.ui

interface MainView{

    fun contentText(text:String?)

    fun showMsg(msg:String)

    fun changView()

    fun shakeAndNotice()

    fun stationOff()

    fun locationOk(str:String)
}
