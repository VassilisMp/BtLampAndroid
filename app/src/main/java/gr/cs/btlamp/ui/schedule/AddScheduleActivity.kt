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

    @RequiresApi(Build.VERSION_CODES.O)
    private val getRepeatDays = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            // Handle the Intent
            intent?.getSerializableExtra(daysResult)?.also { it ->
                @Suppress("UNCHECKED_CAST")
                with(it as Array<DayOfWeek>) {
                    if (size == 0) repeat_days_text.text = RepeatActivity.once
                    else {
                        repeat_days_text.text = this.joinToString(separator=",") {
                            it.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        }
                        resultIntent.putExtra(daysResult, this)
                    }
                }
            }
        }
    }

//    private var repeatDays: Array<CharSequence>? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_schedule)
        timePickerS.run {
            Calendar.getInstance().run {
                hour = get(Calendar.HOUR_OF_DAY)
                minute = get(Calendar.MINUTE)
            }
        }
        repeat_view.setOnClickListener {
            // Use the Kotlin extension in activity-ktx
            // passing it the Intent you want to start
            getRepeatDays.launch(Intent(this, RepeatActivity::class.java))
        }
        switch_view.setOnClickListener {
            val checkedItem = if (textView5.text == ON) 0 else 1
            AlertDialog.Builder(this)
                .setTitle("switch type choice")
                .setSingleChoiceItems(arrayOf(ON, OFF), checkedItem, null)
                .setPositiveButton("Confirm") { _, which ->
                    if (which == 0) textView5.text = ON
                    else textView5.text = OFF
                }.setCancelable(false)
        }
        button2.setOnClickListener {
            resultIntent.run{
                putExtra(time, timePickerS.hour to timePickerS.minute)
                putExtra(switchVal, textView5.text == ON)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }

    companion object {
        const val ON = "ON"
        const val OFF = "OFF"
        const val time = "time"
        const val repeatDays = "repeatDays"
        const val switchVal = "switch"
    }
}