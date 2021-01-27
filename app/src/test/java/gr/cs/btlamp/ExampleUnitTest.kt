package gr.cs.btlamp

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Test
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Executors
import kotlin.properties.Delegates
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
}