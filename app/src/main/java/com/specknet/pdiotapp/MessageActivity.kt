package com.specknet.pdiotapp


import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cxc.arduinobluecontrol.AlertMsg
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import java.text.SimpleDateFormat


class MessageActivity : AppCompatActivity() {

    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    private val database = Firebase.database
    private val auth = Firebase.auth
    private val gson = Gson()


    private val valueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // Get Post object and use the values to update the UI
            val map = dataSnapshot.value as Map<String, Any>
            Log.i("MessageActivity", "onDataChange: ${gson.toJson(map)}")

            val resultList = mutableListOf<AlertMsg>()
            map.values.forEach {
                val map1 = it as Map<String, Any>
                val alertMsgs = map1["alertmsg"] as ArrayList<HashMap<String, Any>>

                alertMsgs.forEach {

                    // map -》 json
                    // gson.toJson(it)
                    // json -> AlertMsg   gson.fromJson(jsonString, AlertMsg::class.java)

                    val jsonString = gson.toJson(it)
                    val alertMsg = gson.fromJson(jsonString, AlertMsg::class.java)

                    resultList.add(alertMsg)
                }

            }

            updateUIList(resultList.sortedByDescending { it.timestamp })

        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Post failed, log a message

        }
    }

    private fun updateUIList(alertMsgList: List<AlertMsg>) {

        val listView = findViewById<ListView>(R.id.message_list)

        listView.adapter = object : BaseAdapter() {
            // 列表展示多少条数据
            override fun getCount(): Int {
                return alertMsgList.size
            }

            override fun getItem(i: Int): Any {
                return alertMsgList[i]
            }

            override fun getItemId(position: Int): Long {
                return alertMsgList[position].timestamp
            }

            override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

                var itemView = convertView
                if (itemView == null) {
                    itemView = layoutInflater.inflate(R.layout.item_alert_message, parent, false)
                }

                // 这个itemview要展示的数据
                val alertMsg = alertMsgList[position]

                // 心率异常：80
                val tvMsgType = itemView!!.findViewById<TextView>(R.id.tv_msg_type)

                val tvMsg = itemView!!.findViewById<TextView>(R.id.tv_msg)

                if (alertMsg.msg == "心率异常") {
                    tvMsgType.text = "H"
                    tvMsgType.setBackgroundResource(R.drawable.bg_item_text_round_h)
                    tvMsg.text = "${alertMsg.msg} : ${alertMsg.heartRateAverage}"
                } else if (alertMsg.msg == "血氧异常") {
                    tvMsgType.text = "O"
                    tvMsgType.setBackgroundResource(R.drawable.bg_item_text_round_o)
                    tvMsg.text = "${alertMsg.msg} : ${alertMsg.spo2Average}"
                } else if (alertMsg.msg == "Abnormal SpO2 level") {
                    tvMsgType.text = "O"
                    tvMsgType.setBackgroundResource(R.drawable.bg_item_text_round_o)
                    tvMsg.text = "${alertMsg.msg} : ${alertMsg.spo2Average}"
                } else if (alertMsg.msg == "Abnormal heart rate") {
                    tvMsgType.text = "H"
                    tvMsgType.setBackgroundResource(R.drawable.bg_item_text_round_h)
                    tvMsg.text = "${alertMsg.msg} : ${alertMsg.heartRateAverage}"
                }

                val tvTime = itemView.findViewById<TextView>(R.id.tv_time)
                tvTime.text = "${sdf.format(alertMsg.timestamp)}"
                return itemView
            }
        }

    }

    private var databaseReference: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        // 返回键
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val path = "${auth.uid}"
        databaseReference = database.getReference(path)
        // 给数据库添加监听
        databaseReference?.addValueEventListener(valueEventListener)

    }

    override fun onDestroy() {
        super.onDestroy()
        databaseReference?.removeEventListener(valueEventListener)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            android.R.id.home -> { // 点解了返回键
                finish() // 关闭页面
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}