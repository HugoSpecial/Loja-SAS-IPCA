package ipca.project.lojasas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import ipca.project.lojasas.ui.authentication.ForgotPasswordView
import ipca.project.lojasas.ui.authentication.LoginView
import ipca.project.lojasas.ui.home.HomeView
import ipca.project.lojasas.ui.theme.LojaSASTheme

const val TAG = "LojaSAS-IPCA"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val navController = rememberNavController()

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

                        composable("home") {
                            HomeView(
                                navController = navController
                            )
                        }

                        composable("forgot-password") {
                            ForgotPasswordView(
                                navController = navController
                            )
                        }
                    }
                }
            }
            LaunchedEffect(Unit) {
                val userID = Firebase.auth.currentUser?.uid
                if (userID != null) {
                    navController.navigate("home")
                }
            }
        }
    }
}


