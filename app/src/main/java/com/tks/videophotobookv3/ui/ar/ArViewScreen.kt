package com.tks.videophotobookv3.ui.ar

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tks.videophotobookv3.R
import com.google.ar.core.*
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.VideoNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.math.Rotation
import com.tks.videophotobookv3.model.ArKeyPair
import com.tks.videophotobookv3.repository.KeyPairRepository
import com.tks.videophotobookv3.ui.main.loadBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// トラッキング状態とアンカー、プレイヤーを保持するクラス
private class ActiveVideo(
    val id: String,
    val anchor: Anchor,
    val mediaPlayer: MediaPlayer,
    val size: Size
)

@Composable
fun ArViewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val engine = rememberEngine()
    val materialLoader = rememberMaterialLoader(engine)
    val cameraStream = rememberARCameraStream(materialLoader = materialLoader)
    val repository = remember { KeyPairRepository(context.applicationContext) }
    val pairs = remember { repository.getPairs() }
    val bitmaps = remember { mutableStateMapOf<String, Bitmap>() }
    var isLoading by remember { mutableStateOf(true) }
    var isBitmapsLoaded by remember { mutableStateOf(false) }
    var arSession by remember { mutableStateOf<Session?>(null) }

    // カメラ権限の状態管理とリクエスト処理
    var hasCameraPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // トラッキング中のアクティブな動画セッションを管理するマップ (Composeが変更を検知できるようにする)
    val activeVideos = remember { mutableStateMapOf<String, ActiveVideo>() }

    // 画面破棄時にすべてのMediaPlayerリソースを確実に解放する
    DisposableEffect(Unit) {
        onDispose {
            activeVideos.forEach { (_, activeVideo) ->
                try {
                    if (activeVideo.mediaPlayer.isPlaying) {
                        activeVideo.mediaPlayer.stop()
                    }
                    activeVideo.mediaPlayer.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
            activeVideos.clear()
        }
    }

    // カメラ権限がなければ要求する
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // 起動時にすべてのマーカー画像を非同期で事前デコードする
    LaunchedEffect(pairs) {
        withContext(Dispatchers.IO) {
            for (pair in pairs) {
                val bitmap = loadBitmapFromUri(context, pair.markerUri)
                if (bitmap != null) {
                    bitmaps[pair.id] = bitmap
                }
            }
            isBitmapsLoaded = true
        }
    }

    // ARセッションが起動し、かつマーカー画像のロードが終わったら、バックグラウンドスレッドで画像データベースを構築・登録する
    LaunchedEffect(arSession, isBitmapsLoaded) {
        val session = arSession
        if (session != null && isBitmapsLoaded && isLoading) {
            withContext(Dispatchers.IO) {
                val database = AugmentedImageDatabase(session)
                for (pair in pairs) {
                    val bitmap = bitmaps[pair.id]
                    if (bitmap != null) {
                        database.addImage(pair.id, bitmap, pair.physicalWidth)
                    }
                }
                try {
                    val config = session.config
                    config.augmentedImageDatabase = database
                    config.focusMode = Config.FocusMode.AUTO
                    session.configure(config)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (!hasCameraPermission) {
            // カメラ権限がない場合のミニマルUI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.camera_permission_required),
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(stringResource(R.string.allow_camera_permission))
                }
            }
        } else {
            // ARSceneViewを全画面表示 (常に背面に配置)
            ARSceneView(
                modifier = Modifier.fillMaxSize(),
                engine = engine,
                cameraStream = cameraStream,
                sessionConfiguration = { session, config ->
                    arSession = session
                    // 初期構成（画像認識をスムーズにするためフォーカスモードをAUTOに設定）
                    config.focusMode = Config.FocusMode.AUTO
                },
                onSessionUpdated = { session, frame ->
                    val updatedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

                    for (image in updatedImages) {
                        val id = image.name
                        val pair = pairs.firstOrNull { it.id == id } ?: continue

                        when (image.trackingState) {
                            TrackingState.TRACKING -> {
                                val activeVideo = activeVideos[id]
                                if (activeVideo == null) {
                                    // 検出画像に対するARアンカーの生成
                                    val anchor = image.createAnchor(image.centerPose)

                                    // MediaPlayerの設定
                                    val mediaPlayer = MediaPlayer().apply {
                                        setDataSource(context, Uri.parse(pair.videoUri))
                                        isLooping = true
                                        // 音声ありで再生
                                        setVolume(1.0f, 1.0f)
                                        prepare()
                                    }

                                    // 動画のアスペクト比を計算し、マーカー画像の物理サイズに合わせたSizeを算出（アスペクト比を維持してフィット）
                                    val videoWidth = mediaPlayer.videoWidth.toFloat()
                                    val videoHeight = mediaPlayer.videoHeight.toFloat()
                                    val videoRatio = if (videoWidth > 0f && videoHeight > 0f) videoWidth / videoHeight else (image.extentX / image.extentZ)
                                    val imageRatio = image.extentX / image.extentZ

                                    val baseSize = if (videoRatio > imageRatio) {
                                        // 横長動画：マーカーの横幅に合わせ、縦幅をアスペクト比で縮小
                                        Size(image.extentX, image.extentX / videoRatio)
                                    } else {
                                        // 縦長動画：マーカーの縦幅に合わせ、横幅をアスペクト比で縮小
                                        Size(image.extentZ * videoRatio, image.extentZ)
                                    }

                                    // 拡大率（scaleFactor）を適用（古いデータで0またはマイナス値の場合は等倍1.0にする）
                                    val scale = if (pair.scaleFactor <= 0f) 1f else pair.scaleFactor
                                    val size = Size(baseSize.x * scale, baseSize.y * scale)

                                    // 描画開始
                                    mediaPlayer.start()

                                    // 管理マップへ保存 (Composeの状態が更新され、Recompositionが発生)
                                    activeVideos[id] = ActiveVideo(
                                        id = id,
                                        anchor = anchor,
                                        mediaPlayer = mediaPlayer,
                                        size = size
                                    )
                                } else {
                                    // 既に登録済みの場合は、一時停止されていたら再生再開
                                    if (!activeVideo.mediaPlayer.isPlaying) {
                                        activeVideo.mediaPlayer.start()
                                    }
                                }
                            }
                            TrackingState.PAUSED -> {
                                // 追跡が一時的に失われた場合は動画を一時停止
                                activeVideos[id]?.let {
                                    if (it.mediaPlayer.isPlaying) {
                                        it.mediaPlayer.pause()
                                    }
                                }
                            }
                            TrackingState.STOPPED -> {
                                // トラッキングが完全に停止した場合は解放
                                activeVideos[id]?.let {
                                    try {
                                        if (it.mediaPlayer.isPlaying) {
                                            it.mediaPlayer.stop()
                                        }
                                        it.mediaPlayer.release()
                                    } catch (e: Exception) {
                                        // Ignore
                                    }
                                    activeVideos.remove(id)
                                }
                            }
                        }
                    }
                }
            ) {
                // 宣言的にARノードを描画 (ComposeツリーとFilamentのシーンが同期される)
                activeVideos.values.forEach { activeVideo ->
                    AnchorNode(anchor = activeVideo.anchor) {
                        VideoNode(
                            player = activeVideo.mediaPlayer,
                            size = activeVideo.size,
                            rotation = Rotation(x = -90f, y = 0f, z = 0f)
                        )
                    }
                }
            }

            // ペアデータが登録されていない場合は画面中央にミニマルな警告メッセージを表示
            if (pairs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_pairs_ar),
                        color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // マーカー画像デコードおよびARCoreデータベース構築中のローディング表示
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading_markers),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 左上の戻るボタン (ミニマルデザイン)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .safeDrawingPadding()
                .padding(16.dp)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
    }
}
