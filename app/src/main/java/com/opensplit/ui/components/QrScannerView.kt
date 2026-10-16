package com.opensplit.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.opensplit.ui.theme.OpenSplitIcons
import java.util.concurrent.Executors

/**
 * Inline QR scanner: a CameraX preview with ML Kit barcode analysis, living inside whatever
 * sheet hosts it (no full-screen scanner activity). Asks for the camera permission itself and
 * reports each decoded QR value once via [onScanned].
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QrScannerView(onScanned: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var denied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> granted = ok; denied = !ok }
    val latestOnScanned by rememberUpdatedState(onScanned)

    if (!granted) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = modifier) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                SymbolIcon(OpenSplitIcons.CameraAlt, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (denied) "Camera permission was denied. Allow it to scan a friend's code, or paste their link below."
                    else "Allow the camera to scan a friend's invite code right here.",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(shapes = ButtonDefaults.shapes(), onClick = { permission.launch(Manifest.permission.CAMERA) }) { Text(if (denied) "Try again" else "Allow camera") }
            }
        }
        return
    }

    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()) }
    var lastValue by remember { mutableStateOf<String?>(null) }
    // Google Pay-style controls: flashlight toggle and "scan from a photo".
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(torchOn, camera) { camera?.cameraControl?.enableTorch(torchOn) }
    val snackbar = LocalSnackbarController.current
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching { InputImage.fromFilePath(context, uri) }
            .onFailure { snackbar.showMessage("Couldn't open that image") }
            .onSuccess { image ->
                scanner.process(image)
                    .addOnSuccessListener { codes ->
                        val value = codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                        if (value != null) latestOnScanned(value) else snackbar.showMessage("No QR code found in that photo")
                    }
                    .addOnFailureListener { snackbar.showMessage("Couldn't read that photo") }
            }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            scanner.close(); executor.shutdown()
        }
    }
    Box(modifier = modifier.clip(RoundedCornerShape(24.dp))) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also { it.surfaceProvider = surfaceProvider }
                        val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                        analysis.setAnalyzer(executor) { proxy ->
                            val media = proxy.image
                            if (media == null) { proxy.close(); return@setAnalyzer }
                            scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                                .addOnSuccessListener { codes ->
                                    val value = codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                                    if (value != null && value != lastValue) { lastValue = value; latestOnScanned(value) }
                                }
                                .addOnCompleteListener { proxy.close() }
                        }
                        runCatching {
                            provider.unbindAll()
                            camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        // Viewfinder frame so the user knows where to aim.
        Box(
            modifier = Modifier.align(Alignment.Center).size(200.dp)
                .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
        )
        androidx.compose.foundation.layout.Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp)
        ) {
            val hasFlash = camera?.cameraInfo?.hasFlashUnit() == true
            androidx.compose.material3.FilledTonalIconButton(
                shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
                enabled = hasFlash,
                onClick = { torchOn = !torchOn }
            ) {
                SymbolIcon(if (torchOn) OpenSplitIcons.FlashOff else OpenSplitIcons.FlashOn, contentDescription = if (torchOn) "Turn flashlight off" else "Turn flashlight on")
            }
            androidx.compose.material3.FilledTonalIconButton(
                shapes = androidx.compose.material3.IconButtonDefaults.shapes(),
                onClick = { galleryPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            ) { SymbolIcon(OpenSplitIcons.PhotoLibrary, contentDescription = "Scan a QR code from a photo") }
        }
    }
}
