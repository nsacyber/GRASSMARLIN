package core.fingerprint;

import TemplateEngine.Data.FunctionalFingerprint;
import java.nio.charset.Charset;

/**
 *
 */
public interface Fingerprint extends FunctionalFingerprint {
    public static final Charset CHARSET = Charset.forName("UTF-8");
    
    public final class Cursor implements TemplateEngine.Data.Cursor {
        int a, b, c;
        public Cursor() {
            a = b = c = 0;
        }
        @Override
        public int getA() {
            return a;
        }
        @Override
        public int getB() {
            return b;
        }
        @Override
        public int get() {
            return c;
        }
        @Override
        public void setA(int i) {
            a = i;
        }
        @Override
        public void setB(int i) {
            b = i;
        }
        @Override
        public void set(int i) {
            c = i;
        }
        @Override
        public void forward(int i) {
            c += i;
        }
        @Override
        public void reset() {
            a = b = c = 0;
        }
        @Override
        public String toString() {
            return String.format("Current[%d] A[%d] B[%d]", c, a, b);
        }
    }
    
    public Operation loadMethod(String methodName);
    
}
