package grassmarlin.ui.console;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CommandParser {
    public interface ICommandParser {
        String getPrompt();
        ICommandParser parseCommand(final String command, final ConsoleSessionInterfaceController controller);
    }

    private final Stack<ICommandParser> parsers;
    private final List<Logger.Message> messagesPending;

    private ConsoleSessionInterfaceController controller;

    private final RuntimeConfiguration config;
    final AtomicBoolean allowColor = new AtomicBoolean(false);

    public CommandParser(final RuntimeConfiguration config) {
        this.parsers = new Stack<>();
        this.config = config;
        this.parsers.push(new ParserRoot(this, this.config));

        this.controller = new ConsoleSessionInterfaceController(new Session(this.config));

        this.messagesPending = new ArrayList<>();
        Logger.getInstance().onMessage.addHandler((source, arguments) -> {
            synchronized(CommandParser.this.messagesPending) {
                CommandParser.this.messagesPending.add(arguments.getMessage());
            }
        });
    }

    public void executeLoop() {
        final Scanner scannerConsole = new Scanner(System.in);

        while(!parsers.isEmpty()) {
            //Flush pending message buffer
            synchronized(this.messagesPending) {
                final boolean useColor = this.allowColor.get();
                for(final Logger.Message message : this.messagesPending) {
                    if(useColor) {
                        switch(message.getSeverity()) {
                            case INFORMATION:
                                System.out.print("\u001B[34m");
                                break;
                            case WARNING:
                                System.out.print("\u001B[33m");
                                break;
                            case ERROR:
                                System.out.print("\u001B[31m");
                                break;
                            case COMPLETION:
                                System.out.print("\u001B[32m");
                                break;
                            default:
                                System.out.print("\u001B[35m");
                                break;
                        }
                    }
                    System.out.println(message.toString());
                }
                this.messagesPending.clear();
                if(useColor) {
                    System.out.print("\u001B[0m");
                }
            }
            //Draw prompt
            System.out.print(parsers.stream().map(parser -> parser.getPrompt()).collect(Collectors.joining("->","",">")));

            final String input = scannerConsole.nextLine();
            if(input.trim().isEmpty()) {
                continue;
            }

            final ICommandParser parserNext = this.parsers.peek().parseCommand(input, this.controller);
            if(parserNext == this.parsers.peek()) {
                //No transition
            } else if(parserNext == null) {
                this.parsers.pop();
            } else {
                this.parsers.push(parserNext);
            }
        }
    }
}
