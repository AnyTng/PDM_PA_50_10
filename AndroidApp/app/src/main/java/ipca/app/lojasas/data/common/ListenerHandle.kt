package ipca.app.lojasas.data.common

import com.google.firebase.firestore.ListenerRegistration

fun interface ListenerHandle {
    fun remove()
}

fun ListenerRegistration.asListenerHandle(): ListenerHandle = ListenerHandle { remove() }
