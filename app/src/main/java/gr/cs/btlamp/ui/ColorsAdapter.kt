package gr.cs.btlamp.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import gr.cs.btlamp.R
import java.util.*
import kotlin.collections.ArrayList

private const val TAG = "ColorsAdapter"

class ColorsAdapter(val colorList: ArrayList<Color> = ArrayList()) :
    RecyclerView.Adapter<ColorsAdapter.ViewHolder>() {

    init {
        this.setHasStableIds(true)
    }

    override fun getItemId(position: Int) = colorList[position].hashCode().toLong()

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val cardView = view.findViewById<CardView>(R.id.color_cardview)
        var color: Color? = null

        fun bind(color: Color) {
            this.color = color
            cardView.setCardBackgroundColor(color.color)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.color_layout, viewGroup, false)
//        Log.d(TAG, "onCreateViewHolder")
        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        Log.d(TAG, "position $position")
//        val color = getItem(position)
        val color = colorList[position]
        viewHolder.bind(color)
    }

    override fun getItemCount() = colorList.size

    fun moveItem(from: Int, to: Int) = Collections.swap(colorList, from, to)

    fun removeItem(index: Int) = colorList.removeAt(index)

    inner class ItemTouchHelperCallback : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.START or ItemTouchHelper.END,
        ItemTouchHelper.UP or ItemTouchHelper.DOWN
    ) {

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder): Boolean {

            val adapter = recyclerView.adapter as ColorsAdapter
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition      // 2. Update the backing model. Custom implementation in
            //    MainRecyclerViewAdapter. You need to implement
            //    reordering of the backing model inside the method.
            adapter.moveItem(from, to)      // 3. Tell adapter to render the model update.
            adapter.notifyItemMoved(from, to)

            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder,
                              direction: Int) {
            Log.d(TAG, this::onSwiped.name)
//            if (direction == ItemTouchHelper.UP) {
                val position = viewHolder.adapterPosition
                this@ColorsAdapter.removeItem(position)
                this@ColorsAdapter.notifyItemRemoved(position)
//            }
            // 4. Code block for horizontal swipe.
            //    ItemTouchHelper handles horizontal swipe as well, but
            //    it is not relevant with reordering. Ignoring here.
        }

        // 1. This callback is called when a ViewHolder is selected.
        //    We highlight the ViewHolder here.
        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?,
                                       actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)

            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                viewHolder?.itemView?.alpha = 0.5f
            }
        }

        // 2. This callback is called when the ViewHolder is
        //    unselected (dropped). We unhighlight the ViewHolder here.
        override fun clearView(recyclerView: RecyclerView,
                               viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.alpha = 1.0f
        }
    }

    val itemTouchHelper: ItemTouchHelper by lazy {
        ItemTouchHelper(ItemTouchHelperCallback())
    }
}

object DiffCallback : DiffUtil.ItemCallback<Color>() {
    override fun areItemsTheSame(oldItem: Color, newItem: Color): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: Color, newItem: Color): Boolean {
        return oldItem.color == newItem.color
    }
}

data class Color(val color: Int)

