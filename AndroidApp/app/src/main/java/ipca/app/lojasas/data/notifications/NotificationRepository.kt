package ipca.app.lojasas.data.notifications

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import ipca.app.lojasas.data.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val messaging: FirebaseMessaging,
    private val firestore: FirebaseFirestore
) {
    fun configureForRole(role: UserRole) {
        val isFuncionario = role == UserRole.FUNCIONARIO || role == UserRole.ADMIN
        if (isFuncionario) {
            messaging.subscribeToTopic("funcionarios")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) Log.d(TAG, "FCM: Subscribed to funcionarios")
                    else Log.e(TAG, "FCM: Failed to subscribe funcionarios", task.exception)
                }
        } else {
            messaging.unsubscribeFromTopic("funcionarios")
        }
    }

    fun saveApoiadoToken(email: String) {
        val normalizedEmail = email.trim()
        if (normalizedEmail.isBlank()) {
            Log.e(TAG, "FCM Apoiado: email is blank")
            return
        }

        messaging.token
            .addOnSuccessListener { token ->
                fun saveTokenOnDoc(docId: String) {
                    firestore.collection("apoiados")
                        .document(docId)
                        .collection("fcmTokens")
                        .document(token)
                        .set(mapOf("updatedAt" to FieldValue.serverTimestamp()))
                        .addOnSuccessListener { Log.d(TAG, "FCM Apoiado: token saved") }
                        .addOnFailureListener { e -> Log.e(TAG, "FCM Apoiado: token save failed", e) }
                }

                firestore.collection("apoiados")
                    .whereEqualTo("emailApoiado", normalizedEmail)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snap ->
                        val doc = snap.documents.firstOrNull()
                        if (doc != null) {
                            saveTokenOnDoc(doc.id)
                        } else {
                            firestore.collection("apoiados")
                                .whereEqualTo("email", normalizedEmail)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { snap2 ->
                                    val doc2 = snap2.documents.firstOrNull()
                                    if (doc2 != null) {
                                        saveTokenOnDoc(doc2.id)
                                    } else {
                                        Log.e(TAG, "FCM Apoiado: no apoiado for email=$normalizedEmail")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "FCM Apoiado: error querying by email", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "FCM Apoiado: error querying by emailApoiado", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "FCM Apoiado: error getting FCM token", e)
            }
    }

    private companion object {
        const val TAG = "NotificationRepo"
    }
}
