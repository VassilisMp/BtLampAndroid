package gr.cs.btlamp.customViews

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TimePicker
import android.widget.TimePicker.OnTimeChangedListener
import gr.cs.btlamp.R

/*
* Copyright (C) 2007 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/**
 * A dialog that prompts the user for the time of day using a
 * [TimePicker].
 *
 *
 *
 * See the [Pickers]({@docRoot}guide/topics/ui/controls/pickers.html)
 * guide.
 */
open class TimePickerDialogCustom(
    context: Context,
    private val mTimeSetListener: OnTimeSetListener?,
    private val mInitialHourOfDay: Int,
    private val mInitialMinute: Int,
    private val mIs24HourView: Boolean
) :
    AlertDialog(context, 0/*resolveDialogTheme(context, themeResId)*/),
    DialogInterface.OnClickListener, OnTimeChangedListener {
    /**
     * @return the time picker displayed in the dialog
     * @hide For testing only.
     */
    private val timePicker: TimePicker

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (e.g. they clicked on the 'OK' button).
     */
    interface OnTimeSetListener {
        /**
         * Called when the user is done setting a new time and the dialog has
         * closed.
         *
         * @param view the view associated with this listener
         * @param hourOfDay the hour that was set
         * @param minute the minute that was set
         */
        fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int)
    }

    override fun onTimeChanged(view: TimePicker, hourOfDay: Int, minute: Int) {
        /* do nothing */
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        when (which) {
            BUTTON_POSITIVE -> {
                // Note this skips input validation and just uses the last valid time and hour
                // entry. This will only be invoked programmatically. User clicks on BUTTON_POSITIVE
                // are handled in show().
                mTimeSetListener?.onTimeSet(
                    timePicker, timePicker.currentHour,
                    timePicker.currentMinute
                )
                // Clearing focus forces the dialog to commit any pending
                // changes, e.g. typed text in a NumberPicker.
                timePicker.clearFocus()
                dismiss()
            }
            BUTTON_NEGATIVE -> cancel()
        }
    }

    /**
     * Sets the current time.
     *
     * @param hourOfDay The current hour within the day.
     * @param minuteOfHour The current minute within the hour.
     */
    fun updateTime(hourOfDay: Int, minuteOfHour: Int) {
        timePicker.currentHour = hourOfDay
        timePicker.currentMinute = minuteOfHour
    }

    override fun onSaveInstanceState(): Bundle {
        val state = super.onSaveInstanceState()
        state.putInt(HOUR, timePicker.currentHour)
        state.putInt(MINUTE, timePicker.currentMinute)
        state.putBoolean(IS_24_HOUR, timePicker.is24HourView)
        return state
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val hour = savedInstanceState.getInt(HOUR)
        val minute = savedInstanceState.getInt(MINUTE)
        timePicker.setIs24HourView(savedInstanceState.getBoolean(IS_24_HOUR))
        timePicker.currentHour = hour
        timePicker.currentMinute = minute
    }

    companion object {
        private const val HOUR = "hour"
        private const val MINUTE = "minute"
        private const val IS_24_HOUR = "is24hour"
        /*fun resolveDialogTheme(context: Context, resId: Int): Int {
            return if (resId == 0) {
                val outValue = TypedValue()
                context.theme.resolveAttribute(R.attr.timePickerDialogTheme, outValue, true)
                outValue.resourceId
            } else {
                resId
            }
        }*/
    }

    /**
     * Creates a new time picker dialog with the specified theme.
     *
     *
     * The theme is overlaid on top of the theme of the parent `context`.
     * If `themeResId` is 0, the dialog will be inflated using the theme
     * specified by the
     * [android:timePickerDialogTheme][android.R.attr.timePickerDialogTheme]
     * attribute on the parent `context`'s theme.
     *
     * @param context the parent context
     * @param themeResId the resource ID of the theme to apply to this dialog
     * @param listener the listener to call when the time is set
     * @param hourOfDay the initial hour
     * @param minute the initial minute
     * @param is24HourView Whether this is a 24 hour view, or AM/PM.
     */
    init {
        val themeContext = getContext()
        val inflater = LayoutInflater.from(themeContext)
        val view: View = inflater.inflate(R.layout.time_picker_dialog, null)
        setView(view)
        setButton(BUTTON_POSITIVE, themeContext.getString(R.string.ok), this)
        setButton(BUTTON_NEGATIVE, themeContext.getString(R.string.cancel), this)
//        setButtonPanelLayoutHint(LAYOUT_HINT_SIDE)
        timePicker = view.findViewById<View>(R.id.timePicker) as TimePicker
        timePicker.setIs24HourView(mIs24HourView)
        timePicker.currentHour = mInitialHourOfDay
        timePicker.currentMinute = mInitialMinute
        timePicker.setOnTimeChangedListener(this)
    }
}