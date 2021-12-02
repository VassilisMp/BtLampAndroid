package gr.cs.btlamp.ui.schedule

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import gr.cs.btlamp.R
import kotlinx.android.synthetic.main.activity_repeat.*
import java.time.DayOfWeek

class RepeatActivity : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_repeat)
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