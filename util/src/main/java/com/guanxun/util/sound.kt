package com.guanxun.util

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import androidx.annotation.RawRes

/**
 * SoundPool 辅助类
 */
class SoundDriver {
    private var mSoundPool: SoundPool = SoundPool.Builder().setMaxStreams(5).build()
    private var mSoundId = 0
    /**
     * 获取音量
     *
     * @return 音频播放音量 range 0.0-1.0
     */

    /**
     * 设置音量
     * 音频播放音量 range 0.0-1.0
     */
    var playVolume = 0f

    /**
     * 加载音频资源
     *
     * @param context
     * 上下文
     * @param resId
     * 音频资源 [RawRes]
     */
    fun load(context: Context, @RawRes resId: Int) {
        mSoundId = mSoundPool.load(context, resId, 1)

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // 获取系统媒体当前音量
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        // 获取系统媒体最大音量
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // 设置播放音量
        playVolume = currentVolume * 1.0f / maxVolume
    }

    fun unload() {
        mSoundId = 0
    }

    var streamId: Int = 0

    /**
     * 播放声音效果
     */
    fun playSound() {
        if (mSoundId != 0) {
            if (streamId > 0) {
                mSoundPool.stop(streamId)
            }
            streamId = mSoundPool.play(mSoundId, playVolume, playVolume, 1, 0, 2f)
        }

    }

    /**
     * 释放SoundPool
     */
    fun release() {
        mSoundPool.release()
    }
}