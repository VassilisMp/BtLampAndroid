package gr.cs.btlamp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.larswerkman.holocolorpicker.ColorPicker
import com.larswerkman.holocolorpicker.OpacityBar
import com.larswerkman.holocolorpicker.SVBar
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Executors


private const val REQUEST_ENABLE_BT: Int = 1
private const val btDeviceName = "HUAWEI P8 lite"
private const val STD_UUID = "00001101-0000-1000-8000-00805f9b34fb"
private const val other_uuid = "0000110a-0000-1000-8000-00805f9b34fb"
private val PORT_UUID =
        UUID.fromString(other_uuid) //Serial Port Service ID
private const val TAG = "colPic"
private const val HUAWEI_MAC = "8C:25:05:0E:CB:06"
private const val XIAOMI_MAC = "18:F0:E4:E0:A3:16"
class ColorPickerActivity : AppCompatActivity(), ColorPicker.OnColorChangedListener {

    private lateinit var picker: ColorPicker
    private lateinit var svBar: SVBar
    private lateinit var opacityBar: OpacityBar
    private lateinit var onOffBar: SeekBar
    private lateinit var text: TextView

    private lateinit var nestedLinLt: LinearLayout
    private lateinit var btn: Button
    var initialHeight = 0
    var actualHeight = 0

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var device: BluetoothDevice? = null

    private var inputStream: InputStream? = null
    private val sb = StringBuilder()
    var buffer: ByteArray? = null
    var thread: Thread? = null
    var stopThread = false

    /* service binding code */
//    private lateinit var mService: LocalService
//    private var mBound: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    /*private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as LocalService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        // Bind to LocalService
        Intent(this, LocalService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }

    fun useService() {
        if (mBound) {
            // Call a method from the LocalService.
            // However, if this call were something that might hang, then this request should
            // occur in a separate thread to avoid slowing down the activity performance.
            val num: Int = mService.randomNumber
            Toast.makeText(this, "number: $num", Toast.LENGTH_SHORT).show()
        }
    }*/

    var mmSocket: BluetoothSocket? = null
    var connectedThread: ConnectedThread? = null
    var createConnectThread: CreateConnectThread? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)
        picker = findViewById(R.id.picker)
        svBar = findViewById(R.id.svbar)
        opacityBar = findViewById(R.id.opacitybar)
        text = findViewById(R.id.textView_color)
        onOffBar = findViewById(R.id.on_off_bar)
        picker.addSVBar(svBar)
        picker.addOpacityBar(opacityBar)
        picker.onColorChangedListener = this
        /*button.setOnClickListener {
            text!!.setTextColor(picker.color)
            picker.oldCenterColor = picker.color
        }*/
        /*val serviceIntent = Intent(this, MyBluetoothService::class.java)
        startService(serviceIntent)
        bindService(Intent(this, MyBluetoothService::class.java), )*/
        val bluetoothDispatcher: ExecutorCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        GlobalScope.launch(bluetoothDispatcher) { 
            
        }
        GlobalScope
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        createConnectThread = CreateConnectThread(bluetoothAdapter, HUAWEI_MAC)
        createConnectThread!!.start()

    }



    /* ============================ Thread to Create Bluetooth Connection =================================== */
    inner class CreateConnectThread(bluetoothAdapter: BluetoothAdapter, address: String?) : Thread() {
        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            bluetoothAdapter.cancelDiscovery()
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket!!.connect()
                Log.e("Status", "Device connected")
//                handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget()
            } catch (connectException: IOException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket?.close()
                    Log.e("Status", "Cannot connect to device")
//                    handler.obtainMessage(CONNECTING_STATUS, -1, -1).sendToTarget()
                } catch (closeException: IOException) {
                    Log.e(TAG, "Could not close the client socket", closeException)
                }
                return
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = ConnectedThread(mmSocket!!)
            connectedThread!!.run()
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }

        init {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
            var tmp: BluetoothSocket? = null
            val uuid = bluetoothDevice.uuids[0].uuid
            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid)
            } catch (e: IOException) {
                Log.e(TAG, "Socket's create() method failed", e)
            }
            mmSocket = tmp
        }
    }

    /* =============================== Thread for Data Transfer =========================================== */
    class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            val channel = Channel<String>()
            val bufferedReader = mmInStream!!.bufferedReader()
            val readDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            GlobalScope.launch(Dispatchers.Main) {
                while (true) {
                    withContext(readDispatcher) {
                        val message = bufferedReader.readLine()
                        channel.send(message)
                    }
                }
            }
            val buffer = ByteArray(1024) // buffer store for the stream
            var bytes = 0 // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = mmInStream!!.read().toByte()
                    var readMessage: String
                    if (buffer[bytes].toChar() == '\n') {
                        readMessage = String(buffer, 0, bytes)
                        Log.e("Arduino Message", readMessage)
//                        handler.obtainMessage(MESSAGE_READ, readMessage).sendToTarget()
                        bytes = 0
                    } else {
                        bytes++
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        fun write(input: String) {
            val bytes = input.toByteArray() //converts entered String into bytes
            try {
                mmOutStream?.write(bytes)
            } catch (e: IOException) {
                Log.e("Send Error", "Unable to send message", e)
            }
        }

        /* Call this from the main activity to shutdown the connection */
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private val receiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when(action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    val deviceName = device?.name
                    val deviceHardwareAddress = device?.address // MAC address
                }
            }
        }
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

    fun BTconnect() {
        val fetchUuidsWithSdp = device?.fetchUuidsWithSdp()
        Log.d(TAG, fetchUuidsWithSdp.toString())
        device?.uuids?.forEach {
            Log.d(TAG, it.uuid.toString())
        }
        var connected = true
        GlobalScope.launch(Dispatchers.IO) {
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
//            val serviceIntent = Intent(this, MyBluetoothService::class.java)
//            startService(serviceIntent)
//            beginListenForData()
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
    }

    private fun bluetoothNotAvailable() {
        // Device doesn't support Bluetooth
        showToast("Device doesn't support Bluetooth")
    }

    private fun bluetoothEnabled() {
        /*mBluetoothStatusBtn.setImageResource(R.drawable.bluetooth_enabled)
        mBluetoothStatusText.setText("Bluetooth is ON")*/
        device = BTinit()
        Log.d(TAG, device?.name.toString())
        if (device != null)
            BTconnect()
    }

    private fun bluetoothDisabled() {
        /*mBluetoothStatusBtn.setImageResource(R.drawable.bluetooth_disabled)
        mBluetoothStatusText.setText("Bluetooth is OFF")*/
    }

    @SuppressLint("SetTextI18n")
    override fun onColorChanged(color: Int) {
        //gives the color when it's changed.
//        Toast.makeText(this, """color: $color""", Toast.LENGTH_SHORT).show()
        picker.oldCenterColor = picker.color
        val red = Color.red(picker.color)
        val green = Color.green(picker.color)
        val blue = Color.blue(picker.color)
        val alpha = Color.alpha(picker.color)

//        text.setTextColor(picker.color)
        text.text = "%02x%02x%02x%02x".format(red, green, blue, alpha)
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

    override fun onDestroy() {
        super.onDestroy()
        thread!!.interrupt()
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver)

    }

    inner class DropDownAnim(
            private val view: View,
            private val targetHeight: Int,
            private val down: Boolean
    ) : Animation() {
        override fun applyTransformation(
                interpolatedTime: Float,
                t: Transformation
        ) {
            val newHeight: Int = if (down) {
                (targetHeight * interpolatedTime).toInt()
            } else {
                (targetHeight * (1 - interpolatedTime)).toInt()
            }
            view.layoutParams.height = newHeight
            view.requestLayout()
        }

        override fun willChangeBounds(): Boolean {
            return true
        }

    }

    private fun Int.toHexString() = Integer.toHexString(this)
}

fun Context.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Context.BTinit(): BluetoothDevice? {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val pairedDevices = bluetoothAdapter?.bondedDevices
    if (pairedDevices?.isEmpty() == true) showToast("Pair with device first")
    else {
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            if (deviceName == btDeviceName) {
                return device
            }
        }
        // if not found
        showToast("Pair with device first")
    }
    return null
}