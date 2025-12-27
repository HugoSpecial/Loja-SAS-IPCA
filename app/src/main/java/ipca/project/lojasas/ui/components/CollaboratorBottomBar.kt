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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

sealed class BottomBarItemCollaborator(val title: String, val route: String) {
    object Home : BottomBarItemCollaborator("Início", "collaborator")
    object Notification : BottomBarItemCollaborator("Notificações", "notification-collaborador")
    object History : BottomBarItemCollaborator("Histórico ", "history-collaborador")
    object Profile : BottomBarItemCollaborator("Perfil", "profile-collaborator")
}

@Composable
fun CollaboratorBottomBar(
    navController: NavController,
    currentRoute: String? = null,
    unreadCount: Int = 0 // <--- NOVO PARÂMETRO: Recebe o número de notificações por ler
) {
    val items = listOf(
        BottomBarItemCollaborator.Home,
        BottomBarItemCollaborator.Notification,
        BottomBarItemCollaborator.History,
        BottomBarItemCollaborator.Profile
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

        // 1. A BARRA BRANCA
        NavigationBar(
            modifier = Modifier
                .height(80.dp) // Ajustei para 80dp (150dp era muito alto para a barra base)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            containerColor = Color.White,
            tonalElevation = 10.dp
        ) {

            // --- ITEM 1: HOME ---
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
                colors = navItemsColors(),
                modifier = Modifier.weight(1f)
            )

            // --- ITEM 2: NOTIFICAÇÕES (COM BADGE) ---
            NavigationBarItem(
                selected = selectedItem == BottomBarItemCollaborator.Notification,
                onClick = {
                    selectedItem = BottomBarItemCollaborator.Notification
                    navController.navigate(BottomBarItemCollaborator.Notification.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    // LÓGICA DO PONTO VERMELHO
                    if (unreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color.Red,
                                    contentColor = Color.White
                                ) {
                                    Text("$unreadCount") // Mostra o número. Se quiseres só a bola, apaga esta linha.
                                }
                            }
                        ) {
                            Icon(painter = painterResource(id = R.drawable.outline_notifications), contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                    } else {
                        // Ícone normal sem badge
                        Icon(painter = painterResource(id = R.drawable.outline_notifications), contentDescription = null, modifier = Modifier.size(28.dp))
                    }
                },
                label = { Text(text = BottomBarItemCollaborator.Notification.title, fontSize = 10.sp) },
                colors = navItemsColors(),
                modifier = Modifier.weight(1f)
            )

            // --- ESPAÇO VAZIO NO MEIO ---
            Box(modifier = Modifier.weight(1f))

            // --- ITEM 3: HISTÓRICO ---
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
                colors = navItemsColors(),
                modifier = Modifier.weight(1f)
            )

            // --- ITEM 4: PERFIL ---
            NavigationBarItem(
                selected = selectedItem == BottomBarItemCollaborator.Profile,
                onClick = {
                    selectedItem = BottomBarItemCollaborator.Profile
                    navController.navigate(BottomBarItemCollaborator.Profile.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    Icon(painter = painterResource(id = R.drawable.outline_user), contentDescription = null, modifier = Modifier.size(28.dp))
                },
                label = { Text(text = BottomBarItemCollaborator.Profile.title, fontSize = 11.sp) },
                colors = navItemsColors(),
                modifier = Modifier.weight(1f)
            )
        }

        // 2. BOTÃO FLUTUANTE
        Surface(
            onClick = { navController.navigate("stock") },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            border = BorderStroke(4.dp, Color.White),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(70.dp)
                .offset(y = (-15).dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.box_storage),
                    contentDescription = "Carrinho",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun navItemsColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = Color(0xFF4A4A4A),
    unselectedTextColor = Color(0xFF4A4A4A),
    indicatorColor = Color.Transparent
)