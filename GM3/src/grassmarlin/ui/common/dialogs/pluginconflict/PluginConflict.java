package grassmarlin.ui.common.dialogs.pluginconflict;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Arrays;
import java.util.Collection;

public class PluginConflict {
    public enum Action {
        Ignore,
        Load,
        Unload,
        Discard,
        UseBlank("Load With Empty Data");

        private final String displayText;

        Action() {
            this.displayText = null;
        }
        Action(final String displayText) {
            this.displayText = displayText;
        }

        @Override
        public String toString() {
            if(this.displayText == null) {
                return super.toString();
            } else {
                return this.displayText;
            }
        }
    }

    public enum Conflict {
        Missing("Missing", Action.Ignore),
        New("New", Action.Load, Action.Unload),
        MissingWithData("Missing (Has Data)", Action.Discard),
        NewWithoutData("New (Expects Data)", Action.Unload, Action.UseBlank),
        UnexpectedData("Unexpected Data", Action.Discard),
        MissingData("Missing Data", Action.Unload, Action.UseBlank),
        VersionMismatch("Version Mismatch", Action.Load, Action.Unload),
        VersionMismatchWithData("Version Mismatch (Has Data)", Action.Load, Action.Unload, Action.UseBlank)
        ;

        private final String displayText;
        private final Collection<Action> actionsAllowed;

        Conflict(final String displayText, final Action... actions) {
            this.displayText = displayText;
            this.actionsAllowed = Arrays.asList(actions);
        }

        public String getDisplayText() {
            return this.displayText;
        }
        public Collection<Action> getAllowedActions() {
            return actionsAllowed;
        }
    }

    private final SimpleObjectProperty<Action> action;
    private final SimpleStringProperty plugin;
    private final SimpleObjectProperty<Conflict> conflict;

    public PluginConflict(final String plugin, final Conflict conflict) {
        this.action = new SimpleObjectProperty<>(null);
        this.plugin = new SimpleStringProperty(plugin);
        this.conflict = new SimpleObjectProperty<>(conflict);
    }

    public ObjectProperty<Action> actionProperty() {
        return this.action;
    }
    public StringProperty pluginProperty() {
        return this.plugin;
    }
    public ObjectProperty<Conflict> conflictProperty() {
        return this.conflict;
    }

    public BooleanBinding isResolvedProperty() {
        return action.isNotNull();
    }
}
