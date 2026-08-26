package id.kopikontrol.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class PosStore(context: Context) {
    private val preferences = context.getSharedPreferences("kopikontrol_pos", Context.MODE_PRIVATE)

    fun chargeSettings(): ChargeSettings {
        val json = preferences.getString("charges", null)?.let(::JSONObject) ?: return ChargeSettings()
        return ChargeSettings(
            taxEnabled = json.optBoolean("taxEnabled"), taxRate = json.optDouble("taxRate", 11.0),
            taxAfterDiscount = json.optBoolean("taxAfterDiscount", true), taxIncluded = json.optBoolean("taxIncluded"),
            serviceEnabled = json.optBoolean("serviceEnabled"), serviceRate = json.optDouble("serviceRate", 5.0),
        )
    }

    fun saveChargeSettings(value: ChargeSettings) {
        preferences.edit().putString("charges", JSONObject()
            .put("taxEnabled", value.taxEnabled).put("taxRate", value.taxRate)
            .put("taxAfterDiscount", value.taxAfterDiscount).put("taxIncluded", value.taxIncluded)
            .put("serviceEnabled", value.serviceEnabled).put("serviceRate", value.serviceRate).toString()).apply()
    }

    fun receiptSettings(): ReceiptSettings {
        val json = preferences.getString("receipt", null)?.let(::JSONObject) ?: return ReceiptSettings()
        return ReceiptSettings(json.optString("storeName"), json.optString("address"), json.optString("footer", "Terima kasih."))
    }

    fun saveReceiptSettings(value: ReceiptSettings) {
        preferences.edit().putString("receipt", JSONObject().put("storeName", value.storeName).put("address", value.address).put("footer", value.footer).toString()).apply()
    }

    fun printerSettings(): PrinterSettings {
        val json = preferences.getString("printer", null)?.let(::JSONObject) ?: return PrinterSettings()
        return PrinterSettings(json.optString("type", "bluetooth"), json.optInt("paperWidth", 58), json.optString("deviceName"), json.optString("deviceAddress"))
    }

    fun savePrinterSettings(value: PrinterSettings) {
        preferences.edit().putString("printer", JSONObject().put("type", value.type).put("paperWidth", value.paperWidth).put("deviceName", value.deviceName).put("deviceAddress", value.deviceAddress).toString()).apply()
    }

    fun printerConnected(address: String): Boolean = address.isNotBlank() &&
        preferences.getBoolean("printer_connected", false) && preferences.getString("printer_connected_address", "") == address

    fun savePrinterConnection(address: String, connected: Boolean) {
        preferences.edit().putString("printer_connected_address", address).putBoolean("printer_connected", connected).apply()
    }

    fun transactions(): List<PosTransaction> {
        val array = runCatching { JSONArray(preferences.getString("transactions", "[]").orEmpty()) }.getOrDefault(JSONArray())
        return (0 until array.length()).map { array.getJSONObject(it) }.map { json ->
            val items = json.optJSONArray("items") ?: JSONArray()
            PosTransaction(
                id = json.optString("id"), createdAt = json.optLong("createdAt"),
                items = (0 until items.length()).map { items.getJSONObject(it) }.map { PosTransactionItem(it.optString("name"), it.optInt("quantity"), it.optDouble("price")) },
                subtotal = json.optDouble("subtotal"), discount = json.optDouble("discount"), tax = json.optDouble("tax"), service = json.optDouble("service"),
                total = json.optDouble("total"), paymentMethod = json.optString("paymentMethod"), received = json.optDouble("received"),
            )
        }
    }

    fun addTransaction(value: PosTransaction) {
        val data = (listOf(value) + transactions()).take(100)
        val array = JSONArray()
        data.forEach { transaction ->
            val items = JSONArray()
            transaction.items.forEach { items.put(JSONObject().put("name", it.name).put("quantity", it.quantity).put("price", it.price)) }
            array.put(JSONObject().put("id", transaction.id).put("createdAt", transaction.createdAt).put("items", items)
                .put("subtotal", transaction.subtotal).put("discount", transaction.discount).put("tax", transaction.tax)
                .put("service", transaction.service).put("total", transaction.total).put("paymentMethod", transaction.paymentMethod).put("received", transaction.received))
        }
        preferences.edit().putString("transactions", array.toString()).apply()
    }

    fun productSkus(): Map<String, String> {
        val json = runCatching { JSONObject(preferences.getString("product_skus", "{}").orEmpty()) }.getOrDefault(JSONObject())
        return json.keys().asSequence().associateWith { json.optString(it) }.filterValues { it.isNotBlank() }
    }

    fun saveProductSku(recipeId: String, sku: String) {
        val values = productSkus().toMutableMap()
        if (sku.isBlank()) values.remove(recipeId) else values[recipeId] = sku.trim()
        val json = JSONObject(); values.forEach { (id, value) -> json.put(id, value) }
        preferences.edit().putString("product_skus", json.toString()).apply()
    }
}
