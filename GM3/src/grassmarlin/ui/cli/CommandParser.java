package grassmarlin.ui.cli;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Stack;

public class CommandParser {
    protected final RuntimeConfiguration config;
    private final Stack<CommandMode> modeStack;
    private final BufferedReader reader;

    // Application state
    private Session session = null;

    public CommandParser(RuntimeConfiguration config) {
        this.config = config;

        this.modeStack = new Stack<>();
        this.modeStack.push(new grassmarlin.ui.cli.modes.Root());

        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    public RuntimeConfiguration getConfiguration() {
        return this.config;
    }

    public String receiveCommand() {
        System.out.flush();
        System.out.print(this.modeStack.peek().getPrompt());
        try {
            return reader.readLine();
        } catch(IOException ex) {
            return null;
        }
    }

    public void executeUi() {
        while(!this.modeStack.isEmpty()) {
            final String command = receiveCommand();
            if(command == null) {
                System.out.println("Error reading input from the console; terminating.");
                return;
            }
            try {
                final CommandMode modeNext = this.modeStack.peek().processCommand(this, command);
                if (modeNext == null) {
                    //Terminate the current mode
                    this.modeStack.pop();
                } else if (modeNext != this.modeStack.peek()) {
                    //The mode is not the current, so push it on the stack.
                    this.modeStack.push(modeNext);
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public Session getSession() {
        return this.session;
    }
    public void setSession(final Session sessionNew) {
        this.session = sessionNew;
    }
}
