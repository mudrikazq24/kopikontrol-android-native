package id.kopikontrol.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import id.kopikontrol.app.ui.theme.Caramel
import id.kopikontrol.app.ui.theme.Muted
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun BarcodeCamera(onDetected: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    if (granted) CameraPreview(onDetected)
    else Box(Modifier.fillMaxWidth().height(220.dp).background(Color(0xFF1B1B1B), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.QrCodeScanner, null, tint = Color.White)
            Text("Izin kamera diperlukan untuk scan barcode.", color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Izinkan Kamera") }
        }
    }
}

@Composable
@OptIn(ExperimentalGetImage::class)
private fun CameraPreview(onDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val locked = remember { AtomicBoolean(false) }
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_EAN_8, Barcode.FORMAT_EAN_13, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_39, Barcode.FORMAT_CODE_93, Barcode.FORMAT_CODE_128, Barcode.FORMAT_ITF,
            Barcode.FORMAT_QR_CODE,
        ).build())
    }
    DisposableEffect(Unit) { onDispose { scanner.close(); executor.shutdown() } }
    Box(Modifier.fillMaxWidth().height(240.dp).background(Color.Black, RoundedCornerShape(16.dp))) {
        AndroidView(
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
                        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                        analysis.setAnalyzer(executor) { proxy ->
                            val image = proxy.image
                            if (image == null || locked.get()) { proxy.close(); return@setAnalyzer }
                            scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                                .addOnSuccessListener { barcodes ->
                                    val value = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
                                    if (value != null && locked.compareAndSet(false, true)) {
                                        onDetected(value)
                                        Handler(Looper.getMainLooper()).postDelayed({ locked.set(false) }, 1_200)
                                    }
                                }
                                .addOnCompleteListener { proxy.close() }
                        }
                        runCatching {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        }
                    }, ContextCompat.getMainExecutor(viewContext))
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.align(Alignment.Center).fillMaxWidth(.78f).height(92.dp).border(2.dp, Caramel, RoundedCornerShape(12.dp)))
        Text("Posisikan barcode di dalam area", color = Color.White, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = .55f), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 7.dp))
    }
}
