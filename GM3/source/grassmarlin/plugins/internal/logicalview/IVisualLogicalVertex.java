package grassmarlin.plugins.internal.logicalview;

public interface IVisualLogicalVertex {
    double getTranslateX();
    double getTranslateY();

    void setTranslateX(final double x);
    void setTranslateY(final double y);

    boolean isSubjectToLayout();
    GraphLogicalVertex getVertex();
}
