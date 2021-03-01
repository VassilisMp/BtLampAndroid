package gr.cs.btlamp

import android.content.Context
import android.widget.Toast
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun Int.toHexString() = Integer.toHexString(this)

fun Context.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, text, duration).show()

suspend inline fun Context.showToastC(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) = withContext(
    Dispatchers.Main
){
    showToast(text, duration)
}

suspend inline fun Context.snackBarMakeC(
    text: CharSequence,
    @BaseTransientBottomBar.Duration duration: Int = Snackbar.LENGTH_INDEFINITE,
    actionText: String? = null,
    crossinline block: () -> Unit
) = withContext(Dispatchers.Main) {
    SnackbarWrapper.make(
        this@snackBarMakeC,
        text,
        duration
    ).apply {
        if (actionText != null)
            setAction(actionText) { block() }
        show()
    }
}

@ExperimentalUnsignedTypes
fun UInt.toByteArray(isBigEndian: Boolean = true): ByteArray {
    var bytes = byteArrayOf()

    var n = this

    if (n == 0x00u) {
        bytes += n.toByte()
    } else {
        while (n != 0x00u) {
            val b = n.toByte()

            bytes += b

            n = n.shr(Byte.SIZE_BITS)
        }
    }

    val padding = 0x00u.toByte()
    var paddings = byteArrayOf()
    repeat(UInt.SIZE_BYTES - bytes.count()) {
        paddings += padding
    }

    return if (isBigEndian) {
        paddings + bytes.reversedArray()
    } else {
        paddings + bytes
    }
}

@ExperimentalUnsignedTypes
fun ULong.toByteArray(): ByteArray {
    return ByteBuffer.allocate(Long.SIZE_BYTES)
        .order(ByteOrder.BIG_ENDIAN)  // BIG_ENDIAN is default byte order, so it is not necessary.
        .putLong(this.toLong())
        .array()
}

@ExperimentalUnsignedTypes
fun UInt.toByteArray(): ByteArray {
    return ByteBuffer.allocate(Int.SIZE_BYTES)
        .order(ByteOrder.BIG_ENDIAN)  // BIG_ENDIAN is default byte order, so it is not necessary.
        .putInt(this.toInt())
        .array()
}
