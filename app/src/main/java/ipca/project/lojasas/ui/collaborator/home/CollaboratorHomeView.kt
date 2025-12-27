package ipca.project.lojasas.ui.collaborator.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R

// --- PALETA DE CORES ---
val IpcaGreen = Color(0xFF00864F)
val IpcaDarkTeal = Color(0xFF005A49)
val IpcaOlive = Color(0xFF689F38)
val IpcaBlueGray = Color(0xFF455A64)

val IpcaBlackGreen = Color(0xFF1B2E25)
val BgLight = Color(0xFFF2F4F3)

@Composable
fun CollaboratorHomeView(
    navController: NavController,
    viewModel: CollaboratorHomeViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState)
    ) {

        // --- 1. CABEÇALHO (LOGO IGUAL AO HISTÓRICO) ---
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.logo_sas),
            contentDescription = "Logótipo",
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            contentScale = ContentScale.Fit // Garante que não fica distorcido
        )

        Spacer(modifier = Modifier.height(24.dp))


        // --- 2. BOAS-VINDAS ---
        Text(
            text = "Olá, ${state.userName}",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1A1A1A)
        )
        Text(
            text = "Resumo da atividade hoje.",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- 3. RESUMO ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ){
            SummaryCard(
                title = "Entregas para Hoje",
                count = state.deliveriesTodayCount.toString(),
                icon = ImageVector.vectorResource(id = R.drawable.delivery),
                iconColor = IpcaDarkTeal,
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "Campanhas Ativas",
                count = state.activeCampaignsCount.toString(),
                icon = ImageVector.vectorResource(id = R.drawable.megaphone),
                iconColor = IpcaDarkTeal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                title = "Candidaturas Pendentes",
                count = state.pendingCount.toString(),
                icon = ImageVector.vectorResource(id = R.drawable.file_dock),
                iconColor = IpcaDarkTeal,
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "Pedidos Pendentes",
                count = state.pendingSolicitationsCount.toString(),
                icon = ImageVector.vectorResource(id = R.drawable.shopping_cart),
                iconColor = IpcaDarkTeal,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))

        // --- 4. ÁREA DE GESTÃO ---
        Text(
            text = "Menu de Gestão",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // LINHA 1
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                ActionCard(
                    title = "Entregas",
                    subtitle = "Gestão de Entregas",
                    icon = ImageVector.vectorResource(id = R.drawable.delivery),
                    backgroundColor = IpcaOlive,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("deliveries") }
                )

                ActionCard(
                    title = "Pedidos",
                    subtitle = "Gestão de Pedidos",
                    icon = ImageVector.vectorResource(id = R.drawable.shopping_cart),
                    backgroundColor = IpcaDarkTeal,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("orders") }
                )
            }

            // LINHA 2
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActionCard(
                    title = "Doações",
                    subtitle = "Gestão de Doações",
                    icon = ImageVector.vectorResource(id = R.drawable.donate),
                    backgroundColor = IpcaBlueGray,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("donations_list") }
                )

                ActionCard(
                    title = "Candidaturas",
                    subtitle = "Gestão de Candidaturas",
                    icon = ImageVector.vectorResource(id = R.drawable.file_dock),
                    backgroundColor = IpcaGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate("candidature_list") }
                )
            }

            // LINHA 3
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                 ActionCard(
                     title = "Campanhas",
                     subtitle = "Gestão de Campanhas",
                     icon = ImageVector.vectorResource(id = R.drawable.megaphone),
                     backgroundColor = IpcaBlackGreen,
                     modifier = Modifier.weight(1f),
                     onClick = { navController.navigate("campaigns") }
                 )
             }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

// --- COMPONENTES AUXILIARES ---

@Composable
fun SummaryCard(
    title: String,
    count: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = count,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    lineHeight = 26.sp
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 15.dp, y = (-15).dp)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp
                )
            }
        }
    }
}