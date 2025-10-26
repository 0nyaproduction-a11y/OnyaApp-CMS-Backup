package com.onya.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.onya.app.ui.theme.*
import com.onya.app.ui.viewmodel.VerticalPlayerViewModel
import com.onya.app.utils.VideoPlayerManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerticalVideoPlayerScreen(
    seriesId: String,
    navController: NavController,
    viewModel: VerticalPlayerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val videoPlayerManager = remember { 
        VideoPlayerManager(context).apply {
            initializePlayer()
        }
    }
    
    // PlayerView reference for proper setup
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    // Load series when screen opens
    LaunchedEffect(seriesId) {
        android.util.Log.d("VerticalVideoPlayerScreen", "Loading series: $seriesId")
        android.util.Log.d("VerticalVideoPlayerScreen", "Screen opened successfully")
        viewModel.loadSeriesForPlayback(seriesId)
    }
    
    // Update player with current episode's video URL
    LaunchedEffect(uiState.currentEpisode) {
        uiState.currentEpisode?.let { episode ->
            android.util.Log.d("VerticalVideoPlayerScreen", "Current episode: ${episode.title}, Video URL: ${episode.videoUrl}")
            if (episode.videoUrl.isNotEmpty()) {
                android.util.Log.d("VerticalVideoPlayerScreen", "Loading video URL: ${episode.videoUrl}")
                try {
                    // Validate URL format
                    if (episode.videoUrl.startsWith("http")) {
                        videoPlayerManager.loadVideo(episode.videoUrl)
                        android.util.Log.d("VerticalVideoPlayerScreen", "Video loaded successfully")
                    } else {
                        android.util.Log.e("VerticalVideoPlayerScreen", "Invalid video URL format: ${episode.videoUrl}")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VerticalVideoPlayerScreen", "Error loading video: ${e.message}")
                    android.util.Log.e("VerticalVideoPlayerScreen", "Stack trace: ${e.stackTraceToString()}")
                }
            } else {
                android.util.Log.w("VerticalVideoPlayerScreen", "Episode has no video URL: ${episode.title}")
            }
        }
    }
    
    // Cleanup player when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            videoPlayerManager.releasePlayer()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player (Full Screen)
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = videoPlayerManager.exoPlayer
                    useController = false // We'll use custom controls
                    android.util.Log.d("VerticalVideoPlayerScreen", "PlayerView created with ExoPlayer: ${videoPlayerManager.exoPlayer}")
                    // Store reference for proper setup
                    playerView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                playerView.player = videoPlayerManager.exoPlayer
                android.util.Log.d("VerticalVideoPlayerScreen", "PlayerView updated with ExoPlayer: ${videoPlayerManager.exoPlayer}")
                // Properly setup the player view
                videoPlayerManager.setupPlayerView(playerView)
            }
        )
        
        // Top Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        // Top Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = { navController.navigateUp() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Episode Counter
            if (uiState.currentEpisode != null && uiState.allEpisodes.isNotEmpty()) {
                val currentIndex = uiState.allEpisodes.indexOfFirst { it.id == uiState.currentEpisode?.id } + 1
                Text(
                    text = "Episode $currentIndex of ${uiState.allEpisodes.size}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        
        // Bottom Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )
        
        // Episode Info (Bottom)
        val currentEpisode = uiState.currentEpisode
        val currentSeries = uiState.currentSeries
        if (currentEpisode != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth(0.7f)
            ) {
                Text(
                    text = currentEpisode.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                
                if (currentSeries != null) {
                    Text(
                        text = currentSeries.title,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Text(
                    text = currentEpisode.description,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        
                    // Center Play/Pause Button (when not playing)
                    if (!uiState.isPlaying && !uiState.isBuffering) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(80.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable {
                                    android.util.Log.d("VerticalVideoPlayerScreen", "Play button clicked")
                                    videoPlayerManager.play()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
        
        // Loading Indicator
        if (uiState.isBuffering) {
            Box(
                modifier = Modifier.align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = OnyaPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        // Error state - show when no real content is available
        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No Content Available",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.errorMessage ?: "This series has no episodes yet.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    TextButton(
                        onClick = { navController.navigateUp() },
                        colors = ButtonDefaults.textButtonColors(contentColor = OnyaPrimary)
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }
        
        // TODO: Add gesture detection, social actions, and bottom sheets
        // These will be implemented in the next phases
    }
}
