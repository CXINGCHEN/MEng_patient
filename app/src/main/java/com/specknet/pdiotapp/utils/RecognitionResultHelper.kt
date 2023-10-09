package com.specknet.pdiotapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.specknet.pdiotapp.App
import com.specknet.pdiotapp.bean.RecognitionResultBean

object RecognitionResultHelper {

    val gson = Gson()

    // 存
    fun saveUserRecognitionData(newList: ArrayList<RecognitionResultBean>) {
        // 需要区分用户
        val fileName = "RecognitionResult"

        val sharedPreferences: SharedPreferences =
            App.instance.getSharedPreferences(fileName, Context.MODE_PRIVATE)

        // 原来的数据
        val localRecognitionResult = sharedPreferences.getString("RecognitionResult", "")

        if (localRecognitionResult.isNullOrEmpty()) {
            sharedPreferences.edit().putString("RecognitionResult", gson.toJson(newList)).apply()
        } else {
            val oldList: ArrayList<RecognitionResultBean> =
                gson.fromJson<ArrayList<RecognitionResultBean>>(
                    localRecognitionResult,
                    object : TypeToken<ArrayList<RecognitionResultBean?>?>() {}.type
                )
            oldList.addAll(newList)
            sharedPreferences.edit().putString("RecognitionResult", gson.toJson(oldList)).apply()
        }

    }


    // 取
    fun getUserRecognitionData(): List<RecognitionResultBean> {
        // 需要区分用户
        val fileName = "RecognitionResult"

        val sharedPreferences: SharedPreferences =
            App.instance.getSharedPreferences(fileName, Context.MODE_PRIVATE)

        // 原来的数据
        val localRecognitionResult = sharedPreferences.getString("RecognitionResult", "")

        if (localRecognitionResult?.isNotEmpty() == true) {
            val oldList: ArrayList<RecognitionResultBean> =
                gson.fromJson<ArrayList<RecognitionResultBean>>(
                    localRecognitionResult,
                    object : TypeToken<ArrayList<RecognitionResultBean?>?>() {}.type
                )
            return oldList
        }

        return emptyList()
    }


}