package ipca.project.lojasas.ui.beneficiary.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun ProfileView(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    val state = viewModel.uiState.value
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start
    ) {

        Text(
            text = "Perfil",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "O perfil atualiza automaticamente, escreva somente o seu telemóvel e as suas preferências.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        CustomProfileField(
            label = "Email",
            value = state.email,
            onValueChange = {},
            icon = Icons.Default.Email,
            isReadOnly = true,
            placeholder = "Sem Email"
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomProfileField(
            label = "Nome",
            value = state.name,
            onValueChange = {},
            icon = Icons.Default.Person,
            isReadOnly = true,
            placeholder = "Sem Nome"
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (state.fault > 0) {
            CustomProfileField(
                label = "Faltas",
                value = "${state.fault} faltas",
                onValueChange = {},
                icon = Icons.Default.Warning,
                isReadOnly = true,
                textColor = MaterialTheme.colorScheme.error,
                iconColor = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        CustomProfileField(
            label = "Telemóvel",
            value = state.phone,
            onValueChange = { newValue ->
                viewModel.onPhoneChange(newValue)
            },
            icon = Icons.Default.Phone,
            isReadOnly = false,
            placeholder = "Sem Telemóvel"
        )

        Spacer(modifier = Modifier.height(16.dp))

        CustomProfileField(
            label = "Preferências",
            value = state.preferences,
            onValueChange = { newValue ->
                viewModel.onPreferencesChange(newValue)
            },
            icon = null,
            isReadOnly = false,
            isMultiLine = true,
            placeholder = "Sem Preferências"
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Botão Suporte
        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Suporte Chat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botão Logout
        OutlinedButton(
            onClick = {
                viewModel.logout {
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.ExitToApp, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Terminar Sessão",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(50.dp))
    }
}

@Composable
fun CustomProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector? = null,
    isReadOnly: Boolean = false,
    isMultiLine: Boolean = false,
    placeholder: String = "",
    textColor: Color? = null,
    iconColor: Color? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = if (isReadOnly) { {} } else onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isMultiLine) 120.dp else 56.dp),

            shape = RoundedCornerShape(12.dp),

            enabled = !isReadOnly,
            readOnly = isReadOnly,
            singleLine = !isMultiLine,
            maxLines = if (isMultiLine) 5 else 1,

            textStyle = androidx.compose.ui.text.TextStyle(
                color = textColor ?: MaterialTheme.colorScheme.onSurface
            ),

            leadingIcon = if (icon != null) {
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor ?: if (isReadOnly)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
            } else null,

            placeholder = {
                Text(
                    text = placeholder,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                disabledBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),

                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),

                disabledTextColor = textColor ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
    }
}