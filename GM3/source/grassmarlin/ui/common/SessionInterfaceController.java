package grassmarlin.ui.common;

import com.sun.istack.internal.NotNull;
import grassmarlin.Event;
import grassmarlin.common.edit.IActionStack;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;

import java.util.List;

/**
 * The SessionInterfaceController (formerly TabController) manages the UI elements associated with a particular View.
 *
 * Each View consists of:
 *  - A Pane of content
 *  - A TreeItem (nullable)
 *  - A List of MenuItems (nullable, can be empty)
 *  - An ActionStack (nullable, can be a property)
 *  - TODO: Clipboard support
 *
 *  Each of these elements is tightly coupled to a specific Session, the SessionInterfaceController manages all of the interfaces for a single Session.
 *
 *  The goal is that the UI can modify itself based on the current View, incorporating the requisite UI elements.
 *
 *  TODO: This should also track state information for the current Session as SingleDocumentState currently does?
 */
public interface SessionInterfaceController {
    class View {
        private final StringExpression name;
        private final Node content;
        private final TreeItem<Object> root;
        private final ObjectExpression<? extends IActionStack> undoBuffer;
        private final List<MenuItem> viewMenuItems;

        /**
         * A dismissable View can be hidden by the user, using a mechanism chosen by the UI that manages the SessionInterfaceController.
         */
        private final boolean dismissable;

        /**
         * Temporary Views are destroyed instead of being dismissed.  A non-dismissable view that is also temporary isn't wrong, but is bizarre.
         */
        private final boolean temporary;

        /**
         * Constructs a simple View that has only a name and content.
         */
        public static View simple(final String name, final Node content) {
            return simple(name, content, null);
        }

        /**
         * Constructs a simple View with a name, content, and a tree, but no undo capability, menu items, etc.
         */
        public static View simple(final String name, final Node content, final TreeItem<Object> root) {
            return new View(new ReadOnlyStringWrapper(name), content, root, new ReadOnlyObjectWrapper<>(null), null, true, false);
        }

        public View(
                @NotNull final StringExpression name,
                @NotNull final Node content,
                final TreeItem<Object> root,
                @NotNull final ObjectProperty<? extends IActionStack> undoBuffer,
                final List<MenuItem> viewMenuItems,
                final boolean dismissable,
                final boolean temporary) {
            this.name = name;
            this.content = content;
            this.root = root;
            this.undoBuffer = undoBuffer;
            this.viewMenuItems = viewMenuItems;

            this.dismissable = dismissable;
            this.temporary = temporary;
        }

        @NotNull
        public StringExpression titleProperty() {
            return this.name;
        }

        @NotNull
        public Node getContent() {
            return this.content;
        }

        public TreeItem<Object> getNavigationRoot() {
            return this.root;
        }

        @NotNull
        public ObjectExpression<? extends IActionStack> undoBufferProperty() {
            return this.undoBuffer;
        }

        public List<MenuItem> getViewMenuItems() {
            return this.viewMenuItems;
        }

        public boolean isDismissable() {
            return this.dismissable;
        }

        public boolean isTemporary() {
            return this.temporary;
        }

        public void onDestroy() {
            //In a derived class, this can clean up resources.
            //It will be called when the View is being removed from the SessionInterfaceController
        }
    }

    Event.IAsyncExecutionProvider getUiExecutionProvider();

    void createView(final View view, final boolean visible);
    void setViewVisible(final View view, final boolean visible);
    void removeView(final View view);

    void invalidateSession();

    IActionStack registerActionStack(final IActionStack stackRoot);
}
