package dev.local.ideavim.strictime;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Uses a persistent Fcitx5 D-Bus connection to avoid spawning helper processes.
 *
 * <p>Fcitx5 Controller1.State returns 2 while an input method is active and
 * Deactivate switches back to the configured English keyboard layout.</p>
 */
final class Fcitx5Switcher implements Disposable {
    private static final Logger LOG = Logger.getInstance(Fcitx5Switcher.class);
    private static final int ACTIVE_STATE = 2;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "ideavim-strict-ime-fcitx5-dbus");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean();
    private final AtomicBoolean warned = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Object connectionLock = new Object();

    private DBusConnection connection;
    private FcitxController controller;

    void requestEnglish() {
        if (closed.get() || !running.compareAndSet(false, true)) {
            return;
        }

        executor.execute(() -> {
            try {
                FcitxController fcitx = controller();
                if (fcitx.state() == ACTIVE_STATE) {
                    fcitx.deactivate();
                }
            } catch (Throwable exception) {
                if (warned.compareAndSet(false, true)) {
                    LOG.warn("Unable to control Fcitx5 through D-Bus; using fcitx5-remote fallback", exception);
                }
                resetConnection();
                runFcitx5RemoteFallback();
            } finally {
                running.set(false);
            }
        });
    }

    private void runFcitx5RemoteFallback() {
        try {
            Process process = new ProcessBuilder("fcitx5-remote", "-c")
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(500, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
        } catch (IOException exception) {
            LOG.debug("fcitx5-remote fallback is unavailable", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private FcitxController controller() throws Exception {
        synchronized (connectionLock) {
            if (controller != null) {
                return controller;
            }

            connection = DBusConnectionBuilder.forSessionBus().build();
            controller = connection.getRemoteObject(
                    "org.fcitx.Fcitx5",
                    "/controller",
                    FcitxController.class,
                    false
            );
            warned.set(false);
            return controller;
        }
    }

    private void resetConnection() {
        synchronized (connectionLock) {
            controller = null;
            if (connection != null) {
                try {
                    connection.close();
                } catch (IOException exception) {
                    LOG.debug("Error closing Fcitx5 D-Bus connection", exception);
                }
                connection = null;
            }
        }
    }

    @Override
    public void dispose() {
        if (closed.compareAndSet(false, true)) {
            executor.shutdownNow();
            resetConnection();
        }
    }
}
