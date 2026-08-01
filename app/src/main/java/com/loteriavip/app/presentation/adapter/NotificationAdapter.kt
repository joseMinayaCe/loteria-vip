package com.loteriavip.app.presentation.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.loteriavip.app.R
import com.loteriavip.app.data.local.entity.NotificationEntity
import com.loteriavip.app.databinding.ItemNotificationBinding
import org.json.JSONArray

class NotificationAdapter(
    private var notifications: List<NotificationEntity>,
    private val onClick: (NotificationEntity) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        holder.binding.tvTitle.text = notification.title
        holder.binding.tvBody.text = notification.body
        
        holder.binding.tvTime.text = DateUtils.getRelativeTimeSpanString(
            notification.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )
        
        holder.binding.indicatorUnread.visibility = if (notification.isRead) View.INVISIBLE else View.VISIBLE
        
        // Render numbers
        holder.binding.layoutNumbers.removeAllViews()
        try {
            if (notification.numbersJson != "[]") {
                val jsonArray = JSONArray(notification.numbersJson)
                val context = holder.binding.root.context
                for (i in 0 until jsonArray.length()) {
                    val bubble = LayoutInflater.from(context).inflate(R.layout.item_number_bubble, holder.binding.layoutNumbers, false) as TextView
                    val size = context.resources.getDimensionPixelSize(R.dimen.bubble_size_small)
                    val params = bubble.layoutParams as android.widget.LinearLayout.LayoutParams
                    params.width = size
                    params.height = size
                    params.marginEnd = context.resources.getDimensionPixelSize(R.dimen.bubble_margin)
                    bubble.layoutParams = params
                    
                    val numStr = jsonArray.optString(i, "")
                    val parsedNum = numStr.toIntOrNull()
                    bubble.text = if (parsedNum != null) String.format("%02d", parsedNum) else numStr
                    holder.binding.layoutNumbers.addView(bubble)
                }
            }
        } catch (e: Exception) {}

        holder.binding.root.setOnClickListener {
            onClick(notification)
        }
    }

    override fun getItemCount() = notifications.size

    fun updateData(newNotifications: List<NotificationEntity>) {
        val diffCallback = NotificationDiffCallback(notifications, newNotifications)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notifications = newNotifications
        diffResult.dispatchUpdatesTo(this)
    }

    class NotificationDiffCallback(
        private val oldList: List<NotificationEntity>,
        private val newList: List<NotificationEntity>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition].id == newList[newItemPosition].id
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) = oldList[oldItemPosition] == newList[newItemPosition]
    }
}
