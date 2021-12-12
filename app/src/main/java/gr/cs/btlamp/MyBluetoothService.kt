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
import android.view.View
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import gr.cs.btlamp.ui.schedule.ScheduleActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.Executors


private const val TAG = "MyBluetoothService"
//private const val btDeviceName = "HUAWEI P8 lite"
private const val btDeviceName = "HC-06"
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
    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    val channel: Channel<String> by lazy { Channel() }
    private var started = false
    private var scope: CoroutineScope = CoroutineScope(btDispatcher)
        get() {
            if (!field.isActive) {
                field = CoroutineScope(btDispatcher)
            }
            return field
        }
    private var bound = false
    private var destroyTimer: Job? = null
    var boundActivity: AppCompatActivity? = null

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
        /*destroyTimer?.cancel()
        if (!started) {
            // start Bluetooth service
            BTconnect()
            started = true
        }*/
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        showToast("Service Destroyed")
        Log.d(TAG, "onDestroy")
        // TODO was using unregisterReceiver on TabbedActivity, but crashes after inserting
        //  schedule classes
//        unregisterReceiver(mReceiver)
        scope.cancel()
        socket?.close()
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "OnBind")
        destroyTimer?.cancel()
        if (!started) {
            // start Bluetooth service
            BTconnect()
            started = true
        }
        bound = true
        return mBinder
    }

    // bluetooth connection function
    @Suppress("BlockingMethodInNonBlockingContext")
    fun BTconnect(): Job = scope.launch(btDispatcher) {
        if (bluetoothAdapter?.state != BluetoothAdapter.STATE_ON) {
            mBinder.view?.snackBarMake(
                    "Bluetooth is off, turn on and retry.",
                    actionText = "Retry",
                    block = { BTconnect() }
            )?: showToast("Bluetooth is off, turn on and retry.")
            return@launch
        }
        device = bluetoothAdapter?.bondedDevices?.firstOrNull { it.name == btDeviceName }?.apply {
            bluetoothAdapter?.cancelDiscovery()
            showToastC("Paired with $btDeviceName")
        } ?: mBinder.view?.snackBarMake(
                    "Not paired with device, pair with $btDeviceName first and retry.",
                    actionText = "Retry",
                    block = { BTconnect() }
            ).run { return@launch }
        try {
            socket = device?.createRfcommSocketToServiceRecord(PORT_UUID)
            socket?.connect() ?: throw IOException("Couldn't connect")
            inputStream = socket!!.inputStream
            outputStream = socket!!.outputStream
        } catch (e: IOException) {
            e.printStackTrace()
            // show reconnect snackbar
            mBinder.view?.snackBarMake(
                "Could not connect to host",
                actionText = "Retry",
                block = { BTconnect() }
            )
            return@launch
        }
        launch(Dispatchers.Main) {
            mBinder.view?.let { Snackbar.make(it, "Connected", Snackbar.LENGTH_LONG).show() }
            initReceiver()
            connectionListener?.onConnected()
        }
        // launch loop in the same thread
        scope.launch(btDispatcher) {
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
    fun write(bytes: ByteArray) = scope.launch(btDispatcher) {
        try {
            outputStream?.write(bytes + '\n'.toByte()) ?: throw IOException("not Connected")
        } catch (e: IOException) {
            Log.e("Send Error", "Unable to send message", e)
            showToastC("Unable to send message")
            cancel("send error", e)
        }
    }

    @JvmName("writeVarArgs")
    fun write(vararg elements: Byte) = write(elements)

    fun write(input: String) = write(input.toByteArray())

    private fun initReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        this@MyBluetoothService.registerReceiver(mReceiver, filter)
    }

    /*This method is used to check if bluetooth is disconnected. If disconnected try to reconnect automatically*/
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    GlobalScope.launch(Dispatchers.Main) {
                        connectionListener?.onDisconnected()
                        val binder = mBinder as MyBluetoothService.LocalBinder
                        mBinder.view?.snackBarMake(
                                "Lost bluetooth connection with ${device?.name}",
                                actionText = "Retry",
                                block = { BTconnect() }
                        )
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            showToast("Bluetooth off")
                            scope.cancel()
                            socket?.close()
                            BTconnect()
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> showToast("Turning Bluetooth off...")
                        BluetoothAdapter.STATE_ON -> showToast("Bluetooth on")
                        BluetoothAdapter.STATE_TURNING_ON -> showToast("Turning Bluetooth on...")
                    }
                }
            }
        }
    }

    val btApi: BtApi by lazy { BtApi() }

    // TODO finish API
    inner class BtApi {
        fun changeColor(red: Byte, green: Byte, blue: Byte, alpha: Byte) =
                write(CHANGE_COLOR, red, green, blue, alpha)
        fun changeColor(color: Int) =
                write(CHANGE_COLOR, *color.toByteArray())
        @ExperimentalUnsignedTypes
        // UInt size is 32-bit, In C lang unsigned long is 32-bit
        fun changePowerInterval(interval: UInt) = write(CHANGE_POWER_INTERVAL, *interval.toByteArray())
        fun changePowerInterval(interval: Int) = write(CHANGE_POWER_INTERVAL, *interval.toByteArray())
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
        fun enableTimer(hours: Byte, mins: Byte) = write(ENABLE_TIMER, hours, mins)
        fun disableTimer() = write(DISABLE_TIMER)
        fun addSchedule(schedule: ScheduleActivity.Schedule) = with(schedule) {
            write(
                ADD_SCHEDULE,
                switch.toByte(),
                getHour().toByte(),
                getMinute().toByte(),
                *daysToByteArray(),
                *id.encodeToByteArray()
            ) }
        fun removeSchedule(schedule: ScheduleActivity.Schedule) =
            write(REMOVE_SCHEDULE, *schedule.id.encodeToByteArray())
        fun changeSchedule(schedule: ScheduleActivity.Schedule) {
            // TODO can make it more efficient by replacing only the changed fields
            removeSchedule(schedule)
            addSchedule(schedule)
        }
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
private const val ENABLE_TIMER = 'T'.toByte()
private const val DISABLE_TIMER = 't'.toByte()
private const val ADD_SCHEDULE = 'A'.toByte()
private const val REMOVE_SCHEDULE = 'a'.toByte()

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