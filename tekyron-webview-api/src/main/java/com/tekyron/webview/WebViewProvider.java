package com.tekyron.webview;

public interface WebViewProvider {

    Platform platform();

    WebView create();
}