package iadgov.importcompletealert;

import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;

import java.io.Serializable;

public class Configuration implements Serializable, Cloneable {
    @PreferenceDialog.Field(name="Script", accessorName="Script", nullable = true)
    private String script;
    @PreferenceDialog.Field(name="Working Directory", accessorName="WorkingDirectory", nullable = true)
    private String workingDirectory;

    public Configuration() {
        script = null;
        workingDirectory = null;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
}
