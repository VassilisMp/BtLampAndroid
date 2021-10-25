package gr.cs.btlamp.ui

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import gr.cs.btlamp.MyBluetoothService
import gr.cs.btlamp.R
import gr.cs.btlamp.showToast
import gr.cs.btlamp.showToastC
import gr.cs.btlamp.ui.tabbed.SectionsPagerAdapter
import kotlinx.android.synthetic.main.activity_tabbed.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val TAG = "TabbedActivity"
private const val REQUEST_ENABLE_BT: Int = 1
class TabbedActivity : AppCompatActivity() {

    private val mBluetoothAdapter: BluetoothAdapter? by lazy { BluetoothAdapter.getDefaultAdapter() }

    /* service binding code */
    internal var mService: MyBluetoothService? = null
    private var mBound: Boolean = false
    private var channelListenJob: Job? = null

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as MyBluetoothService.LocalBinder
            mService = binder.service
            // assign view, so that service can show snackbar
            binder.view = findViewById<View>(android.R.id.content).findViewById(R.id.corLay)
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

    override fun onStart() {
        super.onStart()
        Intent(this, MyBluetoothService::class.java).also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
        mBound = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabbed)
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
        lifecycleScope.launchWhenStarted { btPermission() }
        switchButton.setOnClickListener {
            if (switchButton.isChecked)
                mService?.btApi?.enableLight()
            else
                mService?.btApi?.disableLight()
        }
        left_time_btn.setOnClickListener {
            // TODO("show activity for leftTime")
        }
        schedule_btn.setOnClickListener {
            // TODO("show activity for Schedule")
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

    private fun bluetoothDisabled() {
        showToast("Bluetooth is disabled, please enable first.")
//        btPermission()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, this::onActivityResult.name)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                when (resultCode) {
//                    RESULT_OK ->
                    RESULT_CANCELED -> {
//                        finish()
                        btPermission()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}