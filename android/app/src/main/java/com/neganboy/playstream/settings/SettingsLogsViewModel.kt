// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL

package com.neganboy.playstream.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.neganboy.playstream.common.AppDatabase
import com.neganboy.playstream.common.LogFile
import com.neganboy.playstream.common.LogManager
import com.neganboy.playstream.common.RegisteredHost
import com.neganboy.playstream.common.ext.toLiveData
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers

class SettingsLogsViewModel(val logManager: LogManager): ViewModel()
{
	private val _sessionLogs = MutableLiveData<List<LogFile>>(logManager.files)
	val sessionLogs: LiveData<List<LogFile>> get() = _sessionLogs

	private fun updateLogs()
	{
		_sessionLogs.value = logManager.files
	}

	fun deleteLog(file: LogFile)
	{
		file.file.delete()
		updateLogs()
	}
}