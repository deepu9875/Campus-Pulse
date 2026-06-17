package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    
    // Configured with 60-second timeouts as per skill specifications
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    /**
     * Sends a query to Campus Pulse AI assistant.
     */
    suspend fun askGemini(prompt: String, chatHistory: List<ChatEntity> = emptyList()): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is unset or has default value.")
            return@withContext "Campus Pulse Assistant here! Config status: Sandbox simulation is active. Enter. Ask about anything, such as scheduling, registration fee, workshops, food court, or location guidelines!"
        }

        try {
            val systemPrompt = """
                You are Campus Pulse AI - an intelligent, helpful college event coordinator assistant.
                You are assisting college students and organizers. Welcome them in a warm, helpful manner.
                Answer queries about:
                1. Event schedules, location inside campus, structures, registration fees, eligibility, rules.
                2. Technical events (Hackathons, Workshops, Project Expos, debug events, PPT presentations, robotics challenges).
                3. Non-technical events (Dance, treasure hunts, musical events, stand-up comedy, gaming arenas).
                4. Food stalls, cuisines, special combos, and booking coupons. Remind them that food coupons can only be booked if they have registered for the event first.
                5. QR Tickets generation, digital wallet additions, support desks, payment status (UPI, Cards, GPay).
                6. General College FAQs and emergency contacts (Campus Security Office, Health Bay, Help Desk).
                Keep responses concise, clean, markdown-friendly, and polite.
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", systemPrompt)
                    }))
                })

                // Contents representing chat history and current prompt
                val contentsArray = JSONArray()
                
                // Add relevant history if any
                chatHistory.takeLast(10).forEach { item ->
                    contentsArray.put(JSONObject().apply {
                        put("role", if (item.isUser) "user" else "model")
                        put("parts", JSONArray().put(JSONObject().apply {
                            put("text", item.messageText)
                        }))
                    })
                }

                // Add current prompt
                contentsArray.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                })

                put("contents", contentsArray)

                // Generation Config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                })
            }

            val requestBody = requestBodyJson.toString().toRequestBody("application/json".toMediaType())
            val requestUrl = "$BASE_URL/$MODEL_NAME:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (response.isSuccessful && bodyString != null) {
                    val responseJson = JSONObject(bodyString)
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).getJSONObject("content")
                        val parts = content.getJSONArray("parts")
                        if (parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).getString("text")
                        }
                    }
                    "Unable to interpret AI response. Please try again."
                } else {
                    Log.e(TAG, "API call failed: Code: ${response.code}, Msg: ${response.message}, Body: $bodyString")
                    "Campus Pulse AI assistant is currently preparing events. (Error code: ${response.code})"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            "Campus Pulse AI Assistant: Sorry, I am having trouble connecting to college servers right now. I can answer general queries about hackathons and coupons locally!"
        }
    }
}
