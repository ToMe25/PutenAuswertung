package com.tome25.auswertung.tests.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import org.junit.Test;

import com.tome25.auswertung.IntOrStringComparator;
import com.tome25.auswertung.utils.MapUtils;

/**
 * A unit test class for the {@link MapUtils} included in this program.
 * 
 * @author theodor
 */
public class MapUtilsTest {

	/**
	 * A unit test for sorting a unordered {@link HashMap} using natural ordering.
	 */
	@Test
	public void sortIntKeys() {
		Map<Integer, String> unsorted = new HashMap<Integer, String>();
		unsorted.put(15, "Something");
		unsorted.put(121, "Test");
		unsorted.put(2, "131");
		Map<Integer, String> sorted = MapUtils.sortByKey(unsorted, null);

		Map<Integer, String> expected = new LinkedHashMap<Integer, String>();
		expected.put(2, "131");
		expected.put(15, "Something");
		expected.put(121, "Test");

		assertOrderEquals("The sorted int key map didn't match expectations.", expected, sorted);
	}

	/**
	 * Tests sorting a {@link HashMap} with string keys by natural ordering.
	 */
	@Test
	public void sortStringKeys() {
		Map<String, String> unsorted = new HashMap<String, String>();
		unsorted.put("Test", "Test");
		unsorted.put("Another", "Test");
		unsorted.put("123", "Whatever");
		unsorted.put("32", "Number");
		unsorted.put("Test1", "Test2");
		Map<String, String> sorted = MapUtils.sortByKey(unsorted, null);

		Map<String, String> expected = new LinkedHashMap<String, String>();
		expected.put("123", "Whatever");
		expected.put("32", "Number");
		expected.put("Another", "Test");
		expected.put("Test", "Test");
		expected.put("Test1", "Test2");

		assertOrderEquals("The sorted string key map didn't match expectations.", expected, sorted);
	}

	/**
	 * Tests sorting a {@link HashMap} using a specified comparator, rather than
	 * natural ordering.
	 */
	@Test
	public void sortCustomComparator() {
		Map<String, Double> unsorted = new HashMap<String, Double>();
		unsorted.put("String", 15.3);
		unsorted.put("Test", 901.5);
		unsorted.put("123", 512.0);
		unsorted.put("51", 1861.2);
		Map<String, Double> sorted = MapUtils.sortByKey(unsorted, IntOrStringComparator.INSTANCE);

		Map<String, Double> expected = new LinkedHashMap<String, Double>();
		expected.put("51", 1861.2);
		expected.put("123", 512.0);
		expected.put("String", 15.3);
		expected.put("Test", 901.5);

		assertOrderEquals("The map sorted using a custom comparator didn't match.", expected, sorted);
	}

	/**
	 * Test sorting an already sorted {@link TreeMap}.
	 */
	@Test
	public void sortTreeMap() {
		TreeMap<String, String> unsorted = new TreeMap<String, String>();
		unsorted.put("Test", "String");
		unsorted.put("Another", "Test");
		unsorted.put("123", "321");
		Map<String, String> sorted = MapUtils.sortByKey(unsorted, null);

		Map<String, String> expected = new LinkedHashMap<String, String>();
		expected.put("123", "321");
		expected.put("Another", "Test");
		expected.put("Test", "String");

		assertOrderEquals("Sorting a tree map didn't work.", expected, sorted);
	}

	/**
	 * Test sorting a {@code null} map.
	 */
	@Test
	public void sortNull() {
		assertNull("Sorting a null map didn't return null.", MapUtils.sortByKey(null, null));
	}

	/**
	 * Asserts that the two given maps have the same content and the same iteration
	 * order.
	 * 
	 * @param <K>      The key type of the two maps.
	 * @param <V>      The value type of the two maps.
	 * @param message  The message to use in case of failure.
	 * @param expected A map containing the entries in the expected order.
	 * @param actual   The map containing the potentially incorrectly ordered
	 *                 entries.
	 */
	public static <K, V> void assertOrderEquals(String message, Map<K, V> expected, Map<K, V> actual) {
		assertEquals(message, expected, actual);

		Iterator<Entry<K, V>> exp = expected.entrySet().iterator();
		Iterator<Entry<K, V>> act = actual.entrySet().iterator();
		while (exp.hasNext() && act.hasNext()) {
			Entry<K, V> expect = exp.next();
			if (!Objects.equals(expect, act.next())) {
				fail(message + " expected order:<" + expected.toString() + "> but was:<" + actual.toString() + ">");
			}
		}
	}

}
