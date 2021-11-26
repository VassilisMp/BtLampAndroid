package gr.cs.btlamp.customViews

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TimePicker

class TimePickerC : TimePicker {

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)


    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return if (isEnabled) super.onInterceptTouchEvent(ev)
        else true
    }
}