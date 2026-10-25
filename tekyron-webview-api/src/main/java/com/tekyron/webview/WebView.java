
package com.tekyron.webview;

public interface WebView extends AutoCloseable {

    void navigate(String url);
    void loadHtml(String html);
    void evaluateJavaScript(String script);

    void run();

    @Override
    void close();
}
