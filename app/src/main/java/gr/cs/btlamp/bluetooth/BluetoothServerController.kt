package gr.cs.btlamp.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import gr.cs.btlamp.ColorPickerActivity
import java.io.IOException
import java.util.*

val uuid: UUID = UUID.fromString("8989063a-c9af-463a-b3f1-f21d9b2b827b")
class BluetoothServerController(private val activity: ColorPickerActivity) : Thread() {
    private var cancelled: Boolean
    private val serverSocket: BluetoothServerSocket?

    init {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null) {
            // Create a server socket, identified by the uuid, in the class constructor
            this.serverSocket = btAdapter.listenUsingRfcommWithServiceRecord("test", uuid)
            this.cancelled = false
        } else {
            this.serverSocket = null
            this.cancelled = true
        }

    }

    override fun run() {
        var socket: BluetoothSocket

        while(true) {
            if (this.cancelled) {
                break
            }

            try {
                // Once thread execution started wait for the client connections using accept() method
                socket = serverSocket!!.accept()
            } catch(e: IOException) {
                break
            }

            if (!this.cancelled && socket != null) {
                Log.i("server", "Connecting")
                // Once client established connection accept() method returns a BluetoothSocket reference that gives access to the input and output streams. We use this socket to start the Server thread.
                BluetoothServer(this.activity, socket).start()
            }
        }
    }

    fun cancel() {
        this.cancelled = true
        this.serverSocket!!.close()
    }
}