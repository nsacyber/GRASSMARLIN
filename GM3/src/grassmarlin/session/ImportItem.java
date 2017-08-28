package grassmarlin.session;

import grassmarlin.common.FileUnits;
import grassmarlin.common.fxobservables.FxBooleanProperty;
import grassmarlin.common.fxobservables.FxFileParseProgressProperty;
import grassmarlin.common.fxobservables.FxLongProperty;
import grassmarlin.session.serialization.XmlSerializable;
import javafx.beans.binding.NumberExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class ImportItem implements XmlSerializable {
    /**
     * Implementation to handle everything that isn't live PCAP.
     */
    public static class FromPlugin extends ImportItem {
        private final NumberExpression progress;

        public FromPlugin(final XMLStreamReader reader) throws XMLStreamException {
            super(Paths.get(reader.getAttributeValue(null, "path")), reader.getAttributeValue(null, "entry"));

            super.readFromXml(reader);


            //If we ran out of content before the end element, there is an error and the progress is the least of our worries.  Raptors are at the top of the list along with the inland taipan, while swimming dogs with handguns are somewhere near the middle.  Progress is the bottom of the list.
            if(isImportCompleted.get()) {
                this.progress = new SimpleDoubleProperty(1.0);
            } else {
                this.progress = new SimpleDoubleProperty(0.0);
            }
        }

        public FromPlugin(final Path path, final String entryPoint) {
            super(path, entryPoint);

            try {
                this.size.set(Files.size(path));
            } catch (IOException ex) {
                this.size.set(-1);
            }

            if(this.size.get() != -1) {
                this.progress = new FxFileParseProgressProperty(this.size.get());
            } else {
                //TODO: some alternative property.
                this.progress = null;
            }
        }

        @Override
        public NumberExpression progressProperty() {
            return this.progress;
        }

        @Override
        public void recordProgress(final long amount) {
            if(this.progress instanceof FxFileParseProgressProperty) {
                ((FxFileParseProgressProperty)this.progress).recordProgress(amount);
            } else {
                //TODO: Re-evaluate why this might happen and handle this case better.
                System.err.println("Attempting to record progress in invalid property type.");
            }
        }

        @Override
        public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
            super.writeToXml(target);
            target.writeStartElement("FromPlugin");
            if(this.progress == null) {
                target.writeAttribute("progress", "1.0");
            } else {
                target.writeAttribute("progress", Double.toString(progress.doubleValue()));
            }
            target.writeEndElement();
        }
    }

    protected final Path path;
    protected final StringProperty pipelineEntry;
    protected final LongProperty size;

    protected final StringProperty nameImporterPlugin;
    protected final StringProperty nameImporterFunction;

    protected final BooleanProperty isImportStarted;
    protected final BooleanProperty isImportCompleted;

    protected ImportItem(final Path path, final String entryPoint) {
        this.path = path;
        this.pipelineEntry = new SimpleStringProperty(entryPoint);

        this.size = new FxLongProperty(0);

        this.nameImporterPlugin = new SimpleStringProperty(null) {
            @Override
            public void set(final String value) {
                if(ImportItem.this.isImportStarted.get()) {
                    return;
                } else {
                    super.set(value);
                }
            }
        };
        this.nameImporterFunction = new SimpleStringProperty(null) {
            @Override
            public void set(final String value) {
                if(ImportItem.this.isImportStarted.get()) {
                    return;
                } else {
                    super.set(value);
                }
            }
        };

        this.isImportStarted = new FxBooleanProperty(false);
        this.isImportCompleted = new FxBooleanProperty(false);
    }

    //Properties / Accessors for various UI elements
    public abstract NumberExpression progressProperty();

    public Path getPath() {
        return this.path;
    }
    public StringProperty pipelineEntryProperty() {
        return this.pipelineEntry;
    }

    public LongProperty sizeProperty() {
        return this.size;
    }
    public ObjectExpression<FileUnits.FileSize> displaySizeProperty() {
        return new FileUnits.Binding(this.size);
    }


    public BooleanProperty importStartedProperty() {
        return this.isImportStarted;
    }
    public BooleanProperty importCompleteProperty() {
        return this.isImportCompleted;
    }

    public abstract void recordProgress(final long amount);

    public StringProperty importerPluginNameProperty() {
        return this.nameImporterPlugin;
    }
    public StringProperty importerFunctionNameProperty() {
        return this.nameImporterFunction;
    }

    @Override
    public String toString() {
        return String.format("{%s/%s:%s}",
                path.toString(),
                this.nameImporterPlugin.get() == null ? "" : this.nameImporterPlugin.get(),
                this.nameImporterFunction.get() == null ? "" : this.nameImporterFunction.get());
    }

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        //path had to be dealt with already
        //entry had to be provided in the constructor, but it may not match the content being loaded, so we will force the update
        this.pipelineEntry.set(source.getAttributeValue(null, "entry"));
        this.size.set(Long.parseLong(source.getAttributeValue(null, "size")));

        this.nameImporterPlugin.set(source.getAttributeValue(null, "importerPlugin"));
        this.nameImporterFunction.set(source.getAttributeValue(null, "importerFunction"));
        if("true".equals(source.getAttributeValue(null, "complete"))) {
            this.isImportCompleted.setValue(true);
            //If it is complete, it had to have started.  If it has started but no completed, we never should have saved it.
            this.isImportStarted.set(true);
        }
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeAttribute("path", path.toAbsolutePath().toString());
        target.writeAttribute("entry", this.pipelineEntry.get());
        target.writeAttribute("size", Long.toString(this.size.get()));

        if(this.nameImporterPlugin.get() != null) {
            target.writeAttribute("importerPlugin", this.nameImporterPlugin.get());
        }
        if(this.nameImporterFunction.get() != null) {
            target.writeAttribute("importerFunction", this.nameImporterFunction.get());
        }

        if(isImportCompleted.get()) {
            target.writeAttribute("complete", "true");
        }
    }
}
