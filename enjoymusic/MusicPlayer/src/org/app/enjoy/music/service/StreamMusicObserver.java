package org.app.enjoy.music.service;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;

import org.app.enjoy.music.tool.LogTool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by Administrator on 2017/1/2.
 */

public class StreamMusicObserver extends ContentObserver {
    int previousVolume;
    int previousSystem;
    Context context;

    public StreamMusicObserver(Context c, Handler handler) {
        super(handler);
        context = c;

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        previousSystem = audio.getStreamVolume(AudioManager.STREAM_SYSTEM);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int currVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        int currSystem = audio.getStreamVolume(AudioManager.STREAM_SYSTEM);
        LogTool.d("currVolume:" + currVolume);
        LogTool.d("currVolume currSystem:" + currSystem);
        if (previousVolume != currVolume) {
            if (currVolume > 0) {
                execCommand(String.format(strCommand, currVolume + 126));
                previousVolume = currVolume;
            } else {
                execCommand(String.format(strCommand, 0));
            }
            audio.setStreamVolume(AudioManager.STREAM_SYSTEM, currVolume, 0); //tempVolume:音量绝对值
        }else if (previousSystem != currSystem) {
            if (currSystem > 0) {
                execCommand(String.format(strCommand, currSystem + 126));
                previousSystem = currSystem;
            } else {
                execCommand(String.format(strCommand, 0));
            }
            audio.setStreamVolume(AudioManager.STREAM_SYSTEM, currVolume, 0); //tempVolume:音量绝对值
        }
    }

    private String strCommand = "tinymix 0 %d";

    public void execCommand(String command) {
        LogTool.d("Volume command:" + command);
        Process proc = null;        //这句话就是shell与高级语言间的调用
        try {
            // start the ls command running
            //String[] args =  new String[]{"sh", "-c", command};
            Runtime runtime = Runtime.getRuntime();
            proc = runtime.exec(command);
            //如果有参数的话可以用另外一个被重载的exec方法
            //实际上这样执行时启动了一个子进程,它没有父进程的控制台
            //也就看不到输出,所以我们需要用输出流来得到shell执行后的输出
            InputStream inputstream = proc.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            // read the ls output
            String line = "";
            StringBuilder sb = new StringBuilder(line);
            while ((line = bufferedreader.readLine()) != null) {
                //System.out.println(line);
                sb.append(line);
                sb.append('\n');
            }
            LogTool.d("result:" + sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //tv.setText(sb.toString());
        //使用exec执行不会等执行成功以后才返回,它会立即返回
        //所以在某些情况下是很要命的(比如复制文件的时候)
        //使用wairFor()可以等待命令执行完成以后才返回
        try {
            if (proc.waitFor() != 0) {
                System.err.println("exit value = " + proc.exitValue());
            }
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }
}