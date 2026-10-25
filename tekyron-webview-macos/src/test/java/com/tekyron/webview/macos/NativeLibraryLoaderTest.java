
package com.tekyron.webview.macos;

import com.tekyron.webview.macos.internal.NativeLibraryLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class NativeLibraryLoaderTest {

    private NativeLibraryLoaderTest() {
    }

    public static void main(String[] args) throws Exception {
        Path first = NativeLibraryLoader.extract();
        Path second = NativeLibraryLoader.extract();

        if (!first.equals(second)) {
            throw new AssertionError("Library was extracted more than once");
        }

        if (!Files.isRegularFile(first)) {
            throw new AssertionError("Extracted native library does not exist");
        }

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = java.util.stream.IntStream.range(0, 100)
                    .<java.util.concurrent.Callable<Path>>mapToObj(
                            ignored -> NativeLibraryLoader::extract
                    )
                    .toList();

            for (var future : executor.invokeAll(tasks)) {
                if (!first.equals(future.get())) {
                    throw new AssertionError(
                            "Concurrent extraction returned a different path"
                    );
                }
            }
        }

        System.out.println("NativeLibraryLoaderTest PASSED");
        System.out.println("Extracted library: " + first);
    }
}
