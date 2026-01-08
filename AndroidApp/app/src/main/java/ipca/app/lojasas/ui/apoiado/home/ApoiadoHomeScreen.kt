package ipca.app.lojasas.ui.apoiado.home

import ipca.app.lojasas.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.data.cestas.ApoiadoCesta
import ipca.app.lojasas.data.requests.UrgentRequest
import ipca.app.lojasas.ui.apoiado.formulario.CompleteDataView
import ipca.app.lojasas.ui.funcionario.calendar.MandatoryPasswordChangeDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// -----------------------------------------------------------------------------
//  Design tokens (cores aproximadas do mockup)
// -----------------------------------------------------------------------------
private val DarkGreenCard = GreenSas

// Card para: "Entregas não levantadas"

// Card para: "Entregas canceladas"

// Card para: "Documentos em Falta"


private enum class CestaCardStyle {
    PENDENTE,
    CONCLUIDA,
    NAO_LEVANTADA,
    CANCELADA
}

private data class ProfileStatusUi(
    val label: String,
    val color: Color,
    val iconText: String? = null
)

@Composable
fun ApoiadoHomeScreen(
    navController: NavController
) {
    val viewModel: ApoiadoViewModel = hiltViewModel()
    val state by viewModel.uiState

    // Garante refresh quando a navegação chega aqui
    LaunchedEffect(Unit) {
        viewModel.checkStatus()
    }

    // 1) Loading
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenSas)
        }
        return
    }

    // 2) Mudança de Password Obrigatória
    if (state.showMandatoryPasswordChange) {
        MandatoryPasswordChangeDialog(
            isLoading = state.isLoading,
            errorMessage = state.error,
            onConfirm = { old, new ->
                viewModel.changePassword(old, new) {
                    viewModel.checkStatus()
                }
            }
        )
        return
    }

    // 3) Conta Bloqueada (não mostra cestas/pedidos)
    if (state.estadoConta.equals("Bloqueado", ignoreCase = true)) {
        BlockedAccountScreen(
            navController = navController,
            onLogout = { viewModel.signOut() }
        )
        return
    }

    // 4) Dados Incompletos
    if (state.dadosIncompletos) {
        CompleteDataView(
            docId = state.docId,
            onSuccess = { viewModel.checkStatus() },
            navController = navController,
            validadeExpiradaEm = if (state.contaExpirada) state.contaExpiradaEm else null
        )
        return
    }

    val statusUi = remember(state.estadoConta, state.faltaDocumentos) {
        getProfileStatusUi(state.estadoConta, state.faltaDocumentos)
    }

    // Pedidos urgentes que já têm cesta associada devem ser apresentados como cesta (e não duplicar cartões)
    val (urgentToShow, deniedUrgentRequests) = remember(state.urgentRequests) {
        val withoutCesta = state.urgentRequests.filter { it.cestaId.isNullOrBlank() }
        val denied = withoutCesta.filter { isDeniedUrgentRequest(it) }
        val active = withoutCesta.filterNot { isDeniedUrgentRequest(it) }
        active to denied
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // -----------------------------------------------------------------
        // Cabeçalho (Olá + Estado + Ver perfil)
        // -----------------------------------------------------------------
        item {
            HomeHeader(
                nome = state.nome,
                statusUi = statusUi,
                onVerPerfil = { navController.navigate(Screen.ProfileApoiado.route) }
            )
        }

        // Estado especial: Suspenso / Negado (mas AINDA mostramos cartões abaixo, como pedido)
        if (state.estadoConta.equals("Suspenso", ignoreCase = true)) {
            item { PausedCard() }
        }

        if (state.estadoConta.equals("Negado", ignoreCase = true)) {
            item {
                DeniedCard(
                    reason = state.motivoNegacao ?: "Sem motivo.",
                    onTryAgain = { viewModel.resetToTryAgain { viewModel.checkStatus() } }
                )
            }
        }

        // -----------------------------------------------------------------
        // Secção: A Acontecer Agora
        // -----------------------------------------------------------------
        item { SectionSeparator(title = "A Acontecer Agora") }

        // Card de Documentos em Falta (quando aplicável)
        if (
            state.faltaDocumentos ||
            state.estadoConta.equals("Falta_Documentos", ignoreCase = true) ||
            state.estadoConta.equals("Correcao_Dados", ignoreCase = true)
        ) {
            item {
                MissingDocumentsCard(
                    onEnviar = { navController.navigate(Screen.DocumentSubmission.route) }
                )
            }
        }

        // Cestas por entregar (verde escuro)
        if (state.cestasPendentes.isNotEmpty()) {
            items(state.cestasPendentes, key = { it.id }) { cesta ->
                CestaHomeCard(
                    cesta = cesta,
                    style = CestaCardStyle.PENDENTE
                )
            }
        }

        // Pedidos de Ajuda (azul acinzentado)
        if (urgentToShow.isNotEmpty()) {
            items(urgentToShow, key = { it.id }) { request ->
                UrgentRequestHomeCard(request = request)
            }
        }

        // Empty state (quando não há nada a acontecer agora)
        if (
            !state.faltaDocumentos &&
            !state.estadoConta.equals("Falta_Documentos", ignoreCase = true) &&
            !state.estadoConta.equals("Correcao_Dados", ignoreCase = true) &&
            state.cestasPendentes.isEmpty() &&
            urgentToShow.isEmpty()
        ) {
            item { EmptyStateCheck() }
        }

        // -----------------------------------------------------------------
        // Secção: Anteriormente
        // -----------------------------------------------------------------
        item { SectionSeparator(title = "Anteriormente") }

        if (deniedUrgentRequests.isNotEmpty()) {
            items(deniedUrgentRequests, key = { it.id }) { request ->
                UrgentRequestHomeCard(request = request)
            }
        }

        // Entregas canceladas (cinzento claro)
        if (state.cestasCanceladas.isNotEmpty()) {
            items(state.cestasCanceladas, key = { it.id }) { cesta ->
                CestaHomeCard(
                    cesta = cesta,
                    style = CestaCardStyle.CANCELADA
                )
            }
        }

        // Entregas não levantadas (amarelo claro)
        if (state.cestasNaoLevantadas.isNotEmpty()) {
            items(state.cestasNaoLevantadas, key = { it.id }) { cesta ->
                CestaHomeCard(
                    cesta = cesta,
                    style = CestaCardStyle.NAO_LEVANTADA
                )
            }
        }

        // Entregas concluídas (verde claro)
        if (state.cestasRealizadas.isNotEmpty()) {
            items(state.cestasRealizadas, key = { it.id }) { cesta ->
                CestaHomeCard(
                    cesta = cesta,
                    style = CestaCardStyle.CONCLUIDA
                )
            }
        }

        // Espaço extra para o footer não tapar o último card
        item { Spacer(modifier = Modifier.height(90.dp)) }
    }
}

// -----------------------------------------------------------------------------
//  UI Components
// -----------------------------------------------------------------------------

@Composable
private fun HomeHeader(
    nome: String,
    statusUi: ProfileStatusUi,
    onVerPerfil: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Olá, ${nome.ifBlank { "(nome)" }}",
            fontFamily = IntroFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = GreenSas
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Estado do Perfil: ",
                    fontFamily = IntroFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = BlackColor
                )
                Text(
                    text = statusUi.label,
                    fontFamily = IntroFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = statusUi.color
                )
                if (!statusUi.iconText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusUi.iconText,
                        fontFamily = IntroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = statusUi.color
                    )
                }
            }

            Button(
                onClick = onVerPerfil,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSas),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = "Ver perfil",
                    fontFamily = IntroFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = WhiteColor
                )
            }
        }
    }
}

@Composable
private fun SectionSeparator(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = DividerGrey,
            thickness = 1.dp
        )

        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontFamily = IntroFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = GreyColor
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = DividerGrey,
            thickness = 1.dp
        )
    }
}

@Composable
private fun CardActionButton(
    text: String,
    accentColor: Color,
    textColor: Color? = null,
    iconColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        color = WhiteColor,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = textColor ?: accentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = iconColor ?: accentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun CestaHomeCard(
    cesta: ApoiadoCesta,
    style: CestaCardStyle
) {
    val (title, background, textColor, accentColor) = when (style) {
        CestaCardStyle.PENDENTE -> Quadruple(
            "Entrega de bens agendada",
            DarkGreenCard,
            WhiteColor,
            DarkGreenCard
        )

        CestaCardStyle.CONCLUIDA -> Quadruple(
            "Entrega de bens concluída",
            LightGreenCard,
            GreenSas,
            GreenSas
        )

        CestaCardStyle.NAO_LEVANTADA -> Quadruple(
            "Entrega não levantada",
            LightYellowCard,
            LightYellowText,
            LightYellowText
        )
        CestaCardStyle.CANCELADA -> Quadruple(
            "Entrega cancelada",
            DarkRedCard,
            WhiteColor,
            WhiteColor
        )
    }

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        CestaDetailsDialog(
            cesta = cesta,
            onDismiss = { showDialog = false }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = textColor
            )

            if (cesta.origem?.equals("Urgente", ignoreCase = true) == true) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Origem: Pedido Urgente",
                    fontFamily = IntroFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.95f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Dia: ${formatDate(cesta.dataRecolha)}",
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = textColor
            )
            Text(
                text = "Horas: ${formatTime(cesta.dataRecolha)}",
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = textColor
            )

            if (style == CestaCardStyle.PENDENTE) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Caso não possa levantar a cesta, reagende via email, sas@ipca.pt",
                    fontFamily = IntroFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.95f)
                )
            }

            if (style == CestaCardStyle.NAO_LEVANTADA) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Faltas: ${cesta.faltas}",
                    fontFamily = IntroFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CardActionButton(
                    text = "Ver Mais",
                    accentColor = accentColor,
                    textColor = if (style == CestaCardStyle.CANCELADA) background else null,
                    iconColor = if (style == CestaCardStyle.CANCELADA) background else null,
                    onClick = { showDialog = true }
                )
            }
        }
    }
}

@Composable
private fun UrgentRequestHomeCard(request: UrgentRequest) {
    val title = if (request.tipo.equals("Urgente", ignoreCase = true)) {
        "Pedido de Ajuda Urgente"
    } else {
        // Mantém o texto do design (imagem 1)
        "Pedido de Ajuda"
    }

    val isDenied = isDeniedUrgentRequest(request)
    val background = if (isDenied) BlueGreyCard.copy(alpha = 0.85f) else BlueGreyCard
    val estadoLabel = formatPedidoEstado(request.estado)

    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        UrgentRequestDetailsDialog(request = request, onDismiss = { showDialog = false })
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = WhiteColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = estadoLabel,
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = WhiteColor.copy(alpha = 0.95f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CardActionButton(
                    text = "Ver Mais",
                    accentColor = background,
                    onClick = { showDialog = true }
                )
            }
        }
    }
}

@Composable
private fun MissingDocumentsCard(onEnviar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WarningOrange),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Documentos em Falta",
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = WhiteColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                /*Surface(
                    color = WhiteColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Nome do Documento",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        fontFamily = IntroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = WarningOrange
                    )
                }*/

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    color = WhiteColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .clickable(onClick = onEnviar)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Enviar",
                            fontFamily = IntroFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = WarningOrange
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCheck() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓",
            fontSize = 54.sp,
            color = TextMutedGrey
        )
    }
}

// -----------------------------------------------------------------------------
//  Dialogs ("Ver Mais")
// -----------------------------------------------------------------------------

@Composable
private fun CestaDetailsDialog(
    cesta: ApoiadoCesta,
    onDismiss: () -> Unit
) {
    val fmtFull = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
            ) { Text("Fechar", color = WhiteColor) }
        },
        title = {
            Text(
                text = "Detalhes da Entrega",
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Estado: ${cesta.estadoCesta.ifBlank { "—" }}",
                    fontFamily = IntroFontFamily
                )

                Text(
                    text = "Data recolha: ${cesta.dataRecolha?.let { fmtFull.format(it) } ?: "A definir"}",
                    fontFamily = IntroFontFamily
                )

                if (cesta.dataAgendada != null) {
                    Text(
                        text = "Data agendada: ${fmtFull.format(cesta.dataAgendada)}",
                        fontFamily = IntroFontFamily
                    )
                }

                Text(
                    text = "Produtos: ${cesta.numeroItens}",
                    fontFamily = IntroFontFamily
                )

                /*Text(
                    text = "ID: ${cesta.id}",
                    fontFamily = IntroFontFamily,
                    fontSize = 12.sp,
                    color = GreyColor
                )*/
            }
        }
    )
}

@Composable
private fun UrgentRequestDetailsDialog(
    request: UrgentRequest,
    onDismiss: () -> Unit
) {
    val fmtFull = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
            ) { Text("Fechar", color = WhiteColor) }
        },
        title = {
            Text(
                text = "Pedido de Ajuda",
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tipo: ${request.tipo.ifBlank { "—" }}",
                    fontFamily = IntroFontFamily
                )
                Text(
                    text = "Estado: ${formatPedidoEstado(request.estado)}",
                    fontFamily = IntroFontFamily
                )
                Text(
                    text = "Data: ${request.data?.let { fmtFull.format(it) } ?: "—"}",
                    fontFamily = IntroFontFamily
                )

                if (request.descricao.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Descrição:",
                        fontFamily = IntroFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = request.descricao,
                        fontFamily = IntroFontFamily
                    )
                }
            }
        }
    )
}

// -----------------------------------------------------------------------------
//  Estados especiais (Mantidos)
// -----------------------------------------------------------------------------

@Composable
private fun PausedCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = WarningOrange),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = WhiteColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Apoio Pausado",
                    color = WhiteColor,
                    fontSize = 16.sp,
                    fontFamily = IntroFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Contacte o SAS: sas@ipca.pt",
                color = WhiteColor,
                fontSize = 14.sp,
                fontFamily = IntroFontFamily
            )
        }
    }
}

@Composable
fun BlockedAccountScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteColor)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Conta Bloqueada",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = IntroFontFamily,
            color = ErrorRed
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Dirija-se aos serviços do SAS.",
            textAlign = TextAlign.Center,
            fontFamily = IntroFontFamily
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                onLogout()
                navController.navigate(Screen.Login.route) { popUpTo(0) }
            },
            colors = ButtonDefaults.buttonColors(containerColor = BlackColor)
        ) {
            Text("Terminar Sessão", color = WhiteColor)
        }
    }
}

@Composable
private fun DeniedCard(reason: String, onTryAgain: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ErrorRed),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pedido Rejeitado",
                color = WhiteColor,
                fontSize = 16.sp,
                fontFamily = IntroFontFamily,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Motivo: $reason",
                color = WhiteColor,
                fontSize = 13.sp,
                fontFamily = IntroFontFamily
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onTryAgain,
                    colors = ButtonDefaults.buttonColors(containerColor = WhiteColor),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Tentar de Novo",
                        color = ErrorRed,
                        fontFamily = IntroFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
//  Helpers
// -----------------------------------------------------------------------------

private fun getProfileStatusUi(rawStatus: String, faltaDocumentos: Boolean): ProfileStatusUi {
    val status = rawStatus.trim()

    return when {
        status.equals("Aprovado", ignoreCase = true) -> ProfileStatusUi("Aprovado", GreenSas, "✓")

        // A app usa "Analise" na BD
        status.equals("Analise", ignoreCase = true) ||
                status.equals("Em_Analise", ignoreCase = true) ||
                status.equals("Em analise", ignoreCase = true) -> ProfileStatusUi("Em análise", WarningOrange, "⟳")

        // Documentos em falta / correção de dados
        faltaDocumentos || status.equals("Falta_Documentos", ignoreCase = true) ->
            ProfileStatusUi("Faltam Documentos", WarningOrange, "⛔")

        status.equals("Correcao_Dados", ignoreCase = true) ->
            ProfileStatusUi("Faltam Dados", WarningOrange, "⛔")

        status.equals("Suspenso", ignoreCase = true) -> ProfileStatusUi("Em pausa", WarningOrange, "⏸")
        status.equals("Negado", ignoreCase = true) -> ProfileStatusUi("Rejeitado", ErrorRed, "⛔")
        status.equals("Bloqueado", ignoreCase = true) -> ProfileStatusUi("Bloqueado", ErrorRed, "⛔")

        status.isBlank() -> ProfileStatusUi("Por submeter", WarningOrange, null)
        else -> ProfileStatusUi(status, BlueGreyCard, null)
    }
}

private fun formatDate(date: Date?): String {
    return date?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "A definir"
}

private fun formatTime(date: Date?): String {
    return date?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) } ?: "--:--"
}

private fun formatPedidoEstado(raw: String): String {
    val normalized = raw.trim().lowercase(Locale.getDefault())
    return when {
        normalized.isBlank() -> "—"
        // Estados do fluxo de aprovação (Pedidos Urgentes)
        normalized == "preparar_apoio" || normalized == "preparar apoio" || normalized == "em preparar apoio" ->
            "Pedido Aprovado. O seu pedido esta a ser preparado. Quando pronto ira aparecer para levantar a cesta"
        normalized == "negado" -> "Pedido Negado"
        normalized == "analise" || normalized == "em analise" || normalized == "em_analise" -> "Em Análise"
        normalized == "aprovado" -> "Aprovado"
        normalized == "rejeitado" -> "Rejeitado"
        normalized == "concluido" || normalized == "concluído" -> "Concluído"
        else -> raw
    }
}

private fun isDeniedUrgentRequest(request: UrgentRequest): Boolean {
    val tipo = request.tipo.trim().lowercase(Locale.getDefault())
    if (tipo != "urgente") return false
    val estado = request.estado.trim().lowercase(Locale.getDefault())
    return estado == "negado" || estado == "rejeitado"
}

/** Pequeno helper para evitar criar uma data class só para 4 valores. */      
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
