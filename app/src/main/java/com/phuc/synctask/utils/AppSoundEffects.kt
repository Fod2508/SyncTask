package com.phuc.synctask.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.phuc.synctask.R

enum class AppSoundEffect {
    ACHIEVEMENT_UNLOCKED,
    TASK_CREATED,
    TASK_DELETED,
    TASK_RESTORED,
    TASK_COMPLETED_ON_TIME,
    TASK_COMPLETED_LATE,
    TASK_ASSIGNED,
    AUTH_SUCCESS,
    ERROR,
    NOTIFICATION
}

object AppSoundPlayer {
    @Volatile
    private var enabled: Boolean = true

    @Volatile
    private var volumePercent: Int = 80

    private var soundPool: SoundPool? = null
    private var mediaPlayer: MediaPlayer? = null
    private val soundMap = mutableMapOf<AppSoundEffect, Int>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.let { pool ->
            soundMap[AppSoundEffect.ACHIEVEMENT_UNLOCKED] = pool.load(context, R.raw.yippee, 1)
            soundMap[AppSoundEffect.TASK_CREATED] = pool.load(context, R.raw.pop, 1)
            soundMap[AppSoundEffect.TASK_ASSIGNED] = pool.load(context, R.raw.pop, 1)
            soundMap[AppSoundEffect.TASK_RESTORED] = pool.load(context, R.raw.pop, 1)
            soundMap[AppSoundEffect.TASK_DELETED] = pool.load(context, R.raw.vine_boom, 1)
            soundMap[AppSoundEffect.TASK_COMPLETED_ON_TIME] = pool.load(context, R.raw.wow, 1)
            soundMap[AppSoundEffect.TASK_COMPLETED_LATE] = pool.load(context, R.raw.wasted, 1)
            soundMap[AppSoundEffect.AUTH_SUCCESS] = pool.load(context, R.raw.ding, 1)
            soundMap[AppSoundEffect.ERROR] = pool.load(context, R.raw.vine_boom, 1)
            soundMap[AppSoundEffect.NOTIFICATION] = pool.load(context, R.raw.ding, 1)
        }

        // Initialize Background Music
        try {
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.bgm_lofi).apply {
                isLooping = true
            }
            updateBgmVolume()
            if (enabled) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        isInitialized = true
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        if (enabled) {
            mediaPlayer?.start()
        } else {
            mediaPlayer?.pause()
        }
    }

    fun setVolumePercent(value: Int) {
        val next = value.coerceIn(0, 100)
        if (next == volumePercent) return
        volumePercent = next
        updateBgmVolume()
    }

    private fun updateBgmVolume() {
        // BGM should be slightly quieter than SFX
        val actualVol = (volumePercent / 100f) * 0.4f
        mediaPlayer?.setVolume(actualVol, actualVol)
    }

    fun play(effect: AppSoundEffect) {
        if (!enabled) return
        val soundId = soundMap[effect] ?: return
        val actualVol = volumePercent / 100f
        soundPool?.play(soundId, actualVol, actualVol, 1, 0, 1f)
    }

    fun pauseBgm() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }
    }

    fun resumeBgm() {
        if (enabled && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }
}
