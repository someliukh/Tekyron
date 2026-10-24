
package com.tekyron.webview.macos;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class MacOSWebViewShutdownStressTest {

    private static final int WORKER_COUNT = 20;
    private static final int TEST_DURATION_SECONDS = 5;
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 10;

    public static void main(String[] args) throws InterruptedException {

        Path library = Path.of(
                "tekyron-webview-macos/build/native/core/libwebviewd.dylib"
        ).toAbsolutePath();

        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger submitted = new AtomicInteger();
        AtomicInteger executed = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger unexpectedFailures = new AtomicInteger();

        CountDownLatch workersFinished =
                new CountDownLatch(WORKER_COUNT);

        List<Thread> workers = new ArrayList<>();

        try (MacOSWebView webView = new MacOSWebView(library)) {

            Thread uiThread = Thread.currentThread();

            webView.loadHtml("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body {
                                background: #171717;
                                color: white;
                                font-family: -apple-system, sans-serif;
                                padding: 40px;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>Tekyron Shutdown Stress Test</h1>
                        <p>20 virtual threads are dispatching UI tasks.</p>
                        <p>Close this window after a few seconds.</p>
                    </body>
                    </html>
                    """);

            for (int i = 0; i < WORKER_COUNT; i++) {

                Thread worker = Thread.startVirtualThread(() -> {
                    try {
                        while (running.get()) {
                            try {
                                webView.dispatch(() -> {
                                    if (Thread.currentThread() != uiThread) {
                                        unexpectedFailures.incrementAndGet();
                                        System.err.println(
                                                "Callback executed on wrong thread"
                                        );
                                    }

                                    executed.incrementAndGet();
                                });

                                submitted.incrementAndGet();

                            } catch (IllegalStateException e) {
                                rejected.incrementAndGet();

                                if (!running.get()) {
                                    break;
                                }

                            } catch (Throwable e) {
                                unexpectedFailures.incrementAndGet();
                                e.printStackTrace();
                                break;
                            }

                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    } finally {
                        workersFinished.countDown();
                    }
                });

                workers.add(worker);
            }

            Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(
                            TimeUnit.SECONDS.toMillis(
                                    TEST_DURATION_SECONDS
                            )
                    );

                    System.out.println(
                            "Five seconds elapsed. Close the window now."
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            System.out.println("Stress test started.");
            System.out.println("Close the window to test shutdown.");

            try {
                webView.run();
            } finally {
                running.set(false);
            }

            boolean allWorkersStopped = workersFinished.await(
                    SHUTDOWN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            System.out.println();
            System.out.println("===== SHUTDOWN STRESS TEST =====");
            System.out.println("Submitted: " + submitted.get());
            System.out.println("Executed: " + executed.get());
            System.out.println("Rejected: " + rejected.get());
            System.out.println(
                    "Unexpected failures: " + unexpectedFailures.get()
            );
            System.out.println(
                    "Workers stopped: " + allWorkersStopped
            );
            System.out.println("================================");

            if (!allWorkersStopped) {
                throw new AssertionError(
                        "Workers did not stop within timeout"
                );
            }

            if (unexpectedFailures.get() != 0) {
                throw new AssertionError(
                        "Unexpected failures occurred"
                );
            }

            try {
                webView.dispatch(() -> {
                    throw new AssertionError(
                            "Callback must not execute after close"
                    );
                });

                throw new AssertionError(
                        "Dispatch should be rejected after close"
                );

            } catch (IllegalStateException expected) {
                System.out.println(
                        "Dispatch correctly rejected after close"
                );
            }

            System.out.println("Shutdown stress test PASSED");
        }
    }
}
