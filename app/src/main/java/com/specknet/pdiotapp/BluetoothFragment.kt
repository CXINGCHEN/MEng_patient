package com.specknet.pdiotapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.cxc.arduinobluecontrol.bluetooth.BluetoothManager
import com.cxc.arduinobluecontrol.dialog.DeviceListDialog
import com.cxc.arduinobluecontrol.dialog.DeviceListDialog.DeviceSelectedListener
import com.cxc.arduinobluecontrol.dialog.LoadingDialog
import com.cxc.arduinobluecontrol.util.SharedPrefsUtil
import com.specknet.pdiotapp.bluetooth.BluetoothSpeckService
import com.specknet.pdiotapp.utils.Constants
import com.specknet.pdiotapp.utils.Utils

/**
 * A simple [Fragment] subclass.
 * Use the [BluetoothFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BluetoothFragment : Fragment() {

    private lateinit var thingy1ID: EditText
    private lateinit var connectSensorsButton: Button
    private lateinit var restartConnectionButton: Button
    private lateinit var thingy2ID: EditText
    lateinit var sharedPreferences: SharedPreferences

    var nfcAdapter: NfcAdapter? = null
    val MIME_TEXT_PLAIN = "application/vnd.bluetooth.le.oob"
    private val TAG = "NFCReader"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bluetooth, container, false)
    }

    /**
     * 视图创建完成
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        thingy1ID = view.findViewById(R.id.thingy1_code)
        connectSensorsButton = view.findViewById(R.id.connect_sensors_button)
        restartConnectionButton = view.findViewById(R.id.restart_service_button)

        thingy2ID = view.findViewById(R.id.thingy2_code)

        view.findViewById<Button>(R.id.connect_arduino_button).setOnClickListener {

            showDeviceList()
        }

        connectSensorsButton.setOnClickListener {
            // TODO don't enable this until both sensors have been scanned? or at least warn the user
            // start the bluetooth service

            sharedPreferences.edit().putString(
                Constants.THINGY1_MAC_ADDRESS_PREF,
                thingy1ID.text.toString()
            ).apply()

            sharedPreferences.edit().putString(
                Constants.THINGY2_MAC_ADDRESS_PREF,
                thingy2ID.text.toString()
            ).apply()

            startSpeckService()

        }

        restartConnectionButton.setOnClickListener {
            startSpeckService()
        }


        sharedPreferences =
            activity!!.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE)
        if (sharedPreferences.contains(Constants.THINGY1_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a thingy1 ID")
            thingy1ID.setText(
                sharedPreferences.getString(
                    Constants.THINGY1_MAC_ADDRESS_PREF,
                    ""
                )
            )
        } else {
            Log.i("sharedpref", "No thingy1 seen before")
            connectSensorsButton.isEnabled = false
            connectSensorsButton.isClickable = false
        }

        if (sharedPreferences.contains(Constants.THINGY2_MAC_ADDRESS_PREF)) {
            Log.i("sharedpref", "Already saw a thingy2 ID")

            thingy2ID.setText(
                sharedPreferences.getString(
                    Constants.THINGY2_MAC_ADDRESS_PREF,
                    ""
                )
            )
        }

        thingy1ID.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(cs: CharSequence, start: Int, before: Int, count: Int) {
                if (cs.toString().trim().length != 17) {
                    connectSensorsButton.isEnabled = false
                    connectSensorsButton.isClickable = false
                } else {
                    connectSensorsButton.isEnabled = true
                    connectSensorsButton.isClickable = true
                }
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })

        thingy1ID.filters = arrayOf<InputFilter>(InputFilter.AllCaps())

        thingy2ID.filters = arrayOf<InputFilter>(InputFilter.AllCaps())
        val nfcManager = activity!!.getSystemService(Context.NFC_SERVICE) as NfcManager
        nfcAdapter = nfcManager.defaultAdapter

        if (nfcAdapter == null) {
            Toast.makeText(activity, "Phone does not support NFC pairing", Toast.LENGTH_LONG).show()
        } else if (nfcAdapter!!.isEnabled()) {
            Toast.makeText(activity, "NFC Enabled", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(activity, "NFC Disabled", Toast.LENGTH_LONG).show()
        }


    }

    fun showDeviceList() {
        DeviceListDialog(object : DeviceSelectedListener {
            // com.broxcode.arduinobluetoothfree.dialog.DeviceListDialog.DeviceSelectedListener
            override fun onDeviceSelected(str: String, str2: String) {
                Log.d("HomeActivity", "Selected device $str2| Address $str")
                BluetoothConnectTask(activity as AppCompatActivity, childFragmentManager, str, str2)
                    .execute(*arrayOfNulls<Void>(0))
            }

            // com.broxcode.arduinobluetoothfree.dialog.DeviceListDialog.DeviceSelectedListener
            override fun onDismiss() {
            }
        }).show(childFragmentManager, DeviceListDialog.TAG)
    }


    fun startSpeckService() {
        // TODO if it's not already running
        val isServiceRunning =
            Utils.isServiceRunning(BluetoothSpeckService::class.java, context!!.applicationContext)
        Log.i("service", "isServiceRunning = " + isServiceRunning)

        if (!isServiceRunning) {
            Log.i("service", "Starting BLT service")
            val simpleIntent = Intent(activity, BluetoothSpeckService::class.java)
            activity!!.startService(simpleIntent)
        } else {
            Log.i("service", "Service already running, restart")
            activity!!.stopService(Intent(activity, BluetoothSpeckService::class.java))
            Toast.makeText(activity, "restarting service with new sensors", Toast.LENGTH_SHORT)
                .show()
            activity!!.startService(Intent(activity, BluetoothSpeckService::class.java))

        }
    }

    class BluetoothConnectTask internal constructor(
        private val activity: AppCompatActivity,
        private val fragmentManager: FragmentManager,
        private val mAddress: String,
        private val mDeviceName: String
    ) :
        AsyncTask<Void?, Void?, Void?>() {
        private var mLoadingDialog: LoadingDialog? = null

        // android.os.AsyncTask
        public override fun onPreExecute() {
            // 先loading弹窗 标记任务即将开始执行
            mLoadingDialog = LoadingDialog()
            val bundle = Bundle()
            bundle.putString(LoadingDialog.DEVICE_NAME_KEY, mDeviceName)
            mLoadingDialog!!.arguments = bundle
            mLoadingDialog!!.show(fragmentManager, LoadingDialog.TAG)
        }

        // android.os.AsyncTask
        override fun doInBackground(vararg voidArr: Void?): Void? {
            // 耗时任务
            connectToDevice(mAddress, mDeviceName)
            return null
        }

        // android.os.AsyncTask
        public override fun onPostExecute(r2: Void?) {
            // 执行完了 loading弹窗消失
            val loadingDialog = mLoadingDialog
            loadingDialog?.dismissAllowingStateLoss()
        }

        fun connectToDevice(mac: String?, devicename: String) {
            // mac 和name 保存到本地  下次就可以自动连了
            SharedPrefsUtil.save(activity.applicationContext, SharedPrefsUtil.MAC_ADDRESS_KEY, mac)
            SharedPrefsUtil.save(
                activity.applicationContext,
                SharedPrefsUtil.DEVICE_NAME_KEY,
                devicename
            )
            val success: Boolean = BluetoothManager.getInstance().connectTo(mac)

            if (success) {
                // 通知HomeFragment处理数据
               (activity as HomeActivity?)?.notifyHomeStartListen()

            }

            activity.runOnUiThread {
                com.cxc.arduinobluecontrol.util.Utils.showToastMessage(
                    activity,
                    (if (success) "Connected to " else "Could not connect to ") + devicename
                )
            }
        }
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BluetoothFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() = BluetoothFragment()
    }
}