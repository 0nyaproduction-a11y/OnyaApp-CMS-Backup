package com.onya.app.utils

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VideoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer? get() = _exoPlayer
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()
    
    private val trackSelector = DefaultTrackSelector(context)
    
    fun initializePlayer() {
        if (_exoPlayer == null) {
            _exoPlayer = ExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .build()
                .apply {
                    addListener(playerListener)
                }
        }
    }
    
    fun setupPlayerView(playerView: PlayerView) {
        playerView.player = _exoPlayer
        playerView.useController = true
        playerView.controllerAutoShow = true
        playerView.controllerHideOnTouch = true
    }
    
    fun loadVideo(videoUrl: String, startPosition: Long = 0L) {
        android.util.Log.d("VideoPlayerManager", "Loading video: $videoUrl")
        android.util.Log.d("VideoPlayerManager", "ExoPlayer exists: ${_exoPlayer != null}")
        _exoPlayer?.let { player ->
            // Convert S3 URL to CloudFront URL if needed
            val optimizedUrl = optimizeVideoUrl(videoUrl)
            android.util.Log.d("VideoPlayerManager", "Optimized URL: $optimizedUrl")
            val mediaItem = MediaItem.fromUri(optimizedUrl)
            android.util.Log.d("VideoPlayerManager", "MediaItem created: ${mediaItem.mediaId}")
            player.setMediaItem(mediaItem)
            player.seekTo(startPosition)
            player.prepare()
            android.util.Log.d("VideoPlayerManager", "Video prepared successfully")
            // Auto-play the video
            player.play()
            android.util.Log.d("VideoPlayerManager", "Video playback started")
        } ?: android.util.Log.e("VideoPlayerManager", "ExoPlayer is null - cannot load video")
    }
    
    /**
     * Optimize video URL for better streaming performance
     * Converts S3 URLs to CloudFront CDN URLs when available
     */
    private fun optimizeVideoUrl(videoUrl: String): String {
        if (videoUrl.isBlank()) return videoUrl
        
        android.util.Log.d("VideoPlayerManager", "Original URL: $videoUrl")
        
        // Check if it's an S3 URL that can be optimized
        if (videoUrl.contains("onya-video-storage-mumbai.s3.ap-south-1.amazonaws.com")) {
            // For now, use the S3 URL directly since CloudFront is not configured yet
            android.util.Log.d("VideoPlayerManager", "Using S3 URL directly: $videoUrl")
            return videoUrl
        }
        
        // Check if it's already a CloudFront URL
        if (videoUrl.contains("cloudfront.net")) {
            android.util.Log.d("VideoPlayerManager", "Using CloudFront URL: $videoUrl")
            return videoUrl
        }
        
        // Return original URL if no optimization needed
        android.util.Log.d("VideoPlayerManager", "Using original URL: $videoUrl")
        return videoUrl
    }
    
    /**
     * Get video quality options based on URL
     * For CloudFront URLs, we can potentially request different qualities
     */
    fun getAvailableQualities(videoUrl: String): List<VideoQualityOption> {
        val optimizedUrl = optimizeVideoUrl(videoUrl)
        
        return if (optimizedUrl.contains("cloudfront.net")) {
            // CloudFront supports adaptive bitrate streaming
            listOf(
                VideoQualityOption.AUTO,
                VideoQualityOption.LOW,
                VideoQualityOption.MEDIUM,
                VideoQualityOption.HD,
                VideoQualityOption.FULL_HD
            )
        } else {
            // For direct S3 URLs, limited quality options
            listOf(
                VideoQualityOption.AUTO,
                VideoQualityOption.MEDIUM,
                VideoQualityOption.HD
            )
        }
    }
    
    /**
     * Set video quality for CloudFront URLs
     */
    fun setVideoQuality(quality: VideoQualityOption) {
        _exoPlayer?.let { player ->
            when (quality) {
                VideoQualityOption.AUTO -> {
                    // Let ExoPlayer automatically select the best quality
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSizeSd()
                    )
                }
                VideoQualityOption.LOW -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(854, 480)
                    )
                }
                VideoQualityOption.MEDIUM -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(1280, 720)
                    )
                }
                VideoQualityOption.HD -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(1920, 1080)
                    )
                }
                VideoQualityOption.FULL_HD -> {
                    trackSelector.setParameters(
                        trackSelector.buildUponParameters()
                            .setMaxVideoSize(2560, 1440)
                    )
                }
            }
        }
    }
    
    fun play() {
        _exoPlayer?.play()
    }
    
    fun pause() {
        _exoPlayer?.pause()
    }
    
    fun seekTo(position: Long) {
        _exoPlayer?.seekTo(position)
    }
    
    fun setPlaybackSpeed(speed: Float) {
        _exoPlayer?.setPlaybackSpeed(speed)
    }
    
    fun getCurrentPosition(): Long {
        return _exoPlayer?.currentPosition ?: 0L
    }
    
    fun getDuration(): Long {
        return _exoPlayer?.duration ?: 0L
    }
    
    fun getProgress(): Float {
        val duration = getDuration()
        return if (duration > 0) {
            getCurrentPosition().toFloat() / duration.toFloat()
        } else {
            0f
        }
    }
    
    fun releasePlayer() {
        _exoPlayer?.removeListener(playerListener)
        _exoPlayer?.release()
        _exoPlayer = null
    }
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = _playbackState.value.copy(
                state = when (playbackState) {
                    Player.STATE_IDLE -> PlayerState.IDLE
                    Player.STATE_BUFFERING -> PlayerState.BUFFERING
                    Player.STATE_READY -> PlayerState.READY
                    Player.STATE_ENDED -> PlayerState.ENDED
                    else -> PlayerState.IDLE
                }
            )
            
            _isBuffering.value = playbackState == Player.STATE_BUFFERING
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
        
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updateProgress()
        }
        
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            updateProgress()
        }
    }
    
    private fun updateProgress() {
        _exoPlayer?.let { player ->
            _currentPosition.value = player.currentPosition
            _duration.value = player.duration
        }
    }
}

data class PlaybackState(
    val state: PlayerState = PlayerState.IDLE,
    val error: String? = null
)

enum class PlayerState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR
}

// Video Quality Selection
enum class VideoQualityOption(val displayName: String, val height: Int) {
    AUTO("Auto", -1),
    LOW("480p", 480),
    MEDIUM("720p", 720),
    HD("1080p", 1080),
    FULL_HD("1440p", 1440)
}

// Playback Speed Options
enum class PlaybackSpeedOption(val displayName: String, val speed: Float) {
    SLOW("0.5x", 0.5f),
    NORMAL("1x", 1.0f),
    FAST("1.25x", 1.25f),
    FASTER("1.5x", 1.5f),
    FASTEST("2x", 2.0f)
}
