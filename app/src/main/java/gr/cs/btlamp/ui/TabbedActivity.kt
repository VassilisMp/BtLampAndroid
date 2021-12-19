package gr.cs.btlamp.ui

import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.os.*
import android.util.Log
import android.widget.Switch
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import gr.cs.btlamp.*
import gr.cs.btlamp.android.bluetoothchat.BluetoothService
import gr.cs.btlamp.android.bluetoothchat.BluetoothService.Companion.STATE_CONNECTED
import gr.cs.btlamp.android.bluetoothchat.Constants
import gr.cs.btlamp.android.common.logger.LogFragment
import gr.cs.btlamp.android.common.logger.LogWrapper
import gr.cs.btlamp.android.common.logger.MessageOnlyLogFilter
import gr.cs.btlamp.customViews.TimePickerDialogCustom
import gr.cs.btlamp.ui.schedule.ScheduleActivity
import gr.cs.btlamp.ui.tabbed.SectionsPagerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_tabbed.*
import kotlinx.android.synthetic.main.activity_tabbed.left_time_btn
import kotlinx.android.synthetic.main.activity_tabbed.schedule_btn
import kotlinx.android.synthetic.main.activity_tabbed.switchButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.*
import kotlin.properties.Delegates

private const val TAG = "TabbedActivity"
private const val REQUEST_ENABLE_BT: Int = 1

class TabbedActivity : AppCompatActivity() {

    private val mBluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    private var timerSwitch: Switch? = null
    private var timer: CountDownTimer? = null
    private var timeRemaining: Pair<Int, Int>? by Delegates.observable(null) { _, _, newValue ->
        if (newValue != null && timePicker.isShowing)
            timePicker.updateTime(newValue.first, newValue.second)
    }

    // listener which is triggered when the
    // time is picked from the time picker dialog
    private lateinit var timePicker: TimePickerDialogCustom

    /**
     * Member object for the bluetooth service
     */
    internal var mService: BluetoothService? = null

    private val btPermResultLauncher = registerForActivityResult(
        ActivityResultContracts
            .StartActivityForResult()
    ) { result ->
        Log.d(TAG, "BtPermissionLauncher")
        when (result.resultCode) {
            RESULT_OK -> {
                mService = BluetoothService(this, mHandler)
                connectDevice()
            }
            RESULT_CANCELED -> {
//            finish()
                // User did not enable Bluetooth or an error occurred
                gr.cs.btlamp.android.common.logger.Log.d(TAG, "BT not enabled")
                Toast.makeText(
                    this, R.string.bt_not_enabled_leaving,
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    /*private fun btPermission() {
        if (mBluetoothAdapter?.isEnabled == false)
            btPermResultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        if (mBluetoothAdapter == null) {
            bluetoothNotAvailable()
        } else {
            if (mBluetoothAdapter!!.isEnabled) {
                bluetoothEnabled()
            } else {
                bluetoothDisabled()
            }
        }
    }*/

    override fun onStart() {
        super.onStart()
        initializeLogging()
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter!!.isEnabled) {
            btPermResultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            // Otherwise, setup the chat session
        } else if (mService == null) {
            mService = BluetoothService(this, mHandler)
            connectDevice()
        } else if (mService!!.state != STATE_CONNECTED) {

            connectDevice()
        }
//        btPermission()
    }

    override fun onStop() {
        super.onStop()
        mService?.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabbed)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            finish()
        }
//        lifecycleScope.launchWhenStarted { btPermission() }
        switchButton.setOnClickListener {
            if (switchButton.isChecked) {
                GlobalScope.launch(Dispatchers.IO) { mService?.btApi?.enableLight() }
            } else
                GlobalScope.launch(Dispatchers.IO) { mService?.btApi?.disableLight() }
        }
        timePicker = object : TimePickerDialogCustom(
            this@TabbedActivity,
            // listener to perform task
            // when time is picked
            object : OnTimeSetListener {
                override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
                    timeRemaining = hourOfDay to minute
                }
            },
            // default hour when the time picker dialog is opened
            0,
            // default minute when the time picker dialog is opened
            0,
            // 24 hours time picker is true
            true
        ) {
            override fun onClick(dialog: DialogInterface, which: Int) {
                super.onClick(dialog, which)
                if (which == BUTTON_POSITIVE && timerSwitch!!.isChecked) {
                    mService?.btApi?.enableTimer(
                        timeRemaining!!.first.toByte(),
                        timeRemaining!!.second.toByte()
                    )?.let {
                        timer = object : CountDownTimer(
                            timeToMillis(
                                timeRemaining!!.first,
                                timeRemaining!!.second
                            ), 60000
                        ) {
                            override fun onTick(millisUntilFinished: Long) {
                                timeRemaining = millisToTime(millisUntilFinished)
                            }

                            override fun onFinish() {
                                timerSwitch?.isChecked = false
                            }
                        }.start()
                    }
                } else if (which == BUTTON_NEGATIVE && timerSwitch!!.isChecked) {
                    if (timer == null) timerSwitch!!.toggle()
                }
            }
        }
        left_time_btn.setOnClickListener {
            // dialog show the dialog to user
            timePicker.show()
            timerSwitch = timePicker.findViewById(R.id.timer_switch)
            if (timer == null && timerSwitch!!.isChecked) timerSwitch!!.isChecked = false
            val timePickerView = timePicker.findViewById<TimePicker>(R.id.timePicker)
            // initialize as disabled, it's needed only on the first dialog open
            if (!timerSwitch!!.isChecked) timePickerView.isEnabled = false
            // set timerSwitch listener if it hasn't been set yet
            if (!timerSwitch!!.hasOnClickListeners()) {
                timerSwitch!!.setOnCheckedChangeListener { _, isChecked ->
                    when (isChecked) {
                        true -> timePickerView.enable()
                        false -> {
                            if (timer != null) {
                                timer!!.cancel()
                                timer = null
                                mService?.btApi?.disableTimer()
                            }
                            timePickerView.disable()
                            timePicker.updateTime(0, 0)
                        }
                    }
                }
            }
        }
        schedule_btn.setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mService!!.state == BluetoothService.STATE_NONE) {
                // Start the Bluetooth services
                mService!!.start()
            }
        }
    }

    // Device doesn't support Bluetooth
    private fun bluetoothNotAvailable() = showToast("Device doesn't support Bluetooth")

    private fun bluetoothEnabled() = showToast("Bluetooth enabled")

    private fun bluetoothDisabled() {
        showToast("Bluetooth is disabled, please enable first.")
//        btPermission()
    }

    /**
     * Establish connection with other device
     *
     * @param data   An [Intent] with [DeviceListActivity.EXTRA_DEVICE_ADDRESS] extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private fun connectDevice(
        btDeviceName: String = "HC-06", /*data: Intent, */secure: Boolean =
            true
    ) {
        // Get the device MAC address
//        val extras = data.extras ?: return
//        val address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        // Get the BluetoothDevice object
//        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        val device =
            mBluetoothAdapter?.bondedDevices?.firstOrNull { it.name == btDeviceName }?.apply {
                mBluetoothAdapter?.cancelDiscovery()
//            showToastC("Paired with ${gr.cs.btlamp.btDeviceName}")
            }
        if (device == null) {
            lifecycleScope.launch {
                view?.snackBarMake(
                    "Not paired with device, pair with $btDeviceName first and retry.",
                    actionText = "Retry",
                    block = { connectDevice() }
                )
            }
        } else {
            // Attempt to connect to the device
            mService?.connect(device, secure)
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private val mHandler: Handler = HandlerImpl(this)

    internal class HandlerImpl(mService: TabbedActivity) : Handler(Looper.getMainLooper()) {
        private val mService: WeakReference<TabbedActivity> = WeakReference(mService)
        override fun handleMessage(msg: Message) {
            val service = mService.get() ?: return
            val activity = service
            when (msg.what) {
                Constants.MESSAGE_STATE_CHANGE -> when (msg.arg1) {
                    BluetoothService.STATE_CONNECTED -> {}
                    BluetoothService.STATE_CONNECTING -> {}
                    BluetoothService.STATE_LISTEN, BluetoothService.STATE_NONE -> { /* state note
                     connected */
                    }
                }
                Constants.MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    // construct a string from the buffer
                    val writeMessage = String(writeBuf)
                }
                Constants.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer
                    val readMessage = String(readBuf, 0, msg.arg1)
                }
                Constants.MESSAGE_DEVICE_NAME -> {
                    // save the connected device's name
                    val mConnectedDeviceName = msg.data.getString(Constants.DEVICE_NAME)
                    if (null != activity) {
                        Toast.makeText(
                            activity, "Connected to "
                                    + mConnectedDeviceName, Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                Constants.MESSAGE_TOAST -> if (null != activity) {
                    Toast.makeText(
                        activity, msg.data.getString(Constants.TOAST),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    /**
     * Create a chain of targets that will receive log data
     */
    private fun initializeLogging() {
        // Wraps Android's native log framework.
        val logWrapper = LogWrapper()
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        gr.cs.btlamp.android.common.logger.Log.setLogNode(logWrapper)

        // Filter strips out everything except the message text.
        val msgFilter = MessageOnlyLogFilter()
        logWrapper.next = msgFilter

        // TODO
        // On screen logging via a fragment with a TextView.
        /*val logFragment = supportFragmentManager
            .findFragmentById(R.id.log_fragment) as LogFragment?
        msgFilter.next = logFragment!!.logView*/
        gr.cs.btlamp.android.common.logger.Log.i(TAG, "Ready")
    }
}