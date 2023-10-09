package com.specknet.pdiotapp.utils

import android.content.Intent
import android.util.Log
import com.specknet.pdiotapp.bluetooth.BluetoothSpeckService

/**
 * This class processes new RESpeck packets which are passed from the SpeckBluetoothService.
 * It contains all logic to transform the incoming bytes into the desired variables and then stores and broadcasts
 * this information
 */
class Thingy1PacketHandler(val speckService: BluetoothSpeckService) {

    private val TAG = "Thingy1PacketHandler"

    fun processThingy1Packet(values: ByteArray) {
        val actualPhoneTimestamp = Utils.getUnixTimestamp()

        val thingy1Data = ThingyPacketDecoder.decodeThingyPacket(values)

        Log.d(TAG, "processThingy1Packet: decoded data " + thingy1Data.toString())

        // TODO only one sample per batch here
        val newThingyLiveData = ThingyLiveData(
            actualPhoneTimestamp,
            thingy1Data.accelData.x,
            thingy1Data.accelData.y,
            thingy1Data.accelData.z,
            thingy1Data.gyroData,
            thingy1Data.magData
        )
        Log.i("Freq", "newThingyLiveData = $newThingyLiveData")

        // Send live broadcast intent
        val liveDataIntent = Intent(Constants.ACTION_THINGY1_BROADCAST)
        liveDataIntent.putExtra(Constants.THINGY1_LIVE_DATA, newThingyLiveData)
        speckService.sendBroadcast(liveDataIntent)

    }

}