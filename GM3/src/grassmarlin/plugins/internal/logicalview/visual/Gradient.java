package grassmarlin.plugins.internal.logicalview.visual;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.scene.paint.Color;

public class Gradient {
    private final ObjectExpression<Color> start;
    private final ObjectExpression<Color> stop;

    public Gradient(final ObjectExpression<Color> start, final ObjectExpression<Color> stop) {
        this.start = start;
        this.stop = stop;
    }

    public ObjectBinding<Color> colorAt(final double offset) {
        return new ObjectBinding<Color>() {
            {
                super.bind(Gradient.this.start, Gradient.this.stop);
            }

            public Color computeValue() {
                if(start.get() instanceof Color && stop.get() instanceof Color) {
                    final Color startColor = start.get();
                    final Color stopColor = stop.get();

                    double dH = stopColor.getHue() - startColor.getHue();
                    if(dH > 180.0) {
                        dH -= 360.0;
                    } else if(dH < -180.0) {
                        dH += 360.0;
                    }
                    final double dS = stopColor.getSaturation() - startColor.getSaturation();
                    final double dB = stopColor.getBrightness() - startColor.getBrightness();
                    double hueBaseline = startColor.getHue();
                    //When fading to/from black, maintain Hue
                    if(startColor.equals(Color.BLACK)) {
                        dH = 0;
                        hueBaseline = stopColor.getHue();
                    } else if(stopColor.equals(Color.BLACK)) {
                        dH = 0;
                    }
                    return Color.hsb(
                            hueBaseline + dH * offset,
                            startColor.getSaturation() + dS * offset,
                            startColor.getBrightness() + dB * offset,
                            startColor.getOpacity() + (stopColor.getOpacity() - startColor.getOpacity()) * offset);
                } else {
                    return Color.BLACK;
                }
            }
        };
    }
}
