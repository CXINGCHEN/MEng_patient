package com.specknet.pdiotapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.cxc.arduinobluecontrol.AlertMsg
import com.cxc.arduinobluecontrol.DatabaseManager
import com.cxc.arduinobluecontrol.MsgUtils
import com.cxc.arduinobluecontrol.Spo2AndHeartRateBean
import com.cxc.arduinobluecontrol.bluetooth.BluetoothManager
import com.cxc.arduinobluecontrol.bluetooth.IncomingDataListener
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.specknet.pdiotapp.bean.RecognitionResultBean
import com.specknet.pdiotapp.ml.MyRmodel
import com.specknet.pdiotapp.ml.MyTmodel
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.ThingyLiveData
import org.json.JSONException
import org.json.JSONObject
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment(), IncomingDataListener {

    private var tvHeartRate: TextView? = null
    private var tvo2: TextView? = null


    private var errorLayout // 传感器异常的提示
            : LinearLayout? = null

    var bluetoothManager = BluetoothManager.getInstance()

    // 上一次接收到数据的时间
    private var lastReceiveDataTime = 0L

    private val handler = Handler()


    private val heartRateList = mutableListOf<Int>()
    private val spo2List = mutableListOf<Double>()

    val DATA_SIZE = 20


    lateinit var predictionProgressBar: ProgressBar
    lateinit var predictionText: TextView


    // 存储每个动作识别到的次数
    val map = mutableMapOf<String, Int>()
    var count = 0


    val labels = listOf(
        "Sitting straight",
        "Sitting bent forward",
        "Sitting bent backward",
        "Standing",
        "Lying down left",
        "Lying down right",
        "Lying down front",
        "Lying down back",
        "Walking",
        "Running",
        "Ascending stairs",
        "Descending stairs",
        "Desk work",
        "General movement"
    )

    // 所有的动作概率
    val actionsMap = hashMapOf<String, Float>()

    private var lastlabel = ""


    var thingy1ProbabilityArray: FloatArray = FloatArray(14)
    var thingy1PointsNum = 0f
    var thingy1DataPack = java.util.ArrayList<Float>(300)

    lateinit var thingy1LiveUpdateReceiver: BroadcastReceiver
    lateinit var looperThingy1: Looper
    private val filterTestThingy1 = IntentFilter(Constants.ACTION_THINGY1_BROADCAST)


    var thingy2ProbabilityArray: FloatArray = FloatArray(14)
    var thingy2PointsNum = 0f
    var thingy2DataPack = java.util.ArrayList<Float>(300)

    lateinit var thingy2LiveUpdateReceiver: BroadcastReceiver
    lateinit var looperThingy2: Looper
    private val filterTestThingy2 = IntentFilter(Constants.ACTION_THINGY2_BROADCAST)

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


    private var spo2AndHeartRateList = mutableListOf<Spo2AndHeartRateBean>()
    private var actionRealtimeList = mutableListOf<RecognitionResultBean>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * 创建视图
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }


    private fun mockList(): MutableList<RecognitionResultBean> {
        val result = mutableListOf<RecognitionResultBean>()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

        val timestampStart = sdf.parse(sdf.format(Date())).time + (1000 * 60 * 60 * 12)
        val timestampEnd =
            sdf.parse(sdf.format(Date())).time + (1000 * 60 * 60 * 24) - (1000 * 60 * 60 * 11)


        var timestamp = timestampStart
        while (timestamp < (timestampEnd)) {
            val bean = RecognitionResultBean()

            bean.timestamp = timestamp
            bean.labelIndex = Random.nextInt(0, 14)
            result.add(bean)

            timestamp += 50000
        }

        return result

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvHeartRate = view.findViewById<TextView>(R.id.tv_heart_rate)
        tvo2 = view.findViewById<TextView>(R.id.tv_o2)

        predictionProgressBar = view.findViewById(R.id.predicted_activity)
        predictionText = view.findViewById(R.id.predicted_activity_text)

        errorLayout = view.findViewById<LinearLayout>(R.id.ll_error_layout)

        thingy1Chart = view.findViewById(R.id.thingy1_chart)
        thingy2Chart = view.findViewById(R.id.thingy2_chart)

        view.findViewById<View>(R.id.tvThingy1).setOnClickListener {

            // 测试用
            val mockList = mockList()

            actionRealtimeList.addAll(mockList)

            if(actionRealtimeList.size >= 5) {
                DatabaseManager.addActionRealtime(actionRealtimeList)
                actionRealtimeList = mutableListOf()
            }

        }

        setupCharts()

        thingy1LiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)
                val action = intent.action
                if (action == Constants.ACTION_THINGY1_BROADCAST) {
                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY1_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ
                    val gx = liveData.gyro.x
                    val gy = liveData.gyro.y
                    val gz = liveData.gyro.z

                    time += 1
                    updateGraph("thingy1", x, y, z)

                    thingy1PointsNum += 1
                    thingy1DataPack.addAll(
                        arrayListOf(
                            x, y, z, gx, gy, gz
                        )
                    )

                    if (thingy1PointsNum >= 50) {
                        if (thingy1PointsNum % 50 == 0f) {
                            val model = MyRmodel.newInstance(context)
                            // Creates inputs for reference.
                            val inputFeature0 =
                                TensorBuffer.createFixedSize(intArrayOf(1, 50, 6), DataType.FLOAT32)
                            inputFeature0.loadArray(thingy1DataPack.toFloatArray())

                            // Runs model inference and gets result.
                            val outputs = model.process(inputFeature0)
                            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
                            thingy1ProbabilityArray = outputFeature0.floatArray


                            val mergeClassifyResult = mergeClassifyResult(
                                thingy1ProbabilityArray, thingy2ProbabilityArray
                            )

                            if (mergeClassifyResult != null) {
                                val tinyLstmConfidence =
                                    mergeClassifyResult.max()?.times(100)?.roundToInt()

//                                if (lastlabel.isNotEmpty()) {
//
//                                    when (lastlabel) {
//                                        "Sitting straight", "Sitting bent forward", "Sitting bent backward", "Desk work" -> {
//                                            mergeClassifyResult[9] = 0f
//                                            mergeClassifyResult[10] = 0f
//                                            mergeClassifyResult[11] = 0f
//
//                                        }
//                                        "Lying down left", "Lying down right", "Lying down front", "Lying down back" -> {
//                                            mergeClassifyResult[8] = 0f
//                                            mergeClassifyResult[9] = 0f
//                                            mergeClassifyResult[10] = 0f
//                                            mergeClassifyResult[11] = 0f
//                                        }
//                                        "Walking" -> {
//                                            mergeClassifyResult[4] = 0f
//                                            mergeClassifyResult[5] = 0f
//                                            mergeClassifyResult[6] = 0f
//                                            mergeClassifyResult[7] = 0f
//                                        }
//                                        "Running", "Ascending stairs", "Descending stairs" -> {
//                                            mergeClassifyResult[0] = 0f
//                                            mergeClassifyResult[1] = 0f
//                                            mergeClassifyResult[2] = 0f
//                                            mergeClassifyResult[4] = 0f
//                                            mergeClassifyResult[5] = 0f
//                                            mergeClassifyResult[6] = 0f
//                                            mergeClassifyResult[7] = 0f
//                                            mergeClassifyResult[12] = 0f
//                                        }
//                                    }
//
//                                }


                                val label = getLabelText(mergeClassifyResult)
                                lastlabel = label
                                updateUI(label, tinyLstmConfidence)
                                Log.i("Tiny LSTM MODEL scheduleAtFixedRate", label)

                                val bean =
                                    RecognitionResultBean()
                                bean.labelIndex = getLabelIndex(mergeClassifyResult)
                                bean.timestamp = System.currentTimeMillis()
                                bean.label = label
                                actionRealtimeList.add(bean)

                                if(actionRealtimeList.size == 20) {
                                    DatabaseManager.addActionRealtime(actionRealtimeList)
                                    actionRealtimeList = mutableListOf()
                                }


                                // 识别到几个动作 map里面就会有几个key
                                map[label] = map[label]?.plus(1) ?: 1


                                // [0.10,0.46]
                                mergeClassifyResult.forEachIndexed { index, fl ->
                                    actionsMap[labels[index]] = fl
                                }

                                // actionsMap.entries.sortedBy {  }
                                val result = actionsMap.entries.sortedByDescending { it.value }


                            }


                        }

                        thingy1DataPack = java.util.ArrayList(thingy1DataPack.drop(6))
                    }

                }
            }
        }

        // set up the broadcast receiver
        thingy2LiveUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.i("thread", "I am running on thread = " + Thread.currentThread().name)

                val action = intent.action

                if (action == Constants.ACTION_THINGY2_BROADCAST) {

                    val liveData =
                        intent.getSerializableExtra(Constants.THINGY2_LIVE_DATA) as ThingyLiveData
                    Log.d("Live", "onReceive: liveData = " + liveData)

                    val x = liveData.accelX
                    val y = liveData.accelY
                    val z = liveData.accelZ
                    val gx = liveData.gyro.x
                    val gy = liveData.gyro.y
                    val gz = liveData.gyro.z


                    time += 1
                    updateGraph("thingy2", x, y, z)

                    thingy2PointsNum += 1
                    thingy2DataPack.addAll(
                        arrayListOf(
                            x, y, z, gx, gy, gz
                        )
                    )

                    if (thingy2PointsNum >= 50) { // 这边保证给模型的数据thingy2DataPack等于300个

                        // 第51、52、53、...99次就不会运算模型了，直到第100才会再次运算
                        if (thingy2PointsNum % 50 == 0f) {

                            val model = MyTmodel.newInstance(context)

                            // Creates inputs for reference.
                            val inputFeature0 =
                                TensorBuffer.createFixedSize(intArrayOf(1, 50, 6), DataType.FLOAT32)
                            inputFeature0.loadArray(thingy2DataPack.toFloatArray())

                            // Runs model inference and gets result.
                            val outputs = model.process(inputFeature0)
                            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

                            thingy2ProbabilityArray = outputFeature0.floatArray


                            val mergeClassifyResult = mergeClassifyResult(
                                thingy1ProbabilityArray, thingy2ProbabilityArray
                            )

                            if (mergeClassifyResult != null) {
                                val tinyLstmConfidence =
                                    mergeClassifyResult.max()?.times(100)?.roundToInt()

//                                if (lastlabel.isNotEmpty()) {
//
//                                    when (lastlabel) {
//                                        "Sitting straight", "Sitting bent forward", "Sitting bent backward", "Desk work" -> {
//                                            mergeClassifyResult[9] = 0f
//                                            mergeClassifyResult[10] = 0f
//                                            mergeClassifyResult[11] = 0f
//
//                                        }
//                                        "Lying down left", "Lying down right", "Lying down front", "Lying down back" -> {
//                                            mergeClassifyResult[8] = 0f
//                                            mergeClassifyResult[9] = 0f
//                                            mergeClassifyResult[10] = 0f
//                                            mergeClassifyResult[11] = 0f
//                                        }
//                                        "Walking" -> {
//                                            mergeClassifyResult[4] = 0f
//                                            mergeClassifyResult[5] = 0f
//                                            mergeClassifyResult[6] = 0f
//                                            mergeClassifyResult[7] = 0f
//                                        }
//                                        "Running", "Ascending stairs", "Descending stairs" -> {
//                                            mergeClassifyResult[0] = 0f
//                                            mergeClassifyResult[1] = 0f
//                                            mergeClassifyResult[2] = 0f
//                                            mergeClassifyResult[4] = 0f
//                                            mergeClassifyResult[5] = 0f
//                                            mergeClassifyResult[6] = 0f
//                                            mergeClassifyResult[7] = 0f
//                                            mergeClassifyResult[12] = 0f
//                                        }
//                                    }
//
//                                }


                                val label = getLabelText(mergeClassifyResult)
                                lastlabel = label

                                if (label == "Sitting straight" || label == "Sitting bent forward" || label == "Sitting bent backward" || label == "Desk work") {
                                    count += 1
                                    if (count == 5) {
                                        activity?.runOnUiThread {
                                            Toast.makeText(
                                                activity,
                                                "Sitting too long, Please relax for a while",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        count = 0
                                    }
                                } else count = 0


                                updateUI(label, tinyLstmConfidence)
                                Log.i("Tiny LSTM MODEL scheduleAtFixedRate", label)

                                val bean =
                                    RecognitionResultBean()
                                bean.labelIndex = getLabelIndex(mergeClassifyResult)
                                bean.timestamp = System.currentTimeMillis()
                                bean.label = label
                                actionRealtimeList.add(bean)

                                if(actionRealtimeList.size == 20) {
                                    DatabaseManager.addActionRealtime(actionRealtimeList)
                                    actionRealtimeList = mutableListOf()
                                }


                                // 识别到几个动作 map里面就会有几个key
                                map[label] = map[label]?.plus(1) ?: 1


                                // [0.10,0.46]
                                mergeClassifyResult.forEachIndexed { index, fl ->
                                    actionsMap[labels[index]] = fl
                                }

                                // actionsMap.entries.sortedBy {  }
                                val result = actionsMap.entries.sortedByDescending { it.value }


                            }


                        }

                        // 保证thingy2DataPack里面的数据只有300个
                        thingy2DataPack = java.util.ArrayList(thingy2DataPack.drop(6))
                    }

                }
            }
        }


        // register receiver on another thread
        val handlerThreadThingy2 = HandlerThread("bgThreadThingy2Live")
        handlerThreadThingy2.start()
        looperThingy2 = handlerThreadThingy2.looper
        val handlerThingy2 = Handler(looperThingy2)
        activity?.registerReceiver(
            thingy2LiveUpdateReceiver, filterTestThingy2, null, handlerThingy2
        )


        val handlerThreadThingy1 = HandlerThread("bgThreadThingy1Live")
        handlerThreadThingy1.start()
        looperThingy1 = handlerThreadThingy1.looper
        val handlerThingy1 = Handler(looperThingy1)
        activity?.registerReceiver(
            thingy1LiveUpdateReceiver, filterTestThingy1, null, handlerThingy1
        )


    }

    fun getLabelIndex(predictions: FloatArray): Int {
        var max = Float.MIN_VALUE
        var maxIdx = -1
        for (i in labels.indices) {
            if (predictions[i] > max) {
                max = predictions[i]
                maxIdx = i
            }
        }
        return maxIdx
    }


    fun getLabelText(predictions: FloatArray): String {
        var max = Float.MIN_VALUE
        var maxIdx = -1
        for (i in labels.indices) {
            if (predictions[i] > max) {
                max = predictions[i]
                maxIdx = i
            }
        }
        return labels[maxIdx]
    }

    fun updateUI(prediction: String, confidence: Int?) {
        activity?.runOnUiThread {
            predictionProgressBar.progress = confidence as Int
            predictionText.text = prediction
        }
    }

    /**
     * 合并预测结果
     *
     * [0.1,0.001,0.03]
     *
     * [ [0.1,0.001,0.03] ]
     *
     * 默许输入的参数 长度都是outputClasses的大小
     */
    fun mergeClassifyResult(thingy1Result: FloatArray?, thingy2Result: FloatArray?): FloatArray? {

        if (thingy1Result == null && thingy2Result == null) {
            return null
        }
        if (thingy1Result == null) {
            return thingy2Result
        }
        if (thingy2Result == null) {
            return thingy1Result
        }

//   0     "Sitting straight",
//    1    "Sitting bent forward",
//    2    "Sitting bent backward",
//    3    "Standing",
//    4    "Lying down left",
//    5    "Lying down right",
//    6    "Lying down front",
//    7    "Lying down back",
//    8    "Walking",
//    9    "Running",
//    10    "Ascending stairs",
//    11    "Descending stairs",
//        "Desk work",
//        "General movement"

        val result = FloatArray(14)

        result[0] = thingy1Result[0] * 0.4f + thingy2Result[0] * 0.6f
        result[1] = thingy1Result[1] * 0.6f + thingy2Result[1] * 0.4f
        result[2] = thingy1Result[2] * 0.6f + thingy2Result[2] * 0.4f
        result[3] = thingy1Result[3] * 0.4f + thingy2Result[3] * 0.6f
        result[4] = thingy1Result[4]
        result[5] = thingy1Result[5]
        result[6] = thingy1Result[6]
        result[7] = thingy1Result[7]
        result[8] = thingy1Result[8] * 0.7f + thingy2Result[8] * 0.3f
        result[9] = thingy1Result[9] * 0.7f + thingy2Result[9] * 0.3f
        result[10] = thingy1Result[10] * 0.4f + thingy2Result[10] * 0.6f
        result[11] = thingy1Result[11] * 0.4f + thingy2Result[11] * 0.6f
        result[12] = thingy1Result[12] * 0.5f + thingy2Result[12] * 0.5f
        result[13] = thingy1Result[13] * 0.5f + thingy2Result[13] * 0.5f


        return result

    }

    fun setupCharts() {


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
                activity!!, // !! 告诉编译器 activity强制非空 肯定不为空
                R.color.red
            )
        )
        datasetThingy1AccelY.setColor(
            ContextCompat.getColor(
                activity!!, R.color.green
            )
        )
        datasetThingy1AccelZ.setColor(
            ContextCompat.getColor(
                activity!!, R.color.blue
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
                activity!!, R.color.red
            )
        )
        datasetThingy2AccelY.setColor(
            ContextCompat.getColor(
                activity!!, R.color.green
            )
        )
        datasetThingy2AccelZ.setColor(
            ContextCompat.getColor(
                activity!!, R.color.blue
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

            activity?.runOnUiThread {
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

            activity?.runOnUiThread {
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
        bluetoothManager.removeIncomingDataListener(this)

        activity?.unregisterReceiver(thingy1LiveUpdateReceiver)
        looperThingy1.quit()

        activity?.unregisterReceiver(thingy2LiveUpdateReceiver)
        looperThingy2.quit()

        val list = mutableListOf<RecognitionResultBean>()
        map.forEach { (label, count) ->
            list.add(RecognitionResultBean(System.currentTimeMillis(), label, count, "respeck"))
        }


        // mock 测试用
//        val list = listOf(
//            RecognitionResultBean(
//                System.currentTimeMillis(), "walk", 10, "respeck"
//            ),
//            RecognitionResultBean(System.currentTimeMillis(), "Sitting straight", 100, "respeck"),
//            RecognitionResultBean(System.currentTimeMillis(), "Standing", 200, "respeck"),
//            RecognitionResultBean(System.currentTimeMillis(), "Lying down back", 300, "respeck"),
//            RecognitionResultBean(System.currentTimeMillis(), "Ascending stairs", 400, "respeck"),
//        )


        // 页面关闭的时候把数据存入本地
        if (list.isNotEmpty()) {
            // 存储在本地
            // RecognitionResultHelper.saveUserRecognitionData(list)
            // 存储在firebase
            DatabaseManager.addUserRecognitionListData(list)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }

    fun addIncomingDataListener() {
        bluetoothManager.addIncomingDataListener(this)
    }

    override fun onDataReceived(str: String?) {

        try {
            // 把接受到的数据 转换成JSONObject， 方便读取
            val jsonObject = JSONObject(str)
            val spo2 = jsonObject.getDouble("Spo2") ///可能是大于100
            val heartRate = jsonObject.getInt("HeartRate")
            lastReceiveDataTime = System.currentTimeMillis()// 系统当前时间
            errorLayout!!.visibility = View.GONE

            // 给TextView等UI空间修改值的时候必须放到UI线程去执行
            activity?.runOnUiThread(Runnable {
                tvHeartRate!!.text = heartRate.toString()
                tvo2!!.text = Math.min(spo2, 100.0).toString()
            })

            val spo2AndHeartRateBean =
                Spo2AndHeartRateBean(System.currentTimeMillis(), heartRate, spo2)
            spo2AndHeartRateList.add(spo2AndHeartRateBean)

            if(spo2AndHeartRateList.size == 20) {
                DatabaseManager.addSpo2AndHeartRate(spo2AndHeartRateList)
                spo2AndHeartRateList = mutableListOf()
            }


            // 把接受的到最新的数据添加到第0个位置
            heartRateList.add(0, heartRate)

            spo2List.add(0, spo2)
            if (heartRateList.size > DATA_SIZE) {
                // 如果到达期望的长度了 就移除掉最后一个
                heartRateList.removeAt(heartRateList.size - 1)
                spo2List.removeAt(spo2List.size - 1)
                // 移除掉最后一个之后 长度就是N了
                // 现在就可以计算平均值

                val heartRateAverage = heartRateList.average()
                val spo2Average = spo2List.average()

                if (heartRateAverage > 160 || heartRateAverage < 40) {
                    heartRateList.clear()
                    spo2List.clear()
                    val msg = AlertMsg() // 创建了一个对象
                    msg.timestamp = System.currentTimeMillis()
                    //msg.msg = "心率异常"
                    msg.msg = "Abnormal heart rate"
                    msg.heartRateAverage = heartRateAverage
                    msg.spo2Average = spo2Average
                    // MsgUtils.addOneAlertMsgToLocal(msg, activity)
                    DatabaseManager.addOneAlertMsg(msg)
                }
                if (spo2Average < MsgUtils.spo2Average) {
                    heartRateList.clear()
                    spo2List.clear()
                    val msg = AlertMsg() // 创建了一个对象
                    msg.timestamp = System.currentTimeMillis()
                    //msg.msg = "血氧异常"
                    msg.msg = "Abnormal SpO2 level"
                    msg.heartRateAverage = heartRateAverage
                    msg.spo2Average = spo2Average
                    // MsgUtils.addOneAlertMsgToLocal(msg, activity)
                    DatabaseManager.addOneAlertMsg(msg)
                }

            }


            // 异常检测
            handler.removeCallbacksAndMessages(null)
            // 启动一个延迟任务 30秒之后再执行的任务
            handler.postDelayed({ // 30秒之后才会执行到的代码
                val diff = System.currentTimeMillis() - lastReceiveDataTime
                if (diff > 30000) {
                    errorLayout!!.visibility = View.VISIBLE
                }
            }, 30000)
        } catch (e: JSONException) {
            // 如果传进来的str不符合json结构 就会报异常
            tvHeartRate!!.text = "--"
            tvo2!!.text = "--"
        }

    }
}