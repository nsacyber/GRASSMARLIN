package iadgov.fingerprint.processor;


import core.fingerprint3.Fingerprint;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FingerprintState implements Serializable {
    private transient BooleanProperty dirty;
    private ObjectProperty<Path> path;
    private Fingerprint fingerprint;

    // just for serialization
    private FingerprintState() {
    }

    public FingerprintState(Fingerprint fp, Path savePath) {
        this.dirty = new SimpleBooleanProperty(false);
        this.path = new SimpleObjectProperty<>(savePath);
        this.fingerprint = fp;
    }

    public FingerprintState(Fingerprint fp) {
        this(fp, null);
    }


    public BooleanProperty dirtyProperty() {
        return this.dirty;
    }

    public ObjectProperty<Path> pathProperty() {
        return this.path;
    }

    public Fingerprint getFingerprint() {
        return this.fingerprint;
    }


    @Override
    public int hashCode() {
        //paths should be unique
        return this.path.get().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof FingerprintState) {
            Path otherP = ((FingerprintState) other).path.get();
            String otherName = ((FingerprintState) other).fingerprint.getHeader().getName();

            return this.equals(otherName, otherP);
        } else {
            return false;
        }
    }

    public boolean equals(String name, Path path) {
        boolean equals;

        //if both have paths than the path must match
        //if neither have paths than the name must match
        //otherwise not equal
        if (path != null && this.path.get() != null) {
            equals = this.path.get().equals(path);
        } else if (name != null) {
            equals = this.fingerprint.getHeader().getName().equals(name);
        } else {
            equals = false;
        }

        return equals;
    }

    // custom serialization read and write methods because SimpleObjectProperty is not Serializable
    private void writeObject(ObjectOutputStream stream) throws IOException {
        String path = this.path.get().toAbsolutePath().toString();

        stream.writeObject(path);
        stream.writeObject(this.fingerprint);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        String path = (String) stream.readObject();
        Fingerprint fingerprint = (Fingerprint) stream.readObject();

        this.dirty = new SimpleBooleanProperty(false);
        this.fingerprint = fingerprint;
        this.path = new SimpleObjectProperty<>(Paths.get(path));

    }
}
