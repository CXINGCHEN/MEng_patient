package com.specknet.pdiotapp.utils

import android.content.Intent
import android.util.Log
import com.specknet.pdiotapp.bluetooth.BluetoothSpeckService

class Thingy2PacketHandler(val speckService: BluetoothSpeckService) {

    private val TAG = "Thingy2PacketHandler"

    fun processThingy2Packet(values: ByteArray) {
        val actualPhoneTimestamp = Utils.getUnixTimestamp()

        val thingy2Data = ThingyPacketDecoder.decodeThingyPacket(values)

        Log.d(TAG, "processThingy2Packet: decoded data " + thingy2Data.toString())

        // TODO only one sample per batch here
        val newThingyLiveData = ThingyLiveData(
            actualPhoneTimestamp,
            thingy2Data.accelData.x,
            thingy2Data.accelData.y,
            thingy2Data.accelData.z,
            thingy2Data.gyroData,
            thingy2Data.magData
        )
        Log.i("Freq", "newThingy2LiveData = $newThingyLiveData")

        // Send live broadcast intent
        val liveDataIntent = Intent(Constants.ACTION_THINGY2_BROADCAST)
        liveDataIntent.putExtra(Constants.THINGY2_LIVE_DATA, newThingyLiveData)
        speckService.sendBroadcast(liveDataIntent)

    }

}