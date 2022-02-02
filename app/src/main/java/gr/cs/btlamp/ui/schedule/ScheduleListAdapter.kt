package gr.cs.btlamp.ui.schedule

import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.markushi.ui.CircleButton
import com.google.android.material.switchmaterial.SwitchMaterial
import gr.cs.btlamp.MyBluetoothService
import gr.cs.btlamp.R
import gr.cs.btlamp.android.bluetoothchat.BluetoothService
import gr.cs.btlamp.ui.schedule.AddScheduleActivity.Companion.OFF
import gr.cs.btlamp.ui.schedule.AddScheduleActivity.Companion.ON
import kotlinx.android.synthetic.main.schedule_item.view.*

class ScheduleListAdapter(
    private val getSchedule: ActivityResultLauncher<Intent>,
    val deleteFunction: (ScheduleActivity.Schedule) -> Unit
):
    ListAdapter<ScheduleActivity.Schedule, ScheduleListAdapter.ScheduleViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.schedule_item, parent, false)
        return ScheduleViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = getItem(position)
        with(holder) {
            this.schedule = schedule
            time.text = String.format("%02d:%02d", schedule.time.first, schedule.time.second)
            switchV.text = schedule.switch.run { if(this) ON else OFF }
            days.text = schedule.daysToString()
            switch.setOnCheckedChangeListener { compoundButton, b ->
                schedule.activated = b
                with(BluetoothService.getService().btApi) {
                    if (b) addSchedule(schedule)
                    else removeSchedule(schedule)
                }
            }
            view.setOnClickListener {
                with(Intent(view.context, AddScheduleActivity::class.java)) {
                    putExtra(edit_schedule, schedule)
                    getSchedule.launch(this)
                }
            }
            deleteButton.setOnClickListener {
                deleteFunction(schedule)
            }
        }
    }

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val view = itemView
        lateinit var schedule: ScheduleActivity.Schedule
        val time: TextView = itemView.sch_item_time
        val switchV: TextView = itemView.sch_item_switchVal
        val days: TextView = itemView.sch_item_repeat
        val switch: SwitchMaterial = itemView.sch_item_switch
        val deleteButton: CircleButton = itemView.schedule_del_button
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<ScheduleActivity.Schedule> = object : DiffUtil.ItemCallback<ScheduleActivity.Schedule>() {
            override fun areItemsTheSame(oldItem: ScheduleActivity.Schedule, newItem: ScheduleActivity.Schedule): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ScheduleActivity.Schedule, newItem: ScheduleActivity.Schedule) =
                (oldItem.switch == newItem.switch
                        && oldItem.days.contentEquals(newItem.days)
                        && oldItem.time.first == newItem.time.first
                        && oldItem.time.second == newItem.time.second)
        }
        private val TAG = ScheduleListAdapter::class.java.simpleName
    }
}

const val edit_schedule = "edit_schedule"