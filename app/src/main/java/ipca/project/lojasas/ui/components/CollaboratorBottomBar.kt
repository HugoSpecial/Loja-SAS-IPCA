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

sealed class BottomBarItemCollaborator(val title: String, val route: String) {
    object Home : BottomBarItemCollaborator("Início", "collaborator")
    object Notification : BottomBarItemCollaborator("Notificações", "notification-collaborador")
    object History : BottomBarItemCollaborator("Histórico", "history-collaborador")
    object BeneficiaryList : BottomBarItemCollaborator("Beneficiários", "list-beneficiary")
}

@Composable
fun CollaboratorBottomBar(
    navController: NavController,
    currentRoute: String? = null,
    unreadCount: Int = 0
) {
    val items = listOf(
        BottomBarItemCollaborator.Home,
        BottomBarItemCollaborator.Notification,
        BottomBarItemCollaborator.History,
        BottomBarItemCollaborator.BeneficiaryList
    )

    var selectedItem by remember { mutableStateOf(items[0]) }

    // Atualiza o item selecionado quando a rota muda
    LaunchedEffect(currentRoute) {
        if (currentRoute != null) {
            items.find { it.route == currentRoute }?.let {
                if (it != selectedItem) selectedItem = it
            }
        }
    }

    // Cores do tema
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
                .navigationBarsPadding() // CRUCIAL: Adapta-se à barra de sistema do Android
                .height(80.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            containerColor = barBackgroundColor,
            tonalElevation = 10.dp
        ) {
            // ITENS DA ESQUERDA (Início e Notificações)
            items.take(2).forEach { item ->
                NavigationBarItem(
                    selected = selectedItem == item,
                    onClick = {
                        selectedItem = item
                        navController.navigate(item.route) { launchSingleTop = true; restoreState = true }
                    },
                    icon = {
                        if (item == BottomBarItemCollaborator.Notification && unreadCount > 0) {
                            BadgedBox(badge = { Badge(containerColor = MaterialTheme.colorScheme.error, contentColor = Color.White) { Text("$unreadCount") } }) {
                                Icon(painter = painterResource(id = getCollaboratorIconRes(item)), contentDescription = null, modifier = Modifier.size(24.dp))
                            }
                        } else {
                            Icon(painter = painterResource(id = getCollaboratorIconRes(item)), contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    },
                    label = { Text(text = item.title, fontSize = 10.sp, maxLines = 1) },
                    colors = navItemsColorsCollaborator(),
                    alwaysShowLabel = true
                )
            }

            // ESPAÇO CENTRAL
            Spacer(modifier = Modifier.weight(1f))

            // ITENS DA DIREITA (Histórico e Beneficiários)
            items.takeLast(2).forEach { item ->
                NavigationBarItem(
                    selected = selectedItem == item,
                    onClick = {
                        selectedItem = item
                        navController.navigate(item.route) { launchSingleTop = true; restoreState = true }
                    },
                    icon = {
                        Icon(painter = painterResource(id = getCollaboratorIconRes(item)), contentDescription = null, modifier = Modifier.size(24.dp))
                    },
                    label = { Text(text = item.title, fontSize = 10.sp, maxLines = 1) },
                    colors = navItemsColorsCollaborator(),
                    alwaysShowLabel = true
                )
            }
        }

        // --- BOTÃO CENTRAL (STOCK) ---
        Box(
            modifier = Modifier
                .navigationBarsPadding() // Sobe junto com a barra
                .offset(y = (-28).dp) // Ajuste para ficar "flutuante" no meio
                .align(Alignment.BottomCenter)
        ) {
            Surface(
                onClick = { navController.navigate("stock") },
                shape = CircleShape,
                color = primaryColor,
                // Borda da mesma cor da barra para criar o efeito de recorte
                border = BorderStroke(4.dp, barBackgroundColor),
                shadowElevation = 4.dp,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.box_storage),
                        contentDescription = "Stock",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// Função auxiliar para mapear ícones
@Composable
fun getCollaboratorIconRes(item: BottomBarItemCollaborator): Int {
    return when (item) {
        BottomBarItemCollaborator.Home -> R.drawable.icon_home
        BottomBarItemCollaborator.Notification -> R.drawable.outline_notifications
        BottomBarItemCollaborator.History -> R.drawable.outline_watch
        BottomBarItemCollaborator.BeneficiaryList -> R.drawable.persons_two
    }
}

@Composable
fun navItemsColorsCollaborator() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    indicatorColor = Color.Transparent
)