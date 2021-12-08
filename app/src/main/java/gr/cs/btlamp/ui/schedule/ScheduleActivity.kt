package gr.cs.btlamp.ui.schedule

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gr.cs.btlamp.MyBluetoothService
import gr.cs.btlamp.R
import gr.cs.btlamp.ui.schedule.RepeatActivity.Companion.once
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_add_schedule.*
import kotlinx.android.synthetic.main.activity_schedule.*
import java.lang.reflect.Type
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*
import kotlin.properties.Delegates

@SuppressLint("NotifyDataSetChanged")
class ScheduleActivity : AppCompatActivity() {

    @Parcelize
    public data class Schedule(
        var time: Pair<Int, Int>,
        var switch: Boolean,
        var days: Array<DayOfWeek>,
        val id: String = UUID.randomUUID().toString()
    ): Parcelable {
        fun getHour() = time.first
        fun getMinute() = time.second
        fun switchToString() = if (switch) AddScheduleActivity.ON else AddScheduleActivity.OFF
        fun switchToInt() = if (switch) 0 else 1
        @RequiresApi(Build.VERSION_CODES.O)
        fun daysToString() = if (days.isNotEmpty()) days.joinToString(separator=", ") {
            it.getDisplayName(TextStyle.FULL, Locale.getDefault())
        } else once

        /**
         * @return DayOfWeek Array as a ByteArray using the DayOfWeek ordinals
         */
        fun daysToByteArray() = byteArrayOf(7, 7, 7, 7, 7, 7, 7).apply {
            days.iterator().withIndex().forEach { this[it.index] = it.value.ordinal.toByte() }
        }
        constructor(): this(with(Calendar.getInstance()) { get(Calendar.HOUR_OF_DAY) to get(Calendar.MINUTE) }, true, emptyArray())
    }

//    private val viewModel: ScheduleViewModel by viewModels()
//    val sharedPref by lazy { getPreferences(Context.MODE_PRIVATE) }
    private var schedules: MutableList<Schedule> by Delegates.observable(mutableListOf()) { property,
                                                                                           oldValue,
                                                                                           newValue ->
        Log.d("schedulesAdd", "added schedule")
        scheduleListAdapter.submitList(newValue)
        scheduleListAdapter.notifyDataSetChanged()
    }

    private fun deleteSchedule(schedule: Schedule) = with(getPreferences(MODE_PRIVATE).edit()) {
        schedules.remove(schedule)
        schedules.filter { it != schedule }
        schedules = schedules.toMutableList()
        putString(schedules_key, schedules.toJson())
        apply()
        // TODO fix BluetoothService first
        /*with(MyBluetoothService().BtApi()) {
            removeSchedule(schedule)
        }*/
    }

    private lateinit var scheduleListAdapter: ScheduleListAdapter

    @RequiresApi(Build.VERSION_CODES.N)
    private val getSchedule: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            // Handle the Intent
            /*intent?.getParcelableArrayExtra(RepeatActivity.daysResult)?.run {
                if (size == 0) repeat_days_text.text = RepeatActivity.once
                else repeat_days_text.text = this.joinToString(separator=",")
            }*/
            if (intent != null) {
                /*val time: Pair<Int, Int> = intent.getSerializableExtra(AddScheduleActivity.time) as Pair<Int, Int>
                val switchVal: Boolean = intent.getIntExtra(AddScheduleActivity.switchVal, 0) == 0
                val days: Array<DayOfWeek> = intent.getSerializableExtra(AddScheduleActivity
                    .repeatDaysStr) as Array<DayOfWeek>*/
                /*viewModel.insert(Schedule(hour = time.first, minute = time.second, switch = switchVal,
                    days = days.toJson()))*/
                with(getPreferences(MODE_PRIVATE).edit()) {
//                    val schedule = Schedule(time, switchVal, days)
                    val schedule: Schedule = intent.getParcelableExtra(edit_schedule)!!
                    schedules.removeIf { it.id.equals(schedule.id) }
                    schedules += schedule
                    schedules.sortBy { it.getHour().toFloat() + (it.getMinute() / 60.0) }
                    schedules = schedules.toMutableList()
                    putString(schedules_key, schedules.toJson())
                    apply()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        add_schedule_button.setOnClickListener {
            getSchedule.launch(Intent(this, AddScheduleActivity::class.java))
        }
        scheduleListAdapter = ScheduleListAdapter(getSchedule, this::deleteSchedule)
        with(schedule_rv) {
            adapter = scheduleListAdapter
            layoutManager = LinearLayoutManager(this@ScheduleActivity)
            addItemDecoration(
                DividerItemDecoration(context, (layoutManager as LinearLayoutManager).orientation)
            )
        }
        val schedulesJson = getPreferences(MODE_PRIVATE).getString(schedules_key, "")!!
        scheduleListFromJson(schedulesJson)?.toMutableList()?.apply { schedules = this }
//        scheduleListAdapter.submitList(schedules)
        /*viewModel.schedules().observe(this) { schedules ->
            scheduleListAdapter.submitList(schedules)
        }*/
    }

    companion object {
        const val schedules_key = "schedules"
    }
}

fun List<ScheduleActivity.Schedule>.toJson(): String = Gson().toJson(this)

fun scheduleListFromJson(json: String): MutableList<ScheduleActivity.Schedule>? {
    val objectType: Type = object : TypeToken<MutableList<ScheduleActivity.Schedule>>() {}.type
    return Gson().fromJson(json, objectType)
}