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
    object History : BottomBarItem("Histórico", "history")
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

    val cartCount = CartManager.cartItems.count()
    val barBackgroundColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // --- BARRA DE NAVEGAÇÃO ---
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            containerColor = barBackgroundColor,
            tonalElevation = 10.dp,
            windowInsets = NavigationBarDefaults.windowInsets
        ) {
            // 1. ITENS DA ESQUERDA
            items.take(2).forEach { item ->
                // AQUI ESTÁ A LÓGICA: Verifica se a rota atual bate certo
                val isSelected = currentRoute == item.route

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                                // Opcional: popUpTo para evitar empilhar navegação
                                popUpTo("home") { saveState = true }
                            }
                        }
                    },
                    icon = {
                        if (item == BottomBarItem.Notification && unreadCount > 0) {
                            BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) { Text("$unreadCount") } }) {
                                Icon(
                                    painter = painterResource(id = getIconRes(item)),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                painter = painterResource(id = getIconRes(item)),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { Text(text = item.title, fontSize = 10.sp, maxLines = 1) },
                    colors = navItemColors(),
                    alwaysShowLabel = true
                )
            }

            // --- ESPAÇO CENTRAL ---
            Spacer(modifier = Modifier.weight(1f))

            // 2. ITENS DA DIREITA
            items.takeLast(2).forEach { item ->
                val isSelected = currentRoute == item.route

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo("home") { saveState = true }
                            }
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = getIconRes(item)),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(text = item.title, fontSize = 10.sp, maxLines = 1) },
                    colors = navItemColors(),
                    alwaysShowLabel = true
                )
            }
        }

        // --- BOTÃO FLUTUANTE (CARRINHO) ---
        // Verifica se estamos na rota do carrinho
        val isBasketActive = currentRoute == "newbasket"

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .offset(y = (-28).dp)
                .align(Alignment.BottomCenter)
        ) {
            val cartButtonContent = @Composable {
                Surface(
                    onClick = {
                        if (currentRoute != "newbasket") {
                            navController.navigate("newbasket") {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    shape = CircleShape,
                    color = primaryColor,
                    border = BorderStroke(4.dp, barBackgroundColor),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.shopping_cart),
                            contentDescription = "Carrinho",
                            // Se estivermos no carrinho fica Branco Brilhante, senão ligeiramente transparente
                            tint = if (isBasketActive) Color.White else Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            if (cartCount > 0) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                            modifier = Modifier.offset(x = (-4).dp, y = 4.dp)
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

// Função auxiliar
@Composable
fun getIconRes(item: BottomBarItem): Int {
    return when (item) {
        BottomBarItem.Home -> R.drawable.icon_home
        BottomBarItem.Notification -> R.drawable.outline_notifications
        BottomBarItem.History -> R.drawable.outline_watch
        BottomBarItem.Profile -> R.drawable.outline_user
    }
}

@Composable
fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    indicatorColor = Color.Transparent
)