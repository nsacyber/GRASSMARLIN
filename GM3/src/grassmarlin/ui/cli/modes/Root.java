package grassmarlin.ui.cli.modes;

import grassmarlin.ui.cli.CommandMode;
import grassmarlin.ui.cli.CommandParser;

import java.util.HashMap;
import java.util.Map;

public class Root extends CommandMode {
    private final Map<String, CommandMode> commands;

    public Root() {
        super("> ");

        final CommandMode modeShow = new Show();

        this.commands = new HashMap<>();
        this.commands.put("SHOW", modeShow);
        this.commands.put("LIST", modeShow);
        this.commands.put("SESSION", new CliSession());
    }

    private CommandMode resultOf(final CommandMode mode, final CommandParser parser, final String command) {
        final CommandMode result = mode.processCommand(parser, command);
        if(result == mode || result == null) {
            return this;
        } else {
            return result;
        }
    }

    @Override
    public CommandMode processCommand(final CommandParser parser, final String text) {
        final String[] tokens = text.split(" ", 2);
        final String cmdFirst = tokens[0].toUpperCase();

        switch(cmdFirst) {
            case "EXIT":
            case "QUIT":
                return null;
            case "HELP":
                System.out.println("TODO: Help system.");
                break;
            default:
                final CommandMode mode = commands.get(cmdFirst);
                if(mode != null) {
                    if(tokens.length > 1) {
                        return resultOf(mode, parser, tokens[1]);
                    } else {
                        return mode;
                    }
                } else {
                    System.out.println("Unknown command: " + cmdFirst);
                    break;
                }
        }
        return this;
    }
}
