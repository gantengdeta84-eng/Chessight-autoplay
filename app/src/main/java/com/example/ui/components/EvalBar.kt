package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.EngineLine
import com.example.ui.theme.EvalBlack
import com.example.ui.theme.EvalWhite
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun VerticalEvalBar(
    topLine: EngineLine?,
    isFlipped: Boolean,
    modifier: Modifier = Modifier
) {
    val cd = stringResource(R.string.cd_eval_bar)
    val rawWhiteFraction = computeWhiteFraction(topLine)
    val animatedWhiteFraction by animateFloatAsState(
        targetValue = rawWhiteFraction,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "eval_bar_fraction"
    )
    val label = topLine?.formattedEval() ?: "0.0"
    val whiteFavored = (topLine?.mateIn?.let { it > 0 } ?: ((topLine?.evalScorePawns ?: 0.0) >= 0.0))

    Box(
        modifier = modifier
            .width(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(EvalBlack)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .semantics { contentDescription = cd }
    ) {
        // When not flipped: White is at bottom, Black at top.
        // When flipped: Black is at bottom, White at top.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedWhiteFraction)
                .align(if (isFlipped) Alignment.TopCenter else Alignment.BottomCenter)
                .background(EvalWhite)
        )

        val textAlign = when {
            isFlipped && whiteFavored -> Alignment.TopCenter
            isFlipped && !whiteFavored -> Alignment.BottomCenter
            !isFlipped && whiteFavored -> Alignment.BottomCenter
            else -> Alignment.TopCenter
        }
        val textColor = if (whiteFavored) Color(0xFF0D1117) else Color(0xFFF0F6FC)

        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = textColor,
            maxLines = 1,
            modifier = Modifier
                .align(textAlign)
                .padding(vertical = 4.dp)
        )
    }
}

private fun computeWhiteFraction(line: EngineLine?): Float {
    if (line == null) return 0.5f
    line.mateIn?.let { mate ->
        return if (mate > 0) 0.97f else 0.03f
    }
    return (line.winChanceWhite / 100.0).toFloat().coerceIn(0.05f, 0.95f)
}
