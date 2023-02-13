package com.tome25.auswertung.testdata;

import static com.tome25.auswertung.testdata.AntennaDataGenerator.RANDOM;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.tome25.auswertung.utils.Pair;

/**
 * A class for generating random zones for test data generation.
 * 
 * @author theodor
 */
public class ZoneGenerator {

	/**
	 * The max possible id for an antenna.
	 */
	private static int MAX_ANTENNA_ID = 0xFFFF;

	/**
	 * The default max number of antennas per zone.
	 */
	private static int DEFAULT_MAX_ANTENNAS = 5;

	/**
	 * Generates a zone with the given name and a random number of antennas.
	 * 
	 * @param id The id of the new zone.
	 * @return A Pair representing the newly created zone.
	 * @throws IllegalArgumentException If {@code id} is empty or
	 *                                  {@code maxAntennas} is less than 1.
	 * @throws NullPointerException     If {@code id} is {@code null}.
	 */
	public static Pair<String, List<String>> generateZone(String id)
			throws IllegalArgumentException, NullPointerException {
		return generateZone(id, DEFAULT_MAX_ANTENNAS);
	}

	/**
	 * Generates a new zone with the given id and a random number of up to
	 * {@code maxAntennas} antennas.
	 * 
	 * @param id          The id/name of the zone to generate.
	 * @param maxAntennas The max number of antennas to give the zone.<br/>
	 *                    A zone has at least one antenna.
	 * @return A {@link Pair} containing the zone id and antennas list.
	 * @throws IllegalArgumentException If {@code id} is empty or
	 *                                  {@code maxAntennas} is less than 1.
	 * @throws NullPointerException     If {@code id} is {@code null}.
	 */
	public static Pair<String, List<String>> generateZone(String id, int maxAntennas)
			throws IllegalArgumentException, NullPointerException {
		Objects.requireNonNull(id, "The id for the newly generated zone can't be null.");
		id = id.trim();

		if (id.isEmpty()) {
			throw new IllegalArgumentException("The zone id cannot be empty.");
		}

		if (maxAntennas < 1) {
			throw new IllegalArgumentException("maxAntennas cannot be <= 0.");
		}

		int nAnt = RANDOM.nextInt(maxAntennas) + 1;
		List<String> antennas = new ArrayList<String>();
		for (int i = 0; i < nAnt; i++) {
			String antenna = Integer.toHexString(RANDOM.nextInt(MAX_ANTENNA_ID));
			if (!antennas.contains(antenna)) {
				antennas.add(antenna);
			}
		}

		return new Pair<String, List<String>>(id, antennas);
	}

	/**
	 * Generates a list of {@code number} zones with the name "{@code Zone i}" where
	 * i is in incrementing number.
	 * 
	 * @param number The number of zones to generate.
	 * @return A map containing all the generated zones.
	 * @throws IllegalArgumentException If {@code number} is less than 1.
	 */
	public static Map<String, List<String>> generateZones(int number) throws IllegalArgumentException {
		return generateZones(number, DEFAULT_MAX_ANTENNAS);
	}

	/**
	 * Generates a list of {@code number} zones with the name "{@code Zone i}" where
	 * i is in incrementing number.
	 * 
	 * @param number      The number of zones to generate.
	 * @param maxAntennas The max number of antennas per zone.
	 * @return A map containing all the generated zones.
	 * @throws IllegalArgumentException If {@code number} or {@code maxAntennas} is
	 *                                  less than 1.
	 */
	public static Map<String, List<String>> generateZones(int number, int maxAntennas) throws IllegalArgumentException {
		if (number < 1) {
			throw new IllegalArgumentException("Cannot generate less than one zone.");
		} else if (maxAntennas < 1) {
			throw new IllegalArgumentException("Cannot generate zones with less than one antenna.");
		}

		Map<String, List<String>> zones = new LinkedHashMap<String, List<String>>();
		for (int i = 1; i <= number; i++) {
			Pair<String, List<String>> zone = generateZone("Zone " + i, maxAntennas);
			zones.put(zone.getKey(), zone.getValue());
		}

		return zones;
	}

}
