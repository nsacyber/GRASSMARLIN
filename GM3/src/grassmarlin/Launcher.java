package grassmarlin;

import grassmarlin.ui.cli.CommandParser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Map;

public class Launcher {
    private static final RuntimeConfiguration configuration = new RuntimeConfiguration();
    private static Path pathLogFile;

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            System.err.println(String.format("Unhandled error in thread %s: %s", thread.getName(), Arrays.toString(throwable.getStackTrace())));
        });

        configuration.parseArgs(args);

        try {
            configuration.createUserDataDirectories();
            configuration.createAppDataDirectories();
        } catch(Exception ex) {
            System.out.println(String.format("Unable to start GrassMarlin:  There was an error initializing the runtime directories (%s)", ex.getMessage()));
        }

        Event.EventListener<Logger.MessageEventArgs> handlerFileLogging = null;

        switch(configuration.getUiMode()) {
            case CONSOLE:
                handlerFileLogging = initializeLoggingToFile();
                configuration.loadPlugins();
                launchConsole();
                break;
            case SDI:
                handlerFileLogging = initializeLoggingToFile();
                configuration.loadPlugins();
                launchSdi();
                break;
            case DIAGNOSTIC:
                launchDiagnostic();
                break;
            default:
                System.out.println("Unsupported UI mode: " + configuration.getUiMode());
        }

        if(handlerFileLogging != null) {
            Logger.getInstance().onMessage.removeHandler(handlerFileLogging);
        }
    }

    public static RuntimeConfiguration getConfiguration() {
        return configuration;
    }

    private static Event.EventListener<Logger.MessageEventArgs> initializeLoggingToFile() {
        pathLogFile = Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_APP_DATA), "logs", Instant.now().atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_DATE_TIME).replaceAll("[^0-9TZ]", "") + ".log").toAbsolutePath();
        try {
            final BufferedWriter writer = Files.newBufferedWriter(pathLogFile, Charset.defaultCharset());
            Event.EventListener<Logger.MessageEventArgs> result = Logger.getInstance().onMessage.addHandler((source, arguments) -> {
                try {
                    synchronized(writer) {
                        writer.write(String.format("%s - %s - %s", arguments.getMessage().getTimestamp(), arguments.getMessage().getSeverity(), arguments.getMessage().getText()));
                        writer.newLine();
                        writer.flush();
                    }
                } catch(IOException ex) {
                    //Ignore it--there isn't anything we can do at this point.
                }
            });
            Logger.log(Logger.Severity.INFORMATION, "Logging to file: %s", pathLogFile.toAbsolutePath());

            return result;
        } catch(InvalidPathException | IOException ex) {
            Logger.log(Logger.Severity.ERROR, "There was an error initializing the log file: %s", ex.getMessage());
            return null;
        }
    }

    private static void launchConsole() {
        Logger.getInstance().onMessage.addHandler((source, arguments) -> System.err.println(arguments.getMessage().toString()));
        new CommandParser(configuration).executeUi();
    }

    private static void launchSdi() {
        configuration.setUiEventProvider(Event.PROVIDER_JAVAFX);
        grassmarlin.ui.sdi.MainWindow.launchFx(null);
    }

    private static void launchDiagnostic() {
        System.out.println(Version.APPLICATION_TITLE + "r" + Version.APPLICATION_REVISION + " Diagnostic Mode");
        System.out.println("ENVIRONMENT:");
        for(Map.Entry<String, String> env : System.getenv().entrySet()) {
            System.out.println(String.format("  [%s]: %s", env.getKey(), env.getValue()));
        }
        System.out.println("SYSTEM:");
        for(Map.Entry<Object, Object> property : System.getProperties().entrySet()) {
            System.out.println(String.format("  [%s]: %s", property.getKey(), property.getValue().toString().replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r")));
        }
        System.out.println("CONFIGURATION:");
        System.out.println(configuration.toString());


        System.out.println("To execute GRASSMARLIN specify the 'SDI' or 'CONSOLE' option with the -ui parameter.");
    }

    public static Path getLogFilePath() {
        return pathLogFile;
    }
}
