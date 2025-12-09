package ipca.project.lojasas.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ipca.project.lojasas.ui.collaborator.campaigns.TextDark

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black, modifier = Modifier.width(140.dp))
        Text(value.ifEmpty { "-" }, fontSize = 15.sp, color = TextDark, modifier = Modifier.weight(1f))
    }
}