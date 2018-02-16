package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class VertexColorAssignment {
    public static abstract class VertexColor {
        private final ObjectExpression<Color> paintBackground;
        private final ObjectExpression<Color> paintText;

        public VertexColor(final Color background, final Color text) {
            this.paintBackground = new ReadOnlyObjectWrapper<>(background);
            this.paintText = new ReadOnlyObjectWrapper<>(text);
        }
        public VertexColor(final ObjectExpression<Color> background, final ObjectExpression<Color> text) {
            this.paintBackground = background;
            this.paintText = text;
        }

        public ObjectExpression<Color> backgroundProperty() {
            return this.paintBackground;
        }
        public ObjectExpression<Color> textProperty() {
            return this.paintText;
        }
    }

    private final static Map<Class<?>, VertexColor> associations;

    static {
        associations = new HashMap<>();
        associations.put(Object.class, new VertexColor(RuntimeConfiguration.colorGraphElementBackgroundProperty(), RuntimeConfiguration.colorGraphElementTextProperty()) { });
    }

    public static ObjectExpression<Color> backgroundColorFor(final Class<?> clazz) {
        final Class<?> classBestFit = getBestMatchingClass(clazz);
        if(classBestFit == null) {
            //We should have an entry for Object, but if we don't, then null is the correct result.
            return null;
        } else {
            return associations.get(classBestFit).paintBackground;
        }
    }

    public static ObjectExpression<Color> textColorFor(final Class<?> clazz) {
        final Class<?> classBestFit = getBestMatchingClass(clazz);
        if(classBestFit == null) {
            //We should have an entry for Object, but if we don't, then null is the correct result.
            return null;
        } else {
            return associations.get(classBestFit).paintText;
        }
    }

    private static Class<?> getBestMatchingClass(final Class<?> clazz) {
        final VertexColor resultExact = associations.get(clazz);
        if(resultExact != null) {
            return clazz;
        }

        return associations.keySet().stream()
                .filter(c -> c.isAssignableFrom(clazz))
                .sorted((c1, c2) -> c1.isAssignableFrom(c2) ? 1 : -1)
                .findFirst().orElse(null);
    }

    public static <T extends VertexColor> boolean defineColorMapping(final Class<?> clazz, final T color) {
        if(color.getClass().getClassLoader() != clazz.getClassLoader()) {
            Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Cannot define a color mapping for %s; the class loader for the target class must match that of the color.", clazz.getName());
            return false;
        }
        associations.put(clazz, color);
        return true;
    }
}
