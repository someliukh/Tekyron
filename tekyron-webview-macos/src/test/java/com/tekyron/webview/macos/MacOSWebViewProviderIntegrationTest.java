
package com.tekyron.webview.macos;

import com.tekyron.core.WebViewProviderResolver;
import com.tekyron.webview.Platform;
import com.tekyron.webview.WebViewProvider;

public final class MacOSWebViewProviderIntegrationTest {

    public static void main(String[] args) {
        WebViewProvider provider = WebViewProviderResolver.resolve();

        if (!(provider instanceof MacOSWebViewProvider)) {
            throw new AssertionError(
                    "Expected MacOSWebViewProvider, but got: "
                            + provider.getClass().getName()
            );
        }

        if (provider.platform() != Platform.MACOS) {
            throw new AssertionError(
                    "Expected MACOS platform, but got: "
                            + provider.platform()
            );
        }

        System.out.println("Resolved provider: "
                + provider.getClass().getName());

        System.out.println("Platform: " + provider.platform());

        System.out.println("WebViewProvider integration test PASSED");
    }
}
