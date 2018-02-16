package grassmarlin.ui.console;

import grassmarlin.Logger;
import grassmarlin.plugins.IPlugin;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ParserPluginWrapper implements CommandParser.ICommandParser {
    private final IPlugin plugin;

    private final Map<String, MenuItem> cacheMenuItems;

    public ParserPluginWrapper(final IPlugin plugin) {
        this.plugin = plugin;

        this.cacheMenuItems = new LinkedHashMap<>();
        this.processMenuItems(this.plugin.getMenuItems(), "");
    }

    private void processMenuItems(final Collection<MenuItem> items, final String base) {
        for(final MenuItem item : items) {
            if(item instanceof Menu) {
                processMenuItems(((Menu)item).getItems(), base + item.getText() + ".");
            } else {
                if(item instanceof CheckMenuItem || item instanceof RadioMenuItem || item.getClass().getAnnotation(IPlugin.SafeForConsole.class) != null) {
                    this.cacheMenuItems.put(base + item.getText(), item);
                }
            }
        }
    }

    @Override
    public String getPrompt() {
        return this.plugin.getName();
    }

    @Override
    public CommandParser.ICommandParser parseCommand(String command, ConsoleSessionInterfaceController controller) {
        final String[] commandTokens = command.split(" ", 2);
        switch(commandTokens[0].toUpperCase()) {
            case "BACK":
            case "EXIT":
            case "QUIT":
                return null;
            case "MENU":
                if(commandTokens.length > 1 && !commandTokens[1].isEmpty()) {
                    final MenuItem item = this.cacheMenuItems.get(commandTokens[1]);
                    if(item != null) {
                        if(item.isDisable() || !item.isVisible()) {
                            Logger.log(Logger.Severity.WARNING, "The menu item is not available at this time.");
                        } else {
                            if(item instanceof CheckMenuItem) {
                                ((CheckMenuItem)item).setSelected(!((CheckMenuItem)item).isSelected());
                            } else if(item instanceof RadioMenuItem) {
                                final ToggleGroup group = ((RadioMenuItem)item).getToggleGroup();
                                if(group != null) {
                                    group.selectToggle((RadioMenuItem) item);
                                }
                            } else {
                                if(item.getClass().getAnnotation(IPlugin.SafeForConsole.class) != null) {
                                    final EventHandler<ActionEvent> handler = item.getOnAction();
                                    if(handler != null) {
                                        handler.handle(null);
                                    } else {
                                        Logger.log(Logger.Severity.WARNING, "Unable to invoke menu item.");
                                    }
                                } else {
                                    Logger.log(Logger.Severity.WARNING, "The selected command cannot be executed in Console mode.");
                                }
                            }
                        }
                    } else {
                        Logger.log(Logger.Severity.ERROR, "Could not locate menu item named %s", commandTokens[1]);
                    }
                } else {
                    for(final Map.Entry<String, MenuItem> entry : this.cacheMenuItems.entrySet()) {
                        if(entry.getValue() instanceof CheckMenuItem) {
                            Logger.log(entry.getValue().isDisable() ? Logger.Severity.WARNING : Logger.Severity.COMPLETION, "[%s] %s", ((CheckMenuItem)entry.getValue()).isSelected() ? "x" : " ", entry.getKey());
                        } else if(entry.getValue() instanceof RadioMenuItem) {
                            Logger.log(entry.getValue().isDisable() ? Logger.Severity.WARNING : Logger.Severity.COMPLETION, "[%s] %s", ((RadioMenuItem)entry.getValue()).isSelected() ? "x" : " ", entry.getKey());
                        } else {
                            Logger.log(entry.getValue().isDisable() ? Logger.Severity.WARNING : Logger.Severity.COMPLETION, "    %s", entry.getKey());
                        }
                    }
                }
                return this;
            default:
                if(this.plugin instanceof IPlugin.ConsoleInput) {
                    ((IPlugin.ConsoleInput)this.plugin).processConsoleCommand(command, controller);
                } else {
                    Logger.log(Logger.Severity.ERROR, "Unknown command: %s, expected (BACK|EXIT|QUIT|MENU)", commandTokens[0]);
                }
                return this;
        }
    }
}
