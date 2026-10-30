package com.example

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        RandomSeekScreen(activity = this)
      }
    }
  }
}

class AndroidBridge(private val activity: Activity) {
  @JavascriptInterface
  fun keepScreenOn(enable: Boolean) {
    activity.runOnUiThread {
      if (enable) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RandomSeekScreen(activity: Activity) {
  val context = LocalContext.current
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
  var showInfoDialog by remember { mutableStateOf(false) }
  var pageProgress by remember { mutableFloatStateOf(0f) }
  var isLoading by remember { mutableStateOf(true) }

  // File Picker Launcher for HTML5 <input type="file">
  val fileChooserLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val callback = filePathCallback
    filePathCallback = null
    if (callback != null) {
      if (result.resultCode == Activity.RESULT_OK) {
        val data = result.data
        val results = WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
        if (results != null && results.isNotEmpty()) {
          callback.onReceiveValue(results)
        } else if (data?.data != null) {
          callback.onReceiveValue(arrayOf(data.data!!))
        } else {
          callback.onReceiveValue(null)
        }
      } else {
        callback.onReceiveValue(null)
      }
    }
  }

  // Handle hardware / gesture back button
  BackHandler(enabled = webViewRef?.canGoBack() == true) {
    webViewRef?.goBack()
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Случайная перемотка",
            style = MaterialTheme.typography.titleMedium
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface,
          actionIconContentColor = MaterialTheme.colorScheme.primary
        ),
        actions = {
          IconButton(
            onClick = {
              // Trigger built-in synthesizer demo groove
              webViewRef?.evaluateJavascript("if (typeof window.loadDemoAudio === 'function') { window.loadDemoAudio(); }", null)
            },
            modifier = Modifier.testTag("action_demo_audio")
          ) {
            Icon(
              imageVector = Icons.Default.MusicNote,
              contentDescription = "Демо трек"
            )
          }

          IconButton(
            onClick = {
              // Trigger file picker in HTML
              webViewRef?.evaluateJavascript("document.getElementById('file')?.click();", null)
            },
            modifier = Modifier.testTag("action_pick_file")
          ) {
            Icon(
              imageVector = Icons.Default.Audiotrack,
              contentDescription = "Выбрать аудиофайл"
            )
          }

          IconButton(
            onClick = {
              webViewRef?.reload()
            },
            modifier = Modifier.testTag("action_reload")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Перезагрузить"
            )
          }

          IconButton(
            onClick = { showInfoDialog = true },
            modifier = Modifier.testTag("action_info")
          ) {
            Icon(
              imageVector = Icons.Default.Info,
              contentDescription = "О приложении"
            )
          }
        },
        modifier = Modifier.testTag("top_app_bar")
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AndroidView(
        modifier = Modifier
          .fillMaxSize()
          .testTag("webview_container"),
        factory = { ctx ->
          val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
            .build()

          WebView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
              javaScriptEnabled = true
              domStorageEnabled = true
              databaseEnabled = true
              mediaPlaybackRequiresUserGesture = false
              allowFileAccess = true
              allowContentAccess = true
              useWideViewPort = true
              loadWithOverviewMode = true
              mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
              cacheMode = WebSettings.LOAD_DEFAULT
            }

            addJavascriptInterface(AndroidBridge(activity), "AndroidBridge")

            webViewClient = object : WebViewClient() {
              override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
              ): WebResourceResponse? {
                request?.url?.let { uri ->
                  val intercepted = assetLoader.shouldInterceptRequest(uri)
                  if (intercepted != null) return intercepted
                }
                return super.shouldInterceptRequest(view, request)
              }

              override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
              }
            }

            webChromeClient = object : WebChromeClient() {
              override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                pageProgress = newProgress / 100f
                if (newProgress >= 100) {
                  isLoading = false
                }
              }

              override fun onShowFileChooser(
                webView: WebView?,
                filePathCb: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
              ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = filePathCb

                val intent = try {
                  fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "audio/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                  }
                } catch (e: Exception) {
                  Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "audio/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                  }
                }

                try {
                  fileChooserLauncher.launch(intent)
                  return true
                } catch (e: Exception) {
                  filePathCallback?.onReceiveValue(null)
                  filePathCallback = null
                  return false
                }
              }
            }

            loadUrl("https://appassets.androidplatform.net/assets/index.html")
            webViewRef = this
          }
        },
        update = { webView ->
          webViewRef = webView
        }
      )

      AnimatedVisibility(
        visible = isLoading && pageProgress < 1f,
        modifier = Modifier.align(Alignment.TopCenter)
      ) {
        LinearProgressIndicator(
          progress = { pageProgress },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.destroy()
      webViewRef = null
    }
  }

  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = { showInfoDialog = false },
      title = { Text("О приложении") },
      text = {
        Text(
          "«Случайная перемотка» (Random Seek) — аудиопроигрыватель для нелинейного прослушивания музыки.\n\n" +
            "• Загрузите любой аудиофайл (mp3, wav, flac, ogg, m4a) или нажмите «Демо трек»\n" +
            "• Приложение будет само случайно перескакивать в другое место трека\n" +
            "• Настраивайте интервал прыжков, разброс, дальность и плавность стыков (crossfade)\n" +
            "• Поддерживается реверс (воспроизведение задом наперёд)\n" +
            "• Все файлы обрабатываются локально на вашем устройстве."
        )
      },
      confirmButton = {
        TextButton(onClick = { showInfoDialog = false }) {
          Text("Понятно")
        }
      }
    )
  }
}
