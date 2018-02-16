package grassmarlin.plugins;

public class PluginNotFoundException extends Exception {
    private String plugin;

    public PluginNotFoundException(String message, String plugin, Throwable cause) {
        super(message, cause);

        this.plugin = plugin;
    }

    public PluginNotFoundException(String message, String plugin) {
        super(message);

        this.plugin = plugin;
    }

    public PluginNotFoundException(String plugin, Throwable cause) {
        super(cause);

        this.plugin = plugin;
    }

    public PluginNotFoundException(String plugin) {
        super();

        this.plugin = plugin;
    }

    public String getPlugin() {
        return this.plugin;
    }
}
