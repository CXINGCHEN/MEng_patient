package com.cxc.arduinobluecontrol.util;

import android.util.Log;

/* loaded from: classes.dex */
public class StringBuilderUtil {
    private static final String NEW_LINE_CHAR = "\n";
    private static final String NEW_LINE_CHAR_HTML = "<br>";
    private static final String TAG = "StringBuilderUtil";

    public static void deleteFirstLine(StringBuilder sb) {
        int indexOf = sb.indexOf(NEW_LINE_CHAR_HTML, 1);
        Log.d(TAG, "first line end index" + indexOf);
        Log.d(TAG, "sb length" + sb.length());
        if (indexOf > -1) {
            sb.delete(0, indexOf + 4);
            Log.d(TAG, "sb length after deleting first line" + sb.length());
            return;
        }
        Log.e(TAG, "New line mark not found");
    }

    public static void appendLine(StringBuilder sb, String str) {
        if (sb == null || str == null) {
            return;
        }
        sb.append(str);
        sb.append(NEW_LINE_CHAR_HTML);
    }

    public static void appendLine(StringBuilder sb, String str, String str2) {
        if (sb == null || str2 == null) {
            return;
        }
        if (str == null) {
            appendLine(sb, str2);
            return;
        }
        sb.append(str);
        sb.append(str2);
        sb.append(NEW_LINE_CHAR_HTML);
    }
}
