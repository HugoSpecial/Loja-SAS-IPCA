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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.project.lojasas.ui.authentication.LoginView
import ipca.project.lojasas.ui.authentication.LoginViewModel
import ipca.project.lojasas.ui.candidature.CandidatureView
import ipca.project.lojasas.ui.components.MyBottomBar
import ipca.project.lojasas.ui.home.HomeView
import ipca.project.lojasas.ui.profile.ProfileView
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

            // Estado para controlar a visibilidade do BottomBar
            val shouldShowBottomBar = remember { mutableStateOf(false) }

            LojaSASTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Só mostra o BottomBar se shouldShowBottomBar for true
                        if (shouldShowBottomBar.value) {
                            MyBottomBar(
                                navController = navController,
                                onAddClick = {
                                    println("Botão + clicado!")
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("login") {
                            LoginView(navController = navController)
                            LaunchedEffect(Unit) {
                                shouldShowBottomBar.value = false
                            }
                        }

                        composable("candidature") {
                            CandidatureView(navController = navController)
                            LaunchedEffect(Unit) {
                                shouldShowBottomBar.value = false
                            }
                        }

                        composable("home") {
                            HomeView(navController = navController)
                            LaunchedEffect(Unit) {
                                shouldShowBottomBar.value = true
                            }
                        }

                        composable("profile") {
                            ProfileView(navController = navController)
                            LaunchedEffect(Unit) {
                                shouldShowBottomBar.value = true
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
}