package com.tome25.auswertung;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.tome25.auswertung.utils.IntOrStringComparator;

/**
 * A class containing the information representing a single one of the zones in
 * which a turkey can stay.<br/>
 * <br/>
 * The natural ordering for {@link ZoneInfo} objects is to sort them by their
 * {@link #getId() id} using an {@link IntOrStringComparator}.<br/>
 * <br/>
 * Note: this class has a natural ordering that is inconsistent with equals.
 * 
 * @author theodor
 */
public class ZoneInfo implements Comparable<ZoneInfo> {

	/**
	 * The unique identifier of the zone represented by this object.
	 */
	private final String id;

	/**
	 * The ids of the antennas associated with this zone.
	 */
	private final String antennas[];

	/**
	 * Whether the zone represented by this object has food.
	 */
	private final boolean food;

	/**
	 * Creates a new {@link ZoneInfo} object with the given properties.
	 * 
	 * @param id       The id of the zone represented by this object.
	 * @param food     Whether this zone contains food.
	 * @param antennas The antennas belonging to this zone.
	 * @throws NullPointerException     If {@code id} or {@code antennas} is
	 *                                  {@code null}.
	 * @throws IllegalArgumentException If {@code id} or {@code antennas} is empty.
	 */
	public ZoneInfo(final String id, final boolean food, final String... antennas)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(id, "The id of the zone cannot be null.");
		Objects.requireNonNull(antennas, "The antennas to of this zone cannot be null.");

		if (id.trim().isEmpty()) {
			throw new IllegalArgumentException("The id of the zone cannot be empty.");
		}

		if (antennas.length == 0) {
			throw new IllegalArgumentException("The number of antennas cannot be zero.");
		}

		this.id = id;
		this.antennas = antennas;
		this.food = food;
	}

	/**
	 * Creates a new {@link ZoneInfo} object with the given properties.
	 * 
	 * @param id       The id of the zone represented by this object.
	 * @param food     Whether this zone contains food.
	 * @param antennas The antennas belonging to this zone.
	 * @throws NullPointerException     If {@code id} or {@code antennas} is
	 *                                  {@code null}.
	 * @throws IllegalArgumentException If {@code id} or {@code antennas} is empty.
	 */
	public ZoneInfo(final String id, final boolean food, final Collection<String> antennas)
			throws NullPointerException, IllegalArgumentException {
		this(id, food, antennas.toArray(new String[antennas.size()]));
	}

	/**
	 * Gets the unique identifier of the zone represented by this object.
	 * 
	 * @return The unique identifier of the zone represented by this object.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns a {@link List} containing the antennas belonging to this zone.
	 * 
	 * @return A {@link List} containing the antennas belonging to this zone.
	 */
	public List<String> getAntennas() {
		return Arrays.asList(antennas);
	}

	/**
	 * Returns the number of antennas associated with this zone.
	 * 
	 * @return The number of antennas associated with this zone.
	 */
	public int getAntennaCount() {
		return antennas.length;
	}

	/**
	 * Checks whether this zone contains food.
	 * 
	 * @return {@code true} if this zone has food, {@code false} otherwise.
	 */
	public boolean hasFood() {
		return food;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ZoneInfo[id=");
		builder.append(id);
		builder.append(", antennas=");
		builder.append(Arrays.toString(antennas));
		builder.append(", food=");
		builder.append(food);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(antennas);
		result = prime * result + Objects.hash(food, id);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ZoneInfo other = (ZoneInfo) obj;
		return Arrays.equals(antennas, other.antennas) && food == other.food && Objects.equals(id, other.id);
	}

	@Override
	public int compareTo(ZoneInfo o) {
		Objects.requireNonNull(o, "The object to compare this object to can't be null.");
		return IntOrStringComparator.INSTANCE.compare(id, o.getId());
	}

}
