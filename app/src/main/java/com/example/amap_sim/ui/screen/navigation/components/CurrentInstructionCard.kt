package com.example.amap_sim.ui.screen.navigation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.amap_sim.domain.model.InstructionSign
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteInstruction
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapGreen
import com.example.amap_sim.ui.theme.AmapSimTheme
import com.example.amap_sim.ui.theme.MapMarkerEnd

/**
 * 当前导航指令卡片
 * 
 * 显示在导航页面顶部，包含：
 * - 距离提示（如 "300米后"）
 * - 转向图标
 * - 指令文字（如 "左转进入中山大道"）
 * - 下一条指令预览
 */
@Composable
fun CurrentInstructionCard(
    instruction: RouteInstruction?,
    nextInstruction: RouteInstruction?,
    distanceToNext: String,
    modifier: Modifier = Modifier
) {
    if (instruction == null) return
    
    val isArriving = instruction.sign == InstructionSign.ARRIVE
    
    // 背景渐变色
    val gradientColors = if (isArriving) {
        listOf(AmapGreen, AmapGreen.copy(alpha = 0.9f))
    } else {
        listOf(AmapBlue, AmapBlue.copy(alpha = 0.9f))
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(gradientColors)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // 主指令区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 转向图标
                    InstructionIconLarge(
                        sign = instruction.sign,
                        modifier = Modifier.size(72.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        // 距离提示
                        if (!isArriving) {
                            Text(
                                text = distanceToNext,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 36.sp
                            )
                        }
                        
                        // 指令文字
                        Text(
                            text = instruction.text,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        // 道路名称
                        instruction.streetName?.let { street ->
                            if (street.isNotBlank() && !instruction.text.contains(street)) {
                                Text(
                                    text = street,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                
                // 下一条指令预览
                if (nextInstruction != null && !isArriving) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    NextInstructionPreview(
                        instruction = nextInstruction,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 大号转向图标
 */
@Composable
private fun InstructionIconLarge(
    sign: InstructionSign,
    modifier: Modifier = Modifier
) {
    val icon = getInstructionIcon(sign)
    val isArriving = sign == InstructionSign.ARRIVE
    
    // 到达时的动画效果
    val scale by animateFloatAsState(
        targetValue = if (isArriving) 1.1f else 1f,
        animationSpec = tween(500),
        label = "icon_scale"
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = sign.description,
            modifier = Modifier.size(48.dp),
            tint = Color.White
        )
    }
}

/**
 * 下一条指令预览
 */
@Composable
private fun NextInstructionPreview(
    instruction: RouteInstruction,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "然后",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Icon(
            imageVector = getInstructionIcon(instruction.sign),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = instruction.text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        if (instruction.distance > 0) {
            Text(
                text = instruction.getFormattedDistance(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * 获取指令对应的图标
 */
private fun getInstructionIcon(sign: InstructionSign): ImageVector {
    return when (sign) {
        InstructionSign.CONTINUE, InstructionSign.STRAIGHT -> Icons.Default.KeyboardArrowUp
        InstructionSign.SLIGHT_LEFT -> Icons.Default.SubdirectoryArrowLeft
        InstructionSign.LEFT, InstructionSign.SHARP_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
        InstructionSign.SLIGHT_RIGHT -> Icons.Default.SubdirectoryArrowRight
        InstructionSign.RIGHT, InstructionSign.SHARP_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
        InstructionSign.U_TURN, InstructionSign.U_TURN_LEFT, InstructionSign.U_TURN_RIGHT -> Icons.Default.Refresh
        InstructionSign.ROUNDABOUT, InstructionSign.LEAVE_ROUNDABOUT -> Icons.Default.Refresh
        InstructionSign.DEPART -> Icons.Default.MyLocation
        InstructionSign.ARRIVE -> Icons.Default.Flag
        InstructionSign.REACHED_VIA -> Icons.Default.Flag
        InstructionSign.KEEP_LEFT -> Icons.Default.SubdirectoryArrowLeft
        InstructionSign.KEEP_RIGHT -> Icons.Default.SubdirectoryArrowRight
        InstructionSign.UNKNOWN -> Icons.Default.KeyboardArrowUp
    }
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
private fun CurrentInstructionCardPreview() {
    AmapSimTheme {
        CurrentInstructionCard(
            instruction = RouteInstruction(
                text = "左转进入中山大道",
                distance = 350.0,
                time = 60000,
                sign = InstructionSign.LEFT,
                location = LatLng(30.5433, 114.3416),
                streetName = "中山大道",
                turnAngle = -90.0
            ),
            nextInstruction = RouteInstruction(
                text = "右转进入解放大道",
                distance = 500.0,
                time = 90000,
                sign = InstructionSign.RIGHT,
                location = LatLng(30.5450, 114.3450),
                streetName = "解放大道",
                turnAngle = 90.0
            ),
            distanceToNext = "300米后"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentInstructionCardArrivePreview() {
    AmapSimTheme {
        CurrentInstructionCard(
            instruction = RouteInstruction(
                text = "到达目的地",
                distance = 0.0,
                time = 0,
                sign = InstructionSign.ARRIVE,
                location = LatLng(30.5355, 114.3645),
                streetName = null,
                turnAngle = null
            ),
            nextInstruction = null,
            distanceToNext = "即将"
        )
    }
}

