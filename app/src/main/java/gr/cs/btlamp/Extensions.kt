package gr.cs.btlamp

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.graphics.drawable.TransitionDrawable
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.animation.ValueAnimator

import android.animation.ValueAnimator.AnimatorUpdateListener

import android.R
import android.graphics.drawable.GradientDrawable








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

// show Snackbar on this View
suspend inline fun View.snackBarMake(
    text: CharSequence,
    @BaseTransientBottomBar.Duration duration: Int = Snackbar.LENGTH_INDEFINITE,
    actionText: String? = null,
    crossinline block: () -> Unit
) = withContext(Dispatchers.Main) {
    Snackbar.make(
        this@snackBarMake,
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

fun Int.toByteArray(): ByteArray = ByteBuffer.allocate(Int.SIZE_BYTES)
    .order(ByteOrder.BIG_ENDIAN)  // BIG_ENDIAN is default byte order, so it is not necessary.
    .putInt(this)
    .array()

fun timeToMillis(hours: Number = 0, minutes: Number = 0) =
    hours.toLong() * 3600000 + minutes.toLong() * 60000

fun timeToMillis(time: Pair<Int, Int>) = timeToMillis(time.first, time.second)

/**
 * @returns a pair with hours as first and minutes as second
 */
fun millisToTime(millis: Long): Pair<Int, Int> =
    (millis/1000/60/60).toInt() to ((millis/1000/60)%60).toInt()

/**
 * Disables and darkens view with animation.
 */
fun View.disable() {
    ValueAnimator.ofObject(
        ArgbEvaluator(),
        0x00000000,
        0x80000000.toInt()
    ).run {
        addUpdateListener { animator ->
            (foreground as ColorDrawable).color = (animator.animatedValue as Int)
        }
        duration = 250
        start()
    }
    isEnabled = false
}

/**
 * Enables and undoes darken view with animation.
 */
fun View.enable() {
    ValueAnimator.ofObject(
        ArgbEvaluator(),
        0x80000000.toInt(),
        0x00000000
    ).run {
        addUpdateListener { animator ->
            (foreground as ColorDrawable).color = (animator.animatedValue as Int)
        }
        duration = 250
        start()
    }
    isEnabled = true
}
