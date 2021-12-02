package gr.cs.btlamp.ui.schedule

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import gr.cs.btlamp.R
import kotlinx.android.synthetic.main.activity_add_schedule.*
import kotlinx.android.synthetic.main.activity_repeat.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

class RepeatActivity : AppCompatActivity() {

    @ExperimentalStdlibApi
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repeat)
        // Handle the Intent
        intent?.getSerializableExtra(daysResult)?.also { it ->
            @Suppress("UNCHECKED_CAST")
            (it as Array<DayOfWeek>).forEach { day ->
                when(day) {
                    DayOfWeek.SUNDAY -> checkBox_sunday.isChecked = true
                    DayOfWeek.MONDAY -> checkBox_monday.isChecked = true
                    DayOfWeek.TUESDAY -> checkBox_tuesday.isChecked = true
                    DayOfWeek.WEDNESDAY -> checkBox_wednesday.isChecked = true
                    DayOfWeek.THURSDAY -> checkBox_thursday.isChecked = true
                    DayOfWeek.FRIDAY -> checkBox_friday.isChecked = true
                    DayOfWeek.SATURDAY -> checkBox_saturday.isChecked = true
                }
            }
        }
        save_days_button.setOnClickListener {
            val array = buildList {
                if (checkBox_sunday.isChecked) this += DayOfWeek.SUNDAY
                if (checkBox_monday.isChecked) this += DayOfWeek.MONDAY
                if (checkBox_tuesday.isChecked) this += DayOfWeek.TUESDAY
                if (checkBox_wednesday.isChecked) this += DayOfWeek.WEDNESDAY
                if (checkBox_thursday.isChecked) this += DayOfWeek.THURSDAY
                if (checkBox_friday.isChecked) this += DayOfWeek.FRIDAY
                if (checkBox_saturday.isChecked) this += DayOfWeek.SATURDAY
            }.toTypedArray()
            val result = Intent().putExtra(daysResult, array)
                /*Intent().putExtra(
                SecondActivityContract.RESULT_KEY,
                enterResultText.text.toString().toIntOrNull()
            )*/
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    companion object {
        const val daysResult = "days"
        const val once = "once"
    }
}