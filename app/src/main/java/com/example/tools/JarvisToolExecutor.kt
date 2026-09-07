package com.example.tools

import android.Manifest
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.JarvisDatabase
import com.example.data.MemoryEntity
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ToolResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, String> = emptyMap()
)

class JarvisToolExecutor(private val context: Context) {

    private val db = JarvisDatabase.getInstance(context)

    fun openApp(appName: String?, packageName: String?): ToolResult {
        val targetName = appName?.trim()?.lowercase(Locale.ROOT) ?: ""
        val targetPkg = packageName?.trim()

        // 1. Direct package name launch if supplied
        if (!targetPkg.isNullOrBlank()) {
            val intent = context.packageManager.getLaunchIntentForPackage(targetPkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return ToolResult(true, "Opened package $targetPkg successfully.")
            }
        }

        // 2. Common application aliases & deep intents
        when {
            targetName.contains("whatsapp") -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                    ?: context.packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return ToolResult(true, "Opening WhatsApp, sir.")
                }
            }
            targetName.contains("youtube") -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return ToolResult(true, "Launching YouTube.")
                }
            }
            targetName.contains("chrome") || targetName.contains("browser") -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.android.chrome")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return ToolResult(true, "Opening Chrome browser.")
            }
            targetName.contains("instagram") -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return ToolResult(true, "Launching Instagram.")
                }
            }
            targetName.contains("settings") -> {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ToolResult(true, "Accessing device settings.")
            }
            targetName.contains("camera") -> {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ToolResult(true, "Camera interface active.")
            }
            targetName.contains("clock") || targetName.contains("alarm") -> {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return ToolResult(true, "Accessing chronometer and alarms.")
            }
            targetName.contains("calculator") -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.calculator")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return ToolResult(true, "Opening calculator.")
                }
            }
            targetName.contains("mail") || targetName.contains("gmail") -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return ToolResult(true, "Launching Gmail.")
                }
            }
            targetName.contains("maps") || targetName.contains("navigation") -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return ToolResult(true, "Accessing planetary maps.")
                }
            }
        }

        // 3. Search installed applications by display label
        try {
            val pm = context.packageManager
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase(Locale.ROOT)
                if (label.contains(targetName) || targetName.contains(label)) {
                    val launchIntent = pm.getLaunchIntentForPackage(app.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                        return ToolResult(true, "Found and launched ${pm.getApplicationLabel(app)}.")
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through to error
        }

        return ToolResult(false, "Application '$appName' is not installed or could not be accessed.")
    }

    fun searchContact(contactName: String): ToolResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return ToolResult(false, "Contacts permission is required to search contacts.")
        }

        val results = mutableListOf<String>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && results.size < 5) {
                    val name = cursor.getString(nameIndex)
                    val number = cursor.getString(numberIndex)
                    results.add("$name: $number")
                }
            }
        } catch (e: Exception) {
            return ToolResult(false, "Error querying contacts: ${e.localizedMessage}")
        }

        return if (results.isNotEmpty()) {
            ToolResult(true, "Found matching contact(s):\n" + results.joinToString("\n"))
        } else {
            ToolResult(false, "No contacts found matching '$contactName'.")
        }
    }

    fun makePhoneCall(contactName: String?, phoneNumber: String?): ToolResult {
        var resolvedNumber = phoneNumber?.trim()

        if (resolvedNumber.isNullOrBlank() && !contactName.isNullOrBlank()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%$contactName%")
                context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        resolvedNumber = cursor.getString(numberIndex)
                    }
                }
            }
        }

        if (resolvedNumber.isNullOrBlank()) {
            return ToolResult(false, "Could not identify phone number for ${contactName ?: "contact"}.")
        }

        val cleanNumber = resolvedNumber!!.replace(" ", "").replace("-", "")
        val hasCallPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

        val intent = if (hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(intent)
            val actionType = if (hasCallPermission) "Calling" else "Opening dialer for"
            ToolResult(true, "$actionType $cleanNumber ${if (!contactName.isNullOrBlank()) "($contactName)" else ""}.")
        } catch (e: Exception) {
            ToolResult(false, "Failed to initiate call: ${e.localizedMessage}")
        }
    }

    fun sendWhatsAppMessage(contactName: String?, phoneNumber: String?, message: String): ToolResult {
        var targetNumber = phoneNumber?.trim()?.replace("+", "")?.replace(" ", "")?.replace("-", "")

        if (targetNumber.isNullOrBlank() && !contactName.isNullOrBlank()) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%$contactName%")
                context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        targetNumber = cursor.getString(numberIndex).replace("+", "").replace(" ", "").replace("-", "")
                    }
                }
            }
        }

        val encodedMsg = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (e: Exception) {
            message
        }

        val url = if (!targetNumber.isNullOrBlank()) {
            "https://api.whatsapp.com/send?phone=$targetNumber&text=$encodedMsg"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMsg"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(true, "WhatsApp prepared with message for ${contactName ?: targetNumber ?: "chat"}.")
        } catch (e: Exception) {
            // Fallback to browser
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallbackIntent)
                ToolResult(true, "WhatsApp web interface launched.")
            } catch (e2: Exception) {
                ToolResult(false, "WhatsApp is not installed on this device.")
            }
        }
    }

    fun sendEmail(recipientEmail: String, subject: String, body: String): ToolResult {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(intent)
            ToolResult(true, "Composing email to $recipientEmail.")
        } catch (e: Exception) {
            ToolResult(false, "No email application available: ${e.localizedMessage}")
        }
    }

    fun openWebsite(url: String): ToolResult {
        var cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult(true, "Navigating to $cleanUrl.")
        } catch (e: Exception) {
            ToolResult(false, "Failed to open link: ${e.localizedMessage}")
        }
    }

    fun searchWeb(query: String): ToolResult {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult(true, "Searching the global network for '$query'.")
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
                ToolResult(true, "Searching Google for '$query'.")
            } catch (e2: Exception) {
                ToolResult(false, "Web search failed: ${e2.localizedMessage}")
            }
        }
    }

    fun getCurrentTime(): ToolResult {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy, h:mm a", Locale.getDefault())
        val formatted = sdf.format(Date())
        return ToolResult(true, "Current standard time is $formatted.")
    }

    fun getDeviceInformation(): ToolResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val gigabytesAvailable = bytesAvailable / (1024 * 1024 * 1024)

        val info = buildString {
            appendLine("SYSTEM TELEMETRY REPORT:")
            appendLine("Hardware: ${Build.MANUFACTURER.uppercase(Locale.ROOT)} ${Build.MODEL}")
            appendLine("Android OS: Release ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            if (batteryPct >= 0) appendLine("Power Reservoir: $batteryPct% charge")
            appendLine("Free Internal Storage: ~${gigabytesAvailable}GB")
            appendLine("Security Protocol: Quantum Verified")
            append("Architect: Rauf")
        }
        return ToolResult(true, info)
    }

    suspend fun saveMemory(key: String, value: String): ToolResult {
        db.memoryDao().saveMemory(MemoryEntity(key = key, value = value))
        return ToolResult(true, "Stored memory key '$key': '$value'.")
    }

    suspend fun recallMemory(key: String): ToolResult {
        val item = db.memoryDao().getMemory(key)
        return if (item != null) {
            ToolResult(true, "Memory recall for '$key': ${item.value}")
        } else {
            ToolResult(false, "No record found in memory banks for '$key'.")
        }
    }

    suspend fun clearMemory(): ToolResult {
        db.memoryDao().clearAllMemories()
        db.conversationDao().clearHistory()
        return ToolResult(true, "All memory banks and logs have been wiped.")
    }
}
