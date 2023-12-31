package com.tome25.auswertung;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.log.LogHandler;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.IInputStreamHandler;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.utils.IntOrStringComparator;
import com.tome25.auswertung.utils.MapUtils;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.StringUtils;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * This class is responsible for converting between internal data structures and
 * parts of a csv file.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class CSVHandler {

	/**
	 * The default character to use as a value separator in produced csvs.
	 */
	public static final char DEFAULT_SEPARATOR = ';';

	/**
	 * A regex string containing all the characters to split csv files at.
	 */
	private static final Pattern SEPARATOR_REGEX = Pattern.compile("[,;\t]");

	/**
	 * A regex string matching a valid id.<br/>
	 * Ids can contain uppercase letters, lowercase letters, digits, spaces, and
	 * hyphens.
	 */
	public static final Pattern ID_REGEX = Pattern.compile("[A-Za-z0-9\\s\\-]+");

	/**
	 * The {@link Comparator} used to compare downtimes for sorting.<br/>
	 * Downtimes are sorted by their start time.
	 */
	private static final Comparator<Pair<Long, Long>> DOWNTIME_COMPARATOR = new Comparator<Pair<Long, Long>>() {
		@Override
		public int compare(Pair<Long, Long> o1, Pair<Long, Long> o2) {
			return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, o1.getKey() - o2.getKey()));
		}
	};

	/**
	 * Makes sure the given input stream is non-null and non-empty.<br/>
	 * Also prints a debug message about starting to read the input file.
	 * 
	 * @param input The input stream handler to check.
	 * @throws EOFException         If the input stream handler is already
	 *                              {@link IInputStreamHandler#done() done}, aka has
	 *                              no more content to read.
	 * @throws NullPointerException If the given input stream handler is
	 *                              {@code null}.
	 */
	private static void checkInput(IInputStreamHandler input) throws EOFException, NullPointerException {
		Objects.requireNonNull(input, "input cannot be null.");

		if (input.done()) {
			throw new EOFException("Trying to read empty input stream.");
		}

		if (input instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println("Started reading file " + ((FileInputStreamHandler) input).getInputFile().getPath(),
					true);
		}
	}

	/**
	 * Splits the given input line into its separate tokens.<br/>
	 * Attempts to re-join hundredth separated from a time using a comma.
	 * 
	 * @param line       The input line to split.
	 * @param min_tokens The minimum number of tokens expected.
	 * @param times      The indices of time values.
	 * @return A string array containing the separate tokens.
	 * @throws NullPointerException     If {@code line} is {@code null}.
	 * @throws IllegalArgumentException If {@code line} is empty or contains less
	 *                                  than {@code min_tokens} tokens.
	 */
	private static String[] splitLine(String line, int min_tokens, Collection<Integer> times)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(line, "The line to split cannot be null.");
		line = line.trim();
		if (line.isEmpty()) {
			throw new IllegalArgumentException("The given line is empty.");
		}

		if (times == null) {
			times = new HashSet<Integer>();
		}

		List<String> tokens = new ArrayList<String>();
		Matcher match = SEPARATOR_REGEX.matcher(line);
		int separators = 0;
		int end = 0;
		while (match.find()) {
			separators++;
			end = match.end();
		}
		if (end == line.length()) {
			separators--;
		}
		match.reset();

		if (separators + 1 < min_tokens) {
			throw new IllegalArgumentException(
					"The line \"" + line + "\" did not contain at least " + min_tokens + " tokens.");
		}

		int index = 0;
		while (match.find()) {
			String subString = line.substring(index, match.start());
			if (times.contains(tokens.size()) && !subString.contains(".") && line.charAt(match.start()) == ','
					&& separators >= min_tokens) {
				end = match.end();
				if (match.find()) {
					if (match.start() - end <= 2 && match.start() > end) {
						boolean failed = false;
						for (int i = end; i < match.start(); i++) {
							if (!Character.isDigit(line.charAt(i))) {
								failed = true;
								break;
							}
						}

						if (!failed) {
							tokens.add(line.substring(index, match.start()));
							index = match.end();
							separators--;
						} else {
							tokens.add(subString);
							tokens.add(line.substring(end, match.start()));
							index = match.end();
						}
					} else {
						tokens.add(subString);
						tokens.add(line.substring(end, match.start()));
						index = match.end();
					}
				}
			} else {
				tokens.add(subString);
				index = match.end();
			}
		}

		if (index < line.length()) {
			tokens.add(line.substring(index));
		}

		if (tokens.size() < min_tokens) {
			throw new IllegalArgumentException(
					"The line \"" + line + "\" did not contain at least " + min_tokens + " tokens.");
		}

		return tokens.toArray(new String[tokens.size()]);
	}

	/**
	 * Reads the data from the stream handler as a CSV file and converts it to a
	 * map.<br/>
	 * Creates a {@link ZoneInfo} object for each valid line.<br/>
	 * The format of the map is The second map format is
	 * {@code antenna id -> zone info}.
	 * 
	 * @param input The stream handler containing the data to be read.
	 * @return A map containing the {@link ZoneInfo} object for each antenna
	 *         id.<br/>
	 *         Or {@code null} if there was no valid data in the input.
	 * @throws NullPointerException if input is {@code null}.
	 */
	public static Map<String, ZoneInfo> readZonesCSV(IInputStreamHandler input) throws NullPointerException {
		try {
			checkInput(input);
		} catch (EOFException e) {
			LogHandler.err_println("Input file did not contain any data.");
			LogHandler.print_exception(e, "read a zones file", "Input Stream Handler: %s", input.toString());
			return null;
		}

		Map<String, ZoneInfo> zones = new HashMap<String, ZoneInfo>();
		Set<String> zoneIds = new HashSet<String>();
		boolean last_failed = false;
		while (!input.done()) {
			try {
				String line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Skipped an empty line from input file.", true);
					LogHandler.print_debug_info("Input Stream Handler: %s, Line: \"%s\"", input.toString(), line);
					continue;
				}

				String tokens[] = null;
				try {
					tokens = splitLine(line, 3, null);
				} catch (IllegalArgumentException e) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain at least two tokens. Skipping line.");
					LogHandler.print_exception(e, "split input line",
							"Separator Chars: %s, Line: \"%s\", Input Stream Handler: %s", SEPARATOR_REGEX.toString(),
							line, input.toString());
					continue;
				}

				if (tokens[0].equalsIgnoreCase("bereich")) {
					LogHandler.out_println("Read header line \"" + line + "\".", true);
					continue;
				}

				if (zoneIds.contains(tokens[0])) {
					LogHandler.err_println("Found duplicate zone id \"" + tokens[0] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				} else if (tokens[0].isEmpty()) {
					LogHandler.err_println("Found empty zone id in line \"" + line + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				} else if (!ID_REGEX.matcher(tokens[0]).matches()) {
					LogHandler.err_println("Found invalid zone id \"" + tokens[0] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				}

				boolean hasFood = true;
				tokens[1] = tokens[1].trim();
				if (!tokens[1].isEmpty() && tokens[1].length() == 1
						&& Character.toLowerCase(tokens[1].charAt(0)) == 'x') {
					hasFood = false;
				} else if (!tokens[1].isEmpty()) {
					LogHandler.err_println(
							"Found invalid nofood value \"" + tokens[1] + "\". Treating it like a zone with food.");
					LogHandler.err_println(
							"Please put an 'X' to set that the zone has no food, or leave it empty if it has food.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
				}

				List<String> antennas = new ArrayList<String>();
				for (int i = 2; i < tokens.length; i++) {
					if (zones.containsKey(tokens[i])) {
						LogHandler.err_println("Found duplicate antenna id \"" + tokens[i]
								+ "\". Ignoring the occurrence for zone \"" + tokens[0] + "\".");
						LogHandler.print_debug_info(
								"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					} else if (tokens[i].isEmpty()) {
						LogHandler.err_println("Found empty antenna id in line \"" + line + "\". Skipping.", true);
						LogHandler.print_debug_info(
								"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					} else if (!ID_REGEX.matcher(tokens[i]).matches()) {
						LogHandler.err_println("Found invalid antenna id \"" + tokens[i] + "\" for zone \"" + tokens[0]
								+ "\". Skipping.");
						LogHandler.print_debug_info(
								"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					} else {
						antennas.add(tokens[i]);
					}
				}

				if (antennas.isEmpty()) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain at least one valid value. Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				}

				ZoneInfo zone = new ZoneInfo(tokens[0], hasFood, antennas);
				for (String antenna : antennas) {
					zones.put(antenna, zone);
				}
				zoneIds.add(tokens[0]);
				last_failed = false;
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Returning current zones set.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read a zones file", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX.toString());

				if (last_failed) {
					break;
				} else {
					last_failed = true;
				}
			}
		}

		if (input instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println("Finished reading file " + ((FileInputStreamHandler) input).getInputFile().getPath(),
					true);
		}

		if (zones.isEmpty()) {
			LogHandler.err_println("Zones input file did not contain any valid data.");
			LogHandler.print_debug_info("Input Stream Handler: %s", input.toString());
			return null;
		} else {
			return zones;
		}
	}

	/**
	 * Writes the given zones map to an input file.
	 * 
	 * @param zones  The zones to write to the file.
	 * @param output The output stream handler to write the zones to.
	 * @throws NullPointerException If one of the parameters if {@code null}.
	 */
	public static void writeZonesCSV(final List<ZoneInfo> zones, IOutputStreamHandler output)
			throws NullPointerException {
		Objects.requireNonNull(zones, "The map of zones to write to the file cannot be null.");
		Objects.requireNonNull(output, "The output stream handler to write to cannot be null.");

		int numAntennas = 0;
		for (ZoneInfo zone : zones) {
			if (zone.getAntennaCount() > numAntennas) {
				numAntennas = zone.getAntennaCount();
			}
		}

		StringBuilder headers = new StringBuilder();
		headers.append("Bereich");
		headers.append(DEFAULT_SEPARATOR);
		headers.append("Kein Essen");
		for (int i = 1; i <= numAntennas; i++) {
			headers.append(DEFAULT_SEPARATOR);
			headers.append("Antenne ");
			headers.append(i);
		}

		output.println(headers.toString());

		for (ZoneInfo zone : zones) {
			StringBuilder sb = new StringBuilder();
			sb.append(zone.getId());
			sb.append(DEFAULT_SEPARATOR);
			if (!zone.hasFood()) {
				sb.append('X');
			}

			for (String antenna : zone.getAntennas()) {
				sb.append(DEFAULT_SEPARATOR);
				sb.append(antenna);
			}

			for (int i = numAntennas - zone.getAntennaCount(); i > 0; i--) {
				sb.append(DEFAULT_SEPARATOR);
			}

			output.println(sb.toString());
		}
	}

	/**
	 * Reads the data from the stream handler as a turkeys CSV file and converts it
	 * to a map.<br/>
	 * A {@link TurkeyInfo} object is created for every valid line.<br/>
	 * The format of the output map is {@code transponder id -> turkey info}.
	 * 
	 * @param input The stream handler containing the data to be read.
	 * @param args  The {@link Arguments} instance to use for the {@link TurkeyInfo}
	 *              objects.
	 * @param zones A {@link Collection} containing the valid zone names, to be used
	 *              to check the turkeys start zones.<br/>
	 *              Use an empty collection to disable start zones.
	 * @return A map containing the {@link TurkeyInfo} object for each transponder
	 *         id.<br/>
	 *         Or {@code null} if there was no valid data in the input.
	 * @throws NullPointerException if one of the arguments is {@code null}.
	 */
	public static Map<String, TurkeyInfo> readTurkeyCSV(IInputStreamHandler input, final Arguments args,
			final Collection<ZoneInfo> zones) throws NullPointerException {
		Objects.requireNonNull(input, "The to read cannot be null.");
		Objects.requireNonNull(args, "The arguments to use to create TurkeyInfos cannot be null.");
		Objects.requireNonNull(zones, "The valid zone names cannot be null.");
		try {
			checkInput(input);
		} catch (EOFException e) {
			LogHandler.err_println("Input file did not contain any data.");
			LogHandler.print_exception(e, "read a turkey file", "Input Stream Handler: %s", input.toString());
			return null;
		}

		Map<String, ZoneInfo> idToZone = new HashMap<String, ZoneInfo>();
		for (ZoneInfo zone : zones) {
			idToZone.put(zone.getId(), zone);
		}

		Map<String, TurkeyInfo> turkeys = new HashMap<String, TurkeyInfo>();
		Set<String> turkeyIds = new HashSet<String>();
		boolean last_failed = false;
		while (!input.done()) {
			try {
				String line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Skipped an empty line from input file.", true);
					LogHandler.print_debug_info("Input Stream Handler: %s, Line: \"%s\"", input.toString(), line);
					continue;
				}

				String tokens[] = null;
				try {
					tokens = splitLine(line, 5, Collections.singleton(2));
				} catch (IllegalArgumentException e) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain at least five tokens. Skipping line.");
					LogHandler.print_exception(e, "split input line",
							"Separator Chars: %s, Line: \"%s\", Input Stream Handler: %s", SEPARATOR_REGEX.toString(),
							line, input.toString());
					continue;
				}

				if (tokens[0].equalsIgnoreCase("tier")) {
					LogHandler.out_println("Read header line \"" + line + "\".", true);
					continue;
				}

				if (turkeyIds.contains(tokens[0])) {
					LogHandler.err_println("Found duplicate turkey id \"" + tokens[0] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				} else if (tokens[0].isEmpty()) {
					LogHandler.err_println("Found empty turkey id in line \"" + line + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				} else if (!ID_REGEX.matcher(tokens[0]).matches()) {
					LogHandler.err_println("Found invalid turkey id \"" + tokens[0] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				}

				String startZone = null;
				if (!tokens[1].isEmpty() && !ID_REGEX.matcher(tokens[1]).matches()) {
					LogHandler.err_println("Found invalid start zone id \"" + tokens[1] + "\" for turkey \"" + tokens[0]
							+ "\". Ignoring.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
				} else if (!tokens[1].isEmpty()) {
					startZone = tokens[1];
				}

				if (startZone != null && zones.isEmpty()) {
					LogHandler.err_println("Found start zone \"" + startZone + "\" for turkey \"" + tokens[0]
							+ "\" with start zones disabled. Ignoring.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Zones: %s, Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), zones.toString(), line,
							input.toString());
					startZone = null;
				} else if (startZone != null && !idToZone.containsKey(startZone)) {
					LogHandler.err_println("Found unknown start zone \"" + startZone + "\" for turkey \"" + tokens[0]
							+ "\". Ignoring.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Zones: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens),
							StringUtils.collectionToString(", ", zones), line, input.toString());
					startZone = null;
				}

				Calendar endTime = null;
				if (!tokens[2].isEmpty() && tokens[3].isEmpty()) {
					LogHandler.err_println("Found end time \"" + tokens[2] + "\" without end date for turkey \""
							+ tokens[0] + "\". Ignoring.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
				} else if (tokens[2].isEmpty() && !tokens[3].isEmpty()) {
					try {
						endTime = TimeUtils.parseDate(tokens[3]);
						LogHandler.err_println("Found end date \"" + tokens[3] + "\" without end time for turkey \""
								+ tokens[0] + "\". Removing turkey at beginning of the day.");
						LogHandler.print_debug_info(
								"End Date: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								tokens[3], SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line,
								input.toString());
					} catch (IllegalArgumentException e) {
						LogHandler.err_println("Failed to parse end date \"" + tokens[3] + "\" for turkey \""
								+ tokens[0] + "\". Ignoring.");
						LogHandler.print_exception(e, "parse end date",
								"End Date: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								tokens[3], SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line,
								input.toString());
					}
				} else if (!tokens[2].isEmpty() && !tokens[3].isEmpty()) {
					try {
						endTime = TimeUtils.parseTime(tokens[3], tokens[2]);
					} catch (IllegalArgumentException e) {
						LogHandler.err_println("Failed to parse end time \"" + tokens[2] + "\" or end date \""
								+ tokens[3] + "\" for turkey \"" + tokens[0] + "\". Ignoring end time and date.");
						LogHandler.print_exception(e, "parse end time",
								"End Date: %s, End Time: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								tokens[3], tokens[2], SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line,
								input.toString());
					}
				}

				List<String> transponders = new ArrayList<String>();
				for (int i = 4; i < tokens.length; i++) {
					if (turkeys.containsKey(tokens[i])) {
						LogHandler.err_println("Found duplicate transponder id \"" + tokens[i]
								+ "\". Ignoring the occurrence for turkey \"" + tokens[0] + "\".");
						LogHandler.print_debug_info(
								"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					} else if (tokens[i].isEmpty()) {
						LogHandler.err_println("Found empty transponder id in line \"" + line + "\". Skipping.", true);
						LogHandler.print_debug_info(
								"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					} else if (!ID_REGEX.matcher(tokens[i]).matches()) {
						LogHandler.err_println("Found invalid transponder id \"" + tokens[i] + "\" for turkey \""
								+ tokens[0] + "\". Skipping.");
						LogHandler.print_debug_info(
								"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					} else {
						transponders.add(tokens[i]);
					}
				}

				if (transponders.isEmpty()) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain at least one valid value. Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				}

				TurkeyInfo turkey = new TurkeyInfo(tokens[0], transponders, null, idToZone.get(startZone), null, null,
						endTime, args);
				for (String transponder : transponders) {
					turkeys.put(transponder, turkey);
				}
				turkeyIds.add(tokens[0]);
				last_failed = false;
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Returning current turkey set.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read a turkey file", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX.toString());

				if (last_failed) {
					break;
				} else {
					last_failed = true;
				}
			}
		}

		if (input instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println("Finished reading file " + ((FileInputStreamHandler) input).getInputFile().getPath(),
					true);
		}

		turkeys = MapUtils.sortByValue(turkeys, null);

		if (turkeys.isEmpty()) {
			LogHandler.err_println("Turkey input file did not contain any valid data.");
			LogHandler.print_debug_info("Input Stream Handler: %s", input.toString());
			return null;
		} else {
			return turkeys;
		}
	}

	/**
	 * Writes the given list of {@link TurkeyInfo} objects to a turkeys input
	 * file.<br/>
	 * Writs the current zone of the {@link TurkeyInfo} as its start zone.
	 * 
	 * @param turkeys The {@link TurkeyInfo} objects to write to a file.
	 * @param output  The output stream handler to write the file to.
	 * @throws NullPointerException If one of the parameters if {@code null}.
	 */
	public static void writeTurkeyCSV(List<TurkeyInfo> turkeys, IOutputStreamHandler output)
			throws NullPointerException {
		Objects.requireNonNull(turkeys, "The list of turkeys to write can't be null.");
		Objects.requireNonNull(output, "The output stream handler to write to can't be null.");

		int numTransponders = 0;
		for (TurkeyInfo ti : turkeys) {
			if (ti.getTransponders().size() > numTransponders) {
				numTransponders = ti.getTransponders().size();
			}
		}

		StringBuilder headers = new StringBuilder();
		headers.append(String.format("Tier%1$cStartbereich%1$cEndzeit%1$cEnddatum", DEFAULT_SEPARATOR));
		for (int i = 1; i <= numTransponders; i++) {
			headers.append(DEFAULT_SEPARATOR);
			headers.append("Transponder ");
			headers.append(i);
		}

		output.println(headers.toString());

		for (TurkeyInfo ti : turkeys) {
			StringBuilder sb = new StringBuilder();
			sb.append(ti.getId());
			sb.append(DEFAULT_SEPARATOR);
			if (ti.getCurrentZone() != null) {
				sb.append(ti.getCurrentZone().getId());
			}

			sb.append(DEFAULT_SEPARATOR);
			if (ti.getEndCal() != null) {
				sb.append(TimeUtils.encodeTime(TimeUtils.getMsOfDay(ti.getEndCal())));
			}
			sb.append(DEFAULT_SEPARATOR);
			if (ti.getEndCal() != null) {
				sb.append(TimeUtils.encodeDate(ti.getEndCal()));
			}

			List<String> trans = ti.getTransponders();
			for (int i = 0; i < trans.size(); i++) {
				sb.append(DEFAULT_SEPARATOR);
				sb.append(trans.get(i));
			}

			for (int i = numTransponders - trans.size(); i > 0; i--) {
				sb.append(DEFAULT_SEPARATOR);
			}

			output.println(sb.toString());
		}
	}

	/**
	 * Parses a downtimes csv file.<br/>
	 * Creates a pair with the start and end time of each downtime and sorts them by
	 * start time.<br/>
	 * Overlapping downtimes are merged to prevent issues.
	 * 
	 * @param input The {@link IInputStreamHandler} to read the data from.
	 * @return A list containing the downtimes, or {@code null} if there weren't
	 *         any.
	 * @throws NullPointerException If {@code input} is {@code null}.
	 */
	public static List<Pair<Long, Long>> readDowntimesCSV(IInputStreamHandler input) throws NullPointerException {
		try {
			checkInput(input);
		} catch (EOFException e) {
			LogHandler.err_println("Input file did not contain any data.");
			LogHandler.print_exception(e, "read a downtimes file", "Input Stream Handler: %s", input.toString());
			return null;
		}

		List<Pair<Long, Long>> downtimes = new ArrayList<Pair<Long, Long>>();
		boolean last_failed = false;
		while (!input.done()) {
			String line = null;
			String tokens[] = null;
			Calendar start = null;
			try {
				line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Skipped an empty line from input file.", true);
					LogHandler.print_debug_info("Input Stream Handler: %s, Line: \"%s\"", input.toString(), line);
					continue;
				}

				try {
					tokens = splitLine(line, 4, Arrays.asList(1, 3));
				} catch (IllegalArgumentException e) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain exactly four tokens. Skipping line.");
					LogHandler.print_exception(e, "split input line",
							"Separator Chars: %s, Line: \"%s\", Input Stream Handler: %s", SEPARATOR_REGEX.toString(),
							line, input.toString());
					continue;
				}

				if (tokens.length != 4) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain exactly four tokens. Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				}

				if (tokens[0].toLowerCase().startsWith("start")) {
					LogHandler.out_println("Read header line \"" + line + "\".", true);
					continue;
				}

				start = TimeUtils.parseTime(tokens[0], tokens[1]);
				Calendar end = TimeUtils.parseTime(tokens[2], tokens[3]);
				if (!end.after(start)) {
					LogHandler.err_println("Downtime end wasn't after its start. Skipping line.");
					LogHandler.print_debug_info(
							"Start Date: %s, Start Time: %s, End Date: %s, End Time: %s, Line: \"%s\", "
									+ "Separator Chars: %s, Tokens: [%s], Input Stream Handler: %s",
							TimeUtils.encodeDate(start), TimeUtils.encodeTime(TimeUtils.getMsOfDay(start)),
							TimeUtils.encodeDate(end), TimeUtils.encodeTime(TimeUtils.getMsOfDay(end)), line,
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), input.toString());
					continue;
				}

				// Handle overlapping downtimes.
				long startMs = start.getTimeInMillis();
				long endMs = end.getTimeInMillis();
				Iterator<Pair<Long, Long>> downtimesIt = downtimes.iterator();
				while (downtimesIt.hasNext()) {
					Pair<Long, Long> downtime = downtimesIt.next();
					if (startMs >= downtime.getKey() && startMs <= downtime.getValue()) {
						startMs = downtime.getKey();
						if (endMs >= downtime.getKey() && endMs <= downtime.getValue()) {
							endMs = downtime.getValue();
						}
						downtimesIt.remove();
					} else if (endMs >= downtime.getKey() && endMs <= downtime.getValue()) {
						endMs = downtime.getValue();
						downtimesIt.remove();
					} else if (startMs < downtime.getKey() && endMs > downtime.getValue()) {
						downtimesIt.remove();
					}
				}
				downtimes.add(new Pair<Long, Long>(startMs, endMs));
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Returning current downtimes.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read a downtimes file", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX.toString());

				if (last_failed) {
					break;
				} else {
					last_failed = true;
				}
			} catch (IllegalArgumentException e) {
				if (start == null) {
					LogHandler
							.err_println("Failed to parse start time or date of line \"" + line + "\". Skipping line.");
				} else {
					LogHandler.err_println("Failed to parse end time or date of line \"" + line + "\". Skipping line.");
				}
				LogHandler.print_exception(e, "parse downtime line",
						"Start Date: %s, Start Time: %s, Line: \"%s\", Separator Chars: %s, Tokens: [%s], Input Stream Handler: %s",
						start == null ? "null" : TimeUtils.encodeDate(start),
						start == null ? "null" : TimeUtils.encodeTime(TimeUtils.getMsOfDay(start)), line,
						SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), input.toString());
			}
		}

		if (input instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println("Finished reading file " + ((FileInputStreamHandler) input).getInputFile().getPath(),
					true);
		}

		// Due to merging there can never be two pairs with the same key.
		Collections.sort(downtimes, DOWNTIME_COMPARATOR);

		if (downtimes.isEmpty()) {
			LogHandler.err_println("Input file did not contain any valid data.");
			LogHandler.print_debug_info("Input Stream Handler: %s", input.toString());
			return null;
		} else {
			return downtimes;
		}
	}

	/**
	 * Reads the next {@link AntennaRecord} from the given input.<br/>
	 * Handles skipping the header line and unparsable lines.
	 * 
	 * @param input      The stream handler to read from.
	 * @param tokenOrder The order in which the tokens to be parsed are in the input
	 *                   file.<br/>
	 *                   Set to {@code null} to use the default(0, 1, 2, 3).<br/>
	 *                   If a header line is found this array is updated.<br/>
	 *                   This is what each number in the array represents:<br/>
	 *                   <ul>
	 *                   <li>The first: The position of the recorded
	 *                   transponder</li>
	 *                   <li>The second: The position of the record date</li>
	 *                   <li>The third: The position of the record time</li>
	 *                   <li>The fourth: The position of the recording antenna</li>
	 *                   </ul>
	 * @return The newly created {@link AntennaRecord}. Or null if there was none.
	 * @throws NullPointerException If the input stream handler to read from is
	 *                              {@code null}.
	 */
	public static AntennaRecord readAntennaRecord(IInputStreamHandler input, short[] tokenOrder)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(input, "The input stream handler to read can't be null.");

		if (tokenOrder == null) {
			tokenOrder = new short[] { 0, 1, 2, 3 };
		}

		if (tokenOrder.length != 4) {
			throw new IllegalArgumentException("Token order has to be 4 numbers long.");
		}

		short valid = 0;
		for (int i = 0; i < tokenOrder.length; i++) {
			valid ^= (1 << tokenOrder[i]);
		}

		if (valid != 0b1111) {
			throw new IllegalArgumentException("Token order has to contain each number from 0 to 3 exactly once.");
		}

		AntennaRecord result = null;
		boolean last_failed = false;
		main_loop: while (!input.done()) {
			String line = null;
			String tokens[] = null;
			try {
				line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Skipped an empty line from input file.", true);
					LogHandler.print_debug_info("Input Stream Handler: %s, Line: \"%s\"", input.toString(), line);
					continue;
				}

				try {
					tokens = splitLine(line, 4, Collections.singleton((int) tokenOrder[2]));
				} catch (IllegalArgumentException e) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain exactly four tokens. Skipping line.");
					LogHandler.print_exception(e, "split input line",
							"Spearator Chars: %s, Line: \"%s\", Input Stream Handler: %s", SEPARATOR_REGEX.toString(),
							line, input.toString());
					continue;
				}

				if (tokens.length != 4) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain exactly four tokens. Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				}

				if (tokens[0].equalsIgnoreCase("transponder") || tokens[1].equalsIgnoreCase("transponder")
						|| tokens[2].equalsIgnoreCase("transponder") || tokens[3].equalsIgnoreCase("transponder")) {
					LogHandler.out_println("Read header line \"" + line + "\".", true);
					tokenOrder[0] = tokenOrder[1] = tokenOrder[2] = tokenOrder[3] = 0;

					for (short i = 0; i < tokens.length; i++) {
						tokens[i] = tokens[i].toLowerCase();
						if (tokens[i].equals("transponder")) {
							tokenOrder[0] = i;
						} else if (tokens[i].equals("date") || tokens[i].equals("datum")) {
							tokenOrder[1] = i;
						} else if (tokens[i].equals("time") || tokens[i].equals("zeit")) {
							tokenOrder[2] = i;
						} else if (tokens[i].equals("antenna") || tokens[i].equals("antenne")) {
							tokenOrder[3] = i;
						} else {
							LogHandler.err_println("Found invalid header \"" + tokens[i] + "\".");
							LogHandler.print_debug_info(
									"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
									SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
							break;
						}
					}

					valid = 0;
					for (int i = 0; i < tokenOrder.length; i++) {
						valid ^= (1 << tokenOrder[i]);
					}

					if (valid != 0b1111) {
						LogHandler.err_println(
								"Header line \"" + line + "\" was invalid. Assuming default column order.");
						LogHandler.print_debug_info(
								"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
						tokenOrder[0] = 0;
						tokenOrder[1] = 1;
						tokenOrder[2] = 2;
						tokenOrder[3] = 3;
					} else {
						LogHandler.out_println("Valid header line \"" + line + "\" found. Reordering columns.");
					}
					continue;
				}

				for (int i = 0; i < tokens.length; i++) {
					tokens[i] = tokens[i].trim();
					if (tokens[i].isEmpty()) {
						LogHandler.err_println("Input line \"" + line + "\" contained an empty token. Skipping line.");
						LogHandler.print_debug_info(
								"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
						continue main_loop;
					}
				}

				if (!ID_REGEX.matcher(tokens[tokenOrder[0]]).matches()) {
					LogHandler.err_println("Input line \"" + line + "\" contains invalid transponder id \""
							+ tokens[tokenOrder[0]] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				} else if (!ID_REGEX.matcher(tokens[tokenOrder[3]]).matches()) {
					LogHandler.err_println("Input line \"" + line + "\" contains invalid antenna id \""
							+ tokens[tokenOrder[3]] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				}

				result = new AntennaRecord(tokens[tokenOrder[0]], tokens[tokenOrder[1]], tokens[tokenOrder[2]],
						tokens[tokenOrder[3]]);
				break;
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Stopping.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read data input", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX.toString());

				if (last_failed) {
					break;
				} else {
					last_failed = true;
				}
			} catch (IllegalArgumentException e) {
				LogHandler.err_println("Parsing time of day or date of line \"" + line + "\" failed. Skipping line.");
				LogHandler.print_exception(e, "parse record time",
						"Line: \"%s\", Separator Chars: %s, Tokens: [%s], Input Stream Handler: %s", line,
						SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), input.toString());
			}
		}

		if (result == null) {
			LogHandler.err_println("Failed to read an antenna record from the input file.", true);
			LogHandler.print_debug_info("Input Stream Handler: %s", input.toString());
		}

		return result;
	}

	/**
	 * Returns the headers to be used for a totals csv with the given zones.
	 * 
	 * @param zones The zones that can be found in the input data.
	 * @return The csv headers as a single string.
	 * @throws NullPointerException If {@code zones} is {@code null}.
	 */
	public static String turkeyCsvHeader(Collection<String> zones) throws NullPointerException {
		Objects.requireNonNull(zones, "The list of zones for the header line cannot be null.");

		StringBuilder header = new StringBuilder();
		header.append("Tier");
		header.append(DEFAULT_SEPARATOR);
		header.append("Datum");
		header.append(DEFAULT_SEPARATOR);
		header.append("Bereichswechsel");
		for (String zone : zones) {
			header.append(DEFAULT_SEPARATOR);
			header.append("Zeit in Zone ");
			header.append(zone);
		}
		header.append(DEFAULT_SEPARATOR);
		header.append("Unzuverlaessig");

		return header.toString();
	}

	/**
	 * Creates a csv output line for the given turkey and date.
	 * 
	 * @param turkey The turkey for which an output line should be generated.
	 * @param date   The date for which to generate an output line. Set to
	 *               {@code null} to generate a total line.
	 * @param zones  A collection containing the names of all the zones to
	 *               write.<br/>
	 *               Used for ordering, selective printing, or printing zones this
	 *               turkey did not enter.<br/>
	 *               If this is {@code null} or empty the potentially incomplete
	 *               zone list from the {@link TurkeyInfo} is used instead.<br/>
	 *               If the {@link TurkeyInfo} zones are used they are ordered
	 *               alphabetically.
	 * @return The newly generated output line.
	 * @throws NullPointerException If turkey is {@code null}.
	 */
	public static String turkeyToCsvLine(TurkeyInfo turkey, String date, Collection<String> zones)
			throws NullPointerException {
		Objects.requireNonNull(turkey, "The turkey object to convert cannot be null.");

		Map<String, ?> zoneTimes = date == null ? turkey.getTotalZoneTimes() : turkey.getDayZoneTimes(date);

		if (zones == null || zones.isEmpty()) {
			zones = new ArrayList<String>(zoneTimes.keySet());
			Collections.sort((List<String>) zones, IntOrStringComparator.INSTANCE);
		}

		StringBuilder result = new StringBuilder();
		if (date != null) {
			result.append(turkey.getId());
			result.append(DEFAULT_SEPARATOR);
			result.append(date);
			result.append(DEFAULT_SEPARATOR);
			result.append(Integer.toString(turkey.getDayZoneChanges(date)));
		} else {
			result.append(turkey.getId());
			result.append(DEFAULT_SEPARATOR);
			result.append("total");
			result.append(DEFAULT_SEPARATOR);
			result.append(Integer.toString(turkey.getTotalZoneChanges()));
		}

		for (String zone : zones) {
			result.append(DEFAULT_SEPARATOR);
			if (zoneTimes.containsKey(zone)) {
				Object time = zoneTimes.get(zone);
				if (time instanceof Long) {
					result.append(TimeUtils.encodeTime((long) (Long) zoneTimes.get(zone)));
				} else if (time instanceof Integer) {
					result.append(TimeUtils.encodeTime((int) (Integer) zoneTimes.get(zone)));
				}
			} else {
				result.append(TimeUtils.encodeTime(0));
			}
		}

		result.append(DEFAULT_SEPARATOR);
		if ((date != null && turkey.isDayUnreliable(date)) || (date == null && turkey.hasUnreliableDay())) {
			result.append('X');
		}

		return result.toString();
	}

	/**
	 * Reads a totals output csv generated by this program.<br/>
	 * The result contains three maps:<br/>
	 * The first map is {@code turkey -> date -> zone -> time}.<br/>
	 * The second map is {@code turkey -> date -> zoneChanges}.<br/>
	 * The third map is {@code turkey -> [date]}
	 * 
	 * @param input The input stream handler to read the data from.
	 * @return The parsed totals date. Or {@code null} if the file did not contain
	 *         any valid data.
	 * @throws IOException          If reading the headers line fails.
	 * @throws NullPointerException If {@code input} is {@code null}.
	 */
	public static Pair<Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>>, Map<String, Set<String>>> readTotalsCSV(
			IInputStreamHandler input) throws IOException, NullPointerException {
		try {
			checkInput(input);
		} catch (EOFException e) {
			LogHandler.err_println("Input file did not contain any data.");
			LogHandler.print_exception(e, "read a totals file", "Input Stream Handler: %s", input.toString());
			return null;
		}

		String headerLine = input.readline().trim();
		if (!headerLine.toLowerCase().startsWith("tier")) {
			LogHandler.err_println("Totals file did not start with a valid header line.");
			LogHandler.print_debug_info("Separator Chars: %s, Line: \"%s\", Input Stream Handler: %s",
					SEPARATOR_REGEX.toString(), headerLine, input.toString());
			throw new IOException("Invalid input file");
		}

		String headers[] = SEPARATOR_REGEX.split(headerLine);

		if (headers.length < 5) {
			LogHandler.err_println("Header line \"" + headerLine + "\" did not contain at least five headers.");
			LogHandler.print_debug_info("Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
					SEPARATOR_REGEX.toString(), StringUtils.join(", ", headers), headerLine, input.toString());
			throw new IOException("Invalid input file");
		}

		Map<String, Map<String, Map<String, Long>>> turkeyTimes = new LinkedHashMap<String, Map<String, Map<String, Long>>>();
		Map<String, Map<String, Integer>> turkeyChanges = new LinkedHashMap<String, Map<String, Integer>>();
		Map<String, Set<String>> unreliableDays = new LinkedHashMap<String, Set<String>>();
		boolean last_failed = false;
		while (!input.done()) {
			String line = null;
			String tokens[] = null;
			String time = null;
			try {
				line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Skipped an empty line from input file.", true);
					LogHandler.print_debug_info("Input Stream Handler: %s, Line: \"%s\"", input.toString(), line);
					continue;
				}

				try {
					Set<Integer> times = new HashSet<Integer>();
					int numTokens = SEPARATOR_REGEX.split(line).length;
					for (int i = 2; i < numTokens; i++) {
						times.add(i);
					}
					tokens = splitLine(line, 4, times);
				} catch (IllegalArgumentException e) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain at least four tokens. Skipping line.");
					LogHandler.print_exception(e, "split input line",
							"Separator Chars: %s, Line: \"%s\", Input Stream Handler: %s", SEPARATOR_REGEX.toString(),
							line, input.toString());
					continue;
				}

				String turkey = tokens[0];
				String day = tokens[1];
				int zoneChanges = Integer.parseInt(tokens[2]);

				Matcher m = SEPARATOR_REGEX.matcher(line);
				int lastSeparator = 0;
				while (m.find()) {
					lastSeparator = m.end();
				}

				String unreliableToken = line.substring(lastSeparator).trim();

				if (!turkeyTimes.containsKey(turkey)) {
					turkeyTimes.put(turkey, new LinkedHashMap<String, Map<String, Long>>());
					turkeyChanges.put(turkey, new LinkedHashMap<String, Integer>());
					unreliableDays.put(turkey, new HashSet<String>());
				}

				Map<String, Map<String, Long>> dateTimes = turkeyTimes.get(turkey);

				if (dateTimes.containsKey(day)) {
					LogHandler.err_println("Found already parsed turkey date combo. Skipping line.");
					LogHandler.print_debug_info(
							"Turkey: %s, Date: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							turkey, day, SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line,
							input.toString());
					continue;
				}

				Map<String, Long> zoneTimes = new LinkedHashMap<String, Long>();
				for (int i = 3; i < (unreliableToken.isEmpty() ? tokens.length : tokens.length - 1); i++) {
					time = tokens[i];
					// "Zeit in Zone " = 13 chars.
					zoneTimes.put(headers[i].substring(13), TimeUtils.parseTime(time));
				}

				dateTimes.put(day, zoneTimes);

				turkeyChanges.get(turkey).put(day, zoneChanges);

				if (!unreliableToken.isEmpty() && unreliableToken.length() == 1
						&& Character.toLowerCase(unreliableToken.charAt(0)) == 'x') {
					unreliableDays.get(turkey).add(tokens[1]);
				} else if (!unreliableToken.isEmpty()) {
					LogHandler.err_println("Found invalid unreliable data value \"" + tokens[tokens.length - 1]
							+ "\". Treating it like a reliable day.");
					LogHandler.err_println(
							"Expected an 'X' to mark the day as unreliable, or nothing if it is reliable.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
				}

				last_failed = false;
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Returning output data set.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read an output file", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX.toString());

				if (last_failed) {
					break;
				} else {
					last_failed = true;
				}
			} catch (NumberFormatException e) {
				LogHandler.err_println("Parsing the zone changes number failed. Skipping line.");
				LogHandler.print_debug_info("Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
						SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
			} catch (IllegalArgumentException e) {
				LogHandler.err_println("Parsing time or date of line \"" + line + "\" failed. Skipping line.");
				LogHandler.print_exception(e, "parse record time of day or date",
						"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
						SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
			}
		}

		if (input instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println("Finished reading file " + ((FileInputStreamHandler) input).getInputFile().getPath(),
					true);
		}

		if (turkeyTimes.isEmpty() && turkeyChanges.isEmpty()) {
			LogHandler.err_println("Input file did not contain any valid data.");
			LogHandler.print_debug_info("Input Stream Handler: %s", input.toString());
			return null;
		} else {
			return new Pair<>(new Pair<>(turkeyTimes, turkeyChanges), unreliableDays);
		}
	}

	/**
	 * Returns the headers to be used for a stays csv.
	 * 
	 * @return The csv headers as a single string.
	 */
	public static String staysCsvHeader() {
		return StringUtils.join(DEFAULT_SEPARATOR, "Tier", "Bereich", "Startdatum", "Startzeit", "Enddatum", "Endzeit",
				"Aufenthaltszeit", "Unzuverlaessig");
	}

	/**
	 * Converts a {@link ZoneStay} to a csv line string.
	 * 
	 * @param stay The {@link ZoneStay} to convert to a csv line.
	 * @return The finished string.
	 * @throws NullPointerException     If {@code stay} is {@code null}.
	 * @throws IllegalArgumentException If the zone stay does not have an exit time
	 *                                  yet.
	 */
	public static String stayToCsvLine(ZoneStay stay) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(stay, "The zone stay to convert to a string can't be null.");
		if (!stay.hasLeft()) {
			throw new IllegalArgumentException("The zone to convert has no exit time.");
		}

		return StringUtils.join(DEFAULT_SEPARATOR, stay.getTurkey(), stay.getZone().getId(), stay.getEntryDate(),
				TimeUtils.encodeTime(stay.getEntryTime()), stay.getExitDate(), TimeUtils.encodeTime(stay.getExitTime()),
				TimeUtils.encodeTime(stay.getStayTime()), stay.isUnreliable() ? "X" : "");
	}

	/**
	 * Reads a stays csv generated by this program.<br/>
	 * Returns a {@code turkey -> stays} map with the parsed data.
	 * 
	 * @param input The input stream handler to read the file from.
	 * @param zones The possible zones for stays.
	 * @return The parsed stays map. Or {@code null} if the file did not contain any
	 *         valid data.
	 * @throws IOException          If reading the header line fails.
	 * @throws NullPointerException If {@code input} or {@code zones} is
	 *                              {@code null}.
	 */
	public static Map<String, List<ZoneStay>> readStaysCSV(IInputStreamHandler input, final Collection<ZoneInfo> zones)
			throws IOException, NullPointerException {
		Objects.requireNonNull(input, "The input to read cannot be null.");
		Objects.requireNonNull(zones, "The zones to use cannot be null.");
		try {
			checkInput(input);
		} catch (EOFException e) {
			LogHandler.err_println("Input file did not contain any data.");
			LogHandler.print_exception(e, "read a stays file", "Input Stream Handler: %s", input.toString());
			return null;
		}

		String headerLine = input.readline().trim();
		if (!headerLine.toLowerCase().startsWith("tier")) {
			LogHandler.err_println("Stays file did not start with a valid header line.");
			LogHandler.print_debug_info("Separator Chars: %s, Line: \"%s\", Input Stream Handler: %s",
					SEPARATOR_REGEX.toString(), headerLine, input.toString());
			throw new IOException("Invalid input file");
		}

		String headers[] = SEPARATOR_REGEX.split(headerLine);

		if (headers.length != 8) {
			LogHandler.err_println("Header line \"" + headerLine + "\" did not contain exactly seven headers.");
			LogHandler.print_debug_info("Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
					SEPARATOR_REGEX.toString(), StringUtils.join(", ", headers), headerLine, input.toString());
			throw new IOException("Invalid input file");
		}

		Map<String, ZoneInfo> idToZone = new HashMap<String, ZoneInfo>();
		for (ZoneInfo zone : zones) {
			idToZone.put(zone.getId(), zone);
		}

		Map<String, List<ZoneStay>> stays = new HashMap<String, List<ZoneStay>>();
		boolean last_failed = false;
		while (!input.done()) {
			String line = null;
			String tokens[] = null;
			try {
				line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Skipped an empty line from input file.", true);
					LogHandler.print_debug_info("Input Stream Handler: %s, Line: \"%s\"", input.toString(), line);
					continue;
				}

				try {
					tokens = splitLine(line, 7, Arrays.asList(3, 5, 6));
				} catch (IllegalArgumentException e) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain exactly seven tokens. Skipping line.");
					LogHandler.print_exception(e, "split input line",
							"Separator Chars: %s, Line: \"%s\", Input Stream Handler: %s", SEPARATOR_REGEX.toString(),
							line, input.toString());
				}

				if (tokens.length < 7 || tokens.length > 8) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain seven or eight tokens. Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				}

				String turkey = tokens[0];
				if (!stays.containsKey(turkey)) {
					stays.put(turkey, new ArrayList<ZoneStay>());
				}

				if (!idToZone.containsKey(tokens[1])) {
					LogHandler.err_println(
							"Input line \"" + line + "\" had unknown zone \"" + tokens[1] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Zones: [%s], Input Stream Handler: %s",
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line,
							StringUtils.collectionToString(", ", zones), input.toString());
					continue;
				}

				Calendar entryCal = TimeUtils.parseTime(tokens[2], tokens[3]);
				Calendar exitCal = TimeUtils.parseTime(tokens[4], tokens[5]);
				long fileStayTime = TimeUtils.parseTime(tokens[6]);

				ZoneStay stay = new ZoneStay(turkey, idToZone.get(tokens[1]), entryCal, exitCal);
				if (fileStayTime != stay.getStayTime()) {
					LogHandler.err_println("Input line \"" + line
							+ "\" + stay time did not match match entry and exit time. Skipping line.");
					LogHandler.print_debug_info(
							"File Stay Time: %s, Calculated Stay Time: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
							TimeUtils.encodeTime(fileStayTime), TimeUtils.encodeTime(stay.getStayTime()),
							SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					continue;
				}

				boolean unreliable = false;
				if (tokens.length == 8) {
					tokens[7] = tokens[7].trim();
					if (!tokens[7].isEmpty() && tokens[7].length() == 1
							&& Character.toLowerCase(tokens[7].charAt(0)) == 'x') {
						unreliable = true;
					} else if (!tokens[1].isEmpty()) {
						LogHandler.err_println("Found invalid unreliable data value \"" + tokens[7]
								+ "\". Treating it like a reliable stay.");
						LogHandler.err_println(
								"Expected an 'X' to mark the stay as unreliable, or nothing if it is reliable.");
						LogHandler.print_debug_info(
								"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
								SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
					}
				}

				// The actual last record is not known at this point.
				stay.setLastRecord(exitCal, false);
				if (unreliable) {
					stay.markUnreliable();
				}

				stays.get(turkey).add(stay);
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Returning output data set.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read an output file", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX.toString());

				if (last_failed) {
					break;
				} else {
					last_failed = true;
				}
			} catch (IllegalArgumentException e) {
				LogHandler.err_println("Parsing time of day or date in line \"" + line + "\" failed. Skipping line.");
				LogHandler.print_exception(e, "parse zone stay time of day or date",
						"Separator Chars: %s, Tokens: [%s], Line: \"%s\", Input Stream Handler: %s",
						SEPARATOR_REGEX.toString(), StringUtils.join(", ", tokens), line, input.toString());
			}
		}

		if (input instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println("Finished reading file " + ((FileInputStreamHandler) input).getInputFile().getPath(),
					true);
		}

		if (stays.isEmpty()) {
			LogHandler.err_println("Input file did not contain any valid data.");
			LogHandler.print_debug_info("Input Stream Handler: %s", input.toString());
			return null;
		} else {
			return stays;
		}
	}

}
