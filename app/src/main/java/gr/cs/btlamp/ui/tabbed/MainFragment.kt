package gr.cs.btlamp.ui.tabbed

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.larswerkman.holocolorpicker.ColorPicker
import gr.cs.btlamp.*
import gr.cs.btlamp.ui.TabbedActivity
import kotlinx.android.synthetic.main.fragment_tabbed.view.*

/**
 * A placeholder fragment containing a simple view.
 */
class MainFragment : Fragment(), ColorPicker.OnColorChangedListener, View.OnClickListener {

    private lateinit var pageViewModel: PageViewModel
    private val mService: MyBluetoothService?
        get() = (activity as? TabbedActivity)?.mService
    private val power: Boolean?
        get() = activity?.findViewById<ToggleButton>(R.id.switchButton)?.isChecked

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageViewModel = ViewModelProvider(this).get(PageViewModel::class.java).apply {
            setIndex(arguments?.getInt(ARG_SECTION_NUMBER) ?: 1)
        }
    }

    /*override fun onAttach(context: Context) {
        super.onAttach(context)
        mService = (context as TabbedActivity).mService
    }

    override fun onDetach() {
        super.onDetach()
        mService = null
    }*/

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_tabbed, container, false)
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, periodicFunNames).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            root.spinner_periodic_fun.adapter = adapter
            root.spinner_periodic_fun.setSelection(adapter.getPosition(SQUARE))
            root.spinner_periodic_fun.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    when(parent.getItemAtPosition(pos).toString()) {
                        SINE -> mService?.btApi?.enableSine()
                        COSINE -> mService?.btApi?.enableCosine()
                        TANGENT -> mService?.btApi?.enableTangent()
                        SQUARE -> mService?.btApi?.enableSquare()
                        TRIANGLE -> mService?.btApi?.enableTriangle()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
        ArrayAdapter(context!!, android.R.layout.simple_spinner_item, RANDOM_MODES).run {
            // Specify the layout to use when the list of choices appears
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            root.spinner_random_modes.adapter = this
            root.spinner_random_modes.setSelection(this.getPosition(RANDOM_COLOR_1))
            root.spinner_random_modes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    when(parent.getItemAtPosition(pos).toString()) {
                        RANDOM_COLOR_0 -> mService?.btApi?.enableRandomColorContinuous()
                        RANDOM_COLOR_1 -> mService?.btApi?.enableRandomColor()
                        RANDOM_COLOR_2 -> mService?.btApi?.enableRandomColor2()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }
        root.on_off_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val newProgress = if (progress > 0) {
                    root.spinner_periodic_fun.visibility = View.VISIBLE
                    progress + 80
                } else {
                    root.spinner_periodic_fun.visibility = View.GONE
                    0
                }
                root.on_off_value_text.text = newProgress.toString()
                mService?.btApi?.changePowerInterval(newProgress)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        root.picker.run {
            addSVBar(root.svbar)
            addOpacityBar(root.opacitybar)
            onColorChangedListener = this@MainFragment
            showOldCenterColor = false
        }
        root.random_color.setOnClickListener(this)
        /*val textView: TextView = root.findViewById(R.id.section_label)
        pageViewModel.text.observe(this, {
            textView.text = it
        })*/
        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onColorChanged(color: Int) {
        //gives the color when it's changed.
        val red = Color.red(view!!.picker.color).toByte()
        val green = Color.green(view!!.picker.color).toByte()
        val blue = Color.blue(view!!.picker.color).toByte()
        val alpha = Color.alpha(view!!.picker.color).toByte()

//        text.setTextColor(picker.color)
//        textView_color.text = "%02x%02x%02x%02x".format(red, green, blue, alpha)
        if (power == true) mService?.btApi?.changeColor(red, green, blue, alpha)
    }

    override fun onClick(view: View?) {
        when(view) {
            getView()!!.random_color -> {
                if (getView()!!.random_color.isChecked) {
                    getView()!!.spinner_random_modes.run {
                        // reselect the selected item to run the proper btApi function
                        onItemSelectedListener?.onItemSelected(this, selectedView, selectedItemPosition, selectedItemId)
                        visibility = View.VISIBLE
                    }
                } else {
                    getView()!!.spinner_random_modes.visibility = View.GONE
                    mService?.btApi?.disableRandomColor()
                }
            }
        }
    }

    companion object {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private const val ARG_SECTION_NUMBER = "section_number"

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        @JvmStatic
        fun newInstance(sectionNumber: Int): MainFragment {
            return MainFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SECTION_NUMBER, sectionNumber)
                }
            }
        }
    }
}