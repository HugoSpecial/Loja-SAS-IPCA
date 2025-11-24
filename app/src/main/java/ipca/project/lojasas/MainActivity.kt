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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ipca.project.lojasas.ui.authentication.LoginView
import ipca.project.lojasas.ui.authentication.LoginViewModel
import ipca.project.lojasas.ui.candidature.AwaitCandidatureView
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

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            LojaSASTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val showBottomBar = currentRoute in listOf("home", "notification", "history", "profile")

                        if (showBottomBar) {
                            MyBottomBar(
                                navController = navController,
                                currentRoute = currentRoute,
                                onAddClick = {
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

                        composable("await-candidature") {
                            AwaitCandidatureView(navController = navController)
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

                LaunchedEffect(Unit) {
                    val user = Firebase.auth.currentUser

                    if (user != null) {
                        coroutineScope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()

                                val document = db.collection("users").document(user.uid).get().await()

                                val isCollaborator = document.getBoolean("isCollaborator") ?: false
                                val isBeneficiary = document.getBoolean("isBeneficiary") ?: false
                                val candidatureId = document.getString("candidature")

                                if (isCollaborator) {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                                else if (isBeneficiary) {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                                else {
                                    if (!candidatureId.isNullOrEmpty()) {
                                        navController.navigate("await-candidature") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("candidature") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}