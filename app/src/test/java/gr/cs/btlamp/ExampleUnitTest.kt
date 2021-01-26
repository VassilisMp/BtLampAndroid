package gr.cs.btlamp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.junit.Test

import org.junit.Assert.*

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

        // thread to keep process running
        Thread {
            while (true) {

            }
        }.run {
            start()
            join()
        }

    }
}