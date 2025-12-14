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
import ipca.project.lojasas.ui.beneficiary.history.BeneficiaryHistoryView
import ipca.project.lojasas.ui.candidature.AwaitCandidatureView
import ipca.project.lojasas.ui.candidature.CandidatureView
import ipca.project.lojasas.ui.collaborator.candidature.CandidatureDetailsView
import ipca.project.lojasas.ui.collaborator.candidature.CandidatureListView
import ipca.project.lojasas.ui.beneficiary.home.HomeView
import ipca.project.lojasas.ui.beneficiary.newBasket.NewBasketView
import ipca.project.lojasas.ui.beneficiary.notifications.NotificationView
import ipca.project.lojasas.ui.beneficiary.orders.BeneficiaryOrderDetailView
import ipca.project.lojasas.ui.beneficiary.profile.ProfileView
import ipca.project.lojasas.ui.collaborator.campaigns.CampaignDetailsView
import ipca.project.lojasas.ui.collaborator.campaigns.CampaignsView
import ipca.project.lojasas.ui.collaborator.campaigns.NewCampaignView
import ipca.project.lojasas.ui.collaborator.donation.DonationListView
import ipca.project.lojasas.ui.collaborator.donation.DonationView
import ipca.project.lojasas.ui.collaborator.history.CollatorHistoryView
import ipca.project.lojasas.ui.collaborator.home.CollaboratorHomeView
import ipca.project.lojasas.ui.collaborator.notifications.CollaboratorNotificationView
import ipca.project.lojasas.ui.collaborator.orders.OrderDetailView
import ipca.project.lojasas.ui.collaborator.orders.OrderListView
import ipca.project.lojasas.ui.collaborator.profile.ProfileCollaboratorView
import ipca.project.lojasas.ui.collaborator.stock.StockView
import ipca.project.lojasas.ui.components.BeneficiaryBottomBar
import ipca.project.lojasas.ui.components.CollaboratorBottomBar
import ipca.project.lojasas.ui.components.SplashView
import ipca.project.lojasas.ui.theme.LojaSASTheme
import kotlinx.coroutines.delay

const val TAG = "LojaSAS-IPCA"

class MainActivity : ComponentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            LojaSASTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        val showBeneficiaryBottomBar = currentRoute in listOf(
                            "home", "notification", "history", "profile"
                        )

                        val showCollaboratorBottomBar = currentRoute in listOf(
                            "collaborator",
                            "stock",
                            "notification-collaborador",
                            "history-collaborador",
                            "profile-collaborator"
                        )

                        if (showBeneficiaryBottomBar) {
                            BeneficiaryBottomBar(navController = navController, currentRoute = currentRoute)
                        }

                        if (showCollaboratorBottomBar) {
                            CollaboratorBottomBar(navController = navController, currentRoute = currentRoute)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        // --- ECRA INICIAL ---
                        composable("splash") { SplashView() }
                        composable("login") { LoginView(navController = navController) }

                        // --- ROTAS COLABORADOR ---
                        composable("collaborator") { CollaboratorHomeView(navController = navController) }
                        composable("notification-collaborador") { CollaboratorNotificationView(navController = navController) }
                        composable("donations_list") { DonationListView(navController = navController) }
                        composable("stock") { StockView(navController = navController) }
                        composable("profile-collaborator") { ProfileCollaboratorView(navController = navController) } // Usa a view correta
                        composable("history-collaborador") { CollatorHistoryView(navController = navController) }
                        composable("orders") { OrderListView(navController = navController) }
                        composable(
                            route = "order_details/{orderId}",
                            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val orderId = backStackEntry.arguments?.getString("orderId")
                            if (orderId != null) {
                                OrderDetailView(navController = navController, orderId = orderId)
                            }
                        }

                        // Campanhas
                        composable("campaigns") { CampaignsView(navController = navController) }
                        composable("new-campaign") { NewCampaignView(navController = navController) }
                        composable(
                            route = "campaign_details/{campaignId}",
                            arguments = listOf(navArgument("campaignId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val campaignId = backStackEntry.arguments?.getString("campaignId")
                            if (campaignId != null) {
                                CampaignDetailsView(navController = navController, campaignId = campaignId)
                            }
                        }

                        // Candidaturas
                        composable("candidature_list") { CandidatureListView(navController = navController) }
                        composable("candidature_details/{candidatureId}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("candidatureId")
                            if (id != null) {
                                CandidatureDetailsView(navController = navController, candidatureId = id)
                            }
                        }

                        // Produtos / Doações
                        composable(
                            route = "product?productId={productId}",
                            arguments = listOf(navArgument("productId") {
                                nullable = true
                                defaultValue = null
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getString("productId")
                            DonationView(navController, productId)
                        }

                        // --- ROTAS BENEFICIÁRIO ---
                        composable("candidature") { CandidatureView(navController = navController) }
                        composable("await-candidature") { AwaitCandidatureView(navController = navController) }
                        composable("home") { HomeView(navController = navController) }
                        composable("notification") { NotificationView(navController = navController) }
                        composable("newbasket") { NewBasketView(navController = navController) }
                        composable("history") { BeneficiaryHistoryView(navController = navController) }
                        composable("profile") { ProfileView(navController = navController) }
                        composable(
                            route = "beneficiary_order_details/{orderId}",
                            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val orderId = backStackEntry.arguments?.getString("orderId")
                            if (orderId != null) {
                                BeneficiaryOrderDetailView(navController = navController, orderId = orderId)
                            }
                        }
                    }
                }

                // <--- LÓGICA DE VERIFICAÇÃO INICIAL (SPLASH) --->
                LaunchedEffect(Unit) {
                    delay(1000) // Pequeno delay para o splash ser visível

                    val user = Firebase.auth.currentUser

                    if (user != null) {
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val document = db.collection("users").document(user.uid).get().await()

                            val isCollaborator = document.getBoolean("isCollaborator") ?: false
                            val isBeneficiary = document.getBoolean("isBeneficiary") ?: false
                            val candidatureId = document.getString("candidatureId")

                            val destination = if (isCollaborator) {
                                "collaborator"
                            } else if (isBeneficiary) {
                                "home"
                            } else {
                                if (!candidatureId.isNullOrEmpty()) "await-candidature" else "candidature"
                            }

                            navController.navigate(destination) {
                                popUpTo("splash") { inclusive = true }
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                            navController.navigate("login") {
                                popUpTo("splash") { inclusive = true }
                            }
                        }
                    } else {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            }
        }
    }
}