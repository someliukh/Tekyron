
package com.tekyron.webview.macos;

import java.nio.file.Path;

public final class MacOSWebViewNavigationTest {

    public static void main(String[] args) {

        Path library = Path.of(
                "tekyron-webview-macos/build/native/core/libwebviewd.dylib"
        ).toAbsolutePath();

        try (MacOSWebView webView = new MacOSWebView(library)) {

            webView.navigate("https://example.com");

            webView.run();
        }
    }
}
