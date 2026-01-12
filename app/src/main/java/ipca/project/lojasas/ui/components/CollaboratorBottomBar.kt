package ipca.project.lojasas.ui.components

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
import androidx.navigation.NavGraph.Companion.findStartDestination
import ipca.project.lojasas.R

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

    // Cores
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
                val isSelected = currentRoute == item.route

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                // CORREÇÃO AQUI: Usar findStartDestination().id
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        if (item == BottomBarItemCollaborator.Notification && unreadCount > 0) {
                            BadgedBox(badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = Color.White
                                ) { Text("$unreadCount") }
                            }) {
                                Icon(
                                    painter = painterResource(id = getCollaboratorIconRes(item)),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                painter = painterResource(id = getCollaboratorIconRes(item)),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { Text(text = item.title, fontSize = 10.sp, maxLines = 1) },
                    colors = navItemsColorsCollaborator(),
                    alwaysShowLabel = true
                )
            }

            // 2. ESPAÇO CENTRAL
            Spacer(modifier = Modifier.weight(1f))

            // 3. ITENS DA DIREITA
            items.takeLast(2).forEach { item ->
                val isSelected = currentRoute == item.route

                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                // CORREÇÃO AQUI TAMBÉM
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = getCollaboratorIconRes(item)),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(text = item.title, fontSize = 10.sp, maxLines = 1) },
                    colors = navItemsColorsCollaborator(),
                    alwaysShowLabel = true
                )
            }
        }

        // --- BOTÃO CENTRAL (STOCK) ---
        val isStockActive = currentRoute == "stock"

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .offset(y = (-28).dp)
                .align(Alignment.BottomCenter)
        ) {
            Surface(
                onClick = {
                    if (currentRoute != "stock") {
                        navController.navigate("stock") {
                            // O Stock não precisa de popUpTo da mesma forma, pois é uma "folha"
                            // mas usamos launchSingleTop para evitar duplicações
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
                        painter = painterResource(id = R.drawable.box_storage),
                        contentDescription = "Stock",
                        tint = if (isStockActive) Color.White else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// --- FUNÇÕES AUXILIARES ---

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