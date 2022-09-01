package com.tome25.auswertung;

import java.util.Comparator;

import com.tome25.auswertung.utils.StringUtils;

/**
 * A class that compares strings first as integers, if possible, and if not as
 * strings.
 * 
 * @author theodor
 */
public class IntOrStringComparator implements Comparator<String> {

	/**
	 * The default instance of this type of {@link Comparator}.<br/>
	 * There should be no need to ever create another instance.
	 */
	public static final IntOrStringComparator INSTANCE = new IntOrStringComparator();

	@Override
	public int compare(String o1, String o2) {
		if (StringUtils.isInteger(o1) && !StringUtils.isInteger(o2)) {
			return -1;
		} else if (!StringUtils.isInteger(o1) && StringUtils.isInteger(o2)) {
			return 1;
		} else if (StringUtils.isInteger(o1) && StringUtils.isInteger(o2)) {
			try {
				Integer i1 = Integer.parseInt(o1);
				Integer i2 = Integer.parseInt(o2);

				return i1.compareTo(i2);
			} catch (NumberFormatException e) {
				LogHandler.print_exception(e, "convert string to int for comparison",
						"String 1: \"%s\", String 2: \"%s\"", o1, o2);
				return o1.compareTo(o2);
			}
		}

		return o1.compareTo(o2);
	}

}
