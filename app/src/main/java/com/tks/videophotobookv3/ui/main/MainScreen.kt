package com.tks.videophotobookv3.ui.main

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.tks.videophotobookv3.ArView
import com.tks.videophotobookv3.R
import com.tks.videophotobookv3.model.ArKeyPair
import com.tks.videophotobookv3.repository.KeyPairRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: MainScreenViewModel = viewModel {
        MainScreenViewModel(KeyPairRepository(context))
    }
    val keyPairs by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPair by remember { mutableStateOf<ArKeyPair?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "VideoPhotoBook v3",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Light,
                            color = Color.Black
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color.Black,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Pair")
            }
        },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            if (keyPairs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_pairs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(keyPairs) { pair ->
                            PairItemRow(
                                pair = pair,
                                onEdit = { editingPair = pair },
                                onDelete = { viewModel.deletePair(pair.id) }
                            )
                        }
                    }

                    // AR起動ボタン (ミニマルデザイン)
                    Button(
                        onClick = { onItemClick(ArView) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ar_launch),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddKeyPairDialog(
                onDismiss = { showAddDialog = false },
                onSave = { markerUri, videoUri, width, scale ->
                    viewModel.addPair(markerUri, videoUri, width, scale)
                    showAddDialog = false
                }
            )
        }

        if (editingPair != null) {
            EditKeyPairDialog(
                pair = editingPair!!,
                onDismiss = { editingPair = null },
                onSave = { markerUri, videoUri, width, scale ->
                    viewModel.updatePair(editingPair!!.id, markerUri, videoUri, width, scale)
                    editingPair = null
                }
            )
        }
    }
}

@Composable
fun PairItemRow(
    pair: ArKeyPair,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val markerName = remember(pair.markerUri) { getFileName(context, Uri.parse(pair.markerUri)) }
    val videoName = remember(pair.videoUri) { getFileName(context, Uri.parse(pair.videoUri)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .clickable { onEdit() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // マーカー画像プレビュー
        UriPreviewImage(
            uriString = pair.markerUri,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFAFAFA))
        )

        Spacer(modifier = Modifier.width(16.dp))

        // ファイル情報
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.image_label, markerName),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.video_label, videoName),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.info_label, pair.physicalWidth.toString(), pair.scaleFactor.toString()),
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // 削除ボタン
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFE57373)
            )
        }
    }
}

@Composable
fun UriPreviewImage(
    uriString: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uriString) {
        withContext(Dispatchers.IO) {
            bitmap = loadBitmapFromUri(context, uriString)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Preview",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("...", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddKeyPairDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Float, Float) -> Unit
) {
    val context = LocalContext.current
    var markerUri by remember { mutableStateOf<Uri?>(null) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var physicalWidthStr by remember { mutableStateOf("0.1") }
    var scaleFactorStr by remember { mutableStateOf("1.0") }

    val markerName = markerUri?.let { getFileName(context, it) } ?: stringResource(R.string.tap_to_select)
    val videoName = videoUri?.let { getFileName(context, it) } ?: stringResource(R.string.select_video_btn)

    // 画像ピッカーのランチャー
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            persistUriAccess(context, uri)
            markerUri = uri
        }
    }

    // 動画ピッカーのランチャー
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            persistUriAccess(context, uri)
            videoUri = uri
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.add_pair_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // マーカー画像選択
                Text(stringResource(R.string.step_image), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFAFAFA))
                        .clickable {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (markerUri != null) {
                        UriPreviewImage(
                            uriString = markerUri.toString(),
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.tap_to_change), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text(stringResource(R.string.tap_to_select), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    text = markerName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    maxLines = 1
                )

                // 動画選択
                Text(stringResource(R.string.step_video), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        pickVideoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text(stringResource(R.string.select_video_btn), fontWeight = FontWeight.Normal)
                }
                Text(
                    text = videoName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    maxLines = 1
                )

                // 物理サイズ入力
                Text(stringResource(R.string.step_width), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = physicalWidthStr,
                    onValueChange = { physicalWidthStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.width_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                // 拡大率入力
                Text(stringResource(R.string.step_scale), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = scaleFactorStr,
                    onValueChange = { scaleFactorStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.scale_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
                )

                // アクションボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val width = physicalWidthStr.toFloatOrNull() ?: 0.1f
                            val scale = scaleFactorStr.toFloatOrNull() ?: 1.0f
                            onSave(markerUri.toString(), videoUri.toString(), width, scale)
                        },
                        enabled = markerUri != null && videoUri != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditKeyPairDialog(
    pair: ArKeyPair,
    onDismiss: () -> Unit,
    onSave: (String, String, Float, Float) -> Unit
) {
    val context = LocalContext.current
    var markerUri by remember { mutableStateOf<Uri?>(Uri.parse(pair.markerUri)) }
    var videoUri by remember { mutableStateOf<Uri?>(Uri.parse(pair.videoUri)) }
    var physicalWidthStr by remember { mutableStateOf(pair.physicalWidth.toString()) }
    var scaleFactorStr by remember { mutableStateOf(pair.scaleFactor.toString()) }

    val markerName = markerUri?.let { getFileName(context, it) } ?: stringResource(R.string.tap_to_select)
    val videoName = videoUri?.let { getFileName(context, it) } ?: stringResource(R.string.select_video_btn)

    // 画像ピッカーのランチャー
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            persistUriAccess(context, uri)
            markerUri = uri
        }
    }

    // 動画ピッカーのランチャー
    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            persistUriAccess(context, uri)
            videoUri = uri
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.edit_pair_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // マーカー画像選択
                Text(stringResource(R.string.step_image_change), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFAFAFA))
                        .clickable {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (markerUri != null) {
                        UriPreviewImage(
                            uriString = markerUri.toString(),
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.tap_to_change), color = Color.White, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text(stringResource(R.string.tap_to_select), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    text = markerName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    maxLines = 1
                )

                // 動画選択
                Text(stringResource(R.string.step_video_change), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedButton(
                    onClick = {
                        pickVideoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text(stringResource(R.string.change_video_btn), fontWeight = FontWeight.Normal)
                }
                Text(
                    text = videoName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    maxLines = 1
                )

                // 物理サイズ入力
                Text(stringResource(R.string.step_width), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = physicalWidthStr,
                    onValueChange = { physicalWidthStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.width_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
                )

                // 拡大率入力
                Text(stringResource(R.string.step_scale), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = scaleFactorStr,
                    onValueChange = { scaleFactorStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                Text(
                    text = stringResource(R.string.scale_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 2.dp, bottom = 24.dp)
                )

                // アクションボタン
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val width = physicalWidthStr.toFloatOrNull() ?: pair.physicalWidth
                            val scale = scaleFactorStr.toFloatOrNull() ?: pair.scaleFactor
                            onSave(markerUri.toString(), videoUri.toString(), width, scale)
                        },
                        enabled = markerUri != null && videoUri != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

// ヘルパー：URIの永続権限を取得
private fun persistUriAccess(context: Context, uri: Uri) {
    try {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.contentResolver.takePersistableUriPermission(uri, takeFlags)
    } catch (e: SecurityException) {
        // ログ出力など。Photo Pickerを利用する場合は自動的にパーミッションが永続化されない場合もあるが、SAFからの移行用としてトライする。
    }
}

// ヘルパー：URIからファイル名を取得
private fun getFileName(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "不明なファイル"
}

// ヘルパー：URIからBitmapをデコード
fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
    return try {
        val uri = Uri.parse(uriString)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: Exception) {
        null
    }
}
