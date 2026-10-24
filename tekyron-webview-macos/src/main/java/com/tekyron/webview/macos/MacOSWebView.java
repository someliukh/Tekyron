
package com.tekyron.webview.macos;

import com.tekyron.webview.WebView;
import com.tekyron.webview.macos.internal.WebViewNative;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.Objects;

public final class MacOSWebView implements WebView {

    private final WebViewNative nativeApi;
    private final MemorySegment handle;

    private boolean closed;

    public MacOSWebView(Path libraryPath) {
        this.nativeApi = new WebViewNative(libraryPath);
        this.handle = nativeApi.create();
        nativeApi.setSize(handle, 1000, 700);
    }

    @Override
    public void navigate(String url) {
        throw new UnsupportedOperationException(
                "Navigation is not implemented yet"
        );
    }

    @Override
    public void loadHtml(String html) {
        ensureOpen();
        nativeApi.setHtml(handle, Objects.requireNonNull(html));
    }

    @Override
    public void evaluateJavaScript(String script) {
        throw new UnsupportedOperationException(
                "JavaScript evaluation is not implemented yet"
        );
    }

    public void run() {
        ensureOpen();
        nativeApi.run(handle);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        nativeApi.destroy(handle);
        closed = true;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "WebView is already closed"
            );
        }
    }
}
