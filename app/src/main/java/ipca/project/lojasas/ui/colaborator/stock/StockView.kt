package ipca.project.lojasas.ui.colaborator.stock

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun StockView(
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
) {
    Text("isto e a StockView")
}