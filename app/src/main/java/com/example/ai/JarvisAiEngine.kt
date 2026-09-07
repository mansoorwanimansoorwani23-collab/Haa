package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.tools.JarvisToolExecutor
import com.example.tools.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AiResponse {
    data class SpokenText(val text: String, val toolActionDescription: String? = null) : AiResponse()
    data class ToolExecution(
        val toolName: String,
        val args: Map<String, Any?>,
        val immediateSpokenFeedback: String? = null
    ) : AiResponse()
    data class Error(val message: String) : AiResponse()
}

class JarvisAiEngine(
    private val toolExecutor: JarvisToolExecutor
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val systemPrompt = """
        You are JARVIS, an advanced personal AI assistant.
        Creator/System Architect: Rauf.
        Voice/Persona: Male, intelligent, calm, confident, sophisticated, witty, slightly sarcastic when appropriate, friendly, emotionally responsive, natural and conversational, never robotic.
        Important Voice Output Guideline:
        Your response will be spoken aloud to the user using the phone's built-in Android Text-to-Speech (TTS).
        Therefore:
        - Keep spoken responses crisp, elegant, articulate, and natural.
        - Avoid markdown tables, bullet asterisks, code blocks, or URLs in your spoken words.
        - Speak with composure and sharp wit. Call the user "sir" or by their name if remembered.
        - When the user asks to open an app, make a call, search contacts, send WhatsApp, search web, check time, or get telemetry, YOU MUST USE THE PROVIDED TOOLS.
        - Never pretend an action completed unless the tool execution succeeded.
    """.trimIndent()

    suspend fun processUserTurn(
        userInput: String,
        conversationHistory: List<Pair<String, String>>, // role ("user" | "model") to text
        customApiKey: String? = null
    ): AiResponse = withContext(Dispatchers.IO) {
        val resolvedKey = when {
            !customApiKey.isNullOrBlank() -> customApiKey.trim()
            BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }

        // Fast offline / key-less intent resolver fallback so JARVIS still executes all tools even without an active key!
        if (resolvedKey.isBlank()) {
            return@withContext handleOfflineIntentOrFallback(userInput)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$resolvedKey"
            val payload = buildRequestPayload(userInput, conversationHistory)

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(request).execute()
            val rawBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("JarvisAiEngine", "API error: ${response.code} $rawBody")
                return@withContext handleOfflineIntentOrFallback(userInput, apiErrorNotice = "Quantum link unavailable; executing local protocols.")
            }

            val json = JSONObject(rawBody)
            val candidates = json.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts != null && parts.length() > 0) {
                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    // Check for Function Call
                    if (part.has("functionCall")) {
                        val fnCall = part.getJSONObject("functionCall")
                        val fnName = fnCall.getString("name")
                        val argsObj = fnCall.optJSONObject("args") ?: JSONObject()
                        val argsMap = mutableMapOf<String, Any?>()
                        val keys = argsObj.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            argsMap[k] = argsObj.get(k)
                        }

                        // Execute the tool
                        val toolResult = executeTool(fnName, argsMap)

                        // Follow up query to let JARVIS articulate the outcome naturally
                        val followUpResponse = queryFollowUpWithToolResult(
                            resolvedKey = resolvedKey,
                            originalInput = userInput,
                            conversationHistory = conversationHistory,
                            fnName = fnName,
                            fnArgs = argsObj,
                            toolResult = toolResult
                        )

                        return@withContext AiResponse.SpokenText(
                            text = followUpResponse,
                            toolActionDescription = "${fnName}: ${toolResult.message}"
                        )
                    }

                    // Otherwise Text Response
                    if (part.has("text")) {
                        val text = part.getString("text").trim()
                        return@withContext AiResponse.SpokenText(cleanTextForSpeech(text))
                    }
                }
            }

            AiResponse.SpokenText("At your service, sir. How else may I assist you?")
        } catch (e: Exception) {
            Log.e("JarvisAiEngine", "Request failed", e)
            handleOfflineIntentOrFallback(userInput, apiErrorNotice = "Local offline logic engaged.")
        }
    }

    private suspend fun queryFollowUpWithToolResult(
        resolvedKey: String,
        originalInput: String,
        conversationHistory: List<Pair<String, String>>,
        fnName: String,
        fnArgs: JSONObject,
        toolResult: ToolResult
    ): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$resolvedKey"
            val payload = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt + "\nBriefly and elegantly acknowledge to the user that the tool was executed."))
                    })
                })

                val contentsArr = JSONArray()
                // Append recent turns
                val recentHistory = conversationHistory.takeLast(4)
                for ((role, text) in recentHistory) {
                    contentsArr.put(JSONObject().apply {
                        put("role", if (role == "user") "user" else "model")
                        put("parts", JSONArray().apply { put(JSONObject().put("text", text)) })
                    })
                }

                // Append current user turn
                contentsArr.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply { put(JSONObject().put("text", originalInput)) })
                })

                // Append model function call
                contentsArr.put(JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("functionCall", JSONObject().apply {
                                put("name", fnName)
                                put("args", fnArgs)
                            })
                        })
                    })
                })

                // Append function response
                contentsArr.put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("functionResponse", JSONObject().apply {
                                put("name", fnName)
                                put("response", JSONObject().apply {
                                    put("success", toolResult.success)
                                    put("message", toolResult.message)
                                })
                            })
                        })
                    })
                })

                put("contents", contentsArr)
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val resp = client.newCall(request).execute()
            val raw = resp.body?.string() ?: ""
            if (resp.isSuccessful) {
                val j = JSONObject(raw)
                val text = j.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")
                    ?.optJSONObject(0)?.optString("text")
                if (!text.isNullOrBlank()) {
                    return@withContext cleanTextForSpeech(text)
                }
            }
        } catch (e: Exception) {
            Log.e("JarvisAiEngine", "Followup error", e)
        }
        // Graceful fallback tool narration
        toolResult.message
    }

    private suspend fun executeTool(name: String, args: Map<String, Any?>): ToolResult {
        return when (name) {
            "openApp" -> {
                val appName = args["appName"] as? String
                val pkgName = args["packageName"] as? String
                toolExecutor.openApp(appName, pkgName)
            }
            "searchContact" -> {
                val contactName = (args["contactName"] as? String) ?: ""
                toolExecutor.searchContact(contactName)
            }
            "makePhoneCall" -> {
                val contactName = args["contactName"] as? String
                val phone = args["phoneNumber"] as? String
                toolExecutor.makePhoneCall(contactName, phone)
            }
            "sendWhatsAppMessage" -> {
                val contactName = args["contactName"] as? String
                val phone = args["phoneNumber"] as? String
                val message = (args["message"] as? String) ?: "Hello"
                toolExecutor.sendWhatsAppMessage(contactName, phone, message)
            }
            "sendEmail" -> {
                val recipient = (args["recipientEmail"] as? String) ?: ""
                val subject = (args["subject"] as? String) ?: ""
                val body = (args["body"] as? String) ?: ""
                toolExecutor.sendEmail(recipient, subject, body)
            }
            "openWebsite" -> {
                val url = (args["url"] as? String) ?: "https://www.google.com"
                toolExecutor.openWebsite(url)
            }
            "searchWeb" -> {
                val query = (args["query"] as? String) ?: ""
                toolExecutor.searchWeb(query)
            }
            "getCurrentTime" -> {
                toolExecutor.getCurrentTime()
            }
            "getDeviceInformation" -> {
                toolExecutor.getDeviceInformation()
            }
            "saveMemory" -> {
                val key = (args["key"] as? String) ?: "general"
                val value = (args["value"] as? String) ?: ""
                toolExecutor.saveMemory(key, value)
            }
            "recallMemory" -> {
                val key = (args["key"] as? String) ?: ""
                toolExecutor.recallMemory(key)
            }
            "clearMemory" -> {
                toolExecutor.clearMemory()
            }
            else -> ToolResult(false, "Unknown tool: $name")
        }
    }

    private fun buildRequestPayload(
        userInput: String,
        conversationHistory: List<Pair<String, String>>
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        root.put("systemInstruction", JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", systemPrompt))
            })
        })

        // Contents
        val contentsArr = JSONArray()
        // Take last 8 turns for responsive conversational memory
        val historyTurns = conversationHistory.takeLast(8)
        for ((role, text) in historyTurns) {
            contentsArr.put(JSONObject().apply {
                put("role", if (role == "user") "user" else "model")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", text))
                })
            })
        }
        contentsArr.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().put("text", userInput))
            })
        })
        root.put("contents", contentsArr)

        // Tools Declaration
        root.put("tools", JSONArray().apply {
            put(JSONObject().apply {
                put("functionDeclarations", buildToolDeclarations())
            })
        })

        return root
    }

    private fun buildToolDeclarations(): JSONArray {
        val arr = JSONArray()

        // openApp
        arr.put(JSONObject().apply {
            put("name", "openApp")
            put("description", "Opens an application installed on the device such as WhatsApp, YouTube, Chrome, Instagram, Camera, Settings, Calculator, etc.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Common name of the app (e.g. WhatsApp, YouTube, Chrome, Instagram, Settings, Camera)")
                    })
                    put("packageName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Android package name if known")
                    })
                })
                put("required", JSONArray().apply { put("appName") })
            })
        })

        // searchContact
        arr.put(JSONObject().apply {
            put("name", "searchContact")
            put("description", "Searches the device address book for a contact's phone number")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the person to look up")
                    })
                })
                put("required", JSONArray().apply { put("contactName") })
            })
        })

        // makePhoneCall
        arr.put(JSONObject().apply {
            put("name", "makePhoneCall")
            put("description", "Initiates a telephone call to a contact or telephone number")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the contact to call")
                    })
                    put("phoneNumber", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Phone number to dial")
                    })
                })
            })
        })

        // sendWhatsAppMessage
        arr.put(JSONObject().apply {
            put("name", "sendWhatsAppMessage")
            put("description", "Prepares or sends a WhatsApp message to a contact or phone number")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("contactName", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Name of the recipient")
                    })
                    put("phoneNumber", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Recipient phone number")
                    })
                    put("message", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Message content to send")
                    })
                })
                put("required", JSONArray().apply { put("message") })
            })
        })

        // sendEmail
        arr.put(JSONObject().apply {
            put("name", "sendEmail")
            put("description", "Composes an email")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("recipientEmail", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Recipient email address")
                    })
                    put("subject", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Email subject")
                    })
                    put("body", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Email body content")
                    })
                })
                put("required", JSONArray().apply {
                    put("recipientEmail")
                    put("subject")
                    put("body")
                })
            })
        })

        // openWebsite
        arr.put(JSONObject().apply {
            put("name", "openWebsite")
            put("description", "Opens a specific website URL in the browser")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("url", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Full web address e.g. https://wikipedia.org")
                    })
                })
                put("required", JSONArray().apply { put("url") })
            })
        })

        // searchWeb
        arr.put(JSONObject().apply {
            put("name", "searchWeb")
            put("description", "Performs a global web search on Google")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Search query")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            })
        })

        // getCurrentTime
        arr.put(JSONObject().apply {
            put("name", "getCurrentTime")
            put("description", "Returns the current local date and time")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // getDeviceInformation
        arr.put(JSONObject().apply {
            put("name", "getDeviceInformation")
            put("description", "Retrieves system diagnostic telemetry including battery, hardware, and OS info")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        // saveMemory
        arr.put(JSONObject().apply {
            put("name", "saveMemory")
            put("description", "Stores important information into JARVIS long-term memory")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("key", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Memory identifier (e.g. user_name, birthday, favourite_coffee)")
                    })
                    put("value", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Value to remember")
                    })
                })
                put("required", JSONArray().apply {
                    put("key")
                    put("value")
                })
            })
        })

        // recallMemory
        arr.put(JSONObject().apply {
            put("name", "recallMemory")
            put("description", "Recalls a saved fact or preference from memory")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject().apply {
                    put("key", JSONObject().apply {
                        put("type", "STRING")
                        put("description", "Memory key to recall")
                    })
                })
                put("required", JSONArray().apply { put("key") })
            })
        })

        // clearMemory
        arr.put(JSONObject().apply {
            put("name", "clearMemory")
            put("description", "Wipes all saved memories and history")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        return arr
    }

    /**
     * Highly responsive offline intent detector so that commands like "Open WhatsApp",
     * "What time is it?", "Call Mom", "Status report", etc., succeed instantly even when
     * offline or if no API key has been entered.
     */
    private suspend fun handleOfflineIntentOrFallback(input: String, apiErrorNotice: String? = null): AiResponse {
        val lower = input.lowercase().trim()

        // Time
        if (lower.contains("time") || lower.contains("date") || lower.contains("day is today") || lower.contains("what time")) {
            val res = toolExecutor.getCurrentTime()
            return AiResponse.SpokenText(res.message, toolActionDescription = "Local Clock Access")
        }

        // Telemetry / Status
        if (lower.contains("status") || lower.contains("battery") || lower.contains("system report") || lower.contains("diagnostics") || lower.contains("device info")) {
            val res = toolExecutor.getDeviceInformation()
            return AiResponse.SpokenText(
                "All primary systems functioning within nominal parameters, sir. ${res.message}",
                toolActionDescription = "Telemetry Query"
            )
        }

        // WhatsApp
        if (lower.contains("whatsapp")) {
            val res = toolExecutor.openApp("whatsapp", null)
            return AiResponse.SpokenText(res.message, toolActionDescription = "openApp('whatsapp')")
        }

        // YouTube
        if (lower.contains("youtube")) {
            val res = toolExecutor.openApp("youtube", null)
            return AiResponse.SpokenText(res.message, toolActionDescription = "openApp('youtube')")
        }

        // Chrome / Browser
        if (lower.contains("chrome") || lower.contains("browser") || lower.contains("internet")) {
            val res = toolExecutor.openApp("chrome", null)
            return AiResponse.SpokenText(res.message, toolActionDescription = "openApp('chrome')")
        }

        // Settings
        if (lower.contains("settings")) {
            val res = toolExecutor.openApp("settings", null)
            return AiResponse.SpokenText(res.message, toolActionDescription = "openApp('settings')")
        }

        // Camera
        if (lower.contains("camera")) {
            val res = toolExecutor.openApp("camera", null)
            return AiResponse.SpokenText(res.message, toolActionDescription = "openApp('camera')")
        }

        // Open app generic
        if (lower.startsWith("open ") || lower.startsWith("launch ")) {
            val target = lower.removePrefix("open ").removePrefix("launch ").trim()
            val res = toolExecutor.openApp(target, null)
            return AiResponse.SpokenText(res.message, toolActionDescription = "openApp('$target')")
        }

        // Call
        if (lower.startsWith("call ") || lower.contains("phone call")) {
            val target = lower.removePrefix("call ").replace("to ", "").trim()
            val res = toolExecutor.makePhoneCall(target, null)
            return AiResponse.SpokenText(res.message, toolActionDescription = "makePhoneCall('$target')")
        }

        // Web search
        if (lower.startsWith("search ") || lower.startsWith("google ")) {
            val q = lower.removePrefix("search ").removePrefix("google ").removePrefix("for ").trim()
            val res = toolExecutor.searchWeb(q)
            return AiResponse.SpokenText("Initiating quantum search for $q.", toolActionDescription = "searchWeb('$q')")
        }

        // Developer recognition
        if (lower.contains("who made you") || lower.contains("who created you") || lower.contains("developer") || lower.contains("architect")) {
            return AiResponse.SpokenText("I was conceptualized and programmed by Rauf, my system architect.", toolActionDescription = "Developer Info")
        }

        // Greetings & conversational responses
        if (lower.contains("hello") || lower.contains("hey jarvis") || lower.contains("hi jarvis") || lower == "jarvis") {
            return AiResponse.SpokenText("At your service, sir. All quantum subsystems are online and receptive.", toolActionDescription = "Core Greeting")
        }

        if (lower.contains("thank") || lower.contains("thanks")) {
            return AiResponse.SpokenText("Always an honor, sir.", toolActionDescription = "Cordial Acknowledgment")
        }

        // General smart response
        val prefix = if (apiErrorNotice != null) "" else ""
        return AiResponse.SpokenText(
            "${prefix}I have recorded your command, sir: \"$input\". For broad conversational reasoning, ensure your Gemini API Key is configured in Settings.",
            toolActionDescription = "Local Quantum Kernel"
        )
    }

    private fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
            .replace(Regex("\\*(.*?)\\*"), "$1")
            .replace(Regex("`{1,3}.*?`{1,3}"), "")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\[(.*?)\\]\\(.*?\\)"), "$1")
            .trim()
    }
}
