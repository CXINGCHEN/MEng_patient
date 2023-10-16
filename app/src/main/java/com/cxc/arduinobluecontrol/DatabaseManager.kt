package com.cxc.arduinobluecontrol

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.specknet.pdiotapp.bean.RecognitionResultBean
import com.specknet.pdiotapp.bean.UserInfoBean
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Timer
import java.util.TimerTask

object DatabaseManager {

    private const val TAG = "DatabaseManager"


    private var todayDate: String? = null

    private val database = Firebase.database
    private val auth = Firebase.auth
    private val sdf = SimpleDateFormat("yyyyMMdd")
    private val gson = Gson()


    private var alertMsgDbPath = ""
    private var alertMsgRef: DatabaseReference? = null
    private var alertMsgValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val value = snapshot.value
            if (value == null) {
                todayAlertMsgList = mutableListOf()
            } else {
                todayAlertMsgList = value as MutableList<AlertMsg>
            }

            Log.i(TAG, "alertMsgValueEventListener onDataChange: ${gson.toJson(todayAlertMsgList)}")
        }

        override fun onCancelled(error: DatabaseError) {

        }

    }

    private var todayAlertMsgList = mutableListOf<AlertMsg>()

    // ---------------心率血氧所有数据--------------------------


    private var spo2AndHeartRateDbPath = ""
    private var spo2AndHeartRateRef: DatabaseReference? = null
    private var spo2AndHeartRateValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val value = snapshot.value
            if (value == null) {
                todaySpo2AndHeartRateList = mutableListOf()
            } else {
                todaySpo2AndHeartRateList = value as MutableList<Spo2AndHeartRateBean>
            }

            Log.i(
                TAG,
                "spo2AndHeartRateValueEventListener onDataChange: ${gson.toJson(todayAlertMsgList)}"
            )
        }

        override fun onCancelled(error: DatabaseError) {

        }

    }

    private var todaySpo2AndHeartRateList = mutableListOf<Spo2AndHeartRateBean>()
    // -----------------------------------------

    // ---------------动作类型所有数据--------------------------


    private var actionRealtimeDbPath = ""
    private var actionRealtimeRef: DatabaseReference? = null
    private var actionRealtimeValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val value = snapshot.value
            if (value == null) {
                todayActionRealtimeList = mutableListOf()
            } else {
                todayActionRealtimeList = value as MutableList<RecognitionResultBean>
            }

            Log.i(
                TAG,
                "actionRealtimeValueEventListener onDataChange: ${gson.toJson(todayActionRealtimeList)}"
            )
        }

        override fun onCancelled(error: DatabaseError) {

        }

    }

    private var todayActionRealtimeList = mutableListOf<RecognitionResultBean>()
    // -----------------------------------------

    private var actionDbPath = ""
    private var actionRef: DatabaseReference? = null
    private var actionValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val value = snapshot.value
            if (value == null) {
                todayActionList = mutableListOf()
            } else {
                todayActionList = value as MutableList<RecognitionResultBean>
            }
            Log.i(TAG, "actionValueEventListener onDataChange: ${gson.toJson(todayActionList)}")
        }

        override fun onCancelled(error: DatabaseError) {

        }

    }

    private var todayActionList = mutableListOf<RecognitionResultBean>()


    fun initDatabase() {
        val uid = auth.uid
        if (uid.isNullOrEmpty()) {
            return
        }

        // 创建一个 Timer
        val timer = Timer()

        // 第一个参数是任务，第二个参数是延迟执行的毫秒数，第三个参数是重复执行的毫秒间隔
        timer.schedule(object : TimerTask() {
            override fun run() {
                val current = sdf.format(Date())
                if (todayDate != sdf.format(Date())) {
                    Log.i(TAG, "run: 日期发生了变化，原来的日期：${todayDate}，新日期：${current}")
                    todayDate = current
                    initRefWithDate(todayDate!!)
                }
            }

        }, 0, 1000)


    }

    fun addUserinfoToDb(userInfoBean: UserInfoBean) {

        val uid = auth.uid
        val userInfoPath = "${uid}/userInfo"
        val reference = database.getReference(userInfoPath)

        reference.setValue(userInfoBean).addOnSuccessListener {
            Log.i(TAG, "addUserinfoToDb: success")
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }

    private fun initRefWithDate(date: String) {

        val uid = auth.uid

        alertMsgDbPath = "${uid}/date${date}/alertmsg/"
        alertMsgRef = database.getReference(alertMsgDbPath)
        alertMsgRef?.addValueEventListener(alertMsgValueEventListener)


        spo2AndHeartRateDbPath = "${uid}/date${date}/spo2AndHeartRate/"
        spo2AndHeartRateRef = database.getReference(spo2AndHeartRateDbPath)
        spo2AndHeartRateRef?.addValueEventListener(spo2AndHeartRateValueEventListener)


        actionDbPath = "${uid}/date${date}/action/"
        actionRef = database.getReference(actionDbPath)
        actionRef?.addValueEventListener(actionValueEventListener)


        actionRealtimeDbPath = "${uid}/date${date}/actionrealtime/"
        actionRealtimeRef = database.getReference(actionRealtimeDbPath)
        actionRealtimeRef?.addValueEventListener(actionRealtimeValueEventListener)

    }


    fun addActionRealtime(list: List<RecognitionResultBean>) {
        val uid = auth.uid
        if (uid.isNullOrEmpty()) {
            // 没有登录
            return
        }

        if (actionRealtimeRef == null) {
            return
        }

        todayActionRealtimeList.addAll(list)

        // 更新远端
        actionRealtimeRef!!.setValue(todayActionRealtimeList).addOnSuccessListener {
            Log.i(TAG, "actionRealtimeRef setValue: Success")
        }.addOnFailureListener {
            Log.i(TAG, "actionRealtimeRef setValue: Failure")
            it.printStackTrace()
        }
    }


    fun addSpo2AndHeartRate(spo2AndHeartRateList: List<Spo2AndHeartRateBean>) {
        val uid = auth.uid
        if (uid.isNullOrEmpty()) {
            // 没有登录
            return
        }

        if (spo2AndHeartRateRef == null) {
            return
        }

        todaySpo2AndHeartRateList.addAll(spo2AndHeartRateList)

        // 更新远端
        spo2AndHeartRateRef!!.setValue(todaySpo2AndHeartRateList).addOnSuccessListener {
            Log.i(TAG, "spo2AndHeartRateRef setValue: Success")
        }.addOnFailureListener {
            Log.i(TAG, "spo2AndHeartRateRef setValue: Failure")
            it.printStackTrace()
        }
    }

    fun addOneAlertMsg(newAlertMsg: AlertMsg) {

        // /uid/dateyyyyMMdd/alertmsg/

        val uid = auth.uid
        if (uid.isNullOrEmpty()) {
            // 没有登录
            return
        }

        if (alertMsgRef == null) {
            return
        }

        todayAlertMsgList.add(newAlertMsg)


        // 更新远端
        alertMsgRef!!.setValue(todayAlertMsgList).addOnSuccessListener {
            Log.i(TAG, "alertMsgRef setValue: Success")
        }.addOnFailureListener {
            Log.i(TAG, "alertMsgRef setValue: Failure")
            it.printStackTrace()
        }

    }


    fun addUserRecognitionListData(newList: List<RecognitionResultBean>) {

        // /uid/dateyyyyMMdd/action/
        val uid = auth.uid
        if (uid.isNullOrEmpty()) {
            // 没有登录
            return
        }

        if (newList.isEmpty()) {
            return
        }

        if (actionRef == null) {
            return
        }

        todayActionList.addAll(newList)
        // 更新远端
        actionRef!!.setValue(todayActionList).addOnSuccessListener {
            Log.i(TAG, "actionRef setValue: Success")
        }.addOnFailureListener {
            Log.i(TAG, "actionRef setValue: Failure")
            it.printStackTrace()
        }

    }


}