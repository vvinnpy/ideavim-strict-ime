package dev.local.ideavim.strictime;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;

import java.awt.Component;

/**
 * Decides which editors are eligible for the IdeaVim strict-mode guard.
 *
 * <p>The reworked IntelliJ terminal is implemented with editor components for
 * terminal output. IdeaVim sees those components as command-mode editors, but
 * keystrokes belong to the terminal process (for example tig), so they must not
 * be forced into English.</p>
 */
final class EditorTargetPolicy {
    private static final String TERMINAL_PLATFORM_PREFIX = "com.intellij.terminal.";
    private static final String TERMINAL_PLUGIN_PREFIX = "org.jetbrains.plugins.terminal.";
    private static final String TERMINAL_PANEL_MARKER =
            "org.jetbrains.plugins.terminal.TerminalPanelMarker";

    private EditorTargetPolicy() {
    }

    static boolean shouldGuard(Editor editor, String modeName) {
        return shouldGuard(isTerminalEditor(editor), modeName);
    }

    static boolean shouldGuard(boolean terminalEditor, String modeName) {
        return !terminalEditor && InputMethodPolicy.isStrictMode(modeName);
    }

    static boolean isTerminalEditor(Editor editor) {
        if (editor == null) {
            return false;
        }

        VirtualFile file = editor.getVirtualFile();
        if (file != null && isTerminalClass(file.getClass())) {
            return true;
        }

        for (Component component = editor.getContentComponent();
             component != null;
             component = component.getParent()) {
            if (isTerminalClass(component.getClass())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTerminalClass(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (isTerminalClassName(current.getName())) {
                return true;
            }
            if (implementsInterface(current, TERMINAL_PANEL_MARKER)) {
                return true;
            }
        }
        return false;
    }

    static boolean isTerminalClassName(String className) {
        return className != null
                && (className.startsWith(TERMINAL_PLATFORM_PREFIX)
                || className.startsWith(TERMINAL_PLUGIN_PREFIX));
    }

    private static boolean implementsInterface(Class<?> type, String interfaceName) {
        for (Class<?> implemented : type.getInterfaces()) {
            if (interfaceName.equals(implemented.getName())
                    || implementsInterface(implemented, interfaceName)) {
                return true;
            }
        }
        return false;
    }
}
