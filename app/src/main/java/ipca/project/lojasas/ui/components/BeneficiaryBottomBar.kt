package ipca.project.lojasas.ui.components

import ipca.project.lojasas.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import ipca.project.lojasas.ui.beneficiary.CartManager

sealed class BottomBarItem(val title: String, val route: String) {
    object Home : BottomBarItem("Início", "home")
    object Notification : BottomBarItem("Notificações", "notification")
    object History : BottomBarItem("Histórico ", "history")
    object Profile : BottomBarItem("Perfil", "profile")
}

@Composable
fun BeneficiaryBottomBar(
    navController: NavController,
    currentRoute: String? = null,
    unreadCount: Int = 0
) {
    val items = listOf(
        BottomBarItem.Home,
        BottomBarItem.Notification,
        BottomBarItem.History,
        BottomBarItem.Profile
    )

    var selectedItem by remember { mutableStateOf(items[0]) }

    if (currentRoute != null) {
        items.find { it.route == currentRoute }?.let {
            if (it != selectedItem) selectedItem = it
        }
    }

    val cartCount = CartManager.cartItems.count()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // --- BARRA DE NAVEGAÇÃO ---
        NavigationBar(
            modifier = Modifier
                .height(80.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            // Fundo adaptável (Branco no Light, Escuro no Dark)
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp
        ) {
            // 1. HOME
            NavigationBarItem(
                selected = selectedItem == BottomBarItem.Home,
                onClick = { selectedItem = BottomBarItem.Home; navController.navigate(BottomBarItem.Home.route) { launchSingleTop = true; restoreState = true } },
                icon = { Icon(painter = painterResource(id = R.drawable.icon_home), contentDescription = null, modifier = Modifier.size(28.dp)) },
                label = { Text(text = BottomBarItem.Home.title, fontSize = 11.sp) },
                colors = navItemColors(),
                modifier = Modifier.weight(1f)
            )

            // 2. NOTIFICAÇÕES
            NavigationBarItem(
                selected = selectedItem == BottomBarItem.Notification,
                onClick = { selectedItem = BottomBarItem.Notification; navController.navigate(BottomBarItem.Notification.route) { launchSingleTop = true; restoreState = true } },
                icon = {
                    if (unreadCount > 0) {
                        BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White) { Text("$unreadCount") } }) {
                            Icon(painter = painterResource(id = R.drawable.outline_notifications), contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        Icon(painter = painterResource(id = R.drawable.outline_notifications), contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                },
                label = { Text(text = BottomBarItem.Notification.title, fontSize = 10.sp) },
                colors = navItemColors(),
                modifier = Modifier.weight(1f)
            )

            // Espaço vazio para o botão central
            Box(modifier = Modifier.weight(1f))

            // 3. HISTÓRICO
            NavigationBarItem(
                selected = selectedItem == BottomBarItem.History,
                onClick = { selectedItem = BottomBarItem.History; navController.navigate(BottomBarItem.History.route) { launchSingleTop = true; restoreState = true } },
                icon = { Icon(painter = painterResource(id = R.drawable.outline_watch), contentDescription = null, modifier = Modifier.size(28.dp)) },
                label = { Text(text = BottomBarItem.History.title, fontSize = 11.sp) },
                colors = navItemColors(),
                modifier = Modifier.weight(1f)
            )

            // 4. PERFIL
            NavigationBarItem(
                selected = selectedItem == BottomBarItem.Profile,
                onClick = { selectedItem = BottomBarItem.Profile; navController.navigate(BottomBarItem.Profile.route) { launchSingleTop = true; restoreState = true } },
                icon = { Icon(painter = painterResource(id = R.drawable.outline_user), contentDescription = null, modifier = Modifier.size(28.dp)) },
                label = { Text(text = BottomBarItem.Profile.title, fontSize = 11.sp) },
                colors = navItemColors(),
                modifier = Modifier.weight(1f)
            )
        }

        // --- BOTÃO FLUTUANTE (CARRINHO) ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-15).dp)
        ) {
            // Função auxiliar para desenhar o botão com ou sem badge
            val cartButtonContent = @Composable {
                Surface(
                    onClick = { navController.navigate("newbasket") },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    // A borda deve ter a mesma cor do fundo da barra (surface) para parecer um recorte
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier.size(70.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.shopping_cart),
                            contentDescription = "Carrinho",
                            tint = Color.White, // Ícone sempre branco sobre o verde
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }

            if (cartCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        ) {
                            Text("$cartCount")
                        }
                    }
                ) {
                    cartButtonContent()
                }
            } else {
                cartButtonContent()
            }
        }
    }
}

@Composable
fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    // Cores adaptáveis para itens não selecionados (Cinza escuro no Light, Cinza claro no Dark)
    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    indicatorColor = Color.Transparent
)