package prefuse.util.collections;

import java.util.Comparator;

/**
 * A do-nothing comparator that simply treats all objects as equal.
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class NullComparator implements Comparator {

	public int compare(Object o1, Object o2) {
		return 0;
	}

} // end of class NullComparator
