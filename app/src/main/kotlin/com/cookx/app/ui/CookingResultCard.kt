package com.cookx.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cookx.app.R
import com.cookx.app.domain.Recipe
import kotlin.math.cos
import kotlin.math.sin

@Composable internal fun CookingResultCard(recipe: Recipe, firstCooking: Boolean) {
    val shape = RoundedCornerShape(24.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().clip(shape)
        .background(Brush.linearGradient(listOf(Color(0xFF375C45), Color(0xFF172F27))))
        .border(1.dp, Color(0xFF929D73), shape).testTag("cookingResultCard")) {
        Canvas(Modifier.matchParentSize()) {
            val center = Offset(size.width * .27f, size.height * .5f)
            val radius = size.maxDimension * .7f
            drawCircle(Brush.radialGradient(listOf(Color(0xFFDBD59C).copy(alpha = .22f), Color.Transparent), center, radius), radius, center)
            repeat(20) { index ->
                val angle = index * Math.PI / 10
                drawLine(Color(0xFFC8CE8E).copy(alpha = .10f), center,
                    center + Offset(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius), 5.dp.toPx())
            }
        }
        val image = when (recipe.id) {
            "example-tomato-eggs" -> R.drawable.recipe_tomato_eggs
            "example-mushrooms" -> R.drawable.recipe_mushrooms
            else -> R.drawable.recipe_placeholder
        }
        if (maxWidth < 310.dp) {
            Column(Modifier.fillMaxWidth().padding(28.dp).padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Image(painterResource(image), null, Modifier.size(112.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Text(recipe.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFFFFBEA))
            }
        } else {
            Row(Modifier.fillMaxWidth().heightIn(min = 224.dp).padding(horizontal = 26.dp, vertical = 44.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Image(painterResource(image), null, Modifier.size(112.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Text(recipe.name, Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold, color = Color(0xFFFFFBEA))
            }
        }
        if (firstCooking) Box(Modifier.offset(x = (-28).dp, y = 16.dp).rotate(-45f).width(120.dp).height(28.dp)
            .background(Color(0xFFC66A37)).testTag("firstCookingBadge"), contentAlignment = Alignment.Center) {
            Text("NEW", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
