package com.example.lenovo.stationoff.recorder;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WavEncoder extends AudioEncoder {

    @Override
    public void init(int SAMPLE_RATE, int BIT_RATE, int CHANNEL_COUNT) {
        super.init(SAMPLE_RATE, BIT_RATE, CHANNEL_COUNT);
        destinationFile = AudioFileUtils.getWavFileAbsolutePath(System.currentTimeMillis() + "");
    }

    @Override
    public void encode(String sourceFile) {
        byte buffer[] = null;
        int TOTAL_SIZE = 0;
        File file = new File(sourceFile);
        if (!file.exists()) {
            return;
        }
        TOTAL_SIZE = (int) file.length();
        WaveHeader header = new WaveHeader();
        header.fileLength = TOTAL_SIZE + (44 - 8);
        header.FmtHdrLeth = 16;
        header.BitsPerSample = 16;
        header.Channels = (short) CHANNEL_COUNT;
        header.FormatTag = 0x0001;
        header.SamplesPerSec = SAMPLE_RATE;
        header.BlockAlign = (short) (header.Channels * header.BitsPerSample / 8);
        header.AvgBytesPerSec = header.BlockAlign * header.SamplesPerSec;
        header.DataHdrLeth = TOTAL_SIZE;

        byte[] h = null;
        try {
            h = header.getHeader();
        } catch (IOException e1) {
            Log.e("WavEncoder", e1.getMessage());
            return;
        }

        if (h.length != 44)
            return;

        File destfile = new File(destinationFile);
        if (destfile.exists())
            destfile.delete();

        try {
            buffer = new byte[1024 * 4];
            InputStream inStream = null;
            OutputStream ouStream = null;

            ouStream = new BufferedOutputStream(new FileOutputStream(destinationFile));
            ouStream.write(h, 0, h.length);
            inStream = new BufferedInputStream(new FileInputStream(file));
            int size = inStream.read(buffer);
            while (size != -1) {
                ouStream.write(buffer);
                size = inStream.read(buffer);
            }
            inStream.close();
            ouStream.close();
        } catch (IOException ioe) {
            Log.e("WavEncoder", ioe.getMessage());
            return;
        }
        Log.i("WavEncoder", "makePCMFileToWAVFile  success!" + new SimpleDateFormat("yyyy-MM-dd hh:mm").format(new Date()));
        return;
    }

}


