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
    showClear: Boolean,
    onQuery: (String) -> Unit,
    onRow: (SearchRow) -> Unit,
    onClear: () -> Unit,
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
                        text = if (field == Field.A) "Saan ka manggagaling?" else "Saan ka pupunta?",
                        color = colors.faint,
                        fontSize = 16.sp,
                    )
                }
                inner()
            },
        )
        if (showClear) {
            Text(
                text = "Alisin",
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
                SearchRow.UseMyLocation -> Text(
                    text = "Gamitin ang location ko",
                    color = colors.ink,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRow(row) }
                        .padding(vertical = 6.dp),
                )
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
    }
}
