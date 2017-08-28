package grassmarlin.ui.cli.modes;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.ui.cli.CommandMode;
import grassmarlin.ui.cli.CommandParser;

public class Show extends CommandMode {
    public Show() {
        super("SHOW> ");
    }

    public static void showPlugins(final RuntimeConfiguration config) {
        for(IPlugin plugin : config.enumeratePlugins(IPlugin.class)) {
            System.out.println(String.format("  [%s] (%s)", plugin.getName(), plugin.getClass().getClassLoader()));
            Class<?> classPlugin = plugin.getClass();
            while(!classPlugin.equals(Object.class)) {
                for (Class<?> clazz : classPlugin.getInterfaces()) {
                    if (IPlugin.class.isAssignableFrom(clazz)) {
                        System.out.println(String.format("  + %s (from %s)", clazz.getName(), classPlugin.getName()));
                    }
                }
                classPlugin = classPlugin.getSuperclass();
            }
        }
    }

    @Override
    public CommandMode processCommand(final CommandParser parser, final String text) {
        final String textUpperCase = text.toUpperCase();

        switch(textUpperCase) {
            case "EXIT":
            case "QUIT":
            default:
                return null;
            case "PLUGINS":
                showPlugins(parser.getConfiguration());
                break;
            case "SESSION":
                if(parser.getSession() == null) {
                    System.out.println("[null]");
                } else {
                    System.out.println(parser.getSession());
                }
                break;
            case "IMPORTS":
                if(parser.getSession() != null) {
                    parser.getSession().allImportsProperty().forEach(System.out::println);
                }
                break;
            case "HELP":
                System.out.println("Available commands (EXIT is always available):\nPLUGINS");
                break;
        }
        return this;
    }
}
