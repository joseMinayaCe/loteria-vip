package com.loteriavip.app.presentation.screens.luck

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import com.loteriavip.app.databinding.FragmentLuckBinding
import kotlin.random.Random

class LuckFragment : Fragment() {

    private var _binding: FragmentLuckBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLuckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnGenerate.setOnClickListener {
            generateNumbers()
        }

        binding.btnShare.setOnClickListener {
            shareNumbers()
        }
    }

    private fun generateNumbers() {
        val n1 = Random.nextInt(0, 100).toString().padStart(2, '0')
        val n2 = Random.nextInt(0, 100).toString().padStart(2, '0')
        val n3 = Random.nextInt(0, 100).toString().padStart(2, '0')

        animateBubble(binding.num1, n1)
        animateBubble(binding.num2, n2)
        animateBubble(binding.num3, n3)
    }

    private fun animateBubble(view: View, value: String) {
        view.animate()
            .scaleX(0.5f)
            .scaleY(0.5f)
            .setDuration(100)
            .withEndAction {
                (view as? android.widget.TextView)?.text = value
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(300)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }
            .start()
    }

    private fun shareNumbers() {
        val msg = "¡Mis números de la suerte hoy en Lotería VIP son: " +
                "${binding.num1.text}, ${binding.num2.text} y ${binding.num3.text}! 🍀"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, msg)
        }
        startContext(Intent.createChooser(intent, "Compartir Suerte"))
    }

    private fun startContext(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Handle error
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
