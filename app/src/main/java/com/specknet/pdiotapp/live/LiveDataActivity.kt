package com.specknet.pdiotapp.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.R
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.ThingyLiveData


class LiveDataActivity : AppCompatActivity() {

    // global graph variables
    lateinit var datasetThingy1AccelX: LineDataSet
    lateinit var datasetThingy1AccelY: LineDataSet
    lateinit var datasetThingy1AccelZ: LineDataSet

    lateinit var datasetThingy2AccelX: LineDataSet
    lateinit var datasetThingy2AccelY: LineDataSet
    lateinit var datasetThingy2AccelZ: LineDataSet

    var time = 0f
    lateinit var allThingy1Data: LineData

    lateinit var allThingy2Data: LineData

    lateinit var thingy1Chart: LineChart
    lateinit var thingy2Chart: LineChart

    // global broadcast receiver so we can unregister it
    lateinit var thingy1LiveUpdateReceiver: BroadcastReceiver
    lateinit var thingy2LiveUpdateReceiver: BroadcastReceiver
    lateinit var looperThingy1: Looper
    lateinit var looperThingy2: Looper

    val filterTestThingy1 = IntentFilter(Constants.ACTION_THINGY1_BROADCAST)
    val filterTestThingy2 = IntentFilter(Constants.ACTION_THINGY2_BROADCAST)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_data)

        setupCharts()

        // set up the broadcast receiver
        thingy1LiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY1_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY1_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    time += 1
                    updateGraph("thingy1", x, y, z)

                }
            }
        }

        // register receiver on another thread
        val handlerThreadThingy1 = HandlerThread("bgThreadThingy1Live")
        handlerThreadThingy1.start()
        looperThingy1 = handlerThreadThingy1.looper
        val handlerThingy1 = Handler(looperThingy1)
        this.registerReceiver(thingy1LiveUpdateReceiver, filterTestThingy1, null, handlerThingy1)

        // set up the broadcast receiver
        thingy2LiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY2_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY2_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    // get all relevant intent contents
                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ

                    time += 1
                    updateGraph("thingy2", x, y, z)

                }
            }
        }

        // register receiver on another thread
        val handlerThreadThingy2 = HandlerThread("bgThreadThingy2Live")
        handlerThreadThingy2.start()
        looperThingy2 = handlerThreadThingy2.looper
        val handlerThingy2 = Handler(looperThingy2)
        this.registerReceiver(thingy2LiveUpdateReceiver, filterTestThingy2, null, handlerThingy2)

    }


    fun setupCharts() {
        thingy1Chart = findViewById(R.id.thingy1_chart)
        thingy2Chart = findViewById(R.id.thingy2_chart)

        time = 0f
        val entries_res_accel_x = ArrayList<Entry>()
        val entries_res_accel_y = ArrayList<Entry>()
        val entries_res_accel_z = ArrayList<Entry>()

        datasetThingy1AccelX = LineDataSet(entries_res_accel_x, "Accel X")
        datasetThingy1AccelY = LineDataSet(entries_res_accel_y, "Accel Y")
        datasetThingy1AccelZ = LineDataSet(entries_res_accel_z, "Accel Z")

        datasetThingy1AccelX.setDrawCircles(false)
        datasetThingy1AccelY.setDrawCircles(false)
        datasetThingy1AccelZ.setDrawCircles(false)

        datasetThingy1AccelX.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        datasetThingy1AccelY.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        datasetThingy1AccelZ.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsRes = ArrayList<ILineDataSet>()
        dataSetsRes.add(datasetThingy1AccelX)
        dataSetsRes.add(datasetThingy1AccelY)
        dataSetsRes.add(datasetThingy1AccelZ)

        allThingy1Data = LineData(dataSetsRes)
        thingy1Chart.data = allThingy1Data
        thingy1Chart.invalidate()

        // Thingy

        time = 0f
        val entries_thingy_accel_x = ArrayList<Entry>()
        val entries_thingy_accel_y = ArrayList<Entry>()
        val entries_thingy_accel_z = ArrayList<Entry>()

        datasetThingy2AccelX = LineDataSet(entries_thingy_accel_x, "Accel X")
        datasetThingy2AccelY = LineDataSet(entries_thingy_accel_y, "Accel Y")
        datasetThingy2AccelZ = LineDataSet(entries_thingy_accel_z, "Accel Z")

        datasetThingy2AccelX.setDrawCircles(false)
        datasetThingy2AccelY.setDrawCircles(false)
        datasetThingy2AccelZ.setDrawCircles(false)

        datasetThingy2AccelX.setColor(
            ContextCompat.getColor(
                this,
                R.color.red
            )
        )
        datasetThingy2AccelY.setColor(
            ContextCompat.getColor(
                this,
                R.color.green
            )
        )
        datasetThingy2AccelZ.setColor(
            ContextCompat.getColor(
                this,
                R.color.blue
            )
        )

        val dataSetsThingy = ArrayList<ILineDataSet>()
        dataSetsThingy.add(datasetThingy2AccelX)
        dataSetsThingy.add(datasetThingy2AccelY)
        dataSetsThingy.add(datasetThingy2AccelZ)

        allThingy2Data = LineData(dataSetsThingy)
        thingy2Chart.data = allThingy2Data
        thingy2Chart.invalidate()
    }

    fun updateGraph(graph: String, x: Float, y: Float, z: Float) {
        // take the first element from the queue
        // and update the graph with it
        if (graph == "thingy1") {
            datasetThingy1AccelX.addEntry(Entry(time, x))
            datasetThingy1AccelY.addEntry(Entry(time, y))
            datasetThingy1AccelZ.addEntry(Entry(time, z))

            runOnUiThread {
                allThingy1Data.notifyDataChanged()
                thingy1Chart.notifyDataSetChanged()
                thingy1Chart.invalidate()
                thingy1Chart.setVisibleXRangeMaximum(150f)
                thingy1Chart.moveViewToX(thingy1Chart.lowestVisibleX + 40)
            }
        } else if (graph == "thingy2") {
            datasetThingy2AccelX.addEntry(Entry(time, x))
            datasetThingy2AccelY.addEntry(Entry(time, y))
            datasetThingy2AccelZ.addEntry(Entry(time, z))

            runOnUiThread {
                allThingy2Data.notifyDataChanged()
                thingy2Chart.notifyDataSetChanged()
                thingy2Chart.invalidate()
                thingy2Chart.setVisibleXRangeMaximum(150f)
                thingy2Chart.moveViewToX(thingy2Chart.lowestVisibleX + 40)
            }
        }


    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(thingy1LiveUpdateReceiver)
        unregisterReceiver(thingy2LiveUpdateReceiver)
        looperThingy1.quit()
        looperThingy2.quit()
    }
}
