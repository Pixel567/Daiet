package com.nutrichat.app.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nutrichat.app.network.NutritionalInfo
import com.nutrichat.app.network.OpenFoodFactsService
import com.nutrichat.app.network.NetworkUtils
import com.nutrichat.app.ui.chat.ChatViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    onBack: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    var textInput by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var scannedProduct by remember { mutableStateOf<NutritionalInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf(EditMode.NONE) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val offService = remember { OpenFoodFactsService() }

    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val lastBotInfo = chatState.messages.lastOrNull { !it.isUser && it.nutritionalInfo != null }?.nutritionalInfo
    val isLoading = chatState.isLoading

    var currentInfo by remember { mutableStateOf<NutritionalInfo?>(null) }
    
    LaunchedEffect(scannedProduct, lastBotInfo) {
        if (scannedProduct != null) {
            currentInfo = scannedProduct
            editMode = EditMode.NONE
        } else if (lastBotInfo != null) {
            currentInfo = lastBotInfo
            editMode = EditMode.NONE
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Meal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isScanning) {
                BarcodeScannerView(
                    onBarcodeScanned = { barcode ->
                        if (!NetworkUtils.isInternetAvailable(context)) {
                            errorMessage = "No internet connection."
                            isScanning = false
                            return@BarcodeScannerView
                        }
                        isScanning = false
                        scannedProduct = null
                        scope.launch {
                            offService.getProduct(barcode).fold(
                                onSuccess = { info ->
                                    scannedProduct = info
                                    errorMessage = null
                                },
                                onFailure = {
                                    errorMessage = "Product not found or network error"
                                }
                            )
                        }
                    },
                    onClose = { isScanning = false }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Describe what you ate", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. 2 eggs and a toast") },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        if (textInput.isNotBlank()) {
                                            if (!NetworkUtils.isInternetAvailable(context)) {
                                                errorMessage = "No internet connection."
                                                return@IconButton
                                            }
                                            scannedProduct = null
                                            chatViewModel.sendMessage(textInput)
                                            errorMessage = null
                                        }
                                    },
                                    enabled = !isLoading
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = "Analyze")
                                }
                            }
                        )
                    }
                }

                Text("OR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)

                Button(
                    onClick = { 
                        if (!NetworkUtils.isInternetAvailable(context)) {
                            errorMessage = "No internet connection."
                            return@Button
                        }
                        isScanning = true 
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Barcode")
                }

                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                currentInfo?.let { info ->
                    NutritionalResultView(
                        info = info,
                        editMode = editMode,
                        onEditModeChange = { editMode = it },
                        onAdd = { finalInfo ->
                            chatViewModel.addToMealPlan(finalInfo)
                            onBack()
                        },
                        onRecalculateAmount = { newAmount ->
                            if (!NetworkUtils.isInternetAvailable(context)) {
                                errorMessage = "No internet connection."
                                return@NutritionalResultView
                            }
                            chatViewModel.sendMessage("Recalculate nutrition for: $newAmount of ${info.productName}")
                            editMode = EditMode.NONE
                        },
                        onUpdateLocally = { updated ->
                            currentInfo = updated
                            editMode = EditMode.NONE
                        }
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun BarcodeScannerView(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    val previewView = remember { PreviewView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) onClose()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.height(300.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        ) { view ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }

                val barcodeScanner = BarcodeScanning.getClient()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes: List<Barcode> ->
                                for (barcode in barcodes) {
                                    barcode.rawValue?.let { 
                                        onBarcodeScanned(it)
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
        
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier.size(200.dp).background(Color.Transparent).padding(2.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }
    }
}
