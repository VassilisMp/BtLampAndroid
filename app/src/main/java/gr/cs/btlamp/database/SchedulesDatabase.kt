package gr.cs.btlamp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Schedule::class], version = 1)
//@TypeConverters(Converters::class)
abstract class SchedulesDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao

    companion object {

        @Volatile private var INSTANCE: SchedulesDatabase? = null

        fun getInstance(context: Context): SchedulesDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                SchedulesDatabase::class.java, "btApp.db")
//                .addTypeConverter(Converters())
                .build()
    }
}
