package core.fingerprint;


import core.fingerprint3.Fingerprint;

import java.nio.file.Path;

public class FingerprintState {
    private Boolean dirty;
    private Boolean enabled;
    private Path path;
    private final Fingerprint fingerprint;

    public FingerprintState(Fingerprint fp, Path savePath) {
        this.dirty = false;
        this.enabled = false;
        this.path = savePath;
        this.fingerprint = fp;
    }

    public FingerprintState(Fingerprint fp) {
        this(fp, null);
    }


    public Boolean getDirtyProperty() {
        return this.dirty;
    }
    public void setDirtyProperty(Boolean dirty) {
        this.dirty = dirty;
    }

    public Boolean getEnabledProperty() {
        return this.enabled;
    }
    public void setEnabledProperty(Boolean enabled) {
        this.enabled = enabled;
    }

    public Path getPathProperty() {
        return this.path;
    }

    public void setPathProperty(Path path) {
        this.path = path;
    }

    public Fingerprint getFingerprint() {
        return this.fingerprint;
    }


    @Override
    public int hashCode() {
        //paths should be unique
        return this.path.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other instanceof FingerprintState) {
            Path p = this.path;
            String name = this.fingerprint.getHeader().getName();

            Path otherP = ((FingerprintState) other).path;
            String otherName = ((FingerprintState) other).fingerprint.getHeader().getName();

            //if both have paths than the path must match
            //if neither have paths than the name must match
            //otherwise not equal
            if (p != null && otherP != null) {
                return (p.equals(otherP));
            } else if (p == null && otherP == null) {
                return name.equals(otherName);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean equals(String name, Path path) {
        boolean equals;
        if (path != null && this.path != null) {
            equals = this.path.equals(path);
        } else if (name != null) {
            equals = this.fingerprint.getHeader().getName().equals(name);
        } else {
            equals = false;
        }

        return equals;
    }
}
