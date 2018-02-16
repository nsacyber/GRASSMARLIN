package grassmarlin.ui.sdi;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.common.edit.IActionStack;
import grassmarlin.ui.common.ActionStackPropertyWrapper;
import grassmarlin.ui.common.SessionInterfaceController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;

import java.util.LinkedList;

public class SdiSessionInterfaceController implements SessionInterfaceController {
    private final ActionStackPropertyWrapper masterActionStack;
    private final SingleDocumentState documentState;

    private final SimpleObjectProperty<View> currentView;
    private final ObservableList<View> displayedViews;
    private final ObservableList<View> hiddenViews;

    public SdiSessionInterfaceController(final ActionStackPropertyWrapper masterActionStack, final SingleDocumentState documentState) {
        this.masterActionStack = masterActionStack;
        this.documentState = documentState;

        this.currentView = new SimpleObjectProperty<>();
        this.displayedViews = new ObservableListWrapper<>(new LinkedList<>());
        this.hiddenViews = new ObservableListWrapper<>(new LinkedList<>());
    }

    public ObjectProperty<View> currentViewProperty() {
        return this.currentView;
    }

    @Override
    public Event.IAsyncExecutionProvider getUiExecutionProvider() {
        return Event.PROVIDER_JAVAFX;
    }

    @Override
    public void createView(View view, final boolean visible) {
        if(visible) {
            this.displayedViews.add(view);
            if(this.currentView.get() == null) {
                this.currentView.set(view);
            }
        } else {
            this.hiddenViews.add(view);
        }
    }

    @Override
    public void setViewVisible(final View view, final boolean visible) {
        if(visible) {
            if(hiddenViews.remove(view)) {
                displayedViews.add(view);
            }
        } else {
            if(displayedViews.remove(view)) {
                hiddenViews.add(view);
            }
        }
    }

    @Override
    public void removeView(View view) {
        //There might be a chance we receive an event to remove a view from a different controller.
        //Really that situation is a race condition, but we can handle it by ignoring the request.
        if(this.displayedViews.contains(view) || this.hiddenViews.contains(view)) {
            if (this.currentView.get() == view) {
                final int idxRemoved = this.displayedViews.indexOf(view);

                if (idxRemoved - 1 >= 0) {
                    this.currentView.set(this.displayedViews.get(idxRemoved - 1));
                } else if (idxRemoved + 1 < this.displayedViews.size()) {
                    this.currentView.set(this.displayedViews.get(idxRemoved + 1));
                } else {
                    //If the previous and next are both invalid, then set to null.
                    this.currentView.set(null);
                }
            }

            this.displayedViews.remove(view);
            this.hiddenViews.remove(view);

            view.onDestroy();
        }
    }

    @Override
    public void invalidateSession() {
        this.documentState.invalidate();
    }

    @Override
    public IActionStack registerActionStack(final IActionStack stackRoot) {
        return masterActionStack.wrapActionStack(stackRoot);
    }

    public ObservableList<View> getVisibleViews() {
        return this.displayedViews;
    }
    public ObservableList<View> getHiddenViews() {
        return this.hiddenViews;
    }
}
