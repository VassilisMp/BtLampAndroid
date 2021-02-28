package gr.cs.btlamp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.larswerkman.holocolorpicker.ColorPicker
import gr.cs.btlamp.R
import kotlinx.android.synthetic.main.activity_sequence_picker.*


private const val TAG = "SequencePickerActivity"
class SequencePickerActivity : AppCompatActivity() {

    /*private val itemTouchHelper by lazy {  // 1. Note that I am specifying all 4 directions.
        //    Specifying START and END also allows
        //    more organic dragging than just specifying UP and DOWN.
        ItemTouchHelper(ColorsAdapter.ItemTouchHelperCallback())
    }*/

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
            setHasFixedSize(false)
            colorsAdapter.itemTouchHelper.attachToRecyclerView(this)
        }
        //to turn of showing the old color
        picker.run {
            showOldCenterColor = false
            addSVBar(svbar)
            addOpacityBar(opacitybar)
            onColorChangedListener = ColorPicker.OnColorChangedListener { currentColor = it }
            // init currentColor
            currentColor = color
        }
        pick_button.setOnClickListener {
            /*val newList = colorsAdapter.currentList.toMutableList().apply {
                add(Color(currentColor))
            }
            colorsAdapter.submitList(newList)*/
            colorsAdapter.colorList.add(Color(currentColor))
            // TODO don' t use notifyDataSetChanged
            colorsAdapter.notifyDataSetChanged()
//            colorsAdapter.notifyItemInserted(colorsAdapter.colorList.lastIndex)
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
            Intent().apply { putExtra(SEQUENCE, colorsAdapter.colorList.map { it.color }.toIntArray()) })
        finish()
    }
}