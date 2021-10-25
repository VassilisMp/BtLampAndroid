package gr.cs.btlamp.ui.tabbed

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import gr.cs.btlamp.showToast
import gr.cs.btlamp.snackBarMakeC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()
    val text: LiveData<String> = Transformations.map(_index) {
        "Hello world from section: $it"
    }

    fun setIndex(index: Int) {
        _index.value = index
    }
}

/*
class ConnectionLiveData(private val context: Context) : LiveData<Boolean>() {

    */
/*This method is used to check if bluetooth is disconnected. If disconnected try to reconnect automatically*//*

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    GlobalScope.launch(Dispatchers.Main) {
                        connectionListener?.onDisconnected()
                        snackBarMakeC(
                                "Lost bluetooth connection with ${device?.name}",
                                actionText = "Retry",
                                block = { BTconnect() }
                        )
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            showToast("Bluetooth off")
                            scope.cancel()
                            socket?.close()
                            BTconnect()
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> showToast("Turning Bluetooth off...")
                        BluetoothAdapter.STATE_ON -> showToast("Bluetooth on")
                        BluetoothAdapter.STATE_TURNING_ON -> showToast("Turning Bluetooth on...")
                    }
                }
            }
        }
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            postValue(context.isConnected)
        }
    }

    override fun onActive() {
        super.onActive()
        context.registerReceiver(
                networkReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    override fun onInactive() {
        super.onInactive()
        try {
            context.unregisterReceiver(networkReceiver)
        } catch (e: Exception) {
        }
    }
}val Context.isConnected: Boolean
    get() = (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isConnected == true*/
