// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL

package com.neganboy.playstream.settings

import androidx.lifecycle.ViewModel
import com.neganboy.playstream.common.AppDatabase
import com.neganboy.playstream.common.Preferences
import com.neganboy.playstream.common.ext.toLiveData

class SettingsViewModel(val database: AppDatabase, val preferences: Preferences): ViewModel()
{
	val registeredHostsCount = database.registeredHostDao().count().toLiveData()

	val bitrateAuto = preferences.bitrateAutoObservable.toLiveData()
}