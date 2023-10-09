package com.cxc.arduinobluecontrol.util;

import android.content.Context;
import android.os.Vibrator;

/* loaded from: classes.dex */
public class VibratorUtil {
    public static void vibrate(Context context, int i) {
        ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(i);
    }

    public static void vibrateOnClick(Context context) {
        vibrate(context, 100);
    }
}
