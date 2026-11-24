// SPDX-License-Identifier: LicenseRef-AGPL-3.0-only-OpenSSL

package com.neganboy.playstream.common

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.neganboy.playstream.R
import com.neganboy.playstream.lib.Codec
import com.neganboy.playstream.lib.ConnectVideoProfile
import com.neganboy.playstream.lib.VideoFPSPreset
import com.neganboy.playstream.lib.VideoResolutionPreset
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlin.math.max
import kotlin.math.min

class Preferences(context: Context)
{
	enum class Resolution(val value: String, @StringRes val title: Int, val preset: VideoResolutionPreset)
	{
		RES_360P("360p", R.string.preferences_resolution_title_360p, VideoResolutionPreset.RES_360P),
		RES_540P("540p", R.string.preferences_resolution_title_540p, VideoResolutionPreset.RES_540P),
		RES_720P("720p", R.string.preferences_resolution_title_720p, VideoResolutionPreset.RES_720P),
		RES_1080P("1080p", R.string.preferences_resolution_title_1080p, VideoResolutionPreset.RES_1080P),
	}

	enum class FPS(val value: String, @StringRes val title: Int, val preset: VideoFPSPreset)
	{
		FPS_30("30", R.string.preferences_fps_title_30, VideoFPSPreset.FPS_30),
		FPS_60("60", R.string.preferences_fps_title_60, VideoFPSPreset.FPS_60)
	}

	enum class Codec(val value: String, @StringRes val title: Int, val codec: com.neganboy.playstream.lib.Codec)
	{
		CODEC_H264("h264", R.string.preferences_codec_title_h264, com.neganboy.playstream.lib.Codec.CODEC_H264),
		CODEC_H265("h265", R.string.preferences_codec_title_h265, com.neganboy.playstream.lib.Codec.CODEC_H265),
		CODEC_H265_HDR("h265_hdr", R.string.preferences_codec_title_h265_hdr, com.neganboy.playstream.lib.Codec.CODEC_H265_HDR)
	}

	/**
	 * How the stream is drawn. The presets Competitive and Story decide this on their own,
	 * the others follow the settings for smooth gradients and super resolution.
	 */
	enum class RenderPath
	{
		/** Follow the settings */
		PREFERENCES,
		/** Straight to the SurfaceView, no shaders */
		DIRECT,
		/** Through the shaders, with super resolution on */
		UPSCALE
	}

	/**
	 * Resolution, frame rate and bitrate together. Custom uses the separate settings for these.
	 */
	enum class Quality(val value: String, @StringRes val title: Int, @StringRes val summary: Int)
	{
		SMOOTH("smooth", R.string.quality_smooth, R.string.quality_smooth_summary),
		BALANCED("balanced", R.string.quality_balanced, R.string.quality_balanced_summary),
		SHARP("sharp", R.string.quality_sharp, R.string.quality_sharp_summary),
		/** 720p 60 fps H.264 15 Mbit/s, drawn straight to the SurfaceView */
		COMPETITIVE("competitive", R.string.quality_competitive, R.string.quality_competitive_summary),
		/** 1080p 60 fps H.265, the Sharp bitrate, with super resolution */
		STORY("story", R.string.quality_story, R.string.quality_story_summary),
		/** 1080p 60 fps at 50 Mbit/s, only for a strong local Wi-Fi */
		ULTRA_50("ultra50", R.string.quality_ultra_50, R.string.quality_ultra_summary),
		/** 1080p 60 fps at 100 Mbit/s, only for a strong local Wi-Fi */
		ULTRA_100("ultra100", R.string.quality_ultra_100, R.string.quality_ultra_summary),
		CUSTOM("custom", R.string.quality_custom, R.string.quality_custom_summary);

		companion object
		{
			fun fromValue(value: String?) = values().firstOrNull { it.value == value }
		}

		/** The Ultra presets send more data than most away-from-home networks can carry */
		val localOnly get() = this == ULTRA_50 || this == ULTRA_100

		val renderPath get() = when(this)
		{
			COMPETITIVE -> RenderPath.DIRECT
			STORY -> RenderPath.UPSCALE
			else -> RenderPath.PREFERENCES
		}
	}

	/**
	 * App theme: the look of the main screen and settings.
	 * NEON_BLUE is the default, the cyan glow from the reference screenshot.
	 */
	enum class Theme(val value: String, @StringRes val title: Int, val styleRes: Int)
	{
		NEON_BLUE("neon_blue", R.string.theme_neon_blue, R.style.AppTheme_NeonBlue),
		VIOLET("violet", R.string.theme_violet, R.style.AppTheme_Violet),
		GOLD("gold", R.string.theme_gold, R.style.AppTheme_Gold);

		companion object
		{
			fun fromValue(value: String?) = values().firstOrNull { it.value == value }
		}
	}

	companion object
	{
		val resolutionAll = Resolution.values()
		/** Little data and delay, for a weak network and away from home */
		private const val SMOOTH_BITRATE = 5000
		/** A PS4 Pro can't stream much more */
		private const val SHARP_BITRATE_PS4 = 15000
		private const val SHARP_BITRATE_PS5 = 25000
		private const val COMPETITIVE_BITRATE = 15000
		private const val ULTRA_50_BITRATE = 50000
		private const val ULTRA_100_BITRATE = 100000
		val qualityAwayDefault = Quality.SMOOTH
		val fpsDefault = FPS.FPS_60
		val fpsAll = FPS.values()
		val codecDefault = Codec.CODEC_H265
		val codecAll = Codec.values()

		/**
		 * Whether the phone has an H.265 decoder for 10 bit video (Main10), which HDR needs.
		 * This says nothing about whether the display or the console's HDR setting will show it.
		 */
		fun isHdrDecodingSupported(): Boolean = try
		{
			MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
				!info.isEncoder && info.supportedTypes.any { it.equals("video/hevc", ignoreCase = true) } &&
					info.getCapabilitiesForType("video/hevc").profileLevels.any {
						it.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
						it.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
					}
			}
		}
		catch(e: Exception)
		{
			false
		}
	}

	internal val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
	private val sharedPreferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		when(key)
		{
			resolutionKey -> bitrateAutoSubject.onNext(bitrateAuto)
		}
	}.also { sharedPreferences.registerOnSharedPreferenceChangeListener(it) }

	private val resources = context.resources
	private val packageManager = context.packageManager
	private val homeActivity = ComponentName(context, "com.neganboy.playstream.main.HomeActivity")

	/**
	 * TV boxes and other devices without a touchscreen, played with a controller on a big screen
	 */
	val isTv = !packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
			|| packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

	private val resolutionDefault = if(isTv) Resolution.RES_1080P else Resolution.RES_720P

	val discoveryEnabledKey get() = resources.getString(R.string.preferences_discovery_enabled_key)
	var discoveryEnabled
		get() = sharedPreferences.getBoolean(discoveryEnabledKey, true)
		set(value) { sharedPreferences.edit().putBoolean(discoveryEnabledKey, value).apply() }

	val onScreenControlsEnabledKey get() = resources.getString(R.string.preferences_on_screen_controls_enabled_key)
	var onScreenControlsEnabled
		get() = sharedPreferences.getBoolean(onScreenControlsEnabledKey, !isTv)
		set(value) { sharedPreferences.edit().putBoolean(onScreenControlsEnabledKey, value).apply() }


	/**
	 * Whether Chiaki is offered as the home screen, which makes a TV box start into it
	 */
	val homeScreenKey get() = resources.getString(R.string.preferences_home_screen_key)
	var homeScreen
		get() = packageManager.getComponentEnabledSetting(homeActivity) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
		set(value) = packageManager.setComponentEnabledSetting(homeActivity,
			if(value) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
			PackageManager.DONT_KILL_APP)

	val rumbleEnabledKey get() = resources.getString(R.string.preferences_rumble_enabled_key)
	var rumbleEnabled
		get() = sharedPreferences.getBoolean(rumbleEnabledKey, true)
		set(value) { sharedPreferences.edit().putBoolean(rumbleEnabledKey, value).apply() }

	val dualSenseEnabledKey get() = resources.getString(R.string.preferences_dualsense_enabled_key)
	var dualSenseEnabled
		get() = sharedPreferences.getBoolean(dualSenseEnabledKey, true)
		set(value) { sharedPreferences.edit().putBoolean(dualSenseEnabledKey, value).apply() }

	val controllerHeadphonesKey get() = resources.getString(R.string.preferences_controller_headphones_key)
	var controllerHeadphones
		get() = sharedPreferences.getBoolean(controllerHeadphonesKey, false)
		set(value) { sharedPreferences.edit().putBoolean(controllerHeadphonesKey, value).apply() }

	val motionEnabledKey get() = resources.getString(R.string.preferences_motion_enabled_key)
	var motionEnabled
		get() = sharedPreferences.getBoolean(motionEnabledKey, true)
		set(value) { sharedPreferences.edit().putBoolean(motionEnabledKey, value).apply() }

	val buttonHapticEnabledKey get() = resources.getString(R.string.preferences_button_haptic_enabled_key)
	var buttonHapticEnabled
		get() = sharedPreferences.getBoolean(buttonHapticEnabledKey, true)
		set(value) { sharedPreferences.edit().putBoolean(buttonHapticEnabledKey, value).apply() }

	val logVerboseKey get() = resources.getString(R.string.preferences_log_verbose_key)
	var logVerbose
		get() = sharedPreferences.getBoolean(logVerboseKey, false)
		set(value) { sharedPreferences.edit().putBoolean(logVerboseKey, value).apply() }


	val debandingEnabledKey get() = resources.getString(R.string.preferences_debanding_key)
	var debandingEnabled
		// Off by default, like the switch in the settings: the shader's extra rendering pass adds delay
		get() = sharedPreferences.getBoolean(debandingEnabledKey, false)
		set(value) { sharedPreferences.edit().putBoolean(debandingEnabledKey, value).apply() }

	val upscalingEnabledKey get() = resources.getString(R.string.preferences_upscaling_key)
	var upscalingEnabled
		// Off by default, like the switch in the settings: drawing through shaders adds a little delay,
		// and it only helps on screens with more pixels than the stream
		get() = sharedPreferences.getBoolean(upscalingEnabledKey, false)
		set(value) { sharedPreferences.edit().putBoolean(upscalingEnabledKey, value).apply() }

	val touchscreenTouchpadEnabledKey get() = "preferences_touchscreen_touchpad_enabled"
	var touchscreenTouchpadEnabled
		get() = sharedPreferences.getBoolean(touchscreenTouchpadEnabledKey, false)
		set(value) { sharedPreferences.edit().putBoolean(touchscreenTouchpadEnabledKey, value).apply() }

	/** Whether leaving the app keeps the stream in a small window. TV launchers handle it poorly. */
	val pictureInPictureKey get() = "stream_picture_in_picture"
	var pictureInPicture
		get() = sharedPreferences.getBoolean(pictureInPictureKey, !isTv)
		set(value) { sharedPreferences.edit().putBoolean(pictureInPictureKey, value).apply() }

	/** Dual-screen handhelds: the touchpad and the PS, Create and Options buttons on the second screen */
	val secondScreenKey get() = "second_screen"
	var secondScreen: Boolean
		get() = sharedPreferences.getBoolean(secondScreenKey, true)
		set(value) { sharedPreferences.edit().putBoolean(secondScreenKey, value).apply() }

	/** Time spent streaming, for the one-time thank-you on the main screen */
	var playTimeMs: Long
		get() = sharedPreferences.getLong("play_time_ms", 0)
		set(value) { sharedPreferences.edit().putLong("play_time_ms", value).apply() }

	/** Whether the one-time thank-you was answered, either way */
	var supportPromptDone: Boolean
		get() = sharedPreferences.getBoolean("support_prompt_done", false)
		set(value) { sharedPreferences.edit().putBoolean("support_prompt_done", value).apply() }

	val streamStatsEnabledKey get() = "stream_stats_enabled"
	var streamStatsEnabled
		get() = sharedPreferences.getBoolean(streamStatsEnabledKey, false)
		set(value) { sharedPreferences.edit().putBoolean(streamStatsEnabledKey, value).apply() }

	// Last used Fit/Zoom/Stretch mode of the stream, stored as the TransformMode name
	val streamDisplayModeKey get() = "stream_display_mode"
	var streamDisplayMode: String?
		get() = sharedPreferences.getString(streamDisplayModeKey, null)
		set(value) { sharedPreferences.edit().putString(streamDisplayModeKey, value).apply() }

	val sharpnessIntensityKey get() = "preferences_sharpness_intensity"
	/** The same setting as sharpnessIntensity, in whole percent, so a slider can store it without rounding errors */
	var sharpnessPercent: Int
		get() = sharedPreferences.getInt(sharpnessIntensityKey, 0).coerceIn(0, 100)
		set(value) { sharedPreferences.edit().putInt(sharpnessIntensityKey, value.coerceIn(0, 100)).apply() }
	var sharpnessIntensity: Float
		get() = sharedPreferences.getInt(sharpnessIntensityKey, 0).toFloat() / 100f
		set(value) { sharedPreferences.edit().putInt(sharpnessIntensityKey, (value * 100f).toInt()).apply() }


	val resolutionKey get() = resources.getString(R.string.preferences_resolution_key)
	var resolution
		get() = sharedPreferences.getString(resolutionKey, resolutionDefault.value)?.let { value ->
			Resolution.values().firstOrNull { it.value == value }
		} ?: resolutionDefault
		set(value) { sharedPreferences.edit().putString(resolutionKey, value.value).apply() }

	val fpsKey get() = resources.getString(R.string.preferences_fps_key)
	var fps
		get() = sharedPreferences.getString(fpsKey, fpsDefault.value)?.let { value ->
			FPS.values().firstOrNull { it.value == value }
		}  ?: fpsDefault
		set(value) { sharedPreferences.edit().putString(fpsKey, value.value).apply() }

	fun validateBitrate(bitrate: Int) = max(2000, min(100000, bitrate))
	val bitrateKey get() = resources.getString(R.string.preferences_bitrate_key)
	var bitrate
		get() = sharedPreferences.getInt(bitrateKey, 0).let { if(it == 0) null else validateBitrate(it) }
		set(value) { sharedPreferences.edit().putInt(bitrateKey, if(value != null) validateBitrate(value) else 0).apply() }
	val bitrateAuto get() = videoProfileDefaultBitrate.bitrate
	private val bitrateAutoSubject by lazy { BehaviorSubject.createDefault(bitrateAuto) }
	val bitrateAutoObservable: Observable<Int> get() = bitrateAutoSubject

	val codecKey get() = resources.getString(R.string.preferences_codec_key)
	var codec
		get() = sharedPreferences.getString(codecKey, codecDefault.value)?.let { value ->
			Codec.values().firstOrNull { it.value == value }
		}  ?: codecDefault
		set(value) { sharedPreferences.edit().putString(codecKey, value.value).apply() }

	private val videoProfileDefaultBitrate get() = ConnectVideoProfile.preset(resolution.preset, fps.preset, codecEffective)
	val videoProfile get() = videoProfile(quality, true)

	val qualityKey get() = "stream_quality"
	var quality: Quality
		get() = Quality.fromValue(sharedPreferences.getString(qualityKey, null))
			// The details someone set before there were presets stay as they are
			?: if(listOf(resolutionKey, fpsKey, bitrateKey).any { sharedPreferences.contains(it) }) Quality.CUSTOM else Quality.BALANCED
		set(value) { sharedPreferences.edit().putString(qualityKey, value.value).apply() }

	/** For playing away from home, where the network is usually slower. Custom has the same values as at home. */
	val qualityAwayKey get() = "stream_quality_away"
	var qualityAway: Quality
		get() = Quality.fromValue(sharedPreferences.getString(qualityAwayKey, null)) ?: qualityAwayDefault
		set(value) { sharedPreferences.edit().putString(qualityAwayKey, value.value).apply() }

	/** App theme, see Preferences.Theme. Changing it needs an activity restart to take effect. */
	val themeKey get() = "app_theme"
	var theme: Theme
		get() = Theme.fromValue(sharedPreferences.getString(themeKey, null)) ?: Theme.NEON_BLUE
		set(value) { sharedPreferences.edit().putString(themeKey, value.value).apply() }

	private fun consoleNameKey(mac: MacAddress) = "console/${mac.value}/name"

	/** A name given to the console here, for example to tell two of them apart, instead of its own */
	fun consoleName(mac: MacAddress): String? = sharedPreferences.getString(consoleNameKey(mac), null)?.takeIf { it.isNotBlank() }

	fun setConsoleName(mac: MacAddress, name: String?) = sharedPreferences.edit().also {
		if(name.isNullOrBlank())
			it.remove(consoleNameKey(mac))
		else
			it.putString(consoleNameKey(mac), name.trim())
	}.apply()

	private fun consoleQualityKey(mac: MacAddress) = "console/${mac.value}/quality"

	/** The quality of a console, or null for the one in the settings */
	fun consoleQuality(mac: MacAddress) = Quality.fromValue(sharedPreferences.getString(consoleQualityKey(mac), null))

	fun setConsoleQuality(mac: MacAddress, quality: Quality?) = sharedPreferences.edit().also {
		if(quality == null)
			it.remove(consoleQualityKey(mac))
		else
			it.putString(consoleQualityKey(mac), quality.value)
	}.apply()

	/** @param away whether connecting from away from home, through PSN */
	/** The quality a stream to this console will use */
	fun effectiveQuality(mac: MacAddress?, away: Boolean): Quality =
		// Away from home an Ultra preset falls back to Sharp, which the away list doesn't offer it as
		(if(away) qualityAway else mac?.let { consoleQuality(it) } ?: quality).let { if(away && it.localOnly) Quality.SHARP else it }

	/** The codec to ask for: H.265 HDR becomes plain H.265 when this phone can't decode 10 bit video */
	val codecEffective get() = codec.codec.let {
		if(it == com.neganboy.playstream.lib.Codec.CODEC_H265_HDR && !isHdrDecodingSupported())
			com.neganboy.playstream.lib.Codec.CODEC_H265
		else
			it
	}

	/** Whether the chosen codec is H.265 HDR but the phone can't decode it, so the stream uses H.265 */
	val hdrFallbackActive get() = codec.codec == com.neganboy.playstream.lib.Codec.CODEC_H265_HDR && !isHdrDecodingSupported()

	fun videoProfile(mac: MacAddress?, ps5: Boolean, away: Boolean) =
		videoProfile(effectiveQuality(mac, away), ps5)

	fun videoProfile(quality: Quality, ps5: Boolean): ConnectVideoProfile = when(quality)
	{
		Quality.SMOOTH -> ConnectVideoProfile.preset(Resolution.RES_720P.preset, FPS.FPS_60.preset, codecEffective)
			.copy(bitrate = SMOOTH_BITRATE)
		Quality.BALANCED -> ConnectVideoProfile.preset(resolutionDefault.preset, FPS.FPS_60.preset, codecEffective)
		Quality.SHARP -> ConnectVideoProfile.preset(Resolution.RES_1080P.preset, FPS.FPS_60.preset, codecEffective)
			.copy(bitrate = if(ps5) SHARP_BITRATE_PS5 else SHARP_BITRATE_PS4)
		Quality.COMPETITIVE -> ConnectVideoProfile.preset(Resolution.RES_720P.preset, FPS.FPS_60.preset, com.neganboy.playstream.lib.Codec.CODEC_H264)
			.copy(bitrate = COMPETITIVE_BITRATE)
		Quality.STORY -> ConnectVideoProfile.preset(Resolution.RES_1080P.preset, FPS.FPS_60.preset, com.neganboy.playstream.lib.Codec.CODEC_H265)
			.copy(bitrate = if(ps5) SHARP_BITRATE_PS5 else SHARP_BITRATE_PS4)
		Quality.ULTRA_50 -> ConnectVideoProfile.preset(Resolution.RES_1080P.preset, FPS.FPS_60.preset, codecEffective)
			.copy(bitrate = ULTRA_50_BITRATE)
		Quality.ULTRA_100 -> ConnectVideoProfile.preset(Resolution.RES_1080P.preset, FPS.FPS_60.preset, codecEffective)
			.copy(bitrate = ULTRA_100_BITRATE)
		Quality.CUSTOM -> videoProfileDefaultBitrate.let {
			val bitrate = bitrate
			if(bitrate == null)
				it
			else
				it.copy(bitrate = bitrate)
		}
	}
}