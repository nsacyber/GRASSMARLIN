package grassmarlin.ui.cli;

public abstract class CommandMode {
    private final String prompt;

    protected CommandMode(final String prompt) {
        this.prompt = prompt;
    }

    public abstract CommandMode processCommand(final CommandParser parser, final String text);

    public String getPrompt() {
        return this.prompt;
    }
}
