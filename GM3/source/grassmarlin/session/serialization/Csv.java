package grassmarlin.session.serialization;

import grassmarlin.Logger;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Csv {
    private static String fieldFromString(final Object source) {
        if(source == null) {
            return "";
        }
        final String text = source.toString();
        if(text.startsWith("\"") || text.contains(",") || text.contains("\r") || text.contains("\n")) {
            return String.format("\"%s\"", text.replace("\"", "\"\"").replaceAll("(?<!\\r)\\n", "\r\n"));
        } else {
            return text;
        }
    }

    public static <TRow> void fromTable(final TableView<TRow> table, final Path destination) {
        boolean bFirst;
        try(BufferedWriter writer = Files.newBufferedWriter(destination, StandardCharsets.UTF_8)) {
            //Headers
            bFirst = true;
            for(final TableColumn<TRow, ?> col : table.getColumns()) {
                if(!bFirst) {
                    writer.write(",");
                }
                bFirst = false;
                writer.write(fieldFromString(col.getText()));
            }
            writer.write("\r\n");

            //Content
            for(final TRow row : table.getItems()) {
                bFirst = true;
                for(TableColumn<TRow, ?> col : table.getColumns()) {
                    if(!bFirst) {
                        writer.write(",");
                    }
                    bFirst = false;
                    final ObservableValue val = col.getCellObservableValue(row);
                    if(val != null) {
                        final Object content = val.getValue();
                        if(content != null) {
                            writer.write(fieldFromString(content.toString()));
                        }
                    }
                }
                writer.write("\r\n");
            }

            Logger.log(Logger.Severity.COMPLETION, "Export to '%s' successful.", destination.getFileName());
        } catch(IOException ex) {
            Logger.log(Logger.Severity.ERROR, "Unable to export to CSV: %s", ex.getMessage());
        }
    }
}
