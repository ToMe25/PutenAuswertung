package com.tome25.auswertung.testdata;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.ZoneInfo;
import com.tome25.auswertung.args.Arguments;

/**
 * A class for generating random {@link TurkeyInfo} objects, for example for
 * random input data.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class TurkeyGenerator {

	/**
	 * The maximum possible turkey id, for if a turkey id is to be randomly
	 * generated.<br/>
	 * Turkey ids can be anything from 0 to {@code MAX_TURKEY_ID}.
	 */
	protected static final int MAX_TURKEY_ID = 0xFFF;

	/**
	 * The maximum possible transponder id to be used for turkeys.
	 */
	protected static final int MAX_TRANSPONDER_ID = 0xFFFFFF;

	/**
	 * The default max number of transponders per turkey.
	 */
	private static final int DEFAULT_MAX_TRANSPONDERS = 10;

	/**
	 * An empty {@link Arguments} object for internal use.
	 */
	private static final Arguments EMPTY_ARGS = Arguments.empty();

	/**
	 * Generates a {@link TurkeyInfo} object with a random id and a random number of
	 * random transponders.
	 * 
	 * @return A {@link TurkeyInfo} object representing a not yet recorded turkey.
	 */
	public static TurkeyInfo generateTurkey() {
		return generateTurkey(null);
	}

	/**
	 * Generates a {@link TurkeyInfo} object with a random number of random
	 * transponders, and the given id.<br/>
	 * The max number of transponders is {@link #DEFAULT_MAX_TRANSPONDERS}.
	 * 
	 * @param id The id of the newly generated {@link TurkeyInfo}.<br/>
	 *           {@code null} for random.
	 * @return A {@link TurkeyInfo} object representing a not yet recorded turkey.
	 */
	public static TurkeyInfo generateTurkey(String id) {
		return generateTurkey(id, DEFAULT_MAX_TRANSPONDERS);
	}

	/**
	 * Generates a {@link TurkeyInfo} object with a random number of at most
	 * {@code maxTransponders} random transponders, and the given turkey id.
	 * 
	 * @param id              The id of the new turkey.
	 * @param maxTransponders The max number of transponders to give to the new
	 *                        {@link TurkeyInfo}.
	 * @return A {@link TurkeyInfo} object representing a not yet recorded turkey.
	 */
	public static TurkeyInfo generateTurkey(String id, int maxTransponders) {
		return generateTurkey(id, maxTransponders, EMPTY_ARGS);
	}

	/**
	 * Generates a {@link TurkeyInfo} object with a random number of at most
	 * {@code maxTransponders} random transponders, and the given turkey id.
	 * 
	 * @param id              The id of the new turkey.
	 * @param maxTransponders The max number of transponders to give to the new
	 *                        {@link TurkeyInfo}.
	 * @param args            The arguments to be used by the new
	 *                        {@link TurkeyInfo}.
	 * @return A {@link TurkeyInfo} object representing a not yet recorded turkey.
	 */
	public static TurkeyInfo generateTurkey(String id, int maxTransponders, Arguments args) {
		return generateTurkey(id, maxTransponders, args, null);
	}

	/**
	 * Generates a {@link TurkeyInfo} object with a random number of at most
	 * {@code maxTransponders} random transponders, and the given turkey id.
	 * 
	 * @param id                     The id of the new turkey.
	 * @param maxTransponders        The max number of transponders to give to the
	 *                               new {@link TurkeyInfo}.
	 * @param args                   The arguments to be used by the new
	 *                               {@link TurkeyInfo}.
	 * @param transponderIdBlacklist A collection of strings that may not be used as
	 *                               transponder ids.<br/>
	 *                               Can be null of no id should be blacklisted.
	 * @return A {@link TurkeyInfo} object representing a not yet recorded turkey.
	 */
	public static TurkeyInfo generateTurkey(String id, int maxTransponders, Arguments args,
			Collection<String> transponderIdBlacklist) {
		return generateTurkey(id, maxTransponders, args, transponderIdBlacklist, null, null);
	}

	/**
	 * Generates a {@link TurkeyInfo} object with a random number of at most
	 * {@code maxTransponders} random transponders, and the given turkey id.
	 * 
	 * @param id                     The id of the new turkey.
	 * @param maxTransponders        The max number of transponders to give to the
	 *                               new {@link TurkeyInfo}.
	 * @param args                   The arguments to be used by the new
	 *                               {@link TurkeyInfo}.
	 * @param transponderIdBlacklist A collection of strings that may not be used as
	 *                               transponder ids.<br/>
	 *                               Can be null of no id should be blacklisted.
	 * @param initZone               The initial zone for the new turkey.
	 * @param initTime               The time of the first record for the new
	 *                               turkey.
	 * @return A {@link TurkeyInfo} object representing a not yet recorded turkey.
	 * @throws IllegalArgumentException If {@code maxTransponders} is greater than
	 *                                  {@link #MAX_TRANSPONDER_ID}.
	 */
	public static TurkeyInfo generateTurkey(String id, int maxTransponders, Arguments args,
			Collection<String> transponderIdBlacklist, ZoneInfo initZone, Calendar initTime)
			throws IllegalArgumentException {
		if (maxTransponders > MAX_TRANSPONDER_ID) {
			throw new IllegalArgumentException("Cannot generate more than MAX_TRANSPONDER_ID(" + MAX_TRANSPONDER_ID
					+ ") transponders for one turkey.");
		}

		if (id == null || id.trim().isEmpty()) {
			id = Integer.toString(AntennaDataGenerator.nextInt(MAX_TURKEY_ID));
		}

		if (maxTransponders < 1) {
			maxTransponders = DEFAULT_MAX_TRANSPONDERS;
		}

		if (args == null) {
			args = EMPTY_ARGS;
		}

		if (transponderIdBlacklist == null) {
			transponderIdBlacklist = new HashSet<String>();
		}

		int nTrans = AntennaDataGenerator.nextInt(maxTransponders, 1);
		List<String> transponders = new ArrayList<String>();
		for (int i = 0; i < nTrans; i++) {
			String transponder = String.format("%06X", AntennaDataGenerator.nextInt(MAX_TRANSPONDER_ID));
			while (transponders.contains(transponder) || transponderIdBlacklist.contains(transponder)) {
				transponder = String.format("%06X", AntennaDataGenerator.nextInt(MAX_TRANSPONDER_ID));
			}
			transponders.add(transponder);
		}

		return new TurkeyInfo(id, transponders, null, initZone, initTime, null, null, args);
	}

	/**
	 * Generates a list of turkeys with incrementing ids of 0 to
	 * {@code number-1}.<br/>
	 * These {@link TurkeyInfo TurkeyInfos} represent not yet recorded turkeys.<br/>
	 * Each turkey has a random number of up to {@link #DEFAULT_MAX_TRANSPONDERS}
	 * random transponders.
	 * 
	 * @param number The number of turkeys to generate.
	 * @return The list containing all the newly generated turkeys.
	 * @throws IllegalArgumentException If {@code number} is less than 1.
	 */
	public static List<TurkeyInfo> generateTurkeys(final int number) throws IllegalArgumentException {
		return generateTurkeys(number, DEFAULT_MAX_TRANSPONDERS);
	}

	/**
	 * Generates a list of turkeys with incrementing ids of 0 to
	 * {@code number-1}.<br/>
	 * These {@link TurkeyInfo TurkeyInfos} represent not yet recorded turkeys.<br/>
	 * Each turkey has a random number of up to {@code maxTransponders} random
	 * transponders.
	 * 
	 * @param number          The number of turkeys to generate.
	 * @param maxTransponders The max number of transponders per turkey.
	 * @return The list containing all the newly generated turkeys.
	 * @throws IllegalArgumentException If {@code number} is less than 1,
	 *                                  {@code number} is greater than
	 *                                  {@link #MAX_TURKEY_ID},
	 *                                  {@code maxTransponders} is less than 1, or
	 *                                  {@code number * maxTransponders} is greater
	 *                                  than {@link #MAX_TRANSPONDER_ID}.
	 */
	public static List<TurkeyInfo> generateTurkeys(final int number, final int maxTransponders)
			throws IllegalArgumentException {
		if (number < 1) {
			throw new IllegalArgumentException("Cannot generate less than 1 turkey.");
		} else if (number > MAX_TURKEY_ID) {
			throw new IllegalArgumentException(
					"Cannot generate more than MAX_TURKEY_ID(" + MAX_TURKEY_ID + ") turkeys.");
		} else if (maxTransponders < 1) {
			throw new IllegalArgumentException("Cannot generate turkeys with less than 1 transponder.");
		} else if (maxTransponders * number > MAX_TRANSPONDER_ID) {
			throw new IllegalArgumentException("Cannot generate " + maxTransponders + " transponders for " + number
					+ " turkeys, since this could generate more than MAX_TRANSPONDER_ID(" + MAX_TRANSPONDER_ID
					+ ") transponders.");
		}

		List<TurkeyInfo> turkeys = new ArrayList<TurkeyInfo>();
		Set<String> transponders = new HashSet<String>();
		for (int i = 0; i < number; i++) {
			TurkeyInfo turkey = generateTurkey(Integer.toString(i), maxTransponders, EMPTY_ARGS, transponders);
			transponders.addAll(turkey.getTransponders());
			turkeys.add(turkey);
		}

		return turkeys;
	}

	/**
	 * Generates a list of a bit more advanced {@link TurkeyInfo} objects.<br/>
	 * Their ids are a 'T' followed by 3 random Hex digits.<br/>
	 * That means this method can not generate more than {@link #MAX_TURKEY_ID}
	 * turkeys, and generating that may will be incredibly slow since duplicate ids
	 * aren't allowed.<br/>
	 * <br/>
	 * Each {@link TurkeyInfo} object represents a not yet recorded turkey.<br/>
	 * If {@code zones} isn't {@code null} each turkey has a 50% chance to have a
	 * random current zone.<br/>
	 * Each turkey also has a 50% chance to have an end time somewhere between
	 * {@code start} and {@code end}.
	 * 
	 * @param number          The number of {@link TurkeyInfo TurkeyInfos} to
	 *                        generate.
	 * @param maxTransponders The max number of transponders per turkey.
	 * @param zones           The zones in which a turkey can start.<br/>
	 *                        Use {@code null} to disable giving turkeys a start
	 *                        zone.
	 * @param start           The earliest possible end time for turkeys.
	 * @param end             The last possible end time for turkeys.
	 * @return The list containing all the newly generated turkeys.
	 * @throws IllegalArgumentException If {@code number} is less than 1,
	 *                                  {@code number} is greater than
	 *                                  {@link #MAX_TURKEY_ID},
	 *                                  {@code maxTransponders} is less than 1, or
	 *                                  {@code number * maxTransponders} is greater
	 *                                  than {@link #MAX_TRANSPONDER_ID}. Also if
	 *                                  {@code end} isn't after {@code start}.
	 * @throws NullPointerException     If {@code zones} is {@code null}.
	 */
	public static List<TurkeyInfo> generateTurkeysAdvanced(final short number, final int maxTransponders,
			final List<ZoneInfo> zones, final long start, final long end)
			throws IllegalArgumentException, NullPointerException {
		if (number < 1) {
			throw new IllegalArgumentException("Cannot generate less than 1 turkey.");
		} else if (number > MAX_TURKEY_ID) {
			throw new IllegalArgumentException(
					"Cannot generate more than MAX_TURKEY_ID(" + MAX_TURKEY_ID + ") turkeys.");
		} else if (maxTransponders < 1) {
			throw new IllegalArgumentException("Cannot generate turkeys with less than 1 transponder.");
		} else if (maxTransponders * number > MAX_TRANSPONDER_ID) {
			throw new IllegalArgumentException("Cannot generate " + maxTransponders + " transponders for " + number
					+ " turkeys, since this could generate more than MAX_TRANSPONDER_ID(" + MAX_TRANSPONDER_ID
					+ ") transponders.");
		} else if (end <= start) {
			throw new IllegalArgumentException("End time has to be after start time.");
		}

		List<TurkeyInfo> turkeys = new ArrayList<TurkeyInfo>();
		Set<String> transponders = new HashSet<String>();
		Set<Integer> ids = new HashSet<Integer>();
		for (int i = 0; i < number; i++) {
			int tId = AntennaDataGenerator.nextInt(MAX_TURKEY_ID);
			while (ids.contains(tId)) {
				tId = AntennaDataGenerator.nextInt(MAX_TURKEY_ID);
			}
			TurkeyInfo turkey = generateTurkey(String.format("T%03X", tId), maxTransponders, EMPTY_ARGS, transponders);
			ids.add(tId);

			ZoneInfo startZone = null;
			if (zones != null && AntennaDataGenerator.nextInt(1) == 0) {
				startZone = zones.get(AntennaDataGenerator.nextInt(zones.size() - 1));
			}

			Calendar endTime = null;
			if (AntennaDataGenerator.nextInt(1) == 0) {
				endTime = new GregorianCalendar();
				endTime.setTimeZone(TimeZone.getTimeZone("GMT"));
				endTime.setTimeInMillis(start + AntennaDataGenerator.nextInt((int) (end - start)) / 10 * 10);
			}

			turkey = new TurkeyInfo(turkey.getId(), turkey.getTransponders(), null, startZone, null, null, endTime,
					EMPTY_ARGS);
			transponders.addAll(turkey.getTransponders());
			turkeys.add(turkey);
		}

		return turkeys;
	}

}
