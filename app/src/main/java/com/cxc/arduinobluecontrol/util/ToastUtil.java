package com.cxc.arduinobluecontrol.util;

import android.content.Context;
import android.widget.Toast;

/* loaded from: classes.dex */
public class ToastUtil {
    public static void showToast(Context context, String str) {
        Toast.makeText(context, str, 1).show();
    }

    public static void showToastAtCenter(Context context, String str) {
        Toast makeText = Toast.makeText(context, str, 1);
        makeText.setGravity(17, 0, 0);
        makeText.show();
    }
}
