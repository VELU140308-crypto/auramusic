package com.aura.music.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aura.music.data.model.SortBy
import com.aura.music.data.model.SortOption
import com.aura.music.data.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortMenuBottomSheet(
    currentSortOption: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Sort Songs By",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sort order chips (Ascending / Descending)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = currentSortOption.sortOrder == SortOrder.ASCENDING,
                    onClick = {
                        onSortSelected(currentSortOption.copy(sortOrder = SortOrder.ASCENDING))
                    },
                    label = { Text("Ascending") },
                    leadingIcon = {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = null)
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                FilterChip(
                    selected = currentSortOption.sortOrder == SortOrder.DESCENDING,
                    onClick = {
                        onSortSelected(currentSortOption.copy(sortOrder = SortOrder.DESCENDING))
                    },
                    label = { Text("Descending") },
                    leadingIcon = {
                        Icon(Icons.Rounded.ArrowDownward, contentDescription = null)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))

            val sortOptions = listOf(
                SortBy.TITLE to "Song Title",
                SortBy.ARTIST to "Artist Name",
                SortBy.ALBUM to "Album Title",
                SortBy.DATE_ADDED to "Date Added",
                SortBy.DURATION to "Track Duration"
            )

            sortOptions.forEach { (option, label) ->
                val isSelected = currentSortOption.sortBy == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSortSelected(currentSortOption.copy(sortBy = option))
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
