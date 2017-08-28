package grassmarlin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Logger {
    public enum Severity {
        ERROR,
        WARNING,
        COMPLETION,
        INFORMATION,
        PEDANTIC_DEVELOPER_SPAM
    }
    public static class Message {
        private final Severity severity;
        private final int volume;
        private final String text;
        private Instant tsCreated;

        public Message(final Severity severity, final int volume, final String text) {
            this.severity = severity;
            this.volume = volume;
            this.text = text;
            this.tsCreated = Instant.now();
        }

        public Severity getSeverity() {
            return this.severity;
        }
        public String getText() {
            return this.text;
        }
        public Instant getTimestamp() {
            return this.tsCreated;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s - %s",
                    this.tsCreated.atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_DATE_TIME),
                    this.severity,
                    this.text);
        }
    }
    public class MessageEventArgs {
        private final Message message;

        MessageEventArgs(Message message) {
            this.message = message;
        }

        public Message getMessage() {
            return this.message;
        }
    }

    private static Logger instance;

    public Event<MessageEventArgs> onMessage = new Event<>(Event.PROVIDER_IN_THREAD);  //Messages are not logged to the UI thread; they are fired in-thread; listeners will have to hand off to another thread if needed.

    private Logger() { }

    public static void log(final Severity severity, final int volume, final String format, final Object... params) {
        if(instance == null) {
            instance = new Logger();
        }
        instance.onMessage.call(instance.new MessageEventArgs(new Message(severity, volume, String.format(format, params))));
    }

    public static void log(final Severity severity, final String format, final Object... params) {
        log(severity, 0, format, params);
    }

    public static Logger getInstance() {
        if(instance == null) {
            instance = new Logger();
        }
        return instance;
    }
}
