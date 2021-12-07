package gr.cs.btlamp

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import gr.cs.btlamp.ui.schedule.AddScheduleActivity
import gr.cs.btlamp.ui.schedule.ScheduleActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.lang.reflect.Type
import java.time.DayOfWeek
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.exitProcess


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun test_channels() {
        val channel = Channel<Int>()
        GlobalScope.launch {
            for (x in 1..5) channel.send(x * x)
            channel.close() // we're done sending
        }

        GlobalScope.launch(Dispatchers.IO) {
            // here we print received values using `for` loop (until the channel is closed)
            for (y in channel) println(y)
            println("Done!")
        }

        while (true) {

        }

    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun test_suspending() {
        val filePath = "/tmp/test.txt"
        val file1 = File(filePath)
        file1.delete()
        file1.createNewFile()
        val file = File(filePath)
        val inputStream = file.inputStream()
        val writeDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val ioDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        suspend fun run() = withContext(writeDispatcher) {
            for (i in 0 .. 10) {
                File(filePath).appendBytes(i.toString().toByteArray())
//                print(i)
                delay(100)
            }
            File(filePath).appendBytes("\n".toByteArray())
            delay(100)
        }

        GlobalScope.launch {
            while (true)
                run()
        }

        val channel: Channel<String> = Channel()
//        val writeThread = WriteThread().apply { start() }
        val buffer = ByteArray(1024) // buffer store for the stream
        var bytes = 0 // bytes returned from read()
        GlobalScope.launch(ioDispatcher) {
            while (true) {
                if (inputStream.available() > 0) {
                    buffer[bytes] = inputStream.read().toByte()
                    if (buffer[bytes].toChar() == '\n'){
                        val readMessage = String(buffer, 0, bytes)
                        bytes = 0
//                        print(readMessage + "\n")
                        channel.send(readMessage)
                    } else {
                        bytes += 1
                    }
//                    val line = bufferedReader.readLine()
//                    print(stringBuilder.toString() + "\n")
                } else {
                    delay(1)
                }
            }
        }

        GlobalScope.launch(ioDispatcher) {
            for (string in channel)
                print(string + "\n")
        }

        val scanner = Scanner(System.`in`)

        var line: String?
        print("give input")
            line = scanner.nextLine()
            while (line != "exit") {
                print("give input")
                line = scanner.nextLine()
            }
            exitProcess(0)
    }

    @Test
    fun mytest() {
        val scanner = Scanner(System.`in`)

        var line: String?
        print("give input")
        line = scanner.nextLine()
        while (line != "exit") {
            print("give input")
            line = scanner.nextLine()
        }
        exitProcess(0)
    }

    private val btDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var scope: CoroutineScope = CoroutineScope(btDispatcher)
        get() {
            if (!field.isActive) {
                field = CoroutineScope(btDispatcher)
            }
            return field
        }

    @Test
    fun test_cor() {
        val launch = scope.launch {
            println("sdcscs")
            cancel("cancel test", Exception())
        }
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        GlobalScope.launch(dispatcher) {
            delay(1000)
            println("hahah" + launch.isCancelled)
        }
        Thread.currentThread().run {
            Thread.sleep(2000)
            scope.launch {
                println("after cancel")
            }
        }
    }

    @Test
    fun cor_scope() {
        val jobList = mutableListOf<Job>()
        if (scope.isActive)
            println("scope is active")
        jobList += scope.launch {
            println("launched 1, waiting...")
            delay(1000)
            println("1 after wait.")
        }
        jobList += scope.launch {
            println("launched 2, waiting...")
            delay(3000)
            println("2 after wait.")
        }
        jobList += GlobalScope.launch {
            delay(1100)
            scope.cancel()
            println("cancel scope")
            println(scope.isActive)
            scope.launch(btDispatcher) { println("after canceled scope") }
            println(scope.isActive)
        }
        jobList += GlobalScope.launch(btDispatcher) {
            println("GlobalScope")
            println("launched 3, waiting...")
            delay(3000)
            println("3 after wait.")
        }
        runBlocking {
            jobList.joinAll()
        }
    }

    @Test
    fun testIntTobytearray() {
        val int: Int = 1
        val bytes = int.toByteArray()
        println(bytes.size)
    }

    @Test
    fun json_test() {
        fun fromString(value: String): Array<DayOfWeek> {
            val listType: Type = object : TypeToken<Array<DayOfWeek>>() {}.type
            return Gson().fromJson(value, listType)
        }
        fun fromArrayList(list: Array<DayOfWeek>): String {
            val gson = Gson()
            return gson.toJson(list)
        }

        val days = arrayOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)
        println(fromString(fromArrayList(days)).contentToString())
    }

    @Test
    fun scheduleToJson() {
        fun fromString(value: String): ScheduleActivity.ScheduleNew {
            val objectType: Type = object : TypeToken<ScheduleActivity.ScheduleNew>() {}.type
            return Gson().fromJson(value, objectType)
        }
        fun fromSchedule(schedule: ScheduleActivity.ScheduleNew): String {
            val gson = Gson()
            return gson.toJson(schedule)
        }
        val time: Pair<Int, Int> = 10 to 45
        val switchVal = true
        val days = arrayOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)
        val scheduleNew = ScheduleActivity.ScheduleNew(time, switchVal, days)
        val remade = fromString(fromSchedule(scheduleNew))
        println(remade)
        with(scheduleNew) {
            assertEquals(this.time, remade.time)
            assertEquals(this.switch, remade.switch)
            assertArrayEquals(this.days, remade.days)
        }
    }

    @Test
    fun scheduleListJson() {
        fun List<ScheduleActivity.ScheduleNew>.toJson(): String = Gson().toJson(this)

        fun scheduleListFromJson(json: String): List<ScheduleActivity.ScheduleNew> {
            val objectType: Type = object : TypeToken<List<ScheduleActivity.ScheduleNew>>() {}.type
            return Gson().fromJson(json, objectType)
        }

        val time: Pair<Int, Int> = 10 to 45
        val switchVal = true
        val days = arrayOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY)
        val scheduleNew = ScheduleActivity.ScheduleNew(time, switchVal, days)
        val scheduleNew2 = ScheduleActivity.ScheduleNew(11 to 50, switchVal, days)
        val list = listOf(scheduleNew, scheduleNew2)
        println(list.toJson())
        println(scheduleListFromJson(list.toJson()).toJson())
    }
}