package ipca.project.lojasas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Outlined.Info
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícone Grande
        Icon(
            imageVector = icon,
            contentDescription = null,
            // Usa a cor base (Preto/Branco) com 30% de opacidade.
            // Light Mode: Fica cinza claro.
            // Dark Mode: Fica branco translúcido (visível no fundo escuro).
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mensagem de Texto
        Text(
            text = message,
            // Usa a cor base com 60% de opacidade para parecer texto secundário.
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontSize = 16.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    // Para testar o tema corretamente no preview, terias de envolver com LojaSASTheme
    EmptyState(message = "Nenhum item para mostrar")
}