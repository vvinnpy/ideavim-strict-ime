package dev.local.ideavim.strictime;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;

public final class StrictImeStartupActivity implements StartupActivity.DumbAware {
    @Override
    public void runActivity(Project project) {
        StrictImeGuardService.getInstance().install();
    }
}
