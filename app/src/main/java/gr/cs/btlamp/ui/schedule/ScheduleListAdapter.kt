package gr.cs.btlamp.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import gr.cs.btlamp.R
import gr.cs.btlamp.database.Schedule
import gr.cs.btlamp.database.toArrayDaysOfWeek
import kotlinx.android.synthetic.main.schedule_item.view.*

class ScheduleListAdapter: ListAdapter<ScheduleActivity.ScheduleNew, ScheduleListAdapter.ScheduleViewHolder>
    (DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.schedule_item, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = getItem(position)
        with(holder) {
            this.schedule = schedule
            time.text = String.format("%2d:%2d", schedule.time.first, schedule.time.second)
            switchV.text = schedule.switch.run { if(this) "ON" else "" }
            days.text = schedule.days.joinToString(separator = ", ")
            switch.setOnCheckedChangeListener { compoundButton, b ->
            TODO()
                if (b) {

                } else {

                }
            }
        }
    }

    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var schedule: ScheduleActivity.ScheduleNew
        val time: TextView = itemView.sch_item_time
        val switchV: TextView = itemView.sch_item_switchVal
        val days: TextView = itemView.sch_item_repeat
        val switch: SwitchMaterial = itemView.sch_item_switch
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<ScheduleActivity.ScheduleNew> = object : DiffUtil.ItemCallback<ScheduleActivity.ScheduleNew>() {
            override fun areItemsTheSame(oldItem: ScheduleActivity.ScheduleNew, newItem: ScheduleActivity.ScheduleNew): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ScheduleActivity.ScheduleNew, newItem: ScheduleActivity.ScheduleNew) =
                (oldItem.switch == newItem.switch
                        && oldItem.days.contentEquals(newItem.days)
                        && oldItem.time.first == newItem.time.first
                        && oldItem.time.second == newItem.time.second)
        }
        private val TAG = ScheduleListAdapter::class.java.simpleName
    }
}