package grassmarlin.plugins.internal.logicalview;

import grassmarlin.plugins.internal.logicalview.visual.LogicalVisualization;
import grassmarlin.plugins.internal.logicalview.visual.colorfactories.ConstantColorFactory;
import grassmarlin.plugins.internal.logicalview.visual.colorfactories.HashColorFactory;
import grassmarlin.plugins.internal.logicalview.visual.colorfactories.ReferenceColorFactory;
import grassmarlin.ui.common.menu.CheckColorSelectionMenuItem;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.function.Supplier;

public class ColorFactoryMenuItem extends Menu {
    public interface IColorFactory extends LogicalVisualization.ICalculateColorsForAggregate{
        CheckMenuItem getMenuItem();
    }

    private List<Supplier<IColorFactory>> colorFactoryFactories;

    {
        colorFactoryFactories = new LinkedList<>();

        colorFactoryFactories.addAll(Arrays.asList(
                () -> new IColorFactory() {
                    final CheckMenuItem mi = new CheckMenuItem("Compute From Group");
                    final HashColorFactory factory = new HashColorFactory();

                    @Override
                    public CheckMenuItem getMenuItem() {
                        return mi;
                    }

                    @Override
                    public Color getBackgroundColor(final Object o) {
                        return factory.getBackgroundColor(o);
                    }

                    @Override
                    public Color getBorderColor(final Object o) {
                        return factory.getBorderColor(o);
                    }
                },
                () -> new IColorFactory() {
                    final ConstantColorFactory factory = new ConstantColorFactory();
                    final CheckColorSelectionMenuItem mi = new CheckColorSelectionMenuItem(factory.colorProperty(), "Fixed Color");

                    @Override
                    public CheckMenuItem getMenuItem() {
                        return mi;
                    }

                    @Override
                    public Color getBackgroundColor(final Object o) {
                        return factory.getBackgroundColor(o);
                    }

                    @Override
                    public Color getBorderColor(final Object o) {
                        return factory.getBorderColor(o);
                    }
                }
        ));
    }
    public void addFactory(final Supplier<IColorFactory> factory) {
        colorFactoryFactories.add(factory);
    }

    private final ReferenceColorFactory factory;
    private final Map<CheckMenuItem, LogicalVisualization.ICalculateColorsForAggregate> items;

    public ColorFactoryMenuItem(final String title) {
        super(title);

        this.items = new LinkedHashMap<>();

        for(Supplier<IColorFactory> supplier : colorFactoryFactories) {
            final IColorFactory factory = supplier.get();
            this.items.put(factory.getMenuItem(), factory);
        }

        Map.Entry<CheckMenuItem, LogicalVisualization.ICalculateColorsForAggregate> entryFirst = null;
        for(Map.Entry<CheckMenuItem, LogicalVisualization.ICalculateColorsForAggregate> entry : this.items.entrySet()) {
            if(entryFirst == null) {
                entryFirst = entry;
            }

            entry.getKey().setSelected(false);
            entry.getKey().addEventHandler(ActionEvent.ACTION, event -> {
                ColorFactoryMenuItem.this.factory.valueProperty().set(entry.getValue());
                for(final CheckMenuItem item : ColorFactoryMenuItem.this.items.keySet()) {
                    item.setSelected(item == entry.getKey());
                }
            });
        }
        this.getItems().addAll(this.items.keySet());

        if(entryFirst != null) {
            this.factory = new ReferenceColorFactory(entryFirst.getValue());
            entryFirst.getKey().setSelected(true);
        } else {
            this.factory = new ReferenceColorFactory(new HashColorFactory());
        }

        this.setOnShowing(event -> {
            while(ColorFactoryMenuItem.this.items.size() < ColorFactoryMenuItem.this.colorFactoryFactories.size()) {
                final IColorFactory factoryCurrent = ColorFactoryMenuItem.this.colorFactoryFactories.get(ColorFactoryMenuItem.this.items.size()).get();
                this.items.put(factoryCurrent.getMenuItem(), factoryCurrent);

                factoryCurrent.getMenuItem().setSelected(false);
                factoryCurrent.getMenuItem().addEventHandler(ActionEvent.ACTION, event2 -> {
                    ColorFactoryMenuItem.this.factory.valueProperty().set(factoryCurrent);
                    for(final CheckMenuItem item : ColorFactoryMenuItem.this.items.keySet()) {
                        item.setSelected(item == factoryCurrent.getMenuItem());
                    }
                });

                this.getItems().add(factoryCurrent.getMenuItem());
            }
        });
    }

    public ReferenceColorFactory getFactory() {
        return this.factory;
    }
}
