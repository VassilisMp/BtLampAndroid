
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
import androidx.appcompat.app.AppCompatActivity
import com.larswerkman.holocolorpicker.ColorPicker
import gr.cs.btlamp.MyBluetoothService
import gr.cs.btlamp.R
import gr.cs.btlamp.showToast
import gr.cs.btlamp.showToastC
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.properties.Delegates


private const val TAG = "MainActivity"
private const val REQUEST_ENABLE_BT: Int = 1
internal const val REQUEST_COLOR_SEQUENCE: Int = 2
internal const val SEQUENCE: String = "sequence"
class MainActivity : AppCompatActivity(), ColorPicker.OnColorChangedListener, View.OnClickListener {

    private lateinit var nestedLinLt: LinearLayout
    private lateinit var btn: Button
    var initialHeight = 0
    var actualHeight = 0

    private val mBluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    /* service binding code */
    private var mService: MyBluetoothService? = null
    private var mBound: Boolean = false
    private var channelListenJob: Job? = null
    private val power: Boolean
        get() = switchButton.isChecked

    private var btEnabled: Boolean by Delegates.observable(false) { prop, old, new ->
        if (new && !mBound) {
            // Bind to LocalService
            GlobalScope.launch(Dispatchers.Default) {
                Intent(this@MainActivity, MyBluetoothService::class.java).also { intent ->
                    bindService(intent, connection, Context.BIND_AUTO_CREATE)
                    startService(intent)
                }
            }
        }
    }

    private var colorSequence: IntArray? = null

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
                        for (string in mService!!.channel) {
                            Log.d(TAG, "Message received: $string")
                            showToastC("Message received: $string")
                        }
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

    @ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        on_off_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mService?.btApi?.changePowerInterval(progress.toUInt())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        picker.run {
            addSVBar(svbar)
            addOpacityBar(opacitybar)
            onColorChangedListener = this@MainActivity
            showOldCenterColor = false
        }
        random_color.setOnClickListener(this)
        pick_color_seq.setOnClickListener(this)
        switchButton.setOnClickListener(this)
        schedule_but.setOnClickListener {
            // TODO("show activity for scheduling")
        }
        music_btn.setOnClickListener {
            // TODO("show activity for music")
        }
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
        val red = Color.red(picker.color).toByte()
        val green = Color.green(picker.color).toByte()
        val blue = Color.blue(picker.color).toByte()
        val alpha = Color.alpha(picker.color).toByte()

//        text.setTextColor(picker.color)
        textView_color.text = "%02x%02x%02x%02x".format(red, green, blue, alpha)
        if (power) mService?.btApi?.changeColor(red, green, blue, alpha)
    }

    override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?
    ) {
        Log.d(TAG, this::onActivityResult.name)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                when (resultCode) {
                    RESULT_OK -> btEnabled = true
                    RESULT_CANCELED -> finish()
                }
            }
            REQUEST_COLOR_SEQUENCE -> {
                Log.d(TAG, "REQUEST_COLOR_SEQUENCE")
                colorSequence = data?.getIntArrayExtra(SEQUENCE)
                colorSequence?.forEach {
                    Log.d(TAG, "$it") }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
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

    override fun onClick(view: View?) {
        when(view) {
            random_color -> {
                if (random_color.isChecked) {
                    mService?.btApi?.enableRandomColor()
                } else {
                    mService?.btApi?.disableRandomColor()
                }
            }
            pick_color_seq -> startActivityForResult(
                Intent(this, SequencePickerActivity::class.java),
                REQUEST_COLOR_SEQUENCE
            )
            switchButton -> {
                if (switchButton.isChecked)
                    mService?.btApi?.enableLight()
                else
                    mService?.btApi?.disableLight()
            }
        }
    }
}