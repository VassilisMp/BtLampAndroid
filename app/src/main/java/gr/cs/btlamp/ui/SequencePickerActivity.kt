package gr.cs.btlamp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.larswerkman.holocolorpicker.ColorPicker
import gr.cs.btlamp.R
import kotlinx.android.synthetic.main.activity_sequence_picker.*


private const val TAG = "SequencePickerActivity"
class SequencePickerActivity : AppCompatActivity() {

    private var currentColor: Int = 0
    private lateinit var colorsAdapter: ColorsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sequence_picker)
        colorsAdapter = ColorsAdapter()
        // TODO implement Drag and reorder RecyclerView items
        colors_rv.apply {
            adapter = colorsAdapter
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL))
        }
        //to turn of showing the old color
        picker.run {
            showOldCenterColor = false
            addSVBar(svbar)
            addOpacityBar(opacitybar)
            onColorChangedListener = ColorPicker.OnColorChangedListener { currentColor = it }
        }
        pick_button.setOnClickListener {
            val newList = colorsAdapter.currentList.toMutableList().apply {
                add(Color(currentColor))
            }
            colorsAdapter.submitList(newList)
            // TODO don' t use notifyDataSetChanged
            colorsAdapter.notifyDataSetChanged()
            /*listItems.add(Color(currentColor))
            adapter!!.notifyDataSetChanged()*/
//            colorArrayAdapter.add(Color(currentColor))
//            colorArrayAdapter.notifyDataSetChanged()
        }
        back_button.setOnClickListener { returnResult() }
    }

    private fun returnResult() {
        Log.d(TAG, ::returnResult.name)
        setResult(REQUEST_COLOR_SEQUENCE,
            Intent().apply { putExtra(SEQUENCE, colorsAdapter.currentList.map { it.color }.toIntArray()) })
        finish()
    }
}