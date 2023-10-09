package com.cxc.arduinobluecontrol.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

/* loaded from: classes.dex */
public class Utils {
    private static final boolean DEBUG = true;
    private static final String PCBWAY_AD_LINK = "https://www.pcbway.com/?from=BroXCodes2023";
    private static final String TAG = "Utils";

    public static void showToastMessage(Context context, String str) {
        Toast.makeText(context, str, Toast.LENGTH_LONG).show();
    }

    public static void showSnackBar(View view, String str, String str2, View.OnClickListener onClickListener) {
        Snackbar.make(view, str, BaseTransientBottomBar.LENGTH_LONG).setAction(str2, onClickListener).show();
    }

    public static boolean isChromeInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo("com.android.chrome", 0);
            return true;
        } catch (PackageManager.NameNotFoundException unused) {
            return false;
        }
    }

    public static void openPCBWayAdLink(Context context) {
//        if (isChromeInstalled(context)) {
//            Log.d(TAG, "Chrome installed.");
//            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
//            builder.setStartAnimations(context, R.anim.slide_in_right, R.anim.slide_out_left);
//            builder.setToolbarColor(ContextCompat.getColor(context, R.color.pcbway_default_color));
//            builder.setShowTitle(true);
//            builder.build().launchUrl(context, Uri.parse(PCBWAY_AD_LINK));
//            return;
//        }
//        Log.d(TAG, "Chrome not installed.");
//        context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(PCBWAY_AD_LINK)));
    }
}
