package com.example.pharma.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pharma.data.gemini.ChatMessage
import com.example.pharma.data.gemini.GeminiClient
import com.example.pharma.data.gemini.GeminiTaskType
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    sender = "assistant",
                    text = "Welcome to the Pharmaceutical Scale-Up Technical Assistant. I can help evaluate equipment invariants, verify Froude number vs Tip Speed trade-offs, troubleshoot fluidization bed dynamics, or analyze equipment photos and technical batch records."
                )
            )
        )
    }

    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var selectedTaskType by remember { mutableStateOf(GeminiTaskType.GENERAL_CHAT) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ttsPlayingId by remember { mutableStateOf<String?>(null) }

    // Fallback Android TTS in case gemini-3.8-flash-tts is offline or processing
    var androidTts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }
        androidTts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    // Photo picker for image understanding
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    selectedBitmap = bmp
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Scale-Up Specialist",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Powered by Gemini Multi-Turn & Vision",
                            fontSize = 10.sp,
                            color = PharmaBlueLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PharmaBluePrimary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Model Selector Bar
            Surface(
                color = Slate100,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Model:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)

                    FilterChip(
                        selected = selectedTaskType == GeminiTaskType.GENERAL_CHAT,
                        onClick = { selectedTaskType = GeminiTaskType.GENERAL_CHAT },
                        label = { Text("gemini-3.5-flash", fontSize = 10.sp) },
                        modifier = Modifier.testTag("chip_model_flash")
                    )
                    FilterChip(
                        selected = selectedTaskType == GeminiTaskType.COMPLEX_REASON,
                        onClick = { selectedTaskType = GeminiTaskType.COMPLEX_REASON },
                        label = { Text("gemini-3.1-pro", fontSize = 10.sp) },
                        modifier = Modifier.testTag("chip_model_pro")
                    )
                    FilterChip(
                        selected = selectedTaskType == GeminiTaskType.FAST_QUERY,
                        onClick = { selectedTaskType = GeminiTaskType.FAST_QUERY },
                        label = { Text("flash-lite", fontSize = 10.sp) },
                        modifier = Modifier.testTag("chip_model_lite")
                    )
                }
            }

            // Chat Message Thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.sender == "user"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            color = if (isUser) PharmaBluePrimary else MaterialTheme.colorScheme.surface,
                            contentColor = if (isUser) Color.White else Slate900,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = if (isUser) 0.dp else 1.dp,
                            modifier = Modifier.widthIn(max = 320.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (msg.imageBitmap != null) {
                                    Image(
                                        bitmap = msg.imageBitmap.asImageBitmap(),
                                        contentDescription = "Uploaded Photo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .padding(bottom = 6.dp)
                                    )
                                }

                                Text(
                                    text = msg.text,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )

                                if (!isUser) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = {
                                                ttsPlayingId = msg.id
                                                coroutineScope.launch {
                                                    // Request TTS using gemini-3.8-flash-tts
                                                    val audioBytes = GeminiClient.textToSpeech(msg.text.take(300))
                                                    if (audioBytes != null) {
                                                        try {
                                                            val tempFile = File.createTempFile("gemini_tts", ".mp3", context.cacheDir)
                                                            FileOutputStream(tempFile).use { it.write(audioBytes) }
                                                            val mp = MediaPlayer().apply {
                                                                setAudioAttributes(
                                                                    AudioAttributes.Builder()
                                                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                                                        .build()
                                                                )
                                                                setDataSource(tempFile.absolutePath)
                                                                prepare()
                                                                start()
                                                                setOnCompletionListener {
                                                                    ttsPlayingId = null
                                                                    it.release()
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            androidTts?.speak(msg.text, TextToSpeech.QUEUE_FLUSH, null, null)
                                                            ttsPlayingId = null
                                                        }
                                                    } else {
                                                        // Fallback to Android TTS
                                                        androidTts?.speak(msg.text, TextToSpeech.QUEUE_FLUSH, null, null)
                                                        ttsPlayingId = null
                                                    }
                                                }
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(24.dp).testTag("btn_tts_${msg.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (ttsPlayingId == msg.id) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                                                contentDescription = "Speak",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Listen (TTS)", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing formulation scale-up data...", fontSize = 11.sp, color = Slate500)
                        }
                    }
                }
            }

            // Attached Photo Preview
            selectedBitmap?.let { bmp ->
                Surface(
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Attached equipment / document photo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Will be analyzed using gemini-3.1-pro-preview", fontSize = 9.sp, color = Slate500)
                        }
                        IconButton(onClick = { selectedBitmap = null }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        modifier = Modifier.testTag("btn_attach_photo")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "Attach photo", tint = MaterialTheme.colorScheme.primary)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask technical scale-up question...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text"),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val textToSend = inputText.trim()
                            val bmpToSend = selectedBitmap
                            if (textToSend.isNotEmpty() || bmpToSend != null) {
                                val userMsg = ChatMessage(
                                    sender = "user",
                                    text = if (textToSend.isEmpty() && bmpToSend != null) "Please inspect this equipment plate / document and explain the scale-up implications." else textToSend,
                                    imageBitmap = bmpToSend
                                )
                                messages = messages + userMsg
                                inputText = ""
                                selectedBitmap = null
                                isLoading = true

                                coroutineScope.launch {
                                    val reply = if (bmpToSend != null) {
                                        GeminiClient.analyzeImage(bmpToSend, userMsg.text)
                                    } else {
                                        GeminiClient.sendChatMessage(messages, userMsg.text, selectedTaskType)
                                    }
                                    messages = messages + ChatMessage(sender = "assistant", text = reply)
                                    isLoading = false
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        enabled = !isLoading && (inputText.isNotBlank() || selectedBitmap != null),
                        modifier = Modifier.testTag("btn_send_chat")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (!isLoading && (inputText.isNotBlank() || selectedBitmap != null)) MaterialTheme.colorScheme.primary else Slate300
                        )
                    }
                }
            }
        }
    }
}
