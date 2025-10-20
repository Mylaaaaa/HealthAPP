package com.zihanwang.myhealth.presentation.component

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap // <-- 用于把 Drawable 转 Bitmap
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Material 2 card-style row for one exercise session.
 * - Title emphasized
 * - Second line shows time range + duration
 * - Third line shows source app (optional icon)
 * - Whole card navigates to details; trash deletes
 *
 * NOTE: sourceAppIcon is Drawable? to match your caller; we convert inside.
 */
@Composable
fun ExerciseSessionRow(
    start: ZonedDateTime,
    end: ZonedDateTime,
    uid: String,
    name: String,
    sourceAppName: String,
    sourceAppIcon: Drawable?,              // <-- 改成 Drawable?
    onDeleteClick: (String) -> Unit,
    onDetailsClick: (String) -> Unit,
    showId: Boolean = false
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val startText = timeFormatter.format(start)
    val endText = timeFormatter.format(end)
    val durationMin = Duration.between(start, end).toMinutes().coerceAtLeast(0)
    val durationText = "$durationMin mins"

    // 将 Drawable? 安全转换为 Painter（若为 null，就不显示图标）
    val iconPainter = sourceAppIcon?.let {
        val bmp = it.toBitmap()               // core-ktx 的扩展
        BitmapPainter(bmp.asImageBitmap())
    }

    Card(
        elevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick(uid) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading accent dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.85f))
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { "Unnamed session" },
                    style = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1
                )

                Text(
                    text = "$startText – $endText  ·  $durationText",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (iconPainter != null) {
                        Icon(
                            painter = iconPainter,
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(end = 6.dp),
                            tint = Color.Unspecified
                        )
                    }
                    Text(
                        text = "From: $sourceAppName",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (showId) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "id: $uid",
                        style = MaterialTheme.typography.overline,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }

            IconButton(onClick = { onDeleteClick(uid) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete session")
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
