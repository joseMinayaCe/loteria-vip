package com.loteriavip.app.presentation.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.loteriavip.app.R
import com.loteriavip.app.domain.model.LiveLotteryResult

private data class CompanyInfo(val label: String, val color: Color)

private fun getCompanyInfo(companyClass: String?): CompanyInfo {
    return when (companyClass) {
        "company-block-10" -> CompanyInfo("Nacional", Color(0xFF3BB34A)) // Green
        "company-block-9" -> CompanyInfo("Leidsa", Color(0xFFFFB300)) // Yellow
        "company-block-11" -> CompanyInfo("Lotería Real", Color(0xFF0D47A1)) // Dark Blue
        "company-block-12" -> CompanyInfo("Loteka", Color(0xFF00BCD4)) // Light Blue
        "company-block-13" -> CompanyInfo("Americanas", Color(0xFF757575)) // Grey
        "company-block-98" -> CompanyInfo("La Primera", Color(0xFFE53935)) // Red
        "company-block-106" -> CompanyInfo("La Suerte", Color(0xFF1A237E)) // Navy Blue
        "company-block-114" -> CompanyInfo("LoteDom", Color(0xFF002FA7)) // Royal Blue
        "company-block-120" -> CompanyInfo("Anguila", Color(0xFFFF6D00)) // Orange
        "company-block-124" -> CompanyInfo("King Lottery", Color(0xFF0066CC)) // Blue
        "company-block-19" -> CompanyInfo("Nueva York", Color(0xFF1565C0)) // Dark Blue
        "company-block-20" -> CompanyInfo("Florida", Color(0xFFEF6C00)) // Orange
        else -> CompanyInfo("", Color(0xFF757575))
    }
}

private fun shouldShowVote(name: String): Boolean {
    val n = name.lowercase()
    return n.contains("quiniela") || n.contains("gana más") || n.contains("nacional") || n.contains("primera") || n.contains("suerte") || n.contains("lotedom") || n.contains("anguila") || n.contains("king lottery")
}

private fun getBallColors(name: String, index: Int, total: Int): Pair<Color, Color> {
    val n = name.lowercase()
    if (n.contains("juega + pega")) {
        return when (index) {
            0, 1 -> Pair(Color(0xFF2196F3), Color.White) // Blue background, White text
            2, 3 -> Pair(Color(0xFFF44336), Color.White) // Red background, White text
            else -> Pair(Color(0xFF4CAF50), Color.White) // Green background, White text
        }
    }
    if (n.contains("powerball") && index == total - 1) {
        return Pair(Color(0xFFF44336), Color.White) // Red background, White text
    }
    if ((n.contains("loto") || n.contains("mega")) && !n.contains("loto pool")) {
        if (index == total - 2) return Pair(Color(0xFFF44336), Color.White) // Red background, White text
        if (index == total - 1) return Pair(Color(0xFF2196F3), Color.White) // Blue background, White text
    }
    return Pair(Color(0xFF4CAF50), Color.White) // Green background, White text
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LiveResultCard(
    result: LiveLotteryResult,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val companyInfo = getCompanyInfo(result.companyClass)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.5.dp, companyInfo.color),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(companyInfo.color)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (companyInfo.label.isNotEmpty()) {
                    Text(
                        text = companyInfo.label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                if (shouldShowVote(result.name)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "¿Ganaste?",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(0xFF00C853), CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Marcado como ganado. ¡Éxito!", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_thumb_up),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(Color(0xFFD50000), CircleShape)
                                .clickable {
                                    Toast.makeText(context, "Marcado como no ganado. ¡Suerte!", Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_thumb_down),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Main Content Area
            Column(modifier = Modifier.padding(12.dp)) {
                // Top Row: Date Pill, Stats, Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Pill
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF455A64), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = result.dateText,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Stats Pill
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF455A64), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_stats),
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "ESTADÍSTICAS",
                                color = Color(0xFFE0E0E0),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Logo image loaded from web
                    if (!result.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = result.logoUrl,
                            contentDescription = result.name,
                            modifier = Modifier.size(width = 130.dp, height = 52.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Mid Row: Name & Favorite
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = result.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        if (result.isVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verificado",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (result.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (result.isFavorite) Color.Red else Color.LightGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Bottom Row: Dynamic Numbers Row or Pending State
                if (result.numbers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Sorteo en espera",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                } else {
                    val isManyNumbers = result.numbers.size > 5
                    val bubbleSize = if (isManyNumbers) 32.dp else 38.dp
                    val bubbleTextSize = if (isManyNumbers) 13.sp else 16.sp
                    val bubblePadding = if (isManyNumbers) 2.dp else 4.dp
                    
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Center,
                        maxItemsInEachRow = Int.MAX_VALUE
                    ) {
                        result.numbers.forEachIndexed { index, number ->
                            val (bgColor, textColor) = if (result.isPast) {
                                Pair(Color(0xFFE0E0E0), Color(0xFF616161))
                            } else {
                                getBallColors(result.name, index, result.numbers.size)
                            }

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = bubblePadding, vertical = bubblePadding)
                                    .size(bubbleSize)
                                    .background(bgColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = number.toString().padStart(2, '0'),
                                    fontSize = bubbleTextSize,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
