package gr.cs.btlamp.database

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.time.DayOfWeek

@Deprecated("")
@ProvidedTypeConverter
class Converters {
    @TypeConverter
    fun fromString(value: String): Array<DayOfWeek> {
        val listType: Type = object : TypeToken<Array<DayOfWeek>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArray(list: Array<DayOfWeek>): String {
        val gson = Gson()
        return gson.toJson(list)
    }
}

fun String.toArrayDaysOfWeek(): Array<DayOfWeek> =
    Gson().fromJson(this, object : TypeToken<Array<DayOfWeek>>() {}.type)

fun Array<DayOfWeek>.toJson(): String = Gson().toJson(this)
