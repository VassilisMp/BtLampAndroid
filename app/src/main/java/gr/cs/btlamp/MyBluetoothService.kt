package gr.cs.btlamp

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Executors


private const val TAG = "MY_APP_DEBUG_TAG"

// Defines several constants used when transmitting messages between the
// service and the UI.
const val MESSAGE_READ: Int = 0
const val MESSAGE_WRITE: Int = 1
const val MESSAGE_TOAST: Int = 2
// ... (Add other message types here as needed.)
private const val btDeviceName = "HUAWEI P8 lite"
private const val STD_UUID = "00001101-0000-1000-8000-00805f9b34fb"
private const val other_uuid = "0000110a-0000-1000-8000-00805f9b34fb"
private val PORT_UUID =
    UUID.fromString(other_uuid) //Serial Port Service ID
private const val HUAWEI_MAC = "8C:25:05:0E:CB:06"
private const val XIAOMI_MAC = "18:F0:E4:E0:A3:16"
val btDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

class MyBluetoothService : Service() {

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var device: BluetoothDevice? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    val channel: Channel<String> by lazy { Channel() }
    private var connectionListener: ConnectionListener? = null

    fun setConnectionListener(listener: ConnectionListener) {
        connectionListener = listener
    }

    override fun onCreate() {
        super.onCreate()
//        Toast.makeText(this, "The new Service was Created", Toast.LENGTH_LONG).show()
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // For time consuming an long tasks you can launch a new thread here...
        // Do your Bluetooth Work Here
        GlobalScope.launch { BTinit() }
//        showToast(" Service Started")
        return super.onStartCommand(intent, flags, startId)
    }

    suspend fun BTinit(): Unit = withContext(btDispatcher) {
        /*val fetchUuidsWithSdp = device?.fetchUuidsWithSdp()
        Log.d(TAG, fetchUuidsWithSdp.toString())
        device?.uuids?.forEach {
            Log.d(TAG, it.uuid.toString())
        }*/
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices = bluetoothAdapter?.bondedDevices
        Log.d(TAG, "in BTinit")
        if (pairedDevices?.isEmpty() == true) showToastC("Pair with device first and retry")
        else {
            for (device in pairedDevices!!) {
                val deviceName = device.name
//                val deviceHardwareAddress = device.address // MAC address
                if (deviceName == btDeviceName) {
                    this@MyBluetoothService.device = device
                    showToastC("Paired with $deviceName")
                    bluetoothAdapter.cancelDiscovery()
                    BTconnect()
                    return@withContext
                }
            }
            // if not found
            showToastC("Pair with device first and retry")
        }
        snackBarMakeC(
            "Not paired with device",
            actionText = "Retry",
            block = { GlobalScope.launch(Dispatchers.Main) { BTinit() } }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        showToast("Service Destroyed")
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder {
        //mHandler = getApplication().getHandler();
        return mBinder
    }

    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: MyBluetoothService
            get() = this@MyBluetoothService
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun BTconnect(): Boolean = withContext(btDispatcher) {
        val socket: BluetoothSocket?
        try {
            socket = device!!.createRfcommSocketToServiceRecord(PORT_UUID)
            socket.connect()
            inputStream = socket?.inputStream
            outputStream = socket?.outputStream
        } catch (e: IOException) {
            e.printStackTrace()
            snackBarMakeC(
                "Could not connect to host",
                actionText = "Retry",
                block = { GlobalScope.launch(Dispatchers.Main) { BTconnect() } }
            )
            return@withContext false
        }
//            beginListenForData()
        val buffer = ByteArray(1024) // buffer store for the stream
        var bytes = 0 // bytes returned from read()
        launch(btDispatcher) {
            while (true) {
                try {
                    if (inputStream!!.available() > 0) {
                        buffer[bytes] = inputStream!!.read().toByte()
                        if (buffer[bytes].toChar() == '\n'){
                            val readMessage = String(buffer, 0, bytes)
                            bytes = 0
//                        print(readMessage + "\n")
                            channel.send(readMessage)
                        } else {
                            bytes += 1
                        }
//                    val line = bufferedReader.readLine()
//                    print(stringBuilder.toString() + "\n")
                    } else {
                        delay(1)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        launch(Dispatchers.Main) {
            SnackbarWrapper.make(applicationContext, "Connected", Snackbar.LENGTH_LONG).show()
            initReceiver()
            connectionListener?.onConnected()
        }
        return@withContext true
    }

    /* Call this from the main activity to send data to the remote device */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun write(input: String) = GlobalScope.launch(btDispatcher) {
        val bytes = input.toByteArray() //converts entered String into bytes
        try {
            outputStream?.write(bytes)
        } catch (e: IOException) {
            Log.e("Send Error", "Unable to send message", e)
            showToast("Unable to send message")
        }
    }

    private fun initReceiver() {
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        this@MyBluetoothService.registerReceiver(mReceiver, filter)
    }

    /*This method is used to check if bluetooth is disconnected. If disconnected try to reconnect automatically*/
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                GlobalScope.launch(Dispatchers.Main) {
                    connectionListener?.onDisconnected()
                    while (!BTconnect()) {
                        showToast("Lost bluetooth connection with ${device?.name}, retrying...")
                    }
                }
            }
        }
    }

    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
    }
}