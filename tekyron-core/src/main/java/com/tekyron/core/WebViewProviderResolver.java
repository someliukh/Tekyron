
package com.tekyron.core;

import com.tekyron.webview.Platform;
import com.tekyron.webview.PlatformDetector;
import com.tekyron.webview.WebViewProvider;

import java.util.Objects;
import java.util.ServiceLoader;

public final class WebViewProviderResolver {

    private WebViewProviderResolver() {
    }

    public static WebViewProvider resolve() {
        return resolve(
                PlatformDetector.detect(),
                ServiceLoader.load(WebViewProvider.class)
        );
    }

    public static WebViewProvider resolve(
            Platform platform,
            Iterable<WebViewProvider> providers
    ) {
        Objects.requireNonNull(platform, "platform");
        Objects.requireNonNull(providers, "providers");

        WebViewProvider selected = null;

        for (WebViewProvider provider : providers) {
            if (provider.platform() != platform) {
                continue;
            }

            if (selected != null) {
                throw new IllegalStateException(
                        "Multiple WebView providers found for platform: " + platform
                );
            }

            selected = provider;
        }

        if (selected == null) {
            throw new IllegalStateException(
                    "No WebView provider found for platform: " + platform
            );
        }

        return selected;
    }
}
