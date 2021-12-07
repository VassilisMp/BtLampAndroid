package gr.cs.btlamp.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity
data class Schedule(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var hour: Int,
    var minute: Int,
    var switch: Boolean,
    var days: String
) {
    constructor() : this(0, 0, 0, true, "")
}
