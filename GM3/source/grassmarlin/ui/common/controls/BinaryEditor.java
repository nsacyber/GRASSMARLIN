package grassmarlin.ui.common.controls;

import grassmarlin.Logger;
import grassmarlin.common.TextLengthBinding;
import grassmarlin.ui.common.TextMeasurer;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.Arrays;

public class BinaryEditor extends StackPane {
    private static class Label_internal extends Label {
        @Override
        public ObservableList<Node> getChildren() {
            return super.getChildren();
        }
    }
    private class Selection {
        int start = -1;
        int end = -1;
        boolean isHexEditable;

        public int getLowAddress() {
            return start < end ? start : end;
        }
        public int getHighAddress() {
            return start > end ? start : end;
        }
        public boolean isSelectionEmpty() {
            return start == -1;
        }

        public void startSelection(final int start, final boolean isHexEditable) {
            this.start = start;
            this.end = start;
            this.isHexEditable = isHexEditable;
            if(isHexEditable) {
                BinaryEditor.this.selectionHex.bind(BinaryEditor.this.selectionPrimary);
                BinaryEditor.this.selectionAscii.bind(BinaryEditor.this.selectionSecondary);
            } else {
                BinaryEditor.this.selectionHex.bind(BinaryEditor.this.selectionSecondary);
                BinaryEditor.this.selectionAscii.bind(BinaryEditor.this.selectionPrimary);
            }
        }
        public void endSelection(final int end) {
            this.end = end;
        }
        public void clear() {
            this.start = -1;
            this.end = -1;
        }
    }

    private class EditorRow extends HBox {
        private final int offset;
        private final StackPane paneHex;
        private final StackPane paneAscii;
        private final Label_internal lblHexText;
        private final Label_internal lblAsciiText;
        private final Rectangle selectionHex;
        private final Rectangle selectionAscii;

        private final double[] offsetStartsHex;
        private final double[] offsetEndsHex;
        private final double[] offsetsAscii;

        public EditorRow(int offset) {
            super(8.0);

            this.offset = offset;
            this.offsetStartsHex = new double[16];
            this.offsetEndsHex = new double[16];
            this.offsetsAscii = new double[17]; //We will be checking offset+1 for the end of the highlight

            final Label lblOffset = new Label();
            lblOffset.fontProperty().bind(BinaryEditor.this.fontProperty());
            lblOffset.prefWidthProperty().bind(BinaryEditor.this.offsetLabelWidthProperty());
            lblOffset.setText(String.format("%08x", this.offset));

            this.paneHex = new StackPane();
            this.paneHex.setAlignment(Pos.TOP_LEFT);
            this.lblHexText = new Label_internal();
            this.lblHexText.prefWidthProperty().bind(BinaryEditor.this.hexBlockWidthProperty());
            this.lblHexText.fontProperty().bind(BinaryEditor.this.fontProperty());
            this.selectionHex = new Rectangle();
            this.selectionHex.setVisible(false);
            this.selectionHex.fillProperty().bind(BinaryEditor.this.selectionHex);
            this.selectionHex.setStrokeWidth(0.0);
            this.paneHex.getChildren().addAll(this.selectionHex, this.lblHexText);

            this.paneAscii = new StackPane();
            this.paneAscii.setAlignment(Pos.TOP_LEFT);
            this.lblAsciiText = new Label_internal();
            this.lblAsciiText.prefWidthProperty().bind(BinaryEditor.this.asciiBlockWidthProperty());
            this.lblAsciiText.fontProperty().bind(BinaryEditor.this.fontProperty());
            this.selectionAscii = new Rectangle();
            this.selectionAscii.setVisible(false);
            this.selectionAscii.fillProperty().bind(BinaryEditor.this.selectionAscii);
            this.selectionAscii.setStrokeWidth(0.0);
            this.paneAscii.getChildren().addAll(this.selectionAscii, this.lblAsciiText);

            this.getChildren().addAll(
                    lblOffset,
                    paneHex,
                    paneAscii
            );

            this.invalidate();

            this.setOnMousePressed(this::handleMousePressed);
            this.setOnDragDetected(this::handleMouseDragStarted);
            this.setOnMouseDragOver(this::handleMouseDragOver);
            this.setOnMouseReleased(this::handleMouseReleased);
        }

        private void handleMousePressed(final MouseEvent event) {
            if(event.getButton() == MouseButton.PRIMARY) {
                if(event.isShiftDown()) {
                    if (event.getTarget().equals(this.lblHexText.getChildren().get(0))) {
                        BinaryEditor.this.processMouseDragged(this.offset + offsetOfHex(event.getX(), true));
                    } else {
                        BinaryEditor.this.processMouseDragged(this.offset + offsetOfAscii(event.getX()));
                    }
                } else {
                    if (event.getTarget().equals(this.lblHexText.getChildren().get(0))) {
                        BinaryEditor.this.processMousePressed(this.offset + offsetOfHex(event.getX(), true), true);
                    } else if (event.getTarget().equals(this.lblAsciiText.getChildren().get(0))) {
                        BinaryEditor.this.processMousePressed(this.offset + offsetOfAscii(event.getX()), false);
                    }
                }
                event.consume();
                BinaryEditor.this.requestFocus();
            }
        }
        private void handleMouseDragStarted(final MouseEvent event) {
            if(event.getButton() == MouseButton.PRIMARY) {
                BinaryEditor.this.startFullDrag();
                event.consume();
            }
        }
        private void handleMouseDragOver(final MouseEvent event) {
            //TODO: Ensure this is a drag related to the BinaryEditor
            if(BinaryEditor.this.selection.isHexEditable) {
                BinaryEditor.this.processMouseDragged(this.offset + offsetOfHex(event.getX(), true));
            } else {
                BinaryEditor.this.processMouseDragged(this.offset + offsetOfAscii(event.getX()));
            }
            event.consume();
        }
        private void handleMouseReleased(final MouseEvent event) {
            if(BinaryEditor.this.isSelecting() && event.getButton() == MouseButton.PRIMARY) {
                BinaryEditor.this.processMouseReleased();
                event.consume();
                BinaryEditor.this.requestFocus();
            }
        }

        protected int offsetOfAscii(final double pxX) {
            final double localX = pxX - paneAscii.getLayoutX();

            for(int idx = 0; idx < 16; idx++) {
                if(localX < this.offsetsAscii[idx + 1]) {
                    return idx;
                }
            }
            return 16;
        }

        protected int offsetOfHex(final double pxX, final boolean afterStart) {
            //HACK: We know the label belongs to a StackPane and the StackPane is subject to the HBox layout.
            final double localX = pxX - paneHex.getLayoutX();

            if(afterStart) {
                for(int idx = 15; idx >= 0; idx--) {
                    if(localX > offsetStartsHex[idx]) {
                        return idx;
                    }
                }
                return -1;
            } else {
                //If not afterStart, then testing beforeEnd
                for(int idx = 0; idx < 16; idx++) {
                    if(localX < offsetEndsHex[idx]) {
                        return idx;
                    }
                }
                return 16;
            }
        }

        public void invalidate() {
            final StringBuilder sbHex = new StringBuilder();
            final StringBuilder sbAscii = new StringBuilder();

            for(int idx = 0; idx < 16; idx++) {
                final Byte v = getByte(idx);
                if(v == null) {
                    sbAscii.append(' ');
                    sbHex.append("   ");
                } else {
                    final byte value = v;
                    //32-126 are printable, 128+ will be treated as negative
                    if(v < 32 || v == 127) {
                        sbAscii.append('.');
                    } else {
                        sbAscii.append((char) value);
                    }

                    final int valueUnsigned = ((int)value) & 0xFF;
                    sbHex.append(String.format("%02X ", valueUnsigned));
                }
                if(idx == 7) {
                    sbHex.append(' ');
                }
            }

            final String hex = sbHex.toString();
            if(!hex.equals(this.lblHexText.getText())) {
                this.lblHexText.setText(hex);
                for(int idxOffset = 0; idxOffset < 16; idxOffset++) {
                    this.offsetStartsHex[idxOffset] = TextMeasurer.measureText(hex.substring(0, idxOffset * 3 + (idxOffset >= 8 ? 1 : 0)), this.lblHexText.getFont()).getWidth();
                    this.offsetEndsHex[idxOffset] = TextMeasurer.measureText(hex.substring(0, idxOffset * 3 + (idxOffset >= 8 ? 3 : 2)), this.lblHexText.getFont()).getWidth();
                }
            }
            final String ascii = sbAscii.toString();
            if(!ascii.equals(this.lblAsciiText.getText())) {
                this.lblAsciiText.setText(ascii);
                for(int idxOffset = 0; idxOffset < 16; idxOffset++) {
                    this.offsetsAscii[idxOffset] = TextMeasurer.measureText(ascii.substring(0, idxOffset), this.lblAsciiText.getFont()).getWidth();
                }
                this.offsetsAscii[16] = TextMeasurer.measureText(ascii, this.lblAsciiText.getFont()).getWidth();
            }

            //Test for intersection of the selection and this row.
            if(BinaryEditor.this.selection.getLowAddress() < this.offset + 16 && BinaryEditor.this.selection.getHighAddress() >= this.offset) {
                final int colStart;
                if(BinaryEditor.this.selection.getLowAddress() <= this.offset) {
                    colStart = 0;
                } else {
                    colStart = BinaryEditor.this.selection.getLowAddress() - this.offset;
                }

                final int colEnd;
                if(BinaryEditor.this.selection.getHighAddress() >= this.offset + 15) {
                    colEnd = 15;
                } else {
                    colEnd = BinaryEditor.this.selection.getHighAddress() - this.offset;
                }

                final double pxHexStart = offsetStartsHex[colStart];
                final double pxHexEnd = offsetEndsHex[colEnd];
                this.selectionHex.setTranslateX(pxHexStart);
                this.selectionHex.setWidth(pxHexEnd - pxHexStart);

                this.selectionHex.setHeight(lblHexText.getHeight());
                this.selectionHex.setVisible(true);

                // == ASCII Highlight
                final double pxAsciiStart = offsetsAscii[colStart];
                final double pxAsciiEnd = offsetsAscii[colEnd + 1];
                this.selectionAscii.setTranslateX(pxAsciiStart);
                this.selectionAscii.setWidth(pxAsciiEnd - pxAsciiStart);

                this.selectionAscii.setHeight(lblAsciiText.getHeight());
                this.selectionAscii.setVisible(true);
            } else {
                this.selectionHex.setVisible(false);
                this.selectionAscii.setVisible(false);
            }
        }

        private Byte getByte(int column) {
            final int offsetFinal = this.offset + column;
            if(offsetFinal >= BinaryEditor.this.lengthBuffer) {
                return null;
            } else {
                return BinaryEditor.this.buffer[offsetFinal];
            }
        }
    }


    private byte[] buffer;
    private int lengthBuffer;
    private final Selection selection;
    private boolean selecting;
    private int cursor;
    private boolean cursorIsFirstInput;

    protected final VBox ctrRows;
    protected final Rectangle cursorBox;
    protected final SimpleObjectProperty<Color> selectionPrimary;
    protected final SimpleObjectProperty<Color> selectionSecondary;
    protected final SimpleObjectProperty<Color> selectionHex;
    protected final SimpleObjectProperty<Color> selectionAscii;

    public BinaryEditor() {
        this.buffer = new byte[128];
        this.lengthBuffer = 0;
        this.selection = new Selection();
        this.selecting = false;
        this.cursor = -1;
        this.cursorIsFirstInput = true;

        this.selectionPrimary = new SimpleObjectProperty<>(Color.LIGHTSKYBLUE);
        this.selectionSecondary = new SimpleObjectProperty<>(Color.DARKGRAY);
        this.selectionHex = new SimpleObjectProperty<>();
        this.selectionHex.bind(this.selectionPrimary);
        this.selectionAscii = new SimpleObjectProperty<>();
        this.selectionAscii.bind(this.selectionSecondary);

        this.ctrRows = new VBox();
        this.cursorBox = new Rectangle();
        this.cursorBox.setVisible(false);
        this.cursorBox.setFill(Color.TRANSPARENT);
        this.cursorBox.setStrokeWidth(1.0);
        this.cursorBox.setStroke(Color.BLACK);
        this.cursorBox.setOnMousePressed(event -> {
            BinaryEditor.this.processMousePressed(this.cursor, this.selection.isHexEditable);
        });
        this.cursorBox.setOnDragDetected(event -> {
            if(event.getButton() == MouseButton.PRIMARY) {
                BinaryEditor.this.startFullDrag();
                event.consume();
            }
        });
        this.setOnKeyPressed(event -> {
            //Navigation
            int cursorTarget = -2; //-2 is not a valid destination, so we can use that to see if this is a navigation command.
            switch(event.getCode()) {
                case ESCAPE:
                    //If pressing esc and there is no selection, then ignore the keypress (will probably close the containing dialog with a cancel result).
                    if(this.cursor == -1) {
                        return;
                    }
                    cursorTarget = -1;
                    break;
                case UP:
                    cursorTarget = Math.max(0, this.cursor - 16);
                    break;
                case DOWN:
                    cursorTarget = Math.min(lengthBuffer, this.cursor + 16);
                    break;
                case LEFT:
                    cursorTarget = Math.max(0, this.cursor - 1);
                    break;
                case RIGHT:
                    cursorTarget = Math.min(lengthBuffer, this.cursor + 1);
                    break;
                case HOME:
                    if(event.isControlDown()) {
                        cursorTarget = 0;
                    } else {
                        cursorTarget = this.cursor - (this.cursor % 16);
                    }
                    break;
                case END:
                    if(event.isControlDown()) {
                        cursorTarget = lengthBuffer;
                    } else {
                        cursorTarget = Math.min(this.lengthBuffer, this.cursor + 15 - (this.cursor % 16));
                    }
                    break;
                case DELETE:
                    deleteSelection(true);
                    this.selection.clear();
                    break;
                case BACK_SPACE:
                    deleteSelection(false);
                    this.selection.clear();
                    if(this.cursor > 0) {
                        this.cursor--;
                    }
                    break;
                default:
                    return;
            }

            if(cursorTarget != -2) {
                if(event.isShiftDown()) {
                    if(this.selection.isSelectionEmpty()) {
                        this.selection.startSelection(this.cursor, this.selection.isHexEditable);
                    }
                    this.cursor = cursorTarget;
                    this.selection.endSelection(this.cursor);
                } else {
                    this.cursor = cursorTarget;
                    this.selection.clear();
                }
            }

            BinaryEditor.this.invalidate();
            event.consume();
        });
        this.setOnKeyTyped(event -> {
            if(this.cursor >= 0) {
                if(this.selection.isHexEditable) {
                    switch (event.getCharacter()) {
                        case "0":
                        case "1":
                        case "2":
                        case "3":
                        case "4":
                        case "5":
                        case "6":
                        case "7":
                        case "8":
                        case "9":
                        case "A":
                        case "a":
                        case "B":
                        case "b":
                        case "C":
                        case "c":
                        case "D":
                        case "d":
                        case "E":
                        case "e":
                        case "F":
                        case "f":
                            final byte value = Byte.parseByte(event.getCharacter(), 16);

                            if(cursorIsFirstInput) {
                                this.writeValueAtCursor(value);
                                this.selection.clear();
                            } else {
                                this.writeValueAtCursor((this.buffer[this.cursor] << 4) | value);
                                this.cursor++;
                            }
                            cursorIsFirstInput = !cursorIsFirstInput;
                            this.invalidate();
                            event.consume();
                            break;
                        default:
                            break;
                    }
                } else {
                    // Process editing (ascii)
                    this.writeValueAtCursor((byte)event.getCharacter().charAt(0));
                    this.cursor++;
                    this.selection.clear();
                    this.invalidate();
                    event.consume();
                }
            }
        });

        this.setAlignment(Pos.TOP_LEFT);
        this.setFocusTraversable(true);

        this.font = new SimpleObjectProperty<>();
        //TODO: Font name from configuration
        if(Font.getFamilies().contains("Lucida Console")) {
            this.font.set(Font.font("Lucida Console"));
        } else if(Font.getFamilies().contains("Courier New")) {
            this.font.set(Font.font("Courier New"));
        } else if(Font.getFamilies().contains("Monospace")) {
            this.font.set(Font.font("Monospace"));
        } else {
            this.font.set(Font.getDefault());
        }

        // 'W' tends to be the widest character in a font so we use it for calculating the size.  We expect to use a monospace font but eventually this will be configurable.
        this.offsetLabelWidth = new TextLengthBinding(this.font, "WWWWWWWWWW");
        this.hexBlockWidth = new TextLengthBinding(this.font, "WW WW WW WW WW WW WW WW  WW WW WW WW WW WW WW WW ");
        this.asciiBlockWidth = new TextLengthBinding(this.font, "WWWWWWWWWWWWWWWW");

        this.getChildren().addAll(
                this.ctrRows,
                this.cursorBox
        );
    }

    // == Internal ==

    protected void growBuffer(final int lengthNew) {
        if(lengthNew <= this.buffer.length) {
            this.lengthBuffer = lengthNew;
        } else {
            int iterations = 0;
            while(this.buffer.length < lengthNew && iterations < 10) {
                growBuffer();
                iterations++;
            }
            if(iterations < 10) {
                this.lengthBuffer = lengthNew;
            } else {
                Logger.log(Logger.Severity.ERROR, "Unable to grow buffer to size of %d (%d)", lengthNew, this.buffer.length);
            }
        }
    }
    protected void growBuffer() {
        this.buffer = Arrays.copyOf(this.buffer, this.buffer.length * 2);
    }

    protected void invalidate() {
        final int cntRowsDesired = (this.lengthBuffer + 16) / 16;
        if(this.ctrRows.getChildren().size() < cntRowsDesired) {
            // Add rows
            for(int idx = this.ctrRows.getChildren().size(); idx < cntRowsDesired; idx++) {
                this.ctrRows.getChildren().add(new EditorRow(idx * 16));
            }
        } else if(this.ctrRows.getChildren().size() > cntRowsDesired) {
            // Remove rows
            while((this.ctrRows.getChildren().size() > cntRowsDesired)) {
                this.ctrRows.getChildren().remove(this.ctrRows.getChildren().size() - 1);
            }
        }

        for(int idx = 0; idx < this.ctrRows.getChildren().size(); idx++) {
            ((EditorRow)this.ctrRows.getChildren().get(idx)).invalidate();
        }

        if(this.cursor >= 0) {
            final EditorRow row = (EditorRow)(ctrRows.getChildren().get(this.cursor / 16));
            this.cursorBox.setTranslateY(row.getLayoutY() - 1.0);
            this.cursorBox.setHeight(row.getHeight() + 1.0);

            //HACK: Depends on structure of EditorRow to get the layout x/y
            if(this.selection.isHexEditable) {
                this.cursorBox.setTranslateX(row.paneHex.getLayoutX() + row.offsetStartsHex[cursor - row.offset] - 1.0);
                this.cursorBox.setWidth(row.offsetEndsHex[cursor - row.offset] - row.offsetStartsHex[cursor - row.offset] + 1.0);
            } else {
                this.cursorBox.setTranslateX(row.paneAscii.getLayoutX() + row.offsetsAscii[cursor - row.offset] - 1.0);
                this.cursorBox.setWidth(row.offsetsAscii[cursor - row.offset + 1] - row.offsetsAscii[cursor - row.offset] + 1.0);
            }

            this.cursorBox.setVisible(true);
        } else {
            this.cursorBox.setVisible(false);
        }
    }

    // == Editability ==

    protected void writeValueAtCursor(final int value) {
        this.writeValueAtCursor((byte)value);
    }
    protected void writeValueAtCursor(final byte value) {
        //TODO: Check for insert/overwrite mode.

        //Overwrite
        //If the cursor is at the byte after the buffer's end, then it is a single selected byte and we're appending
        if(this.cursor == this.lengthBuffer) {
            this.growBuffer(this.lengthBuffer + 1);
            this.buffer[cursor] = value;
        } else if(this.selection.start == this.selection.end) {
            //If selection start matches end then either a single byte is selected or nothing is selected.
            //In either case, we will replace the byte at the cursor, provided the cursor is valid
            if(this.cursor >= 0) {
                this.buffer[this.cursor] = value;
            }
        } else {
            //Multiple bytes are selected; replace all those bytes with a single one.
            //We're going to perform this replacement at the low end of the selection and then bulk copy the content after the end of the selection to the following bytes, then shrink the total size.
            this.cursor = this.selection.getLowAddress();
            this.buffer[this.cursor] = value;
            final int span = this.selection.getHighAddress() - this.selection.getLowAddress();
            for(int idx = 1; idx < this.lengthBuffer - this.selection.getHighAddress(); idx++) {
                this.buffer[this.selection.getLowAddress() + idx] = this.buffer[this.selection.getHighAddress() + idx];
            }
            this.lengthBuffer -= span;
        }
    }
    protected void deleteSelection(final boolean removeAtCursor) {
        if(this.selection.isSelectionEmpty()) {
            //If there is no cursor, delete nothing.
            if(this.cursor == -1) {
                return;
            }
            if(removeAtCursor) {
                // Remove the character that is at the cursor
                System.arraycopy(this.buffer, this.cursor + 1, this.buffer, this.cursor, this.lengthBuffer - 1 - this.cursor);
            } else {
                // Remove the character before the cursor
                if(cursor > 0) {
                    System.arraycopy(this.buffer, this.cursor - 1 + 1, this.buffer, this.cursor - 1, this.lengthBuffer - 1 - (this.cursor - 1));
                }
            }
            this.lengthBuffer--;
        } else {
            // Delete the selection, point set the cursor to the first character of what used to be selected.
            this.cursor = this.selection.getLowAddress();
            final int span = this.selection.getHighAddress() - this.selection.getLowAddress() + 1;
            for(int idx = 0; idx < this.lengthBuffer - this.selection.getHighAddress(); idx++) {
                this.buffer[this.selection.getLowAddress() + idx] = this.buffer[this.selection.getHighAddress() + idx + 1];
            }
            this.lengthBuffer -= span;
        }
    }

    // == Highlighting ==

    protected boolean isSelecting() {
        return this.selecting;
    }
    protected void processMousePressed(final int offset, final boolean inHexEditor) {
        //Selecting the character after the end of the buffer is valid; that is how data is appended, after all.
        final int start;
        if(offset > this.lengthBuffer) {
            start = this.lengthBuffer;
        } else if(offset< 0)  {
            start = 0;
        } else {
            start = offset;
        }


        this.selecting = true;
        this.selection.startSelection(start, inHexEditor);

        this.cursor = start;
        this.cursorIsFirstInput = true;

        this.invalidate();
    }
    protected void processMouseDragged(final int offset) {
        final int end;

        if(this.selection.start == this.lengthBuffer && offset >= this.lengthBuffer) {
            //The cursor is on the byte after the end of data and we're selecting at or after that point:
            // Select only the byte after the buffer.
            // The cursor is unaffected.
            end = this.lengthBuffer;
        } else if(this.selection.start == this.lengthBuffer) {
            //The cursor is on the byte after the end of data and we're selecting before that point:
            // Move the start to the last data point
            // Update the selection normally (we know that offset <= start)
            this.cursor = this.selection.start = this.lengthBuffer - 1;
            end = Math.max(0, offset);
        } else {
            //The cursor is somewhere in valid data, so just update the endpoint
            if(offset >= this.lengthBuffer) {
                end = Math.max(0, this.lengthBuffer - 1);
            } else if(offset < 0)  {
                end = 0;
            } else {
                end = offset;
            }
        }

        this.selection.endSelection(end);
        this.cursor = end;
        this.invalidate();
    }
    protected void processMouseReleased() {
        this.selecting = false;
        this.invalidate();
    }

    // == Accessors ==

    public byte[] getBytes() {
        return Arrays.copyOf(this.buffer, this.lengthBuffer);
    }

    public void setBytes(final byte[] bytes) {
        this.buffer = Arrays.copyOf(bytes, bytes.length + 16);  //Leaves a little room to grow before we have to reallocate memory.  Also ensures size > 0 so that growing the buffer can double and always be positive.
        this.lengthBuffer = bytes.length;

        this.invalidate();
    }

    private final SimpleObjectProperty<Font> font;
    public ObjectProperty<Font> fontProperty() {
        return this.font;
    }
    public Font getFont() {
        return this.font.get();
    }
    public void setFont(final Font font) {
        this.font.set(font);
    }

    private final DoubleExpression offsetLabelWidth;
    protected DoubleExpression offsetLabelWidthProperty() {
        return this.offsetLabelWidth;
    }

    private final DoubleExpression hexBlockWidth;
    protected DoubleExpression hexBlockWidthProperty() {
        return this.hexBlockWidth;
    }

    private final DoubleExpression asciiBlockWidth;
    protected DoubleExpression asciiBlockWidthProperty() {
        return this.asciiBlockWidth;
    }
}
