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
import androidx.compose.runtime.getValue // Necessário para o 'by'
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState // IMPORTANTE
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.project.lojasas.ui.authentication.LoginView
import ipca.project.lojasas.ui.authentication.LoginViewModel
import ipca.project.lojasas.ui.candidature.CandidatureView
import ipca.project.lojasas.ui.components.MyBottomBar
import ipca.project.lojasas.ui.history.HistoryView
import ipca.project.lojasas.ui.home.HomeView
import ipca.project.lojasas.ui.newBasket.NewBasketView
import ipca.project.lojasas.ui.notifications.NotificationView
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

            // --- LÓGICA DE NAVEGAÇÃO MELHORADA ---
            // Observa a pilha de navegação para saber onde estamos em tempo real
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            LojaSASTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        // Lógica mais limpa para mostrar/esconder a barra
                        // Se a rota atual estiver nesta lista, mostra a barra
                        val showBottomBar = currentRoute in listOf("home", "notification", "history", "profile")

                        if (showBottomBar) {
                            MyBottomBar(
                                navController = navController,
                                currentRoute = currentRoute, // Passamos a rota para o ícone acender corretamente
                                onAddClick = {
                                    // Ação do botão do carrinho
                                    // Ex: navController.navigate("cart")
                                    println("Botão Carrinho clicado!")
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
                        }

                        composable("candidature") {
                            CandidatureView(navController = navController)
                        }

                        composable("home") {
                            HomeView(navController = navController)
                        }

                        composable("notification") {
                            NotificationView(navController = navController)
                        }

                        composable("newbasket") {
                            NewBasketView(navController = navController)
                        }

                        composable("history") {
                            HistoryView(navController = navController)
                        }

                        composable("profile") {
                            ProfileView(navController = navController)
                        }
                    }
                }

                // Verifica utilizador
                LaunchedEffect(Unit) {
                    val userID = Firebase.auth.currentUser?.uid
                    if (userID != null) {
                        coroutineScope.launch {
                            val isBeneficiary = loginViewModel.isBeneficiario(userID)
                            if (isBeneficiary == true) {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            } else {
                                navController.navigate("candidature") {
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