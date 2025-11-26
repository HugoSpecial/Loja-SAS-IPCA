package ipca.project.lojasas

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ipca.project.lojasas.ui.authentication.LoginView
import ipca.project.lojasas.ui.authentication.LoginViewModel
import ipca.project.lojasas.ui.candidature.AwaitCandidatureView
import ipca.project.lojasas.ui.candidature.CandidatureView
import ipca.project.lojasas.ui.colaborator.candidature.CandidatureDetailsView
import ipca.project.lojasas.ui.colaborator.candidature.CandidatureListView
import ipca.project.lojasas.ui.benefeciary.history.HistoryView
import ipca.project.lojasas.ui.benefeciary.home.HomeView
import ipca.project.lojasas.ui.benefeciary.newBasket.NewBasketView
import ipca.project.lojasas.ui.benefeciary.notifications.NotificationView
import ipca.project.lojasas.ui.benefeciary.profile.ProfileView
import ipca.project.lojasas.ui.colaborator.campaigns.CampaignDetailsView
import ipca.project.lojasas.ui.colaborator.campaigns.CampaignsView
import ipca.project.lojasas.ui.colaborator.campaigns.NewCampaignView
import ipca.project.lojasas.ui.colaborator.history.CollatorHistoryView
import ipca.project.lojasas.ui.colaborator.home.ColaboratorHomeView
import ipca.project.lojasas.ui.colaborator.notifications.ColaboratorNotificationView
import ipca.project.lojasas.ui.colaborator.product.ProductView
import ipca.project.lojasas.ui.colaborator.profile.ProfileCollaboratorView
import ipca.project.lojasas.ui.colaborator.stock.StockView
import ipca.project.lojasas.ui.components.BeneficiaryBottomBar
import ipca.project.lojasas.ui.components.CollaboratorBottomBar
// IMPORTANTE: Importa a tua SplashView aqui
import ipca.project.lojasas.ui.components.SplashView
import ipca.project.lojasas.ui.theme.LojaSASTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.getValue

const val TAG = "LojaSAS-IPCA"

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val navController = rememberNavController()
            // Removi o 'coroutineScope' aqui porque o LaunchedEffect já tem o seu próprio scope

            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            LojaSASTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val showBeneficiaryBottomBar = currentRoute in listOf("home", "notification", "history", "profile")

                        val showCollaboratorBottomBar = currentRoute in listOf("colaborador",
                            "notification-collaborador","history-collaborador","profile-collaborator",
                            "candidature_list", "candidature_details/{candidatureId}"
                        )

                        if (showBeneficiaryBottomBar) {
                            BeneficiaryBottomBar(
                                navController = navController,
                                currentRoute = currentRoute,
                            )
                        }

                        if (showCollaboratorBottomBar) {
                            CollaboratorBottomBar(
                                navController = navController,
                                currentRoute = currentRoute,
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        composable("splash") {
                            SplashView()
                        }

                        composable("login") {
                            LoginView(navController = navController)
                        }

                        composable("profile-collaborator") {
                            ProfileCollaboratorView(navController = navController)
                        }

                        composable("colaborador") {
                            ColaboratorHomeView(navController = navController)
                        }
                        composable("campaigns") {
                            CampaignsView(navController = navController)
                        }
                        composable("new-campaign") {
                            NewCampaignView(navController = navController)
                        }
                        composable(
                            route = "campaign_details/{campaignId}",
                            arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val campaignId = backStackEntry.arguments?.getString("campaignId")
                            if (campaignId != null) {
                                CampaignDetailsView(
                                    navController = navController,
                                    campaignId = campaignId
                                )
                            }
                        }


                        composable("notification-collaborador") {
                            ColaboratorNotificationView(navController = navController)
                        }
                        composable("stock") {
                            StockView(navController = navController)
                        }
                        composable("product") {
                            ProductView(navController = navController)
                        }
                        composable("history-collaborador") {
                            CollatorHistoryView(navController = navController)
                        }

                        composable("candidature_list") {
                            CandidatureListView(navController = navController)
                        }


                        composable(
                            route = "product?productId={productId}",
                            arguments = listOf(navArgument("productId") {
                                nullable = true
                                defaultValue = null
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId")
                            ProductView(navController, productId)
                        }

                        composable("candidature_details/{candidatureId}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("candidatureId")
                            if (id != null) {
                                CandidatureDetailsView(
                                    navController = navController,
                                    candidatureId = id
                                )
                            }
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

                        composable("profile-collaborator") {
                            ProfileView(navController = navController)
                        }
                    }
                }

                // <--- LÓGICA DE VERIFICAÇÃO INICIAL --->
                LaunchedEffect(Unit) {
                    // Pequeno delay opcional para ver o splash (se o telemóvel for muito rápido)
                    // delay(1000)

                    val user = Firebase.auth.currentUser

                    if (user != null) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val document = db.collection("users").document(user.uid).get().await()

                            val isCollaborator = document.getBoolean("isCollaborator") ?: false
                            val isBeneficiary = document.getBoolean("isBeneficiary") ?: false
                            val candidatureId = document.getString("candidatureId")

                            // Decide o destino
                            val destination = if (isCollaborator) {
                                "colaborador"
                            } else if (isBeneficiary) {
                                "home"
                            } else {
                                if (!candidatureId.isNullOrEmpty()) "await-candidature" else "candidature"
                            }

                            // Navega para a página certa e remove o "splash" da pilha
                            navController.navigate(destination) {
                                popUpTo("splash") { inclusive = true }
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Se der erro (ex: sem net), manda para login
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    } else {
                        // Se NÃO estiver logado, manda para o Login
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}