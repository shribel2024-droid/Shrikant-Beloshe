package com.example.pharma.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

enum class GeminiTaskType {
    GENERAL_CHAT,   // gemini-3.5-flash
    COMPLEX_REASON, // gemini-3.1-pro-preview
    FAST_QUERY      // gemini-3.1-flash-lite-preview
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageBitmap: Bitmap? = null
)

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val SYSTEM_PROMPT = """You are an expert Senior Pharmaceutical Technology Transfer, Process Engineering, and Formulation Scale-Up Specialist.
You provide technical guidance on Oral Solid Dosage (OSD) manufacturing:
- Rapid Mixer Granulator (RMG) impeller tip speed and Froude number scaling
- Fluid Bed Processors (FBP top-spray evaporation, bottom-spray Wurster column dynamics)
- Tumble Blenders (Froude scaling, total revolutions, fill ratio 50-60%)
- Extrusion-Spheronization (Cube rule vs Heat-transfer limited, friction plate loading)
- Pan Coater (Perforated pan peripheral velocity, surface area spray scaling)
- Roller Compaction (Specific compaction force SCF in kN/cm, roll gap, nip angle)
- Quadro and Multimills (Tip speed PSD invariance)
- Rotary Tablet Compression (Dwell time, turret speed, OEE, capping risk)
- Capsule Fillers (Dosator vs Tamping pin, pellet chamber filling)
Always provide rigorous scientific explanations, referencing ICH Q8 (Pharmaceutical Development), ICH Q9 (Quality Risk Management), and standard scale-up equations. Remind engineers to verify empirical results against process qualification and vendor constraints."""

    /**
     * Sends a multi-turn chat message to Gemini API.
     */
    suspend fun sendChatMessage(
        history: List<ChatMessage>,
        newMessage: String,
        taskType: GeminiTaskType = GeminiTaskType.GENERAL_CHAT
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Gemini API key is not configured. Please add your GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val modelName = when (taskType) {
            GeminiTaskType.COMPLEX_REASON -> "gemini-3.1-pro-preview"
            GeminiTaskType.FAST_QUERY -> "gemini-3.1-flash-lite-preview"
            GeminiTaskType.GENERAL_CHAT -> "gemini-3.5-flash"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val contentsArray = JSONArray()
        // Include recent history (up to last 10 messages for context)
        val recentHistory = history.takeLast(10)
        for (msg in recentHistory) {
            val role = if (msg.sender == "user") "user" else "model"
            val contentObj = JSONObject()
            contentObj.put("role", role)
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", msg.text)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }

        // Add current new message
        val newContent = JSONObject()
        newContent.put("role", "user")
        val newParts = JSONArray()
        val textPart = JSONObject()
        textPart.put("text", newMessage)
        newParts.put(textPart)
        newContent.put("parts", newParts)
        contentsArray.put(newContent)

        val rootObj = JSONObject()
        rootObj.put("contents", contentsArray)

        // System instruction
        val sysContent = JSONObject()
        val sysParts = JSONArray()
        val sysPart = JSONObject()
        sysPart.put("text", SYSTEM_PROMPT)
        sysParts.put(sysPart)
        sysContent.put("parts", sysParts)
        rootObj.put("systemInstruction", sysContent)

        val requestBody = rootObj.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "API error ${response.code}: ${response.message}"
                }
                val bodyStr = response.body?.string() ?: return@withContext "Empty response"
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val cand = candidates.getJSONObject(0)
                    val content = cand.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        sb.append(parts.getJSONObject(i).optString("text", ""))
                    }
                    sb.toString()
                } else {
                    "No response generated by model."
                }
            }
        } catch (e: Exception) {
            "Network error: ${e.localizedMessage}"
        }
    }

    /**
     * Analyze image (equipment nameplate, batch sheet, tablet core photo) using gemini-3.1-pro-preview
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String = "Analyze this pharmaceutical equipment / technical document and extract any machine dimensions, model numbers, process parameters, or scale-up specifications."
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Gemini API key is not configured. Please add your GEMINI_API_KEY in the AI Studio Secrets panel."
        }

        val modelName = "gemini-3.1-pro-preview"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val base64Img = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()

        val textPart = JSONObject()
        textPart.put("text", prompt)
        partsArray.put(textPart)

        val imgPart = JSONObject()
        val inlineData = JSONObject()
        inlineData.put("mimeType", "image/jpeg")
        inlineData.put("data", base64Img)
        imgPart.put("inlineData", inlineData)
        partsArray.put(imgPart)

        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)

        val rootObj = JSONObject()
        rootObj.put("contents", contentsArray)

        val requestBody = rootObj.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Image analysis error ${response.code}: ${response.message}"
                }
                val bodyStr = response.body?.string() ?: return@withContext "Empty response"
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val cand = candidates.getJSONObject(0)
                    val content = cand.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        sb.append(parts.getJSONObject(i).optString("text", ""))
                    }
                    sb.toString()
                } else {
                    "No image analysis content received."
                }
            }
        } catch (e: Exception) {
            "Analysis failed: ${e.localizedMessage}"
        }
    }

    /**
     * Converts text to speech using gemini-3.8-flash-tts
     * Returns audio byte array or null.
     */
    suspend fun textToSpeech(text: String): ByteArray? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") return@withContext null

        val modelName = "gemini-3.8-flash-tts"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val rootObj = JSONObject()
        val contentsArray = JSONArray()
        val contentObj = JSONObject()
        val partsArray = JSONArray()
        val textPart = JSONObject()
        textPart.put("text", text)
        partsArray.put(textPart)
        contentObj.put("parts", partsArray)
        contentsArray.put(contentObj)
        rootObj.put("contents", contentsArray)

        val genConfig = JSONObject()
        val modalities = JSONArray()
        modalities.put("AUDIO")
        genConfig.put("responseModalities", modalities)
        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", "Kore")
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        genConfig.put("speechConfig", speechConfig)
        rootObj.put("generationConfig", genConfig)

        val requestBody = rootObj.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val candidates = json.optJSONArray("candidates") ?: return@withContext null
                if (candidates.length() == 0) return@withContext null
                val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@withContext null
                val parts = content.optJSONArray("parts") ?: return@withContext null
                for (i in 0 until parts.length()) {
                    val p = parts.getJSONObject(i)
                    val inlineData = p.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val base64Data = inlineData.optString("data", "")
                        if (base64Data.isNotEmpty()) {
                            return@withContext Base64.decode(base64Data, Base64.DEFAULT)
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
