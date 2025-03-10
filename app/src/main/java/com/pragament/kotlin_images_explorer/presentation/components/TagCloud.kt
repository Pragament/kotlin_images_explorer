package com.pragament.kotlin_images_explorer.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow
import io.ktor.websocket.Frame

@Composable
fun TagCloud(
    tags: List<String> = emptyList(),
    onTagSelected: (String) -> Unit = {}
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        mainAxisSpacing = 8.dp,
        crossAxisSpacing = 8.dp
    ) {
        tags.forEach { tag ->
            SuggestionChip(
                onClick = { onTagSelected(tag) },
                label = { Frame.Text(tag) }
            )
        }
    }
}