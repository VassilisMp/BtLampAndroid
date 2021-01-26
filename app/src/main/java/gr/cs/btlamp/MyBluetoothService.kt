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
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.Nullable
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
private val PORT_UUID =
        UUID.fromString("00001101-0000-1000-8000-00805f9b34fb") //Serial Port Service ID

class MyBluetoothService : Service() {

    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var device: BluetoothDevice? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(this, "The new Service was Created", Toast.LENGTH_LONG).show()
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        this.registerReceiver(mReceiver, filter)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // For time consuming an long tasks you can launch a new thread here...
        // Do your Bluetooth Work Here
        Toast.makeText(this, " Service Started", Toast.LENGTH_LONG).show()
        device = BTinit()
        BTconnect()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
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
                outputStream = socket?.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
//            beginListenForData()
            val channel = Channel<String>()
            val bufferedReader = inputStream!!.bufferedReader()
            val readDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            GlobalScope.launch(Dispatchers.Main) {
                while (true) {
                    withContext(readDispatcher) {
                        val message = bufferedReader.readLine()
                        channel.send(message)
                    }
                }
            }
            SnackbarWrapper.make(applicationContext, "Connected", Snackbar.LENGTH_LONG)
                    .show()
        } else {
            val connectionSnackbar = SnackbarWrapper.make(
                    applicationContext,
                    "Could not connect to host",
                    Snackbar.LENGTH_INDEFINITE
            )
            connectionSnackbar.setAction(
                    "Retry"
            ) {
                BTconnect()
            }
            connectionSnackbar.show()
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
}
