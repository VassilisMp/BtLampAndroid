package gr.cs.btlamp.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
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
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.larswerkman.holocolorpicker.ColorPicker
import gr.cs.btlamp.MyBluetoothService
import gr.cs.btlamp.R
import gr.cs.btlamp.showToast
import kotlinx.android.synthetic.main.activity_color_picker.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.properties.Delegates


private const val TAG = "ColorPickerActivity"
private const val REQUEST_ENABLE_BT: Int = 1
class ColorPickerActivity : AppCompatActivity(), ColorPicker.OnColorChangedListener {

    private lateinit var nestedLinLt: LinearLayout
    private lateinit var btn: Button
    var initialHeight = 0
    var actualHeight = 0

    private val mBluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    /* service binding code */
    private var mService: MyBluetoothService? = null
    private var mBound: Boolean = false
    private var channelListenJob: Job? = null

    var btEnabled: Boolean by Delegates.observable(false) {
            prop, old, new ->
        if (new && !mBound) {
            // Bind to LocalService
            GlobalScope.launch(Dispatchers.Default) {
                Intent(this@ColorPickerActivity, MyBluetoothService::class.java).also { intent ->
                    bindService(intent, connection, Context.BIND_AUTO_CREATE)
                    startService(intent)
                }
            }
        }
    }

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyBluetoothService.LocalBinder
            mService = binder.service
            mService?.setConnectionListener(object : MyBluetoothService.ConnectionListener {
                override fun onConnected() {
                    channelListenJob = GlobalScope.launch(Dispatchers.IO) {
                        // here we print received values using `for` loop (until the channel is closed)
                        for (string in mService!!.channel)
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
        on_off_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mService?.btApi?.changePowerInterval(progress.toByte())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        picker.addSVBar(svbar)
        picker.addOpacityBar(opacitybar)
        picker.onColorChangedListener = this@ColorPickerActivity
        random_color.setOnClickListener {
            if ((it as ToggleButton).isChecked) {
                mService?.btApi?.enableRandomColor()
            } else {
                mService?.btApi?.disableRandomColor()
            }
        }
        pick_color_seq.setOnClickListener {
            // TODO("show activity for color")
        }
        switchButton.setOnClickListener {
            if ((it as ToggleButton).isChecked)
                mService?.btApi?.lightOn()
            else
                mService?.btApi?.lightOff()
        }
        schedule_but.setOnClickListener {
            // TODO("show activity for scheduling")
        }
        music_btn.setOnClickListener {
            // TODO("show activity for music")
        }
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
        picker.oldCenterColor = picker.color
        val red = Color.red(picker.color).toByte()
        val green = Color.green(picker.color).toByte()
        val blue = Color.blue(picker.color).toByte()
        val alpha = Color.alpha(picker.color).toByte()

//        text.setTextColor(picker.color)
        textView_color.text = "%02x%02x%02x%02x".format(red, green, blue, alpha)
        mService?.btApi?.changeColor(red, green, blue, alpha)
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