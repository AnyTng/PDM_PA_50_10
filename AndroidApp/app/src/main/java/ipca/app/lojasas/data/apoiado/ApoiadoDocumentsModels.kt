package ipca.app.lojasas.data.apoiado

data class UploadedFile(
    val id: String,
    val typeId: String,
    val typeTitle: String,
    val fileName: String,
    val storagePath: String,
    val date: Long,
    val customDescription: String? = null,
    val numeroEntrega: Int,
    val submetido: Boolean
)

data class SubmittedFile(
    val title: String,
    val fileName: String,
    val date: java.util.Date,
    val storagePath: String,
    val numeroEntrega: Int
)
