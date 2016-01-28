package TemplateEngine.Data;

/**
 * Created by BESTDOG on 11/24/2015.
 *
 * Simple cursor implementation with three index.
 *
 * Three indexes is the minimal amount of cursors required to to exacting field extraction.
 */
public interface Cursor {
    int getA();
    int getB();
    int get();
    void setA(int i);
    void setB(int i);
    void set(int i);
    void forward(int i);
    void reset();
}
