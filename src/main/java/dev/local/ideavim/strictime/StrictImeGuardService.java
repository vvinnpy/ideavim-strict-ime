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
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.InputMethodEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.Timer;

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
    private final Timer watchdog = new Timer(200, event -> enforceStrictMode());

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
        watchdog.setRepeats(true);
        watchdog.start();
        LOG.info("IdeaVim strict IME guard installed");
    }

    private void enforceStrictMode() {
        if (!ApplicationManager.getApplication().isActive()) {
            return;
        }

        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (!(focusOwner instanceof EditorComponentImpl editorComponent)) {
            return;
        }

        String modeName = modeQuery.getModeName(editorComponent.getEditor());
        if (EditorTargetPolicy.shouldGuard(editorComponent.getEditor(), modeName)) {
            switcher.requestEnglish();
        }
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
        if (!EditorTargetPolicy.shouldGuard(editorComponent.getEditor(), modeName)
                || !InputMethodPolicy.shouldBlockInputMethodEvent(event, modeName)) {
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
        if (!EditorTargetPolicy.shouldGuard(editorComponent.getEditor(), modeName)) {
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
        watchdog.stop();
        IdeEventQueue.getInstance().removeDispatcher(dispatcher);

        TypedAction typedAction = TypedAction.getInstance();
        if (typedAction.getHandler() instanceof GuardTypedActionHandler guard) {
            typedAction.setupHandler(guard.delegate());
        }
        switcher.dispose();
    }
}
