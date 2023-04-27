package com.tome25.auswertung.tests.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.tome25.auswertung.utils.IntOrStringComparator;

/**
 * The class containing unit tests relating to {@link IntOrStringComparator}.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class IntOrStringComparatorTest {

	/**
	 * Tests comparing two strings using the {@link IntOrStringComparator}.
	 */
	@Test
	public void compareStrings() {
		String s1 = "Test";
		String s2 = "ABC";
		assertEquals("String comparison didn't match.", s1.compareTo(s2),
				IntOrStringComparator.INSTANCE.compare(s1, s2));
		assertEquals("Comparing strings didn't match expectations.", s2.compareTo(s1),
				IntOrStringComparator.INSTANCE.compare(s2, s1));
	}

	/**
	 * Tests comparing two positive integers using the
	 * {@link IntOrStringComparator}.
	 */
	@Test
	public void compareInts() {
		String s1 = "123";
		String s2 = "32";
		assertEquals("Integer comparison didn't match.", 1, IntOrStringComparator.INSTANCE.compare(s1, s2));
		assertEquals("Comparing integers didn't match expectations.", -1,
				IntOrStringComparator.INSTANCE.compare(s2, s1));
	}

	/**
	 * Tests comparing a positive and a negative integer.
	 */
	@Test
	public void compareSignedInts() {
		String s1 = "-312";
		String s2 = "5123";
		assertEquals("Integer comparison didn't match.", -1, IntOrStringComparator.INSTANCE.compare(s1, s2));
		assertEquals("Comparing integers didn't match expectations.", 1,
				IntOrStringComparator.INSTANCE.compare(s2, s1));
	}

	/**
	 * Compares two integers that have spaces before/after them.
	 */
	@Test
	public void compareUntrimmedInts() {
		String s1 = " 1452    ";
		String s2 = "    561";
		assertEquals("Integer comparison didn't match.", 1, IntOrStringComparator.INSTANCE.compare(s1, s2));
		assertEquals("Comparing integers didn't match expectations.", -1,
				IntOrStringComparator.INSTANCE.compare(s2, s1));
	}

	/**
	 * Tests comparing a string and an integer.
	 */
	@Test
	public void compareIntAndString() {
		String s1 = "123";
		String s2 = "#b";
		assertEquals("String to int comparison didn't match.", -1, IntOrStringComparator.INSTANCE.compare(s1, s2));
		assertEquals("String to int comparison failed.", 1, IntOrStringComparator.INSTANCE.compare(s2, s1));
	}

	/**
	 * Compares a {@code null} value using {@link IntOrStringComparator}.
	 */
	@Test(expected = NullPointerException.class)
	public void compareNull() {
		IntOrStringComparator.INSTANCE.compare("test", null);
	}

}
