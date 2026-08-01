package com.loteriavip.app.presentation.screens.notifications

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.loteriavip.app.databinding.FragmentNotificationHistoryBinding
import com.loteriavip.app.presentation.adapter.NotificationAdapter
import kotlinx.coroutines.launch

class NotificationHistoryFragment : Fragment() {

    private var _binding: FragmentNotificationHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotificationHistoryViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return try {
            _binding = FragmentNotificationHistoryBinding.inflate(inflater, container, false)
            binding.root
        } catch (e: Throwable) {
            Log.e("NotifHistory", "Error in onCreateView", e)
            createErrorView("onCreateView: ${Log.getStackTraceString(e)}")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            if (_binding == null) return // Error view was shown
            setupRecyclerView()
            observeViewModel()
            viewModel.markAllAsRead()
        } catch (e: Throwable) {
            Log.e("NotifHistory", "Error in onViewCreated", e)
            showErrorInFragment("onViewCreated: ${Log.getStackTraceString(e)}")
        }
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(emptyList()) { notification ->
            viewModel.markAsRead(notification.id)
        }
        binding.recyclerViewNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewNotifications.adapter = adapter

        // Swipe to delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                try {
                    val position = viewHolder.bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION && position < viewModel.notifications.value.size) {
                        val notification = viewModel.notifications.value[position]
                        viewModel.deleteNotification(notification.id)
                    }
                } catch (e: Exception) {
                    Log.e("NotifHistory", "Error on swipe delete", e)
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewNotifications)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.notifications.collect { notifications ->
                    try {
                        adapter.updateData(notifications)
                        binding.layoutEmpty.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
                    } catch (e: Exception) {
                        Log.e("NotifHistory", "Error updating notifications UI", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotifHistory", "Error collecting notifications flow", e)
                showErrorInFragment("Flow error: ${e.message}")
            }
        }
    }

    private fun createErrorView(errorMessage: String): View {
        val scrollView = ScrollView(requireContext())
        val textView = TextView(requireContext()).apply {
            text = "ERROR EN NOTIFICACIONES:\n\n$errorMessage"
            setTextColor(Color.RED)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setPadding(32, 32, 32, 32)
        }
        scrollView.addView(textView)
        return scrollView
    }

    private fun showErrorInFragment(errorMessage: String) {
        try {
            val container = view as? ViewGroup ?: return
            container.removeAllViews()
            val scrollView = ScrollView(requireContext())
            val textView = TextView(requireContext()).apply {
                text = "ERROR EN NOTIFICACIONES:\n\n$errorMessage"
                setTextColor(Color.RED)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setPadding(32, 32, 32, 32)
            }
            scrollView.addView(textView)
            container.addView(scrollView)
        } catch (e: Throwable) {
            Log.e("NotifHistory", "Even error display failed", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
