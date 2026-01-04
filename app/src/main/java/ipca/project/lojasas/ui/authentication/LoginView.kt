package ipca.project.lojasas.ui.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import ipca.project.lojasas.R // Certifica-te que este import do R está correto para o teu pacote

@Composable
fun LoginView(
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState
    var passwordVisible by remember { mutableStateOf(false) }

    // POP-UP DE ERRO
    if (uiState.error != null) {
        CustomErrorDialog(
            error = uiState.error ?: "Ocorreu um erro inesperado.",
            onDismiss = { viewModel.clearError() }
        )
    }

    // O Scaffold usa automaticamente o 'background' do Theme (MyBackColor)
    Scaffold(modifier = modifier.fillMaxSize()) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background // Garante a cor de fundo Cinza Claro
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
            ) {

                Spacer(modifier = Modifier.height(60.dp))

                // LOGO
                Image(
                    painter = painterResource(id = R.drawable.logo_sas),
                    contentDescription = "Logo IPCA SAS",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )

                Spacer(modifier = Modifier.height(80.dp))

                // TÍTULO
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary, // Usa o GreenPrimary
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                // --- INPUT EMAIL ---
                Text(
                    text = "Email",
                    color = MaterialTheme.colorScheme.primary, // Usa o GreenPrimary
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )

                OutlinedTextField(
                    value = uiState.email ?: "",
                    onValueChange = { viewModel.updateEmail(it) },
                    placeholder = {
                        Text(
                            "Escrever email ...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // Cinza adaptável
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary // Ícone Verde
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,   // Borda Verde ao focar
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary, // Borda Verde sem foco
                        cursorColor = MaterialTheme.colorScheme.primary,          // Cursor Verde

                        // Fundo do input: Branco no Light Mode, Escuro no Dark Mode (definido no Theme)
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- INPUT PASSWORD ---
                Text(
                    text = "Password",
                    color = MaterialTheme.colorScheme.primary, // Usa o GreenPrimary
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                )

                OutlinedTextField(
                    value = uiState.password ?: "",
                    onValueChange = { viewModel.updatePassword(it) },
                    placeholder = {
                        Text(
                            "Escrever password ...",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary // Ícone Verde
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(
                                    id = if (passwordVisible) R.drawable.icon_eyeopen
                                    else R.drawable.icon_eyeclose
                                ),
                                contentDescription = "Toggle password",
                                tint = MaterialTheme.colorScheme.primary, // Ícone Verde
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(40.dp))

                // --- BOTÃO ENTRAR ---
                Button(
                    onClick = {
                        viewModel.login { route ->
                            navController.navigate(route) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    enabled = !uiState.isLoading &&
                            uiState.email?.isNotEmpty() == true &&
                            uiState.password?.isNotEmpty() == true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        // Fundo Verde
                        containerColor = MaterialTheme.colorScheme.primary,
                        // Texto Branco (onPrimary)
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        // Cor quando desativado (opcional, cinza automático)
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary, // Spinner Branco
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = "Entrar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}

// --- COMPONENTE DE ERRO REUTILIZÁVEL ---
@Composable
fun CustomErrorDialog(
    error: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface, // Branco
        title = {
            Text(
                text = "Atenção",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error, // RedPrimary
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = error,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface // Preto
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary, // GreenPrimary
                    contentColor = MaterialTheme.colorScheme.onPrimary // Branco
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 8.dp, end = 8.dp)
            ) {
                Text("OK", fontWeight = FontWeight.Bold)
            }
        }
    )
}