package com.tekyron.webview;

public interface WebView extends AutoCloseable {

    void navigate(String url);
    void loadHtml(String html);
    void evaluateJavaScript(String script);

    @Override
    void close() throws Exception;
}
