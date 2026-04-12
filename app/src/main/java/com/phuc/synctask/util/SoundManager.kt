package com.phuc.synctask.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.phuc.synctask.R

/**
 * Quản lý SFX toàn app bằng SoundPool (độ trễ thấp, phù hợp UI interaction).
 * Khởi tạo một lần trong MainActivity, release() khi onDestroy.
 */
class SoundManager(context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(5)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var clickSoundId: Int = 0
    private var fireworkSoundId: Int = 0
    private var achievementSoundId: Int = 0

    // Track xem sound đã load xong chưa để tránh phát khi chưa sẵn sàng
    private val loadedSounds = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSounds.add(sampleId)
        }
        clickSoundId       = soundPool.load(context, R.raw.click_sound, 1)
        fireworkSoundId    = soundPool.load(context, R.raw.firework_sound, 1)
        achievementSoundId = soundPool.load(context, R.raw.achievement_sound, 1)
    }

    fun playClick() = play(clickSoundId, volume = 0.6f)

    fun playFireworks() = play(fireworkSoundId, volume = 1.0f)

    fun playAchievement() = play(achievementSoundId, volume = 1.0f)

    private fun play(soundId: Int, volume: Float = 1.0f) {
        if (soundId != 0 && soundId in loadedSounds) {
            soundPool.play(soundId, volume, volume, 1, 0, 1.0f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
