package id.kopikontrol.app.ui

import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import id.kopikontrol.app.data.Account
import id.kopikontrol.app.data.IngredientSummary
import id.kopikontrol.app.data.OnboardingDraft
import id.kopikontrol.app.data.RecipeSummary
import id.kopikontrol.app.data.WorkspaceData
import id.kopikontrol.app.data.subrecipeCount
import id.kopikontrol.app.ui.theme.Caramel
import id.kopikontrol.app.ui.theme.Coffee
import id.kopikontrol.app.ui.theme.Cream
import id.kopikontrol.app.ui.theme.Forest
import id.kopikontrol.app.ui.theme.Line
import id.kopikontrol.app.ui.theme.Muted
import id.kopikontrol.app.ui.theme.Paper
import id.kopikontrol.app.ui.theme.Success
import java.text.NumberFormat
import java.util.Locale

private enum class Destination(val label: String, val icon: ImageVector) {
    Dashboard("Ringkasan", Icons.Outlined.Home),
    Pos("Transaksi", Icons.Outlined.AddShoppingCart),
    Ingredients("Bahan", Icons.Outlined.Inventory2),
    Subrecipes("Subresep", Icons.Outlined.MenuBook),
    Recipes("Resep Menu", Icons.Outlined.RestaurantMenu),
    Simulation("Simulasi", Icons.Outlined.Calculate),
    Settings("Pengaturan", Icons.Outlined.Settings),
    Profile("Profil", Icons.Outlined.Person),
}

@Composable
fun KopiKontrolApp(
    oauthCallback: Uri?,
    consumeOauthCallback: () -> Unit,
    openGoogleLogin: () -> Unit,
    viewModel: AppViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(oauthCallback) {
        oauthCallback?.let {
            viewModel.handleOauth(it)
            consumeOauthCallback()
        }
    }
    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        when (val current = state) {
            AppState.Loading -> LoadingScreen()
            is AppState.SignedOut -> AuthScreen(current.error, viewModel::clearError, viewModel::login, viewModel::signup, openGoogleLogin)
            is AppState.Ready -> when {
                current.workspace == null && current.loadingData -> LoadingScreen()
                current.workspace == null -> WorkspaceLoadErrorScreen(current.error, viewModel::reloadWorkspace, viewModel::logout)
                current.workspace.profile == null -> OnboardingScreen(current, viewModel::finishOnboarding, viewModel::logout)
                else -> NativeWorkspace(current.account, current.workspace, current.error, viewModel::clearError, viewModel::reloadWorkspace, viewModel::logout)
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        BrandMark(68)
        Spacer(Modifier.height(18.dp))
        CircularProgressIndicator(color = Coffee, strokeWidth = 3.dp)
        Spacer(Modifier.height(14.dp))
        Text("Menyiapkan ruang kerjamu…", color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WorkspaceLoadErrorScreen(message: String, retry: () -> Unit, logout: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(64)
        Spacer(Modifier.height(20.dp))
        Text("Ruang kerja belum dapat dimuat", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(message.ifBlank { "Periksa koneksi internet lalu coba kembali." }, color = Muted)
        Spacer(Modifier.height(20.dp))
        Button(onClick = retry) { Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Coba lagi") }
        TextButton(onClick = logout) { Text("Keluar dari akun") }
    }
}

@Composable
private fun BrandMark(size: Int = 44) {
    Box(
        Modifier.size(size.dp).background(Paper, RoundedCornerShape((size / 4).dp)).border(1.dp, Line, RoundedCornerShape((size / 4).dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Coffee, contentDescription = null, tint = Coffee, modifier = Modifier.size((size * .56f).dp)) }
}

@Composable
private fun AuthScreen(
    serverError: String,
    clearError: () -> Unit,
    login: (String, String) -> Unit,
    signup: (String, String, String) -> Unit,
    googleLogin: () -> Unit,
) {
    var signupMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var accepted by remember { mutableStateOf(false) }
    val localError = when {
        signupMode && name.trim().length < 2 -> "Nama minimal 2 karakter."
        whatsapp.filter(Char::isDigit).length !in 10..15 -> "Masukkan nomor WhatsApp yang valid."
        password.length < 8 -> "Password minimal 8 karakter."
        signupMode && (!password.any(Char::isLetter) || !password.any(Char::isDigit)) -> "Gunakan kombinasi huruf dan angka."
        signupMode && !accepted -> "Persetujuan diperlukan untuk mendaftar."
        else -> ""
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 700.dp
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.weight(if (tablet) .58f else 1f).fillMaxHeight()
                    .then(if (!tablet || signupMode) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = if (tablet) 64.dp else 22.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                    BrandMark(46)
                    Column { Text("KopiKontrol", style = MaterialTheme.typography.titleLarge); Text("STARTER", style = MaterialTheme.typography.labelMedium, color = Caramel) }
                }
                Spacer(Modifier.height(32.dp))
                Eyebrow("RUANG KERJA KEDAIMU")
                Text(if (signupMode) "Mulai hitung dengan lebih pasti." else "Selamat datang kembali.", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text(if (signupMode) "Buat akun agar data kedaimu tersimpan aman dan terpisah." else "Masuk untuk melanjutkan pengelolaan HPP, margin, dan transaksi kedaimu.", color = Muted)
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth().background(Color(0xFFF4EEE8), RoundedCornerShape(10.dp)).padding(4.dp)) {
                    AuthTab("Masuk", !signupMode, Modifier.weight(1f)) { signupMode = false; clearError() }
                    AuthTab("Daftar", signupMode, Modifier.weight(1f)) { signupMode = true; clearError() }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = googleLogin, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text(if (signupMode) "G   Daftar dengan Google" else "G   Masuk dengan Google") }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Divider(Modifier.weight(1f)); Text("  atau gunakan WhatsApp  ", color = Muted, style = MaterialTheme.typography.bodyMedium); Divider(Modifier.weight(1f)) }
                if (signupMode) {
                    Spacer(Modifier.height(12.dp)); LabeledField("Nama lengkap *", name, { name = it }, "Nama pemilik kedai")
                }
                Spacer(Modifier.height(12.dp))
                LabeledField("Nomor WhatsApp *", whatsapp, { whatsapp = it.filter(Char::isDigit).take(15) }, "81234567890", KeyboardType.Phone, prefix = "+62 ")
                Spacer(Modifier.height(12.dp))
                Text("Password *", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(7.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Minimal 8 karakter") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, null) } },
                )
                if (signupMode) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(accepted, { accepted = it }); Text("Saya menyetujui syarat dan ketentuan KopiKontrol.", style = MaterialTheme.typography.bodyMedium) }
                val error = serverError.ifBlank { localError.takeIf { whatsapp.isNotBlank() || password.isNotBlank() }.orEmpty() }
                if (error.isNotBlank()) { Spacer(Modifier.height(10.dp)); MessageCard(error) }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { if (signupMode) signup(name, whatsapp, password) else login(whatsapp, password) },
                    enabled = localError.isBlank(), modifier = Modifier.fillMaxWidth().height(50.dp),
                ) { Text(if (signupMode) "Buat akun" else "Masuk ke KopiKontrol"); Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.ArrowForward, null) }
            }
            if (tablet) Box(Modifier.weight(.42f).fillMaxHeight().background(Forest), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.Coffee, null, tint = Color.White, modifier = Modifier.size(94.dp)); Spacer(Modifier.height(20.dp)); Text("Hitung lebih pasti.\nKelola lebih rapi.", color = Color.White, style = MaterialTheme.typography.headlineMedium) }
            }
        }
    }
}

@Composable private fun AuthTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier.clickable(onClick = onClick), color = if (selected) Paper else Color.Transparent, shape = RoundedCornerShape(8.dp), shadowElevation = if (selected) 2.dp else 0.dp) {
        Box(Modifier.fillMaxWidth().padding(11.dp), contentAlignment = Alignment.Center) { Text(label, fontWeight = FontWeight.Bold, color = if (selected) Coffee else Muted) }
    }
}

@Composable
private fun LabeledField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, prefix: String? = null, suffix: String? = null) {
    Column { Text(label, style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(7.dp)); OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(placeholder) }, keyboardOptions = KeyboardOptions(keyboardType = keyboardType), prefix = prefix?.let { value -> { Text(value) } }, suffix = suffix?.let { value -> { Text(value) } }) }
}

@Composable
private fun OnboardingScreen(state: AppState.Ready, finish: (OnboardingDraft) -> Unit, logout: () -> Unit) {
    var storeName by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("Kedai kopi kecil") }
    var targetMargin by remember { mutableStateOf("60") }
    var sample by remember { mutableStateOf(true) }
    val valid = storeName.trim().length >= 2 && province.isNotBlank() && city.isNotBlank() && (targetMargin.toIntOrNull() ?: 0) in 1..100
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { BrandMark(); Spacer(Modifier.width(10.dp)); Text("KopiKontrol", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.weight(1f)); TextButton(onClick = logout) { Text("Keluar") } }
        Spacer(Modifier.height(32.dp)); Eyebrow("ONBOARDING NATIVE"); Text("Siapkan ruang kerja kedaimu.", style = MaterialTheme.typography.headlineMedium); Text("Data ini tersimpan pada akun yang sama dengan versi web.", color = Muted)
        Spacer(Modifier.height(24.dp)); Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Paper), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledField("Nama kedai *", storeName, { storeName = it }, "Contoh: Kopi Kontrol")
                LabeledField("Provinsi *", province, { province = it }, "Contoh: Jawa Tengah")
                LabeledField("Kota / kabupaten *", city, { city = it }, "Contoh: Kota Semarang")
                ChoiceField("Tipe usaha *", businessType, listOf("Kedai kopi kecil", "Booth / take away", "Home brewer", "Roastery")) { businessType = it }
                LabeledField("Target margin *", targetMargin, { targetMargin = it.filter(Char::isDigit).take(3) }, "60", KeyboardType.Number, suffix = "%")
                Text("Data awal", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceChip("Data contoh", sample) { sample = true }; ChoiceChip("Data kosong", !sample) { sample = false }
                }
                if (state.error.isNotBlank()) MessageCard(state.error)
            }
        }
        Spacer(Modifier.height(18.dp)); Button(onClick = { finish(OnboardingDraft(storeName, province, city, businessType, targetMargin.toIntOrNull() ?: 60, sample)) }, enabled = valid && !state.loadingData, modifier = Modifier.fillMaxWidth().height(50.dp)) { if (state.loadingData) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Buat ruang kerja") }
    }
}

@Composable
private fun ChoiceField(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column { Text(label, style = MaterialTheme.typography.labelLarge); Spacer(Modifier.height(7.dp)); Box { OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(value, modifier = Modifier.weight(1f)); Text("⌄") }; DropdownMenu(open, { open = false }, Modifier.fillMaxWidth(.8f)) { options.forEach { option -> DropdownMenuItem({ Text(option) }, onClick = { onChange(option); open = false }) } } } }
}

@Composable private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilledTonalButton(onClick, colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (selected) Coffee else Color(0xFFF4EEE8), contentColor = if (selected) Color.White else Coffee)) { Text(label) }
}

@Composable
private fun NativeWorkspace(account: Account, workspace: WorkspaceData, error: String, clearError: () -> Unit, reload: () -> Unit, logout: () -> Unit) {
    var destination by remember { mutableStateOf(Destination.Dashboard) }
    var confirmLogout by remember { mutableStateOf(false) }
    var sidebarOpen by remember { mutableStateOf(true) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 700.dp
        if (tablet) Row(Modifier.fillMaxSize()) {
            if (sidebarOpen) Sidebar(destination, { destination = it }, account, { confirmLogout = true })
            WorkspaceContent(Modifier.weight(1f), destination, workspace, account, error, clearError, reload, onToggleSidebar = { sidebarOpen = !sidebarOpen }, openProfile = { destination = Destination.Profile })
        } else Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Forest) {
                    listOf(Destination.Dashboard, Destination.Pos, Destination.Ingredients, Destination.Recipes, Destination.Settings).forEach { item ->
                        NavigationBarItem(selected = destination == item, onClick = { destination = item }, icon = { Icon(item.icon, null) }, label = { Text(item.label, maxLines = 1) }, colors = androidx.compose.material3.NavigationBarItemDefaults.colors(selectedIconColor = Color.White, selectedTextColor = Color.White, unselectedIconColor = Color(0xFFD6E0DE), unselectedTextColor = Color(0xFFD6E0DE), indicatorColor = Coffee))
                    }
                }
            }
        ) { padding -> WorkspaceContent(Modifier.padding(padding), destination, workspace, account, error, clearError, reload, onToggleSidebar = null, openProfile = { destination = Destination.Profile }) }
    }
    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false }, title = { Text("Keluar dari akun?") }, text = { Text("Kamu perlu masuk kembali untuk mengakses ruang kerja kedai.") }, confirmButton = { Button(onClick = logout) { Text("Keluar") } }, dismissButton = { OutlinedButton(onClick = { confirmLogout = false }) { Text("Batal") } })
}

@Composable
private fun Sidebar(selected: Destination, onSelect: (Destination) -> Unit, account: Account, logout: () -> Unit) {
    Column(Modifier.width(232.dp).fillMaxHeight().background(Forest).padding(15.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { BrandMark(42); Spacer(Modifier.width(10.dp)); Column { Text("KopiKontrol", color = Color.White, style = MaterialTheme.typography.titleMedium); Text("STARTER · NATIVE", color = Color(0xFFE6A763), style = MaterialTheme.typography.labelMedium) } }
        Spacer(Modifier.height(28.dp))
        Destination.entries.forEach { item ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(item) }.background(if (selected == item) Coffee else Color.Transparent, RoundedCornerShape(10.dp)).padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(item.icon, null, tint = Color.White, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(12.dp)); Text(item.label, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = if (selected == item) FontWeight.Bold else FontWeight.Normal)
            }
            Spacer(Modifier.height(3.dp))
        }
        Spacer(Modifier.weight(1f)); Divider(color = Color.White.copy(alpha = .14f)); Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(34.dp).background(Coffee, CircleShape), contentAlignment = Alignment.Center) { Text(account.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(9.dp)); Text(account.name, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)); IconButton(onClick = logout) { Icon(Icons.Outlined.Logout, "Keluar", tint = Color.White) } }
    }
}

@Composable
private fun WorkspaceContent(modifier: Modifier, destination: Destination, workspace: WorkspaceData, account: Account, error: String, clearError: () -> Unit, reload: () -> Unit, onToggleSidebar: (() -> Unit)?, openProfile: () -> Unit) {
    Column(modifier.fillMaxSize().background(Cream)) {
        Row(Modifier.fillMaxWidth().background(Paper).padding(horizontal = 22.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onToggleSidebar != null) { IconButton(onClick = onToggleSidebar) { Icon(Icons.Outlined.Menu, "Buka atau tutup sidebar") }; Spacer(Modifier.width(8.dp)) }
            Column { Eyebrow("KOPI ADMIN"); Text(destination.label, style = MaterialTheme.typography.headlineMedium) }
            Spacer(Modifier.weight(1f)); IconButton(onClick = reload) { Icon(Icons.Outlined.Refresh, "Muat ulang") }; Box(Modifier.size(36.dp).background(Coffee, CircleShape).clickable(onClick = openProfile), contentAlignment = Alignment.Center) { Text(account.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold) }
        }
        Divider(color = Line)
        if (error.isNotBlank()) Row(Modifier.fillMaxWidth().background(Color(0xFFF8E7E1)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(error, color = Coffee, modifier = Modifier.weight(1f)); TextButton(onClick = clearError) { Text("Tutup") } }
        when (destination) {
            Destination.Dashboard -> DashboardScreen(workspace)
            Destination.Ingredients -> IngredientScreen(workspace.ingredients)
            Destination.Recipes -> RecipeScreen(workspace.recipes)
            Destination.Profile -> ProfileScreen(workspace, account)
            Destination.Pos -> PosScreen(workspace)
            Destination.Subrecipes -> MigrationScreen("Subresep", "Fondasi data siap", "Daftar dan editor subresep native akan menggunakan data akun yang sama.", Icons.Outlined.MenuBook)
            Destination.Simulation -> MigrationScreen("Simulasi Harga", "Fondasi perhitungan siap", "UI simulasi harga native akan mengikuti aturan HPP dan margin KopiKontrol.", Icons.Outlined.Calculate)
            Destination.Settings -> SettingsScreen(workspace.profile)
        }
    }
}

@Composable
private fun DashboardScreen(workspace: WorkspaceData) {
    val averageMargin = workspace.recipes.map { it.margin }.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Ringkasan kedai", style = MaterialTheme.typography.headlineMedium); Text("Data langsung dari ruang kerja KopiKontrol.", color = Muted) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Bahan", workspace.ingredients.size.toString(), Modifier.weight(1f)); MetricCard("Subresep", workspace.subrecipeCount.toString(), Modifier.weight(1f)); MetricCard("Menu", workspace.recipes.size.toString(), Modifier.weight(1f)) } }
        item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Forest)) { Column(Modifier.padding(20.dp)) { Text("RATA-RATA MARGIN", color = Color(0xFFC9D6D3), style = MaterialTheme.typography.labelMedium); Text(String.format(Locale("id", "ID"), "%.1f%%", averageMargin), color = Color.White, style = MaterialTheme.typography.headlineLarge); Text("Target ${workspace.profile?.targetMargin ?: 60}%", color = Color(0xFFE6A763)) } } }
        item { SectionTitle("Menu terbaru") }
        items(workspace.recipes.take(5)) { recipe -> RecipeRow(recipe) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable private fun MetricCard(label: String, value: String, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = Paper), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Column(Modifier.padding(15.dp)) { Text(label.uppercase(), color = Muted, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(7.dp)); Text(value, style = MaterialTheme.typography.headlineMedium) } } }

@Composable
private fun IngredientScreen(ingredients: List<IngredientSummary>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("Daftar Bahan", style = MaterialTheme.typography.headlineMedium); Text("${ingredients.size} bahan tersinkron dari akunmu.", color = Muted); Spacer(Modifier.height(6.dp)) }
        items(ingredients) { ingredient -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Paper), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(Color(0xFFF4EEE8), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Inventory2, null, tint = Caramel) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(ingredient.name, fontWeight = FontWeight.Bold); Text("${ingredient.category} · ${ingredient.stock}", color = Muted, style = MaterialTheme.typography.bodyMedium) }; Text("${rupiah(ingredient.price)}/${ingredient.unit}", fontWeight = FontWeight.Bold) } } }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun RecipeScreen(recipes: List<RecipeSummary>) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        item { Text("Resep Menu", style = MaterialTheme.typography.headlineMedium); Text("${recipes.size} menu tersinkron dari akunmu.", color = Muted); Spacer(Modifier.height(6.dp)) }
        items(recipes) { RecipeRow(it) }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable private fun RecipeRow(recipe: RecipeSummary) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Paper), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Color(0xFFF4EEE8), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Coffee, null, tint = Coffee) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(recipe.name, fontWeight = FontWeight.Bold); Text("${recipe.category} · HPP ${rupiah(recipe.hpp)}", color = Muted, style = MaterialTheme.typography.bodyMedium) }; Column(horizontalAlignment = Alignment.End) { Text(rupiah(recipe.price), fontWeight = FontWeight.Bold); Text(String.format(Locale("id", "ID"), "%.1f%%", recipe.margin), color = if (recipe.margin >= 50) Success else Caramel, style = MaterialTheme.typography.bodyMedium) } } } }

@Composable
private fun ProfileScreen(workspace: WorkspaceData, account: Account) {
    val profile = workspace.profile
    val subscription = workspace.subscription
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 700.dp
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(if (compact) 14.dp else 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Column { Text("Profil Kedai", style = MaterialTheme.typography.headlineMedium); Text("Identitas usaha, pemilik akun, dan status ruang kerja.", color = Muted) } }
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Forest), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(if (compact) 18.dp else 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(if (compact) 58.dp else 72.dp).background(Coffee, CircleShape).border(2.dp, Color.White.copy(alpha = .3f), CircleShape), contentAlignment = Alignment.Center) { Text(profile?.name.orEmpty().take(1).ifBlank { "K" }.uppercase(), color = Color.White, style = MaterialTheme.typography.headlineMedium) }
                        Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(profile?.name.orEmpty().ifBlank { "Nama kedai belum diisi" }, color = Color.White, style = MaterialTheme.typography.headlineMedium, maxLines = 2); Text(account.name.ifBlank { "Pemilik kedai" }, color = Color(0xFFD6E0DE)); Spacer(Modifier.height(9.dp)); Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(18.dp)) { Text("${subscription?.plan ?: "Starter"} · ${subscription?.status?.replaceFirstChar { it.uppercase() } ?: "Aktif"}", Modifier.padding(horizontal = 11.dp, vertical = 6.dp), color = Color(0xFFE6A763), style = MaterialTheme.typography.labelLarge) } }
                        if (!compact) Box(Modifier.background(Color.White.copy(alpha = .1f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.End) { Text("RUANG KERJA", color = Color(0xFFC9D6D3), style = MaterialTheme.typography.labelMedium); Text("Tersinkron", color = Color.White, fontWeight = FontWeight.Bold) } }
                    }
                }
            }
            item {
                if (compact) Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileSection("Informasi Kedai", Icons.Outlined.Storefront, Modifier.fillMaxWidth()) { StoreProfileDetails(profile) }
                    ProfileSection("Pemilik Akun", Icons.Outlined.Person, Modifier.fillMaxWidth()) { OwnerProfileDetails(account) }
                } else Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileSection("Informasi Kedai", Icons.Outlined.Storefront, Modifier.weight(1f)) { StoreProfileDetails(profile) }
                    ProfileSection("Pemilik Akun", Icons.Outlined.Person, Modifier.weight(1f)) { OwnerProfileDetails(account) }
                }
            }
            item {
                if (compact) Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileSection("Ringkasan Usaha", Icons.Outlined.Calculate, Modifier.fillMaxWidth()) { BusinessProfileDetails(workspace) }
                    ProfileSection("Langganan", Icons.Outlined.WorkspacePremium, Modifier.fillMaxWidth()) { SubscriptionProfileDetails(workspace) }
                } else Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ProfileSection("Ringkasan Usaha", Icons.Outlined.Calculate, Modifier.weight(1f)) { BusinessProfileDetails(workspace) }
                    ProfileSection("Langganan", Icons.Outlined.WorkspacePremium, Modifier.weight(1f)) { SubscriptionProfileDetails(workspace) }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable private fun ProfileSection(title: String, icon: ImageVector, modifier: Modifier, content: @Composable () -> Unit) { Card(modifier, colors = CardDefaults.cardColors(containerColor = Paper), border = androidx.compose.foundation.BorderStroke(1.dp, Line), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).background(Color(0xFFF4EEE8), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Coffee, modifier = Modifier.size(20.dp)) }; Spacer(Modifier.width(10.dp)); Text(title, style = MaterialTheme.typography.titleLarge) }; Divider(color = Line); content() } } }
@Composable private fun StoreProfileDetails(profile: id.kopikontrol.app.data.StoreProfile?) { ProfileDetail(Icons.Outlined.Storefront, "Nama kedai", profile?.name.orEmpty()); ProfileDetail(Icons.Outlined.Coffee, "Tipe usaha", profile?.businessType.orEmpty()); ProfileDetail(Icons.Outlined.LocationOn, "Lokasi", listOfNotNull(profile?.city, profile?.province).filter { it.isNotBlank() }.joinToString(", ")) }
@Composable private fun OwnerProfileDetails(account: Account) { ProfileDetail(Icons.Outlined.Person, "Nama pemilik", account.name); ProfileDetail(Icons.Outlined.Email, "Email", account.email); ProfileDetail(Icons.Outlined.Phone, "WhatsApp", account.whatsapp) }
@Composable private fun BusinessProfileDetails(workspace: WorkspaceData) { ProfileDetail(Icons.Outlined.Calculate, "Target margin", "${workspace.profile?.targetMargin ?: 60}%"); ProfileDetail(Icons.Outlined.RestaurantMenu, "Produk menu", "${workspace.recipes.size} produk"); ProfileDetail(Icons.Outlined.Inventory2, "Bahan tersimpan", "${workspace.ingredients.size} bahan") }
@Composable private fun SubscriptionProfileDetails(workspace: WorkspaceData) { val subscription = workspace.subscription; ProfileDetail(Icons.Outlined.WorkspacePremium, "Paket", subscription?.plan ?: "Starter"); ProfileDetail(Icons.Outlined.ReceiptLong, "Status", subscription?.status?.replaceFirstChar { it.uppercase() } ?: "Aktif"); ProfileDetail(Icons.Outlined.Refresh, "Masa aktif", when { subscription?.isLifetime == true -> "Selamanya"; subscription?.daysLeft != null -> "${subscription.daysLeft} hari tersisa"; else -> "-" }) }
@Composable private fun ProfileDetail(icon: ImageVector, label: String, value: String) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Caramel, modifier = Modifier.size(19.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Muted); Text(value.ifBlank { "-" }, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis) } } }

@Composable private fun MigrationScreen(title: String, badge: String, message: String, icon: ImageVector) { Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Box(Modifier.size(72.dp).background(Color(0xFFF4EEE8), RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Coffee, modifier = Modifier.size(34.dp)) }; Spacer(Modifier.height(18.dp)); Text(title, style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.height(8.dp)); Surface(color = Color(0xFFE3ECE8), shape = RoundedCornerShape(20.dp)) { Text(badge, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Success, style = MaterialTheme.typography.labelLarge) }; Spacer(Modifier.height(12.dp)); Text(message, color = Muted, modifier = Modifier.fillMaxWidth(.75f), style = MaterialTheme.typography.bodyLarge) } }

@Composable private fun Eyebrow(text: String) { Text(text, color = Caramel, style = MaterialTheme.typography.labelMedium) }
@Composable private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge) }
@Composable private fun MessageCard(message: String) { Surface(color = Color(0xFFF8E7E1), shape = RoundedCornerShape(9.dp)) { Text(message, Modifier.fillMaxWidth().padding(12.dp), color = Coffee, style = MaterialTheme.typography.bodyMedium) } }
private fun rupiah(value: Double): String = "Rp ${NumberFormat.getNumberInstance(Locale("id", "ID")).format(value)}"
