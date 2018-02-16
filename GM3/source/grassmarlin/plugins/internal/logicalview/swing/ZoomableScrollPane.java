package grassmarlin.plugins.internal.logicalview.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class ZoomableScrollPane extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    public interface IContent {
        void paint(final Graphics2D g);
        Rectangle2D getBounds();
    }
    public interface IDraggable extends IContent {
        void moveTo(final double x, final double y);
        double getTranslateX();
        double getTranslateY();
    }
    private final Map<String, Collection<IContent>> contents;

    private double translateX;
    private double translateY;
    private double scale;
    private int scaleLevel;

    public ZoomableScrollPane(String... layers) {
        this.contents = new LinkedHashMap<>();

        this.translateX = 0.0;
        this.translateY = 0.0;
        this.scale = 1.0;
        this.scaleLevel = 0;

        for(String layer : layers) {
            this.contents.put(layer, new LinkedHashSet<>());
        }

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);

        super.repaint();
    }

    public void addChild(final IContent node, final String layer) {
        final Collection<IContent> contentsLayer = this.contents.get(layer);
        if(contentsLayer != null) {
            synchronized(this.contents) {
                contentsLayer.add(node);
            }
            super.repaint();
        }
    }
    public void removeChild(final IContent node, final String layer) {
        final Collection<IContent> contentsLayer = this.contents.get(layer);
        if(contentsLayer != null) {
            synchronized(this.contents) {
                contentsLayer.remove(node);
            }
            super.repaint();
        }
    }

    // == Drag support for contents
    private Point worldFromScreen(final Point ptWorld) {
        final Point result = new Point();
        result.setLocation((ptWorld.getX() - translateX) / this.scale, (ptWorld.getY() - translateY) / this.scale);
        return result;
    }

    protected Collection<IContent> contentUnderCursor(final double xWorld, final double yWorld) {
        final LinkedHashSet result = new LinkedHashSet<>();
        synchronized(this.contents) {
            for (final Collection<IContent> layer : this.contents.values()) {
                for (final IContent item : layer) {
                    if (item instanceof IDraggable) {
                        final IDraggable draggable = (IDraggable) item;
                        if (item.getBounds().contains(xWorld - draggable.getTranslateX(), yWorld - draggable.getTranslateY())) {
                            result.add(item);
                        }
                    } else {
                        if (item.getBounds().contains(xWorld, yWorld)) {
                            result.add(item);
                        }
                    }
                }
            }
        }

        return result;
    }

    // == Rendering
    @Override
    public void paintComponent(final Graphics _g) {
        if(_g instanceof Graphics2D) {
            final Graphics2D g = (Graphics2D)_g;

            //TODO: Clear?
            final AffineTransform original = g.getTransform();
            final AffineTransform transform = g.getTransform();
            transform.translate(this.translateX, this.translateY);
            transform.scale(this.scale, this.scale);

            final Collection<IContent> contentRenderable;
            synchronized(this.contents) {
                contentRenderable = this.contents.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
            }

            g.clearRect(0, 0, this.getWidth(), this.getHeight());

            for(IContent item : contentRenderable) {
                g.setTransform(transform);
                item.paint(g);
            }
            g.setTransform(original);
        } else {
            throw new UnsupportedOperationException("ZSP Can only render to a Graphics2D target.");
        }
    }

    // == Mouse Listeners
    @Override
    public void mouseClicked(MouseEvent e) {
        //Don't care
    }

    private Point dragOrigin = null;
    private IDraggable dragTarget = null;
    @Override
    public void mousePressed(MouseEvent e) {
        if(e.getButton() == 1) {
            // Start drag (we need baseline coordinates to use during the drag)
            this.dragOrigin = worldFromScreen(e.getPoint());
            final Collection<IContent> dragTargets = contentUnderCursor(this.dragOrigin.getX(), this.dragOrigin.getY());
            //If there is nothing draggable under the cursor we will be panning.
            this.dragTarget = null;
            for (final IContent target : dragTargets) {
                if (target instanceof IDraggable) {
                    this.dragTarget = (IDraggable) target;
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if(e.getButton() == 1) {
            this.dragTarget = null;
            this.dragOrigin = null;
        } else if(e.getButton() == 2) {
            //TODO: Show context menu
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        //We don't care
    }

    @Override
    public void mouseExited(MouseEvent e) {
        //We don't care
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(e.getButton() == 1 && this.dragOrigin != null) {
            final Point ptEvent = worldFromScreen(e.getPoint());
            if(this.dragTarget != null) {
                this.dragTarget.moveTo(this.dragTarget.getTranslateX() - this.dragOrigin.getX() + ptEvent.getX(), this.dragTarget.getTranslateY() - this.dragOrigin.getY() + ptEvent.getY());
                this.dragOrigin = ptEvent;
            } else {
                this.translateX = this.translateX - this.dragOrigin.getX() + ptEvent.getX();
                this.translateY = this.translateY - this.dragOrigin.getY() + ptEvent.getY();
                //We don't update dragOrigin for a pan because the same world coordinates should always be below the cursor.
            }
            super.repaint();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        //We ignore the move, caring only about the drag
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        final double scalePrevious = this.scale;

        final Point ptPre = worldFromScreen(e.getPoint());

        if(e.getWheelRotation() < 0) {
            this.scaleLevel = Math.min(this.scaleLevel + 1, 48);
        } else {
            this.scaleLevel = Math.max(this.scaleLevel - 1, -48);
        }
        this.scale = Math.pow(1.1, (double)this.scaleLevel);

        if(this.scale != scalePrevious) {
            final Point ptPost = worldFromScreen(e.getPoint());
            //If the scale has changed we will need to translate slightly to keep the same point centered.
            this.translateX += this.scale * (ptPost.getX() - ptPre.getX());
            this.translateY += this.scale * (ptPost.getY() - ptPre.getY());

            super.repaint();
        }
    }
}
