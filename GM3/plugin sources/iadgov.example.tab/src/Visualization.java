package iadgov.example.tab;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;

/**
 * The Visualization class manages the display of a Configuration.
 *
 * The plugin functions by placing a single Visualization on the tab
 * and updating it to interact with a different Configuration, as chosen
 * by the user.
 *
 * As this class handles input mapping and display of data, its contents
 * are beyond the scope of the example being presented.
 */
class Visualization extends ScrollPane {
    final static int WIDTH = 40;
    final static int HEIGHT = 24;

    final static char CH_BLANK = ' ';
    final static char CH_PLAYER = '.';
    final static char CH_WALL = 'x';
    final static char CH_GOAL = 'o';
    final static char CH_BOX = 'n';
    final static char CH_GOAL_FULL = 'O';

    protected static final HashMap<Character, Image> mappingImages = new HashMap<Character, Image>() {
        {
            this.put(CH_BLANK, new Image(Visualization.class.getClassLoader().getResourceAsStream("Blank.png")));
            this.put(CH_PLAYER, new Image(Visualization.class.getClassLoader().getResourceAsStream("Player.png")));
            this.put(CH_WALL, new Image(Visualization.class.getClassLoader().getResourceAsStream("Wall.png")));
            this.put(CH_GOAL, new Image(Visualization.class.getClassLoader().getResourceAsStream("Goal.png")));
            this.put(CH_BOX, new Image(Visualization.class.getClassLoader().getResourceAsStream("Box.png")));
            this.put(CH_GOAL_FULL, new Image(Visualization.class.getClassLoader().getResourceAsStream("Goal_Full.png")));
        }
    };

    private Configuration currentConfiguration;

    private final ImageView[][] tiles;
    private final Text txtProgress;
    private final Text txtSteps;

    public Visualization() {
        this.currentConfiguration = null;

        this.tiles = new ImageView[WIDTH][HEIGHT];

        final Text lblProgress = new Text("Complete / Total");
        lblProgress.setTextAlignment(TextAlignment.CENTER);
        this.txtProgress = new Text();
        this.txtProgress.setTextAlignment(TextAlignment.CENTER);

        final Text lblSteps = new Text("Steps (Best)");
        lblSteps.setTextAlignment(TextAlignment.CENTER);
        this.txtSteps = new Text("");
        this.txtSteps.setTextAlignment(TextAlignment.CENTER);

        final GridPane paneLayout = new GridPane();
        paneLayout.add(lblSteps, 0, 0);
        paneLayout.add(txtSteps, 0, 1);
        paneLayout.add(lblProgress, 0, 2);
        paneLayout.add(this.txtProgress, 0, 3);


        for(int y = 0; y < HEIGHT; y++) {
            for(int x = 0; x < WIDTH; x++) {
                final ImageView view = new ImageView();
                view.setFitWidth(32.0);
                view.setFitHeight(32.0);
                paneLayout.add(view, 1 + x, y);
                this.tiles[x][y] = view;
            }
        }

        this.setContent(paneLayout);

        this.setOnKeyReleased(event -> {
            switch(event.getCode()) {
                case UP:
                case DOWN:
                case LEFT:
                case RIGHT:
                case W:
                case A:
                case S:
                case D:
                    event.consume();
                    break;
            }
        });
        this.setOnKeyPressed(event -> {
            if(this.currentConfiguration == null) {
                return;
            }

            switch(event.getCode()) {
                case UP:
                case DOWN:
                case LEFT:
                case RIGHT:
                case W:
                case A:
                case S:
                case D:
                    if(this.currentConfiguration.processInput(event.getCode())) {
                        this.render();
                    }
                    event.consume();
                    break;
            }
        });
        this.setOnMouseClicked(event -> {
            Visualization.this.requestFocus();
            event.consume();
        });

        this.setFocusTraversable(true);

        this.setOnContextMenuRequested(event -> {
            if(Visualization.this.currentConfiguration != null) {
                final ContextMenu menu = new ContextMenu();
                menu.getItems().addAll(Visualization.this.currentConfiguration.getContextMenuItems());
                menu.show(Visualization.this, event.getScreenX(), event.getScreenY());
            }
        });
    }

    public void setCurrentConfiguration(final Configuration configuration) {
        this.currentConfiguration = configuration;

        this.render();
    }

    protected void render() {
        if(this.currentConfiguration == null) {
            for(int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    this.tiles[x][y].setImage(null);
                }
            }
            this.txtProgress.setText("");
            this.txtSteps.setText("");
        } else {
            final char[] layout = this.currentConfiguration.getCurrent();
            for(int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    this.tiles[x][y].setImage(mappingImages.get(layout[y * WIDTH + x]));
                }
            }
            if(this.currentConfiguration.isComplete()) {
                this.txtProgress.setText("Complete!");
            } else {
                this.txtProgress.setText(String.format("%d / %d", 0, 0));
            }

            if(this.currentConfiguration.getBestSteps() != -1) {
                this.txtSteps.setText(String.format("%d (%d)", this.currentConfiguration.getSteps(), this.currentConfiguration.getBestSteps()));
            } else {
                this.txtSteps.setText(String.format("%d (incomplete)", this.currentConfiguration.getSteps()));
            }
        }
    }
}
