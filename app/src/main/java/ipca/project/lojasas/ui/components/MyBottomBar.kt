package ipca.project.lojasas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.project.lojasas.ui.theme.LojaSASTheme

sealed class BottomBarItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    object Home : BottomBarItem("Início", Icons.Default.Home, "home")
    object Profile : BottomBarItem("Perfil", Icons.Default.Person, "profile")
}

@Composable
fun MyBottomBar(
    navController: NavController,
    currentRoute: String? = null,
    onAddClick: () -> Unit = {}
) {
    val items = listOf(
        BottomBarItem.Home,
        BottomBarItem.Profile
    )

    // MUDAR AQUI: usar remember em vez de rememberSaveable
    var selectedItem by remember { mutableStateOf(items[0]) }

    // Atualiza o item selecionado baseado na rota atual
    if (currentRoute != null) {
        items.find { it.route == currentRoute }?.let {
            if (it != selectedItem) {
                selectedItem = it
            }
        }
    }

    Box(
        modifier = Modifier
            .height(90.dp)
    ) {
        // Navigation Bar - colocada na parte de baixo da Box
        NavigationBar(
            modifier = Modifier
                .height(70.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            // Item da esquerda - Home
            NavigationBarItem(
                selected = selectedItem == BottomBarItem.Home,
                onClick = {
                    selectedItem = BottomBarItem.Home
                    navController.navigate(BottomBarItem.Home.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = BottomBarItem.Home.icon,
                        contentDescription = BottomBarItem.Home.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = BottomBarItem.Home.title,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.weight(1f)
            )

            // Espaço vazio no centro para o botão flutuante
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
            )

            // Item da direita - Profile
            NavigationBarItem(
                selected = selectedItem == BottomBarItem.Profile,
                onClick = {
                    selectedItem = BottomBarItem.Profile
                    navController.navigate(BottomBarItem.Profile.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = BottomBarItem.Profile.icon,
                        contentDescription = BottomBarItem.Profile.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = BottomBarItem.Profile.title,
                        fontSize = 12.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.weight(1f)
            )
        }

        // Botão flutuante central - posicionado acima da NavigationBar
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .size(60.dp)
                .align(Alignment.TopCenter)
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    clip = false
                ),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Adicionar",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

