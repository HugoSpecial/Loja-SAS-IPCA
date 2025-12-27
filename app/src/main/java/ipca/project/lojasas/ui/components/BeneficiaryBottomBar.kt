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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            modifier = Modifier
                .height(80.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            containerColor = Color.White,
            tonalElevation = 10.dp
        ) {
            NavigationBarItem(
                selected = selectedItem == BottomBarItem.Home,
                onClick = {
                    selectedItem = BottomBarItem.Home
                    navController.navigate(BottomBarItem.Home.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    Icon(painter = painterResource(id = R.drawable.icon_home), contentDescription = null, modifier = Modifier.size(28.dp))
                },
                label = { Text(text = BottomBarItem.Home.title, fontSize = 11.sp) },
                colors = navItemColors(),
                modifier = Modifier.weight(1f)
            )

            // ITEM NOTIFICAÇÕES COM BADGE
            NavigationBarItem(
                selected = selectedItem == BottomBarItem.Notification,
                onClick = {
                    selectedItem = BottomBarItem.Notification
                    navController.navigate(BottomBarItem.Notification.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    if (unreadCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = Color.Red, contentColor = Color.White) {
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
                label = { Text(text = BottomBarItem.Notification.title, fontSize = 10.sp) },
                colors = navItemColors(),
                modifier = Modifier.weight(1f)
            )

            Box(modifier = Modifier.weight(1f))

            NavigationBarItem(
                selected = selectedItem == BottomBarItem.History,
                onClick = {
                    selectedItem = BottomBarItem.History
                    navController.navigate(BottomBarItem.History.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    Icon(painter = painterResource(id = R.drawable.outline_watch), contentDescription = null, modifier = Modifier.size(28.dp))
                },
                label = { Text(text = BottomBarItem.History.title, fontSize = 11.sp) },
                colors = navItemColors(),
                modifier = Modifier.weight(1f)
            )

            NavigationBarItem(
                selected = selectedItem == BottomBarItem.Profile,
                onClick = {
                    selectedItem = BottomBarItem.Profile
                    navController.navigate(BottomBarItem.Profile.route) { launchSingleTop = true; restoreState = true }
                },
                icon = {
                    Icon(painter = painterResource(id = R.drawable.outline_user), contentDescription = null, modifier = Modifier.size(28.dp))
                },
                label = { Text(text = BottomBarItem.Profile.title, fontSize = 11.sp) },
                colors = navItemColors(),
                modifier = Modifier.weight(1f)
            )
        }

        Surface(
            onClick = { navController.navigate("newbasket") },
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
                    painter = painterResource(id = R.drawable.shopping_cart),
                    contentDescription = "Carrinho",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

@Composable
fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = Color(0xFF4A4A4A),
    unselectedTextColor = Color(0xFF4A4A4A),
    indicatorColor = Color.Transparent
)