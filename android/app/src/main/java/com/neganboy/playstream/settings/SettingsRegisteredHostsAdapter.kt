// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL

package com.neganboy.playstream.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.neganboy.playstream.common.RegisteredHost
import com.neganboy.playstream.databinding.ItemRegisteredHostBinding

class SettingsRegisteredHostsAdapter(private val clickCallback: (RegisteredHost) -> Unit): RecyclerView.Adapter<SettingsRegisteredHostsAdapter.ViewHolder>()
{
	class ViewHolder(val binding: ItemRegisteredHostBinding): RecyclerView.ViewHolder(binding.root)

	var hosts: List<RegisteredHost> = listOf()
		set(value)
		{
			field = value
			notifyDataSetChanged()
		}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
		= ViewHolder(ItemRegisteredHostBinding.inflate(LayoutInflater.from(parent.context), parent, false))

	override fun getItemCount() = hosts.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int)
	{
		val host = hosts[position]
		holder.binding.nameTextView.text = "${host.serverNickname} (${if(host.target.isPS5) "PS5" else "PS4"})"
		holder.binding.summaryTextView.text = host.serverMac.toString()
		holder.itemView.setOnClickListener { clickCallback(host) }
	}
}