package com.example.lenovo.stationoff.recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 封装了录音的方法：创建录音对象、开始、暂停、停止、取消，使用静态枚举类Status来记录录音的状态。
 */
public class AudioRecorder {

    private static final String  TAG="AudioRecorder";
//    private int audioInput = MediaRecorder.AudioSource.MIC;//麦克风音频源
    private int audioInput = MediaRecorder.AudioSource.VOICE_COMMUNICATION;//麦克风音频源针对VoIP等语音通信进行了调整
    private int audioSampleRate = 16000;//频率
//    private int audioChannel = AudioFormat.CHANNEL_IN_STEREO ;//双声道
    private int audioChannel = AudioFormat.CHANNEL_IN_MONO ;//单声道
    private int audioEncode = AudioFormat.ENCODING_PCM_16BIT;//编码样式

    private int bufferSizeInBytes = 0;
    private AudioRecord audioRecord;
    private Status status = Status.STATUS_NO_READY;
    protected String pcmFileName;

    private int lastVolumn = 0;//录音的分贝
    private AudioEncoder encoder;//编码格式
    private RecorderListen recorderListen;

    public AudioRecorder(AudioEncoder encoder) {
        String fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        pcmFileName = AudioFileUtils.getPcmFileAbsolutePath(fileName);
        Log.i(TAG,"pcmFileName:"+pcmFileName);
        this.encoder = encoder;
        File file = new File(pcmFileName);
        if (file.exists()) {
            file.delete();
        }
        status = Status.STATUS_READY;
    }

    public interface RecorderListen{
        void recorderStop(String filePath);
    }

    public void setRecorderListen(RecorderListen listen){
        this.recorderListen=listen;
    }

    public void startRecord() {
        if (status == Status.STATUS_NO_READY) {
            return;
        }
        if (status == Status.STATUS_START) {
            return;
        }
        bufferSizeInBytes = AudioRecord.getMinBufferSize(audioSampleRate, audioChannel, audioEncode);
        audioRecord = new AudioRecord(audioInput, audioSampleRate, audioChannel, audioEncode, bufferSizeInBytes);
        audioRecord.startRecording();
        new Thread(new Runnable() {
            @Override
            public void run() {
                recordToFile();
            }
        }).start();
    }

    public void stop() {
        if (status != Status.STATUS_START && status != Status.STATUS_PAUSE) {
        } else {
            stopRecorder();
            makeDestFile();
            status = Status.STATUS_READY;
            recorderListen.recorderStop(getVoiceFilePath());
        }
    }

    //文件进行转码
    private void makeDestFile() {
        if (encoder == null)
            return;
        new Thread() {
            @Override
            public void run() {
                encoder.init(audioSampleRate, audioSampleRate * 16 * audioRecord.getChannelCount(), audioRecord.getChannelCount());
                encoder.encode(pcmFileName);
                Logger.e("录音pcm转码wav完成");
//                releaseRecorder();
            }
        }.run();
    }

    /**
     * 取消录音
     */
    public void release() {
        stopRecorder();
        releaseRecorder();
        status = Status.STATUS_READY;
        clearFiles();
    }

    //释放资源
    private void releaseRecorder() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    //停止录音
    private void stopRecorder() {
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Logger.e("停止录音完成");
    }

    /**
     * 清除文件
     */
    public void clearFiles() {
        try {
            File pcmfile = new File(pcmFileName);
            if (pcmfile.exists())
                pcmfile.delete();

            if (encoder != null && !TextUtils.isEmpty(encoder.getDestFile())) {
                File file = new File(encoder.getDestFile());
                if (file.exists())
                    file.delete();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    //将音频写入文件
    private void recordToFile() {
        byte[] audiodata = new byte[bufferSizeInBytes];
        FileOutputStream fos = null;
        int readsize = 0;
        try {
            fos = new FileOutputStream(pcmFileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        status = Status.STATUS_START;
        while (status == Status.STATUS_START && audioRecord != null) {
            readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos != null) {
                try {
                    //get the volumn  1--10
                    int sum = 0;
                    for (int i = 0; i < readsize; i++) {
                        sum += Math.abs(audiodata[i]);
                    }

                    if (readsize > 0) {
                        int raw = sum / readsize;
                        lastVolumn = raw > 10 ? raw - 10 : 0;
//                        Log.i(TAG, "writeDataTOFile: volumn -- " + raw + " / lastvolumn -- " + lastVolumn);
                    }
                    if (readsize > 0 && readsize <= audiodata.length)
                        fos.write(audiodata, 0, readsize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 获取当前的录音状态
     * @return
     */
    public Status getStatus() {
        return status;
    }
    /**
     * 获取当前的录音文件的位置
     * @return
     */
    public String getVoiceFilePath() {
        return encoder == null ? pcmFileName : encoder.getDestFile();
    }
    /**
     * 录音的状态
     */
    public enum Status {
        STATUS_NO_READY,
        STATUS_READY,
        STATUS_START,
        STATUS_PAUSE,
        STATUS_STOP
    }
    /**
     * 暂停录音
     */
    public void pauseRecord() {
        if (status != Status.STATUS_START) {
            throw new IllegalStateException("没有在录音");
        } else {
            stopRecorder();
            status = Status.STATUS_PAUSE;
        }
    }
}
