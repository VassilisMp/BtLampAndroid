package gr.cs.btlamp.ui.schedule

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gr.cs.btlamp.R
import kotlinx.android.synthetic.main.activity_add_schedule.*
import kotlinx.android.synthetic.main.activity_schedule.*
import java.lang.reflect.Type
import java.time.DayOfWeek
import kotlin.properties.Delegates

class ScheduleActivity : AppCompatActivity() {

    data class ScheduleNew(
        val time: Pair<Int, Int>,
        val switch: Boolean,
        val days: Array<DayOfWeek>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ScheduleNew

            if (time != other.time) return false
            if (switch != other.switch) return false
            if (!days.contentEquals(other.days)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = time.hashCode()
            result = 31 * result + switch.hashCode()
            result = 31 * result + days.contentHashCode()
            return result
        }

        fun toJson() = Gson().toJson(this)
    }

//    private val viewModel: ScheduleViewModel by viewModels()
//    val sharedPref by lazy { getPreferences(Context.MODE_PRIVATE) }
    private var schedules: MutableList<ScheduleNew> by Delegates.observable(mutableListOf()) { property, oldValue, newValue ->
        Log.d("schedulesAdd", "added schedule")
        scheduleListAdapter.submitList(newValue)
        scheduleListAdapter.notifyDataSetChanged()
    }

    private lateinit var scheduleListAdapter: ScheduleListAdapter

    private val getSchedule = registerForActivityResult(ActivityResultContracts
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
                val time: Pair<Int, Int> = intent.getSerializableExtra(AddScheduleActivity.time) as Pair<Int, Int>
                val switchVal: Boolean = intent.getIntExtra(AddScheduleActivity.switchVal, 0) == 0
                val days: Array<DayOfWeek> = intent.getSerializableExtra(AddScheduleActivity
                    .repeatDaysStr) as Array<DayOfWeek>
                /*viewModel.insert(Schedule(hour = time.first, minute = time.second, switch = switchVal,
                    days = days.toJson()))*/
                with(getPreferences(MODE_PRIVATE).edit()) {
                    val schedule = ScheduleNew(time, switchVal, days)
                    schedules += schedule
                    schedules = schedules.toMutableList()
                    putString(schedules_key, schedules.toJson())
                    apply()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        add_schedule_button.setOnClickListener {
            getSchedule.launch(Intent(this, AddScheduleActivity::class.java))
        }
        scheduleListAdapter = ScheduleListAdapter()
        with(schedule_rv) {
            adapter = scheduleListAdapter
            layoutManager = LinearLayoutManager(this@ScheduleActivity)
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

fun fromJson(json: String): ScheduleActivity.ScheduleNew {
    val objectType: Type = object : TypeToken<ScheduleActivity.ScheduleNew>() {}.type
    return Gson().fromJson(json, objectType)
}
fun ScheduleActivity.ScheduleNew.fromSchedule() = Gson().toJson(this)

fun List<ScheduleActivity.ScheduleNew>.toJson(): String = Gson().toJson(this)

fun scheduleListFromJson(json: String): MutableList<ScheduleActivity.ScheduleNew>? {
    val objectType: Type = object : TypeToken<MutableList<ScheduleActivity.ScheduleNew>>() {}.type
    return Gson().fromJson(json, objectType)
}