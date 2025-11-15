package ipca.project.lojasas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.project.lojasas.ui.authentication.ForgotPasswordView
import ipca.project.lojasas.ui.authentication.LoginView
import ipca.project.lojasas.ui.authentication.LoginViewModel
import ipca.project.lojasas.ui.candidature.CandidatureView
import ipca.project.lojasas.ui.home.HomeView
import ipca.project.lojasas.ui.theme.LojaSASTheme
import kotlinx.coroutines.launch

const val TAG = "LojaSAS-IPCA"

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()

            LojaSASTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginView(
                                navController = navController
                            )
                        }

                        composable("forgot-password") {
                            ForgotPasswordView(
                                navController = navController
                            )
                        }

                        composable("candidature") {
                            CandidatureView(
                                navController = navController
                            )
                        }

                        composable("home") {
                            HomeView(
                                navController = navController
                            )
                        }
                    }
                }
            }

            // Verificação do usuário atual ao iniciar o app
            LaunchedEffect(Unit) {
                val userID = Firebase.auth.currentUser?.uid
                if (userID != null) {
                    // Verificar se é beneficiário antes de navegar
                    coroutineScope.launch {
                        val isBeneficiary = loginViewModel.isBeneficiario(userID)
                        if (isBeneficiary == true) {
                            navController.navigate("home") {
                                // Limpa o back stack para evitar voltar para login
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            navController.navigate("candidature") {
                                // Limpa o back stack para evitar voltar para login
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }
}