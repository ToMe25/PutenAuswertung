package com.tome25.auswertung.utils;

import java.util.Collection;
import java.util.Objects;

/**
 * A utility class containing a few general String utilities to be used in this
 * project.
 * 
 * @author theodor
 */
public class StringUtils {

	/**
	 * Returns a string representation of the given collection.<br/>
	 * Does not add square brackets to the beginning and end of the output
	 * string.<br/>
	 * Uses ", " as a separator between objects in the collection.
	 * 
	 * @param c The collection to convert to a string.
	 * @return The string representation of the given collection.
	 */
	public static String collectionToString(Collection<?> c) {
		return collectionToString(", ", c);
	}

	/**
	 * Returns a string representation of the given collection.<br/>
	 * Does not add square brackets to the beginning and end of the output string.
	 * 
	 * @param separator The separator to be places between entries in the
	 *                  collection.
	 * @param c         The collection to convert to a string.
	 * @return The string representation of the given collection.
	 */
	public static String collectionToString(char separator, Collection<?> c) {
		return collectionToString(Character.toString(separator), c);
	}

	/**
	 * Returns a string representation of the given collection.<br/>
	 * Does not add square brackets to the beginning and end of the output string.
	 * 
	 * @param separator The separator to be places between entries in the
	 *                  collection.
	 * @param c         The collection to convert to a string.
	 * @return The string representation of the given collection.
	 */
	public static String collectionToString(CharSequence separator, Collection<?> c) {
		return collectionToString(separator, false, c);
	}

	/**
	 * Returns a string representation of the given collection.
	 * 
	 * @param separator The separator to be places between entries in the
	 *                  collection.
	 * @param brackets  Whether square brackets should be added before and after the
	 *                  collection.
	 * @param c         The collection to convert to a string.
	 * @return The string representation of the given collection.
	 */
	public static String collectionToString(char separator, boolean brackets, Collection<?> c) {
		return collectionToString(Character.toString(separator), brackets, c);
	}

	/**
	 * Returns a string representation of the given collection.
	 * 
	 * @param separator The separator to be places between entries in the
	 *                  collection.
	 * @param brackets  Whether square brackets should be added before and after the
	 *                  collection.
	 * @param c         The collection to convert to a string.
	 * @return The string representation of the given collection.
	 */
	public static String collectionToString(CharSequence separator, boolean brackets, Collection<?> c) {
		Objects.requireNonNull(c, "Collection to convert cannot be null.");

		if (c.size() == 0) {
			return brackets ? "[]" : "";
		}

		StringBuilder result = new StringBuilder();
		if (brackets) {
			result.append('[');
		}

		for (Object obj : c) {
			result.append(obj);
			result.append(separator);
		}

		result.delete(result.length() - separator.length(), result.length());

		if (brackets) {
			result.append(']');
		}

		return result.toString();
	}

	/**
	 * Creates a string from all the given tokens separated by separator.
	 * 
	 * @param separator The separator to put between tokens.
	 * @param tokens    The tokens to convert to a single String.
	 * @return The string created by joining the tokens together.
	 */
	public static String join(char separator, Object... tokens) {
		return join(Character.toString(separator), tokens);
	}

	/**
	 * Creates a string from all the given tokens separated by separator.
	 * 
	 * @param separator The separator to put between tokens.
	 * @param tokens    The tokens to convert to a single String.
	 * @return The string created by joining the tokens together.
	 */
	public static String join(CharSequence separator, Object... tokens) {
		Objects.requireNonNull(separator, "The separatur between tokens can not be null.");

		StringBuilder result = new StringBuilder();
		for (Object token : tokens) {
			result.append(token);
			result.append(separator);
		}

		result.delete(result.length() - separator.length(), result.length());

		return result.toString();
	}

	/**
	 * Checks if the given string can be parsed as an integer.<br/>
	 * Does not account for integer overflows.
	 * 
	 * @param str The string to check.
	 * @return Whether or not the given string is a valid integer.
	 */
	public static boolean isInteger(String str) {
		if (str == null || str.isEmpty()) {
			return false;
		}

		int length = str.length();

		int i = 0;
		if (str.charAt(0) == '-' || str.charAt(0) == '+') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}

		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}
		return true;
	}

}
