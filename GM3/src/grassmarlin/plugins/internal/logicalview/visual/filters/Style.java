package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.RuntimeConfiguration;
import javafx.beans.property.DoubleProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Style {
    protected final static String CONFIG_ROOT = "grassmarlin.plugins.logicalview.style";
    public final static RuntimeConfiguration.IPersistedValue STYLE_LIST = new RuntimeConfiguration.IPersistedValue() {
        @Override
        public String getKey() {
            return "grassmarlin.plugins.logicalview.styles";
        }

        @Override
        public RuntimeConfiguration.IDefaultValue getFnDefault() {
            return () -> "";
        }
    };

    public final static double WEIGHT_MIN = 0f;
    public final static double WEIGHT_MAX = 10f;

    public final static double OPACITY_MIN = 0f;
    public final static double OPACITY_MAX = 1f;

    private final String name;
    protected final RuntimeConfiguration.IPersistedValue configWeight;
    protected final RuntimeConfiguration.IPersistedValue configOpacity;
    private final RuntimeConfiguration.PersistedDoubleProperty weight;
    private final RuntimeConfiguration.PersistedDoubleProperty opacity;

    private final static Map<String, Style> lookupStyles = new ConcurrentHashMap<>();
    public static Style getStyle(final String name) {
        lookupStyles.putIfAbsent(name, new Style(name));
        return lookupStyles.get(name);
    }

    private Style(final String name) {
        this.name = name.replace("|", "");
        this.configWeight = new RuntimeConfiguration.IPersistedValue() {
            @Override
            public String getKey() {
                return String.format("%s.%s.weight", CONFIG_ROOT, Style.this.name);
            }

            @Override
            public RuntimeConfiguration.IDefaultValue getFnDefault() {
                return () -> "1.0";
            }
        };
        this.configOpacity = new RuntimeConfiguration.IPersistedValue() {
            @Override
            public String getKey() {
                return String.format("%s.%s.opacity", CONFIG_ROOT, Style.this.name);
            }

            @Override
            public RuntimeConfiguration.IDefaultValue getFnDefault() {
                return () -> "1.0";
            }
        };
        this.weight = new RuntimeConfiguration.PersistedDoubleProperty(configWeight);
        this.opacity = new RuntimeConfiguration.PersistedDoubleProperty(configOpacity);
    }

    public DoubleProperty weightProperty() {
        return this.weight;
    }

    public DoubleProperty opacityProperty() {
        return this.opacity;
    }
    public String getName() {
        return this.name;
    }

    public void clearDataCache() {
        RuntimeConfiguration.clearPersistedString(this.configWeight);
        RuntimeConfiguration.clearPersistedString(this.configOpacity);
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
