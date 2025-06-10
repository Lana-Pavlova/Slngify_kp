package com.example.slngify_kp.viewmodel

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.os.Handler
import android.os.Looper


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeWebView(videoId: String, onError: (String) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                setLayerType(View.LAYER_TYPE_HARDWARE, null) // аппаратное ускорение

                webViewClient = object : WebViewClient() {
                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        val errorMessage = "Error loading video: ${error?.description} (Code: ${error?.errorCode})"
                        onError(errorMessage)
                    }
                }
                webChromeClient = WebChromeClient()
                Handler(Looper.getMainLooper()).postDelayed({
                    loadData(
                        """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <style>
                                body {
                                    margin: 0;
                                }
                                .video-container {
                                    position: relative;
                                    padding-bottom: 56.25%;
                                    padding-top: 30px;
                                    height: 0;
                                    overflow: hidden;
                                }
                                .video-container iframe,  
                                .video-container object,  
                                .video-container embed {
                                    position: absolute;
                                    top: 0;
                                    left: 0;
                                    width: 100%;
                                    height: 100%;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="video-container">
                                <iframe src="https://www.youtube.com/embed/$videoId" 
                                        frameborder="0" 
                                        allowfullscreen>
                                </iframe>
                            </div>
                        </body>
                        </html>
                        """,
                        "text/html",
                        "utf-8"
                    )
                }, 500) // Delay of 500 milliseconds
            }
        },
        update = { view ->
        },
        modifier = Modifier.fillMaxSize()
    )
}