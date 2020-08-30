package com.moyu.apptimes;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.lang.ref.WeakReference;


public class MyAudioRecord {
    static final int SAMPLE_RATE_IN_HZ = 8000;
    static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
            AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
    AudioRecord mAudioRecord;
    boolean isGetVoiceRun;
    Object mLock;
    private WeakReference<MainActivity> mActivity;

    public MyAudioRecord(MainActivity activity) {
        mLock = new Object();
        mActivity = new WeakReference<>(activity);
    }

    public void stop() {
        this.isGetVoiceRun = false;
    }

    public void getNoiseLevel() {
        if (isGetVoiceRun) {
            return;
        }
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_IN_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE);
        if (mAudioRecord == null) {
        }
        isGetVoiceRun = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                mAudioRecord.startRecording();
                short[] buffer = new short[BUFFER_SIZE];
                final MainActivity activity = mActivity.get();
                while (isGetVoiceRun) {
                    int r = mAudioRecord.read(buffer, 0, BUFFER_SIZE);
                    long v = 0;
                    for (int i = 0; i < buffer.length; i++) {
                        v += buffer[i] * buffer[i];
                    }
                    double mean = v / (double) r;
                    final double volume = (10 * Math.log10(mean))<0?0:(10 * Math.log10(mean));
                    if (null != activity) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String s = "分贝数值：" + volume;
                                activity.tv.setText(s.substring(0,(s.length()-s.indexOf("."))>2?s.indexOf(".")+3:s.length()) + " db");
                                activity.pb.setProgress((int)volume*100<=0?0:(int)volume*100);
                                activity.arr[activity.index] = volume;
                                if(activity.index == activity.arr.length-1) {
                                    activity.index = 0;
                                    activity.getData.setEnabled(true);
                                } else {
                                    activity.index++;
                                }
                            }
                        });
                    }
                    // 大概一秒20次
                    synchronized (mLock) {
                        try {
                            mLock.wait(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
//                if (null != activity) {
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
////                            activity.tv.setText(activity.tv.getText() + "");
////                            activity.plateView.setValue(1);
//                        }
//                    });
//                }
            }
        }).start();
    }
}