package grassmarlin.ui.common;

import javafx.geometry.Point2D;

public class MutablePoint {
    private double x;
    private double y;

    public MutablePoint(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    public MutablePoint(final Point2D point) {
        this.x = point.getX();
        this.y = point.getY();
    }

    public double magnitude() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }
    public void normalize() {
        final double magnitude = this.magnitude();

        if(magnitude != 0.0) {
            this.x /= magnitude;
            this.y /= magnitude;
        }
    }
    public void multiply(final double value) {
        this.x *= value;
        this.y *= value;
    }
    public void multiply(final double x, final double y) {
        this.x *= x;
        this.y *= y;
    }
    public void add(final MutablePoint other) {
        this.x += other.x;
        this.y += other.y;
    }
    public void add(final double x, final double y) {
        this.x += x;
        this.y += y;
    }
    public void set(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return this.x;
    }
    public double getY() {
        return this.y;
    }
}
