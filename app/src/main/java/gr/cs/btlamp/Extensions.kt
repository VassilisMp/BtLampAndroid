package gr.cs.btlamp

import android.content.Context
import android.widget.Toast
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
