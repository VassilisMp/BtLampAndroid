package gr.cs.btlamp.ui.schedule

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import gr.cs.btlamp.R
import kotlinx.android.synthetic.main.activity_add_schedule.*
import kotlinx.android.synthetic.main.activity_schedule.*

class ScheduleActivity : AppCompatActivity() {

    private val getSchedule = registerForActivityResult(ActivityResultContracts
        .StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            // Handle the Intent
            intent?.getCharSequenceArrayExtra(RepeatActivity.daysResult)?.run {
                if (size == 0) repeat_days_text.text = RepeatActivity.once
                else repeat_days_text.text = this.joinToString(separator=",")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        add_schedule_button.setOnClickListener {
            getSchedule.launch(Intent(this, AddScheduleActivity::class.java))
        }
    }
}