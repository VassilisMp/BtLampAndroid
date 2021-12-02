
package gr.cs.btlamp.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.larswerkman.holocolorpicker.ColorPicker
import gr.cs.btlamp.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*


private const val TAG = "MainActivity"
private const val REQUEST_ENABLE_BT: Int = 1
internal const val REQUEST_COLOR_SEQUENCE: Int = 2
internal const val SEQUENCE: String = "sequence"
@ExperimentalUnsignedTypes
@Deprecated("")
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
                    mBound = true
                }

                override fun onDisconnected() {
                    channelListenJob?.cancel()
                    channelListenJob = null
                    mBound = false
                }

            })
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            channelListenJob?.cancel()
            channelListenJob = null
        }
    }

    private fun bindService() {
        /*Intent(this@MainActivity, MyBluetoothService::class.java).run {
            startService(this)
        }*/
        // Bind to LocalService
        Intent(this@MainActivity, MyBluetoothService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStart() {
        super.onStart()
        btPermission()
        bindService()
    }

    private fun stopService() {
        unbindService(connection)
        mBound = false
    }

    override fun onStop() {
        super.onStop()
        stopService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter(this, android.R.layout.simple_spinner_item, periodicFunNames).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner_periodic_fun.adapter = adapter
            spinner_periodic_fun.setSelection(adapter.getPosition(SQUARE))
            spinner_periodic_fun.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    when(parent.getItemAtPosition(pos).toString()) {
                        SINE -> mService?.btApi?.enableSine()
                        COSINE -> mService?.btApi?.enableCosine()
                        TANGENT -> mService?.btApi?.enableTangent()
                        SQUARE -> mService?.btApi?.enableSquare()
                        TRIANGLE -> mService?.btApi?.enableTriangle()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
        ArrayAdapter(this, android.R.layout.simple_spinner_item, RANDOM_MODES).run {
            // Specify the layout to use when the list of choices appears
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner_random_modes.adapter = this
            spinner_random_modes.setSelection(this.getPosition(RANDOM_COLOR_1))
            spinner_random_modes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    when(parent.getItemAtPosition(pos).toString()) {
                        RANDOM_COLOR_0 -> mService?.btApi?.enableRandomColorContinuous()
                        RANDOM_COLOR_1 -> mService?.btApi?.enableRandomColor()
                        RANDOM_COLOR_2 -> mService?.btApi?.enableRandomColor2()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
        on_off_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newProgress = if (progress > 0) {
                    spinner_periodic_fun.visibility = View.VISIBLE
                    progress + 80
                } else {
                    spinner_periodic_fun.visibility = View.GONE
                    0
                }
                mService?.btApi?.changePowerInterval(newProgress.toUInt())
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
        left_time_btn.setOnClickListener {
            // TODO("show activity for scheduling")
        }
        schedule_btn.setOnClickListener {
            // TODO("show activity for music")
        }
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
                bluetoothEnabled()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, this::onActivityResult.name)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                when (resultCode) {
//                    RESULT_OK ->
                    RESULT_CANCELED -> finish()
                }
            }
            REQUEST_COLOR_SEQUENCE -> {
                Log.d(TAG, "REQUEST_COLOR_SEQUENCE")
                colorSequence = data?.getIntArrayExtra(SEQUENCE)
                colorSequence?.forEach {
                    Log.d(TAG, "$it")
                }
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
                    spinner_random_modes.run {
                        // reselect the selected item to run the proper btApi function
                        onItemSelectedListener?.onItemSelected(this, selectedView, selectedItemPosition, selectedItemId)
                        visibility = View.VISIBLE
                    }
                } else {
                    spinner_random_modes.visibility = View.GONE
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