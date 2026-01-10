package ipca.app.lojasas.ui.funcionario.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
import ipca.app.lojasas.ui.components.AppHeader
import ipca.app.lojasas.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatListView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: ChatListViewModel = hiltViewModel()
    val state by viewModel.uiState
    var searchQuery by remember { mutableStateOf("") }
    val filteredThreads = remember(state.threads, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            state.threads
        } else {
            val normalized = query.lowercase(Locale.getDefault())
            state.threads.filter { thread ->
                thread.apoiadoNome.lowercase(Locale.getDefault()).contains(normalized) ||
                    thread.apoiadoId.lowercase(Locale.getDefault()).contains(normalized)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppHeader(
                title = "Mensagens",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = GreyBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GreyBg)
        ) {
            ChatListSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" }
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = GreenSas)
                    }
                }

                state.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error!!,
                            color = ErrorRed,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                filteredThreads.isEmpty() -> {
                    val emptyText = if (searchQuery.trim().isBlank()) "Sem chats" else "Sem resultados"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyText,
                            color = GreyColor
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredThreads, key = { it.apoiadoId }) { thread ->
                            ChatThreadCard(
                                nome = thread.apoiadoNome,
                                lastMessage = thread.lastMessageText,
                                lastTime = formatListTime(thread.lastMessageAt?.toDate()?.time),
                                hasUnread = thread.unreadForStaff > 0,
                                onClick = {
                                    navController.navigate(Screen.ChatDetail.createRoute(thread.apoiadoId))
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatThreadCard(
    nome: String,
    lastMessage: String,
    lastTime: String,
    hasUnread: Boolean,
    onClick: () -> Unit
) {
    val accentColor = if (hasUnread) GreenSas else DividerGreenLight
    val avatarColor = if (hasUnread) GreenSas else GreyColor
    val avatarBackground = if (hasUnread) GreenSas.copy(alpha = 0.12f) else SurfaceMuted
    val previewColor = if (hasUnread) TextDarkGrey else GreyColor
    val nameWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold
    val initial = nome.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Card(
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, DividerGreenLight),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = avatarBackground,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, accentColor),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initial,
                            color = avatarColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nome,
                            fontSize = 16.sp,
                            fontWeight = nameWeight,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = lastTime,
                            fontSize = 12.sp,
                            color = GreyColor
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lastMessage.ifBlank { "Sem mensagens" },
                            fontSize = 14.sp,
                            color = previewColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (hasUnread) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = GreenSas.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = "Novo",
                                    color = GreenSas,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, DividerGreenLight),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = GreenSas,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(fontSize = 14.sp, color = TextDark),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isBlank()) {
                        Text(
                            text = "Pesquisar por nome ou numero mecanografico",
                            fontSize = 14.sp,
                            color = GreyColor
                        )
                    }
                    innerTextField()
                }
            )
            if (query.isNotBlank()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Limpar",
                        tint = GreyColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatListTime(millis: Long?): String {
    if (millis == null || millis <= 0L) return ""
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}
