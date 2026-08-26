package id.kopikontrol.app.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

data class PairedPrinter(val name: String, val address: String)

class BluetoothPrinterManager(private val context: Context) {
    private val adapter: BluetoothAdapter? get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    fun requiresPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<PairedPrinter> {
        if (requiresPermission()) return emptyList()
        return adapter?.bondedDevices.orEmpty().map { PairedPrinter(it.name ?: "Printer Bluetooth", it.address) }.sortedBy { it.name }
    }

    @SuppressLint("MissingPermission")
    suspend fun print(address: String, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(!requiresPermission()) { "Izinkan akses Perangkat di sekitar, lalu coba cetak kembali." }
            require(address.isNotBlank()) { "Pilih printer Bluetooth terlebih dahulu." }
            val device = adapter?.getRemoteDevice(address) ?: error("Bluetooth tidak tersedia.")
            val socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            adapter?.cancelDiscovery()
            socket.use {
                it.connect()
                it.outputStream.use { output ->
                    output.write(byteArrayOf(0x1B, 0x40))
                    output.write(text.toByteArray(Charsets.US_ASCII))
                    output.write(byteArrayOf(0x0A, 0x0A, 0x0A))
                    output.flush()
                }
            }
        }
    }
}
