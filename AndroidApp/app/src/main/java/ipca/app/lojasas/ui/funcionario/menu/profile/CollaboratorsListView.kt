package ipca.app.lojasas.ui.funcionario.menu.profile

import ipca.app.lojasas.ui.theme.*
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.funcionario.CollaboratorItem
import ipca.app.lojasas.ui.components.AppHeader

@Composable
fun CollaboratorsListView(
    navController: NavController,
    viewModel: CollaboratorsListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState
    val context = LocalContext.current

    // Estados para controlar qual item está a ser modificado (para abrir os pop-ups)
    var itemToDelete by remember { mutableStateOf<CollaboratorItem?>(null) }
    var itemToPromote by remember { mutableStateOf<CollaboratorItem?>(null) }
    var itemToDemote by remember { mutableStateOf<CollaboratorItem?>(null) }

    // Mostrar erros via Toast se existirem
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AppHeader("Colaboradores", true, { navController.popBackStack() }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(GreyBg)
                .padding(16.dp)
        ) {
            // Barra de Pesquisa
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WhiteColor, RoundedCornerShape(8.dp)),
                placeholder = { Text("Pesquisar (Nome, ID, Função)", color = GreenSas) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenSas,
                    unfocusedBorderColor = GreenSas,
                    focusedPlaceholderColor = GreenSas,
                    unfocusedPlaceholderColor = GreenSas,
                    cursorColor = GreenSas
                )
            )

            Spacer(Modifier.height(16.dp))

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenSas)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.filteredList, key = { it.id }) { item ->
                        CollaboratorCard(
                            item = item,
                            currentUserId = state.currentUserId,
                            onDelete = { itemToDelete = item },
                            onPromote = { itemToPromote = item },
                            onDemote = { itemToDemote = item }
                        )
                    }
                }
            }
        }
    }

    // --- POP-UP: APAGAR ---
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Apagar Colaborador") },
            text = { Text("Tem a certeza que deseja remover ${itemToDelete?.nome}? Esta ação é irreversível.") },
            confirmButton = {
                Button(
                    onClick = {
                        val item = itemToDelete
                        if (item != null) {
                            viewModel.deleteCollaborator(item) {
                                Toast.makeText(context, "Colaborador removido", Toast.LENGTH_SHORT).show()
                            }
                        }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor)
                ) { Text("Apagar") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancelar") }
            },
            containerColor = WhiteColor
        )
    }

    // --- POP-UP: PROMOVER A ADMIN ---
    if (itemToPromote != null) {
        AlertDialog(
            onDismissRequest = { itemToPromote = null },
            title = { Text("Promover a Administrador") },
            text = { Text("Deseja dar permissões de Administrador a ${itemToPromote?.nome}? Ele poderá gerir outros colaboradores.") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToPromote?.let { viewModel.promoteToAdmin(it) }
                        itemToPromote = null
                        Toast.makeText(context, "Promovido com sucesso", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSas)
                ) { Text("Promover") }
            },
            dismissButton = {
                TextButton(onClick = { itemToPromote = null }) { Text("Cancelar") }
            },
            containerColor = WhiteColor
        )
    }

    // --- POP-UP: DESPROMOVER A COLABORADOR ---
    if (itemToDemote != null) {
        AlertDialog(
            onDismissRequest = { itemToDemote = null },
            title = { Text("Remover Permissões de Admin") },
            text = { Text("Tem a certeza que deseja remover as permissões de Admin de ${itemToDemote?.nome}?") },
            confirmButton = {
                Button(
                    onClick = {
                        itemToDemote?.let { viewModel.demoteToFuncionario(it) }
                        itemToDemote = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkRed)
                ) { Text("Remover Admin") }
            },
            dismissButton = {
                TextButton(onClick = { itemToDemote = null }) { Text("Cancelar") }
            },
            containerColor = WhiteColor
        )
    }
}

@Composable
fun CollaboratorCard(
    item: CollaboratorItem,
    currentUserId: String,
    onDelete: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit
) {
    val isAdmin = item.role.equals("Admin", ignoreCase = true)
    val isSelf = item.uid == currentUserId // Verifica se é o próprio utilizador

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(item.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("ID: ${item.id}", fontSize = 12.sp, color = GreyColor)
                    Text(item.email, fontSize = 12.sp, color = GreyColor)
                }

                // Badge da Role
                Surface(
                    color = if (isAdmin) DarkRed else GreenSas,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = if (isAdmin) "ADMIN" else "COLAB",
                        color = WhiteColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = DividerGreenLight)
            Spacer(Modifier.height(8.dp))

            // Ações
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Se for o próprio utilizador, não mostra NENHUM botão de ação
                if (!isSelf) {
                    // Botão Promover/Despromover
                    TextButton(
                        onClick = if (isAdmin) onDemote else onPromote,
                        colors = ButtonDefaults.textButtonColors(contentColor = if (isAdmin) DarkGreyColor else GreenSas)
                    ) {
                        Icon(
                            if (isAdmin) Icons.Default.Security else Icons.Default.SupervisorAccount,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (isAdmin) "Despromover" else "Tornar Admin", fontSize = 13.sp)
                    }

                    Spacer(Modifier.width(8.dp))

                    // Botão Apagar
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Apagar", tint = GreyColor)
                    }
                } else {
                    // Opcional: Mostrar texto informativo
                    Text(
                        text = " (Você) ",
                        fontSize = 12.sp,
                        color = LightGreyColor,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}
