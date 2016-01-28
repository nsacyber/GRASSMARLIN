package core.ui.graph;

/**
 * Simple datapoint class used for graphing
 *
 * @param <X>
 * @param <Y>
 */
public class DataPoint<X,Y> {
	/** the xvalue */
	private X xValue;
	/** the xvalue */
	private Y yValue;
	
	/**
	 * Cunstructs a new DataPoint object
	 * @param xValue
	 * @param yValue
	 */
	public DataPoint(X xValue, Y yValue) {
		this.xValue = xValue;
		this.yValue = yValue;
	}

	/**
	 * Returns the xvalue
	 * @return
	 */
	public X getXValue() {
		return xValue;
	}

	/**
	 * Sets the xvalue
	 * @param xValue
	 */
	public void setXValue(X xValue) {
		this.xValue = xValue;
	}

	/**
	 * Returns the yvalue
	 * @return
	 */
	public Y getYValue() {
		return yValue;
	}

	/**
	 * Sets the yvalue
	 * @param xValue
	 */
	public void setYValue(Y yValue) {
		this.yValue = yValue;
	}
	
}
