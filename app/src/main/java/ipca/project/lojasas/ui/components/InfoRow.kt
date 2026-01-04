package ipca.project.lojasas.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Rótulo em Negrito
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            // Cor principal do texto (Preto ou Branco)
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(140.dp)
        )

        // Valor
        Text(
            text = value.ifEmpty { "-" },
            fontSize = 15.sp,
            // Cor ligeiramente mais suave (Cinza escuro ou Branco acinzentado)
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
    }
}