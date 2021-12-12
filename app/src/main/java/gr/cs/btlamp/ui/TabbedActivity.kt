package gr.cs.btlamp.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Switch
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import gr.cs.btlamp.*
import gr.cs.btlamp.android.bluetoothchat.BluetoothChatFragment
import gr.cs.btlamp.android.bluetoothchat.BluetoothService
import gr.cs.btlamp.android.common.logger.LogFragment
import gr.cs.btlamp.android.common.logger.LogWrapper
import gr.cs.btlamp.android.common.logger.MessageOnlyLogFilter
import gr.cs.btlamp.customViews.TimePickerDialogCustom
import gr.cs.btlamp.ui.schedule.ScheduleActivity
import gr.cs.btlamp.ui.tabbed.SectionsPagerAdapter
import kotlinx.android.synthetic.main.activity_tabbed.*
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
    private val mService: BluetoothService? = null

    var btPermResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "BtPermissionLauncher")
        when (result.resultCode) {
            RESULT_OK -> {

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
//                btPermission()
            }
        }
    }

    private fun btPermission() {
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
    }

    override fun onStart() {
        super.onStart()
        initializeLogging()
        if (mBluetoothAdapter == null) return
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothChatFragment)
            startActivityForResult(enableIntent, BluetoothChatFragment.REQUEST_ENABLE_BT)
            // Otherwise, setup the chat session
        } else if (mService == null) {
            setupChat()
        }
    }

    override fun onStop() {
        super.onStop()
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
        lifecycleScope.launchWhenStarted { btPermission() }
        switchButton.setOnClickListener {
            if (switchButton.isChecked)
                mService?.btApi?.enableLight()
            else
                mService?.btApi?.disableLight()
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
                    mService?.btApi?.enableTimer(timeRemaining!!.first.toByte(), timeRemaining!!.second.toByte())?.invokeOnCompletion {
                            if (it == null) {
                                timer = object : CountDownTimer(timeToMillis(timeRemaining!!.first, timeRemaining!!.second), 60000) {
                                    override fun onTick(millisUntilFinished: Long) { timeRemaining = millisToTime(millisUntilFinished) }
                                    override fun onFinish() { timerSwitch?.isChecked = false }
                                }.start()
                            }
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
            if(!timerSwitch!!.hasOnClickListeners()) {
                timerSwitch!!.setOnCheckedChangeListener { _, isChecked ->
                    when(isChecked) {
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
    private fun connectDevice(data: Intent, secure: Boolean) {
        // Get the device MAC address
        val extras = data.extras ?: return
        val address = extras.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        // Get the BluetoothDevice object
        val device = mBluetoothAdapter!!.getRemoteDevice(address)
        bluetoothAdapter?.bondedDevices?.firstOrNull { it.name == btDeviceName }?.apply {
            bluetoothAdapter?.cancelDiscovery()
            showToastC("Paired with $btDeviceName")
        }
        // Attempt to connect to the device
        mChatService.connect(device, secure)
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

        // On screen logging via a fragment with a TextView.
        val logFragment = supportFragmentManager
            .findFragmentById(R.id.log_fragment) as LogFragment?
        msgFilter.next = logFragment!!.logView
        gr.cs.btlamp.android.common.logger.Log.i(TAG, "Ready")
    }
}