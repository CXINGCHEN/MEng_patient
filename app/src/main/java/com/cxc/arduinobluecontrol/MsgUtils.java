package com.cxc.arduinobluecontrol;

import android.content.Context;
import android.content.SharedPreferences;

import com.specknet.pdiotapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MsgUtils {

    public static int spo2Average = 95;


    /**
     * 获取本地所有的警告消息
     *
     * @param context
     * @return
     * @throws JSONException
     */
    public static List<AlertMsg> getMsgFromLocal(Context context) throws JSONException {

        List<AlertMsg> msgList = new ArrayList<>();

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String msg = sharedPreferences.getString("sp_key_msg", "");

        // 本地json String 要变成 List<AlertMsg>
        if (msg.length() > 0) {
            JSONArray jsonArray = new JSONArray(msg);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject msgObj = jsonArray.getJSONObject(i);
                long timestamp = msgObj.getLong("timestamp");
                String string = msgObj.getString("msg");
                double heartRateAverage = msgObj.getDouble("heartRateAverage");
                double spo2Average = msgObj.getDouble("spo2Average");
                // 用上面拿到的值创建一个AlertMsg对象
                AlertMsg alertMsg = new AlertMsg(timestamp, string, heartRateAverage, spo2Average);
                msgList.add(alertMsg);
            }
        }


        return msgList;

    }


    /**
     * 需要告警的时候就调用一下这个方法
     * 接受一个alertMsg 追加到本地
     *
     * @param alertMsg
     * @param context
     * @throws JSONException
     */
    public static void addOneAlertMsgToLocal(AlertMsg alertMsg, Context context) throws JSONException {

        List<AlertMsg> msgFromLocal = getMsgFromLocal(context);
        msgFromLocal.add(alertMsg);

        // 把List<AlertMsg>变成一个Json String 存到本地
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < msgFromLocal.size(); i++) {
            AlertMsg msg = msgFromLocal.get(i);
            jsonArray.put(i, msg.toJsonObject());
        }

        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("sp_key_msg", jsonArray.toString()).apply();

    }

}
