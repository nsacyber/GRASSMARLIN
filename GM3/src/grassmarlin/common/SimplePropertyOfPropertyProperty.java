package grassmarlin.common;

import javafx.beans.Observable;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import java.util.function.Function;

// If there is ever a need to expand the chain of references further, the next type should be anything but T3--TTheSarahConnorChronicles is a bit verbose, but a far superior choice to T3.  TGenesys is right out.
public class SimplePropertyOfPropertyProperty<T1, T2> extends SimpleObjectProperty<T2> {
    private final ObjectExpression<T1> property;
    private final Function<T1, ? extends ObjectExpression<T2>> fnGetInnerPropery;

    public SimplePropertyOfPropertyProperty(final ObjectExpression<T1> source, final Function<T1, ? extends ObjectExpression<T2>> fnGetInnerProperty) {
        this.property = source;
        this.fnGetInnerPropery = fnGetInnerProperty;

        //Add the listener then call immediately as if set from null.
        this.property.addListener(this.handlerSourceChanged);
        this.handleSourceChanged(this.property, null, this.property.get());
    }

    private final ChangeListener<T1> handlerSourceChanged = this::handleSourceChanged;
    private void handleSourceChanged(final Observable observable, final T1 oldValue, final T1 newValue) {
        if(oldValue != newValue) {
            if (oldValue != null) {
                this.unbind();
            }
            if (newValue != null) {
                this.bind(this.fnGetInnerPropery.apply(newValue));
            }
        }
    }

}
