package iadgov.example.tab;

import grassmarlin.common.edit.ActionStack;
import grassmarlin.common.edit.IAction;
import grassmarlin.common.edit.IActionStack;
import grassmarlin.common.edit.IActionUndoable;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.tree.SelectableTreeItem;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.input.KeyCode;

import java.util.Collections;
import java.util.List;

/**
 * The Configuration contains the information needed to describe a level,
 * including current state, and the methods to act on it.
 *
 * Because this is wrapped in the TreeItem that is used to select this level,
 * the ICanHasContextMenu interface instructs the tree to include the given
 * menu item (reset).
 *
 * As most of this class is concerned with the logic of manipulating a configuration
 * and as the purpose of this plugin is to demonstrate the system for creating
 * tabs, the bulk of this code is undocumented as it is irrelevant.
 */
public class Configuration implements ICanHasContextMenu {
    private final VisualContainer container;
    private final String name;
    private ConfigurationTreeItem treeNode;

    private final String original;
    private char[] current;

    private int currentMoves;
    private int bestMoves;
    private boolean isComplete;

    private final MenuItem miReset;
    private final IActionStack undoBuffer;

    @Override
    public List<MenuItem> getContextMenuItems() {
        return Collections.singletonList(miReset);
    }

    private class ConfigurationTreeItem extends SelectableTreeItem<Object> {
        public ConfigurationTreeItem(final EventHandler<ActionEvent> handler) {
            super(Configuration.this, handler);
        }

        public void update() {
            final TreeModificationEvent<Object> event = new TreeModificationEvent<>(treeNotificationEvent(), this);
            Event.fireEvent(this, event);
        }
    }

    public Configuration(final VisualContainer container, final String name, final String source) {
        this.container = container;

        this.name = name;
        this.original = source;
        this.current = source.toCharArray();

        this.currentMoves = 0;
        this.bestMoves = -1;
        this.isComplete = false;

        this.treeNode = null;

        this.miReset = new ActiveMenuItem("_Reset", event -> {
            Configuration.this.reset();
            container.getVisualization().render();
            container.getVisualization().requestFocus();
        });
        this.undoBuffer = container.getController().registerActionStack(new ActionStack());
    }

    public IActionStack getActionStack() {
        return this.undoBuffer;
    }

    public TreeItem<Object> getTreeItem(final EventHandler<ActionEvent> handler) {
        if(this.treeNode == null) {
            this.treeNode = new ConfigurationTreeItem(handler);
        }
        return this.treeNode;
    }

    public void reset() {
        this.isComplete = false;
        this.currentMoves = 0;
        this.current = this.original.toCharArray();

        this.treeNode.update();
    }

    public char[] getCurrent() {
        return this.current;
    }

    public boolean processInput(KeyCode input) {
        if(isComplete) {
            return false;
        }

        final String previousState = new String(this.current);

        final int dX;
        final int dY;
        switch(input) {
            case W:
            case UP:
                dX = 0;
                dY = -1;
                break;
            case S:
            case DOWN:
                dX = 0;
                dY = 1;
                break;
            case A:
            case LEFT:
                dX = -1;
                dY = 0;
                break;
            case D:
            case RIGHT:
                dX = 1;
                dY = 0;
                break;
            default:
                return false;
        }

        //HACK: We turn current into a string for indexOf()
        final int offsetPlayer = new String(this.current).indexOf(Visualization.CH_PLAYER);
        if(offsetPlayer == -1) {
            return false;
        }

        final int xPlayer = offsetPlayer % Visualization.WIDTH;
        final int yPlayer = offsetPlayer / Visualization.WIDTH;

        final int xPlayerNew = xPlayer + dX;
        final int yPlayerNew = yPlayer + dY;

        //Can't move off edge
        if(xPlayerNew < 0 || xPlayerNew >= Visualization.WIDTH || yPlayerNew < 0 || yPlayerNew >= Visualization.HEIGHT) {
            return false;
        }

        final int offsetPlayerNew = yPlayerNew * Visualization.WIDTH + xPlayerNew;

        switch(this.current[offsetPlayerNew]) {
            case Visualization.CH_GOAL_FULL:
            case Visualization.CH_BOX:
                //Trying to move onto a box means test for a push.
                final int xBoxNew = xPlayerNew + dX;
                final int yBoxNew = yPlayerNew + dY;

                //Can't move off edge
                if(xBoxNew < 0 || xBoxNew >= Visualization.WIDTH || yBoxNew < 0 || yBoxNew >= Visualization.HEIGHT) {
                    return false;
                }

                final int offsetBoxNew = yBoxNew * Visualization.WIDTH + xBoxNew;
                switch(this.current[offsetBoxNew]) {
                    case Visualization.CH_GOAL:
                        this.current[offsetBoxNew] = Visualization.CH_GOAL_FULL;
                        break;
                    case Visualization.CH_BLANK:
                        this.current[offsetBoxNew] = Visualization.CH_BOX;
                        break;
                    default:
                        //Movement blocked
                        return false;
                }
                //We fall through to the blank/goal code since the rest is the same.
            case Visualization.CH_BLANK:
            case Visualization.CH_GOAL:
                //Effectively empty, so allow move.
                final char originalTile = this.original.charAt(offsetPlayer);
                switch(originalTile) {
                    case Visualization.CH_GOAL_FULL:
                        this.current[offsetPlayer] = Visualization.CH_GOAL;
                        break;
                    case Visualization.CH_PLAYER:
                    case Visualization.CH_BOX:
                        this.current[offsetPlayer] = Visualization.CH_BLANK;
                        break;
                    default:
                        this.current[offsetPlayer] = originalTile;
                        break;
                }
                this.current[offsetPlayerNew] = Visualization.CH_PLAYER;
                break;
            default:
                return false;
        }

        final String afterState = new String(this.current);
        this.undoBuffer.doAction(new IActionUndoable() {
            @Override
            public boolean undoAction() {
                Configuration.this.current = previousState.toCharArray();
                Configuration.this.currentMoves--;
                Configuration.this.treeNode.update();
                Configuration.this.container.getVisualization().setCurrentConfiguration(Configuration.this);
                return true;
            }

            @Override
            public boolean doAction() {
                Configuration.this.current = afterState.toCharArray();
                Configuration.this.currentMoves++;
                Configuration.this.treeNode.update();
                Configuration.this.container.getVisualization().setCurrentConfiguration(Configuration.this);
                return true;
            }
        });

        // Check for completion.  If there are no unfullfilled goals, then we're done.
        final String currentAsString = new String(this.current);
        if(
                currentAsString.indexOf(Visualization.CH_GOAL) == -1 &&
                this.original.charAt(offsetPlayerNew) != Visualization.CH_GOAL &&
                this.original.charAt(offsetPlayerNew) != Visualization.CH_GOAL_FULL
        ) {
            //Because this is an IAction and not an IActionUndoable, the Undo buffer will be purged--you can't undo completion.
            this.undoBuffer.doAction(new IAction() {
                @Override
                public boolean doAction() {
                    Configuration.this.isComplete = true;
                    if(Configuration.this.currentMoves < Configuration.this.bestMoves || Configuration.this.bestMoves == -1) {
                        Configuration.this.bestMoves = Configuration.this.currentMoves;
                    }
                    return true;
                }
            });
        }

        return true;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();

        result.append(this.name).append(" - ").append(this.currentMoves);
        if(this.bestMoves > 0) {
            result.append(" (Best Score: ").append(this.bestMoves).append(")");
            //If there is no best score it can't be complete.
            if(this.isComplete) {
                result.append(" Completed.");
            }
        }
        return result.toString();
    }

    public boolean isComplete() {
        return this.isComplete;
    }

    public int getSteps() {
        return this.currentMoves;
    }

    public int getBestSteps() {
        return this.bestMoves;
    }
}
