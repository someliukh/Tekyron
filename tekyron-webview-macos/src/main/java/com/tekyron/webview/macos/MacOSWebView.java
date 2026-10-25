
package com.tekyron.webview.macos;

import com.tekyron.webview.WebView;
import com.tekyron.webview.macos.internal.WebViewDispatcher;
import com.tekyron.webview.macos.internal.WebViewNative;
import com.tekyron.webview.macos.internal.WebViewState;

import java.lang.foreign.MemorySegment;
import java.nio.file.Path;
import java.util.Objects;

public final class MacOSWebView implements WebView {

    private final WebViewNative nativeApi;
    private final MemorySegment handle;
    private final WebViewDispatcher dispatcher;
    private final Thread ownerThread;

    private WebViewState state = WebViewState.CREATED;

    public MacOSWebView(Path libraryPath) {
        ownerThread = Thread.currentThread();

        nativeApi = new WebViewNative(libraryPath);
        handle = nativeApi.create();

        nativeApi.setSize(handle, 1000, 700);

        dispatcher = new WebViewDispatcher(nativeApi, handle);
    }

    @Override
    public void loadHtml(String html) {
        ensureOwnerThread();
        ensureOpen();

        nativeApi.setHtml(
                handle,
                Objects.requireNonNull(html)
        );
    }

    @Override
    public void navigate(String url) {
        ensureOwnerThread();
        ensureOpen();

        nativeApi.navigate(
                handle,
                Objects.requireNonNull(url)
        );
    }

    @Override
    public void evaluateJavaScript(String script) {
        ensureOwnerThread();
        ensureOpen();

        throw new UnsupportedOperationException(
                "JavaScript evaluation is not implemented yet"
        );
    }

    public void dispatch(Runnable task) {
        dispatcher.dispatch(task);
    }

    @Override
    public void run() {
        ensureOwnerThread();

        if (state != WebViewState.CREATED) {
            throw new IllegalStateException(
                    "Cannot run WebView in state: " + state
            );
        }

        state = WebViewState.RUNNING;

        try {
            nativeApi.run(handle);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        ensureOwnerThread();

        if (state == WebViewState.CLOSED) {
            return;
        }

        dispatcher.close();

        try {
            nativeApi.destroy(handle);
        } finally {
            state = WebViewState.CLOSED;
        }
    }

    private void ensureOpen() {
        if (state == WebViewState.CLOSED) {
            throw new IllegalStateException(
                    "WebView is already closed"
            );
        }
    }

    private void ensureOwnerThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException(
                    "WebView must be accessed from its owner thread"
            );
        }
    }
}
