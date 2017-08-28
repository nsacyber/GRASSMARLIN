package iadgov.directorywatcher;

import javafx.beans.property.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class WatcherConfigInfo implements Serializable{
    private final ObjectProperty<Path> watchDir;
    private final BooleanProperty recursive;
    private final LongProperty modifyDelay;
    private final ObjectProperty<String> fileFilter;
    private Pattern compiledFilter;
    private final StringProperty entryPoint;

    public WatcherConfigInfo(Path watchDir, boolean recursive, long modifyDelay, String fileFilter, String entryPoint) {
        this.watchDir = new SimpleObjectProperty<>(watchDir);
        this.recursive = new SimpleBooleanProperty(recursive);
        this.modifyDelay = new SimpleLongProperty(modifyDelay);
        this.fileFilter = new SimpleObjectProperty<>(fileFilter);
        this.entryPoint = new SimpleStringProperty(entryPoint);

        this.fileFilterProperty().addListener(observable -> {
            if (getFileFilter().matches("\\s")) {
                setFileFilter("");
            }
            try {
                this.compiledFilter = Pattern.compile(getFileFilter());
            } catch(PatternSyntaxException e) {
                this.compiledFilter = null;
            }
        });
    }

    public WatcherConfigInfo() {
        this(null, false, 0, null, null);
    }

    public Path getWatchDir() {
        return watchDir.get();
    }

    public ObjectProperty<Path> watchDirProperty() {
        return watchDir;
    }

    public void setWatchDir(Path watchDir) {
        this.watchDir.set(watchDir);
    }

    public boolean isRecursive() {
        return recursive.get();
    }

    public BooleanProperty recursiveProperty() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive.set(recursive);
    }

    public long getModifyDelay() {
        return modifyDelay.get();
    }

    public LongProperty modifyDelayProperty() {
        return modifyDelay;
    }

    public void setModifyDelay(long modifyDelay) {
        this.modifyDelay.set(modifyDelay);
    }

    public String getFileFilter() {
        return fileFilter.get();
    }

    public ObjectProperty<String> fileFilterProperty() {
        return fileFilter;
    }

    public void setFileFilter(String fileFilter) {
        this.fileFilter.set(fileFilter);
    }

    public Pattern getCompiledFilter() {
        return this.compiledFilter;
    }

    public String getEntryPoint() {
        return entryPoint.get();
    }

    public StringProperty entryPointProperty() {
        return entryPoint;
    }

    public void setEntryPoint(String entryPoint) {
        this.entryPoint.set(entryPoint);
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(watchDir.get());
        stream.writeBoolean(recursive.get());
        stream.writeLong(modifyDelay.get());
        stream.writeObject(fileFilter.get());
        stream.writeObject(entryPoint.get());
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        Path watchDir = ((Path) stream.readObject());
        boolean recursive = stream.readBoolean();
        long modifyDelay = stream.readLong();
        String fileFilter = ((String) stream.readObject());
        String entryPoint = ((String) stream.readObject());

        this.watchDir.set(watchDir);
        this.recursive.set(recursive);
        this.modifyDelay.set(modifyDelay);
        this.fileFilter.set(fileFilter);
        this.entryPoint.set(entryPoint);
    }
}
