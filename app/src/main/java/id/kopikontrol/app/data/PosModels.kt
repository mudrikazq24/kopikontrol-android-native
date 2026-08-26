package id.kopikontrol.app.data

data class ChargeSettings(
    val taxEnabled: Boolean = false,
    val taxRate: Double = 11.0,
    val taxAfterDiscount: Boolean = true,
    val taxIncluded: Boolean = false,
    val serviceEnabled: Boolean = false,
    val serviceRate: Double = 5.0,
)

data class ReceiptSettings(
    val storeName: String = "",
    val address: String = "",
    val footer: String = "Terima kasih.",
)

data class PrinterSettings(
    val type: String = "bluetooth",
    val paperWidth: Int = 58,
    val deviceName: String = "",
    val deviceAddress: String = "",
)

data class PosTransactionItem(
    val name: String,
    val quantity: Int,
    val price: Double,
)

data class PosTransaction(
    val id: String,
    val createdAt: Long,
    val items: List<PosTransactionItem>,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val service: Double,
    val total: Double,
    val paymentMethod: String,
    val received: Double,
)

data class PosTotals(
    val subtotal: Double,
    val discount: Double,
    val service: Double,
    val tax: Double,
    val total: Double,
)
