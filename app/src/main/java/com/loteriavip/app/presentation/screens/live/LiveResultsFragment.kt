package com.loteriavip.app.presentation.screens.live

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.tabs.TabLayout
import com.loteriavip.app.R
import com.loteriavip.app.databinding.FragmentLiveResultsBinding
import com.loteriavip.app.domain.model.LiveLotteryResult
import com.loteriavip.app.domain.model.ResultCategory
import com.loteriavip.app.presentation.adapter.ResultAdapter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LiveResultsFragment : Fragment() {

    private var _binding: FragmentLiveResultsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: LiveResultsViewModel by viewModels()
    private lateinit var adapter: ResultAdapter
    
    // AdMob Native Ads
    private val nativeAds = mutableListOf<NativeAd>()
    private var isAdLoading = false
    private var currentResults = emptyList<LiveLotteryResult>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTabs()
        setupToolbarMenu()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = ResultAdapter(emptyList()) { id ->
            viewModel.toggleFavorite(id)
        }
        binding.recyclerViewResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewResults.adapter = adapter
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val category = when (tab?.position) {
                    0 -> ResultCategory.LOTERIA
                    1 -> ResultCategory.LOTTO
                    else -> ResultCategory.AMERICANA
                }
                viewModel.setCategory(category)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupToolbarMenu() {
        val toolbar = (activity as? com.loteriavip.app.MainActivity)?.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar?.let {
            it.menu.clear()
            it.inflateMenu(R.menu.menu_live_results)
            it.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_calendar) {
                    showDatePicker()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnBackToToday.setOnClickListener {
            viewModel.clearDate()
        }
        binding.btnRetry.setOnClickListener {
            viewModel.refresh()
        }
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.select_date))
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val timeZoneUTC = TimeZone.getTimeZone("UTC")
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply {
                timeZone = timeZoneUTC
            }
            val dateString = sdf.format(Date(selection))
            viewModel.setDate(dateString)
        }

        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.filteredResults.collect { results ->
                        currentResults = results
                        if (results.isNotEmpty()) {
                            binding.layoutError.visibility = View.GONE
                            binding.recyclerViewResults.visibility = View.VISIBLE
                            loadNativeAdsAndRefreshList(results)
                        } else {
                            binding.layoutError.visibility = View.VISIBLE
                            binding.recyclerViewResults.visibility = View.GONE
                            adapter.updateData(emptyList())
                        }
                    }
                }

                launch {
                    viewModel.selectedDate.collect { date ->
                        if (date != null) {
                            binding.layoutDateBanner.visibility = View.VISIBLE
                            binding.txtSelectedDate.text = getString(R.string.results_for_date, date)
                        } else {
                            binding.layoutDateBanner.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                        if (isLoading) {
                            binding.recyclerViewResults.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                        } else {
                            if (currentResults.isNotEmpty()) {
                                binding.recyclerViewResults.visibility = View.VISIBLE
                                binding.layoutError.visibility = View.GONE
                            } else {
                                binding.recyclerViewResults.visibility = View.GONE
                                binding.layoutError.visibility = View.VISIBLE
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun loadNativeAdsAndRefreshList(results: List<LiveLotteryResult>) {
        if (results.isEmpty()) return
        
        // Calculate how many ads we need (1 ad per 4 results)
        val targetAdCount = results.size / 4
        
        // Build the combined list with existing ads
        refreshCombinedList(results)
        
        if (nativeAds.size < targetAdCount && !isAdLoading) {
            val ctx = context ?: return
            isAdLoading = true
            val adLoader = AdLoader.Builder(ctx, "ca-app-pub-3940256099942544/2247696110") // Test Native Ad Unit ID
                .forNativeAd { ad: NativeAd ->
                    nativeAds.add(ad)
                    if (isAdded) {
                        refreshCombinedList(currentResults)
                    } else {
                        ad.destroy()
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isAdLoading = false
                    }
                    override fun onAdLoaded() {
                        isAdLoading = false
                        // Load more if needed
                        if (nativeAds.size < targetAdCount) {
                            loadNativeAdsAndRefreshList(currentResults)
                        }
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    private fun refreshCombinedList(results: List<LiveLotteryResult>) {
        val combinedList = mutableListOf<Any>()
        var adIndex = 0
        
        for (i in results.indices) {
            combinedList.add(results[i])
            // Insert an ad every 4 items
            if ((i + 1) % 4 == 0 && adIndex < nativeAds.size) {
                combinedList.add(nativeAds[adIndex])
                adIndex++
            }
        }
        
        adapter.updateData(combinedList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val toolbar = (activity as? com.loteriavip.app.MainActivity)?.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar?.menu?.clear()
        nativeAds.forEach { it.destroy() }
        nativeAds.clear()
        _binding = null
    }
}
