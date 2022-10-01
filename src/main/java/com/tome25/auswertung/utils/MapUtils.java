package com.tome25.auswertung.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A class containing {@link Map} utilities.
 * 
 * @author theodor
 */
public class MapUtils {

	/**
	 * Creates a map containing the entries from the given maps sorted by their keys
	 * using the given comparator..<br/>
	 * The sort is <i>stable</i>: this method does not reorder equal elements.
	 * 
	 * @param <K>        The key type for the input and output maps.
	 * @param <V>        The value type for the input and output maps.
	 * @param values     The map to be sorted.
	 * @param comparator The comparator to use to compare the keys.<br/>
	 *                   Sorts by {@link Comparable natural ordering} if {@code null}.
	 * @return A {@link LinkedHashMap} containing the entries of {@code values}. Or
	 *         {@code null} if {@code values} is {@code null}.
	 */
	public static <K, V> LinkedHashMap<K, V> sortByKey(Map<K, V> values, Comparator<K> comparator) {
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

}
