
package com.tekyron.webview.macos;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class MacOSWebViewSmokeTest {

    private static final int TASK_COUNT = 100;
    private static final int INITIAL_DELAY_MS = 2000;
    private static final int TASK_INTERVAL_MS = 30;

    public static void main(String[] args) {

        Path library = Path.of(
                "tekyron-webview-macos/build/native/core/libwebviewd.dylib"
        ).toAbsolutePath();

        try (MacOSWebView webView = new MacOSWebView(library)) {

            Thread ownerThread = Thread.currentThread();

            AtomicInteger executed = new AtomicInteger();
            AtomicInteger failed = new AtomicInteger();

            CountDownLatch completed = new CountDownLatch(TASK_COUNT);

            webView.loadHtml("""
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <title>Tekyron Concurrency Test</title>
                        <style>
                            body {
                                background: #171717;
                                color: #ffffff;
                                font-family: -apple-system, sans-serif;
                                padding: 40px;
                            }
                            h1 {
                                color: #7dd3fc;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>Tekyron Concurrency Test</h1>
                        <p>Waiting 2 seconds before starting...</p>
                        <p>Tasks: 0 / 100</p>
                    </body>
                    </html>
                    """);

            Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(INITIAL_DELAY_MS);

                    System.out.println("Starting concurrency test...");

                    for (int i = 0; i < TASK_COUNT; i++) {

                        int taskNumber = i + 1;

                        Thread.startVirtualThread(() -> {
                            try {
                                webView.dispatch(() -> {
                                    try {
                                        if (Thread.currentThread() != ownerThread) {
                                            throw new AssertionError(
                                                    "Task " + taskNumber
                                                            + " executed on wrong thread"
                                            );
                                        }

                                        int count = executed.incrementAndGet();

                                        System.out.println(
                                                "Task " + taskNumber
                                                        + " completed. Total: " + count
                                        );

                                    } catch (Throwable e) {
                                        failed.incrementAndGet();
                                        e.printStackTrace();

                                    } finally {
                                        completed.countDown();
                                    }
                                });

                            } catch (Throwable e) {
                                failed.incrementAndGet();
                                System.err.println(
                                        "Failed to dispatch task " + taskNumber
                                );
                                e.printStackTrace();
                                completed.countDown();
                            }
                        });

                        Thread.sleep(TASK_INTERVAL_MS);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            Thread.startVirtualThread(() -> {
                try {
                    completed.await();

                    int successCount = executed.get();
                    int failureCount = failed.get();

                    System.out.println();
                    System.out.println("===== TEST RESULTS =====");
                    System.out.println("Executed: " + successCount);
                    System.out.println("Failed: " + failureCount);
                    System.out.println("========================");

                    webView.dispatch(() -> {
                        webView.loadHtml("""
                                <!DOCTYPE html>
                                <html lang="en">
                                <head>
                                    <meta charset="UTF-8">
                                    <title>Tekyron Test Results</title>
                                    <style>
                                        body {
                                            background: #171717;
                                            color: #ffffff;
                                            font-family: -apple-system, sans-serif;
                                            padding: 40px;
                                        }
                                        h1 {
                                            color: #86efac;
                                        }
                                    </style>
                                </head>
                                <body>
                                    <h1>Concurrency Test Complete</h1>
                                    <p>Executed: %d / %d</p>
                                    <p>Failed: %d</p>
                                </body>
                                </html>
                                """.formatted(
                                successCount,
                                TASK_COUNT,
                                failureCount
                        ));
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                } catch (IllegalStateException e) {
                    System.out.println(
                            "WebView closed before final UI update"
                    );
                }
            });

            System.out.println("Owner thread: " + ownerThread);

            webView.run();

            System.out.println(
                    "WebView closed. Executed: " + executed.get()
            );
        }
    }
}
