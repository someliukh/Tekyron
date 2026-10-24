
package com.tekyron.webview.macos.internal;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.Objects;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public final class WebViewNative {

    private final MethodHandle create;
    private final MethodHandle run;
    private final MethodHandle setHtml;
    private final MethodHandle destroy;
    private final MethodHandle setSize;

    public WebViewNative(Path libraryPath) {
        Objects.requireNonNull(libraryPath, "libraryPath");

        Linker linker = Linker.nativeLinker();

        SymbolLookup lookup = SymbolLookup.libraryLookup(
                libraryPath.toAbsolutePath(),
                Arena.global()
        );

        create = linker.downcallHandle(
                lookup.findOrThrow("webview_create"),
                FunctionDescriptor.of(
                        ADDRESS,
                        JAVA_INT,
                        ADDRESS
                )
        );

        run = linker.downcallHandle(
                lookup.findOrThrow("webview_run"),
                FunctionDescriptor.ofVoid(ADDRESS)
        );

        setHtml = linker.downcallHandle(
                lookup.findOrThrow("webview_set_html"),
                FunctionDescriptor.ofVoid(
                        ADDRESS,
                        ADDRESS
                )
        );

        destroy = linker.downcallHandle(
                lookup.findOrThrow("webview_destroy"),
                FunctionDescriptor.ofVoid(ADDRESS)
        );

        setSize = linker.downcallHandle(
                lookup.findOrThrow("webview_set_size"),
                FunctionDescriptor.ofVoid(
                        ADDRESS,
                        JAVA_INT,
                        JAVA_INT,
                        JAVA_INT
                )
        );
    }

    public MemorySegment create() {
        try {
            MemorySegment window = (MemorySegment) create.invokeExact(
                    0,
                    MemorySegment.NULL
            );

            if (window.equals(MemorySegment.NULL)) {
                throw new IllegalStateException(
                        "Native webview_create returned NULL"
                );
            }

            return window;
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to create native WebView", e
            );
        }
    }

    public void run(MemorySegment window) {
        try {
            run.invokeExact(window);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to run native WebView", e
            );
        }
    }

    public void setHtml(MemorySegment window, String html) {
        Objects.requireNonNull(html, "html");

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment nativeHtml = arena.allocateFrom(html);
            setHtml.invokeExact(window, nativeHtml);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to set WebView HTML", e
            );
        }
    }

    public void destroy(MemorySegment window) {
        try {
            destroy.invokeExact(window);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to destroy native WebView", e
            );
        }
    }

    public void setSize(MemorySegment window, int width, int height) {
        try {
            setSize.invokeExact(window, width, height, 0);
        } catch (Throwable e) {
            throw new IllegalStateException(
                    "Failed to set WebView size", e
            );
        }
    }
}
