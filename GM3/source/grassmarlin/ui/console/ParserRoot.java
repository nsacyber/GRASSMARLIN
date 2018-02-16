package grassmarlin.ui.console;

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Session;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ParserRoot implements CommandParser.ICommandParser {
    private final RuntimeConfiguration config;
    private final CommandParser parser;
    private final ParserSession sessionParser;
    private final ParserPreferences preferencesParser;

    public ParserRoot(final CommandParser parser, final RuntimeConfiguration config) {
        this.parser = parser;
        this.config = config;

        this.sessionParser = new ParserSession();
        this.preferencesParser = new ParserPreferences();
    }

    @Override
    public String getPrompt() {
        return "Root";
    }

    @Override
    public CommandParser.ICommandParser parseCommand(String command, final ConsoleSessionInterfaceController controller) {
        final String[] commandTokens = command.split(" ");

        switch(commandTokens[0].toUpperCase()) {
            case "HELP":
                this.processHelp();
                return this;
            case "SHOW":
            case "VIEW":
                if(commandTokens.length > 1) {
                    this.processView(commandTokens[1], controller);
                } else {
                    Logger.log(Logger.Severity.WARNING, "Expected (SESSION|OPTIONS|PREFERENCES|PLUGINS)");
                }
                return this;
            case "COLOR":
                if(commandTokens.length > 1 && !commandTokens[1].isEmpty()) {
                    if(commandTokens[1].toUpperCase().equals("ON")) {
                        this.parser.allowColor.set(true);
                    } else if(commandTokens[1].toUpperCase().equals("OFF")) {
                        this.parser.allowColor.set(false);
                    } else {
                        Logger.log(Logger.Severity.ERROR, "Unknown value: %s, expected (|ON|OFF)", commandTokens[1]);
                    }
                } else {
                    Logger.log(Logger.Severity.INFORMATION, "Color is %s", this.parser.allowColor.get() ? "ON" : "OFF");
                }
                return this;
            case "WITH":
                if(commandTokens.length > 1) {
                    switch(commandTokens[1].toUpperCase()) {
                        case "SESSION":
                            return this.sessionParser;
                        case "PREFERENCES":
                            return this.preferencesParser;
                        case "PLUGIN":
                            //TODO: Better support for plugins interacting with the console interface
                            if(commandTokens.length > 2) {
                                final IPlugin plugin = this.config.pluginFor(commandTokens[2]);
                                if(plugin == null ) {
                                    Logger.log(Logger.Severity.ERROR, "Unknown plugin: %s", commandTokens[2]);
                                    return this;
                                } else if(plugin.getMenuItems() == null || plugin.getMenuItems().isEmpty()){
                                    Logger.log(Logger.Severity.WARNING, "The specified plugin does not support console interaction.");
                                    return this;
                                } else {
                                    return new ParserPluginWrapper(plugin);
                                }
                            } else {
                                Logger.log(Logger.Severity.ERROR, "Expected Plugin Name");
                                return this;
                            }
                        default:
                            Logger.log(Logger.Severity.ERROR, "Invalid target of WITH, expected (SESSION|PREFERENCES|PLUGIN {Plugin})");
                            return this;
                    }
                } else {
                    Logger.log(Logger.Severity.ERROR, "Missing target of WITH, expected (SESSION|PREFERENCES|PLUGIN {Plugin})");
                }
                return this;
            case "EXIT":
            case "QUIT":
                return null;
            default:
                Logger.log(Logger.Severity.ERROR, "Unknown command: %s; use the HELP command for a list of valid commands.", command);
                return this;
        }
    }

    private void processHelp() {
        Logger.log(Logger.Severity.INFORMATION, "HELP");
        Logger.log(Logger.Severity.INFORMATION, "COLOR (|ON|OFF)");
        Logger.log(Logger.Severity.INFORMATION, "(SHOW|VIEW) (SESSION|OPTIONS|PREFERENCES|PLUGINS)");
        Logger.log(Logger.Severity.INFORMATION, "WITH (SESSION|PREFERENCES|PLUGIN {Plugin})");
        Logger.log(Logger.Severity.INFORMATION, "EXIT|QUIT");
    }

    private void processView(final String target, final ConsoleSessionInterfaceController controller) {
        switch(target.toUpperCase()) {
            case "SESSION":
                if(controller == null) {
                    Logger.log(Logger.Severity.WARNING, "No Session Set");
                } else {
                    if(controller.getSession() == null) {
                        Logger.log(Logger.Severity.WARNING, "No Session Set");
                    } else {
                        final Session session = controller.getSession();
                        //TODO: Rather than write using toString, serialize to XML (?)
                        final StringWriter writerString = new StringWriter();
                        try {
                            final XMLStreamWriter writer = new IndentingXMLStreamWriter(XMLOutputFactory.newInstance().createXMLStreamWriter(writerString));
                            session.writeToXml(writer);
                            writer.flush();
                            Logger.log(Logger.Severity.INFORMATION, "%s:\n%s", controller.isDirty() ? "Dirty" : "Clean", writerString.getBuffer().toString());
                        } catch(XMLStreamException ex) {
                            Logger.log(Logger.Severity.ERROR, "Error while generating XML: %s", ex);
                        }
                    }
                }
                return;
            case "OPTIONS":
                Logger.log(Logger.Severity.INFORMATION, "Active: %s", this.config.allowActiveScanningProperty().get());
                Logger.log(Logger.Severity.INFORMATION, "Live Pcap: %s", this.config.allowLivePcapProperty().get());
                Logger.log(Logger.Severity.INFORMATION, "Plugins: %s", this.config.allowPluginsProperty().get());
                Logger.log(Logger.Severity.INFORMATION, "Developer Mode: %s", this.config.isDeveloperModeProperty().get());
                return;
            case "PREFERENCES":
                for(RuntimeConfiguration.PersistableFields field : RuntimeConfiguration.PersistableFields.values()) {
                    Logger.log(Logger.Severity.INFORMATION, "%s%s: %s", RuntimeConfiguration.isPersistedStringSet(field)? "*" : " ", field.getKey(), RuntimeConfiguration.getPersistedString(field));
                }
                return;
            case "PLUGINS":
                for(final IPlugin plugin : this.config.enumeratePlugins(IPlugin.class)) {
                    final Logger.Severity severity;
                    if(plugin instanceof IPlugin.ConsoleInput) {
                        severity = Logger.Severity.COMPLETION;
                    } else if(plugin.getMenuItems() != null && !plugin.getMenuItems().isEmpty()) {
                        severity = Logger.Severity.INFORMATION;
                    } else {
                        severity = Logger.Severity.WARNING;
                    }
                    Logger.log(severity, "%s : {%s}", RuntimeConfiguration.pluginNameFor(plugin.getClass()), Arrays.stream(plugin.getClass().getInterfaces()).filter(i -> IPlugin.class.isAssignableFrom(i) && i != IPlugin.class).map(i -> i.getSimpleName()).collect(Collectors.joining(",")));
                }
                return;
            default:
                Logger.log(Logger.Severity.WARNING, "Unknown parameter to VIEW command: %s", target);
                return;
        }
    }
}
