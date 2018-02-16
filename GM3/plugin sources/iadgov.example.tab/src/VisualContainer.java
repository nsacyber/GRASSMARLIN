package iadgov.example.tab;

import grassmarlin.common.edit.IActionStack;
import grassmarlin.ui.common.SessionInterfaceController;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * GrassMarlin 3.3 displays a set of Views to the user.
 *
 * A View is relatively abstract but has concrete components.  How they are used is left up to the UI wrapper.
 * Presently, Views are only used with JavaFx and provide a TreeItem&lt;Object&gt; and Node for the main UI,
 * along with menu items to display in the View menu.
 *
 * If an ActionStack is provided, it will be tied to the Edit menu's Undo and Redo functionality.
 * The ActionStack is provided as an ObjectProperty; in the case of Sokoban's VisualContainer,
 * the value in the property is changed by the TreeItems.
 *
 *
 */
public class VisualContainer extends SessionInterfaceController.View {
    private final SessionInterfaceController controller;

    public VisualContainer(final SessionInterfaceController controller) {
        super(
                new ReadOnlyStringWrapper(Plugin.NAME),     // The title has to be reported as a StringExpression so that change events can be fired.  We could provide a SimpleStringProperty and change the tab name in response to the changing configuration.
                new Visualization(),
                new TreeItem<>(),                           // The root item isn't visible, but we will add more items as its children.
                new SimpleObjectProperty<>(null),           // Each configuration has its own action stack (undo buffer).  We will change hte value of the property when a configuration is selected.
                null,
                true,                                       // Can be closed
                false                                       // Is hidden when closed
        );

        this.controller = controller;

        //<editor-fold desc="Define Configurations">
        final Configuration[] CONFIGURATIONS = {
                new Configuration(this, "1",
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                xxxxxxx                 " +
                        "                x     x                 " +
                        "                x  .  x                 " +
                        "                x  n  x                 " +
                        "                x  o  x                 " +
                        "                x     x                 " +
                        "                xxxxxxx                 " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                ),
                new Configuration(this, "2",
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                   xxxx                 " +
                        "                 xxx  x                 " +
                        "                 x n  x                 " +
                        "                 x o  x                 " +
                        "                 x .  x                 " +
                        "                 xxxxxx                 " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                ),
                new Configuration(this, "3",
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                xxxxx                   " +
                        "                x   x xxx               " +
                        "                x n x xox               " +
                        "                x n xxxox               " +
                        "                xxx .  ox               " +
                        "                 xx  x  x               " +
                        "                 x n xxxx               " +
                        "                 x   x                  " +
                        "                 xxxxx                  " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                ),
                new Configuration(this, "4",
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                      xxxxx             " +
                        "                    xxx   x             " +
                        "                    xo.n  x             " +
                        "                    xxx nox             " +
                        "                    xoxxn x             " +
                        "                    x x o xx            " +
                        "                    xn Onnox            " +
                        "                    x   o  x            " +
                        "                    xxxxxxxx            " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                ),
                new Configuration(this, "5",
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "             xxxxx                      " +
                        "             x  ox                      " +
                        "             x xox                      " +
                        "             x xoxxxxxxx                " +
                        "             x n n n . x                " +
                        "             xxx x x x x                " +
                        "               x       x                " +
                        "               xxxxxxxxx                " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                ),
                new Configuration(this, "6",
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "        xxxxxxx                         " +
                        "     xxxx     x                         " +
                        "     x   oxxx x                         " +
                        "     x x x    xx                        " +
                        "     x x n nxo x                        " +
                        "     x x  O  x x                        " +
                        "     x oxn n x x                        " +
                        "     xx    x x xxx                      " +
                        "      x xxxo    .x                      " +
                        "      x     xx   x                      " +
                        "      xxxxxxxxxxxx                      " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                ),
                new Configuration(this, "7", //Note: I've never managed to solve this one.  To the best of my knowledge it is possible, I've just never sat down to really work through it.
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "            xxxxxxx                     " +
                        "            x on  x                     " +
                        "            xno x x                     " +
                        "            x on  x                     " +
                        "            xno xxx                     " +
                        "            x onx                       " +
                        "           xxno x                       " +
                        "           x nonx                       " +
                        "           x.no x                       " +
                        "           x  o x                       " +
                        "           xxxxxx                       " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                )/*,
                new Configuration(this, "8",
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                )/*,
                new Configuration(this, "9",
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                )/*,
                new Configuration(this, "10",
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        " +
                        "                                        "
                )*/
        };
        //</editor-fold>

        //Set up menu items
        //This is the most trivial non-null case that would be relevant:  A fixed list of items set once during initialization.
        this.getNavigationRoot().getChildren().addAll(
                Arrays.stream(CONFIGURATIONS)
                    .map(
                        configuration -> configuration.getTreeItem(event -> {
                            // We are really lazy here and just cast the View's members to what we provided.
                            ((Visualization)VisualContainer.this.getContent()).setCurrentConfiguration(configuration);
                            //In order to properly integrate with the UI, we have to use the registerActionStack method from the controller.
                            // We provide an IActionStack that is functional, but we have to invoke commands on the returned IActionStack.
                            // The returned IActionStack calls the provided stack's methods and updates the UI.
                            // The call to registerActionStack is taken care of in the Configuration constructor.
                            ((SimpleObjectProperty<IActionStack>)VisualContainer.this.undoBufferProperty()).set(configuration.getActionStack());
                            // We want to wait for event processing to be off the stack before calling requestFocus, otherwise the request tends to be ignored.
                            // Far too often when dealing with JavaFx, "wrap it in a Platform.runLater call" is the advised solution to work around some oddity.
                            Platform.runLater(() -> VisualContainer.this.getVisualization().requestFocus());
                        })
                    )
                    .collect(Collectors.toList())
        );
    }

    public Visualization getVisualization() {
        return (Visualization)getContent();
    }

    public SessionInterfaceController getController() {
        return this.controller;
    }
}
