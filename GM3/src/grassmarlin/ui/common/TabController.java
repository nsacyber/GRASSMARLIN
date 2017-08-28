package grassmarlin.ui.common;

import grassmarlin.common.ListSizeBinding;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.ui.common.tree.NavigationView;
import javafx.application.Platform;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TabController {
    protected static class InterfaceTab {
        private final Tab tab;
        private final TreeItem<Object> root;

        public InterfaceTab(final Tab tab, final TreeItem<Object> root) {
            this.tab = tab;
            this.root = root;
        }

        public StringExpression getTitle() {
            return this.tab.textProperty();
        }
        public Tab getTab() {
            return this.tab;
        }
        public TreeItem<Object> getRoot() {
            return this.root;
        }
    }

    protected Pane paneNavigation;
    protected TabPane paneTabs;

    protected final Map<Tab, InterfaceTab> detailForTabs;

    private final TreeView<Object> treeView;

    public TabController() {
        this.detailForTabs = new LinkedHashMap<>();
        this.treeView = new NavigationView();
        this.treeView.setShowRoot(false);
        final ContextMenu menu = new ContextMenu();
        final MenuItem miNoChildren = new MenuItem("There are no valid commands for this item.");
        miNoChildren.setDisable(true);
        miNoChildren.visibleProperty().bind(new ListSizeBinding(menu.getItems()).isEqualTo(1));
        menu.getItems().add(miNoChildren);
        this.treeView.setContextMenu(menu);
        menu.setOnShowing(event -> {
            menu.getItems().clear();
            menu.getItems().add(miNoChildren);

            TreeItem<Object> itemSelected = TabController.this.treeView.getSelectionModel().getSelectedItem();
            while(itemSelected != null) {
                final Object objectSelected = itemSelected.getValue();
                if(objectSelected instanceof ICanHasContextMenu) {
                    final List<MenuItem> items = ((ICanHasContextMenu) objectSelected).getContextMenuItems();
                    if(!items.isEmpty()) {
                        if(menu.getItems().size() > 1) {
                            menu.getItems().add(new SeparatorMenuItem());
                        }
                        menu.getItems().addAll(items);
                    }
                }
                itemSelected = itemSelected.getParent();
            }

            if(menu.getItems().size() == 1) {
                Platform.runLater(menu::hide);
            }
        });
    }

    private final ChangeListener<Tab> handler_SelectedTabChanged = this::Handle_selectedTabChanged;

    public void attachToUi(final Pane containerNavigation, final TabPane containerTabs) {
        if(this.paneNavigation != null) {
            this.paneNavigation.getChildren().remove(this.treeView);
            this.treeView.prefWidthProperty().unbind();
            this.treeView.prefHeightProperty().unbind();
        }
        if(this.paneTabs != null) {
            paneTabs.getSelectionModel().selectedItemProperty().removeListener(handler_SelectedTabChanged);
            paneTabs.getTabs().clear();
        }

        this.paneNavigation = containerNavigation;
        this.paneTabs = containerTabs;

        if(this.paneNavigation != null) {
            this.paneNavigation.getChildren().add(this.treeView);
            this.treeView.prefWidthProperty().bind(this.paneNavigation.widthProperty());
            this.treeView.prefHeightProperty().bind(this.paneNavigation.heightProperty());
        }
        if(this.paneTabs != null) {
            this.paneTabs.getSelectionModel().selectedItemProperty().addListener(handler_SelectedTabChanged);
            this.paneTabs.getTabs().addAll(this.detailForTabs.keySet());
        }
    }

    public void copyFrom(final TabController controller) {
        this.clear();
        for(Map.Entry<Tab, InterfaceTab> entry : controller.detailForTabs.entrySet()) {
            this.addContent(entry.getValue().getTab(), entry.getValue().getRoot(), entry.getValue().getTab().isClosable());
        }
    }

    private void Handle_selectedTabChanged(final ObservableValue<? extends Tab> observable, final Tab oldValue, final Tab newValue) {
        if(newValue == null) {
            this.treeView.setRoot(null);
        } else {
            final InterfaceTab contentNew = detailForTabs.get(newValue);
            if(contentNew == null || contentNew.getRoot() == null) {
                this.treeView.setRoot(null);
            } else {
                this.treeView.setRoot(contentNew.getRoot());
            }
        }
    }

    //TODO: addTransientContent methods
    public Tab addContent(final Tab content) {
        return this.addContent(content, null, false);
    }
    public Tab addContent(final String title, final Pane content) {
        return this.addContent(title, content, null);
    }
    public Tab addContent(final StringExpression title, final Pane content) {
        return this.addContent(title, content, null);
    }
    public Tab addContent(final String title, final Pane content, final TreeItem<Object> navigationRoot) {
        return this.addContent(new ReadOnlyStringWrapper(title), content, navigationRoot);
    }
    public Tab addContent(final StringExpression title, final Pane content, final TreeItem<Object> navigationRoot) {
        final Tab tab = new Tab();
        tab.textProperty().bind(title);
        tab.setContent(content);
        if(paneTabs != null) {
            content.prefWidthProperty().bind(paneTabs.widthProperty());
            content.prefHeightProperty().bind(paneTabs.heightProperty());
        }

        return this.addContent(tab, navigationRoot, false);
    }
    public Tab addContent(final Tab content, final TreeItem<Object> navigationRoot, final boolean isTransient) {
        detailForTabs.put(content, new InterfaceTab(content, navigationRoot));
        if(paneTabs != null) {
            paneTabs.getTabs().add(content);
        }
        content.setClosable(isTransient);

        return content;
    }

    public void removeContent(final Tab content) {
        detailForTabs.remove(content);
        if(paneTabs != null) {
            paneTabs.getTabs().remove(content);
        }
    }

    public void showTab(final Tab tab) {
        paneTabs.getSelectionModel().select(tab);
    }

    public void clear() {
        detailForTabs.clear();
        //Clearing the tabs will select a null tab which will clear the Tree View
        if(paneTabs != null) {
            paneTabs.getTabs().clear();
        }
    }
}
