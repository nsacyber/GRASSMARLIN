/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package ui.platform;

import core.types.EvictingList;
import org.apache.commons.io.FileUtils;
import ui.custom.FlatProgressBar;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;


/**
 * 2015.09.01 - CC - Calmed the memory bar down by using last 20 snapshots of memory usages to get average memeory use
 */
public class Footer extends UISplitPanel {
    final static double MB = FileUtils.ONE_MB;
    JLabel spinnerLabel;
    FlatProgressBar mon, loading;
    ImageIcon active, inactive;
    JPanel indicators, spinner;
    Timer timer;
    int monLastVal, loadLastVal;
    long lastTimeMs;
    Supplier<Integer> loadingProgress;
    List<Integer> memoryReadingsPercent = new EvictingList<>(20);
    List<Integer> memoryReadingscurrent = new EvictingList<>(20);
    CopyOnWriteArrayList<Supplier<String>> messageSuppliers;
    CopyOnWriteArrayList<JLabel> messageLabels;

    public Footer() {
        messageSuppliers = new CopyOnWriteArrayList<>();
        messageLabels = new CopyOnWriteArrayList<>();
        initComponent();
    }

    static Image get(Color fill, Color stroke) {
        Shape shape = new Ellipse2D.Double(12, 12, 78, 78);
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(fill);
        g.fill(shape);
        g.setStroke(new BasicStroke(10));
        g.setColor(stroke);
        g.draw(shape);
        return img.getScaledInstance(14, 14, BufferedImage.SCALE_AREA_AVERAGING);
    }

    public void setLoadingProgress(Supplier<Integer> loadingProgress) {
        this.loadingProgress = loadingProgress;
    }

    private void initComponent() {
        loadImages();
        monLastVal = loadLastVal = 0;
        lastTimeMs = 0;
        loading = new FlatProgressBar(true);
        mon = new FlatProgressBar(true);
        loading.setOnlyShowOnActivity(true);
        left.add(mon, LEFT);

        loading.setValue(0);
        loading.setDisplayText("Fingerprints");
        left.add(loading, LEFT);

        FlowLayout flow = new FlowLayout();
        indicators = new JPanel(flow);
        indicators.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        spinner = new JPanel();

        spinnerLabel = new JLabel();
        spinnerLabel.setHorizontalTextPosition(SwingConstants.LEFT);
        right.add(indicators, RIGHT);
        indicators.add(spinner);
        spinner.add(spinnerLabel);

        run();
        showSpinner(false);
        minInsets(indicators);
    }

    private void minInsets(Object comp) {
        if (comp instanceof JFrame)
            comp = ((JFrame) comp).getContentPane();
        if (comp instanceof JComponent) {
            JComponent c = (JComponent) comp;
            c.setBorder(BorderFactory.createEmptyBorder());
            LayoutManager l = c.getLayout();
            if (l instanceof FlowLayout) {
                FlowLayout f = (FlowLayout) l;
                f.setHgap(0);
                f.setVgap(0);

            }
            Arrays.asList(c.getComponents()).forEach(this::minInsets);
        }
    }

    public void hideSpinner() {
        this.showSpinner(false);
    }

    public void showSpinner() {
        this.showSpinner(true);
    }

    public void showSpinner(Boolean running) {
        SwingUtilities.invokeLater(() -> {
            spinnerLabel.setIcon(running ? active : inactive);
            spinner.revalidate();
        });
    }

    public FlatProgressBar getFingerprintProgressBar() {
        return loading;
    }

    private void loadImages() {
        active = new ImageIcon(get(Color.GREEN, Color.GRAY));
        inactive = new ImageIcon(get(Color.gray, Color.DARK_GRAY));
    }

    private double getAverage(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }

    /**
     * Adds a JLabel which will be updated on each tick of {@link Footer#run()}.
     * If the Supplier returns a null or empty String the Supplier will be removed.
     * @param messageSupplier Supplier of a message which will appear in the UI.
     */
    public void addIndicator(Supplier<String> messageSupplier) {
        if( Objects.nonNull(messageSupplier) ) {
            this.messageSuppliers.add(messageSupplier);
        }
        updateMessageIndacators();
    }

    /**
     * Removed unused indicators and updates the messages displayed for each JLabel.
     */
    private void updateMessageIndacators() {
        int supplierCount = this.messageSuppliers.size();
        int labelsCount = this.messageLabels.size();

        if( supplierCount != labelsCount ) {
            resizeTo(supplierCount, this.messageLabels);
        }

        Object[] suppliers = this.messageSuppliers.toArray();
        Object[] labels = this.messageLabels.toArray();

        for( int i = 0; i < suppliers.length; i++ ) {
            Supplier<String> supplier = (Supplier<String>) suppliers[i];
            JLabel label = (JLabel) labels[i];
            String msg = supplier.get();
            if( Objects.isNull(msg) || msg.isEmpty() ) {
                this.messageSuppliers.remove(supplier);
                label.setText("");
            } else {
                label.setText(msg);
            }
        }
    }

    /**
     * Removes the unused JLabels and creates new ones if necessary.
     * @param suppliers The number of suppliers there must be labels for.
     * @param messageLabels List of labels to modify.
     */
    private void resizeTo(int suppliers, List<JLabel> messageLabels) {
        while( messageLabels.size() > suppliers ) {
            JLabel label = messageLabels.remove(0);
            this.indicators.remove(label);
        }
        while ( messageLabels.size() < suppliers ) {
            JLabel label = new JLabel();
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            messageLabels.add(label);
            this.indicators.add(label);
        }
    }

    public void run() {
        if( timer == null ) {
            timer = new Timer(80 /** arbitrary delay */, this::runInternal);
            timer.start();
        } else if( !timer.isRunning() ) {
            timer.start();
        }
    }

    private void runInternal(Object e) {
        if (System.currentTimeMillis() - lastTimeMs > 1000) {

            lastTimeMs = System.currentTimeMillis();
            double total = Runtime.getRuntime().totalMemory();
            double current = total - Runtime.getRuntime().freeMemory();
            double percent = (current / total) * 100.0f;
            Integer val = (int) percent;

            if (monLastVal != val) {
                monLastVal = val;
                memoryReadingsPercent.add(val);
                memoryReadingscurrent.add((int) current);
                Double avCurr = getAverage(memoryReadingscurrent);
                Double avVal = getAverage(memoryReadingsPercent);
                mon.setValue(avVal.intValue());
                String used = Integer.toString((int) (avCurr / MB));
                String tot = Integer.toString((int) (total / MB));
                mon.setDisplayText("Mem ".concat(used.concat(" / ".concat(tot))));
                mon.repaint();
            }
        }

        updateMessageIndacators();

        if (loadingProgress != null) {
            int nval = loadingProgress.get();
            if (loadLastVal != nval) {
                loading.setVisible(true);
                loadLastVal = nval;
                loading.setValue(nval);
                loading.repaint();
            }
        } else if (loading.isVisible()) {
            loading.setVisible(false);
            loading.repaint();
        }
    }

}
