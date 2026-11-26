package com.rawatsayang.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Kapsul obat, miring 45 derajat, dua belahan dengan celah di tengah. */
val IkonObat: ImageVector by lazy {
    ImageVector.Builder(
        name = "Obat", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        group(rotate = -45f, pivotX = 12f, pivotY = 12f) {
            path(fill = SolidColor(Color.Black)) {
                // belahan kiri
                moveTo(11f, 8f)
                lineTo(7f, 8f)
                arcTo(4f, 4f, 0f, false, false, 7f, 16f)
                lineTo(11f, 16f)
                close()
                // belahan kanan
                moveTo(13f, 8f)
                lineTo(17f, 8f)
                arcTo(4f, 4f, 0f, false, true, 17f, 16f)
                lineTo(13f, 16f)
                close()
            }
        }
    }.build()
}
