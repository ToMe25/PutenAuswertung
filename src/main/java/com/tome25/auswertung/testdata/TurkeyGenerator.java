package com.tome25.auswertung.testdata;

import static com.tome25.auswertung.testdata.AntennaDataGenerator.RANDOM;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.args.Arguments;

/**
 * A class for generating random {@link TurkeyInfo} objects, for example for
 * random input data.
 * 
 * @author theodor
 */
public class TurkeyGenerator {

	/**
	 * The number of possible turkey ids if a turkey id is to be randomly
	 * generated.<br/>
	 * Turkey ids can be anything from 0 to {@code MAX_TURKEY_ID-1}.
	 */
	private static final int MAX_TURKEY_ID = 500;

	/**
	 * The maximum possible transponder id to be used for turkeys.
	 */
	private static final int MAX_TRANSPONDER_ID = 0xFFFFFF;

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
	 */
	public static TurkeyInfo generateTurkey(String id, int maxTransponders, Arguments args,
			Collection<String> transponderIdBlacklist, String initZone, Calendar initTime) {
		if (id == null || id.trim().isEmpty()) {
			id = Integer.toString(RANDOM.nextInt(MAX_TURKEY_ID));
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

		int nTrans = RANDOM.nextInt(maxTransponders);
		List<String> transponders = new ArrayList<String>();
		for (int i = 0; i <= nTrans; i++) {
			String transponder = Integer.toHexString(RANDOM.nextInt(MAX_TRANSPONDER_ID)).toUpperCase();
			while (transponders.contains(transponder) || transponderIdBlacklist.contains(transponder)) {
				transponder = Integer.toHexString(RANDOM.nextInt(MAX_TRANSPONDER_ID)).toUpperCase();
			}
			transponders.add(transponder);
		}

		return new TurkeyInfo(id, transponders, null, initZone, initTime, null, args);
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
	public static List<TurkeyInfo> generateTurkeys(int number) throws IllegalArgumentException {
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
	 * @throws IllegalArgumentException If {@code number} or {@code maxTransponders}
	 *                                  is less than 1.
	 */
	public static List<TurkeyInfo> generateTurkeys(int number, int maxTransponders) throws IllegalArgumentException {
		if (number < 1) {
			throw new IllegalArgumentException("Cannot generate less than 1 turkey.");
		} else if (maxTransponders < 1) {
			throw new IllegalArgumentException("Cannot generate turkeys with less than 1 transponder.");
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

}
