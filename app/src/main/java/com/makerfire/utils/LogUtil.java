package com.makerfire.utils;

import android.util.Log;

/**
 * Created by lindengfu on 17-3-7.
 */

public class LogUtil
{
    public final static String TAG = "bluetooth";

    public static void LOGI(String string)
    {
        Log.i(TAG, string);
    }

    public static void LOGE(String string)
    {
        Log.e(TAG, string);
    }
}
