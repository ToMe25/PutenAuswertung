package com.tome25.auswertung.utils;

import java.util.Objects;

/**
 * A simple key-value Pair object.
 * 
 * @author theodor
 *
 * @param <K> The type of the key object.
 * @param <V> the type of the value object.
 */
public class Pair<K, V> {

	/**
	 * The key object of this pair.
	 */
	private final K key;

	/**
	 * The value object of this pair.
	 */
	private V value;

	/**
	 * Creates a new pair containing the given key and value.
	 * 
	 * @param key   the key for the new pair.
	 * @param value the value for the new pair.
	 */
	public Pair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	/**
	 * Gets the key object of this key-value pair.
	 * 
	 * @return This pairs key.
	 */
	public K getKey() {
		return key;
	}

	/**
	 * Gets the value object of this pair.
	 * 
	 * @return This pairs value.
	 */
	public V getValue() {
		return value;
	}

	/**
	 * Sets this pairs value object.
	 * 
	 * @param value The value of this pair.
	 */
	public void setValue(V value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(key, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;

		if (!(obj instanceof Pair<?, ?>)) {
			return false;
		}

		Pair<?, ?> other = (Pair<?, ?>) obj;
		return Objects.equals(key, other.key) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "Pair[" + key + "=" + value + "]";
	}

}
