
package com.tekyron.webview.macos.internal;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public final class WebViewNative {

    private static final int WEBVIEW_ERROR_OK = 0;

    private static final AtomicLong NEXT_CALLBACK_ID =
            new AtomicLong();

    private static final ConcurrentMap<Long, Runnable> CALLBACKS =
            new ConcurrentHashMap<>();

    private static final MemorySegment CALLBACK_STUB =
            createCallbackStub();

    private final MethodHandle create;
    private final MethodHandle run;
    private final MethodHandle destroy;
    private final MethodHandle setHtml;
    private final MethodHandle setSize;
    private final MethodHandle dispatch;

    public WebViewNative(Path libraryPath) {
        Linker linker = Linker.nativeLinker();

        SymbolLookup lookup = SymbolLookup.libraryLookup(
                libraryPath,
                Arena.global()
        );

        create = linker.downcallHandle(
                lookup.find("webview_create").orElseThrow(),
                FunctionDescriptor.of(
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS
                )
        );

        run = linker.downcallHandle(
                lookup.find("webview_run").orElseThrow(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS
                )
        );

        destroy = linker.downcallHandle(
                lookup.find("webview_destroy").orElseThrow(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS
                )
        );

        setHtml = linker.downcallHandle(
                lookup.find("webview_set_html").orElseThrow(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS
                )
        );

        setSize = linker.downcallHandle(
                lookup.find("webview_set_size").orElseThrow(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT,
                        ValueLayout.JAVA_INT
                )
        );

        dispatch = linker.downcallHandle(
                lookup.find("webview_dispatch").orElseThrow(),
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS
                )
        );
    }

    private static MemorySegment createCallbackStub() {
        try {
            MethodHandle callback = MethodHandles.lookup()
                    .findStatic(
                            WebViewNative.class,
                            "dispatchCallback",
                            MethodType.methodType(
                                    void.class,
                                    MemorySegment.class,
                                    MemorySegment.class
                            )
                    );

            return Linker.nativeLinker().upcallStub(
                    callback,
                    FunctionDescriptor.ofVoid(
                            ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS
                    ),
                    Arena.global()
            );

        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void dispatchCallback(
            MemorySegment webview,
            MemorySegment argument
    ) {
        try {
            long id = argument.address();

            Runnable callback = CALLBACKS.remove(id);

            if (callback != null) {
                callback.run();
            }

        } catch (Throwable e) {
            System.err.println("Native callback failed:");
            e.printStackTrace();
        }
    }

    public MemorySegment create() {
        try {
            MemorySegment handle = (MemorySegment) create.invokeExact(
                    0,
                    MemorySegment.NULL
            );

            if (handle.address() == 0) {
                throw new IllegalStateException(
                        "webview_create returned NULL"
                );
            }

            return handle;

        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to create WebView",
                    e
            );
        }
    }

    public void run(MemorySegment handle) {
        try {
            int result = (int) run.invokeExact(handle);
            checkResult("webview_run", result);

        } catch (Throwable e) {
            throw nativeFailure("webview_run", e);
        }
    }

    public void destroy(MemorySegment handle) {
        try {
            int result = (int) destroy.invokeExact(handle);
            checkResult("webview_destroy", result);

        } catch (Throwable e) {
            throw nativeFailure("webview_destroy", e);
        }
    }

    public void setHtml(MemorySegment handle, String html) {
        Objects.requireNonNull(html);

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nativeHtml = arena.allocateFrom(html);

            int result = (int) setHtml.invokeExact(
                    handle,
                    nativeHtml
            );

            checkResult("webview_set_html", result);

        } catch (Throwable e) {
            throw nativeFailure("webview_set_html", e);
        }
    }

    public void setSize(
            MemorySegment handle,
            int width,
            int height
    ) {
        try {
            int result = (int) setSize.invokeExact(
                    handle,
                    width,
                    height,
                    0
            );

            checkResult("webview_set_size", result);

        } catch (Throwable e) {
            throw nativeFailure("webview_set_size", e);
        }
    }

    public long registerCallback(Runnable callback) {
        Objects.requireNonNull(callback);

        long id = NEXT_CALLBACK_ID.incrementAndGet();

        if (id <= 0) {
            throw new IllegalStateException(
                    "Callback ID overflow"
            );
        }

        CALLBACKS.put(id, callback);
        return id;
    }

    public void unregisterCallback(long id) {
        CALLBACKS.remove(id);
    }

    public void dispatch(MemorySegment handle, long callbackId) {
        if (callbackId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid callback ID"
            );
        }

        try {
            int result = (int) dispatch.invokeExact(
                    handle,
                    CALLBACK_STUB,
                    MemorySegment.ofAddress(callbackId)
            );

            checkResult("webview_dispatch", result);

        } catch (Throwable e) {
            throw nativeFailure("webview_dispatch", e);
        }
    }

    private static void checkResult(String operation, int result) {
        if (result != WEBVIEW_ERROR_OK) {
            throw new IllegalStateException(
                    operation + " failed with error code: " + result
            );
        }
    }

    private static IllegalStateException nativeFailure(
            String operation,
            Throwable cause
    ) {
        if (cause instanceof IllegalStateException exception) {
            return exception;
        }

        return new IllegalStateException(
                operation + " failed",
                cause
        );
    }
}
