
package com.tekyron.webview.macos.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

public final class NativeLibraryLoader {

    private static final String LIBRARY_NAME = "libwebviewd.dylib";

    private static volatile Path extractedLibrary;

    private NativeLibraryLoader() {
    }

    public static Path extract() {
        Path library = extractedLibrary;

        if (library != null) {
            return library;
        }

        synchronized (NativeLibraryLoader.class) {
            library = extractedLibrary;

            if (library == null) {
                library = extractLibrary();
                extractedLibrary = library;
            }
        }

        return library;
    }

    private static Path extractLibrary() {
        String architecture = detectArchitecture();

        String resourcePath = "/native/macos/"
                + architecture + "/" + LIBRARY_NAME;

        try (InputStream input = NativeLibraryLoader.class
                .getResourceAsStream(resourcePath)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Native library resource not found: " + resourcePath
                );
            }

            Path directory = Files.createTempDirectory("tekyron-native-");
            Path libraryPath = directory.resolve(LIBRARY_NAME);

            try {
                Files.copy(input, libraryPath);
            } catch (IOException e) {
                Files.deleteIfExists(libraryPath);
                Files.deleteIfExists(directory);
                throw e;
            }

            libraryPath.toFile().deleteOnExit();
            directory.toFile().deleteOnExit();

            return libraryPath;

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to extract native library", e
            );
        }
    }

    private static String detectArchitecture() {
        String architecture = Objects.requireNonNull(
                System.getProperty("os.arch"),
                "os.arch"
        ).toLowerCase(Locale.ROOT);

        return switch (architecture) {
            case "aarch64", "arm64" -> "aarch64";
            case "x86_64", "amd64" -> "x86_64";
            default -> throw new UnsupportedOperationException(
                    "Unsupported macOS architecture: " + architecture
            );
        };
    }
}
