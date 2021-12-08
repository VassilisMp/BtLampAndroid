package gr.cs.btlamp.ui.schedule

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import gr.cs.btlamp.R
import gr.cs.btlamp.ui.schedule.RepeatActivity.Companion.daysResult
import kotlinx.android.synthetic.main.activity_add_schedule.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

class AddScheduleActivity : AppCompatActivity() {

    private val resultIntent = Intent()

    private val checkedItem = intArrayOf(0)
    private val switchDialog by lazy {
        AlertDialog.Builder(this)
            .setTitle("switch type choice")
            .setSingleChoiceItems(arrayOf(ON, OFF), checkedItem[0]) { _, which ->
                // update the selected item which is selected by the user
                // so that it should be selected when user opens the dialog next time
                // and pass the instance to setSingleChoiceItems method
                checkedItem[0] = which
            }
            .setPositiveButton("Confirm") { _, which ->
//                checkedItem[0] = which
                if (checkedItem[0] == 0) textView5.text = ON
                else textView5.text = OFF
            }.setCancelable(false)
            .create()
    }

//    private lateinit var repeatDays: Array<DayOfWeek>

    @RequiresApi(Build.VERSION_CODES.O)
    private val getRepeatDays = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            // Handle the Intent
            intent?.getSerializableExtra(daysResult)?.also {
                schedule.days = it as Array<DayOfWeek>
                repeat_days_text.text = schedule.daysToString()
                /*@Suppress("UNCHECKED_CAST")
                with(it as Array<DayOfWeek>) {
                    if (size == 0) repeat_days_text.text = RepeatActivity.once
                    else {
                        repeat_days_text.text = this.joinToString(separator=",") {
                            it.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        }
                        resultIntent.putExtra(daysResult, this)
                    }
                    repeatDays = it
                }*/
            }
        }
    }

    private lateinit var schedule: ScheduleActivity.Schedule

//    private var repeatDays: Array<CharSequence>? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)
        with(intent?.getParcelableExtra<ScheduleActivity.Schedule>(edit_schedule)) {
            if (this != null) {
                schedule = this
                /*textView5.text = switchToString()
                repeat_days_text.text = if (days.isNotEmpty()) days.joinToString(separator = ", ")
                else "once"
                timePickerS.run {
                    hour = time.first
                    minute = time.second
                }*/
//                repeatDays = days
            } else {
                /*textView5.text = ON
                repeat_days_text.text = "once"
                timePickerS.run {
                    Calendar.getInstance().run {
                        hour = get(Calendar.HOUR_OF_DAY)
                        minute = get(Calendar.MINUTE)
                    }
                }*/
//                repeatDays = emptyArray()
                schedule = ScheduleActivity.Schedule()
            }
            checkedItem[0] = schedule.switchToInt()
            textView5.text = schedule.switchToString()
            repeat_days_text.text = schedule.daysToString()
            timePickerS.run {
                hour = schedule.time.first
                minute = schedule.time.second
            }
        }
        repeat_view.setOnClickListener {
            // Use the Kotlin extension in activity-ktx
            // passing it the Intent you want to start
            with(Intent(this, RepeatActivity::class.java)) {
//                putExtra(daysResult, repeatDays)
                putExtra(daysResult, schedule.days)
                getRepeatDays.launch(this)
            }
        }
        switch_view.setOnClickListener {
            switchDialog.show()
        }
        button2.setOnClickListener {
            /*resultIntent.run{
                putExtra(time, timePickerS.hour to timePickerS.minute)
                putExtra(switchVal, checkedItem[0])
                putExtra(repeatDaysStr, repeatDays)
            }*/
            schedule.time = timePickerS.hour to timePickerS.minute
            schedule.switch = checkedItem[0] == 0
            setResult(Activity.RESULT_OK, Intent().run { putExtra(edit_schedule, schedule) })
            finish()
        }
    }

    companion object {
        const val ON = "ON"
        const val OFF = "OFF"
        const val time = "time"
        const val repeatDaysStr = "repeatDays"
        const val switchVal = "switch"
    }
}