package ipca.app.lojasas.ui.apoiado.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import ipca.app.lojasas.data.chat.ChatMessage
import ipca.app.lojasas.data.chat.ChatRepository
import ipca.app.lojasas.ui.components.AppHeader
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
    val chatName = "Equipa Loja Social"

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppHeader(
                title = "Mensagens",
                showBack = true,
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            ChatComposer(
                input = input,
                onInputChange = { input = it },
                onSend = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        viewModel.sendMessage(text)
                        input = ""
                    }
                },
                enabled = !state.isLoading
            )
        },
        containerColor = GreyBg
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GreyBg)
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenSas)
                }
                return@Column
            }

            if (state.error != null) {
                Surface(
                    color = ErrorBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = state.error.orEmpty(),
                        color = ErrorRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            ChatContextHeader(name = chatName)

            if (state.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sem mensagens ainda",
                        color = GreyColor,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.messages, key = { it.id }) { msg ->
                        val isMe = msg.senderRole.equals(ChatRepository.ROLE_APOIADO, ignoreCase = true)
                        ChatBubble(
                            message = msg,
                            isMe = isMe,
                            showSenderName = !isMe,
                            senderNameFallback = "Equipa",
                            isSeenByOther = if (isMe) msg.seenByStaffAt != null else false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    enabled: Boolean
) {
    val canSend = enabled && input.trim().isNotEmpty()

    Surface(
        color = WhiteColor,
        shadowElevation = 8.dp,
        modifier = Modifier.imePadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = DividerLight)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = input,
                    onValueChange = onInputChange,
                    placeholder = { Text("Escreve uma mensagem...") },
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = SurfaceLight,
                        focusedContainerColor = SurfaceLight,
                        disabledContainerColor = SurfaceLight,
                        unfocusedIndicatorColor = TransparentColor,
                        focusedIndicatorColor = TransparentColor,
                        disabledIndicatorColor = TransparentColor,
                        cursorColor = GreenSas
                    ),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = onSend,
                    enabled = canSend,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (canSend) GreenSas else SurfaceLight,
                        contentColor = if (canSend) WhiteColor else GreyColor
                    ),
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar"
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isMe: Boolean,
    showSenderName: Boolean,
    senderNameFallback: String,
    isSeenByOther: Boolean
) {
    val bubbleColor = if (isMe) GreenSas else WhiteColor
    val textColor = if (isMe) WhiteColor else TextDark
    val border = if (isMe) null else BorderStroke(1.dp, DividerGreenLight)
    val align = if (isMe) Arrangement.End else Arrangement.Start
    val corner = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 18.dp)
    }

    val time = remember(message.createdAt, message.createdAtClient) {
        formatTime(message.createdAt ?: message.createdAtClient)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = align
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            if (showSenderName) {
                Text(
                    text = message.senderName.ifBlank { senderNameFallback },
                    fontSize = 12.sp,
                    color = GreyColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                color = bubbleColor,
                shape = corner,
                border = border,
                shadowElevation = if (isMe) 2.dp else 0.dp
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
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
                    Icon(
                        imageVector = if (isSeenByOther) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = null,
                        tint = if (isSeenByOther) GreenSas else GreyColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatContextHeader(name: String) {
    if (name.isBlank()) return

    Card(
        colors = CardDefaults.cardColors(containerColor = WhiteColor),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, DividerGreenLight),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Conversa com",
                    fontSize = 12.sp,
                    color = GreyColor
                )
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = GreenSas.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = GreenSas,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Equipa",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenSas
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
