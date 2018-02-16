package grassmarlin.ui.console;

import grassmarlin.Logger;
import grassmarlin.session.ImportItem;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ParserSession implements CommandParser.ICommandParser {
    public ParserSession() {

    }

    @Override
    public String getPrompt() {
        return "Session";
    }

    @Override
    public CommandParser.ICommandParser parseCommand(String command, ConsoleSessionInterfaceController controller) {
        final String[] tokensCommand = command.split(" ", 2);
        switch(tokensCommand[0].toUpperCase()) {
            case "BACK":
            case "EXIT":
            case "QUIT":
                return null;
            case "IMPORT": {
                //TODO: Support quoting arguments
                if(tokensCommand.length == 1) {
                    Logger.log(Logger.Severity.WARNING, "Expected IMPORT {plugin} {parser} {entry} {path}");
                    return this;
                } else {
                    final String[] tokensImport = tokensCommand[1].split(" ", 4);

                    final String entry;
                    final String path;
                    final String plugin;
                    final String parser;
                    if (tokensImport.length == 3) {
                        plugin = tokensImport[0];
                        parser = tokensImport[1];
                        entry = "Default";
                        path = tokensImport[2];
                    } else if (tokensImport.length == 4) {
                        plugin = tokensImport[0];
                        parser = tokensImport[1];
                        entry = tokensImport[2];
                        path = tokensImport[3];
                    } else {
                        Logger.log(Logger.Severity.ERROR, "Cannot start import expected IMPORT {plugin} {parser} {entry} {path}");
                        return this;
                    }
                    final ImportItem item = new ImportItem.FromPlugin(Paths.get(path), entry);
                    item.importerPluginNameProperty().set(plugin);
                    item.importerFunctionNameProperty().set(parser);
                    controller.getSession().processImport(item);
                    return this;
                }
            }
            case "STATUS": {
                final List<ImportItem> imports = new ArrayList<>(controller.getSession().allImportsProperty());
                for (final ImportItem item : imports) {
                    final Logger.Severity severity;
                    if (item.importCompleteProperty().get()) {
                        severity = Logger.Severity.COMPLETION;
                    } else if (item.importStartedProperty().get()) {
                        severity = Logger.Severity.WARNING;
                    } else {
                        severity = Logger.Severity.INFORMATION;
                    }
                    Logger.log(severity, "[%f] %s -> %s", item.progressProperty().doubleValue(), item.getPath(), item.pipelineEntryProperty().get());
                }
                return this;
            }
            case "WAIT":
                //TODO: Wait for import to complete
                return this;
            default:
                Logger.log(Logger.Severity.ERROR, "Unknown Command: %s, Exected (EXIT|QUIT|IMPORT|STATUS|WAIT)", tokensCommand[0]);
                return this;
        }
    }
}
