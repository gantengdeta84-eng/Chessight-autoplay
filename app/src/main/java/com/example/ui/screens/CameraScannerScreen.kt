package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.chess.PieceColor
import com.example.data.scanner.ImageUtils
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TournamentEmerald

private const val TAG = "CameraScannerScreen"

@Composable
fun CameraScannerScreen(
    initialSideToMove: PieceColor,
    initialWhiteBottom: Boolean,
    onCaptured: (Bitmap, PieceColor, Boolean) -> Unit,
    onPickFromGallery: (Uri) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickFromGallery(uri)
        }
    }

    var sideToMove by remember { mutableStateOf(initialSideToMove) }
    var whiteAtBottom by remember { mutableStateOf(initialWhiteBottom) }
    var flashEnabled by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ObsidianBackground
    ) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(TournamentEmerald.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = TournamentEmerald,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.camera_permission_title),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.camera_permission_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("grant_camera_permission_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = TournamentEmerald)
                ) {
                    Text(stringResource(R.string.camera_grant_button), fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("camera_fallback_gallery_button")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.camera_use_gallery))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("camera_back_button")
                ) {
                    Text(stringResource(R.string.cd_back))
                }
            }
        } else {
            val previewView = remember { PreviewView(context) }

            DisposableEffect(lifecycleOwner) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val executor = ContextCompat.getMainExecutor(context)

                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val capture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                        imageCapture = capture
                        boundCamera = camera
                    } catch (e: Exception) {
                        Log.e(TAG, "Camera initialization failed", e)
                        cameraError = e.localizedMessage ?: "Kamera tidak tersedia pada perangkat ini."
                    }
                }, executor)

                onDispose {
                    runCatching {
                        cameraProviderFuture.get().unbindAll()
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // Square 8x8 alignment reticle overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    ScannerGridOverlay(modifier = Modifier.fillMaxSize())
                }

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(ObsidianBackground.copy(alpha = 0.65f))
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("camera_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Posisikan Papan di Dalam Kotak",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    IconButton(
                        onClick = {
                            flashEnabled = !flashEnabled
                            boundCamera?.cameraControl?.enableTorch(flashEnabled)
                        },
                        modifier = Modifier.testTag("camera_flash_button")
                    ) {
                        Icon(
                            imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = stringResource(R.string.cd_flash),
                            tint = if (flashEnabled) TournamentEmerald else Color.White
                        )
                    }
                }

                if (cameraError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = cameraError.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Bottom Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(ObsidianBackground.copy(alpha = 0.85f))
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        FilterChip(
                            selected = sideToMove == PieceColor.WHITE,
                            onClick = {
                                sideToMove = if (sideToMove == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
                            },
                            label = {
                                Text(
                                    text = if (sideToMove == PieceColor.WHITE) {
                                        stringResource(R.string.turn_white)
                                    } else {
                                        stringResource(R.string.turn_black)
                                    }
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TournamentEmerald.copy(alpha = 0.22f),
                                selectedLabelColor = TournamentEmerald
                            ),
                            modifier = Modifier.testTag("camera_turn_toggle")
                        )

                        FilterChip(
                            selected = !whiteAtBottom,
                            onClick = { whiteAtBottom = !whiteAtBottom },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = if (whiteAtBottom) {
                                        stringResource(R.string.bottom_white)
                                    } else {
                                        stringResource(R.string.bottom_black)
                                    }
                                )
                            },
                            modifier = Modifier.testTag("camera_perspective_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("camera_gallery_shortcut")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = stringResource(R.string.action_scan_gallery),
                                tint = Color.White
                            )
                        }

                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(3.dp, TournamentEmerald, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(if (isCapturing) Color.Gray else Color.White)
                                .clickable(enabled = !isCapturing && imageCapture != null) {
                                    val capture = imageCapture ?: return@clickable
                                    isCapturing = true
                                    capture.takePicture(
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                val bmp = ImageUtils.imageProxyToBitmap(image)
                                                image.close()
                                                isCapturing = false
                                                if (bmp != null) {
                                                    onCaptured(bmp, sideToMove, whiteAtBottom)
                                                } else {
                                                    cameraError = "Gagal memproses foto kamera."
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                isCapturing = false
                                                cameraError = exception.localizedMessage ?: "Gagal mengambil foto."
                                            }
                                        }
                                    )
                                }
                                .testTag("camera_shutter_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.cd_capture),
                                tint = ObsidianBackground,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.size(52.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cellW = w / 8f
        val cellH = h / 8f

        // Subtle 8x8 alignment lines
        for (i in 1..7) {
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(i * cellW, 0f),
                end = Offset(i * cellW, h),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, i * cellH),
                end = Offset(w, i * cellH),
                strokeWidth = 1f
            )
        }

        // Bold emerald corner brackets
        val cornerLen = w * 0.11f
        val stroke = 6f
        val color = TournamentEmerald

        // Top-Left
        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), stroke, StrokeCap.Round)
        // Top-Right
        drawLine(color, Offset(w, 0f), Offset(w - cornerLen, 0f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w, 0f), Offset(w, cornerLen), stroke, StrokeCap.Round)
        // Bottom-Left
        drawLine(color, Offset(0f, h), Offset(cornerLen, h), stroke, StrokeCap.Round)
        drawLine(color, Offset(0f, h), Offset(0f, h - cornerLen), stroke, StrokeCap.Round)
        // Bottom-Right
        drawLine(color, Offset(w, h), Offset(w - cornerLen, h), stroke, StrokeCap.Round)
        drawLine(color, Offset(w, h), Offset(w, h - cornerLen), stroke, StrokeCap.Round)
    }
}
