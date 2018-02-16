package grassmarlin.ui.common;

import grassmarlin.common.svg.Svg;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class DrawBuffer {
    private enum Instructions {
        DrawLine,
        DrawOval,
        DrawRectangle,
        DrawText,

        TransformTranslate,
        TransformRotate,
        TransformScale,
        TransformSave,
        TransformRestore
    }

    private static class Command {
        private final Instructions instruction;
        private final Object[] parameters;

        public Command(final Instructions instruction, final Object... parameters) {
            this.instruction = instruction;
            this.parameters = parameters;
        }

        public Instructions getInstruction() {
            return this.instruction;
        }
        public Object[] getParameters() {
            return this.parameters;
        }
    }

    private final List<Command> commands;

    public DrawBuffer() {
        this.commands = new LinkedList<>();
    }

    // == Drawing

    public void drawLine(final Paint stroke, final Double width, final Double x1, final Double y1, final Double x2, final Double y2) {
        this.commands.add(new Command(Instructions.DrawLine, stroke, width, x1, y1, x2, y2));
    }
    public void drawText(final Paint stroke, final Double width, final Paint fill, final Font font, final Double x, final Double y, final String text) {
        this.commands.add(new Command(Instructions.DrawText, stroke, width, fill, font, x, y, text));
    }
    public void drawRectangle(final Paint stroke, final Double strokeWidth, final Paint fill, final Double x, final Double y, final Double width, final Double height) {
        this.commands.add(new Command(Instructions.DrawRectangle, stroke, strokeWidth, fill, x, y, width, height));
    }
    public void drawOval(final Paint stroke, final Double strokeWidth, final Paint fill, final Double x, final Double y, final Double width, final Double height) {
        this.commands.add(new Command(Instructions.DrawOval, stroke, strokeWidth, fill, x, y, width, height));
    }

    // == Transformation

    public void rotate(final double degrees) {
        this.commands.add(new Command(Instructions.TransformRotate, degrees));
    }
    public void translate(final double x, final double y) {
        this.commands.add(new Command(Instructions.TransformTranslate, x, y));
    }
    public void scale(final double x, final double y) {
        this.commands.add(new Command(Instructions.TransformScale, x, y));
    }
    public void save() {
        this.commands.add(new Command(Instructions.TransformSave));
    }
    public void restore() {
        this.commands.add(new Command(Instructions.TransformRestore));
    }


    // == Rendering
    public void renderTo(final XMLStreamWriter svg) throws XMLStreamException {
        Transform transformCurrent = new Affine();
        final Stack<Transform> savedTransforms = new Stack<>();
        //We start in a group
        int levels = 1;
        svg.writeStartElement("g");
        boolean isInGroup = true;

        for(final Command cmd : this.commands) {
            //If a transform is processed, apply it to the current transform and move on to the next command
            switch(cmd.getInstruction()) {
                case TransformTranslate:
                    transformCurrent = transformCurrent.createConcatenation(Transform.translate((double) cmd.getParameters()[0], (double) cmd.getParameters()[1]));
                    continue;
                case TransformRotate:
                    transformCurrent = transformCurrent.createConcatenation(Transform.rotate(-(double) cmd.getParameters()[0], 0.0, 0.0));
                    continue;
                case TransformScale:
                    transformCurrent = transformCurrent.createConcatenation(Transform.scale((double) cmd.getParameters()[0], (double) cmd.getParameters()[1]));
                    continue;
            }
            switch(cmd.getInstruction()) {
                case TransformSave:
                    savedTransforms.push(transformCurrent);
                    transformCurrent = new Affine(transformCurrent);
                    levels++;
                    svg.writeStartElement("g");
                    isInGroup = true;
                    break;
                case TransformRestore:
                    isInGroup = false;
                    transformCurrent = savedTransforms.pop();
                    if(levels > 0) {
                        levels--;
                        svg.writeEndElement();
                    }
                    break;
            }
            //If we're about to draw something then we need to commit any transform we have waiting to the current group, if we are in a position to do so.
            if(isInGroup) {
                isInGroup = false;
                if(!transformCurrent.isIdentity()) {
                    svg.writeAttribute("transform", String.format("matrix(%f,%f,%f,%f,%f,%f)",
                            transformCurrent.getMxx(),
                            transformCurrent.getMxy(),
                            transformCurrent.getMyx(),
                            transformCurrent.getMyy(),
                            transformCurrent.getTx(),
                            transformCurrent.getTy()
                    ));
                }
                //Since the current transformation will be applied to all nested content, we can reset to the identity matrix for further transformations.
                transformCurrent = new Affine();
            }
            switch(cmd.getInstruction()) {
                case DrawLine:
                    svg.writeStartElement("line");

                    if(!transformCurrent.isIdentity()) {
                        svg.writeAttribute("transform", String.format("matrix(%f,%f,%f,%f,%f,%f)",
                                transformCurrent.getMxx(),
                                transformCurrent.getMxy(),
                                transformCurrent.getMyx(),
                                transformCurrent.getMyy(),
                                transformCurrent.getTx(),
                                transformCurrent.getTy()
                        ));
                    }

                    svg.writeAttribute("x1", Double.toString((Double)cmd.getParameters()[2]));
                    svg.writeAttribute("y1", Double.toString((Double)cmd.getParameters()[3]));
                    svg.writeAttribute("x2", Double.toString((Double)cmd.getParameters()[4]));
                    svg.writeAttribute("y2", Double.toString((Double)cmd.getParameters()[5]));

                    //noinspection MalformedFormatString
                    svg.writeAttribute("style", String.format("stroke:%s;stroke-width:%f",
                            Svg.fromPaint((Paint)cmd.getParameters()[0], null),
                            cmd.getParameters()[1]
                    ));

                    svg.writeEndElement();
                    break;
                case DrawOval:
                    svg.writeStartElement("ellipse");

                    if(!transformCurrent.isIdentity()) {
                        svg.writeAttribute("transform", String.format("matrix(%f,%f,%f,%f,%f,%f)",
                                transformCurrent.getMxx(),
                                transformCurrent.getMxy(),
                                transformCurrent.getMyx(),
                                transformCurrent.getMyy(),
                                transformCurrent.getTx(),
                                transformCurrent.getTy()
                        ));
                    }

                    svg.writeAttribute("cx", Double.toString((Double)cmd.getParameters()[3] + 0.5 * (Double)cmd.getParameters()[5]));
                    svg.writeAttribute("cy", Double.toString((Double)cmd.getParameters()[4] + 0.5 * (Double)cmd.getParameters()[6]));

                    svg.writeAttribute("rx", Double.toString((Double)cmd.getParameters()[5] / 2.0));
                    svg.writeAttribute("ry", Double.toString((Double)cmd.getParameters()[6] / 2.0));

                    //noinspection MalformedFormatString
                    svg.writeAttribute("style", String.format("stroke:%s;stroke-width:%f;%s",
                            Svg.fromPaint((Paint)cmd.getParameters()[0], null),
                            cmd.getParameters()[1],
                            Svg.fillFromPaint((Paint)cmd.getParameters()[2], null)
                    ));

                    svg.writeEndElement();
                    break;
                case DrawRectangle:
                    svg.writeStartElement("rect");

                    if(!transformCurrent.isIdentity()) {
                        svg.writeAttribute("transform", String.format("matrix(%f,%f,%f,%f,%f,%f)",
                                transformCurrent.getMxx(),
                                transformCurrent.getMxy(),
                                transformCurrent.getMyx(),
                                transformCurrent.getMyy(),
                                transformCurrent.getTx(),
                                transformCurrent.getTy()
                        ));
                    }

                    svg.writeAttribute("x", Double.toString((Double)cmd.getParameters()[3]));
                    svg.writeAttribute("y", Double.toString((Double)cmd.getParameters()[4]));

                    svg.writeAttribute("width", Double.toString((Double)cmd.getParameters()[5]));
                    svg.writeAttribute("height", Double.toString((Double)cmd.getParameters()[6]));

                    //noinspection MalformedFormatString
                    svg.writeAttribute("style", String.format("stroke:%s;stroke-width:%f;%s",
                            Svg.fromPaint((Paint)cmd.getParameters()[0], null),
                            cmd.getParameters()[1],
                            Svg.fillFromPaint((Paint)cmd.getParameters()[2], null)
                    ));

                    svg.writeEndElement();
                    break;
                case DrawText:
                    svg.writeStartElement("text");

                    if(!transformCurrent.isIdentity()) {
                        svg.writeAttribute("transform", String.format("matrix(%f,%f,%f,%f,%f,%f)",
                                transformCurrent.getMxx(),
                                transformCurrent.getMxy(),
                                transformCurrent.getMyx(),
                                transformCurrent.getMyy(),
                                transformCurrent.getTx(),
                                transformCurrent.getTy()
                        ));
                    }

                    svg.writeAttribute("x", Double.toString((Double)cmd.getParameters()[4]));
                    svg.writeAttribute("y", Double.toString((Double)cmd.getParameters()[5]));

                    //noinspection MalformedFormatString
                    svg.writeAttribute("style", String.format("stroke:%s;stroke-width:%f;%s;font-family:%s",
                            Svg.fromPaint((Paint)cmd.getParameters()[0], null),       //Stroke
                            cmd.getParameters()[1],                     //Stroke Width
                            Svg.fillFromPaint((Paint)cmd.getParameters()[2], null),   //Fill
                            ((Font)cmd.getParameters()[3]).getFamily()          //Font
                    ));
                    svg.writeAttribute("textLength", Double.toString(TextMeasurer.measureText((String)cmd.getParameters()[6], (Font)cmd.getParameters()[3]).getWidth()));

                    svg.writeAttribute("font-size", Double.toString(((Font)cmd.getParameters()[3]).getSize()));

                    svg.writeCharacters((String)cmd.getParameters()[6]);

                    svg.writeEndElement();
                    break;
            }
        }

        while(levels-- > 0) {
            svg.writeEndElement();
        }
    }
    public void renderTo(final GraphicsContext gc) {
        Paint fill = gc.getFill();
        Paint stroke = gc.getStroke();
        Double strokeWidth = gc.getLineWidth();
        Font font = gc.getFont();

        for(final Command cmd : this.commands) {
            switch(cmd.getInstruction()) {
                case DrawLine:
                    if(stroke == null || !stroke.equals(cmd.getParameters()[0])) {
                        stroke = (Paint)cmd.getParameters()[0];
                        gc.setStroke(stroke);
                    }
                    if(!strokeWidth.equals(cmd.getParameters()[1])) {
                        strokeWidth = (Double)cmd.getParameters()[1];
                        gc.setLineWidth(strokeWidth);
                    }
                    gc.strokeLine(
                            (Double)cmd.getParameters()[2], (Double)cmd.getParameters()[3],
                            (Double)cmd.getParameters()[4], (Double)cmd.getParameters()[5]
                    );
                    break;
                case DrawOval:
                    if(cmd.getParameters()[0] != null && (stroke == null || !stroke.equals(cmd.getParameters()[0]))) {
                        stroke = (Paint)cmd.getParameters()[0];
                        gc.setStroke(stroke);
                    }
                    //If the stroke is null, we will ignore the width.
                    if(cmd.getParameters()[0] != null && !strokeWidth.equals(cmd.getParameters()[1])) {
                        strokeWidth = (Double)cmd.getParameters()[1];
                        gc.setLineWidth(strokeWidth);
                    }
                    if(cmd.getParameters()[2] != null && (fill == null || !fill.equals(cmd.getParameters()[2]))) {
                        fill = (Paint)cmd.getParameters()[2];
                        gc.setFill(fill);
                    }
                    if(cmd.getParameters()[2] != null) {
                        gc.fillOval(
                                (Double)cmd.getParameters()[3], (Double)cmd.getParameters()[4],
                                (Double)cmd.getParameters()[5], (Double)cmd.getParameters()[6]
                        );
                    }
                    if(cmd.getParameters()[0] != null) {
                        gc.strokeOval(
                                (Double)cmd.getParameters()[3], (Double)cmd.getParameters()[4],
                                (Double)cmd.getParameters()[5], (Double)cmd.getParameters()[6]
                        );
                    }
                    break;
                case DrawRectangle:
                    if(cmd.getParameters()[0] != null && (stroke == null || !stroke.equals(cmd.getParameters()[0]))) {
                        stroke = (Paint)cmd.getParameters()[0];
                        gc.setStroke(stroke);
                    }
                    //If the stroke is null, we will ignore the width.
                    if(cmd.getParameters()[0] != null && !strokeWidth.equals(cmd.getParameters()[1])) {
                        strokeWidth = (Double)cmd.getParameters()[1];
                        gc.setLineWidth(strokeWidth);
                    }
                    if(cmd.getParameters()[2] != null && (fill == null || !fill.equals(cmd.getParameters()[2]))) {
                        fill = (Paint)cmd.getParameters()[2];
                        gc.setFill(fill);
                    }
                    if(cmd.getParameters()[2] != null) {
                        gc.fillRect(
                                (Double)cmd.getParameters()[3], (Double)cmd.getParameters()[4],
                                (Double)cmd.getParameters()[5], (Double)cmd.getParameters()[6]
                        );
                    }
                    if(cmd.getParameters()[0] != null) {
                        gc.strokeRect(
                                (Double)cmd.getParameters()[3], (Double)cmd.getParameters()[4],
                                (Double)cmd.getParameters()[5], (Double)cmd.getParameters()[6]
                        );
                    }
                    break;
                case DrawText:
                    if(cmd.getParameters()[0] != null && (stroke == null || !stroke.equals(cmd.getParameters()[0]))) {
                        stroke = (Paint)cmd.getParameters()[0];
                        gc.setStroke(stroke);
                    }
                    //If the stroke is null, we will ignore the width.
                    if(cmd.getParameters()[0] != null && !strokeWidth.equals(cmd.getParameters()[1])) {
                        strokeWidth = (Double)cmd.getParameters()[1];
                        gc.setLineWidth(strokeWidth);
                    }
                    if(cmd.getParameters()[2] != null && (fill == null || !fill.equals(cmd.getParameters()[2]))) {
                        fill = (Paint)cmd.getParameters()[2];
                        gc.setFill(fill);
                    }
                    if(cmd.getParameters()[3] != null && (font == null || !font.equals(cmd.getParameters()[3]))) {
                        font = (Font)cmd.getParameters()[3];
                        gc.setFont(font);
                    }

                    if(cmd.getParameters()[2] != null) {
                        gc.fillText((String)cmd.getParameters()[6], (Double)cmd.getParameters()[4], (Double)cmd.getParameters()[5]);
                    }
                    if(cmd.getParameters()[0] != null) {
                        gc.strokeText((String)cmd.getParameters()[6], (Double)cmd.getParameters()[4], (Double)cmd.getParameters()[5]);
                    }
                    break;
                case TransformTranslate:
                    gc.translate((double)cmd.getParameters()[0], (double)cmd.getParameters()[1]);
                    break;
                case TransformRotate:
                    gc.rotate((double)cmd.getParameters()[0]);
                    break;
                case TransformScale:
                    gc.scale((double)cmd.getParameters()[0], (double)cmd.getParameters()[1]);
                    break;
                case TransformSave:
                    gc.save();
                    break;
                case TransformRestore:
                    gc.restore();
                    fill = gc.getFill();
                    stroke = gc.getStroke();
                    strokeWidth = gc.getLineWidth();
                    font = gc.getFont();
                    break;
            }
        }
    }
}
