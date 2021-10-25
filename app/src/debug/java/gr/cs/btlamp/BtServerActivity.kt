package gr.cs.btlamp

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.Executors

private const val TAG = "BtClientActivity"
private const val ACCESS_BACKGROUND_LOCATION_PERMISSION_CODE = 1
private const val REQUEST_ENABLE_BT = 2
private const val NAME = "ArduinoLamp"
class BtServerActivity : AppCompatActivity() {

    val readWriteTextview: TextView by lazy{ findViewById(R.id.r_t_test) }
    val bytesSizeTextview: TextView by lazy{ findViewById(R.id.bytes_size_text) }
    private val pairedDeviceListView: ListView by lazy{ findViewById(R.id.paired_devices) }
    val sendButton: Button by lazy { findViewById(R.id.send_button) }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bt_server)
        checkPermissions()

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        pairedDeviceListView.run {
            val devicesArray: Array<String>? = bluetoothAdapter?.bondedDevices?.map { "${it.name} : ${it.address}" }?.toTypedArray()
            if (devicesArray != null) adapter = ArrayAdapter(this.context, R.layout.activity_listview, devicesArray)
        }

        BluetoothServer().waitForConnections()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(baseContext,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    ACCESS_BACKGROUND_LOCATION_PERMISSION_CODE)
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private inner class BluetoothServer {

        private val btThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            BluetoothAdapter.getDefaultAdapter()?.listenUsingRfcommWithServiceRecord(NAME, PORT_UUID)
        }

        private var connectedSocket: BluetoothSocket? = null

        fun waitForConnections() = GlobalScope.launch(btThread) {
            // Keep listening until exception occurs or a socket is returned.
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    Log.d(TAG, "before mmServerSocket?.accept()")
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    showToastC("Socket's accept() method failed")
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
//                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() = GlobalScope.launch(btThread) {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }

        fun manageMyConnectedSocket(socket: BluetoothSocket) {
            connectedSocket = socket
            showToast("manage socket")
            val outputStream = connectedSocket?.outputStream ?: return
            // send button set onClick()
            sendButton.setOnClickListener {
                showToast("sendButton onClick")
                GlobalScope.launch(btThread) {
                    outputStream.write((readWriteTextview.text.toString() + '\n').toByteArray())
                }
            }

            GlobalScope.launch(btThread) {
                val inputStream = connectedSocket?.inputStream ?: return@launch
                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    // Read from the InputStream.
                    try {
                        if (inputStream.available() > 0) {
                            /*val read = inputStream.bufferedReader().readLine()
                            lifecycleScope.launch {
                                readWriteTextview.text = read
                                bytesSizeTextview.text = read.length.toString()
                            }*/
                            val charArray = arrayListOf<Char>().apply {
                                while (inputStream.available() > 0) {
                                    val byte = inputStream.read().toChar()
                                    if (byte == '\n') break
                                    add(byte)
                                }
                            }.toCharArray()
                            lifecycleScope.launch {
                                readWriteTextview.text = charArray.joinToString()
                                bytesSizeTextview.text = charArray.size.toString()
                            }
                        } else delay(1)
                    } catch (e: IOException) {
                        Log.d(TAG, "Input stream was disconnected", e)
                        showToastC("Input stream was disconnected")
                        waitForConnections()
                        break
                    }
                }
            }
        }

    }
}