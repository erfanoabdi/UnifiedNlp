/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import org.microg.nlp.api.Constants.ACTION_GEOCODER_BACKEND
import org.microg.nlp.api.Constants.ACTION_LOCATION_BACKEND
import org.microg.nlp.client.UnifiedLocationClient
import org.microg.nlp.ui.databinding.BackendListBinding
import org.microg.nlp.ui.databinding.BackendListEntryBinding

class BackendListFragment : Fragment(R.layout.backend_list) {
    val locationAdapter: BackendSettingsLineAdapter = BackendSettingsLineAdapter(this)
    val geocoderAdapter: BackendSettingsLineAdapter = BackendSettingsLineAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = BackendListBinding.inflate(inflater, container, false)
        binding.fragment = this
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        UnifiedLocationClient[requireContext()].ref()
        lifecycleScope.launchWhenStarted { updateAdapters() }
    }

    override fun onPause() {
        super.onPause()
        UnifiedLocationClient[requireContext()].unref()
    }

    fun onBackendSelected(tag: Any?) {
        val binding = tag as? BackendListEntryBinding ?: return
        val entry = binding.entry ?: return
        findNavController().navigate(R.id.openDetails, bundleOf(
                "type" to entry.type.name,
                "package" to entry.serviceInfo.packageName,
                "name" to entry.serviceInfo.name
        ))
    }

    private suspend fun updateAdapters() {
        val context = requireContext()
        locationAdapter.setEntries(createBackendInfoList(context, Intent(ACTION_LOCATION_BACKEND), UnifiedLocationClient[context].getLocationBackends(), BackendType.LOCATION))
        geocoderAdapter.setEntries(createBackendInfoList(context, Intent(ACTION_GEOCODER_BACKEND), UnifiedLocationClient[context].getGeocoderBackends(), BackendType.GEOCODER))
    }

    private fun createBackendInfoList(context: Context, intent: Intent, enabledBackends: Array<String>, type: BackendType): Array<BackendInfo?> {
        val backends = context.packageManager.queryIntentServices(intent, GET_META_DATA).map { BackendInfo(context, it.serviceInfo, type, lifecycleScope, enabledBackends) }
        if (backends.isEmpty()) return arrayOf(null)
        return backends.toTypedArray()
    }
}

class BackendSettingsLineViewHolder(val binding: BackendListEntryBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(fragment: BackendListFragment, entry: BackendInfo?) {
        binding.fragment = fragment
        binding.entry = entry
        binding.tag = binding
        binding.executePendingBindings()
    }
}

class BackendSettingsLineAdapter(val fragment: BackendListFragment) : RecyclerView.Adapter<BackendSettingsLineViewHolder>() {
    private val entries: MutableList<BackendInfo?> = arrayListOf()

    fun addOrUpdateEntry(entry: BackendInfo?) {
        if (entry == null) {
            if (entries.contains(null)) return
            entries.add(entry)
            notifyItemInserted(entries.size - 1)
        } else {
            val oldIndex = entries.indexOfFirst { it?.unsignedComponent == entry.unsignedComponent }
            if (oldIndex != -1) {
                if (entries[oldIndex] == entry) return
                entries.removeAt(oldIndex)
            }
            val targetIndex = when (val i = entries.indexOfFirst { it == null || it.name.toString() > entry.name.toString() }) {
                -1 -> entries.size
                else -> i
            }
            entries.add(targetIndex, entry)
            when (oldIndex) {
                targetIndex -> notifyItemChanged(targetIndex)
                -1 -> notifyItemInserted(targetIndex)
                else -> notifyItemMoved(oldIndex, targetIndex)
            }
        }
    }

    fun removeEntry(entry: BackendInfo?) {
        val index = entries.indexOfFirst { it == entry || it?.unsignedComponent == entry?.unsignedComponent }
        entries.removeAt(index)
        notifyItemRemoved(index)
    }

    fun setEntries(entries: Array<BackendInfo?>) {
        val oldEntries = this.entries.toTypedArray()
        for (oldEntry in oldEntries) {
            if (!entries.any { it == oldEntry || it?.unsignedComponent == oldEntry?.unsignedComponent }) {
                removeEntry(oldEntry)
            }
        }
        entries.forEach { addOrUpdateEntry(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackendSettingsLineViewHolder {
        return BackendSettingsLineViewHolder(BackendListEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BackendSettingsLineViewHolder, position: Int) {
        holder.bind(fragment, entries[position])
    }

    override fun getItemCount(): Int {
        return entries.size
    }
}


