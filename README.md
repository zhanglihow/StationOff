## 关于一个到站提醒的Demo

#### 大概的实现过程是这样的：

1，保存用户输入目的地的站名和拼音，并点击开启监听按钮；

2，开启间隔定位，获取第一次信息，得到当前城市code，然后去获取这个站名的信息（经纬度）；

3，继续定位获取当前的经纬度，比较与目的地的直线距离；

4，如果小于预设值，则认为是进入该预设值的范围，就停止定位，开启录音；	

5，录音5秒，然后利用讯飞语音转文字，再转为拼音，与目的地的拼音进行匹配；

6，如果与目的地的拼音匹配率超过50%（比方：目的地是5个字，识别的语音有3个字是相同的，就认为是匹配上了），然后开启震动和弹出通知栏告知用户到站下车  (匹配的算法可能还有待优化，能够拼音模糊匹配就更好了)



#### 涉及到的技术点：

1，定位，计算距离，站点信息  用的都是[高德地图](https://lbs.amap.com/api/android-location-sdk/locationsummary)

2，语音识别，用的是[讯飞语音](https://www.xfyun.cn/)

3，录音用的是 Android 的 AudioRecorder

4，汉字转拼音用的是 Android  的 HanziToPinyin

 

#### Android 下录音的两个类 AudioRecorder 和 MediaRecord 

AudioRecord 是一个比较偏底层的API,它可以获取到一帧帧 PCM 数据；MediaRecord 录制的音频文件是经过压缩后的。  

（讯飞语音文件语音转文字支持两种格式 pcm 和 wav ,wav可直接在手机播放，所以这里录音用的是 AudioRecorder  文件格式是 pcm ，后转为wav）    
PCM是一种没有压缩且无损的编码方式，WAV是微软开发的一种无损的音频文件格式 ， 而WAV是通过PCM数据的基础上添加头部信息而生成的一种音频格式，音质与CD接近


#### AudioRecorder 的几个需要注意的参数设置

录音采样率设置为：1600Hz  (每秒录取声音的次数)

声音来源设置为：MediaRecorder.AudioSource.VOICE_COMMUNICATION(麦克风音频源针对语音通信进行了调整，会比设置为MIC好一些)

采样位数设置为：16bit (AudioFormat.ENCODING_PCM_16BIT  每秒录取声音的精度)

声道数设置为:  单声道(MONO) 

音频文件的音质比特率:（kbps)=【采样率】（kHz）×（bit采样位数）×【声道数量】  


这里的录音过程是这样：录5秒，停止，开始语音识别文件，识别完成后匹配，然后再启动录音  

停止录音需要注意一下，

使用 audioRecord.stop()这个方法即可

最终停止才用 audioRecord.release()



#### 录音大概就是这样  

  

#### 讯飞的语音识别目前有几个问题

1，语音识别率不是太高  

2，识别语音时，手机的音乐和视频都会被暂停，识别完之后会继续

3，语音识别是网络识别，有时候延迟比较大，不是手机网络的原因，应该是sdk免费的问题，会一直卡在等待返回那里，api中设置了超时也不管用，所以这里是主动判断超时3秒就取消掉，然后重新开始录音（这里可优化为继续识别当前录音文件，还待测试）  



#### 涉及到的通知栏

系统可以区分前台和后台应用，满足以下任意条件，应用将被视为处于前台 ：

- 它具有可见的 Activity，无论 Activity 处于启动还是暂停状态；

- 它具有前台服务；

- 另一个前台应用通过绑定到应用的其中一个服务或使用应用的其中一个内容提供者与应用相连；

  

如果应用默认被置于后台，那在8.0以上由于系统耗电管理的优化，对后台应用定位的频率进行限制，高德地图说是一小时几次。

所以为保证定位的有效性，需要使用到 service ，然后在 Activity中开启时使用 startForegroundService(Intent service) 启动，表示在前台启动新服务，5秒内在service中需要调用 startForeground(int id, Notification notification)，开启一个常驻通知栏，这样就将应用置为前台，需要取消前台时，调用 stopForeground(int id) ，再清除 Notification就可以了。



#### 震动

```
vibrator.hasVibrator()//可以判断手机是否有振动器
```

可设置震动频率，和是否循环等   

sdk 21以上需要设置震动类型，有来电，闹钟，游戏等  



#### 关于调试

将这个demo的Log输出，都以文本格式保存在手机上，方便手机没在连接电脑时，查看问题。





参考：

[Android音频开发之音频采集](https://www.jianshu.com/p/e4357f00a43e)

[Audiorecorder和  Mediarcoder](https://www.jianshu.com/p/1f78c4211ab7)

[Android手机直播（三）声音采集](https://www.jianshu.com/p/2cb75a71009f/)

[Android音频开发（1）：基础知识](http://blog.51cto.com/ticktick/1748506)

[通知栏微技巧](https://blog.csdn.net/guolin_blog/article/details/79854070)

[Android 后台位置限制](https://developer.android.com/about/versions/oreo/background-location-limits)

[Android 8.0变更](https://developer.android.com/about/versions/oreo/android-8.0-changes)

[Android 9.0变更](https://developer.android.com/about/versions/pie/android-9.0-changes-28)

