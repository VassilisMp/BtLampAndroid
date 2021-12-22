/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gr.cs.btlamp.android.bluetoothchat

import android.bluetooth.BluetoothAdapter
import gr.cs.btlamp.android.bluetoothchat.BluetoothService.AcceptThread
import gr.cs.btlamp.android.bluetoothchat.BluetoothService.ConnectThread
import gr.cs.btlamp.android.bluetoothchat.BluetoothService.ConnectedThread
import kotlin.jvm.Synchronized
import gr.cs.btlamp.android.bluetoothchat.BluetoothService
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.bluetooth.BluetoothServerSocket
import android.content.Context
import android.os.Handler
import gr.cs.btlamp.*
import gr.cs.btlamp.android.common.logger.Log
import gr.cs.btlamp.ui.schedule.ScheduleActivity
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
class BluetoothService(handler: Handler? = null) {
    // Member fields
    private val mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var mHandler: Handler?
    private var mSecureAcceptThread: AcceptThread? = null
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    /**
     * Return the current connection state.
     */
    @get:Synchronized
    var state: Int
        private set
    private var mNewState: Int

    /**
     * Update UI title according to the current state of the chat connection
     */
    @Synchronized
    private fun updateUserInterfaceTitle() {
        state = state
        Log.d(TAG, "updateUserInterfaceTitle() $mNewState -> $state")
        mNewState = state

        // Give the new state to the Handler so the UI Activity can update
        mHandler?.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1)?.sendToTarget()
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    @Synchronized
    fun start() {
        Log.d(TAG, "start")

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = AcceptThread(true)
            mSecureAcceptThread!!.start()
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = AcceptThread(false)
            mInsecureAcceptThread!!.start()
        }
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    @Synchronized
    fun connect(device: BluetoothDevice, secure: Boolean) {
        Log.d(TAG, "connect to: $device")

        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device, secure)
        mConnectThread!!.start()
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    @Synchronized
    fun connected(socket: BluetoothSocket?, device: BluetoothDevice, socketType: String) {
        Log.d(TAG, "connected, Socket Type:$socketType")

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread!!.cancel()
            mInsecureAcceptThread = null
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread!!.start()

        // Send the name of the connected device back to the UI Activity
        mHandler?.obtainMessage(Constants.MESSAGE_DEVICE_NAME)?.let { msg ->
            val bundle = Bundle()
            bundle.putString(Constants.DEVICE_NAME, device.name)
            msg.data = bundle
            mHandler!!.sendMessage(msg)
        }
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Stop all threads
     */
    @Synchronized
    fun stop() {
        Log.d(TAG, "stop")
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread!!.cancel()
            mInsecureAcceptThread = null
        }
        state = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread.write
     */
    fun write(out: ByteArray): ByteArray? {
        // Create temporary object
        var r: ConnectedThread?
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (state != STATE_CONNECTED) return out
            r = mConnectedThread
        }
        // Perform the write unsynchronized
        r!!.write(out)
        return null
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        // Send a failure message back to the Activity
        mHandler?.obtainMessage(Constants.MESSAGE_TOAST)?.let { msg ->
            val bundle = Bundle()
            bundle.putString(Constants.TOAST, "Unable to connect device")
            msg.data = bundle
            mHandler!!.sendMessage(msg)
        }
        state = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        start()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        // Send a failure message back to the Activity
        mHandler?.obtainMessage(Constants.MESSAGE_TOAST)?.let { msg ->
            val bundle = Bundle()
            bundle.putString(Constants.TOAST, "Device connection was lost")
            msg.data = bundle
            mHandler!!.sendMessage(msg)
        }
        state = STATE_NONE
        // Update UI title
        updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        start()
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private inner class AcceptThread(secure: Boolean) : Thread() {
        // The local server socket
        private val mmServerSocket: BluetoothServerSocket?
        private val mSocketType: String
        override fun run() {
            Log.d(
                TAG, "Socket Type: " + mSocketType +
                        "BEGIN mAcceptThread" + this
            )
            name = "AcceptThread$mSocketType"
            var socket: BluetoothSocket?

            // Listen to the server socket if we're not connected
            while (this@BluetoothService.state != STATE_CONNECTED) {
                socket = try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmServerSocket!!.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e)
                    break
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized(this@BluetoothService) {
                        when (this@BluetoothService.state) {
                            STATE_LISTEN, STATE_CONNECTING ->                                 // Situation normal. Start the connected thread.
                                connected(
                                    socket, socket.remoteDevice,
                                    mSocketType
                                )
                            STATE_NONE, STATE_CONNECTED ->                                 // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    Log.e(TAG, "Could not close unwanted socket", e)
                                }
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: $mSocketType")
        }

        fun cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this)
            try {
                mmServerSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e)
            }
        }

        init {
            var tmp: BluetoothServerSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Create a new listening server socket
            try {
                tmp = if (secure) {
                    mAdapter.listenUsingRfcommWithServiceRecord(
                        NAME_SECURE,
                        MY_UUID_SECURE
                    )
                } else {
                    mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        NAME_INSECURE, MY_UUID_INSECURE
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e)
            }
            mmServerSocket = tmp
            this@BluetoothService.state = STATE_LISTEN
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice, secure: Boolean) :
        Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String
        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:$mSocketType")
            name = "ConnectThread$mSocketType"

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket!!.connect()
            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    Log.e(
                        TAG, "unable to close() " + mSocketType +
                                " socket during connection failure", e2
                    )
                }
                connectionFailed()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this@BluetoothService) { mConnectThread = null }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType)
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect $mSocketType socket failed", e)
            }
        }

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = if (secure) {
                    mmDevice.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE
                    )
                } else {
                    mmDevice.createInsecureRfcommSocketToServiceRecord(
                        MY_UUID_INSECURE
                    )
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e)
            }
            mmSocket = tmp
            this@BluetoothService.state = STATE_CONNECTING
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(socket: BluetoothSocket?, socketType: String) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?
        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (this@BluetoothService.state == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream!!.read(buffer)

                    // Send the obtained bytes to the UI Activity
                    mHandler?.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                        ?.sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        fun write(buffer: ByteArray?) {
            try {
                mmOutStream!!.write(buffer)

                // Share the sent message back to the UI Activity
                mHandler?.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                    ?.sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }

        init {
            Log.d(TAG, "create ConnectedThread: $socketType")
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket!!.inputStream
                tmpOut = socket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }
            mmInStream = tmpIn
            mmOutStream = tmpOut
            this@BluetoothService.state = STATE_CONNECTED
        }
    }

    companion object {
        // Debugging
        private const val TAG = "BluetoothService"

        // Name for the SDP record when creating server socket
        private const val NAME_SECURE = "BluetoothChatSecure"
        private const val NAME_INSECURE = "BluetoothChatInsecure"

        // Unique UUID for this application
        private val MY_UUID_SECURE =
            //            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        private val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

        // Constants that indicate the current connection state
        const val STATE_NONE = 0 // we're doing nothing
        const val STATE_LISTEN = 1 // now listening for incoming connections
        const val STATE_CONNECTING = 2 // now initiating an outgoing connection
        const val STATE_CONNECTED = 3 // now connected to a remote device

        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: BluetoothService? = null

        fun getService(): BluetoothService {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = BluetoothService()
                INSTANCE = instance
                // return instance
                instance
            }
        }

        fun getService(handler: Handler) = getService().also { it.mHandler = handler }
    }

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    init {
        state = STATE_NONE
        mNewState = state
        mHandler = handler
    }

    @JvmName("writeVarArgs")
    fun write(vararg elements: Byte) = write(elements  + '\n'.toByte())

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