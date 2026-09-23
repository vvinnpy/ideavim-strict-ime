package dev.local.ideavim.strictime;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.ActionPlan;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.actionSystem.TypedActionHandlerEx;

/**
 * Fallback guard for committed characters. IdeaVim normally receives these through its typed-action handler.
 */
final class GuardTypedActionHandler implements TypedActionHandlerEx {
    private final TypedActionHandler delegate;
    private final IdeaVimModeQuery modeQuery;
    private final Fcitx5Switcher switcher;

    GuardTypedActionHandler(TypedActionHandler delegate, IdeaVimModeQuery modeQuery, Fcitx5Switcher switcher) {
        this.delegate = delegate;
        this.modeQuery = modeQuery;
        this.switcher = switcher;
    }

    @Override
    public void beforeExecute(Editor editor, char charTyped, DataContext context, ActionPlan plan) {
        if (shouldBlock(editor, charTyped)) {
            return;
        }
        if (delegate instanceof TypedActionHandlerEx extended) {
            extended.beforeExecute(editor, charTyped, context, plan);
        }
    }

    @Override
    public void execute(Editor editor, char charTyped, DataContext context) {
        if (shouldBlock(editor, charTyped)) {
            return;
        }
        delegate.execute(editor, charTyped, context);
    }

    TypedActionHandler delegate() {
        return delegate;
    }

    private boolean shouldBlock(Editor editor, char charTyped) {
        if (!InputMethodPolicy.isNonAscii(charTyped)) {
            return false;
        }

        String modeName = modeQuery.getModeName(editor);
        if (!EditorTargetPolicy.shouldGuard(editor, modeName)) {
            return false;
        }

        switcher.requestEnglish();
        return true;
    }
}
