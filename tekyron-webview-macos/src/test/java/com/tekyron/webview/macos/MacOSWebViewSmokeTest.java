import com.tekyron.webview.macos.MacOSWebView;

public static void main(String[] args) {
    Path libraryPath = Path.of(
            "tekyron-webview-macos/build/native/core/libwebviewd.dylib"
    ).toAbsolutePath();

    System.out.println("1. Loading native library");

    try (MacOSWebView webView = new MacOSWebView(libraryPath)) {
        System.out.println("2. Native WebView created");

        webView.loadHtml("""
                <html>
                    <body>
                        <h1>Welcome to Tekyron!</h1>
                    </body>
                </html>
                """);

        System.out.println("3. HTML loaded");
        System.out.println("4. Starting event loop");

        webView.run();

        System.out.println("5. Event loop finished");
    }
}