
package com.tekyron.webview.macos;

import com.tekyron.webview.Platform;
import com.tekyron.webview.WebView;
import com.tekyron.webview.WebViewProvider;
import com.tekyron.webview.macos.internal.NativeLibraryLoader;

import java.nio.file.Path;

public final class MacOSWebViewProvider implements WebViewProvider {

    @Override
    public Platform platform() {
        return Platform.MACOS;
    }

    @Override
    public WebView create() {
        Path libraryPath = NativeLibraryLoader.extract();
        return new MacOSWebView(libraryPath);
    }
}
