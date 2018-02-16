package grassmarlin.ui.common;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.TransformationList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RevalidatingFilteredList<E> extends TransformationList<E, E> {

    private final ObservableList<E> listComputed;

    public RevalidatingFilteredList(final ObservableList<E> base, final Predicate<E> pred) {
        super(base);

        this.listComputed = new ObservableListWrapper<>(new ArrayList<>());
        this.setPredicate(pred);
    }
    public RevalidatingFilteredList(final ObservableList<E> base) {
        this(base, null);
    }

    private void recomputePredicate() {
        final List<E> valuesCorrect;
        if(this.getPredicate() == null) {
            valuesCorrect = new LinkedList<>(this.getSource());
        } else {
            valuesCorrect = this.getSource().stream().filter(this.predicate.get()).collect(Collectors.toList());
        }
        listComputed.retainAll(valuesCorrect);
        valuesCorrect.removeAll(listComputed);
        listComputed.addAll(valuesCorrect);
    }

    private ObjectProperty<Predicate<? super E>> predicate;

    public final ObjectProperty<Predicate<? super E>> predicateProperty() {
        if (predicate == null) {
            predicate = new ObjectPropertyBase<Predicate<? super E>>() {
                @Override
                protected void invalidated() {
                    RevalidatingFilteredList.this.recomputePredicate();
                }

                @Override
                public Object getBean() {
                    return RevalidatingFilteredList.this;
                }

                @Override
                public String getName() {
                    return "predicate";
                }

            };
        }
        return predicate;
    }

    public final Predicate<? super E> getPredicate() {
        return predicate == null ? null : predicate.get();
    }

    public final void setPredicate(Predicate<? super E> predicate) {
        predicateProperty().set(predicate);
    }

    @Override
    protected void sourceChanged(ListChangeListener.Change<? extends E> c) {
        recomputePredicate();
    }

    @Override
    public int getSourceIndex(int index) {
        return this.getSource().indexOf(this.get(index));
    }

    @Override
    public E get(int index) {
        return this.listComputed.get(index);
    }

    @Override
    public int size() {
        return this.listComputed.size();
    }

    public ObservableList<E> getComputedList() {
        return this.listComputed;
    }
}
