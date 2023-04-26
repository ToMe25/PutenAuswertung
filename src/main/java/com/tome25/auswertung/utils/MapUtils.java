package com.tome25.auswertung.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * A class containing {@link Map} utilities.
 * 
 * @author theodor
 */
public class MapUtils {

	/**
	 * Creates a map containing the entries from the given maps sorted by their keys
	 * using the given comparator.<br/>
	 * The sort is <i>stable</i>: this method does not reorder equal elements.
	 * 
	 * @param <K>        The key type for the input and output maps.
	 * @param <V>        The value type for the input and output maps.
	 * @param values     The map to be sorted.
	 * @param comparator The comparator to use to compare the keys.<br/>
	 *                   Sorts by {@link Comparable natural ordering} if
	 *                   {@code null}.
	 * @return A {@link LinkedHashMap} containing the entries of {@code values}. Or
	 *         {@code null} if {@code values} is {@code null}.
	 */
	public static <K, V> LinkedHashMap<K, V> sortByKey(final Map<K, V> values, Comparator<K> comparator) {
		if (values == null) {
			return null;
		}

		if (values.isEmpty()) {
			return new LinkedHashMap<K, V>();
		}

		List<K> keys = new ArrayList<K>(values.keySet());
		Collections.sort(keys, comparator);

		LinkedHashMap<K, V> sortedMap = new LinkedHashMap<K, V>();
		for (K key : keys) {
			sortedMap.put(key, values.get(key));
		}

		return sortedMap;
	}

	/**
	 * Creates a map containing the entries from the given maps sorted by their
	 * values using the given comparator.<br/>
	 * The sort is <i>stable</i>: this method does not reorder equal elements.
	 * 
	 * @param <K>        The key type for the input and output maps.
	 * @param <V>        The value type for the input and output maps.
	 * @param values     The map to be sorted.
	 * @param comparator The comparator to use to compare the values.<br/>
	 *                   Sorts by {@link Comparable natural ordering} if
	 *                   {@code null}.
	 * @return A {@link LinkedHashMap} containing the entries of {@code values}. Or
	 *         {@code null} if {@code values} is {@code null}.
	 */
	public static <K, V> LinkedHashMap<K, V> sortByValue(final Map<K, V> values, Comparator<V> comparator) {
		if (values == null) {
			return null;
		}

		if (values.isEmpty()) {
			return new LinkedHashMap<K, V>();
		}

		List<V> vals = new ArrayList<V>(values.values());
		Collections.sort(vals, comparator);

		final Map<V, Queue<K>> lookup = new HashMap<V, Queue<K>>();
		for (Map.Entry<K, V> entry : values.entrySet()) {
			if (!lookup.containsKey(entry.getValue())) {
				lookup.put(entry.getValue(), new LinkedList<K>());
			}
			lookup.get(entry.getValue()).add(entry.getKey());
		}

		LinkedHashMap<K, V> sortedMap = new LinkedHashMap<K, V>();
		for (V val : vals) {
			sortedMap.put(lookup.get(val).poll(), val);
		}

		return sortedMap;
	}

}
