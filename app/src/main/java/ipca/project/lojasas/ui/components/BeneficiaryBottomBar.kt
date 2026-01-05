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

    var selectedItem by remember { mutableStateOf(items[0]) }

    // Atualiza o item selecionado com base na rota atual (se houver navegação externa)
    LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            items.find { it.route == currentRoute }?.let {
                if (it != selectedItem) selectedItem = it
            }
        }
    }

    val cartCount = CartManager.cartItems.count()
    val barBackgroundColor = MaterialTheme.colorScheme.surface
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(80.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            containerColor = barBackgroundColor,
            tonalElevation = 10.dp
        ) {
            // Lado Esquerdo
            items.take(2).forEach { item ->
                NavigationBarItem(
                    selected = selectedItem == item,
                    onClick = {
                        selectedItem = item
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        // Lógica específica para notificações com Badge
                        if (item == BottomBarItem.Notification && unreadCount > 0) {
                            BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error) { Text("$unreadCount") } }) {
                                Icon(
                                    painter = painterResource(id = getIconRes(item)),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp) // Tamanho standard
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
            // Spacer com peso 1f para empurrar os itens e criar o buraco para o botão
            Spacer(modifier = Modifier.weight(1f))

            // Lado Direito
            items.takeLast(2).forEach { item ->
                NavigationBarItem(
                    selected = selectedItem == item,
                    onClick = {
                        selectedItem = item
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
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

        Box(
            modifier = Modifier
                .navigationBarsPadding() // Acompanha a subida da barra em telemóveis com gestos
                .offset(y = (-28).dp) // Ajuste para ficar meio dentro/meio fora. Ajusta este valor se necessário
                .align(Alignment.BottomCenter)
        ) {
            val cartButtonContent = @Composable {
                Surface(
                    onClick = { navController.navigate("newbasket") },
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
                            tint = Color.White,
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

// Função para limpar o código principal e obter os ícones
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