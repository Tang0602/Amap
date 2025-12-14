package com.example.amap_sim.ui.screen.route.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.amap_sim.domain.model.InstructionSign
import com.example.amap_sim.domain.model.LatLng
import com.example.amap_sim.domain.model.RouteInstruction
import com.example.amap_sim.ui.theme.AmapBlue
import com.example.amap_sim.ui.theme.AmapGreen
import com.example.amap_sim.ui.theme.AmapSimTheme
import com.example.amap_sim.ui.theme.Gray400
import com.example.amap_sim.ui.theme.Gray500
import com.example.amap_sim.ui.theme.MapMarkerEnd

/**
 * 单个导航指令项
 */
@Composable
fun TurnInstructionItem(
    instruction: RouteInstruction,
    index: Int,
    isLast: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // 指令图标
        InstructionIcon(
            sign = instruction.sign,
            isFirst = index == 0,
            isLast = isLast
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 指令内容
        Column(modifier = Modifier.weight(1f)) {
            // 指令文字
            Text(
                text = instruction.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 道路名称
            instruction.streetName?.let { street ->
                if (street.isNotBlank() && !instruction.text.contains(street)) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = street,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                }
            }
        }
        
        // 距离
        if (instruction.distance > 0) {
            Text(
                text = instruction.getFormattedDistance(),
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
    }
}

/**
 * 指令图标
 */
@Composable
private fun InstructionIcon(
    sign: InstructionSign,
    isFirst: Boolean,
    isLast: Boolean
) {
    val (icon, tint) = when {
        isFirst -> Icons.Default.MyLocation to AmapGreen
        isLast -> Icons.Default.Flag to MapMarkerEnd
        else -> getInstructionIcon(sign) to AmapBlue
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = sign.description,
            modifier = Modifier.size(20.dp),
            tint = tint
        )
    }
}

/**
 * 获取指令对应的图标
 */
private fun getInstructionIcon(sign: InstructionSign): ImageVector {
    return when (sign) {
        InstructionSign.CONTINUE, InstructionSign.STRAIGHT -> Icons.Default.KeyboardArrowUp
        InstructionSign.SLIGHT_LEFT -> Icons.Default.SubdirectoryArrowLeft
        InstructionSign.LEFT -> Icons.AutoMirrored.Filled.ArrowBack
        InstructionSign.SHARP_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
        InstructionSign.SLIGHT_RIGHT -> Icons.Default.SubdirectoryArrowRight
        InstructionSign.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
        InstructionSign.SHARP_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
        InstructionSign.U_TURN, InstructionSign.U_TURN_LEFT -> Icons.Default.Refresh
        InstructionSign.U_TURN_RIGHT -> Icons.Default.Refresh
        InstructionSign.ROUNDABOUT, InstructionSign.LEAVE_ROUNDABOUT -> Icons.Default.Refresh
        InstructionSign.DEPART -> Icons.Default.MyLocation
        InstructionSign.ARRIVE -> Icons.Default.Flag
        InstructionSign.REACHED_VIA -> Icons.Default.Flag
        InstructionSign.KEEP_LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
        InstructionSign.KEEP_RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        InstructionSign.UNKNOWN -> Icons.Default.KeyboardArrowUp
    }
}

/**
 * 指令列表分隔线
 */
@Composable
fun InstructionDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = 68.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

// ========== Previews ==========

@Preview(showBackground = true)
@Composable
private fun TurnInstructionItemPreview() {
    AmapSimTheme {
        Column {
            TurnInstructionItem(
                instruction = RouteInstruction(
                    text = "从出发点出发",
                    distance = 0.0,
                    time = 0,
                    sign = InstructionSign.DEPART,
                    location = LatLng(30.5433, 114.3416),
                    streetName = null,
                    turnAngle = null
                ),
                index = 0,
                isLast = false
            )
            InstructionDivider()
            TurnInstructionItem(
                instruction = RouteInstruction(
                    text = "左转进入中山大道",
                    distance = 350.0,
                    time = 60000,
                    sign = InstructionSign.LEFT,
                    location = LatLng(30.5433, 114.3416),
                    streetName = "中山大道",
                    turnAngle = -90.0
                ),
                index = 1,
                isLast = false
            )
            InstructionDivider()
            TurnInstructionItem(
                instruction = RouteInstruction(
                    text = "到达目的地",
                    distance = 0.0,
                    time = 0,
                    sign = InstructionSign.ARRIVE,
                    location = LatLng(30.5455, 114.3500),
                    streetName = null,
                    turnAngle = null
                ),
                index = 2,
                isLast = true
            )
        }
    }
}
