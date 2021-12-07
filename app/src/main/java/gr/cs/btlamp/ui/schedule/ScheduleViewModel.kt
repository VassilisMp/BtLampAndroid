package gr.cs.btlamp.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import gr.cs.btlamp.database.Schedule
import gr.cs.btlamp.database.ScheduleDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ScheduleViewModel(private val dataSource: ScheduleDao) : ViewModel() {

    fun insert(vararg schedule: Schedule) = GlobalScope.launch(Dispatchers.IO) {
        dataSource.insertAll(*schedule)
    }

    fun update(vararg schedule: Schedule) = GlobalScope.launch(Dispatchers.IO) {
        dataSource.updateSchedules(*schedule)
    }

    fun delete(schedule: Schedule) = GlobalScope.launch(Dispatchers.IO) {
        dataSource.delete(schedule)
    }

    fun schedules(): LiveData<List<Schedule>> = dataSource.getAll()
}