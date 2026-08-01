package com.loteriavip.app.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.loteriavip.app.R
import com.loteriavip.app.databinding.ItemHotNumberBinding
import com.loteriavip.app.domain.model.HotNumber

class StatsAdapter(
    private var items: List<HotNumber>
) : RecyclerView.Adapter<StatsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHotNumberBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHotNumberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        
        with(holder.binding) {
            val numFormatted = item.number.toString().padStart(2, '0')
            txtStatNumber.text = numFormatted
            txtStatTitle.text = "Número $numFormatted"
            
            // Calculate progress out of 100
            val progressVal = (item.trend * 100).toInt().coerceIn(0, 100)
            progressBarFrequency.progress = progressVal
            
            txtStatCount.text = "${item.frequency} veces"
            
            if (item.trend >= 0.5f) {
                imgStatTrend.setImageResource(R.drawable.ic_arrow_up)
                imgStatTrend.setColorFilter(ContextCompat.getColor(context, R.color.verified_green), android.graphics.PorterDuff.Mode.SRC_IN)
                txtTrendLabel.text = "Alta Frecuencia"
            } else {
                imgStatTrend.setImageResource(R.drawable.ic_arrow_down)
                imgStatTrend.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark), android.graphics.PorterDuff.Mode.SRC_IN)
                txtTrendLabel.text = "Baja Frecuencia"
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<HotNumber>) {
        items = newItems
        notifyDataSetChanged()
    }
}
