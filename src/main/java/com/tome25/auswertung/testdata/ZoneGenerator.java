package com.tome25.auswertung.testdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.tome25.auswertung.ZoneInfo;
import com.tome25.auswertung.utils.Pair;

/**
 * A class for generating random zones for test data generation.
 * 
 * @author theodor
 */
public class ZoneGenerator {

	/**
	 * The maximum possible id for an antenna.
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
	 * @throws IllegalArgumentException If {@code id} is empty, {@code maxAntennas}
	 *                                  is less than 1, or {@code maxAntennas} is
	 *                                  greater than {@link #MAX_ANTENNA_ID}.
	 * @throws NullPointerException     If {@code id} is {@code null}.
	 */
	public static ZoneInfo generateZone(String id) throws IllegalArgumentException, NullPointerException {
		return generateZone(id, DEFAULT_MAX_ANTENNAS);
	}

	/**
	 * Generates a new zone with the given id and a random number of up to
	 * {@code maxAntennas} antennas.
	 * 
	 * @param id                 The id/name of the zone to generate.
	 * @param antennaIdBlacklist A collection of possible ids that may not be used.
	 * @return A {@link Pair} containing the zone id and antennas list.
	 * @throws IllegalArgumentException If {@code id} is empty, {@code maxAntennas}
	 *                                  is less than 1, or {@code maxAntennas} is
	 *                                  greater than {@link #MAX_ANTENNA_ID}.
	 * @throws NullPointerException     If {@code id} is {@code null}.
	 */
	public static ZoneInfo generateZone(String id, Collection<String> antennaIdBlacklist)
			throws IllegalArgumentException, NullPointerException {
		return generateZone(id, DEFAULT_MAX_ANTENNAS, antennaIdBlacklist);
	}

	/**
	 * Generates a new zone with the given id and a random number of up to
	 * {@code maxAntennas} antennas.
	 * 
	 * @param id          The id/name of the zone to generate.
	 * @param maxAntennas The max number of antennas to give the zone.<br/>
	 *                    A zone has at least one antenna.
	 * @return A {@link Pair} containing the zone id and antennas list.
	 * @throws IllegalArgumentException If {@code id} is empty, {@code maxAntennas}
	 *                                  is less than 1, or {@code maxAntennas} is
	 *                                  greater than {@link #MAX_ANTENNA_ID}.
	 * @throws NullPointerException     If {@code id} is {@code null}.
	 */
	public static ZoneInfo generateZone(String id, int maxAntennas)
			throws IllegalArgumentException, NullPointerException {
		return generateZone(id, maxAntennas, null);
	}

	/**
	 * Generates a new zone with the given id and a random number of up to
	 * {@code maxAntennas} antennas.
	 * 
	 * @param id                 The id/name of the zone to generate.
	 * @param maxAntennas        The max number of antennas to give the zone.<br/>
	 *                           A zone has at least one antenna.
	 * @param antennaIdBlacklist A collection of possible ids that may not be used.
	 * @return A {@link Pair} containing the zone id and antennas list.
	 * @throws IllegalArgumentException If {@code id} is empty, {@code maxAntennas}
	 *                                  is less than 1, or {@code maxAntennas} is
	 *                                  greater than {@link #MAX_ANTENNA_ID}.
	 * @throws NullPointerException     If {@code id} is {@code null}.
	 */
	public static ZoneInfo generateZone(String id, int maxAntennas, Collection<String> antennaIdBlacklist)
			throws IllegalArgumentException, NullPointerException {
		Objects.requireNonNull(id, "The id for the newly generated zone can't be null.");
		id = id.trim();

		if (id.isEmpty()) {
			throw new IllegalArgumentException("The zone id cannot be empty.");
		}

		if (maxAntennas < 1) {
			throw new IllegalArgumentException("maxAntennas cannot be <= 0.");
		} else if (maxAntennas > MAX_ANTENNA_ID) {
			throw new IllegalArgumentException(
					"Cannot generate more than MAX_ANTENNA_ID(" + MAX_ANTENNA_ID + ") antennas.");
		}

		if (antennaIdBlacklist == null) {
			antennaIdBlacklist = new HashSet<String>();
		}

		int nAnt = AntennaDataGenerator.nextInt(maxAntennas, 1);
		List<String> antennas = new ArrayList<String>();
		for (int i = 0; i < nAnt; i++) {
			String antenna = Integer.toHexString(AntennaDataGenerator.nextInt(MAX_ANTENNA_ID));
			while (antennas.contains(antenna) || antennaIdBlacklist.contains(antenna)) {
				antenna = Integer.toHexString(AntennaDataGenerator.nextInt(MAX_ANTENNA_ID));
			}
			antennas.add(antenna);
		}

		return new ZoneInfo(id, true, antennas);
	}

	/**
	 * Generates a list of {@code number} zones with the name "{@code Zone i}" where
	 * i is in incrementing number.
	 * 
	 * @param number The number of zones to generate.
	 * @return A map containing all the generated zones.
	 * @throws IllegalArgumentException If {@code number} is less than 1.
	 */
	public static List<ZoneInfo> generateZones(int number) throws IllegalArgumentException {
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
	public static List<ZoneInfo> generateZones(int number, int maxAntennas) throws IllegalArgumentException {
		if (number < 1) {
			throw new IllegalArgumentException("Cannot generate less than one zone.");
		} else if (maxAntennas < 1) {
			throw new IllegalArgumentException("Cannot generate zones with less than one antenna.");
		}

		List<ZoneInfo> zones = new ArrayList<ZoneInfo>();
		Set<String> antennas = new HashSet<String>();
		for (int i = 1; i <= number; i++) {
			ZoneInfo zone = generateZone("Zone " + i, maxAntennas, antennas);
			zones.add(zone);
			antennas.addAll(zone.getAntennas());
		}

		return zones;
	}

}
