package grassmarlin.common;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.binding.ObjectBinding;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ObservableListUnion<E> extends ObjectBinding<ObservableList<E>> {
    private List<List<E>> members;
    private final ObservableList<E> resultList;

    public ObservableListUnion(List<E>... members) {
        this.members = new LinkedList<>();
        this.resultList = new ObservableListWrapper<>(new LinkedList<>());

        for(final List<E> member : members) {
            this.members.add(member);
            if(member instanceof ObservableList) {
                super.bind((ObservableList)member);
            }
        }
    }

    @Override
    protected void onInvalidating() {
        super.onInvalidating();
        //When invalidated, we force a get() call because that will fire the events to update the list.
        //Unless consumers bind to the property, they will not be informed of changes otherwise.
        this.get();
    }

    public void removeList(final List<E> member) {
        if(this.members.remove(member)) {
            if(member instanceof ObservableList) {
                super.unbind((ObservableList)member);
            }
            this.invalidate();
        }
    }
    public void addList(final List<E> member) {
        if(!this.members.contains(member)) {
            this.members.add(member);
            if(member instanceof ObservableList) {
                super.bind((ObservableList)member);
            }
            this.invalidate();
        }
    }

    @Override
    public ObservableList<E> computeValue() {
        final ArrayList<E> itemsAll = new ArrayList<>();

        for(final List<E> member : this.members) {
            itemsAll.addAll(member);
        }

        this.resultList.retainAll(itemsAll);
        itemsAll.removeAll(this.resultList);
        this.resultList.addAll(itemsAll);

        return this.resultList;
    }
}
