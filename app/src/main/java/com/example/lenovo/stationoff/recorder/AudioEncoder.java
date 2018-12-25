package com.example.lenovo.stationoff.recorder;

import android.annotation.TargetApi;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.NonNull;

import java.io.IOException;

public abstract class AudioEncoder {
    protected int SAMPLE_RATE = 8000;
    protected int BIT_RATE = 64;
    protected int CHANNEL_COUNT = 1;
    protected String destinationFile;

    public void init(int SAMPLE_RATE, int BIT_RATE, int CHANNEL_COUNT) {
        this.SAMPLE_RATE = SAMPLE_RATE;
        this.BIT_RATE = BIT_RATE;
        this.CHANNEL_COUNT = CHANNEL_COUNT;
    }

    public String getDestFile() {
        return destinationFile;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @NonNull
    protected MediaFormat getMediaFormat(String sourceFile) throws IOException {
        MediaExtractor mex = new MediaExtractor();
        mex.setDataSource(sourceFile);
        return mex.getTrackFormat(0);
    }
    public abstract void encode(String sourceFile);

}
