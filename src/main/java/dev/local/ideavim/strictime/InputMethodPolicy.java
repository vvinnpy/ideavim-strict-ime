package dev.local.ideavim.strictime;

import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.text.AttributedCharacterIterator;
import java.util.Set;

/**
 * Pure policy logic shared by event and typed-action guards.
 */
public final class InputMethodPolicy {
    private static final Set<String> STRICT_MODES = Set.of(
            "COMMAND",
            "VISUAL",
            "SELECT",
            "OP_PENDING"
    );

    private InputMethodPolicy() {
    }

    public static boolean isStrictMode(String modeName) {
        return modeName != null && STRICT_MODES.contains(modeName);
    }

    public static boolean isNonAscii(char character) {
        return character > 0x7f;
    }

    public static boolean isImeSwitchRelease(KeyEvent event) {
        return event.getID() == KeyEvent.KEY_RELEASED
                && event.getKeyCode() == 0
                && event.getKeyChar() == KeyEvent.CHAR_UNDEFINED;
    }

    public static boolean shouldBlockInputMethodEvent(InputMethodEvent event, String modeName) {
        return isStrictMode(modeName)
                && (event.getCommittedCharacterCount() > 0 || hasText(event.getText()));
    }

    private static boolean hasText(AttributedCharacterIterator text) {
        return text != null && text.getEndIndex() > text.getBeginIndex();
    }
}
