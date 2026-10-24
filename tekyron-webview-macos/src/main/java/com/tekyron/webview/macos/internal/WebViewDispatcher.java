
package com.tekyron.webview.macos.internal;

import java.lang.foreign.MemorySegment;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class WebViewDispatcher {

    private final WebViewNative nativeApi;
    private final MemorySegment handle;

    private final Object lifecycleLock = new Object();
    private final Set<Long> pending = new HashSet<>();

    private boolean accepting = true;

    public WebViewDispatcher(
            WebViewNative nativeApi,
            MemorySegment handle
    ) {
        this.nativeApi = Objects.requireNonNull(nativeApi);
        this.handle = Objects.requireNonNull(handle);
    }

    public void dispatch(Runnable task) {
        Objects.requireNonNull(task);

        synchronized (lifecycleLock) {
            if (!accepting) {
                throw new IllegalStateException(
                        "WebView dispatcher is closed"
                );
            }

            final long[] callbackId = new long[1];

            Runnable wrappedTask = () -> {
                synchronized (lifecycleLock) {
                    pending.remove(callbackId[0]);

                    if (!accepting) {
                        return;
                    }
                }

                try {
                    task.run();
                } catch (Throwable e) {
                    System.err.println(
                            "Dispatched UI task failed:"
                    );
                    e.printStackTrace();
                }
            };

            long id = nativeApi.registerCallback(wrappedTask);
            callbackId[0] = id;

            pending.add(id);

            try {
                nativeApi.dispatch(handle, id);

            } catch (RuntimeException e) {
                pending.remove(id);
                nativeApi.unregisterCallback(id);
                throw e;
            }
        }
    }

    public void close() {
        synchronized (lifecycleLock) {
            if (!accepting) {
                return;
            }

            accepting = false;

            for (long id : pending) {
                nativeApi.unregisterCallback(id);
            }

            pending.clear();
        }
    }
}
