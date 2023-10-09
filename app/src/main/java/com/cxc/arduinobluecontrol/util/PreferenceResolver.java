package com.cxc.arduinobluecontrol.util;

import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/* loaded from: classes.dex */
public class PreferenceResolver {
    protected Context mContext;
    private final String TAG = "PreferenceResolver";
    private final boolean DEBUG = true;
    private final String VOICE_TEXT_KEY_1 = "VCtext1";
    private final String VOICE_TEXT_KEY_2 = "VCtext2";
    private final String VOICE_TEXT_KEY_3 = "VCtext3";
    private final String VOICE_TEXT_KEY_4 = "VCtext4";
    private final String VOICE_TEXT_KEY_5 = "VCtext5";
    private final String VOICE_TEXT_KEY_6 = "VCtext6";
    private final String VOICE_TEXT_KEY_7 = "VCtext7";
    private final String VOICE_TEXT_KEY_8 = "VCtext8";
    private final String VOICE_TEXT_KEY_9 = "VCtext9";
    private final String VOICE_TEXT_KEY_10 = "VCtext10";
    private final String VOICE_DATA_KEY_1 = "VCdata1";
    private final String VOICE_DATA_KEY_2 = "VCdata2";
    private final String VOICE_DATA_KEY_3 = "VCdata3";
    private final String VOICE_DATA_KEY_4 = "VCdata4";
    private final String VOICE_DATA_KEY_5 = "VCdata5";
    private final String VOICE_DATA_KEY_6 = "VCdata6";
    private final String VOICE_DATA_KEY_7 = "VCdata7";
    private final String VOICE_DATA_KEY_8 = "VCdata8";
    private final String VOICE_DATA_KEY_9 = "VCdata9";
    private final String VOICE_DATA_KEY_10 = "VCdata10";
    private final String GLOBAL_DEFAULT_VALUE = "";
    private Map<String, String> mMap = new HashMap();

    public PreferenceResolver() {
    }

    public PreferenceResolver(Context context) {
        this.mContext = context;
    }

    public String resolveVocalCommandPreference(String str) {
        buildVoicePreferenceMap();
        for (String str2 : this.mMap.keySet()) {
            Log.d("PreferenceResolver", "Voice Text = " + str2 + "for voice data=" + this.mMap.get(str2));
            if (str2.toLowerCase().equals(str.toLowerCase())) {
                String str3 = this.mMap.get(str2);
                Log.d("PreferenceResolver", "Resolved voice command = " + str3);
                return str3;
            }
        }
        return null;
    }

    private void buildVoicePreferenceMap() {
        this.mMap.clear();
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext1", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata1", ""));
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext2", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata2", ""));
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext3", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata3", ""));
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext4", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata4", ""));
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext5", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata5", ""));
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext6", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata6", ""));
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext7", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata7", ""));
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext8", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata8", ""));
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext9", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata9", ""));
        this.mMap.put(SharedPrefsUtil.getStringOrDefault(this.mContext, "VCtext10", ""), SharedPrefsUtil.getStringOrDefault(this.mContext, "VCdata10", ""));
    }
}
