package grassmarlin.ui.common.dialogs.preferences;

import grassmarlin.RuntimeConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PreferenceValues implements Cloneable {
    @PreferenceDialog.Field(name = "Wireshark Path", accessorName = "WiresharkPath", nullable = true)
    private Path pathWireshark;
    @PreferenceDialog.Field(name = "Text Editor Path", accessorName = "TextEditorPath", nullable = true)
    private Path pathTextEditor;

    public PreferenceValues() {
        this.pathWireshark = Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.PATH_WIRESHARK));
        this.pathTextEditor = Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.TEXT_EDITOR_EXEC));
    }

    public Path getWiresharkPath() {
        return this.pathWireshark;
    }
    public void setWiresharkPath(final Path value) {
        this.pathWireshark = value;
    }

    public Path getTextEditorPath() {
        return this.pathTextEditor;
    }
    public void setTextEditorPath(final Path value) {
        this.pathTextEditor = value;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void apply() {
        RuntimeConfiguration.setPersistedString(RuntimeConfiguration.PersistableFields.PATH_WIRESHARK, this.getWiresharkPath().toString());
        RuntimeConfiguration.setPersistedString(RuntimeConfiguration.PersistableFields.TEXT_EDITOR_EXEC, this.getTextEditorPath().toString());
    }
}
