package core.ui.graph;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Used to generate Pie Graphs for IVE_N graphs on the stats page
 * 
 * 2009.01.23 - SD - Created
 */

public class PieChart{
	
	/** keeps track of the color of the slice of the pie chart being drawn at any given time */ 
	int curPieColor = 0;
	
	/** array of 6 colors used to fill in the slices of the pie chart */
	Color pieColorArray[] = {new Color(210,60,60), 
			new Color(60,210,60), 
			new Color(60,60,210),
			new Color(120,60,120),
			new Color(60,120,120),
			new Color(210,210,60)};
	
	/** returns the current color being used on the pie chart */
	public Color getPieColor(){
		return pieColorArray[this.curPieColor];
	}
	
	/** increments pie color counter */
	public void setNewColor(){
		this.curPieColor++;
		if(this.curPieColor >= pieColorArray.length){
			curPieColor = 0;
		}
	}
	
	/** default settings for generating the graphic */
	Color dropShadow = new Color(240,240,240);
	int innerOffset = 20;
	int WIDTH = 400;
	int HEIGHT = 200;
	int pieHeight = HEIGHT - (innerOffset*2);
	int pieWidth = pieHeight; //makes pie chart circular
	int halfWidth = WIDTH/2;
	Dimension graphDim = new Dimension(WIDTH, HEIGHT);
	Rectangle graphRect = new Rectangle(graphDim);
	Dimension borderDim = new Dimension(halfWidth-2, HEIGHT-2);
	Rectangle borderRect = new Rectangle(borderDim);
	
	public PieChart(){
		
	}
	
	/** overwrites default settings for the pie chart using the 3 parameters below*/
	public PieChart(int width, int height, int innerOffset){
		this.innerOffset = innerOffset;
		this.WIDTH = width;
		this.HEIGHT = height;
		this.pieHeight = this.HEIGHT - this.innerOffset;
		this.pieWidth = this.pieHeight;
		this.halfWidth = this.WIDTH/2;
		this.graphDim = new Dimension(this.WIDTH, this.HEIGHT);
		this.graphRect = new Rectangle(this.graphDim);
		this.borderDim = new Dimension(this.halfWidth-2, this.HEIGHT-2);
		this.borderRect = new Rectangle(borderDim);
	}
	
	/** generates a pie chart buffered image using an array of scores and a clices array whos length
	 * determines the number of slices, and whos element values determines the ranges of scores per 
	 * slice */
	public BufferedImage createGraphFromScores(int[]scores, int[] chartSlices){
			
		BufferedImage bi = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = bi.createGraphics();
		//set Antialiasing 
		RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHints(renderHints);
		//set background color to white
		g2d.setColor(Color.white);
		g2d.fill(graphRect);	
		//draw black border
		g2d.setColor(Color.black);
		borderRect.setLocation(1, 1);
		g2d.draw(borderRect);
		//draw border for legend
		borderRect.setLocation((WIDTH/2) + 1, 1);
		g2d.draw(borderRect);
		//draw data on to graph
		int x_pie = innerOffset;
		int y_pie = innerOffset;
		int border = 20;
		
		Ellipse2D.Double elb = new Ellipse2D.Double(x_pie - border/2, y_pie - border/2, 
				pieWidth+border, pieHeight+border);
		//shadow
		g2d.setColor(dropShadow);
		g2d.fill(elb);
		//border
		g2d.setColor(Color.black);
		g2d.draw(elb);
	//draw pie chart
		int startAngle = 0;
		//legend variables
		int legendWidth = 20;
		int x_legendText = halfWidth+innerOffset/2+legendWidth+5;
		int x_legendBar = halfWidth+innerOffset/2;
		int textHeight = 20;
		int curElement = 0;
		int y_legend = 0;
		//Dimensions of legend bar
		Dimension legendDim = new Dimension(legendWidth, textHeight/2);
		Rectangle legendRect = new Rectangle(legendDim);
		//need DB info
		//TODO
		//Calculate # of scores
		int lastElement = chartSlices.length;
		
		float numberOfScores = 0;
		for(int i = 0; i<scores.length; i++){
			numberOfScores += 1;
		}
		
		if(scores==null||numberOfScores==0){
			//logger.debug("IVE score list null, or number of IVE scores = zero, - "+numberOfScores);
			int sweepAngle = 360;
			//fill in circle - no scores to graph
			g2d.setColor(Color.GRAY);
			g2d.fillArc(x_pie, y_pie, pieWidth, pieHeight, startAngle, sweepAngle);
			//draw legend
			//set y position for the bar
			y_legend = curElement*textHeight+innerOffset;
			//display the current range of scores
			String display = "No dispositions for";
			g2d.setColor(Color.black);
			g2d.drawString(display, x_legendText, y_legend);
			curElement++;
			y_legend = curElement*textHeight+innerOffset;
			display = "this date range.";
			g2d.setColor(Color.black);
			g2d.drawString(display, x_legendText, y_legend);
			
			return bi;
		}		
		else{
			//logger.debug("number of IVE scores - "+numberOfScores);
			
			float[] floatScores = convertIntToFloatArray(scores);
//			for(int i=0; i<floatScores.length;i++){
//				logger.debug("Float Scores "+i+" - "+floatScores[i]);
//			}
			float[] scoresPerSlice = calcScorePerSlice(chartSlices, floatScores);
			
			for(int i=0; i<chartSlices.length; i++){
				//logger.debug("Scores per slice "+i+" - "+scoresPerSlice[i]);
				//Calculate percentage of scores
				float perc = (scoresPerSlice[i]/numberOfScores);
				//Calculate new angle
				int sweepAngle = (int)(perc*360);
				//Check that the last element goes back to 0 position
				if(i == lastElement){
					sweepAngle = 360-startAngle;
				}
				//draw arc
				g2d.setColor(getPieColor());
				g2d.fillArc(x_pie, y_pie, pieWidth, pieHeight, startAngle, sweepAngle);
				//increment startangle and sweepangle
				startAngle += sweepAngle;
				//draw legend
				//set y position for the bar
				y_legend = curElement*textHeight+innerOffset;
				//display the current range of scores
				String display = null;
				if(i==0){
					display = "100 - " + chartSlices[0];
				}
				else{
					display = chartSlices[i-1]-1+" - "+chartSlices[i];
				}
				g2d.setColor(Color.black);
				g2d.drawString(display, x_legendText, y_legend);
				//display scores in current range
				display = "" + (int)scoresPerSlice[i];
				g2d.setColor(Color.black);
				g2d.drawString(display, x_legendText + 80, y_legend);
				//display % of scores in range
				display = " ("+ (int)(perc*100) + "%)";
				g2d.setColor(Color.black);
				g2d.drawString(display, x_legendText + 110, y_legend);
				//draw the bar
				g2d.setColor(getPieColor());
				legendRect.setLocation(x_legendBar, y_legend - textHeight/2);
				g2d.fill(legendRect);
				//set new pie color
				setNewColor();
				curElement++;		
			}
			
			return bi;
		}
		
	}
	
	/** calculates the number of scores per pie slice using the score range array and the array of all 
	 * scores */
	private float[] calcScorePerSlice(int chartSlices[], float[] scores){
		float[] scoresPerSlice = new float[chartSlices.length];
		for(int i=0;i<scoresPerSlice.length;i++){
			scoresPerSlice[i] = 0;
		}
		
		int maxRange = 0;
		int minRange = 0;
		for(int i = 0; i<chartSlices.length;i++){
			
			if(i==0){
				maxRange = 100;
				minRange = chartSlices[0];
			}
			else{
				maxRange = chartSlices[i-1]-1;
				minRange = chartSlices[i];
			}
			for(int j = 0; j<scores.length;j++){
				if(scores[j] >= minRange && scores[j] <= maxRange){
					scoresPerSlice[i]= scoresPerSlice[i]+1;
				}
			}
		}
		
		return scoresPerSlice;
	}
	
	/** simply converts an array of ints to an array of floats */
	private static float[] convertIntToFloatArray(int[] iArray){
		if(iArray!=null){
			float[] fArray = new float[iArray.length];
			for(int i = 0; i<iArray.length;i++){
				fArray[i] = iArray[i];
			}
			return fArray;
		}
		else{
			return null;
		}
	}
	
	public void main (String[] args){
		
	}
	
}
