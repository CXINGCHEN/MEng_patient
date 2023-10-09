package com.cxc.arduinobluecontrol.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/* loaded from: classes.dex */
public class SharedPrefsUtil {
    public static final String ARROW_DOWN_KEY = "down";
    public static final String ARROW_LEFT_KEY = "left";
    public static final String ARROW_RIGHT_KEY = "right";
    public static final String ARROW_UP_KEY = "up";
    public static final String CTRL_BUTTON_A_KEY = "A";
    public static final String CTRL_BUTTON_B_KEY = "B";
    public static final String CTRL_BUTTON_C_KEY = "C";
    public static final String CTRL_BUTTON_D_KEY = "D";
    public static final String CTRL_BUTTON_E_KEY = "E";
    public static final String CTRL_BUTTON_F_KEY = "F";
    public static final String DEVICE_NAME_KEY = "devicename";
    public static final String MAC_ADDRESS_KEY = "macadress";

    public static void save(Context context, String str, String str2) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putString(str, str2);
        edit.apply();
    }

    public static void save(Context context, String str, long j) {
        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
        edit.putLong(str, j);
        edit.apply();
    }

    public static String getStringOrNull(Context context, String str) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(str, null);
    }

    public static String getStringOrDefault(Context context, String str, String str2) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(str, str2);
    }

    public static long getLongOrDefault(Context context, String str, long j) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(str, j);
    }

    public static boolean getBooleanOrDefault(Context context, String str, boolean z) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(str, z);
    }
}
