package ipca.project.lojasas.ui.components

import ipca.project.lojasas.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    object History : BottomBarItemCollaborator("Histórico ", "history-collaborador")
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

    if (currentRoute != null) {
        items.find { it.route == currentRoute }?.let {
            if (it != selectedItem) selectedItem = it
        }
    }

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
                selected = selectedItem == BottomBarItemCollaborator.Home,
                onClick = {
                    selectedItem = BottomBarItemCollaborator.Home
                    navController.navigate(BottomBarItemCollaborator.Home.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    Icon(painter = painterResource(id = R.drawable.icon_home), contentDescription = null, modifier = Modifier.size(28.dp))
                },
                label = { Text(text = BottomBarItemCollaborator.Home.title, fontSize = 11.sp) },
                colors = navItemsColorsCollaborator(),
                modifier = Modifier.weight(1f)
            )

            // 2. NOTIFICAÇÕES
            NavigationBarItem(
                selected = selectedItem == BottomBarItemCollaborator.Notification,
                onClick = {
                    selectedItem = BottomBarItemCollaborator.Notification
                    navController.navigate(BottomBarItemCollaborator.Notification.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    if (unreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error, // Vermelho do tema
                                    contentColor = Color.White
                                ) {
                                    Text("$unreadCount")
                                }
                            }
                        ) {
                            Icon(painter = painterResource(id = R.drawable.outline_notifications), contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        Icon(painter = painterResource(id = R.drawable.outline_notifications), contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                },
                label = { Text(text = BottomBarItemCollaborator.Notification.title, fontSize = 10.sp) },
                colors = navItemsColorsCollaborator(),
                modifier = Modifier.weight(1f)
            )

            // ESPAÇO VAZIO (para o botão central)
            Box(modifier = Modifier.weight(1f))

            // 3. HISTÓRICO
            NavigationBarItem(
                selected = selectedItem == BottomBarItemCollaborator.History,
                onClick = {
                    selectedItem = BottomBarItemCollaborator.History
                    navController.navigate(BottomBarItemCollaborator.History.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    Icon(painter = painterResource(id = R.drawable.outline_watch), contentDescription = null, modifier = Modifier.size(28.dp))
                },
                label = { Text(text = BottomBarItemCollaborator.History.title, fontSize = 11.sp) },
                colors = navItemsColorsCollaborator(),
                modifier = Modifier.weight(1f)
            )

            // 4. BENEFICIÁRIOS
            NavigationBarItem(
                selected = selectedItem == BottomBarItemCollaborator.BeneficiaryList,
                onClick = {
                    selectedItem = BottomBarItemCollaborator.BeneficiaryList
                    navController.navigate(BottomBarItemCollaborator.BeneficiaryList.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    Icon(painter = painterResource(id = R.drawable.persons_two), contentDescription = null, modifier = Modifier.size(28.dp))
                },
                label = { Text(text = BottomBarItemCollaborator.BeneficiaryList.title, fontSize = 9.sp) },
                colors = navItemsColorsCollaborator(),
                modifier = Modifier.weight(1f)
            )
        }

        // --- BOTÃO CENTRAL (STOCK) ---
        Surface(
            onClick = { navController.navigate("stock") },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            // A borda deve ser da mesma cor do fundo da barra (surface) para parecer transparente/recortada
            border = BorderStroke(4.dp, MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(70.dp)
                .offset(y = (-15).dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.box_storage),
                    contentDescription = "Stock",
                    tint = Color.White, // Ícone branco
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun navItemsColorsCollaborator() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    // Cores adaptáveis para itens não selecionados (Cinza escuro no Light, Cinza claro no Dark)
    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    indicatorColor = Color.Transparent
)