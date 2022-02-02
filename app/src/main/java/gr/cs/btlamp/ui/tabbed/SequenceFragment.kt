package gr.cs.btlamp.ui.tabbed

import android.graphics.Color.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.larswerkman.holocolorpicker.ColorPicker
import gr.cs.btlamp.R
import gr.cs.btlamp.android.bluetoothchat.BluetoothService
import gr.cs.btlamp.ui.Color
import gr.cs.btlamp.ui.ColorsAdapter
import kotlinx.android.synthetic.main.fragment_sequence.view.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SequenceFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
private const val TAG = "SequenceFragment"
class SequenceFragment : Fragment() {

    private var currentColor: Int = 0
    private lateinit var colorsAdapter: ColorsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_sequence, container, false)
        //to turn of showing the old color
        root.picker.run {
            showOldCenterColor = false
            addSVBar(root.svbar)
            addOpacityBar(root.opacitybar)
            onColorChangedListener = ColorPicker.OnColorChangedListener { currentColor = it }
            // init currentColor
            currentColor = color
            this.setOnClickListener {
                colorsAdapter.colorList.add(Color(currentColor))
                // TODO don' t use notifyDataSetChanged
                colorsAdapter.notifyDataSetChanged()
            }
            /*this.setOnTouchListener { view, motionEvent ->
                Log.d(TAG, "touch colorpicker")
                when(motionEvent) {
//                    MotionEvent.
                }
                return@setOnTouchListener true
            }*/
        }

        colorsAdapter = ColorsAdapter()
        // TODO implement Drag and reorder RecyclerView items
        root.colors_rv.apply {
            adapter = colorsAdapter
//            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL))
            setHasFixedSize(false)
            colorsAdapter.itemTouchHelper.attachToRecyclerView(this)
        }
        root.pick_button.setOnClickListener {
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

        // save picked colors and send to arduino to play
        with(root.findViewById<SwitchMaterial>(R.id.save_button)) {
            save_button.setOnClickListener {
                with(BluetoothService.getService().btApi) {
                    if (save_button.isChecked)
                        submitColorSequence(
                            *colorsAdapter.colorList.flatMap {
                                byteArrayOf(
                                    red(it.color).toByte(),
                                    green(it.color).toByte(),
                                    blue(it.color).toByte(),
                                    alpha(it.color).toByte()
                                ).asIterable()
                            }.toByteArray()
                        )
                    else removeColorSequence()
                }
            }
        }
        return root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SequenceFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SequenceFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}