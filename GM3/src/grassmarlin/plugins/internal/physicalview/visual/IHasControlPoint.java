package grassmarlin.plugins.internal.physicalview.visual;

import javafx.beans.binding.DoubleExpression;

public interface IHasControlPoint {
    DoubleExpression getTerminalX();
    DoubleExpression getTerminalY();
    DoubleExpression getControlX();
    DoubleExpression getControlY();
}
