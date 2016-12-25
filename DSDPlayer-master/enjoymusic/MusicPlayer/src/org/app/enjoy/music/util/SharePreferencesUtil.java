package org.app.enjoy.music.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.app.enjoy.music.tool.Contsant;

/**
 * Created by Administrator on 2016/6/23.
 */
public class SharePreferencesUtil {
    private static String TAG = "SharePreferencesUtil";

    public static void putInt(Context context,String key,int value) {
        Log.e(TAG,"putInt()-" + key + "=" + value);
        SharedPreferences sp = context.getSharedPreferences(Contsant.MA_DATA, Context.MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putInt(key, value);
        ed.commit();
    }
    public static int getInt(Context context,String key) {
        SharedPreferences sp = context.getSharedPreferences(Contsant.MA_DATA, Context.MODE_WORLD_READABLE);
        int value = sp.getInt(key, 0);
        Log.e(TAG,"putInt()-" + key + "=" + value);
        return value;
    }

    public static void putLong(Context context,String key,long value) {
        Log.e(TAG,"putInt()-" + key + "=" + value);
        SharedPreferences sp = context.getSharedPreferences(Contsant.MA_DATA, Context.MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putLong(key, value);
        ed.commit();
    }
    public static Long getLong(Context context,String key) {
        SharedPreferences sp = context.getSharedPreferences(Contsant.MA_DATA, Context.MODE_WORLD_READABLE);
        Long value = sp.getLong(key, 0);
        Log.e(TAG,"putInt()-" + key + "=" + value);
        return value;
    }

    public static void putString(Context context,String key,String value) {
        Log.e(TAG,"putInt()-" + key + "=" + value);
        SharedPreferences sp = context.getSharedPreferences(Contsant.MA_DATA, Context.MODE_WORLD_WRITEABLE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(key, value);
        ed.commit();
    }
    public static String getString(Context context,String key) {
        SharedPreferences sp = context.getSharedPreferences(Contsant.MA_DATA, Context.MODE_WORLD_READABLE);
        String value = sp.getString(key, "");
        Log.e(TAG,"putInt()-" + key + "=" + value);
        return value;
    }
}
