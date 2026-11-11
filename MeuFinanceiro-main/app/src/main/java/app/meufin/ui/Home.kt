package app.meufin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PluggyOnboardingCard(
    onConnectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Conecte suas contas com Pluggy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Sincronize automaticamente seus extratos e saldos do banco sem complicação.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            StepItem(
                number = "1",
                title = "Obtenha suas credenciais no Dashboard Pluggy",
                subtitle = "Cadastre-se na Pluggy para gerar seu Client ID e Client Secret."
            )

            Spacer(modifier = Modifier.height(10.dp))

            StepItem(
                number = "2",
                title = "Insira as chaves nas configurações do App",
                subtitle = "Cole as chaves cadastradas para habilitar a API de integração."
            )

            Spacer(modifier = Modifier.height(10.dp))

            StepItem(
                number = "3",
                title = "Conecte sua conta bancária via Widget",
                subtitle = "Escolha seu banco (Nubank, Itaú, Banco do Brasil, etc.) e autorize a leitura."
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onConnectClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "Iniciar Configuração")
            }
        }
    }
}

@Composable
private fun StepItem(number: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
