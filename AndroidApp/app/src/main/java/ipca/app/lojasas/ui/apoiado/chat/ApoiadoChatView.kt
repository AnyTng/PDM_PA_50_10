package ipca.app.lojasas.ui.apoiado.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.chat.ChatMessage
import ipca.app.lojasas.data.chat.ChatRepository
import ipca.app.lojasas.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ApoiadoChatView(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val viewModel: ApoiadoChatViewModel = hiltViewModel()
    val state by viewModel.uiState

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GreyBg)
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenSas)
            }
            return
        }

        if (state.error != null) {
            Text(
                text = state.error!!,
                color = ErrorRed,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Lista de mensagens
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.messages, key = { it.id }) { msg ->
                val isMe = msg.senderRole.equals(ChatRepository.ROLE_APOIADO, ignoreCase = true)
                ChatBubble(
                    message = msg,
                    isMe = isMe,
                    showSenderName = !isMe,
                    // Para o apoiado, as mensagens enviadas por si ficam "vistas" quando algum staff marcou seenByStaffAt.
                    isSeenByOther = if (isMe) msg.seenByStaffAt != null else false
                )
            }
        }

        // Composer
        ChatComposer(
            input = input,
            onInputChange = { input = it },
            onSend = {
                val text = input.trim()
                if (text.isNotEmpty()) {
                    viewModel.sendMessage(text)
                    input = ""
                }
            }
        )
    }
}

@Composable
private fun ChatComposer(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(color = WhiteColor, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Escreve uma mensagem…") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = SurfaceLight,
                    focusedContainerColor = SurfaceLight,
                    unfocusedIndicatorColor = TransparentColor,
                    focusedIndicatorColor = TransparentColor
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = input.trim().isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Enviar",
                    tint = if (input.trim().isNotEmpty()) GreenSas else GreyColor
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isMe: Boolean,
    showSenderName: Boolean,
    isSeenByOther: Boolean
) {
    val bubbleColor = if (isMe) GreenSas else WhiteColor
    val textColor = if (isMe) WhiteColor else BlackColor
    val align = if (isMe) Arrangement.End else Arrangement.Start
    val corner = if (isMe) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }

    val time = remember(message.createdAt, message.createdAtClient) {
        formatTime(message.createdAt ?: message.createdAtClient)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = align
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
        ) {
            if (showSenderName) {
                Text(
                    text = message.senderName.ifBlank { "Funcionário" },
                    fontSize = 12.sp,
                    color = GreyColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = bubbleColor,
                shape = corner
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = time,
                    color = GreyColor,
                    fontSize = 11.sp
                )

                if (isMe) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSeenByOther) "✓✓" else "✓",
                        color = GreyColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun formatTime(ts: com.google.firebase.Timestamp?): String {
    val date = ts?.toDate() ?: return ""
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(date)
}
