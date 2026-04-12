package com.phuc.synctask.util

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal cung cấp SoundManager cho toàn bộ cây Compose.
 * Mặc định null — sẽ được provide từ MainActivity.
 */
val LocalSoundManager = staticCompositionLocalOf<SoundManager?> { null }
