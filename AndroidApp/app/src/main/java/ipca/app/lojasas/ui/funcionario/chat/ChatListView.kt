package ipca.app.lojasas.ui.funcionario.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.core.navigation.Screen
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GreyBg)
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    color = GreenSas,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            state.error != null -> {
                Text(
                    text = state.error!!,
                    color = ErrorRed,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }

            else -> {
                if (state.threads.isEmpty()) {
                    Text(
                        text = "Sem chats",
                        color = GreyColor,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.threads, key = { it.apoiadoId }) { thread ->
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
    Card(
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de não lidas
            if (hasUnread) {
                Badge(
                    containerColor = DangerRed,
                    modifier = Modifier.padding(end = 10.dp)
                )
            } else {
                Spacer(modifier = Modifier.width(24.dp))
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
                        fontWeight = FontWeight.SemiBold,
                        color = BlackColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = lastTime,
                        fontSize = 12.sp,
                        color = GreyColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = lastMessage.ifBlank { "Sem mensagens" },
                    fontSize = 14.sp,
                    color = GreyColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatListTime(millis: Long?): String {
    if (millis == null || millis <= 0L) return ""
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}
