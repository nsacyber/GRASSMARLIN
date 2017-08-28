package grassmarlin.ui.cli.modes;

import grassmarlin.Logger;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.ui.cli.CommandMode;
import grassmarlin.ui.cli.CommandParser;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CliSession extends CommandMode {

    private Object pipelineTemplate = null;

    public CliSession() {
        super("SESSION> ");
    }

    protected void importFile(final CommandParser parser, final String path) {
        final Path pathFile = Paths.get(path);
        importFile(parser, parser.getConfiguration().importerForFile(pathFile), pathFile);
    }
    protected void importFile(final CommandParser parser, final String importer, final String path) {
        importFile(parser, parser.getConfiguration().importerForName(importer), Paths.get(path));
    }
    private void importFile(final CommandParser parser, final IPlugin.ImportProcessorWrapper importWrapper, final Path path) {
        if(importWrapper == null) {
            Logger.log(Logger.Severity.ERROR, "Unknown import type.");
            return;
        }

        final IPlugin.HasImportProcessors plugin = (IPlugin.HasImportProcessors)parser.getConfiguration().pluginFor(importWrapper.getClass());

        final ImportItem.FromPlugin temp = new ImportItem.FromPlugin(path, parser.getSession().getDefaultPipelineEntry());
        temp.importerPluginNameProperty().set(parser.getConfiguration().pluginNameFor(importWrapper.getClass()));
        temp.importerFunctionNameProperty().set(importWrapper.getName());

        parser.getSession().processImport(temp);
    }

    @Override
    public CommandMode processCommand(final CommandParser parser, final String text) {
        final String[] tokens= text.split(" ", 2);
        final String tokenCmd = tokens[0].toUpperCase();

        switch (tokenCmd) {
            case "EXIT":
            case "QUIT":
                return null;
            case "NEW":
                if(parser.getSession() != null) {
                    System.out.println("Closing existing session...");
                    try {
                        parser.getSession().close();
                    } catch(Exception ex) {
                        Logger.log(Logger.Severity.ERROR, "Error while closing session: %s", ex.getMessage());
                    }
                }
                parser.setSession(new Session(parser.getConfiguration()));
                System.out.println("Created new Session using pipeline template: " + pipelineTemplate);
                break;
            case "IMPORT":
                //Parameter is a string containing the path to import.
                importFile(parser, tokens[1]);
                break;
            case "IMPORTAS":
                //Two parameters, separated by a space; the first is the name of the importer to use, the second is the file to import.
                final String[] tokensImportAs = tokens[1].split(" ", 2);
                importFile(parser, tokensImportAs[0], tokensImportAs[1]);
                break;
            default:
                System.out.println("TODO: Session is not yet implemented.");
                break;
        }
        return this;
    }
}
