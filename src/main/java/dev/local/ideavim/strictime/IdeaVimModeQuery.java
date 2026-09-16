package dev.local.ideavim.strictime;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Small, fail-open bridge to IdeaVim's internal mode state.
 */
final class IdeaVimModeQuery {
    private static final Logger LOG = Logger.getInstance(IdeaVimModeQuery.class);
    private static final PluginId IDEA_VIM_ID = PluginId.getId("IdeaVIM");

    private final AtomicBoolean initialized = new AtomicBoolean();
    private volatile Method getInstance;
    private volatile Method getMode;

    String getModeName(Editor editor) {
        if (editor == null || !initialize()) {
            return null;
        }

        try {
            Object state = getInstance.invoke(null, editor);
            if (state == null) {
                return null;
            }
            Object mode = getMode.invoke(state);
            return mode == null ? null : mode.toString();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LOG.warn("Unable to query IdeaVim mode", exception);
            return null;
        }
    }

    private boolean initialize() {
        if (initialized.get()) {
            return getInstance != null && getMode != null;
        }

        synchronized (initialized) {
            if (initialized.get()) {
                return getInstance != null && getMode != null;
            }

            try {
                IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(IDEA_VIM_ID);
                ClassLoader classLoader = descriptor == null ? null : descriptor.getPluginClassLoader();
                if (classLoader == null) {
                    LOG.warn("IdeaVIM plugin class loader is unavailable");
                    initialized.set(true);
                    return false;
                }

                Class<?> commandStateClass = Class.forName(
                        "com.maddyhome.idea.vim.command.CommandState",
                        true,
                        classLoader
                );
                getInstance = commandStateClass.getMethod("getInstance", Editor.class);
                getMode = commandStateClass.getMethod("getMode");
            } catch (ReflectiveOperationException | RuntimeException exception) {
                LOG.warn("Unable to initialize IdeaVim mode bridge", exception);
            }

            initialized.set(true);
            return getInstance != null && getMode != null;
        }
    }
}
