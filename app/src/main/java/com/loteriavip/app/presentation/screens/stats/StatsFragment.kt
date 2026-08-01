package com.loteriavip.app.presentation.screens.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.loteriavip.app.R
import com.loteriavip.app.databinding.FragmentStatsBinding
import com.loteriavip.app.presentation.adapter.StatsAdapter
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatsViewModel by viewModels()
    private lateinit var adapter: StatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTabs()
        setupToolbarMenu()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = StatsAdapter(emptyList())
        binding.recyclerViewStats.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewStats.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayoutStats.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val isHot = tab?.position == 0
                viewModel.setHotSelected(isHot)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupToolbarMenu() {
        val toolbar = (activity as? com.loteriavip.app.MainActivity)?.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar?.let {
            it.menu.clear()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.statsNumbers.collect { numbers ->
                adapter.updateData(numbers)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBarStats.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.recyclerViewStats.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isHotSelected.collect { isHot ->
                if (isHot) {
                    binding.txtStatsDescription.text = "Estas estadísticas muestran los números con mayor frecuencia de salida en todos los sorteos dominicanos recientes (últimos 30 días)."
                } else {
                    binding.txtStatsDescription.text = "Estas estadísticas muestran los números con menor frecuencia de salida en todos los sorteos dominicanos recientes (últimos 30 días)."
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
