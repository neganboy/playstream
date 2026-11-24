// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL

package com.neganboy.playstream.common

import androidx.appcompat.app.AppCompatActivity

/**
 * Applies the PLAYSTREAM theme the user picked in Settings before the
 * activity draws anything. Call it at the very start of onCreate,
 * before super.onCreate, or the manifest theme (orange Momiji) shows.
 */
fun AppCompatActivity.applyPlaystreamTheme()
{
	setTheme(Preferences(this).theme.styleRes)
}
