package com.tome25.auswertung.tests.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.tome25.auswertung.utils.StringUtils;

/**
 * A class containing some unit tests for {@link StringUtils}.<br/>
 * Not intended to completely test all functionality.<br/>
 * So far only tests {@link StringUtils#isInteger}.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class StringUtilsTest {

	/**
	 * Checks {@link StringUtils#isInteger} detecting a string containing a single
	 * digit as an integer.
	 */
	@Test
	public void singleDigitIsInteger() {
		assertTrue("A single digit string was not detected as a number.", StringUtils.isInteger("5"));
	}

	/**
	 * Tests whether a large integer is correctly detected as an integer.
	 */
	@Test
	public void largeIntIsInteger() {
		assertTrue("A large integer in string for was not detected as a number.", StringUtils.isInteger("1502375651"));
	}

	/**
	 * Confirm that numbers above {@link Integer#MAX_VALUE} are not considered valid
	 * integers.
	 */
	@Test
	public void longIsNotInteger() {
		assertFalse("A number with more than 10 digits was detected as a valid integer.",
				StringUtils.isInteger("1234987123123"));
		assertFalse("A number larger than the 32 bit integer max was detected as a valid integer.",
				StringUtils.isInteger("5129502153"));
	}

	/**
	 * Confirms that negative numbers can be detected correctly.
	 */
	@Test
	public void negativeIntIsInteger() {
		assertTrue("A negative number was not detected as an integer.", StringUtils.isInteger("-12453456"));
	}

	/**
	 * Makes sure that numbers with an explicit positive sign are also detected
	 * correctly.
	 */
	@Test
	public void signedPositiveIsInteger() {
		assertTrue("A number starting with a + was not considered a valid integer.", StringUtils.isInteger("+340234"));
	}

	/**
	 * Make sure that negative numbers that exceed the 32 bit integer limit are not
	 * considered valid.
	 */
	@Test
	public void negativeLongIsNotInteger() {
		assertFalse("A negative number with more than 10 digits was detected as a valid integer.",
				StringUtils.isInteger("-31230891233"));
		assertFalse("A number smaller than the negative 32 bit integer max was detected as a valid integer.",
				StringUtils.isInteger("-5129502153"));
	}

	/**
	 * Check that a string that isn't a number is not considered a valid integer.
	 */
	@Test
	public void textIsNotInteger() {
		assertFalse("A text string was considered a valid integer.", StringUtils.isInteger("test"));
	}

	/**
	 * Confirm that hexadecimal numbers are not considered valid integers.
	 */
	@Test
	public void hexIsNotInteger() {
		assertFalse("A hex string without start mark was considered a number.", StringUtils.isInteger("051A"));
		assertFalse("A hex number starting with # was considered a valid integer.", StringUtils.isInteger("#56a9"));
		assertFalse("A hex number starting with 0x was consdiered valid.", StringUtils.isInteger("0x0471f"));
	}

	/**
	 * Test that numbers with spaces before or after them are still considered
	 * valid.
	 */
	@Test
	public void endSpacesIsInteger() {
		assertTrue("A valid integer with spaces before it was considered invalid.", StringUtils.isInteger("   54123"));
		assertTrue("A valid integer with spaces after it was considered invalid.", StringUtils.isInteger("89104   "));
	}

}
