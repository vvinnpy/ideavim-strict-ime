package dev.local.ideavim.strictime;

import com.intellij.ide.IdeEventQueue;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.openapi.util.SystemInfo;

import java.awt.AWTEvent;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Application-wide owner of the event and typed-action guards.
 */
@Service(Service.Level.APP)
public final class StrictImeGuardService implements Disposable {
    private static final Logger LOG = Logger.getInstance(StrictImeGuardService.class);

    private final IdeaVimModeQuery modeQuery = new IdeaVimModeQuery();
    private final Fcitx5Switcher switcher = new Fcitx5Switcher();
    private final AtomicBoolean installed = new AtomicBoolean();
    private final IdeEventQueue.EventDispatcher dispatcher = this::dispatch;

    public static StrictImeGuardService getInstance() {
        return ApplicationManager.getApplication().getService(StrictImeGuardService.class);
    }

    void install() {
        if (!SystemInfo.isLinux) {
            LOG.info("IdeaVim strict IME guard is disabled on non-Linux systems");
            return;
        }

        if (!installed.compareAndSet(false, true)) {
            ensureTypedHandlerInstalled();
            return;
        }

        IdeEventQueue.getInstance().addDispatcher(dispatcher, this);
        ensureTypedHandlerInstalled();
        LOG.info("IdeaVim strict IME guard installed");
    }

    private void ensureTypedHandlerInstalled() {
        TypedAction typedAction = TypedAction.getInstance();
        if (typedAction.getHandler() instanceof GuardTypedActionHandler) {
            return;
        }

        TypedActionHandler previous = typedAction.getHandler();
        typedAction.setupHandler(new GuardTypedActionHandler(previous, modeQuery, switcher));
    }

    private boolean dispatch(AWTEvent event) {
        ensureTypedHandlerInstalled();

        if (event instanceof InputMethodEvent inputMethodEvent) {
            return handleInputMethodEvent(inputMethodEvent);
        }
        if (event instanceof KeyEvent keyEvent) {
            return handleKeyEvent(keyEvent);
        }
        return false;
    }

    private boolean handleInputMethodEvent(InputMethodEvent event) {
        if (!(event.getSource() instanceof EditorComponentImpl editorComponent)) {
            return false;
        }

        String modeName = modeQuery.getModeName(editorComponent.getEditor());
        if (!InputMethodPolicy.shouldBlockInputMethodEvent(event, modeName)) {
            return false;
        }

        switcher.requestEnglish();
        event.consume();
        return true;
    }

    private boolean handleKeyEvent(KeyEvent event) {
        if (!(event.getSource() instanceof EditorComponentImpl editorComponent)) {
            return false;
        }

        String modeName = modeQuery.getModeName(editorComponent.getEditor());
        if (!InputMethodPolicy.isStrictMode(modeName)) {
            return false;
        }

        if (InputMethodPolicy.isImeSwitchRelease(event)) {
            switcher.requestEnglish();
            return false;
        }

        if (event.getID() == KeyEvent.KEY_TYPED && InputMethodPolicy.isNonAscii(event.getKeyChar())) {
            switcher.requestEnglish();
            event.consume();
            return true;
        }

        return false;
    }

    @Override
    public void dispose() {
        IdeEventQueue.getInstance().removeDispatcher(dispatcher);

        TypedAction typedAction = TypedAction.getInstance();
        if (typedAction.getHandler() instanceof GuardTypedActionHandler guard) {
            typedAction.setupHandler(guard.delegate());
        }
        switcher.dispose();
    }
}
