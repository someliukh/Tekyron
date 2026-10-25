package com.tekyron.webview;

import java.util.Locale;

public final class PlatformDetector {

    private PlatformDetector() {
    }

    public static Platform detect() {
        return detect(System.getProperty("os.name"));
    }

    public static Platform detect(String osName) {
        if (osName == null || osName.isBlank()) {
            throw new IllegalStateException("Operating system name is missing");
        }

        String normalized = osName.toLowerCase(Locale.ROOT);

        if (normalized.contains("mac") || normalized.contains("darwin")) {
            return Platform.MACOS;
        }

        if (normalized.contains("win")) {
            return Platform.WINDOWS;
        }

        if (normalized.contains("linux")) {
            return Platform.LINUX;
        }

        throw new UnsupportedOperationException(
                "Unsupported operating system: " + osName
        );
    }
}