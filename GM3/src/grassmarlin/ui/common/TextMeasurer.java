package grassmarlin.ui.common;

import com.sun.javafx.geom.BaseBounds;
import com.sun.javafx.scene.text.TextLayout;
import com.sun.javafx.scene.text.TextLayoutFactory;
import com.sun.javafx.tk.Toolkit;
import javafx.scene.text.Font;

public abstract class TextMeasurer {
    private static TextLayout layout;

    public static BaseBounds measureText(final String text, final Font font) {
        if(layout == null) {
            final TextLayoutFactory factory = Toolkit.getToolkit().getTextLayoutFactory();
            layout = factory.createLayout();
        }

        //HACK: There is no good way to do this; the native font solution has been assessed as the least bad option.
        //noinspection deprecation
        layout.setContent(text, font.impl_getNativeFont());
        return layout.getBounds();
    }
}
