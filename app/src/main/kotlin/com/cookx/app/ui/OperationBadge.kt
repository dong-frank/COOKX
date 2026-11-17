package com.cookx.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cookx.app.R
import com.cookx.app.domain.OperationType

@Composable internal fun OperationBadge(operation: OperationType, size: Dp = 36.dp) {
    val drawable = when (operation) {
        OperationType.RINSE -> R.drawable.op_water
        OperationType.CUT, OperationType.SLICE, OperationType.SHRED, OperationType.DICE, OperationType.PEEL -> R.drawable.op_cut
        OperationType.BLANCH, OperationType.SIMMER -> R.drawable.op_pot
        OperationType.MARINATE, OperationType.MIX -> R.drawable.op_mix
        OperationType.STIR_FRY, OperationType.SAUTE -> R.drawable.op_wok
        OperationType.SEASON -> R.drawable.op_season
        OperationType.PLATE -> R.drawable.op_plate
        OperationType.OTHER -> R.drawable.op_other
    }
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Image(painterResource(drawable), contentDescription = operation.label,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
            modifier = Modifier.size(size).padding(7.dp))
    }
}
