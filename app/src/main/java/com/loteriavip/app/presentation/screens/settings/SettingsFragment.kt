package com.loteriavip.app.presentation.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.loteriavip.app.MainActivity
import com.loteriavip.app.R
import com.loteriavip.app.data.notification.NotificationChannelManager

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val ctx = context ?: return@registerForActivityResult
        if (isGranted) {
            triggerTestNotification(ctx)
        } else {
            Toast.makeText(ctx, "Permiso de notificaciones denegado. Actívalo para recibir alertas.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Set custom preference file name before loading preferences
        preferenceManager.sharedPreferencesName = "loteria_vip_prefs"

        // Load preferences from XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Always keep Light mode — dark mode switch is disabled
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Keep the dark mode switch but ignore its value (always stays light)
        val darkModeSwitch = findPreference<SwitchPreferenceCompat>("dark_mode_enabled")
        darkModeSwitch?.isChecked = false
        darkModeSwitch?.isEnabled = false
        darkModeSwitch?.summary = "Modo claro activado por defecto"

        // Setup FCM Token diagnostics
        setupFcmTokenDiagnostics()

        // Setup test notification preference
        setupTestNotification()
    }

    private fun setupTestNotification() {
        val testPref = findPreference<Preference>("diag_test_notification")
        testPref?.setOnPreferenceClickListener {
            val ctx = context ?: return@setOnPreferenceClickListener false
            checkAndTriggerTestNotification(ctx)
            true
        }
    }

    private fun checkAndTriggerTestNotification(context: Context) {
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        
        // 1. Check if notifications are enabled at the Android OS system level
        if (!notificationManagerCompat.areNotificationsEnabled()) {
            AlertDialog.Builder(context)
                .setTitle("Notificaciones desactivadas")
                .setMessage("Las notificaciones están desactivadas en los Ajustes de tu celular para Lotería VIP. ¿Deseas activarlas ahora?")
                .setPositiveButton("Abrir Ajustes") { _, _ ->
                    try {
                        val intent = Intent().apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            } else {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
            return
        }

        // 2. Check for POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        // 3. Permission granted & enabled, send test notification
        triggerTestNotification(context)
    }

    private fun triggerTestNotification(context: Context) {
        try {
            val channelId = NotificationChannelManager.CHANNEL_LOTERIAS_RD
            NotificationChannelManager.createChannels(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context, System.currentTimeMillis().toInt(), intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notificationId = (System.currentTimeMillis() % 10000).toInt() + 1000
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Lotería VIP - Prueba")
                .setContentText("¡Notificación de prueba recibida con éxito! 🎰")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("¡Notificación de prueba recibida con éxito! 🎰\nSi ves esto, tu celular está listo para recibir los resultados en tiempo real."))
                .setAutoCancel(true)
                .setColor(android.graphics.Color.parseColor("#005943"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.notify(notificationId, notificationBuilder.build())
            Toast.makeText(context, "¡Notificación de prueba enviada! Revisa la barra de tu celular.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al enviar notificación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupFcmTokenDiagnostics() {
        val tokenPref = findPreference<Preference>("diag_fcm_token")
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!isAdded || context == null) return@addOnCompleteListener
            if (task.isSuccessful) {
                val token = task.result ?: ""
                tokenPref?.summary = "Toca para copiar token (Termina en ...${token.takeLast(8)})"
                tokenPref?.setOnPreferenceClickListener {
                    val ctx = context ?: return@setOnPreferenceClickListener false
                    try {
                        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("FCM_TOKEN", token)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(ctx, "Token FCM copiado al portapapeles", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
            } else {
                tokenPref?.summary = "Error al obtener token FCM: ${task.exception?.message}"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        // Always ensure Light mode on resume
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val darkModeSwitch = findPreference<SwitchPreferenceCompat>("dark_mode_enabled")
        darkModeSwitch?.isChecked = false
    }

    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == null || sharedPreferences == null) return
        
        val allCategories = listOf(
            "gana-mas", "leidsa", "real", "la-primera",
            "loteka", "anguila", "king-lottery", "la-suerte", "lotedom",
            "new-york", "florida", "haiti-bolet"
        )

        val categoryNames = mapOf(
            "gana-mas" to "Gana Más",
            "leidsa" to "Leidsa",
            "real" to "Lotería Real",
            "la-primera" to "La Primera",
            "loteka" to "Loteka",
            "anguila" to "Anguila",
            "king-lottery" to "King Lottery",
            "la-suerte" to "La Suerte",
            "lotedom" to "LoteDom",
            "new-york" to "New York",
            "florida" to "Florida",
            "haiti-bolet" to "Haiti Bolet"
        )

        if (key == "notification_global_enabled") {
            val isGlobalEnabled = sharedPreferences.getBoolean(key, true)
            Log.d("FCM-Settings", "Global notifications toggled: $isGlobalEnabled")

            // Subscribe/unsubscribe from the global broadcast topics
            try {
                if (isGlobalEnabled) {
                    FirebaseMessaging.getInstance().subscribeToTopic("loteria-all")
                        .addOnCompleteListener { Log.d("FCM-Settings", "Subscribe loteria-all: ${it.isSuccessful}") }
                    FirebaseMessaging.getInstance().subscribeToTopic("all")
                        .addOnCompleteListener { Log.d("FCM-Settings", "Subscribe all: ${it.isSuccessful}") }
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("loteria-all")
                        .addOnCompleteListener { Log.d("FCM-Settings", "Unsubscribe loteria-all: ${it.isSuccessful}") }
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
                        .addOnCompleteListener { Log.d("FCM-Settings", "Unsubscribe all: ${it.isSuccessful}") }
                }
            } catch (e: Exception) { e.printStackTrace() }

            // Also subscribe/unsubscribe individual category topics
            allCategories.forEach { category ->
                val topic = "loteria-$category"
                try {
                    if (isGlobalEnabled) {
                        val isIndivSubscribed = sharedPreferences.getBoolean("notification_$category", true)
                        if (isIndivSubscribed) {
                            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                                .addOnCompleteListener { Log.d("FCM-Settings", "Subscribe $topic: ${it.isSuccessful}") }
                        }
                    } else {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                            .addOnCompleteListener { Log.d("FCM-Settings", "Unsubscribe $topic: ${it.isSuccessful}") }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            val ctx = context
            if (ctx != null) {
                val msg = if (isGlobalEnabled) "✅ Notificaciones VIP activadas" else "🔕 Notificaciones VIP desactivadas"
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
            }

        } else if (key.startsWith("notification_")) {
            val category = key.removePrefix("notification_")
            if (category !in allCategories) return
            
            val isGlobalEnabled = sharedPreferences.getBoolean("notification_global_enabled", true)
            val isSubscribed = sharedPreferences.getBoolean(key, true)
            val topic = "loteria-$category"
            val displayName = categoryNames[category] ?: category
            
            Log.d("FCM-Settings", "Individual toggle: $category = $isSubscribed (global=$isGlobalEnabled)")
            
            try {
                if (isGlobalEnabled && isSubscribed) {
                    FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        .addOnCompleteListener { task ->
                            Log.d("FCM-Settings", "Subscribe $topic: ${task.isSuccessful}")
                            if (task.isSuccessful) {
                                activity?.runOnUiThread {
                                    Toast.makeText(context, "🔔 Notificaciones de $displayName activadas", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                        .addOnCompleteListener { task ->
                            Log.d("FCM-Settings", "Unsubscribe $topic: ${task.isSuccessful}")
                            if (task.isSuccessful) {
                                activity?.runOnUiThread {
                                    Toast.makeText(context, "🔕 Notificaciones de $displayName desactivadas", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
