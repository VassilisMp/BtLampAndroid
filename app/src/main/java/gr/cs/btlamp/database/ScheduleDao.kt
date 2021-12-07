package gr.cs.btlamp.database

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg schedule: Schedule)

    @Update
    suspend fun updateSchedules(vararg schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)

    @Query("SELECT * FROM schedule")
    fun getAll(): LiveData<List<Schedule>>
}