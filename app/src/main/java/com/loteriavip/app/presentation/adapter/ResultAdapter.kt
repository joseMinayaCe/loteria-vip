package com.loteriavip.app.presentation.adapter

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.loteriavip.app.R
import com.loteriavip.app.databinding.ItemResultBinding
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.loteriavip.app.domain.model.LiveLotteryResult

class ResultAdapter(
    private var items: List<Any>,
    private val onFavoriteClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_RESULT = 0
        private const val VIEW_TYPE_AD = 1
    }

    class ResultViewHolder(val binding: ItemResultBinding) : RecyclerView.ViewHolder(binding.root)
    
    class AdViewHolder(val view: NativeAdView) : RecyclerView.ViewHolder(view) {
        val appIcon = view.findViewById<android.widget.ImageView>(R.id.ad_app_icon)
        val headline = view.findViewById<TextView>(R.id.ad_headline)
        val body = view.findViewById<TextView>(R.id.ad_body)
        val callToAction = view.findViewById<android.widget.Button>(R.id.ad_call_to_action)
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is NativeAd) VIEW_TYPE_AD else VIEW_TYPE_RESULT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_AD) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_native_ad, parent, false) as NativeAdView
            AdViewHolder(view)
        } else {
            val binding = ItemResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ResultViewHolder(binding)
        }
    }

    data class CompanyInfo(val label: String, val colorHex: String)

    private fun getCompanyInfo(companyClass: String?): CompanyInfo {
        return when (companyClass) {
            "company-block-10" -> CompanyInfo("Nacional", "#3bb34a") // Green
            "company-block-9" -> CompanyInfo("Leidsa", "#ffb300") // Yellow
            "company-block-11" -> CompanyInfo("Lotería Real", "#0d47a1") // Dark Blue
            "company-block-12" -> CompanyInfo("Loteka", "#00bcd4") // Light Blue
            "company-block-13" -> CompanyInfo("Americanas", "#757575") // Grey
            "company-block-98" -> CompanyInfo("La Primera", "#e53935") // Red
            "company-block-106" -> CompanyInfo("La Suerte", "#1a237e") // Navy Blue
            "company-block-114" -> CompanyInfo("LoteDom", "#002fa7") // Royal Blue
            "company-block-120" -> CompanyInfo("Anguila", "#ff6d00") // Orange
            "company-block-124" -> CompanyInfo("King Lottery", "#0066cc") // Blue
            "company-block-19" -> CompanyInfo("Nueva York", "#1565c0") // Dark Blue
            "company-block-20" -> CompanyInfo("Florida", "#ef6c00") // Orange
            else -> CompanyInfo("", "#757575")
        }
    }

    private fun shouldShowVote(name: String): Boolean {
        val n = name.lowercase()
        return n.contains("quiniela") || n.contains("gana más") || n.contains("nacional") || n.contains("primera") || n.contains("suerte") || n.contains("lotedom") || n.contains("anguila") || n.contains("king lottery")
    }

    private fun getBallColors(name: String, index: Int, total: Int): Pair<String, String> {
        val n = name.lowercase()
        if (n.contains("juega + pega")) {
            return when (index) {
                0, 1 -> Pair("#2196F3", "#FFFFFF") // Blue background, White text
                2, 3 -> Pair("#F44336", "#FFFFFF") // Red background, White text
                else -> Pair("#4CAF50", "#FFFFFF") // Green background, White text
            }
        }
        if (n.contains("powerball") && index == total - 1) {
            return Pair("#F44336", "#FFFFFF") // Red background, White text
        }
        if ((n.contains("loto") || n.contains("mega")) && !n.contains("loto pool")) {
            if (index == total - 2) return Pair("#F44336", "#FFFFFF") // Red background, White text
            if (index == total - 1) return Pair("#2196F3", "#FFFFFF") // Blue background, White text
        }
        return Pair("#4CAF50", "#FFFFFF") // Green background, White text
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        
        if (holder is AdViewHolder && item is NativeAd) {
            holder.view.iconView = holder.appIcon
            holder.view.headlineView = holder.headline
            holder.view.bodyView = holder.body
            holder.view.callToActionView = holder.callToAction
            
            holder.headline?.text = item.headline
            holder.body?.text = item.body
            holder.callToAction?.text = item.callToAction
            
            if (item.icon != null) {
                holder.appIcon?.setImageDrawable(item.icon?.drawable)
                holder.appIcon?.visibility = View.VISIBLE
            } else {
                holder.appIcon?.visibility = View.INVISIBLE
            }
            
            holder.view.setNativeAd(item)
            return
        }
        
        if (holder is ResultViewHolder && item is LiveLotteryResult) {
            val result = item
            val context = holder.itemView.context
            
            with(holder.binding) {
            val companyInfo = getCompanyInfo(result.companyClass)
            val companyColor = Color.parseColor(companyInfo.colorHex)
            
            cardView.strokeColor = companyColor
            layoutHeaderBar.setBackgroundColor(companyColor)
            
            var previousCompanyClass: String? = null
            for (i in position - 1 downTo 0) {
                val prev = items[i]
                if (prev is LiveLotteryResult) {
                    previousCompanyClass = prev.companyClass
                    break
                }
            }
            val showCompanyLabel = previousCompanyClass == null || result.companyClass != previousCompanyClass
            if (showCompanyLabel && companyInfo.label.isNotEmpty()) {
                txtCompanyHeader.visibility = View.VISIBLE
                txtCompanyHeader.text = companyInfo.label
            } else {
                txtCompanyHeader.visibility = View.GONE
            }
            
            val isQuiniela = shouldShowVote(result.name)
            layoutVoteSection.visibility = if (isQuiniela) View.VISIBLE else View.GONE
            
            btnVoteUp.setOnClickListener {
                Toast.makeText(context, "Marcado como ganado. ¡Éxito!", Toast.LENGTH_SHORT).show()
            }
            btnVoteDown.setOnClickListener {
                Toast.makeText(context, "Marcado como no ganado. ¡Suerte para la próxima!", Toast.LENGTH_SHORT).show()
            }
            
            txtLotteryName.text = result.name
            txtDrawDate.text = result.dateText
            imgVerified.visibility = if (result.isVerified) View.VISIBLE else View.GONE
            
            if (!result.logoUrl.isNullOrEmpty()) {
                imgLogo.visibility = View.VISIBLE
                imgLogo.load(result.logoUrl)
            } else {
                imgLogo.visibility = View.GONE
            }
            
            layoutNumbers.removeAllViews()
            
            if (result.numbers.isEmpty()) {
                val pendingView = LayoutInflater.from(context).inflate(R.layout.item_pending_draw, layoutNumbers, false)
                layoutNumbers.addView(pendingView)
            } else {
                val isManyNumbers = result.numbers.size > 5
                result.numbers.forEachIndexed { index, number ->
                    val bubbleView = LayoutInflater.from(context).inflate(R.layout.item_number_bubble, layoutNumbers, false) as TextView
                    bubbleView.text = number.toString().padStart(2, '0')
                    
                    if (isManyNumbers) {
                        val density = context.resources.displayMetrics.density
                        val params = bubbleView.layoutParams as ViewGroup.MarginLayoutParams
                        params.width = (32 * density).toInt()
                        params.height = (32 * density).toInt()
                        params.setMargins((2 * density).toInt(), (2 * density).toInt(), (2 * density).toInt(), (2 * density).toInt())
                        bubbleView.layoutParams = params
                        bubbleView.textSize = 14f
                    }
                    
                    if (result.isPast) {
                        bubbleView.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
                        bubbleView.setTextColor(Color.parseColor("#616161"))
                    } else {
                        val (bgColorHex, textColorHex) = getBallColors(result.name, index, result.numbers.size)
                        bubbleView.backgroundTintList = ColorStateList.valueOf(Color.parseColor(bgColorHex))
                        bubbleView.setTextColor(Color.parseColor(textColorHex))
                    }
                    
                    layoutNumbers.addView(bubbleView)
                }
            }
            
            btnFavorite.setImageResource(
                if (result.isFavorite) R.drawable.ic_star 
                else R.drawable.ic_star_border
            )
            
            btnFavorite.setOnClickListener { onFavoriteClick(result.id) }

            btnShare.setOnClickListener {
                val numbersStr = if (result.numbers.isNotEmpty()) {
                    result.numbers.joinToString(" - ") { it.toString().padStart(2, '0') }
                } else {
                    "Pendiente"
                }

                val shareContent = """
                    🎰 *Lotería VIP* 🎰

                    📌 *${result.name}*
                    📅 Fecha: ${result.dateText}

                    🎯 *Números Ganadores:*
                    ✨ $numbersStr ✨

                    📲 ¡Descarga *Lotería VIP* para ver los resultados en vivo!
                """.trimIndent()

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Resultado: ${result.name}")
                    putExtra(Intent.EXTRA_TEXT, shareContent)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Compartir resultado vía"))
            }
        }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }
}
