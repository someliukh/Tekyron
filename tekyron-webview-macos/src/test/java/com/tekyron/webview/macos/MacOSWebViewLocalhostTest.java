
package com.tekyron.webview.macos;

import java.nio.file.Path;

public final class MacOSWebViewLocalhostTest {

    public static void main(String[] args) {

        Path library = Path.of(
                "tekyron-webview-macos/build/native/core/libwebviewd.dylib"
        ).toAbsolutePath();

        try (MacOSWebView webView = new MacOSWebView(library)) {

            webView.navigate("http://localhost:5173");

            webView.run();
        }
    }
}
