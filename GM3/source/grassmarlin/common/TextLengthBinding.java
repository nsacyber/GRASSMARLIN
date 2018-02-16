package grassmarlin.common;

import grassmarlin.ui.common.TextMeasurer;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.scene.text.Font;

public class TextLengthBinding extends DoubleBinding {
    private final ObjectProperty<Font> font;
    private final StringProperty text;

    public TextLengthBinding(final ObjectProperty<Font> font, final StringProperty text) {
        this.font = font;
        this.text = text;

        super.bind(font, text);
    }

    public TextLengthBinding(final ObjectProperty<Font> font, final String text) {
        this.font = font;
        this.text = new ReadOnlyStringWrapper(text);

        super.bind(font);
    }

    public TextLengthBinding(final Font font, final StringProperty text) {
        this.font = new ReadOnlyObjectWrapper<>(font);
        this.text = text;

        super.bind(text);
    }
    @Override
    protected double computeValue() {
        return TextMeasurer.measureText(this.text.get(), this.font.get()).getWidth();
    }
}
