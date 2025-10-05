package at.mcbabo.authenticator.ui.screen

import android.Manifest
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import at.mcbabo.authenticator.R
import at.mcbabo.authenticator.internal.QrCodeAnalyzer
import at.mcbabo.authenticator.internal.crypto.QRCodeResult
import at.mcbabo.authenticator.internal.crypto.parseQRCodeType
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onAddAccount: (String) -> Unit,
    onImportAccounts: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    var code by remember { mutableStateOf("") }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hapticFeedback = LocalHapticFeedback.current

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    val cameraPermissionState = rememberPermissionState(
        Manifest.permission.CAMERA
    )

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(vertical = 8.dp)
        ) {
            if (!cameraPermissionState.status.isGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(text = "Need camera permission to scan QR codes")
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp)
                        ) {
                            Text(text = "Grant permission")
                        }
                    }
                }
                return@Box
            }
            AndroidView(
                factory = { context ->
                    val previewView = PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }

                    val preview = Preview.Builder()
                        .setPreviewStabilizationEnabled(true)
                        .build()

                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build()

                    preview.surfaceProvider = previewView.surfaceProvider

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(context),
                        QrCodeAnalyzer(
                            onQrCodeScanned = { qrCode ->
                                if (code != qrCode) {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val parsed = parseQRCodeType(qrCode)

                                    when (parsed) {
                                        QRCodeResult.ADD_ACCOUNT -> {
                                            onAddAccount(qrCode)
                                        }

                                        QRCodeResult.IMPORT_ACCOUNTS -> {
                                            onImportAccounts(qrCode)
                                        }

                                        QRCodeResult.INVALID -> Log.w("QrScanner", "Scanned invalid QR code: $qrCode")
                                    }
                                }
                                code = qrCode
                            },
                            onError = { exception ->
                                Log.e("QrScanner", "Analysis error", exception)
                            }
                        )
                    )

                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        // Unbind all use cases before rebinding
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("QrScanner", "Camera binding failed", e)
                    }

                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
            )

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                IconButton(
                    onClick = { onNavigateBack() },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }

            QRCodeCutout()
        }
    }
}

@Composable
fun BoxScope.QRCodeCutout() {
    Box(
        modifier = Modifier
            .align(Alignment.Center)
            .size(256.dp)
            .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
            .padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(32.dp)
                .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(32.dp)
                .border(4.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
        )
    }
}
