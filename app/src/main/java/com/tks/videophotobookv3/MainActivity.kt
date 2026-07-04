package com.tks.videophotobookv3

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.ar.core.ArCoreApk
import com.tks.videophotobookv3.theme.VideoPhotoBookv3Theme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // ARCoreのサポートチェック
    val availability = ArCoreApk.getInstance().checkAvailability(this)
    if (!availability.isTransient() && !availability.isSupported()) {
      Toast.makeText(this, "このデバイスはARCoreに対応していない可能性があります。", Toast.LENGTH_LONG).show()
    }

    enableEdgeToEdge()
    setContent {
      VideoPhotoBookv3Theme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
