package com.loteriavip.app.presentation.components

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.google.firebase.messaging.FirebaseMessaging
import com.loteriavip.app.R

class OnboardingNotificationDialog(
    context: Context,
    private val onDismissed: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_onboarding_notifications)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(false)

        val chkGanaMas = findViewById<CheckBox>(R.id.chk_gana_mas)
        val chkLeidsa = findViewById<CheckBox>(R.id.chk_leidsa)
        val chkReal = findViewById<CheckBox>(R.id.chk_real)
        val chkPrimera = findViewById<CheckBox>(R.id.chk_primera)
        val chkLoteka = findViewById<CheckBox>(R.id.chk_loteka)
        val chkNewYork = findViewById<CheckBox>(R.id.chk_new_york)
        val chkFlorida = findViewById<CheckBox>(R.id.chk_florida)

        val btnSave = findViewById<Button>(R.id.btn_save)
        val btnDismiss = findViewById<TextView>(R.id.btn_dismiss)

        val sharedPreferences = context.getSharedPreferences("loteria_vip_prefs", Context.MODE_PRIVATE)

        btnSave.setOnClickListener {
            val subscriptions = mapOf(
                "gana-mas" to chkGanaMas.isChecked,
                "nacional" to chkGanaMas.isChecked, // Bound to Nacional as well
                "leidsa" to chkLeidsa.isChecked,
                "loterom" to chkLeidsa.isChecked,
                "real" to chkReal.isChecked,
                "la-primera" to chkPrimera.isChecked,
                "loteka" to chkLoteka.isChecked,
                "new-york" to chkNewYork.isChecked,
                "florida" to chkFlorida.isChecked
            )

            val editor = sharedPreferences.edit()
            subscriptions.forEach { (key, isSubscribed) ->
                editor.putBoolean("notification_$key", isSubscribed)
                try {
                    if (isSubscribed) {
                        FirebaseMessaging.getInstance().subscribeToTopic("loteria-$key")
                    } else {
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("loteria-$key")
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
            editor.putBoolean("onboarding_shown", true)
            editor.apply()

            dismiss()
            onDismissed()
        }

        btnDismiss.setOnClickListener {
            // Do not save notification setup, but set onboarding_shown to true so they aren't prompted again
            sharedPreferences.edit().putBoolean("onboarding_shown", true).apply()
            dismiss()
            onDismissed()
        }
    }
}
