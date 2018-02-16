package grassmarlin.common.svg;

import grassmarlin.Logger;
import grassmarlin.ui.common.DrawBuffer;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.BorderStroke;
import javafx.scene.paint.LinearGradient;
import sun.awt.image.IntegerComponentRaster;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Svg class supports the creation of a Svg file from a JavaFx Node.  The Svg will examine the Node graph for known primitive types and generate the Svg code accordingly.
 */
public class Svg {
    public interface PaintToSvg {
        void paint(final DrawBuffer gc);
    }
    /**
     * The ISuppressSvgExport interface on a Node-derived class will exclude that node from being processed by the Svg export logic.  This is intended for use on Nodes that represent transient elements (highlights), as well as interactive elements.
     */
    public interface ISuppressSvgExport {}

    public static String fillFromPaint(final javafx.scene.paint.Paint paint, final Map<LinearGradient, String> gradients) throws XMLStreamException {
        if(paint == null) {
            return "fill:none";
        }
        if(paint instanceof javafx.scene.paint.Color) {
            final javafx.scene.paint.Color color = (javafx.scene.paint.Color) paint;
            final double opacity = color.getOpacity();
            if (opacity > 0.0 && opacity < 1.0) {
                return String.format("fill:%s;fill-opacity:%f", fromColor(color), opacity);
            } else {
                return String.format("fill:%s", fromColor(color));
            }
        } else if(paint instanceof javafx.scene.paint.LinearGradient) {
            final javafx.scene.paint.LinearGradient gradient = (javafx.scene.paint.LinearGradient)paint;
            if(gradients.containsKey(gradient)) {
                return String.format("fill:%s", gradients.get(gradient));
            } else {
                throw new XMLStreamException("Unknown Gradient (" + paint.toString() + ")");
            }
        } else {
            throw new XMLStreamException("Unable to process non-Color fill (" + paint.getClass() + "): " + paint);
        }
    }
    public static String fromPaint(final javafx.scene.paint.Paint paint, final Map<LinearGradient, String> gradients) throws XMLStreamException {
        if(paint == null) {
            return "none";
        } else if(paint instanceof javafx.scene.paint.Color) {
            return fromColor((javafx.scene.paint.Color) paint);
        } else if(paint instanceof LinearGradient) {
            if(gradients.containsKey(paint)) {
                return gradients.get(paint);
            } else {
                throw new XMLStreamException("Unknown Gradient (" + paint.toString() + ")");
            }
        } else {
            throw new XMLStreamException("Unable to process non-Color Paint (" + paint.getClass() + "): " + paint);
        }
    }
    public static String fromColor(final javafx.scene.paint.Color color) {
        if(color == null || color.getOpacity() == 0.0) {
            return "none";
        } else {
            return String.format("rgb(%d,%d,%d)", (int) (255.0 * color.getRed()), (int) (255.0 * color.getGreen()), (int) (255.0 * color.getBlue()));
        }
    }

    public static boolean serialize(final javafx.scene.Parent rootNode, final Path outFile) {
        try(final BufferedWriter writer = Files.newBufferedWriter(outFile)) {
            final XMLStreamWriter writerXml= XMLOutputFactory.newFactory().createXMLStreamWriter(writer);
            serialize(rootNode, writerXml);
        } catch(IOException | XMLStreamException ex) {
            Logger.log(Logger.Severity.ERROR, "There was an error writing the SVG File: %s", ex.getMessage());
            return false;
        }
        return true;
    }

    public static void serialize(final javafx.scene.Parent rootNode, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartDocument();

        writer.writeStartElement("svg");
        writer.writeDefaultNamespace("http://www.w3.org/2000/svg");
        writer.writeNamespace("xlink", "http://www.w3.org/1999/xlink"); //Used for raster images.
        writer.writeAttribute("version", "1.1");

        final javafx.geometry.Bounds boundsAll = rootNode.getLayoutBounds();
        writer.writeAttribute("width", Double.toString(boundsAll.getWidth()));
        writer.writeAttribute("height", Double.toString(boundsAll.getHeight()));

        final Map<LinearGradient, String> gradients = new HashMap<>();
        serializeRootGradients(rootNode, gradients, writer);

        serializeRootNode(rootNode, gradients, writer);

        writer.writeEndElement();

        writer.writeEndDocument();
    }

    private static void writeGradient(final javafx.scene.paint.Paint paint, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        if(paint instanceof LinearGradient) {
            final LinearGradient gradient = (LinearGradient)paint;
            final String name = String.format("lg%d", gradients.size());

            writer.writeStartElement("linearGradient");
            writer.writeAttribute("id", name);
            if(gradient.isProportional()) {
                writer.writeAttribute("x1", (gradient.getStartX() * 100.0) + "%");
                writer.writeAttribute("y1", (gradient.getStartY() * 100.0) + "%");
                writer.writeAttribute("x2", (gradient.getEndX() * 100.0) + "%");
                writer.writeAttribute("y2", (gradient.getEndY() * 100.0) + "%");
            } else {
                //HACK: Rather than track the translations through the scene graph, we instead assume that the gradient covers the complete range where it is being drawn and convert to proportional coordinates.
                //This holds true for all known uses of gradients in GrassMarlin at this time.

                final double proportionalStartX = gradient.getStartX() <= gradient.getEndX() ? 0.0 : 100.0;
                final double proportionalEndX = 100.0 - proportionalStartX;
                final double proportionalStartY = gradient.getStartY() <= gradient.getEndY() ? 0.0 : 100.0;
                final double proportionalEndY = 100.0 - proportionalStartY;

                writer.writeAttribute("x1", proportionalStartX + "%");
                writer.writeAttribute("y1", proportionalStartY + "%");
                writer.writeAttribute("x2", proportionalEndX + "%");
                writer.writeAttribute("y2", proportionalEndY + "%");
            }

            for(final javafx.scene.paint.Stop stop : gradient.getStops()) {
                writer.writeStartElement("stop");
                writer.writeAttribute("offset", (stop.getOffset() * 100.0) + "%");
                writer.writeAttribute("style", String.format("stop-color:%s;stop-opacity:%f", fromColor(stop.getColor()), stop.getColor().getOpacity()));
                writer.writeEndElement();
            }

            writer.writeEndElement();

            if(!gradients.containsKey(gradient)) {
                gradients.put(gradient, "url(#" + name + ")");
            }
        }
    }
    private static void serializeRootGradients(final javafx.scene.Parent root, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("defs");

        for(final javafx.scene.Node child : root.getChildrenUnmodifiable()) {
            serializeNodeGradients(child, gradients, writer);
        }

        writer.writeEndElement();
    }

    private static void serializeNodeGradients(final javafx.scene.Node node, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        if(node == null || !node.isVisible() || node.getOpacity() == 0.0 || node instanceof ISuppressSvgExport) {
            return;
        }

        if(node instanceof javafx.scene.shape.Shape) {
            //Includes circle, rectangle, polygon, line, cubiccurve, text
            final javafx.scene.shape.Shape shape = (javafx.scene.shape.Shape)node;
            writeGradient(shape.getStroke(), gradients, writer);
            writeGradient(shape.getFill(), gradients, writer);
        } else if(node instanceof javafx.scene.Parent) {
            for(final javafx.scene.Node child : ((javafx.scene.Parent)node).getChildrenUnmodifiable()) {
                serializeNodeGradients(child, gradients, writer);
            }
        }
        //TODO: Support gradients in PaintToSvg classes
    }

    private static void serializeRootNode(final javafx.scene.Parent root, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("g");

        final javafx.geometry.Bounds boundsAll = root.getLayoutBounds();
        writer.writeAttribute("transform", String.format("translate(%f,%f)",
                -boundsAll.getMinX(),
                -boundsAll.getMinY()
        ));

        for(final javafx.scene.Node child : root.getChildrenUnmodifiable()) {
            serializeNode(child, gradients, writer);
        }

        writer.writeEndElement();
    }

    /**
     * This makes a few assumptions about the Node graph.  Most significantly, it assumes that primitive elements lack transformation data.  This means that a Line cannot have an applied transform, but it can belong to a Group that has a transformation.
     * @param node
     * @param writer
     * @throws XMLStreamException
     */
    private static void serializeNode(final javafx.scene.Node node, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        if(node == null || !node.isVisible() || node.getOpacity() == 0.0 || node instanceof ISuppressSvgExport) {
            return;
        }

        final javafx.scene.transform.Transform transform = node.getLocalToParentTransform();
        final boolean hasTransform = !node.getLocalToParentTransform().isIdentity();

        if(hasTransform) {
            writer.writeStartElement("g");
            //HACK: Rotations are handled differently in SVG and JavaFx.  Negating the xy and yx components fixes this, but this will not necessarily hold true in the future--the fact that we only perform translate, rotate, and scale operations allows us to make this change.  What impact it would have on other transforms hasn't been pondered and context switching to linear algebra is out of the question at the moment.
            writer.writeAttribute("transform", String.format("matrix(%f,%f,%f,%f,%f,%f)",
                    transform.getMxx(),
                    -transform.getMxy(),
                    -transform.getMyx(),
                    transform.getMyy(),
                    transform.getTx(),
                    transform.getTy()
            ));
        }

        if(node instanceof PaintToSvg) {
            final DrawBuffer buffer = new DrawBuffer();
            ((PaintToSvg)node).paint(buffer);
            buffer.renderTo(writer);
        } else if(node instanceof javafx.scene.shape.Polygon) {
            serializePolygon((javafx.scene.shape.Polygon)node, gradients, writer);
        } else if(node instanceof javafx.scene.shape.Line) {
            serializeLine((javafx.scene.shape.Line)node, gradients, writer);
        } else if(node instanceof javafx.scene.shape.CubicCurve) {
            serializeCurve((javafx.scene.shape.CubicCurve)node, gradients, writer);
        } else if(node instanceof javafx.scene.Parent) {
            serializeParent((javafx.scene.Parent)node, gradients, writer);
        } else if(node instanceof javafx.scene.shape.Rectangle) {
            serializeRectangle((javafx.scene.shape.Rectangle)node, gradients, writer);
        } else if(node instanceof javafx.scene.shape.Circle) {
            serializeCircle((javafx.scene.shape.Circle)node, gradients, writer);
        } else if(node instanceof javafx.scene.image.ImageView) {
            serializeImage((javafx.scene.image.ImageView)node, writer);
        } else if(node instanceof javafx.scene.text.Text) {
            //Note: A Label is a Parent (covered above) which contains a Text
            serializeText((javafx.scene.text.Text)node, gradients, writer);
        } else {
            throw new XMLStreamException("Unable to convert Node to SVG: (" + node.getClass() + ")" + node);
        }

        if(hasTransform) {
            writer.writeEndElement();
        }
    }

    //<editor-fold desc="The easy stuff">
    private static void serializePolygon(final javafx.scene.shape.Polygon polygon, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        final Double[] points = polygon.getPoints().toArray(new Double[polygon.getPoints().size()]);
        if(points.length < 6) {
            //Not actually a polygon.
            return;
        }

        writer.writeStartElement("polygon");

        final StringBuilder sbPoints = new StringBuilder();
        for(int idxPoint = 0; idxPoint < points.length; idxPoint += 2) {
            sbPoints.append(String.format("%f,%f ", points[idxPoint], points[idxPoint + 1]));
        }
        writer.writeAttribute("points", sbPoints.toString().trim());

        writer.writeAttribute("style", String.format("%s;stroke:%s;stroke-width:%f",
                fillFromPaint(polygon.getFill(), gradients),
                fromPaint(polygon.getStroke(), gradients),
                polygon.getStrokeWidth()
                ));
        writer.writeEndElement();
    }

    private static void serializeLine(final javafx.scene.shape.Line line, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("line");

        writer.writeAttribute("x1", Double.toString(line.getStartX()));
        writer.writeAttribute("y1", Double.toString(line.getStartY()));
        writer.writeAttribute("x2", Double.toString(line.getEndX()));
        writer.writeAttribute("y2", Double.toString(line.getEndY()));

        writer.writeAttribute("style", String.format("stroke:%s;stroke-width:%f",
                fromPaint(line.getStroke(), gradients),
                line.getStrokeWidth()
        ));

        writer.writeEndElement();
    }

    private static void serializeCurve(final javafx.scene.shape.CubicCurve curve, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("path");

        writer.writeAttribute("d", String.format("M%f,%f C%f,%f %f,%f %f,%f",
                curve.getStartX(), curve.getStartY(),
                curve.getControlX1(), curve.getControlY1(),
                curve.getControlX2(), curve.getControlY2(),
                curve.getEndX(), curve.getEndY()
        ));
        writer.writeAttribute("style", String.format("%s;stroke:%s;stroke-width:%f",
                fillFromPaint(curve.getFill(), gradients),
                fromPaint(curve.getStroke(), gradients),
                curve.getStrokeWidth()
        ));

        writer.writeEndElement();
    }

    private static void serializeRectangle(final javafx.scene.shape.Rectangle rect, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("rect");

        writer.writeAttribute("x", Double.toString(rect.getX()));
        writer.writeAttribute("y", Double.toString(rect.getY()));

        writer.writeAttribute("width", Double.toString(rect.getWidth()));
        writer.writeAttribute("height", Double.toString(rect.getHeight()));

        writer.writeAttribute("rx", Double.toString(rect.getArcWidth()));
        writer.writeAttribute("ry", Double.toString(rect.getArcHeight()));

        writer.writeAttribute("style", String.format("%s;stroke:%s;stroke-width:%f",
                fillFromPaint(rect.getFill(), gradients),
                fromPaint(rect.getStroke(), gradients),
                rect.getStrokeWidth()
        ));

        writer.writeEndElement();
    }
    private static void serializeCircle(final javafx.scene.shape.Circle circle, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("ellipse");

        writer.writeAttribute("cx", Double.toString(circle.getCenterX()));
        writer.writeAttribute("cy", Double.toString(circle.getCenterY()));

        writer.writeAttribute("rx", Double.toString(circle.getRadius()));
        writer.writeAttribute("ry", Double.toString(circle.getRadius()));

        writer.writeAttribute("style", String.format("%s;stroke:%s;stroke-width:%f",
                fillFromPaint(circle.getFill(), gradients),
                fromPaint(circle.getStroke(), gradients),
                circle.getStrokeWidth()
        ));

        writer.writeEndElement();
    }
    //</editor-fold>
    //<editor-fold desc="The hard(er) stuff">
    private static void serializeText(final javafx.scene.text.Text text, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("text");

        writer.writeAttribute("x", Double.toString(text.getX()));
        writer.writeAttribute("y", Double.toString(text.getY()));

        writer.writeAttribute("style", String.format("%s;font-family:%s",
                fillFromPaint(text.getFill(), gradients),
                text.getFont().getFamily()
        ));
        writer.writeAttribute("textLength", Double.toString(text.getLayoutBounds().getWidth()));

        writer.writeAttribute("font-size", Double.toString(text.getFont().getSize()));

        writer.writeCharacters(text.getText());

        writer.writeEndElement();
    }
    private static void serializeParent(final javafx.scene.Parent parent, final Map<LinearGradient, String> gradients, final XMLStreamWriter writer) throws XMLStreamException {
        if(parent.getChildrenUnmodifiable().isEmpty()) {
            //Lacking children, there is nothing to do.
            return;
        }

        writer.writeStartElement("g");

        if(parent instanceof javafx.scene.layout.Region) {
            //Check for background(s)
            final javafx.scene.layout.Region region = (javafx.scene.layout.Region)parent;
            if(region.getBackground() != null && !region.getBackground().getFills().isEmpty()) {
                for(final javafx.scene.layout.BackgroundFill fill : region.getBackground().getFills()) {
                    writer.writeStartElement("rect");

                    writer.writeAttribute("x", "0");
                    writer.writeAttribute("y", "0");

                    writer.writeAttribute("width", Double.toString(region.getWidth()));
                    writer.writeAttribute("height", Double.toString(region.getHeight()));

                    //HACK: The radius of the top left corner is used and all corners are expected to use this radius.
                    writer.writeAttribute("rx", Double.toString(fill.getRadii().getTopLeftHorizontalRadius()));
                    writer.writeAttribute("ry", Double.toString(fill.getRadii().getTopLeftVerticalRadius()));

                    writer.writeAttribute("style", String.format("%s",
                            fillFromPaint(fill.getFill(), gradients)
                    ));

                    writer.writeEndElement();
                }
            }
            if(region.getBorder() != null) {
                writer.writeStartElement("rect");

                writer.writeAttribute("x", "0");
                writer.writeAttribute("y", "0");

                writer.writeAttribute("width", Double.toString(region.getWidth()));
                writer.writeAttribute("height", Double.toString(region.getHeight()));

                final List<BorderStroke> strokes = region.getBorder().getStrokes();
                if(strokes != null && !strokes.isEmpty()) {
                    final BorderStroke border = strokes.get(0);
                    writer.writeAttribute("rx", Double.toString(border.getRadii().getTopLeftHorizontalRadius()));
                    writer.writeAttribute("ry", Double.toString(border.getRadii().getTopLeftVerticalRadius()));

                    writer.writeAttribute("style", String.format("stroke:%s;stroke-width:%f;fill:transparent",
                            fromPaint(border.getTopStroke(), gradients),
                            border.getWidths().getTop()
                    ));
                }

                writer.writeEndElement();
            }
        }

        for(final javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
            serializeNode(child, gradients, writer);
        }

        writer.writeEndElement();
    }
    private static void serializeImage(final ImageView image, final XMLStreamWriter writer) throws XMLStreamException {
        //TODO: Transparency (including early termination for invisible images)
        if(image.getImage() == null) {
            return;
        }

        writer.writeStartElement("image");

        writer.writeAttribute("x", Double.toString(image.getX()));
        writer.writeAttribute("y", Double.toString(image.getY()));

        writer.writeAttribute("preserveAspectRatio", "none");

        writer.writeAttribute("width", Double.toString(image.getFitWidth() <= 0.0 ? image.prefWidth(0.0) : image.getFitWidth()));
        writer.writeAttribute("height", Double.toString(image.getFitHeight() <= 0.0 ? image.prefHeight(0.0) : image.getFitHeight()));

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            PixelReader pr = image.getImage().getPixelReader();

            Rectangle2D bounds = image.getViewport();
            if(bounds == null) {
                bounds = new Rectangle2D(0, 0, image.getImage().getWidth(), image.getImage().getHeight());
            }
            int iw = (int) bounds.getWidth();
            int ih = (int) bounds.getHeight();

            BufferedImage img = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
            IntegerComponentRaster icr = (IntegerComponentRaster) img.getRaster();
            int offset = icr.getDataOffset(0);
            int scan = icr.getScanlineStride();
            int data[] = icr.getDataStorage();
            WritablePixelFormat<IntBuffer> pf = PixelFormat.getIntArgbInstance();
            pr.getPixels((int)bounds.getMinX(), (int)bounds.getMinY(), iw, ih, pf, data, offset, scan);

            ImageIO.write(img, "png", stream);
        } catch(IOException ex) {
            //Ignore
        }

        writer.writeAttribute("http://www.w3.org/1999/xlink", "href", String.format("data:image/png;base64,%s", Base64.getEncoder().encodeToString(stream.toByteArray())));

        writer.writeEndElement();
    }
    //</editor-fold>
}
