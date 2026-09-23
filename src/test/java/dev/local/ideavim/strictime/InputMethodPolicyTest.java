package dev.local.ideavim.strictime;

import org.junit.jupiter.api.Test;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputMethodPolicyTest {
    @Test
    void insertAndReplaceModesAreEditingModes() {
        assertFalse(InputMethodPolicy.isStrictMode("INSERT"));
        assertFalse(InputMethodPolicy.isStrictMode("INSERT_NORMAL"));
        assertFalse(InputMethodPolicy.isStrictMode("INSERT_VISUAL"));
        assertFalse(InputMethodPolicy.isStrictMode("INSERT_SELECT"));
        assertFalse(InputMethodPolicy.isStrictMode("REPLACE"));
    }

    @Test
    void commandAndVisualModesAreStrict() {
        assertTrue(InputMethodPolicy.isStrictMode("COMMAND"));
        assertTrue(InputMethodPolicy.isStrictMode("VISUAL"));
        assertTrue(InputMethodPolicy.isStrictMode("SELECT"));
        assertTrue(InputMethodPolicy.isStrictMode("OP_PENDING"));
    }

    @Test
    void unknownModeFailsOpen() {
        assertFalse(InputMethodPolicy.isStrictMode(null));
        assertFalse(InputMethodPolicy.isStrictMode("UNKNOWN"));
    }

    @Test
    void nonAsciiCharactersAreBlockedOnlyByPolicy() {
        assertFalse(InputMethodPolicy.isNonAscii('a'));
        assertFalse(InputMethodPolicy.isNonAscii('~'));
        assertTrue(InputMethodPolicy.isNonAscii('中'));
        assertTrue(InputMethodPolicy.isNonAscii('，'));
    }

    @Test
    void detectsImeSwitchRelease() {
        KeyEvent event = new KeyEvent(
                new java.awt.Canvas(),
                KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                0,
                0,
                KeyEvent.CHAR_UNDEFINED
        );
        assertTrue(InputMethodPolicy.isImeSwitchRelease(event));
    }

    @Test
    void terminalEditorsAreNeverStrict() {
        assertFalse(EditorTargetPolicy.shouldGuard(true, "COMMAND"));
        assertTrue(EditorTargetPolicy.shouldGuard(false, "COMMAND"));
        assertFalse(EditorTargetPolicy.shouldGuard(false, "INSERT"));
    }

    @Test
    void detectsTerminalClassNames() {
        assertTrue(EditorTargetPolicy.isTerminalClassName(
                "com.intellij.terminal.frontend.view.impl.TerminalViewImpl$TerminalPanel"));
        assertTrue(EditorTargetPolicy.isTerminalClassName(
                "org.jetbrains.plugins.terminal.block.TerminalWidgetImpl"));
        assertFalse(EditorTargetPolicy.isTerminalClassName(
                "com.intellij.openapi.editor.impl.EditorComponentImpl"));
    }
}
