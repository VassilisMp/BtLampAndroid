package gr.cs.btlamp.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import gr.cs.btlamp.R

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
        val holderView: View = view
        val cardView = view.findViewById<CardView>(R.id.color_cardview)
        val layout: ConstraintLayout = view.findViewById(R.id.background_layout)
        val removeImage: ImageView = view.findViewById(R.id.remove_image)
        var color: Color? = null

        fun bind(color: Color, position: Int) {
            this.color = color
//            layout.setBackgroundColor(color.color)
//            holderView.setBackgroundColor(color.color)
//            layout.setBackgroundColor(color.color)
            cardView.setCardBackgroundColor(color.color)
            removeImage.setOnClickListener {
                Log.d(TAG, "removeImage onClick ")
                /*val newList = this@ColorsAdapter.currentList.toMutableList().apply {
                    remove(color)
                }
                submitList(newList)*/
                colorList.remove(color)
//                notifyItemRemoved(position)
                // TODO don't use notifyDataSetChanged
                notifyDataSetChanged()
            }
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
        viewHolder.bind(color, position)
    }

    override fun getItemCount() = colorList.size
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

