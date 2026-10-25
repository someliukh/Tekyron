
package com.tekyron.core;

import com.tekyron.webview.WebView;
import com.tekyron.webview.WebViewProvider;

import java.net.URI;
import java.util.Objects;

public final class TekyronApplication {

    private TekyronApplication() {
    }

    public static void launch(String frontendUrl) {
        Objects.requireNonNull(frontendUrl, "frontendUrl");

        URI uri = URI.create(frontendUrl);

        if (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "Frontend URL must use HTTP or HTTPS"
            );
        }

        if (uri.getHost() == null) {
            throw new IllegalArgumentException(
                    "Frontend URL must contain a valid host"
            );
        }

        WebViewProvider provider = WebViewProviderResolver.resolve();

        try (WebView webView = provider.create()) {
            webView.navigate(uri.toString());
            webView.run();
        }
    }
}
