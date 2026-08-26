package id.kopikontrol.app.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import id.kopikontrol.app.data.ChargeSettings
import id.kopikontrol.app.data.BluetoothPrinterManager
import id.kopikontrol.app.data.PosStore
import id.kopikontrol.app.data.PosTotals
import id.kopikontrol.app.data.PosTransaction
import id.kopikontrol.app.data.PosTransactionItem
import id.kopikontrol.app.data.RecipeComponent
import id.kopikontrol.app.data.RecipeSummary
import id.kopikontrol.app.data.SystemReceiptPrinter
import id.kopikontrol.app.data.WorkspaceData
import id.kopikontrol.app.ui.theme.Caramel
import id.kopikontrol.app.ui.theme.Coffee
import id.kopikontrol.app.ui.theme.Danger
import id.kopikontrol.app.ui.theme.Line
import id.kopikontrol.app.ui.theme.Muted
import id.kopikontrol.app.ui.theme.Paper
import id.kopikontrol.app.ui.theme.Success
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.round
import kotlinx.coroutines.launch

private enum class PosPage { Catalog, Order, Payment, Scanner, History }

@Composable
fun PosScreen(workspace: WorkspaceData) {
    val context = LocalContext.current
    val store = remember { PosStore(context) }
    val printerManager = remember { BluetoothPrinterManager(context) }
    val systemPrinter = remember { SystemReceiptPrinter(context) }
    val scope = rememberCoroutineScope()
    var pendingBluetoothPrint by remember { mutableStateOf<PosTransaction?>(null) }
    val printerPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val transaction = pendingBluetoothPrint
        if (!granted) Toast.makeText(context, "Izin Perangkat di sekitar diperlukan untuk mencetak.", Toast.LENGTH_LONG).show()
        else if (transaction != null) scope.launch {
            val printer = store.printerSettings()
            val result = printerManager.print(printer.deviceAddress, receiptText(transaction, workspace, store))
            store.savePrinterConnection(printer.deviceAddress, result.isSuccess)
            Toast.makeText(context, result.exceptionOrNull()?.message ?: "Struk dikirim ke printer.", Toast.LENGTH_LONG).show()
        }
        pendingBluetoothPrint = null
    }
    val cart = remember { mutableStateMapOf<String, Int>() }
    var page by remember { mutableStateOf(PosPage.Catalog) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var discountText by remember { mutableStateOf("") }
    var payment by remember { mutableStateOf("") }
    var receivedText by remember { mutableStateOf("") }
    var completed by remember { mutableStateOf<PosTransaction?>(null) }
    var localSkus by remember { mutableStateOf(store.productSkus()) }
    val products = workspace.recipes.map { recipe -> recipe.copy(sku = recipe.sku.ifBlank { localSkus[recipe.id].orEmpty() }) }
    val settings = remember { store.chargeSettings() }
    val available = remember(workspace) { workspace.recipes.associate { it.id to canMake(it, workspace) } }
    val categories = listOf("Semua") + products.map { it.category }.distinct()
    val selectedRecipes = products.filter { (cart[it.id] ?: 0) > 0 }
    val totalItems = cart.values.sum()
    val discount = discountText.digitsValue()
    val totals = calculateTotals(selectedRecipes, cart, discount, settings)
    fun printTransaction(transaction: PosTransaction) {
        val printer = store.printerSettings(); val receipt = receiptText(transaction, workspace, store)
        if (printer.type == "system") systemPrinter.print(transaction.id, receipt, if (printer.paperWidth == 33) 33 else 42)
        else if (printer.deviceAddress.isBlank()) Toast.makeText(context, "Pilih printer Bluetooth di menu Pengaturan.", Toast.LENGTH_LONG).show()
        else if (printerManager.requiresPermission()) { pendingBluetoothPrint = transaction; printerPermission.launch(Manifest.permission.BLUETOOTH_CONNECT) }
        else scope.launch {
            val result = printerManager.print(printer.deviceAddress, receipt)
            store.savePrinterConnection(printer.deviceAddress, result.isSuccess)
            Toast.makeText(context, result.exceptionOrNull()?.message ?: "Struk dikirim ke printer.", Toast.LENGTH_LONG).show()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 700.dp
        when {
            page == PosPage.Scanner -> ScannerPage(products, { page = PosPage.Catalog }, { recipe ->
                if (available[recipe.id] == true) cart[recipe.id] = (cart[recipe.id] ?: 0) + 1
                page = PosPage.Catalog
            }, { recipe, sku -> store.saveProductSku(recipe.id, sku); localSkus = store.productSkus() })
            page == PosPage.History -> HistoryPage(store.transactions(), { page = PosPage.Catalog }, ::printTransaction)
            page == PosPage.Payment -> PaymentPage(
                totals, payment, { payment = it }, receivedText, { receivedText = formatDigits(it) },
                cash = payment == "Tunai", back = { page = if (compact) PosPage.Order else PosPage.Catalog },
                finish = {
                    val received = receivedText.digitsValue()
                    val now = System.currentTimeMillis()
                    val transaction = PosTransaction(
                        id = "TRX-${SimpleDateFormat("ddHHmmss", Locale.US).format(Date(now))}", createdAt = now,
                        items = selectedRecipes.map { PosTransactionItem(it.name, cart[it.id] ?: 0, it.price) },
                        subtotal = totals.subtotal, discount = totals.discount, tax = totals.tax, service = totals.service,
                        total = totals.total, paymentMethod = payment, received = received,
                    )
                    store.addTransaction(transaction); completed = transaction
                }
            )
            compact && page == PosPage.Order -> OrderPanel(
                selectedRecipes, cart, discountText, { discountText = formatDigits(it) }, totals,
                back = { page = PosPage.Catalog }, pay = { page = PosPage.Payment }, compact = true,
            )
            else -> {
                if (compact) CatalogPanel(products, available, cart, query, { query = it }, category, { category = it }, categories, { page = PosPage.Scanner }, { page = PosPage.History })
                else Row(Modifier.fillMaxSize()) {
                    CatalogPanel(products, available, cart, query, { query = it }, category, { category = it }, categories, { page = PosPage.Scanner }, { page = PosPage.History }, Modifier.weight(1f))
                    OrderPanel(selectedRecipes, cart, discountText, { discountText = formatDigits(it) }, totals, back = null, pay = { page = PosPage.Payment }, compact = false, modifier = Modifier.width(360.dp))
                }
                if (compact && totalItems > 0) FloatingActionButton(
                    onClick = { page = PosPage.Order }, containerColor = Coffee, contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(18.dp),
                ) { Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) { Text("Pesanan", fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); QuantityBadge(totalItems) } }
            }
        }
    }

    completed?.let { transaction ->
        AlertDialog(
            onDismissRequest = {}, icon = { Box(Modifier.size(52.dp).background(Color(0xFFE3ECE8), CircleShape), contentAlignment = Alignment.Center) { Text("✓", color = Success, style = MaterialTheme.typography.headlineMedium) } },
            title = { Text("Transaksi berhasil") }, text = { Text("${transaction.id} · ${rupiahPos(transaction.total)}") },
            confirmButton = { Button(onClick = { completed = null; cart.clear(); discountText = ""; payment = ""; receivedText = ""; page = PosPage.Catalog }) { Text("Transaksi Baru") } },
            dismissButton = { OutlinedButton(onClick = { printTransaction(transaction) }) { Text("Cetak Struk") } },
        )
    }
}

@Composable
private fun CatalogPanel(
    recipes: List<RecipeSummary>, available: Map<String, Boolean>, cart: MutableMap<String, Int>, query: String, onQuery: (String) -> Unit,
    category: String, onCategory: (String) -> Unit, categories: List<String>, scan: () -> Unit, history: () -> Unit, modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxHeight().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, onQuery, Modifier.weight(1f), placeholder = { Text("Cari nama atau SKU") }, singleLine = true)
            Spacer(Modifier.width(8.dp)); IconButton(scan) { Icon(Icons.Outlined.QrCodeScanner, "Scan barcode") }; IconButton(history) { Icon(Icons.Outlined.History, "Riwayat") }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { categories.take(5).forEach { value ->
            Surface(Modifier.clickable { onCategory(value) }, color = if (category == value) Coffee else Paper, contentColor = if (category == value) Color.White else Coffee, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (category == value) Coffee else Line)) { Text(value, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge) }
        } }
        Spacer(Modifier.height(12.dp))
        val visible = recipes.filter { (category == "Semua" || it.category == category) && (query.isBlank() || it.name.contains(query, true) || it.sku.contains(query, true)) }
        LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), contentPadding = PaddingValues(bottom = 90.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(visible, key = { it.id.ifBlank { it.name } }) { recipe -> ProductCard(recipe, cart[recipe.id] ?: 0, available[recipe.id] == true) { cart[recipe.id] = (cart[recipe.id] ?: 0) + 1 } }
        }
    }
}

@Composable private fun ProductCard(recipe: RecipeSummary, quantity: Int, enabled: Boolean, add: () -> Unit) {
    Card(Modifier.height(160.dp).clickable(enabled = enabled, onClick = add), colors = CardDefaults.cardColors(containerColor = if (enabled) Paper else Color(0xFFF0EDE8)), border = BorderStroke(1.dp, if (quantity > 0) Caramel else Line)) {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            Column { Box(Modifier.fillMaxWidth().height(64.dp).background(Color(0xFFF4EEE8), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Coffee, null, tint = if (enabled) Caramel else Muted, modifier = Modifier.size(34.dp)) }; Spacer(Modifier.height(8.dp)); Text(recipe.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(if (enabled) recipe.category else "Stok bahan kurang", color = if (enabled) Muted else Danger, style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.weight(1f)); Text(rupiahPos(recipe.price), color = Caramel, fontWeight = FontWeight.Bold) }
            if (quantity > 0) Box(Modifier.align(Alignment.TopEnd)) { QuantityBadge(quantity) }
        }
    }
}

@Composable private fun QuantityBadge(quantity: Int) { Box(Modifier.size(25.dp).background(Coffee, CircleShape).border(2.dp, Paper, CircleShape), contentAlignment = Alignment.Center) { Text(quantity.toString(), color = Color.White, style = MaterialTheme.typography.labelLarge) } }

@Composable
private fun OrderPanel(recipes: List<RecipeSummary>, cart: MutableMap<String, Int>, discount: String, onDiscount: (String) -> Unit, totals: PosTotals, back: (() -> Unit)?, pay: () -> Unit, compact: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight().background(Paper).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { if (back != null) IconButton(back) { Icon(Icons.Outlined.ArrowBack, "Kembali") }; Column { Text("Pesanan", style = MaterialTheme.typography.titleLarge); Text("${cart.values.sum()} produk dipilih", color = Muted) } }
        Spacer(Modifier.height(12.dp))
        if (recipes.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Belum ada menu dipilih.", color = Muted) }
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(recipes, key = { it.id }) { recipe ->
            Row(Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(10.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(recipe.name, fontWeight = FontWeight.Bold); Text(rupiahPos(recipe.price), color = Muted) }
                IconButton({ val next = (cart[recipe.id] ?: 1) - 1; if (next <= 0) cart.remove(recipe.id) else cart[recipe.id] = next }) { Icon(Icons.Outlined.Remove, "Kurangi") }
                Text((cart[recipe.id] ?: 0).toString(), fontWeight = FontWeight.Bold)
                IconButton({ cart[recipe.id] = (cart[recipe.id] ?: 0) + 1 }) { Icon(Icons.Outlined.Add, "Tambah") }
            }
        } }
        OutlinedTextField(discount, onDiscount, Modifier.fillMaxWidth(), label = { Text("Diskon") }, prefix = { Text("Rp ") }, placeholder = { Text("0") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
        Spacer(Modifier.height(10.dp)); TotalRow("Subtotal", totals.subtotal); if (totals.discount > 0) TotalRow("Diskon", -totals.discount); if (totals.service > 0) TotalRow("Service charge", totals.service); if (totals.tax > 0) TotalRow("Pajak", totals.tax)
        Spacer(Modifier.height(5.dp)); TotalRow("Total", totals.total, true); Spacer(Modifier.height(12.dp)); Button(onClick = pay, enabled = recipes.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(if (compact) "Lanjutkan Pembayaran" else "Bayar Pesanan") }
    }
}

@Composable private fun TotalRow(label: String, value: Double, strong: Boolean = false) { Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text(label, fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal); Spacer(Modifier.weight(1f)); Text((if (value < 0) "-" else "") + rupiahPos(kotlin.math.abs(value)), fontWeight = if (strong) FontWeight.Bold else FontWeight.Normal) } }

@Composable
private fun PaymentPage(totals: PosTotals, payment: String, onPayment: (String) -> Unit, received: String, onReceived: (String) -> Unit, cash: Boolean, back: () -> Unit, finish: () -> Unit) {
    val receivedValue = received.digitsValue(); val insufficient = receivedValue > 0 && receivedValue < totals.total
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Outlined.ArrowBack, "Kembali") }; Text("Pembayaran", style = MaterialTheme.typography.headlineMedium) }
        Spacer(Modifier.height(18.dp)); Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Coffee)) { Column(Modifier.padding(20.dp)) { Text("TOTAL TAGIHAN", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.labelMedium); Text(rupiahPos(totals.total), color = Color.White, style = MaterialTheme.typography.headlineLarge) } }
        Spacer(Modifier.height(18.dp)); SelectPayment(payment, onPayment)
        Spacer(Modifier.height(14.dp)); OutlinedTextField(received, onReceived, Modifier.fillMaxWidth(), label = { Text("Uang diterima *") }, placeholder = { Text("Contoh: 20.000") }, prefix = { Text("Rp ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = insufficient)
        if (insufficient) Text("Nominal kurang ${rupiahPos(totals.total - receivedValue)} dari total tagihan.", color = Danger, modifier = Modifier.padding(top = 7.dp))
        if (cash) { Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf(10_000, 20_000, 50_000, 100_000).forEach { nominal -> OutlinedButton(onClick = { onReceived(formatDigits(nominal.toString())) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("${nominal / 1000}rb") } } } }
        if (receivedValue >= totals.total) { Spacer(Modifier.height(14.dp)); TotalRow("Kembalian", receivedValue - totals.total, true) }
        Spacer(Modifier.weight(1f)); Button(onClick = finish, enabled = payment.isNotBlank() && receivedValue >= totals.total && receivedValue > 0, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Simpan Transaksi") }
    }
}

@Composable private fun SelectPayment(value: String, onChange: (String) -> Unit) { var open by remember { mutableStateOf(false) }; Column { Text("Metode pembayaran *", style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(7.dp)); Box { OutlinedButton({ open = true }, Modifier.fillMaxWidth().height(52.dp)) { Text(value.ifBlank { "Pilih metode pembayaran" }, color = if (value.isBlank()) Muted else Coffee, modifier = Modifier.weight(1f)); Text("⌄") }; DropdownMenu(open, { open = false }) { listOf("Tunai", "QRIS", "Kartu debit", "Transfer bank").forEach { method -> DropdownMenuItem({ Text(method) }, onClick = { onChange(method); open = false }) } } } } }

@Composable private fun ScannerPage(recipes: List<RecipeSummary>, back: () -> Unit, select: (RecipeSummary) -> Unit, linkSku: (RecipeSummary, String) -> Unit) {
    var sku by remember { mutableStateOf("") }; var feedback by remember { mutableStateOf("") }; var linkOpen by remember { mutableStateOf(false) }
    val found = recipes.firstOrNull { it.sku.isNotBlank() && it.sku.equals(sku.trim(), true) }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Outlined.ArrowBack, "Kembali") }; Text("Scan Barcode", style = MaterialTheme.typography.headlineMedium) }
        Spacer(Modifier.height(18.dp)); BarcodeCamera { value -> sku = value; val product = recipes.firstOrNull { it.sku.isNotBlank() && it.sku.equals(value, true) }; if (product != null) select(product) else feedback = "Produk dengan SKU $value tidak ditemukan." }
        Spacer(Modifier.height(16.dp)); Text("Kamera membaca barcode otomatis. Scanner Bluetooth/USB dan input manual tetap dapat digunakan.", color = Muted)
        if (feedback.isNotBlank()) Text(feedback, color = Danger, modifier = Modifier.padding(top = 8.dp))
        if (feedback.isNotBlank() && sku.isNotBlank()) { Spacer(Modifier.height(8.dp)); Box { OutlinedButton(onClick = { linkOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Tautkan barcode ke produk", modifier = Modifier.weight(1f)); Text("⌄") }; DropdownMenu(linkOpen, { linkOpen = false }) { recipes.forEach { recipe -> DropdownMenuItem({ Text(recipe.name) }, onClick = { linkSku(recipe, sku); linkOpen = false; select(recipe) }) } } } }
        Spacer(Modifier.height(10.dp)); OutlinedTextField(sku, { sku = it; feedback = "" }, Modifier.fillMaxWidth(), label = { Text("SKU produk") }, placeholder = { Text("Scan atau ketik SKU") }, singleLine = true)
        Spacer(Modifier.height(12.dp)); Button(onClick = { found?.let(select) }, enabled = found != null, modifier = Modifier.fillMaxWidth()) { Text(if (found == null) "Cari produk" else "Tambahkan ${found.name}") }
    }
}
@Composable private fun HistoryPage(transactions: List<PosTransaction>, back: () -> Unit, print: (PosTransaction) -> Unit) { Column(Modifier.fillMaxSize().padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Outlined.ArrowBack, "Kembali") }; Text("Riwayat Transaksi", style = MaterialTheme.typography.headlineMedium) }; Spacer(Modifier.height(12.dp)); if (transactions.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada transaksi tersimpan.", color = Muted) } else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) { items(transactions, key = { it.id }) { transaction -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Paper), border = BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(transaction.id, fontWeight = FontWeight.Bold); Text(SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(transaction.createdAt)), color = Muted) }; Column(horizontalAlignment = Alignment.End) { Text(rupiahPos(transaction.total), fontWeight = FontWeight.Bold); TextButton(onClick = { print(transaction) }) { Text("Cetak ulang") } } } } } } } }

private fun calculateTotals(recipes: List<RecipeSummary>, cart: Map<String, Int>, requestedDiscount: Double, settings: ChargeSettings): PosTotals {
    val subtotal = recipes.sumOf { it.price * (cart[it.id] ?: 0) }; val discount = requestedDiscount.coerceIn(0.0, subtotal); val net = max(0.0, subtotal - discount)
    val service = if (settings.serviceEnabled) round(net * settings.serviceRate / 100.0) else 0.0
    val taxBase = if (settings.taxAfterDiscount) net else subtotal
    val tax = if (!settings.taxEnabled) 0.0 else round(if (settings.taxIncluded) taxBase - taxBase / (1 + settings.taxRate / 100.0) else taxBase * settings.taxRate / 100.0)
    return PosTotals(subtotal, discount, service, tax, net + service + if (settings.taxEnabled && !settings.taxIncluded) tax else 0.0)
}

private fun canMake(recipe: RecipeSummary, workspace: WorkspaceData): Boolean {
    if (recipe.components.isEmpty()) return true
    val ingredientStock = workspace.ingredients.associate { it.id to parseStock(it.stock) }
    val subrecipes = workspace.subrecipes.associateBy { it.id }
    fun enough(component: RecipeComponent, multiplier: Double): Boolean {
        if (component.kind == "ingredient") return (ingredientStock[component.sourceId] ?: Double.MAX_VALUE) >= component.quantity * multiplier
        val sub = subrecipes[component.sourceId] ?: return false
        val batches = if (sub.output > 0) component.quantity * multiplier / sub.output else multiplier
        return sub.components.all { enough(it, batches) }
    }
    return recipe.components.all { enough(it, 1.0) }
}

private fun parseStock(value: String): Double { val match = Regex("([\\d.,]+)\\s*([a-zA-Z]*)").find(value) ?: return Double.MAX_VALUE; val amount = match.groupValues[1].replace(".", "").replace(',', '.').toDoubleOrNull() ?: return Double.MAX_VALUE; val unit = match.groupValues[2].lowercase(); return if (unit.startsWith("kg") || unit.startsWith("kilo") || unit.startsWith("liter")) amount * 1000 else amount }
private fun String.digitsValue(): Double = filter(Char::isDigit).toDoubleOrNull() ?: 0.0
private fun formatDigits(value: String): String { val digits = value.filter(Char::isDigit).trimStart('0'); if (digits.isBlank()) return ""; return NumberFormat.getNumberInstance(Locale("id", "ID")).format(digits.toLongOrNull() ?: 0) }
private fun rupiahPos(value: Double): String = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(value)}"
private fun receiptText(transaction: PosTransaction, workspace: WorkspaceData, store: PosStore): String {
    val receipt = store.receiptSettings(); val storeName = receipt.storeName.ifBlank { workspace.profile?.name.orEmpty() }.uppercase(); val address = receipt.address.ifBlank { listOfNotNull(workspace.profile?.city, workspace.profile?.province).filter { it.isNotBlank() }.joinToString(", ") }
    return buildString {
        appendLine(storeName); if (address.isNotBlank()) appendLine(address); appendLine("--------------------------------")
        appendLine(transaction.id); transaction.items.forEach { appendLine("${it.quantity}x ${it.name}"); appendLine("   ${rupiahPos(it.price * it.quantity)}") }
        appendLine("--------------------------------"); appendLine("Subtotal  ${rupiahPos(transaction.subtotal)}"); if (transaction.discount > 0) appendLine("Diskon   -${rupiahPos(transaction.discount)}"); if (transaction.service > 0) appendLine("Layanan   ${rupiahPos(transaction.service)}"); if (transaction.tax > 0) appendLine("Pajak     ${rupiahPos(transaction.tax)}"); appendLine("TOTAL     ${rupiahPos(transaction.total)}"); appendLine("${transaction.paymentMethod}  ${rupiahPos(transaction.received)}"); appendLine("Kembali   ${rupiahPos(transaction.received - transaction.total)}"); appendLine(); appendLine(receipt.footer)
    }
}
