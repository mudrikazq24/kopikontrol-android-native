package id.kopikontrol.app.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.kopikontrol.app.data.BluetoothPrinterManager
import id.kopikontrol.app.data.ChargeSettings
import id.kopikontrol.app.data.PosStore
import id.kopikontrol.app.data.PrinterSettings
import id.kopikontrol.app.data.ReceiptSettings
import id.kopikontrol.app.data.StoreProfile
import id.kopikontrol.app.data.SystemReceiptPrinter
import id.kopikontrol.app.ui.theme.Coffee
import id.kopikontrol.app.ui.theme.Line
import id.kopikontrol.app.ui.theme.Muted
import id.kopikontrol.app.ui.theme.Paper
import kotlinx.coroutines.launch

private enum class SettingsTab(val label: String, val icon: ImageVector) { Printer("Printer", Icons.Outlined.Print), Receipt("Nota", Icons.Outlined.ReceiptLong), Charges("Pajak & Layanan", Icons.Outlined.Percent) }

@Composable
fun SettingsScreen(profile: StoreProfile?) {
    val context = LocalContext.current
    val store = remember { PosStore(context) }
    val printerManager = remember { BluetoothPrinterManager(context) }
    var tab by remember { mutableStateOf(SettingsTab.Printer) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 700.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(if (compact) 14.dp else 22.dp)) {
            Text("Pengaturan", style = MaterialTheme.typography.headlineMedium); Text("Kelola perangkat, tampilan nota, dan biaya transaksi.", color = Muted)
            Spacer(Modifier.height(16.dp))
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { SettingsTab.entries.forEach { SettingsTabCard(it, tab == it, Modifier.fillMaxWidth()) { tab = it } } }
            else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { SettingsTab.entries.forEach { SettingsTabCard(it, tab == it, Modifier.weight(1f)) { tab = it } } }
            Spacer(Modifier.height(20.dp))
            when (tab) { SettingsTab.Printer -> PrinterSettingsCard(store, printerManager); SettingsTab.Receipt -> ReceiptSettingsCard(store, profile); SettingsTab.Charges -> ChargeSettingsCard(store) }
            Spacer(Modifier.height(90.dp))
        }
    }
}

@Composable private fun SettingsTabCard(tab: SettingsTab, selected: Boolean, modifier: Modifier, select: () -> Unit) { Card(modifier.clickable(onClick = select), colors = CardDefaults.cardColors(containerColor = Paper), border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) Coffee else Line)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.background(if (selected) Coffee else Color(0xFFF4EEE8), RoundedCornerShape(9.dp)).padding(10.dp)) { Icon(tab.icon, null, tint = if (selected) Color.White else Coffee) }; Spacer(Modifier.width(10.dp)); Text(tab.label, fontWeight = FontWeight.Bold) } } }

@Composable private fun PrinterSettingsCard(store: PosStore, manager: BluetoothPrinterManager) {
    val context = LocalContext.current; val scope = rememberCoroutineScope(); var saved by remember { mutableStateOf(store.printerSettings()) }; var draft by remember { mutableStateOf(saved) }; var devicesOpen by remember { mutableStateOf(false) }; var refresh by remember { mutableStateOf(0) }; var testAfterPermission by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(context, "Izin Perangkat di sekitar diperlukan untuk printer.", Toast.LENGTH_LONG).show()
        else {
            refresh++
            if (testAfterPermission) scope.launch {
                val result = manager.print(draft.deviceAddress, "KOPIKONTROL\nTes cetak berhasil\n")
                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Tes cetak dikirim.", Toast.LENGTH_LONG).show()
            }
        }
        testAfterPermission = false
    }
    val devices = remember(refresh) { manager.pairedDevices() }
    SettingCard("Pengaturan Printer", "Hubungkan printer thermal Bluetooth atau gunakan printer sistem.") {
        SettingChoice("Jenis printer", draft.type, listOf("bluetooth" to "Thermal Bluetooth", "system" to "Dot matrix / Printer sistem")) { draft = draft.copy(type = it) }
        if (draft.type == "bluetooth") {
            Text("Printer terhubung", style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(7.dp)); Box { OutlinedButton(onClick = { if (manager.requiresPermission()) permission.launch(Manifest.permission.BLUETOOTH_CONNECT) else devicesOpen = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(draft.deviceName.ifBlank { "Pilih perangkat yang sudah dipasangkan" }, modifier = Modifier.weight(1f)); Text("⌄") }; DropdownMenu(devicesOpen, { devicesOpen = false }) { if (devices.isEmpty()) DropdownMenuItem({ Text("Tidak ada perangkat terpasang") }, enabled = false, onClick = {}) else devices.forEach { device -> DropdownMenuItem({ Text(device.name) }, onClick = { draft = draft.copy(deviceName = device.name, deviceAddress = device.address); devicesOpen = false }) } } }
            Spacer(Modifier.height(14.dp)); SettingChoice("Ukuran kertas", draft.paperWidth.toString(), listOf("58" to "58 mm", "72" to "72 mm", "80" to "80 mm")) { draft = draft.copy(paperWidth = it.toInt()) }
        } else SettingChoice("Ukuran kertas", draft.paperWidth.toString(), listOf("72" to "Continuous form 72 mm", "33" to "Continuous form 33 kolom")) { draft = draft.copy(paperWidth = it.toInt()) }
        Spacer(Modifier.height(16.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = {
            if (draft.type == "system") SystemReceiptPrinter(context).print("TES-CETAK", "KOPIKONTROL\nTes cetak printer sistem\n\n", if (draft.paperWidth == 33) 33 else 42)
            else if (manager.requiresPermission()) { testAfterPermission = true; permission.launch(Manifest.permission.BLUETOOTH_CONNECT) }
            else scope.launch { val result = manager.print(draft.deviceAddress, "KOPIKONTROL\nTes cetak berhasil\n"); Toast.makeText(context, result.exceptionOrNull()?.message ?: "Tes cetak dikirim.", Toast.LENGTH_LONG).show() }
        }, enabled = draft.type == "system" || draft.deviceAddress.isNotBlank()) { Text("Tes Cetak") }; Button(onClick = { store.savePrinterSettings(draft); saved = draft; Toast.makeText(context, "Pengaturan printer disimpan.", Toast.LENGTH_SHORT).show() }, enabled = draft != saved) { Text("Simpan Pengaturan") } }
    }
}

@Composable private fun ReceiptSettingsCard(store: PosStore, profile: StoreProfile?) { var saved by remember { mutableStateOf(store.receiptSettings()) }; var draft by remember { mutableStateOf(saved) }; val context = LocalContext.current; SettingCard("Tampilan Nota", "Kosongkan nama atau alamat untuk mengambil data dari profil kedai.") { OutlinedTextField(draft.storeName, { draft = draft.copy(storeName = it) }, Modifier.fillMaxWidth(), label = { Text("Nama kedai") }, placeholder = { Text(profile?.name.orEmpty().uppercase()) }); Spacer(Modifier.height(12.dp)); OutlinedTextField(draft.address, { draft = draft.copy(address = it) }, Modifier.fillMaxWidth(), label = { Text("Alamat") }, placeholder = { Text(listOfNotNull(profile?.city, profile?.province).joinToString(", ")) }); Spacer(Modifier.height(12.dp)); OutlinedTextField(draft.footer, { draft = draft.copy(footer = it) }, Modifier.fillMaxWidth(), label = { Text("Footer nota") }, minLines = 2); Spacer(Modifier.height(16.dp)); Button(onClick = { store.saveReceiptSettings(draft); saved = draft; Toast.makeText(context, "Pengaturan nota disimpan.", Toast.LENGTH_SHORT).show() }, enabled = draft != saved) { Text("Simpan Pengaturan") } } }

@Composable private fun ChargeSettingsCard(store: PosStore) { var saved by remember { mutableStateOf(store.chargeSettings()) }; var draft by remember { mutableStateOf(saved) }; val context = LocalContext.current; SettingCard("Pajak & Service Charge", "Biaya yang aktif otomatis diterapkan pada transaksi POS.") { ToggleRow("Aktifkan pajak", if (draft.taxEnabled) "${draft.taxRate}% · ${if (draft.taxIncluded) "Include" else "Exclude"}" else "Tidak diterapkan", draft.taxEnabled) { draft = draft.copy(taxEnabled = it) }; if (draft.taxEnabled) { PercentField("Persentase pajak", draft.taxRate) { draft = draft.copy(taxRate = it) }; Spacer(Modifier.height(10.dp)); SettingChoice("Perhitungan pajak", if (draft.taxAfterDiscount) "after" else "before", listOf("after" to "Setelah diskon", "before" to "Sebelum diskon")) { draft = draft.copy(taxAfterDiscount = it == "after") }; Spacer(Modifier.height(10.dp)); SettingChoice("Sifat pajak", if (draft.taxIncluded) "include" else "exclude", listOf("exclude" to "Exclude", "include" to "Include")) { draft = draft.copy(taxIncluded = it == "include") } }; Spacer(Modifier.height(18.dp)); ToggleRow("Aktifkan service charge", if (draft.serviceEnabled) "${draft.serviceRate}% dari subtotal setelah diskon" else "Tidak diterapkan", draft.serviceEnabled) { draft = draft.copy(serviceEnabled = it) }; if (draft.serviceEnabled) PercentField("Persentase service charge", draft.serviceRate) { draft = draft.copy(serviceRate = it) }; Spacer(Modifier.height(18.dp)); Button(onClick = { store.saveChargeSettings(draft); saved = draft; Toast.makeText(context, "Pengaturan biaya disimpan.", Toast.LENGTH_SHORT).show() }, enabled = draft != saved) { Text("Simpan Pengaturan") } } }

@Composable private fun SettingCard(title: String, description: String, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Paper), border = BorderStroke(1.dp, Line)) { Column(Modifier.padding(18.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(description, color = Muted); Spacer(Modifier.height(20.dp)); content() } } }
@Composable private fun ToggleRow(title: String, subtitle: String, value: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium) }; Switch(value, onChange) } }
@Composable private fun PercentField(label: String, value: Double, onChange: (Double) -> Unit) { Spacer(Modifier.height(10.dp)); OutlinedTextField(if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString(), { onChange(it.filter { char -> char.isDigit() || char == '.' }.toDoubleOrNull() ?: 0.0) }, Modifier.fillMaxWidth(), label = { Text(label) }, suffix = { Text("%") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true) }
@Composable private fun SettingChoice(label: String, value: String, options: List<Pair<String, String>>, onChange: (String) -> Unit) { var open by remember { mutableStateOf(false) }; val text = options.firstOrNull { it.first == value }?.second ?: value; Column { Text(label, style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(7.dp)); Box { OutlinedButton({ open = true }, Modifier.fillMaxWidth().height(52.dp)) { Text(text, modifier = Modifier.weight(1f)); Text("⌄") }; DropdownMenu(open, { open = false }) { options.forEach { option -> DropdownMenuItem({ Text(option.second) }, onClick = { onChange(option.first); open = false }) } } } } }
