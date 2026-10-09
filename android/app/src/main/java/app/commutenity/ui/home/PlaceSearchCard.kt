package app.commutenity.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.commutenity.domain.Field
import app.commutenity.domain.SearchRow
import app.commutenity.ui.theme.LocalCommuteColors

@Composable
fun PlaceSearchCard(
    field: Field,
    query: String,
    rows: List<SearchRow>,
    myLocationName: String,
    showClear: Boolean,
    onQuery: (String) -> Unit,
    onRow: (SearchRow) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalCommuteColors.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, ambientColor = colors.shadow, spotColor = colors.shadow)
            .background(colors.surface, shape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQuery,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = colors.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = if (field == Field.A) "Where from?" else "Where to?",
                        color = colors.faint,
                        fontSize = 16.sp,
                    )
                }
                inner()
            },
        )
        Text(
            text = "Close",
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(vertical = 6.dp),
        )
        if (showClear) {
            Text(
                text = "Remove",
                color = colors.pinA,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClear)
                    .padding(vertical = 6.dp),
            )
        }
        rows.forEach { row ->
            when (row) {
                SearchRow.UseMyLocation -> Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onRow(row) }
                        .padding(vertical = 4.dp),
                ) {
                    Text("Use my location", color = colors.ink, fontSize = 15.sp)
                    Text("Sample: sets $myLocationName, not GPS", color = colors.muted, fontSize = 13.sp)
                }
                is SearchRow.PlaceRow -> Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onRow(row) }
                        .padding(vertical = 4.dp),
                ) {
                    Text(row.place.name, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (row.place.area.isNotEmpty()) {
                        Text(row.place.area, color = colors.muted, fontSize = 13.sp)
                    }
                }
            }
        }
        if (rows.none { it is SearchRow.PlaceRow }) {
            Text(
                text = "No match. Only Makati is in this data.",
                color = colors.muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}
