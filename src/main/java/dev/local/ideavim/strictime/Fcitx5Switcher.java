package dev.local.ideavim.strictime;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Switches Fcitx5 from an active input method to its English keyboard state.
 */
final class Fcitx5Switcher implements Disposable {
    private static final Logger LOG = Logger.getInstance(Fcitx5Switcher.class);
    private static final long COOLDOWN_NANOS = TimeUnit.MILLISECONDS.toNanos(180);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ideavim-strict-ime-fcitx5");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean warned = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile long lastSwitchNanos;

    void requestEnglish() {
        long now = System.nanoTime();
        long previous = lastSwitchNanos;
        if (now - previous < COOLDOWN_NANOS) {
            return;
        }
        lastSwitchNanos = now;

        if (closed.get()) {
            return;
        }

        executor.execute(() -> {
            try {
                Process process = new ProcessBuilder("fcitx5-remote", "-c")
                        .redirectErrorStream(true)
                        .start();
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    LOG.warn("Timed out while switching Fcitx5 to English");
                }
            } catch (IOException exception) {
                if (warned.compareAndSet(false, true)) {
                    LOG.warn("fcitx5-remote is unavailable; strict IME guard cannot switch input methods", exception);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Override
    public void dispose() {
        if (closed.compareAndSet(false, true)) {
            executor.shutdownNow();
        }
    }
}
