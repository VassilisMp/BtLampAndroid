package gr.cs.btlamp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.larswerkman.holocolorpicker.ColorPicker
import com.larswerkman.holocolorpicker.OpacityBar
import com.larswerkman.holocolorpicker.SVBar
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.Executors
import kotlin.properties.Delegates


private const val TAG = "ColorPickerActivity"
private const val REQUEST_ENABLE_BT: Int = 1
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

    private val mBluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    /* service binding code */
    private lateinit var mService: MyBluetoothService
    private var mBound: Boolean = false
    private var channelListenJob: Job? = null

    var btEnabled: Boolean by Delegates.observable(false) {
            prop, old, new ->
        if (new && !mBound) {
            // Bind to LocalService
            Intent(this, MyBluetoothService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
                startService(intent)
            }
        }
    }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyBluetoothService.LocalBinder
            mService = binder.service
            mService.setConnectionListener(object : MyBluetoothService.ConnectionListener {
                override fun onConnected() {
                    channelListenJob = GlobalScope.launch(Dispatchers.IO) {
                        // here we print received values using `for` loop (until the channel is closed)
                        for (string in mService.channel)
                            Log.d(TAG, "Message received: $string")
                        println("Done!")
                    }
                }

                override fun onDisconnected() {
                    channelListenJob?.cancel()
                    channelListenJob = null
                }

            })
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            channelListenJob?.cancel()
            channelListenJob = null
        }
    }

    fun stopService() {
        unbindService(connection)
        mBound = false
    }


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
        btPermission()
    }

    private fun btPermission() {
        if (mBluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        if (mBluetoothAdapter == null) {
            bluetoothNotAvailable()
        } else {
            if (mBluetoothAdapter!!.isEnabled) {
                btEnabled = true
            } else {
                bluetoothDisabled()
            }
        }
    }

    // Device doesn't support Bluetooth
    private fun bluetoothNotAvailable() = showToast("Device doesn't support Bluetooth")

    private fun bluetoothEnabled() = showToast("Bluetooth enabled")

    private fun bluetoothDisabled() = showToast("Bluetooth is disabled")

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
                btEnabled = true
            } else if (resultCode == RESULT_CANCELED) {
                finish()
            }
        }
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
}