package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.ListSizeBinding;
import grassmarlin.plugins.internal.logicalview.Plugin;
import grassmarlin.ui.common.controls.DoubleFieldTableCell;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class StyleEditor extends Dialog {
    protected final RuntimeConfiguration config;

    public StyleEditor(final RuntimeConfiguration config, final ObservableList<Style> styles, final ObservableList<EdgeStyleRule> rules, final ObservableList<Plugin.EdgeRuleUiFactory> ruleFactories) {
        this.config = config;

        initComponents(styles, rules, ruleFactories);
    }

    private void initComponents(final ObservableList<Style> styles, final ObservableList<EdgeStyleRule> rules, final ObservableList<Plugin.EdgeRuleUiFactory> ruleFactories) {
        this.setTitle("Edit Styles");
        RuntimeConfiguration.setIcons(this);
        final TabPane tabs = new TabPane();
        this.setResizable(true);

        // == Styles
        final Tab tabStyles = new Tab("Styles");
        tabs.getTabs().add(tabStyles);

        final VBox layoutStyles = new VBox();

        final TableView<Style> tableStyles = new TableView<>();
        tableStyles.setEditable(true);

        tableStyles.setItems(styles);

        final TableColumn<Style, String> columnName = new TableColumn<>("Name");
        columnName.setEditable(false);
        columnName.setCellValueFactory(features -> new ReadOnlyStringWrapper(features.getValue().getName()));

        final TableColumn<Style, Double> columnWeight = new TableColumn<>("Weight");
        columnWeight.setEditable(true);
        columnWeight.setCellValueFactory(param ->  param.getValue().weightProperty().asObject());
        columnWeight.setCellFactory(DoubleFieldTableCell.forTableColumn(Style.WEIGHT_MIN, Style.WEIGHT_MAX));

        final TableColumn<Style, Double> columnOpacity = new TableColumn<>("Opacity");
        columnOpacity.setEditable(true);
        columnOpacity.setCellValueFactory(param ->  param.getValue().opacityProperty().asObject());
        columnOpacity.setCellFactory(DoubleFieldTableCell.forTableColumn(Style.OPACITY_MIN, Style.OPACITY_MAX));

        tableStyles.getColumns().addAll(columnName, columnWeight, columnOpacity);
        final ContextMenu menuStyles = new ContextMenu();
        menuStyles.getItems().addAll(
                new ActiveMenuItem("Delete", event -> {
                    final Style styleRemoved = tableStyles.getItems().remove(tableStyles.getSelectionModel().getSelectedIndex());
                    if(styleRemoved != null) {
                        styleRemoved.clearDataCache();
                    }
                }).bindEnabled(tableStyles.getSelectionModel().selectedItemProperty().isNotNull())
        );
        tableStyles.setContextMenu(menuStyles);

        final HBox layoutAddStyle = new HBox();
        final TextField txtAddStyle = new TextField();
        final Button btnAddStyle = new Button("Add");
        btnAddStyle.setOnAction(event -> {
            styles.add(Style.getStyle(txtAddStyle.getText()));
            txtAddStyle.clear();
            txtAddStyle.requestFocus();
        });
        btnAddStyle.disableProperty().bind(txtAddStyle.textProperty().isEmpty());

        layoutAddStyle.getChildren().addAll(txtAddStyle, btnAddStyle);

        layoutStyles.getChildren().addAll(tableStyles, layoutAddStyle);

        tabStyles.setContent(layoutStyles);

        // == Style Mappings
        final Tab tabMappings = new Tab("Edge Style Mappings");
        final ListView<EdgeStyleRule> listRules = new ListView<>();
        listRules.setItems(rules);

        final ContextMenu menuMappings = new ContextMenu();
        menuMappings.getItems().addAll(
                new ActiveMenuItem("Move Up", event -> {
                    final int idxSelected = listRules.getSelectionModel().selectedIndexProperty().get();
                    listRules.getItems().add(idxSelected - 1, listRules.getItems().remove(idxSelected));
                    listRules.getSelectionModel().select(idxSelected - 1);
                }).bindEnabled(listRules.getSelectionModel().selectedIndexProperty().greaterThan(0)),
            new ActiveMenuItem("Move Down", event -> {
                final int idxSelected = listRules.getSelectionModel().selectedIndexProperty().get();
                listRules.getItems().add(idxSelected + 1, listRules.getItems().remove(idxSelected));
                listRules.getSelectionModel().select(idxSelected + 1);
            }).bindEnabled(listRules.getSelectionModel().selectedIndexProperty().lessThan(new ListSizeBinding(listRules.getItems()).subtract(1))),
            new ActiveMenuItem("Delete", event -> {
                listRules.getItems().remove(listRules.getSelectionModel().getSelectedIndex());
            })
        );
        listRules.setContextMenu(menuMappings);

        // == UI For adding new EdgeStyleRules
        final HBox layoutAddUi = new HBox();
        final ComboBox<Plugin.EdgeRuleUiFactory> cbRules = new ComboBox<>();
        final Pane paneRuleUi = new Pane();
        final ComboBox<Style> styleSelector = new ComboBox<>();
        final Button btnAddRule = new Button("Add");
        cbRules.setItems(ruleFactories);
        cbRules.setOnAction(event -> {
            paneRuleUi.getChildren().clear();
            paneRuleUi.getChildren().add((Node)cbRules.getSelectionModel().getSelectedItem().getUiFactory().get());
        });
        styleSelector.setItems(styles);
        btnAddRule.disableProperty().bind(cbRules.getSelectionModel().selectedItemProperty().isNull().or(styleSelector.getSelectionModel().selectedItemProperty().isNull()));
        btnAddRule.setOnAction(event -> {
            final EdgeStyleRule rule = (EdgeStyleRule)cbRules.getSelectionModel().getSelectedItem().getGetter().apply(paneRuleUi.getChildren().get(0));
            rule.styleProperty().set(styleSelector.getSelectionModel().getSelectedItem());
            rules.add(rule);
        });

        layoutAddUi.getChildren().addAll(cbRules, styleSelector, paneRuleUi, btnAddRule);

        final VBox layoutMappings = new VBox();
        layoutMappings.getChildren().addAll(listRules, layoutAddUi);
        tabMappings.setContent(layoutMappings);

        tabs.getTabs().add(tabMappings);

        this.getDialogPane().setContent(tabs);
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }
}
