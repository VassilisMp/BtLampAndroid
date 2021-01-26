package gr.cs.btlamp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern

private const val REQUEST_ENABLE_BT: Int = 1
class MainActivity : AppCompatActivity() {

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var device: BluetoothDevice? = null
    private val PORT_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") //Serial Port Service ID


    private var inputStream: InputStream? = null
    private val sb = StringBuilder()
    var buffer: ByteArray? = null
    var thread: Thread? = null
    var stopThread = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        this.registerReceiver(mReceiver, filter)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        BTPermission()
    }

    /*This method is used to check if bluetooth is disconnected. If disconnected try to reconnect automatically*/
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device =
                intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                BTconnect()
            }
        }
    }

    fun BTPermission() {
        if (mBluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (mBluetoothAdapter == null) {
            bluetoothNotAvailable()
        } else {
            if (mBluetoothAdapter!!.isEnabled) {
                bluetoothEnabled()
            } else {
                bluetoothDisabled()
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                bluetoothEnabled()
            } else if (resultCode == RESULT_CANCELED) {
                finish()
            }
        }
    }

    private fun bluetoothNotAvailable() {
        TODO()
        /*mBluetoothStatusBtn.setImageResource(R.drawable.bluetooth_unavailable)
        mBluetoothStatusText.setText("Bluetooth is not available")*/
    }

    private fun bluetoothEnabled() {
        /*mBluetoothStatusBtn.setImageResource(R.drawable.bluetooth_enabled)
        mBluetoothStatusText.setText("Bluetooth is ON")*/
        BTinit()
    }

    private fun bluetoothDisabled() {
        /*mBluetoothStatusBtn.setImageResource(R.drawable.bluetooth_disabled)
        mBluetoothStatusText.setText("Bluetooth is OFF")*/
    }


    fun BTinit() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bondedDevices =
            bluetoothAdapter.bondedDevices
        if (bondedDevices.isEmpty()) {
            Toast.makeText(
                applicationContext,
                "Please pair with MotoTelemetry bluetooth HC-06 first",
                Toast.LENGTH_LONG
            ).show()
        } else {
            for (bluetoothDevice in bondedDevices) {
                bluetoothDevice.name
                Log.d("BTname", bluetoothDevice.name)
                if (bluetoothDevice.name == "HUAWEI P8 lite") {
                    device = bluetoothDevice
                    BTconnect()
                    return
                }
            }
            /*If bluetooth device not found on paired devices list show display this toast*/Toast.makeText(
                applicationContext,
                "MotoTelemetry not found. Please pair with the device first",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun BTconnect() {
        var connected = true
        var socket: BluetoothSocket? = null
        try {
            socket = device!!.createRfcommSocketToServiceRecord(PORT_UUID)
            socket.connect()
        } catch (e: IOException) {
            e.printStackTrace()
            connected = false
        }
        if (connected) {
            try {
                inputStream = socket?.inputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            beginListenForData()
//            mBluetoothStatusText.setText("Connected")
            Snackbar.make(findViewById(android.R.id.content), "Connected", Snackbar.LENGTH_LONG)
                .show()
        } else {
            val connectionSnackbar = Snackbar.make(
                findViewById(android.R.id.content),
                "Could not connect to host",
                Snackbar.LENGTH_INDEFINITE
            )
            connectionSnackbar.setAction(
                "Retry"
            ) {
                BTconnect()
                connectionSnackbar.dismiss()
            }
            connectionSnackbar.show()
        }
    }

    private fun beginListenForData() {
        val handler = Handler()
        stopThread = false
        buffer = ByteArray(1024)
        thread = Thread {
            while (!Thread.currentThread().isInterrupted && !stopThread) {
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                try {
                    val byteCount = inputStream!!.available()
                    inputStream?.readBytes()
                    if (byteCount > 0) {
                        val rawBytes = ByteArray(byteCount)
                        inputStream!!.read(rawBytes)
                        val string =
                                String(rawBytes, StandardCharsets.UTF_8)
                        handler.post {
                            val count = string.length - string.replace("s", "").length
                            val checkStr =
                                    Pattern.compile("s.*?s").matcher(string)
                            if (checkStr.find() && count == 2) {
                                val substring = string.substring(
                                        1,
                                        string.length - 1
                                ) // remove the identification symbols
                                val strings =
                                        substring.trim { it <= ' ' }.replace("s".toRegex(), "")
                                                .split("\t".toRegex()).toTypedArray()
                                /*test.ardObjectTemp = strings[0]
                                test.ardPitch = strings[1]
                                test.ardRoll = strings[2]
                                test.ardSpeed = strings[3]
                                test.ardOdometer = strings[4]
                                txtArduino.setText(string)
                                objectTemp.setText(test.ardObjectTemp)
                                pitch.setText(test.ardPitch)
                                roll.setText(test.ardRoll)
                                speed.setText(test.ardSpeed)
                                odometer.setText(test.ardOdometer)
                                Log.d("test", string)
                                val pitchRotation: Double = test.ardPitch.toDouble()
                                val rollRotation: Double = test.ardRoll.toDouble()
                                mmotorbikeSide.setRotation(pitchRotation.toFloat())
                                mmotorbikeFront.setRotation(rollRotation.toFloat())
                                if (uploadData) {
                                    sendDbData()
                                    repeats++
                                }*/
                            } else {
                                Log.d("error", string)
                            }
                        }
                    }
                } catch (ex: IOException) {
                    stopThread = true
                }
            }
        }
        thread!!.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        thread!!.interrupt()
    }
}