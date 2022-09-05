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
	 * @throws NullPointerException if {@code separator} or {@code c} is
	 *                              {@code null}.
	 */
	public static String collectionToString(CharSequence separator, boolean brackets, Collection<?> c)
			throws NullPointerException {
		Objects.requireNonNull(c, "Collection to convert cannot be null.");
		Objects.requireNonNull(separator, "The object separator cannot be null.");

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
	 * @param separator The separator to put between tokens. If {@code null} the
	 *                  tokens are joined without a separator.
	 * @param tokens    The tokens to convert to a single String.
	 * @return The string created by joining the tokens together.
	 */
	public static String join(CharSequence separator, Object... tokens) {
		if (tokens == null || tokens.length == 0) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		for (Object token : tokens) {
			result.append(token);
			if (separator != null) {
				result.append(separator);
			}
		}

		if (separator != null) {
			result.delete(result.length() - separator.length(), result.length());
		}

		return result.toString();
	}

	/**
	 * Checks if the given string can be parsed as an integer.
	 * 
	 * @param str The string to check.
	 * @return Whether or not the given string is a valid integer.
	 */
	public static boolean isInteger(String str) {
		if (str == null || str.trim().isEmpty()) {
			return false;
		}

		str = str.trim();
		int length = str.length();

		int i = 0;
		if (str.charAt(0) == '-' || str.charAt(0) == '+') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}

		if (length - i > 10) {
			return false;
		}

		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c < '0' || c > '9') {
				return false;
			}
		}

		long value = Long.parseLong(str);
		if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
			return false;
		}

		return true;
	}

}
