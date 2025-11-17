package ipca.project.lojasas.ui.profile

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun ProfileView(
    navController: NavController,
    modifier: Modifier = Modifier
) {

    val viewModel: ProfileViewModel = viewModel()


    Text("isto e a ProfileView")

    Button(
        onClick = {
            viewModel.logout {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    ) {
        Text("logout")
    }
}