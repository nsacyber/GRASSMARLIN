package core.ui.graph;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * The LineGraph is used to create line graphs with high and low standard deviations for the daily standup
 * up meetings.  These graphs are displayed on the stats jsps in ProductViewer  
 * 2009.05.19 - SD - New...
 * 2009.05.28 - CC - pushed the labels into their own object
 * 2009.05.28 - SD - added bar graph methods and capabilities
 * 2009.06.18 - SD - Modified determineYMaxMark so that the ceiling of the graph would correctly be computed
 * 2009.07.08 - CC - cleaned up drawChartAndGraphRectangles and moved the drawing of rectangles into separate method, added transparency option
 * 2009.11.06 - SD - Changed many ints to longs in order to handle graphs with very large collection numbers
 * 2009.11.13 - SD - Changed the bar graph so that a 1 pixel line shows up at a bars base when normally the value is
 * 					too insignificant to graph compared to the scale of the y axis
 * 2009.11.16 - SD - Made the legend for the bar and line graph slightly bigger and altered the colored 
 * 					bars and lines within the legend smaller so there is more room for legend text 
 */

public class LineAndBarGrapher{
	//TODO: SD - remove all commented out code
	
	/** default settings for generating the graphic */
	
	/** The number of scale divisions (lines) on the y axis*/
	private int yNumOfDivs = 5;
	/** the pixel radius of the points drawn on the line graph*/
	private int pointRadius = 1;
	/** the pixel width of the entire graph*/
	private int WIDTH = 800;
	/** the pixel height of the entire graph*/
	private int HEIGHT = 600;
	/** the pixel height of the actual line graph in the chart*/
	private int graphHeight = Math.round(HEIGHT*10/16);//(int)Math.round(HEIGHT*3/4);
	/** the pixel width of the actual line graph in the chart*/
	private int graphWidth = Math.round(WIDTH*10/16);//(int)Math.round(WIDTH*3/4);
	/** the y pixel position of the top of the line graph*/
	private int graphTop = HEIGHT*3/16;//HEIGHT/8;
	/** the y pixel position of the bottom of the line graph*/
	private int graphBottom = HEIGHT*13/16;//HEIGHT*7/8;
	/** the x pixel position of the left side of the line graph*/
	private int graphLeft = WIDTH*2/16;//WIDTH/8;
	/** the x pixel position of the right side of the line graph*/
	private int graphRight = WIDTH*12/16;//WIDTH*7/8;
	/** the back ground color of the entire graph*/
	private Color chartBackgroundColor = Color.LIGHT_GRAY;
	/** the color of the border of the entire chart, legend, and the line graph*/
	private Color borderColor = Color.BLACK;
	/** the color of the line graph background*/
	private Color graphBackgroundColor = Color.WHITE;
	/** the color of the legend background*/
	private Color legendBackgroundColor = Color.WHITE;
	/** the color of the legend border*/
	private Color legendBorderColor = Color.BLACK;
	/** color of the title text*/
	private Color titleColor = Color.BLACK;
	/** color of the x and y label text*/
	private Color labelColor = Color.BLACK;
	/** color of the dots and lines in the line graph*/
	private Color lineColor = Color.BLACK;
	/** color of the x and y axis labels for the division amounts*/
	private Color scaleDivisionColor = Color.BLACK;
	/** color of the high standard deviation line*/
	private Color stdDevHighColor = Color.RED;
	/** color of the low standard deviation line*/
	private Color stdDevLowColor = Color.GREEN;
	/** array of colors used to fill in the bars on the graph*/
	private Color[] barColors = {Color.ORANGE, Color.BLUE, Color.GREEN, Color.RED, Color.MAGENTA, Color.CYAN};
	private boolean useTransparentBackgrounds = false;
		
	
	/** color of bar borders on the graph */
	private Color barBorderColor = Color.BLACK;
	/** font used for the x and y axis label divisions and the legend*/
	private Font normFont = new Font("serrif", Font.PLAIN, 12);
	/** font used for the title of the graph*/
	private Font titleFont = new Font("serrif",Font.BOLD,20);
	/** font used for the labels on the x and y axis*/
	private Font axisLabelFont = new Font("serrif",Font.BOLD,16);
	/** font used in the legend to label the lines*/
	private Font legendLabelFont = new Font("serrif",Font.PLAIN,14);
	/** dimension of the entire graph*/
	private Dimension graphDim = new Dimension(WIDTH, HEIGHT);
	/** rectangle used to draw and fill the entire graph*/
	private Rectangle graphRect = new Rectangle(graphDim);
	/** dimensions of the line graph part of the chart*/
	private Dimension lineGraphDim = new Dimension(graphWidth, graphHeight);
	/** rectangle used to draw and fill the line graph portion of the graph*/
	private Rectangle lineGraphRect = new Rectangle(lineGraphDim);
	/** y pixel location of the top of the legend*/
	private int legendTop = HEIGHT*3/8;
	/** y pixel location of the bottom of the legend*/
	private int legendBottom = HEIGHT*5/8;
	/** x pixel location of the right of the legend*/
	private int legendRight = WIDTH*63/64;//WIDTH*62/64;
	/** x pixel location of the left of the legend*/
	private int legendLeft = WIDTH*49/64;//WIDTH*50/64;
	/** pixel width of the legend*/
	private int legendWidth = Math.round(WIDTH*12/64);
	/** pixel height of the legend*/
	private int legendHeight = Math.round(HEIGHT/4);
	/** dimensions of the legend part of the chart*/
	private Dimension legendDim = new Dimension(legendWidth, legendHeight);
	/** rectangle used to draw and fill the legend portion of the graph*/
	private Rectangle legendRect = new Rectangle(legendDim);
	/** */
	private String lineLegendLabel = "Collected";
	/** */
	private String lowLegendLabel = "Low";
	/** */
	private String highLegendLabel = "High";
	
	/** default constructor for the line graph*/
	public LineAndBarGrapher(){
		
	}
	
	/** overwrites default settings for the pie chart using height and width parameters.
	 * The graph looks the best when the height is 75% of the width
	 * @param width The pixel width of the entire graph
	 * @param height The pixel height of the entire graph
	 */
	public LineAndBarGrapher(int width, int height){
		this.WIDTH = width;
		this.HEIGHT = height;
		this.graphHeight = this.HEIGHT*10/16;//(int)Math.round(HEIGHT*3/4);
		this.graphWidth = this.WIDTH*10/16;//(int)Math.round(WIDTH*3/4);
		this.graphTop = this.HEIGHT*3/16;//HEIGHT/8;
		this.graphBottom = this.HEIGHT*13/16;//HEIGHT*7/8;
		this.graphLeft = this.WIDTH*2/16;//WIDTH/8;
		this.graphRight = this.WIDTH*12/16;//WIDTH*7/8;
		this.graphDim = new Dimension(this.WIDTH, this.HEIGHT);
		this.graphRect = new Rectangle(this.graphDim);
		this.lineGraphDim = new Dimension(this.graphWidth, this.graphHeight);
		this.lineGraphRect = new Rectangle(this.lineGraphDim);
		this.legendTop = this.HEIGHT*3/8;
		this.legendBottom = this.HEIGHT*5/8;
		this.legendRight = this.WIDTH*63/64;//62/64
		this.legendLeft = this.WIDTH*49/64;//50/64
		this.legendWidth = (int)Math.round(this.WIDTH*12/64);
		this.legendHeight = (int)Math.round(this.HEIGHT/4);
		this.legendDim = new Dimension(this.legendWidth, this.legendHeight);
		this.legendRect = new Rectangle(this.legendDim);
		this.lineGraphRect.setLocation((int)Math.round(this.WIDTH/4), (int)Math.round(this.HEIGHT/6));
		
	}
	
	/** draws a line graph without adding the standard deviation high and low lines
	 * @param graphTitle The title of the graph
	 * @param xLabel The String to label the x axis with
	 * @param yLabel The String to label the y axis with
	 * @param dataPoints A HashMap containing the x axis label of a point (key) 
	 * 		and the corresponding y value for that point
	 */
	public BufferedImage drawLineGraph(String graphTitle, String xLabel, String yLabel, 
			HashMap<ComparableLabel,Integer> dataPoints){
		return drawLineGraphWithStdDev(graphTitle, xLabel, yLabel, dataPoints, null, null, null);
	}
	
	/** draws a line graph with the standard deviation lines without a user defined maximum y axis value 
	 * for the graph
	 * @param graphTitle The title of the graph
	 * @param xLabel The String to label the x axis with
	 * @param yLabel The String to label the y axis with
	 * @param dataPoints A HashMap containing the x axis label of a point (key) 
	 * 		and the corresponding y value for that point
	 * @param stdDevHigh The value of the graphs high standard deviation. If null the line is not graphed
	 * @param stdDevLow The value of the graphs low standard deviation. If null the line is not graphed
	 */
	public BufferedImage drawLineGraph(String graphTitle, String xLabel, String yLabel, 
			HashMap<ComparableLabel,Integer> dataPoints, Long stdDevHigh, 
			Long stdDevLow){
		return drawLineGraphWithStdDev(graphTitle, xLabel, yLabel, dataPoints, stdDevHigh, stdDevLow, null);
	}
	
	/** draws a line graph with the standard deviation high and low lines
	 * @param graphTitle The title of the graph
	 * @param xLabel The String to label the x axis with
	 * @param yLabel The String to label the y axis with
	 * @param dataPoints A HashMap containing the x axis label of a point (key) 
	 * 		and the corresponding y value for that point
	 * @param stdDevHigh The value of the graphs high standard deviation. If null the line is not graphed
	 * @param stdDevLow The value of the graphs low standard deviation. If null the line is not graphed
	 * @param graphMaxValue Used to manually specify the maximum value for the y axis. If this is null then 
	 * 		the maximum value on the y axis is automatically calculated.  Using the defualt number of 
	 * 		y axis divisions (5), this number should be divisable by 5 and larger than the maximum
	 * 		y value of the dataPoints passed in
	 */
	public BufferedImage drawLineGraphWithStdDev(String graphTitle, String xLabel, String yLabel, 
			HashMap<ComparableLabel,Integer> dataPoints, Long stdDevHigh, 
			Long stdDevLow, Long graphMaxValue){
			
		BufferedImage bi = new BufferedImage(this.WIDTH, this.HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		//set Antialiasing 
		RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHints(renderHints);
		
		drawChartAndGraphRectangles(g2d);
		drawLineLegend(g2d, "Legend", stdDevHigh, stdDevLow);
		
		drawTitleAndLabels(g2d, graphTitle, xLabel, yLabel);
		
		//get min and max y axis values
		Set<ComparableLabel> dateKeySet = dataPoints.keySet();
		long maxValue = determineMaxYValue(dataPoints, dateKeySet);
		
		long yMaxMark = 0;
		int yLabelXOffset = 50;
		
		//determine largest value on the y axis
		yMaxMark = determineYMaxMark(maxValue, stdDevHigh, graphMaxValue);
				
		drawYAxisDivisions(g2d, yMaxMark, yLabelXOffset);
		
		//order the dataPoints by date
		ArrayList<ComparableLabel> orderedDateSet = new ArrayList<ComparableLabel>();
		orderedDateSet.addAll(dateKeySet);
		Collections.sort(orderedDateSet);
		
		drawXAxisDivisions(g2d, orderedDateSet);
		
		//draw line graph
		drawLinesAndPoints(g2d, orderedDateSet, dataPoints, yMaxMark);
		
		//draw std dev lines
		if(stdDevHigh!=null){
			drawStdDevLine(g2d, stdDevHigh, yMaxMark, this.stdDevHighColor);
		}
		if(stdDevLow!=null){
			drawStdDevLine(g2d, stdDevLow, yMaxMark, this.stdDevLowColor);
		}
		
		return bi;
	}
	
	/** draws a line graph without adding the standard deviation high and low lines
	 * @param graphTitle The title of the graph
	 * @param xLabel The String to label the x axis with
	 * @param yLabel The String to label the y axis with
	 * @param barDataPoints A HashMap containing the x axis label of the bars (key) 
	 * 		and the corresponding y value for the top of the bars in the graph
	 * @param barLabels The String array of labels for the bars that goes in the legend.  This array needs 
	 * 		to be the same length as the Integer Array in the HashMap
	 */
	public BufferedImage drawBarGraph(String graphTitle, String xLabel, String yLabel, 
			HashMap<ComparableLabel,Integer[]> barDataPoints, String[] barLabels){
		return drawBarGraphWithStdDev(graphTitle, xLabel, yLabel, barDataPoints, barLabels, null, null, null);
	}
	
	/** draws a line graph without adding the standard deviation high and low lines
	 * @param graphTitle The title of the graph
	 * @param xLabel The String to label the x axis with
	 * @param yLabel The String to label the y axis with
	 * @param barDataPoints A HashMap containing the x axis label of the bars (key) 
	 * 		and the corresponding y value for the top of the bars in the graph
	 * @param barLabels The String array of labels for the bars that goes in the legend.  This array needs 
	 * 		to be the same length as the Integer Array in the HashMap
	 * @param stdDevHigh The value of the graphs high standard deviation. If null the line is not graphed
	 * @param stdDevLow The value of the graphs low standard deviation. If null the line is not graphed
	 */
	public BufferedImage drawBarGraphWithStdDev(String graphTitle, String xLabel, String yLabel, 
			HashMap<ComparableLabel,Integer[]> barDataPoints, String[] barLabels, Long stdDevHigh, 
			Long stdDevLow){
		return drawBarGraphWithStdDev(graphTitle, xLabel, yLabel, barDataPoints, barLabels, 
				stdDevHigh, stdDevLow, null);
	}
	
	/** draws a line graph with the standard deviation high and low lines
	 * @param graphTitle The title of the graph
	 * @param xLabel The String to label the x axis with
	 * @param yLabel The String to label the y axis with
	 * @param barDataPoints A HashMap containing the x axis label of the bars (key) 
	 * 		and the corresponding y value for the top of the bars in the graph
	 * @param barLabels The String array of labels for the bars that goes in the legend.  This array needs 
	 * 		to be the same length as the Integer Array in the HashMap
	 * @param stdDevHigh The value of the graphs high standard deviation. If null the line is not graphed
	 * @param stdDevLow The value of the graphs low standard deviation. If null the line is not graphed
	 * @param graphMaxValue Used to manually specify the maximum value for the y axis. If this is null then 
	 * 		the maximum value on the y axis is automatically calculated.  Using the defualt number of 
	 * 		y axis divisions (5), this number should be divisable by 5 and larger than the maximum
	 * 		y value of the dataPoints passed in
	 */
	public BufferedImage drawBarGraphWithStdDev(String graphTitle, String xLabel, String yLabel, 
			HashMap<ComparableLabel,Integer[]> barDataPoints, String[] barLabels, Long stdDevHigh, 
			Long stdDevLow, Long graphMaxValue){
		
		BufferedImage bi = new BufferedImage(this.WIDTH, this.HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		//set Antialiasing 
		RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHints(renderHints);
		
		drawChartAndGraphRectangles(g2d);
		drawBarLegend(g2d, "Legend", barLabels, stdDevHigh, stdDevLow);
		
		drawTitleAndLabels(g2d, graphTitle, xLabel, yLabel);
		
		//determine max y value for both the bars
		Collection<Integer[]> valueCollection = barDataPoints.values();
		Set<ComparableLabel> dateKeySet = barDataPoints.keySet();
		long maxValue = 0;
		for(Integer[] intArray:valueCollection){
			for(Integer intValue:intArray){
				if(maxValue<intValue){
					maxValue = intValue;
				}
			}
		}	
		
		long yMaxMark = 0;
		int yLabelXOffset = 50;
		
		//determine largest value on the y axis
		yMaxMark = determineYMaxMark(maxValue, stdDevHigh, graphMaxValue);
		
		drawYAxisDivisions(g2d, yMaxMark, yLabelXOffset);
		
		//order the dataPoints by date
		ArrayList<ComparableLabel> orderedDateSet = new ArrayList<ComparableLabel>();
		orderedDateSet.addAll(dateKeySet);
		Collections.sort(orderedDateSet);
		
		drawXAxisDivisions(g2d, orderedDateSet);
				
		//draw both bars on graph
		drawBarsOnGraph(g2d, orderedDateSet, barDataPoints, yMaxMark);		
		
		//draw std dev lines
		if(stdDevHigh!=null){
			drawStdDevLine(g2d, stdDevHigh, yMaxMark, this.stdDevHighColor);
		}
		if(stdDevLow!=null){
			drawStdDevLine(g2d, stdDevLow, yMaxMark, this.stdDevLowColor);
		}
		return bi;
	}
	
	/** draws the background for the entire graph and the background for the line graph.  Uses the height and 
	 * width attributes of the class to determine the dimensions 
	 * @param g2d The java 2d object to draw the chart and graph rectangles on
	 */
	public void drawChartAndGraphRectangles(Graphics2D g2d){
		Composite bgComposite = g2d.getComposite();
		if(this.useTransparentBackgrounds) {
			bgComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f);
		}
		drawRectangleWithCompositeFill(g2d, this.graphRect, 0, 0, this.borderColor, this.chartBackgroundColor, bgComposite);
		drawRectangleWithCompositeFill(g2d, this.lineGraphRect, this.graphLeft, this.graphTop, this.borderColor, this.graphBackgroundColor, bgComposite);
		
		//set whole graph background color to gray and draw black border
		//Composite composite = g2d.getComposite();
		//g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f));
		//g2d.setColor(this.chartBackgroundColor);
		//this.graphRect.setLocation(0, 0);
		//g2d.fill(this.graphRect);
		//g2d.setComposite(composite);
		//g2d.setColor(this.borderColor);
		//g2d.draw(this.graphRect);
		
		//fill in line graph background with white and draw black border
//		g2d.setColor(this.graphBackgroundColor);
//		this.lineGraphRect.setLocation(this.graphLeft, this.graphTop);
//		g2d.fill(this.lineGraphRect);
//		g2d.setColor(this.borderColor);
//		g2d.draw(this.lineGraphRect);
	}
	
	/**
	 * Draws a rectangle with the given composite for the fill.  
	 * for transparent use: AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f);
	 * @param g2d
	 * @param rectangle
	 * @param xLocation
	 * @param yLocation
	 * @param boarderColor
	 * @param bgColor
	 * @param fillComposit
	 */
	public void drawRectangleWithCompositeFill(Graphics2D g2d, Rectangle rectangle, int xLocation,int yLocation, Color boarderColor, Color bgColor, Composite fillComposit) {
		Composite opaqueComposite = g2d.getComposite();
		rectangle.setLocation(xLocation, yLocation);
		
		//fill with composite
		g2d.setComposite(fillComposit);
		g2d.setColor(bgColor);
		g2d.fill(rectangle);		
		
		//draw the outline
		g2d.setComposite(opaqueComposite);
		g2d.setColor(boarderColor);
		g2d.draw(rectangle);
	}
	
	/** */
	public void drawLineLegend(Graphics2D g2d, String legendTitle, Long stdDevHigh, Long stdDevLow){
		Composite bgComposite = g2d.getComposite();
		if(this.useTransparentBackgrounds) {
			bgComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f);
		}
		drawRectangleWithCompositeFill(g2d, this.legendRect, this.legendLeft, this.legendTop, this.legendBorderColor, this.legendBackgroundColor, bgComposite);
		
//		g2d.setColor(this.legendBackgroundColor);
//		this.legendRect.setLocation(this.legendLeft, this.legendTop);
//		g2d.fill(this.legendRect);
//		g2d.setColor(this.legendBorderColor);
//		g2d.draw(this.legendRect);
		g2d.setColor(this.labelColor);
		g2d.setFont(this.axisLabelFont);
		int legendWidthDivs = 14;
		g2d.drawString(legendTitle, this.legendLeft+this.legendWidth/2-
				legendTitle.length()*this.axisLabelFont.getSize()/4, this.legendTop-this.axisLabelFont.getSize());
		if(stdDevHigh == null && stdDevLow==null){
			//just include line in legend
			g2d.setColor(this.lineColor);
			//g2d.drawLine(this.legendLeft+this.legendWidth/12, this.legendBottom - this.legendHeight/2, 
			//		this.legendLeft+this.legendWidth*5/12, this.legendBottom - this.legendHeight/2);
			g2d.drawLine(this.legendLeft+this.legendWidth/legendWidthDivs, this.legendBottom - this.legendHeight/2, 
					this.legendLeft+this.legendWidth*3/12, this.legendBottom - this.legendHeight/2);
			
			g2d.fillOval(this.legendLeft + this.legendWidth*2/legendWidthDivs - this.pointRadius, 
					this.legendBottom - this.legendHeight/2 - this.pointRadius, 
					this.pointRadius*2, this.pointRadius*2);
			g2d.setColor(this.labelColor);
			g2d.setFont(this.legendLabelFont);
			g2d.drawString(this.lineLegendLabel, this.legendLeft+this.legendWidth*4/14, this.legendBottom - 
					this.legendHeight/2);
		}
		else if(stdDevHigh == null){
			//include line and low in legend
			g2d.setColor(this.lineColor);
			g2d.drawLine(this.legendLeft+this.legendWidth/legendWidthDivs, this.legendBottom - this.legendHeight*2/3, 
					this.legendLeft+this.legendWidth*3/legendWidthDivs, this.legendBottom - this.legendHeight*2/3);
			g2d.fillOval(this.legendLeft + this.legendWidth*2/legendWidthDivs - this.pointRadius, 
					this.legendBottom - this.legendHeight*2/3 - this.pointRadius, 
					this.pointRadius*2, this.pointRadius*2);
			g2d.setColor(this.stdDevLowColor);
			g2d.drawLine(this.legendLeft+this.legendWidth/legendWidthDivs, this.legendBottom - this.legendHeight/3, 
					this.legendLeft+this.legendWidth*3/legendWidthDivs, this.legendBottom - this.legendHeight/3);
			g2d.fillOval(this.legendLeft + this.legendWidth*2/legendWidthDivs - this.pointRadius, 
					this.legendBottom - this.legendHeight/3 - this.pointRadius, 
					this.pointRadius*2, this.pointRadius*2);
			g2d.setColor(this.labelColor);
			g2d.setFont(this.legendLabelFont);
			g2d.drawString(this.lineLegendLabel, this.legendLeft+this.legendWidth*4/legendWidthDivs, this.legendBottom - 
					this.legendHeight*2/3);
			g2d.drawString(this.lowLegendLabel, this.legendLeft+this.legendWidth*4/legendWidthDivs, this.legendBottom - 
					this.legendHeight/3);
		}
		else if(stdDevLow == null){
			//include line and high in legend
			g2d.setColor(this.stdDevHighColor);
			g2d.drawLine(this.legendLeft+this.legendWidth/legendWidthDivs, this.legendBottom - this.legendHeight*2/3, 
					this.legendLeft+this.legendWidth*3/legendWidthDivs, this.legendBottom - this.legendHeight*2/3);
			g2d.setColor(this.lineColor);
			g2d.drawLine(this.legendLeft+this.legendWidth/legendWidthDivs, this.legendBottom - this.legendHeight/3, 
					this.legendLeft+this.legendWidth*3/legendWidthDivs, this.legendBottom - this.legendHeight/3);
			g2d.fillOval(this.legendLeft + this.legendWidth*2/legendWidthDivs - this.pointRadius, 
					this.legendBottom - this.legendHeight/3 - this.pointRadius, 
					this.pointRadius*2, this.pointRadius*2);
			g2d.setColor(this.labelColor);
			g2d.setFont(this.legendLabelFont);
			g2d.drawString(this.highLegendLabel, this.legendLeft+this.legendWidth*4/legendWidthDivs, this.legendBottom - 
					this.legendHeight*2/3);
			g2d.drawString(this.lineLegendLabel, this.legendLeft+this.legendWidth*4/legendWidthDivs, this.legendBottom - 
					this.legendHeight/3);
		}
		else{
			//include line, low, and high in legend
			g2d.setColor(this.stdDevHighColor);
			g2d.drawLine(this.legendLeft+this.legendWidth/legendWidthDivs, this.legendBottom - this.legendHeight*3/4, 
					this.legendLeft+this.legendWidth*3/legendWidthDivs, this.legendBottom - this.legendHeight*3/4);
			g2d.setColor(this.lineColor);
			g2d.drawLine(this.legendLeft+this.legendWidth/legendWidthDivs, this.legendBottom - this.legendHeight/2, 
					this.legendLeft+this.legendWidth*3/legendWidthDivs, this.legendBottom - this.legendHeight/2);
			g2d.fillOval(this.legendLeft + this.legendWidth*2/legendWidthDivs - this.pointRadius, 
					this.legendBottom - this.legendHeight/2 - this.pointRadius, 
					this.pointRadius*2, this.pointRadius*2);
			g2d.setColor(this.stdDevLowColor);
			g2d.setFont(this.legendLabelFont);
			g2d.drawLine(this.legendLeft+this.legendWidth/legendWidthDivs, this.legendBottom - this.legendHeight/4, 
					this.legendLeft+this.legendWidth*3/legendWidthDivs, this.legendBottom - this.legendHeight/4);
			g2d.setColor(this.labelColor);
			g2d.drawString(this.highLegendLabel, this.legendLeft+this.legendWidth*4/legendWidthDivs, this.legendBottom - 
					this.legendHeight*3/4);
			g2d.drawString(this.lineLegendLabel, this.legendLeft+this.legendWidth*4/legendWidthDivs, this.legendBottom - 
					this.legendHeight/2);
			g2d.drawString(this.lowLegendLabel, this.legendLeft+this.legendWidth*4/legendWidthDivs, this.legendBottom - 
					this.legendHeight/4);
		}
	}
	
	/** */
	public void drawBarLegend(Graphics2D g2d, String legendTitle, String[] barLabels, Long stdDevHigh, 
			Long stdDevLow){
			
		int numOfBars = barLabels.length;
		int totalBars = numOfBars;
		//this.logger.debug("num of bars: "+numOfBars);
		//this.logger.debug("total bars: "+totalBars);
		
		if(stdDevHigh != null){
			totalBars++;
		}
		if(stdDevLow != null){
			totalBars++;
		}
		
		if(totalBars > 4 && totalBars <= 8){
			lengthenLegend();
		}
		else if(totalBars > 8){
			//TODO throw error
			return;
		}
		
		int legendDivs = totalBars+1;
		int legendDivHeight = this.legendHeight/legendDivs;
		int legendWidthDivs = 14;
		//int barHeight = legendDivHeight/2;
		//int barWidth = this.legendWidth/3;
		int barHeight = legendDivHeight/3;
		int barWidth = this.legendWidth*2/legendWidthDivs;
		
		
		Composite bgComposite = g2d.getComposite();
		if(this.useTransparentBackgrounds) {
			bgComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f);
		}
		drawRectangleWithCompositeFill(g2d, this.legendRect, this.legendLeft, this.legendTop, this.legendBorderColor, this.legendBackgroundColor, bgComposite);
		
		//g2d.setColor(this.legendBackgroundColor);
		//this.legendRect.setLocation(this.legendLeft, this.legendTop);
		//g2d.fill(this.legendRect);
		//g2d.setColor(this.legendBorderColor);
		//g2d.draw(this.legendRect);
		
		g2d.setColor(this.labelColor);
		g2d.setFont(this.axisLabelFont);
		g2d.drawString(legendTitle, this.legendLeft+this.legendWidth/2-
				legendTitle.length()*this.axisLabelFont.getSize()/4, this.legendTop-this.axisLabelFont.getSize());
		int labelPos;
		for(labelPos=1; labelPos<=numOfBars;labelPos++){
			drawBar(g2d, barHeight, barWidth, this.legendLeft+this.legendWidth/legendWidthDivs, 
					this.legendTop+labelPos*legendDivHeight+barHeight/2, this.barColors[labelPos-1]);
			g2d.setColor(this.labelColor);
			g2d.setFont(this.legendLabelFont);
			g2d.drawString(barLabels[labelPos-1], this.legendLeft+this.legendWidth*4/legendWidthDivs, 
					this.legendTop+labelPos*legendDivHeight+barHeight/2);
		}
		if(stdDevHigh!=null){
			drawBar(g2d, 3, barWidth, this.legendLeft+this.legendWidth/legendWidthDivs, 
					this.legendTop+labelPos*legendDivHeight+1, this.stdDevHighColor);
			g2d.setColor(this.labelColor);
			g2d.setFont(this.legendLabelFont);
			g2d.drawString(this.highLegendLabel, this.legendLeft+this.legendWidth*4/legendWidthDivs, 
					this.legendTop+labelPos*legendDivHeight+1);
			labelPos++;
		}
		if(stdDevLow!=null){
			drawBar(g2d, 3, barWidth, this.legendLeft+this.legendWidth/legendWidthDivs, 
					this.legendTop+labelPos*legendDivHeight+1, this.stdDevLowColor);
			g2d.setColor(this.labelColor);
			g2d.setFont(this.legendLabelFont);
			g2d.drawString(this.lowLegendLabel, this.legendLeft+this.legendWidth*4/legendWidthDivs, 
					this.legendTop+labelPos*legendDivHeight+1);
			labelPos++;
		}
	}
	
	/** */
	public void lengthenLegend(){
		this.legendHeight = (int)Math.round(this.HEIGHT/4);
		this.legendTop = this.HEIGHT/4;
		this.legendBottom = this.HEIGHT*3/4;
		this.legendDim = new Dimension(this.legendWidth, this.legendHeight);
		this.legendRect = new Rectangle(this.legendDim);
		
	}
	
	/** draws the title of the graph along with the the x and y axis labels
	 * @param g2d The java 2d object to draw the title and labels on
	 * @param graphTitle The String that will drawn at the top of the graph as the title
	 * @param xLabel The String to label the x axis with
	 * @param yLabel The String to label the y axis with
	 * */
	public void drawTitleAndLabels(Graphics2D g2d, String graphTitle, String xLabel, String yLabel){
		//draw title
		g2d.setColor(this.titleColor);
		g2d.setFont(this.titleFont);
		int xTitleOffset = graphTitle.length()*this.titleFont.getSize()/4;
		g2d.drawString(graphTitle, Math.round(this.WIDTH/2)-xTitleOffset, Math.round(this.HEIGHT/16));
		//draw x label
		g2d.setColor(this.labelColor);
		g2d.setFont(this.axisLabelFont);
		int xLabelOffset = xLabel.length()*this.axisLabelFont.getSize()/4;
		g2d.drawString(xLabel, Math.round(this.WIDTH/2)-xLabelOffset, Math.round(this.HEIGHT*31/32));
		//draw y label
		g2d.translate(0, this.HEIGHT);
		g2d.rotate(-Math.PI/2.0);
		int yLabelOffset = yLabel.length()*this.axisLabelFont.getSize()/4;
		g2d.drawString(yLabel, this.HEIGHT/2-yLabelOffset, this.WIDTH/32);
		g2d.rotate(Math.PI/2.0);
		g2d.translate(0, -this.HEIGHT);
		g2d.setFont(this.normFont);
	}
	
	/** Returns the maximum y value for the set of datapoints used to draw the line graph *IF* a maximum 
	 * value was for the y axis was not passed in
	 * @param dataPoints The HashMap containing all the y values to be graphed
	 * @param dateKeySet The ordered Set of all the dates (keys) for the dataPoints HashMap 
	 * */
	public int determineMaxYValue(HashMap<ComparableLabel, Integer> dataPoints, Set<ComparableLabel> dateKeySet){
		Integer maxValue = Integer.MIN_VALUE;
		Integer  minValue = Integer.MAX_VALUE;
		
		for(ComparableLabel date: dateKeySet){
			int tempInt = dataPoints.get(date);
			if(tempInt<minValue){
				minValue = tempInt;
			}
			if(tempInt>maxValue){
				maxValue = tempInt;
			}
		}
		return maxValue;
	}
	
	/** draws the scale labels and horizontal y axis division lines on the line graph
	 * @param g2d The java 2d object to draw the y axis scale marks on
	 * @param yMaxMark The largest y scale mark on the line graph
	 * @param yLabelXOffset The pixel distance left of the y axis to draw the y axis scale labels
	 * */
	public void drawYAxisDivisions(Graphics2D g2d, long yMaxMark, int yLabelXOffset){
		
		int yGraphDiv = this.graphHeight/this.yNumOfDivs;
		long yDivWidth = yMaxMark/this.yNumOfDivs;
		int digitsToDrop = determineNumberOfDigitsToDrop(yMaxMark);
		String yAxisScaleLabel = determineYAxisScaleLabel(digitsToDrop);
		//draw y axis divisions
		for(int i = 0; i<this.yNumOfDivs; i++){
			//Float yScaleLabel = roundToDecimalPlaces(yDivWidth*i, 4);
			Long yScaleLabel = yDivWidth*i;
			int yOffsetOfLabel = Math.round(yGraphDiv*i);
			g2d.setColor(this.scaleDivisionColor);
			if(i != 0){
				g2d.drawString(dropDigitsFromYAxisLabel(yScaleLabel.toString(), digitsToDrop),
						this.graphLeft-yLabelXOffset, this.graphBottom-yOffsetOfLabel);
			}
			else{
				g2d.drawString(yScaleLabel.toString(),
						this.graphLeft-yLabelXOffset, this.graphBottom-yOffsetOfLabel);
			}
			g2d.setColor(this.borderColor);
			g2d.drawLine(this.graphLeft, this.graphBottom-yOffsetOfLabel, this.graphRight, 
					this.graphBottom-yOffsetOfLabel);
		}
		g2d.setColor(this.scaleDivisionColor);
		g2d.drawString(dropDigitsFromYAxisLabel(yMaxMark + "", digitsToDrop),
				this.graphLeft - yLabelXOffset, this.graphTop);
		if(yAxisScaleLabel!=null){
			g2d.setColor(this.labelColor);
			g2d.setFont(this.axisLabelFont);
			g2d.drawString(yAxisScaleLabel, 
					this.graphLeft - yAxisScaleLabel.length()*this.axisLabelFont.getSize()/2, 
					this.graphTop-this.axisLabelFont.getSize());
		}
	}
	
	/** draws the scale labels and the x axis labels on the line graph
	 * @param g2d The java 2d object to draw the datapoints and connecting lines on
	 * @param orderedDateSet The ArrayList of ordered dates used to get the x axis labels
	 * */
	public void drawXAxisDivisions(Graphics2D g2d, ArrayList<ComparableLabel> orderedDateSet){
		int numberOfPoints = orderedDateSet.size();
		int step =1;
//		if(numberOfPoints > 10) {
//			numberOfPoints = 10;
//			step = orderedDateSet.size()/10;
//		}
//		int sectionWidth = this.graphWidth/orderedDateSet.size();
		int sectionWidth = this.graphWidth/numberOfPoints;
		int xOffset = sectionWidth/2;
		int yOffset = 15;
		int labelSpacing = 0;
		String delimString = "\n";
		//draw X Labels
		g2d.setColor(this.scaleDivisionColor);
		g2d.setFont(this.normFont);

		for(int i=0;i<numberOfPoints;i++){
			String label = orderedDateSet.get(i*step).getLabel();
			AxisLabel axisLabel = new AxisLabel(i*sectionWidth+this.graphLeft+xOffset,this.graphBottom+yOffset,
												label, sectionWidth-labelSpacing, this.axisLabelFont);
			axisLabel.drawLabel(g2d);
		}
	}
	
	/** draws the datapoints and the lines connecting the datapoints on the line graph
	 * @param g2d The java 2d object to draw the datapoints and connecting lines on
	 * @param orderedDateSet The ArrayList of ordered dates used to access the y values in the HashMap
	 * @param dataPoints The HashMap containing the y values of the dataPoints.  The keys are the dates
	 * associated with the y values contained in the orderedDateSet
	 * @param yMaxMark The maximum y axis value on the graph
	 * */
	public void drawLinesAndPoints(Graphics2D g2d, ArrayList<ComparableLabel> orderedDateSet,
			HashMap<ComparableLabel, Integer> dataPoints, long yMaxMark){
		int sectionWidth = this.graphWidth/orderedDateSet.size();
		int xOffset = sectionWidth/2;
		int yPrevValue;
		int yCurrentValue;
		float prevOffsetPerc;
		float currentOffsetPerc;
		g2d.setColor(this.lineColor);
		for(int i=0;i<orderedDateSet.size();i++){
			//draw lines and points
			if(i != 0){
				yPrevValue = dataPoints.get(orderedDateSet.get(i-1));
				yCurrentValue = dataPoints.get(orderedDateSet.get(i));
				prevOffsetPerc = yPrevValue/(float)yMaxMark;
				currentOffsetPerc = yCurrentValue/(float)yMaxMark;		
				g2d.drawLine(this.graphLeft+(i-1)*sectionWidth+xOffset, 
						this.graphBottom-Math.round(prevOffsetPerc*this.graphHeight), 
						this.graphLeft+i*sectionWidth+xOffset, 
						Math.round(this.graphBottom-currentOffsetPerc*this.graphHeight));
//				g2d.fillOval(this.graphLeft+(i)*sectionWidth+xOffset-this.pointRadius,
//						this.graphBottom-Math.round(currentOffsetPerc*this.graphHeight)-this.pointRadius,
//						this.pointRadius*2,	this.pointRadius*2);
				
			}
			else{
				//draw first point
				yCurrentValue = dataPoints.get(orderedDateSet.get(i));
				currentOffsetPerc = yCurrentValue/(float)yMaxMark;
				g2d.fillOval(this.graphLeft+(i)*sectionWidth+xOffset-this.pointRadius, 
						this.graphBottom-Math.round(currentOffsetPerc*this.graphHeight)-this.pointRadius, 
						this.pointRadius*2,	this.pointRadius*2);
			}
		}
	}
	
	/** draws the first and second set of bars on the bar graph
	 * @param g2d The java 2d object to draw the datapoints and connecting lines on
	 * @param orderedDateSet The ArrayList of ordered dates used to access the y values in the HashMap
	 * @param barDataPoints The HashMap containing the y values of the dataPoints for the 
	 * bars.  The keys are the dates associated with the y values contained in the orderedDateSet
	 * @param yMaxMark The maximum y axis value on the graph
	 * */
	public void drawBarsOnGraph(Graphics2D g2d, ArrayList<ComparableLabel> orderedDateSet,
			HashMap<ComparableLabel, Integer[]> barDataPoints, long yMaxMark){
		
		int sectionWidth = this.graphWidth/orderedDateSet.size();
		int xOffset = sectionWidth/2;
		int yValue;
		float yOffsetPerc;
		int numberOfBars = barDataPoints.get(orderedDateSet.get(0)).length;
		int barWidth = sectionWidth/(numberOfBars+1);
		for(int datePos=0;datePos<orderedDateSet.size();datePos++){
			for(int barNumber=0;barNumber<numberOfBars;barNumber++){
				yValue = barDataPoints.get(orderedDateSet.get(datePos))[barNumber];
				yOffsetPerc = yValue/(float)yMaxMark;
				int xLeftBar = this.graphLeft+datePos*sectionWidth+barWidth/2+barWidth*barNumber;
				drawBar(g2d, Math.round(this.graphHeight*yOffsetPerc), barWidth, 
						xLeftBar, this.graphBottom, this.barColors[barNumber]);
				//if bar height == 0 and y value != 0, draw a line of width one where bar would start
				if(Math.round(this.graphHeight*yOffsetPerc) == 0 && yValue != 0){
					g2d.setColor(this.barColors[barNumber]);
					g2d.drawLine(xLeftBar, this.graphBottom, xLeftBar+barWidth, this.graphBottom);
				}
			}			
		}		
	}
	
	/** draw a bar on the bar graph
	 * @param g2d The java 2d object to draw the datapoints and connecting lines on
	 * @param barHeight The height of the bar in pixels
	 * @param barWidth The width of the bar in pixels
	 * @param xLeftPoint the x pixel value of the left side of the bar
	 * @param yGraphBottom The y pixel location of the bottom of the graph
	 * @param barColor The color of the inside of the bar
	 * */ 
	public void drawBar(Graphics2D g2d, int barHeight, int barWidth, int xLeftPoint, int yGraphBottom, 
			Color barColor){
		Dimension barDim = new Dimension(barWidth, barHeight);
		Rectangle barRect = new Rectangle(barDim);
		g2d.setColor(barColor);
		barRect.setLocation(xLeftPoint, yGraphBottom-barHeight);
		g2d.fill(barRect);
		g2d.setColor(this.barBorderColor);
		g2d.draw(barRect);
	}
	
	/** Used to draw the low and high standard devition lines on the line graph
	 * @param g2d The java 2d object to draw the standard deviation lines on
	 * @param stdDevVal The y value of the horizontal line to be drawn
	 * @param yMaxMark The maximum y axis value on the graph
	 * @param color The color of the horizontal line to be drawn
	 * */
	public void drawStdDevLine(Graphics2D g2d, Long stdDevVal, long yMaxMark, Color color){
		float stdDevPerc = stdDevVal/(float)yMaxMark;
		g2d.setColor(color);
		int yPosOfLine = this.graphBottom - Math.round(this.graphHeight*stdDevPerc);
		g2d.drawLine(this.graphLeft, yPosOfLine, this.graphRight, yPosOfLine);
	}
	
	/** Used to determine the maximum mark on the y scale taking into account the maximum y value of all the
	 * points, the high std dev value, and the maximum value the user defined for the y axis
	 * @param maxValue the largest y value from any of the data points
	 * @param stdDevHigh The maximum y value of the high std dev
	 * @param graphMaxValue The maximum y axis value the user defined
	 * */
	public long determineYMaxMark(long maxValue, Long stdDevHigh, Long graphMaxValue){
		long yMaxValue = maxValue;
		//determine if using the max y mark passed in, or generating own max mark
		if(graphMaxValue == null && stdDevHigh == null){
			if(yMaxValue<this.yNumOfDivs){
				yMaxValue = this.yNumOfDivs;
			}
			yMaxValue = determineCeilingFromMaxValue(yMaxValue, this.yNumOfDivs);
		}
		else if(stdDevHigh != null){ 
			//if a std dev high is passed in but no graph max value
			if(stdDevHigh > yMaxValue){
				yMaxValue = stdDevHigh;	
			}
			if(yMaxValue < this.yNumOfDivs){
				yMaxValue = this.yNumOfDivs;
			}
			yMaxValue = determineCeilingFromMaxValue(yMaxValue, this.yNumOfDivs);
		}
		else if(graphMaxValue != null){ 
			//if a graph max value but no std dev high is passed in
			yMaxValue = graphMaxValue;
			if(yMaxValue < this.yNumOfDivs){
				//this.yNumOfDivs = graphMaxValue;
				yMaxValue = this.yNumOfDivs;
			}
			yMaxValue = determineCeilingFromMaxValue(yMaxValue, this.yNumOfDivs);
		}
		else{ 
			//if a std dev high value and a graph max value are passed in
			if(stdDevHigh > graphMaxValue){
				yMaxValue = stdDevHigh;	
			}
			else{
				yMaxValue = graphMaxValue;
			}
			if(yMaxValue < this.yNumOfDivs){
				//this.yNumOfDivs = yMaxValue;
				yMaxValue = this.yNumOfDivs;
			}
			yMaxValue = determineCeilingFromMaxValue(yMaxValue, this.yNumOfDivs);
		}
		return yMaxValue;
	}
	
	/** */
	public int determineNumberOfDigitsToDrop(long maxValue){
		int digitsToDrop = 0;
		if(maxValue > 99999 && maxValue < 100000000){
			digitsToDrop = 3;
		}
		return digitsToDrop;
	}
	
	/** */
	public String dropDigitsFromYAxisLabel(String label, int digitsDropped){
		
		return label.substring(0, label.length()-digitsDropped);
	}
	
	/** */
	public String determineYAxisScaleLabel(int digitsDropped){
		String label = null;
		if(digitsDropped == 3){
			label = "Thousands";
		}
		else if(digitsDropped == 6){
			label = "Millions";
		}
		return label;
	}
	
	/** Returns a float representing a number rounded to a specific number of decimal places
	 * @param numToRound The number to round
	 * @param decPlaces The number of decimal places to round to
	 * */
	public static float roundToDecimalPlaces(float numToRound, int decPlaces){
		float p = (float)Math.pow(10, decPlaces);
		numToRound = numToRound*p;
		float tmp = Math.round(numToRound);
		return (float)tmp/p;
	}
	
	/** Returns a integer for the maximum y value to be used by the graph that is divisible by the number
	 * of y scale divisions.
	 * @param number The current maximum value of the graph
	 * @param yNumOfDivs The number of divisions on the y scale
	 * */
	public static long determineCeilingFromMaxValue(long number, int yNumOfDivs){
		long tempInt = number;
		tempInt = tempInt/5+tempInt;
		if(tempInt<100){
			while(tempInt%yNumOfDivs!=0){
				tempInt++;
			}
			return tempInt;
		}
		else{
			int counter = 0;
			String numStr = ""+number;
			for(int i=0; i<numStr.length()-2; i++){
				tempInt = tempInt/10;
				counter++;
			}
			tempInt++;
			while(tempInt%yNumOfDivs!=0){
				tempInt++;
			}
			return (long)Math.pow(10, counter)*tempInt;
		}
	}
	
	/**
	 * @param chartBackgroundColor the chartBackgroundColor to set
	 */
	public void setChartBackgroundColor(Color chartBackgroundColor) {
		this.chartBackgroundColor = chartBackgroundColor;
	}
	
	/**
	 * @param borderColor the borderColor to set
	 */
	public void setBorderColor(Color borderColor) {
		this.borderColor = borderColor;
	}

	/**
	 * @param graphBackgroundColor the graphBackgroundColor to set
	 */
	public void setGraphBackgroundColor(Color graphBackgroundColor) {
		this.graphBackgroundColor = graphBackgroundColor;
	}

	/**
	 * @param titleColor the titleColor to set
	 */
	public void setTitleColor(Color titleColor) {
		this.titleColor = titleColor;
	}

	/**
	 * @param labelColor the labelColor to set
	 */
	public void setLabelColor(Color labelColor) {
		this.labelColor = labelColor;
	}

	/**
	 * @param lineColor the lineColor to set
	 */
	public void setLineColor(Color lineColor) {
		this.lineColor = lineColor;
	}

	/**
	 * @param scaleDivisionColor the scaleDivisionColor to set
	 */
	public void setScaleDivisionColor(Color scaleDivisionColor) {
		this.scaleDivisionColor = scaleDivisionColor;
	}

	/**
	 * @param stdDevHighColor the stdDevHighColor to set
	 */
	public void setStdDevHighColor(Color stdDevHighColor) {
		this.stdDevHighColor = stdDevHighColor;
	}

	/**
	 * @param stdDevLowColor the stdDevLowColor to set
	 */
	public void setStdDevLowColor(Color stdDevLowColor) {
		this.stdDevLowColor = stdDevLowColor;
	}

	/**
	 * @param legendBackgroundColor the legendBackgroundColor to set
	 */
	public void setLegendBackgroundColor(Color legendBackgroundColor) {
		this.legendBackgroundColor = legendBackgroundColor;
	}

	/**
	 * @param legendBorderColor the legendBorderColor to set
	 */
	public void setLegendBorderColor(Color legendBorderColor) {
		this.legendBorderColor = legendBorderColor;
	}

	/**
	 * @param legendLabelFont the legendLabelFont to set
	 */
	public void setLegendLabelFont(Font legendLabelFont) {
		this.legendLabelFont = legendLabelFont;
	}

	/**
	 *	A smart multiline x axis label that knows how to draw itself 
	 *
	 */
	private class AxisLabel{
		/** the left bottom of the first line of the label */
		private int xVal;
		/** the left bottom of the first line of the label */
		private int yVal;
		/** the label itself */
		private String strLabel;
		/** the max width in pexels of this label */
		private int maxPixelWidth;
		/** the string denoting a newline */
		private String delimString = "\n";
		/** a list of strings for each line of the label */
		private ArrayList<String> splitAndTruncatedLabelList = new ArrayList<String>();
		/** the font of the label for calculating metrics */
		private Font labelFont;
		
		/**
		 * constructs a new axis label and initializes the label so its ready to draw
		 * @param xVal
		 * @param yVal
		 * @param strLabel
		 * @param maxPixelWidth
		 * @param labelFont
		 */
		public AxisLabel(int xVal, int yVal, String strLabel, int maxPixelWidth, Font labelFont) {
			this.xVal = xVal;
			this.yVal = yVal;
			this.strLabel = strLabel;
			this.maxPixelWidth = maxPixelWidth;
			this.labelFont = labelFont;
			//splits the label into mutiple lines
			splitLabel();
			//trims each line so its no wider than maxPixelWidth
			trimLabelStrings(calculateMaxCharacterWidthBasedOnPixelWidth());	
		}
		
		/**
		 * Calculates the max number of characters this font can have within the given pixel width
		 * @return
		 */
		private int calculateMaxCharacterWidthBasedOnPixelWidth() {
			//generate the test font string
			String testWidthString = "";
			for(int i = 0 ; i < 25 ; i ++) {
				testWidthString = testWidthString + "A.";
			}
			BufferedImage temp = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
			Graphics2D tempG2d = temp.createGraphics();;
			FontMetrics metrics = tempG2d.getFontMetrics(this.labelFont);
			//slowely reduce the size by one character at a time untill the pixel width is acceptable
			int strWidth = metrics.stringWidth(testWidthString);
			while( strWidth > this.maxPixelWidth ){
				testWidthString = testWidthString.substring(0, testWidthString.length()-1);
				strWidth = metrics.stringWidth(testWidthString);
			}
			return testWidthString.length();
		}

		/**
		 * simply splits the string based on the delimeter into multiple lines
		 */
		private void splitLabel() {
			if(this.strLabel.contains(this.delimString)) {
				this.splitAndTruncatedLabelList.addAll(Arrays.asList(this.strLabel.split(this.delimString)));
			}
			else {
				this.splitAndTruncatedLabelList.add(this.strLabel);
			}
		}

		/**
		 * trims each string to be less than the mac char width
		 * @param charWidth
		 */
		private void trimLabelStrings(int charWidth) {
			ArrayList<String> tempList = new ArrayList<String>();
			for(String labelLine : this.splitAndTruncatedLabelList) {
				if(labelLine.length() > charWidth) {
					//LineGraph.this.logger.debug("Line too long, truncating");
					tempList.add(labelLine.substring(0, charWidth));
				}
				else {
					tempList.add(labelLine);
				}
			}
			this.splitAndTruncatedLabelList.clear();
			this.splitAndTruncatedLabelList.addAll(tempList);
		}
		
		/**
		 * drays each line of the label
		 * @param g2d
		 */
		public void drawLabel(Graphics2D g2d){
			FontMetrics metrics = g2d.getFontMetrics(this.labelFont);
			int strHeight = metrics.getHeight();
			int yLineVal = this.yVal;
			for(String label : this.splitAndTruncatedLabelList) {
				int xLineVal = this.xVal - (metrics.stringWidth(label)/2);
				g2d.drawString(label, xLineVal, yLineVal);
				yLineVal += strHeight;
			}
		}
		
		/**
		 * changes the default delim string
		 * @param delimString
		 */
		public void setDelimString(String delimString) {
			this.delimString = delimString;
		}		
	}

	/**
	 * @param barColors the firstBarColor to set
	 */
	public void setBarColors(Color[] barColors) {
		this.barColors = barColors;
	}

	/**
	 * @param barBorderColor the barBorderColor to set
	 */
	public void setBarBorderColor(Color barBorderColor) {
		this.barBorderColor = barBorderColor;
	}

	/**
	 * @param lineLegendLabel the lineLegendLabel to set
	 */
	public void setLineLegendLabel(String lineLegendLabel) {
		this.lineLegendLabel = lineLegendLabel;
	}

	/**
	 * @param lowLegendLabel the lowLegendLabel to set
	 */
	public void setLowLegendLabel(String lowLegendLabel) {
		this.lowLegendLabel = lowLegendLabel;
	}

	/**
	 * @param highLegendLabel the highLegendLabel to set
	 */
	public void setHighLegendLabel(String highLegendLabel) {
		this.highLegendLabel = highLegendLabel;
	}
	
	public boolean isUseTransparentBackgrounds() {
		return useTransparentBackgrounds;
	}

	public void setUseTransparentBackgrounds(boolean useTransparentBackgrounds) {
		this.useTransparentBackgrounds = useTransparentBackgrounds;
	}
}

//public void drawXLabel(Graphics2D g2d, String label, String delimString, int sectionWidth, int yOffset,
//int xOffset, int chartSection, int labelSpacing){
//
//FontMetrics metrics = g2d.getFontMetrics(this.axisLabelFont);
//int strWidth = metrics.stringWidth(label);
//int strHeight = metrics.getHeight();
//logger.debug("Label: "+label +", Label width in pixels: "+strWidth);
//ArrayList<String> strList = new ArrayList<String>();
//String subString1 = null;
//String subString2 = label;
//int delimLocation = label.indexOf(delimString);
//if(delimLocation<0){
//strList.add(label);
//logger.debug("Label all on one line, not broken into subStrings to fit on different lines");
//}
//else{
//while(delimLocation>0){
//	//TODO trim the strings?
//	logger.debug("Label broken into sub strings to draw on different Lines");
//	logger.debug("Sub String 1: "+subString1+", Sub String 2: "+subString2);
//	subString1 = subString2.substring(0, delimLocation).trim();
//	strList.add(subString1);
//	subString2 = subString2.substring(delimLocation+1, subString2.length()).trim();
//	strList.add(subString2);
//	delimLocation = subString2.indexOf(delimString);
//}
//}
//
//String curString = null;
//for(int j=0;j<strList.size();j++){
//curString = strList.get(j);
//strWidth = metrics.stringWidth(curString);
//while(strWidth>sectionWidth-labelSpacing){
//	logger.debug("Label width in pixels too long, truncating label");
//	logger.debug(" Label String: "+curString+", pixel width = "+strWidth);
//	curString = curString.substring(0, curString.length()-1);
//	strWidth = metrics.stringWidth(curString);
//}
//
//g2d.drawString(curString, chartSection*sectionWidth+this.graphLeft+xOffset-strWidth/2, 
//		this.graphBottom+yOffset+j*strHeight);
//}
//}

//public static float roundToSignifDigits(Float number, int signifDigits){
//	String numStr = number.toString();
//	int numDigits = numStr.length();
//	if(numStr.contains(".")){
//		numDigits--;
//	}
//	//determine number of sig digits that need to be dropped
//	int placesToDrop = numDigits-signifDigits;
//	int indexOfFirstDecimal;
//	if(numStr.indexOf("0.")==0){
//		indexOfFirstDecimal = 1;
//	}
//	else if(numStr.indexOf("0.")==-1){
//		indexOfFirstDecimal = numDigits+1;
//	}
//	else{
//		indexOfFirstDecimal = numStr.indexOf(".");
//	} 
//	
//	if(placesToDrop>0 && numDigits-placesToDrop < indexOfFirstDecimal){
//		float divNum = (int)Math.pow(10, placesToDrop);
//		float newNumber = Math.round(number/divNum);
//		newNumber = newNumber*Math.round(divNum);
//		return newNumber;
//	}
//	else if(placesToDrop>0 && numDigits-placesToDrop > indexOfFirstDecimal){
//		
//		float j = 0;
//		return j;
//	}
//	else if(placesToDrop < 1){
//		return number;
//	}
//}

//class DataPointDateComparator implements Comparator<DataPoint<Date,Integer>>{
//public int compare(DataPoint<Date,Integer> dataPoint1, DataPoint<Date,Integer> dataPoint2){
//	Date date1 = dataPoint1.getXValue();
//	Date date2 = dataPoint2.getXValue();
//	return date1.compareTo(date2);
//}
//}