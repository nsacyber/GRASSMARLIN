package grassmarlin.ui.console;

import grassmarlin.Logger;

public class ParserPreferences implements CommandParser.ICommandParser {
    public ParserPreferences() {

    }

    @Override
    public String getPrompt() {
        return "Preferences";
    }

    @Override
    public CommandParser.ICommandParser parseCommand(String command, ConsoleSessionInterfaceController controller) {
        final String[] commandTokens = command.split(" ");
        switch(commandTokens[0].toUpperCase()) {
            case "QUIT":
            case "EXIT":
                return null;
            default:
                Logger.log(Logger.Severity.WARNING, "Unknown command: %s", commandTokens[0]);
                return this;
        }
    }
}
