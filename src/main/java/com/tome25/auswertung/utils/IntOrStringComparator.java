package com.tome25.auswertung.utils;

import java.util.Comparator;
import java.util.Objects;

import com.tome25.auswertung.log.LogHandler;

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

	/**
	 * Creates a new IntOrStringComparator.<br/>
	 * Private since there should be no need to create another one.
	 */
	private IntOrStringComparator() {
	}

	/**
	 * Compares the two given {@link String strings}.<br/>
	 * The result is {@code 0} if the two values are equal.<br/>
	 * If {@code o1} is an integer, and either smaller than {@code o2}, or
	 * {@code o2} isn't an integer, the result is {@code -1}.<br/>
	 * In any other case where at least one value is a valid integer the result is
	 * {@code 1}.
	 * 
	 * If neither string is an integer this returns the same as
	 * {@link String#compareTo o1.compareTo(o2)}.
	 * 
	 * @param o1 The first string to compare.
	 * @param o2 The second string to compare.
	 * @return The comparison result.
	 * @throws NullPointerException If one of the arguments is {@code null}.
	 */
	@Override
	public int compare(String o1, String o2) throws NullPointerException {
		Objects.requireNonNull(o1, "The strings to compare cannot be null.");
		Objects.requireNonNull(o2, "The strings to compare cannot be null.");

		if (StringUtils.isInteger(o1) && !StringUtils.isInteger(o2)) {
			return -1;
		} else if (!StringUtils.isInteger(o1) && StringUtils.isInteger(o2)) {
			return 1;
		} else if (StringUtils.isInteger(o1) && StringUtils.isInteger(o2)) {
			try {
				Integer i1 = Integer.parseInt(o1.trim());
				Integer i2 = Integer.parseInt(o2.trim());

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
