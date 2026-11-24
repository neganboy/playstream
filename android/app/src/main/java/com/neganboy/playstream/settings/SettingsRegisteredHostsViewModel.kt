// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL

package com.neganboy.playstream.settings

import androidx.lifecycle.ViewModel
import com.neganboy.playstream.common.AppDatabase
import com.neganboy.playstream.common.RegisteredHost
import com.neganboy.playstream.common.ext.toLiveData
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

class SettingsRegisteredHostsViewModel(val database: AppDatabase): ViewModel()
{
	private val disposable = CompositeDisposable()

	val registeredHosts = database.registeredHostDao().getAll().toLiveData()

	/** To register a console again at the address it was added with, if it was added by IP address */
	val manualHosts = database.manualHostDao().getAll().toLiveData()

	/** Removes all registrations of the console, as registering it again would replace all of them */
	fun removeRegistration(host: RegisteredHost)
	{
		database.registeredHostDao()
			.deleteByMac(host.serverMac)
			.onErrorComplete()
			.subscribeOn(Schedulers.io())
			.subscribe()
			.addTo(disposable)
	}

	override fun onCleared()
	{
		super.onCleared()
		disposable.dispose()
	}
}