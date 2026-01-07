package ipca.app.lojasas.data.users

import com.google.firebase.firestore.FirebaseFirestore
import ipca.app.lojasas.data.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfilesRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    fun createProfile(
        input: UserProfileInput,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userMap = hashMapOf(
            "uid" to input.uid,
            "numMecanografico" to input.numMecanografico,
            "nome" to input.nome,
            "contacto" to input.contacto,
            "documentNumber" to input.documentNumber,
            "documentType" to input.documentType,
            "morada" to input.morada,
            "codPostal" to input.codPostal,
            "email" to input.email,
            "role" to input.role.name,
            "mudarPass" to false
        )

        val collectionName = if (input.role == UserRole.APOIADO) "apoiados" else "funcionarios"
        if (input.role == UserRole.APOIADO) {
            userMap["emailApoiado"] = input.email
            userMap["dadosIncompletos"] = true
        }

        firestore.collection(collectionName)
            .document(input.numMecanografico)
            .set(userMap)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}
