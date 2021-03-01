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
import androidx.annotation.Nullable
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Executors


private const val TAG = "MyBluetoothService"
//private const val btDeviceName = "HUAWEI P8 lite"
private const val btDeviceName = "HC05"
const val STD_UUID = "00001101-0000-1000-8000-00805f9b34fb"
private const val other_uuid = "0000110a-0000-1000-8000-00805f9b34fb"
val PORT_UUID: UUID =
    UUID.fromString(STD_UUID) //Serial Port Service ID
private const val HUAWEI_MAC = "8C:25:05:0E:CB:06"
private const val XIAOMI_MAC = "18:F0:E4:E0:A3:16"
val btDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

class MyBluetoothService : Service() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }
    private var device: BluetoothDevice? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    val channel: Channel<String> by lazy { Channel() }

    // listener for interaction with ui, activity
    interface ConnectionListener {
        fun onConnected()
        fun onDisconnected()
    }

    private var connectionListener: ConnectionListener? = null

    fun setConnectionListener(listener: ConnectionListener) {
        connectionListener = listener
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "$TAG created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // start Bluetooth service
        BTconnect()
        Log.i(TAG, "$TAG started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        showToast("Service Destroyed")
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder = mBinder

    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: MyBluetoothService
            get() = this@MyBluetoothService
    }

    // bluetooth connection function
    @Suppress("BlockingMethodInNonBlockingContext")
    fun BTconnect(): Job = GlobalScope.launch(btDispatcher) {
        /*val fetchUuidsWithSdp = device?.fetchUuidsWithSdp()
        Log.d(TAG, fetchUuidsWithSdp.toString())
        device?.uuids?.forEach {
            Log.d(TAG, it.uuid.toString())
        }*/
        device = bluetoothAdapter?.bondedDevices?.firstOrNull { it.name == btDeviceName }?.apply {
            bluetoothAdapter?.cancelDiscovery()
            showToastC("Paired with $btDeviceName")
        } ?: snackBarMakeC(
                    "Not paired with device",
                    actionText = "Retry",
                    block = { BTconnect() }
            ).run { return@launch }
        try {
            val socket = device?.createRfcommSocketToServiceRecord(PORT_UUID)
            socket?.connect() ?: throw IOException("Couldn't connect")
            inputStream = socket.inputStream
            outputStream = socket.outputStream
        } catch (e: IOException) {
            e.printStackTrace()
            // show reconnect snackbar
            snackBarMakeC(
                "Could not connect to host",
                actionText = "Retry",
                block = { BTconnect() }
            )
            return@launch
        }
        launch(Dispatchers.Main) {
            SnackbarWrapper.make(applicationContext, "Connected", Snackbar.LENGTH_LONG).show()
            initReceiver()
            connectionListener?.onConnected()
        }
        // launch loop in the same thread
        launch(btDispatcher) {
            while (true) {
                try {
                    if (inputStream!!.available() > 0) {
                        channel.send(inputStream!!.bufferedReader().readLine())
                    } else delay(1)
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }
        return@launch
    }

    /* Call this from the main activity to send data to the remote device */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun write(bytes: ByteArray) = GlobalScope.launch(btDispatcher) {
        try {
            outputStream?.write(bytes + '\n'.toByte()) ?: throw IOException("not Connected")
        } catch (e: IOException) {
            Log.e("Send Error", "Unable to send message", e)
            showToastC("Unable to send message")
        }
    }

    @JvmName("writeVarArgs")
    fun write(vararg elements: Byte) = write(elements)

    fun write(input: String) = write(input.toByteArray())

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
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                GlobalScope.launch(Dispatchers.Main) {
                    connectionListener?.onDisconnected()
                    snackBarMakeC(
                            "Lost bluetooth connection with ${device?.name}",
                            actionText = "Retry",
                            block = { BTconnect() }
                    )
                }
            }
        }
    }

    val btApi: BtApi by lazy { BtApi() }

    // TODO finish API
    inner class BtApi {
        fun changeColor(red: Byte, green: Byte, blue: Byte, alpha: Byte) =
                write(CHANGE_COLOR, red, green, blue, alpha)
        @ExperimentalUnsignedTypes
        // UInt size is 32-bit, In C lang unsigned long is 32-bit
        fun changePowerInterval(interval: UInt) = write(CHANGE_POWER_INTERVAL, *interval.toByteArray())
        fun enableRandomColorContinuous() = write(ENABLE_RANDOM_COLOR, 0.toByte())
        fun enableRandomColor() = write(ENABLE_RANDOM_COLOR, 1.toByte())
        fun enableRandomColor2() = write(ENABLE_RANDOM_COLOR, 2.toByte())
        fun disableRandomColor() = write(DISABLE_RANDOM_COLOR)
        fun submitColorSequence(vararg colors: Byte) = write(SUBMIT_COLOR_SEQUENCE, *colors)
        fun enableLight() = write(ENABLE_LIGHT)
        fun disableLight() = write(DISABLE_LIGHT)
        fun enablePump() = write(ENABLE_PUMP)
        fun disablePump() = write(DISABLE_PUMP)
        fun enableSine() = write(ENABLE_SINE)
        fun enableCosine() = write(ENABLE_COSINE)
        fun enableTangent() = write(ENABLE_TANGENT)
        fun enableSquare() = write(ENABLE_SQUARE)
        fun enableTriangle() = write(ENABLE_TRIANGLE)
    }
}

// Bluetooth codes
private const val CHANGE_COLOR = 'c'.toByte()
private const val CHANGE_POWER_INTERVAL = 'i'.toByte()
private const val ENABLE_RANDOM_COLOR = 'R'.toByte()
private const val DISABLE_RANDOM_COLOR = 'r'.toByte()
private const val SUBMIT_COLOR_SEQUENCE = 's'.toByte()
private const val ENABLE_LIGHT = 'L'.toByte()
private const val DISABLE_LIGHT = 'l'.toByte()
private const val ENABLE_PUMP = 'P'.toByte()
private const val DISABLE_PUMP = 'p'.toByte()
private const val ENABLE_SINE = '1'.toByte()
private const val ENABLE_COSINE = '2'.toByte()
private const val ENABLE_TANGENT = '3'.toByte()
private const val ENABLE_SQUARE = '4'.toByte()
private const val ENABLE_TRIANGLE = '5'.toByte()

const val SINE = "Sine"
const val COSINE = "Cosine"
const val TANGENT = "Tangent"
const val SQUARE = "Square"
const val TRIANGLE = "Triangle"

val periodicFunNames = listOf(SINE, COSINE, TANGENT, SQUARE, TRIANGLE)

const val RANDOM_COLOR_0 = "Continuous change"
const val RANDOM_COLOR_1 = "Change on T"
const val RANDOM_COLOR_2 = "Change on T/2"

val RANDOM_MODES = listOf(RANDOM_COLOR_0, RANDOM_COLOR_1, RANDOM_COLOR_2)