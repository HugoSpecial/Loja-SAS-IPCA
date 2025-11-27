package ipca.project.lojasas.ui.benefeciary.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ipca.project.lojasas.R

@Composable
fun HomeView(
    navController: NavController,
    viewModel: BeneficiaryHomeViewModel = viewModel()
) {
    val state = viewModel.uiState.value
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_sas),
                contentDescription = "Logótipo",
                modifier = Modifier.height(55.dp),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = "Olá, ${state.userName}",
            fontSize = 32.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Bem-vindo à Loja Social do IPCA",
            fontSize = 20.sp,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aqui pode consultar os seus pedidos e fazer novos cabazes.",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(30.dp))

        // --- INFO CARD ---
        HomeInfoCard(
            title = "Pedidos Pendentes",
            count = state.upcomingCount.toString()
        )

        Spacer(modifier = Modifier.height(30.dp))

        // --- BOTÕES DE ACESSO RÁPIDO ---
        Text(
            text = "Gestão",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Botão 1: Ver Pedidos
        // !! adicionar rota
        Button(
            onClick = { navController.navigate("") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, contentDescription = null, tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                Text("Ver Pedidos", fontSize = 16.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão 2: Novo Cabaz
        Button(
            onClick = { navController.navigate("newbasket") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                Text("Novo Cabaz", fontSize = 16.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
fun HomeInfoCard(title: String, count: String) {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text("Disponíveis para si", fontSize = 12.sp, color = Color.Gray)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50)
                )
            ) {
                Text(count, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
