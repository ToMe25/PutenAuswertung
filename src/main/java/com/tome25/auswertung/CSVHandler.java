package com.tome25.auswertung;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.tome25.auswertung.log.LogHandler;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.IInputStreamHandler;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.utils.IntOrStringComparator;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.StringUtils;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * This class is responsible for converting between internal data structures and
 * parts of a csv file.
 * 
 * @author theodor
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
	 * Ids can contain letters, digits, and spaces.
	 */
	private static final Pattern ID_REGEX = Pattern.compile("[A-Za-z0-9\\s]+");

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
	 * Reads the data from the stream handler as a CSV file and converts it to a two
	 * maps.<br/>
	 * The format of the first map is
	 * {@code column 1 -> list(column 2, column 3...)}.<br/>
	 * The second map format is {@code column X -> column 1} for each column except
	 * the first one.
	 * 
	 * @param input The stream handler containing the data to be read.
	 * @return A pair containing the two maps described above. Or {@code null} if
	 *         there was no valid data in the input.
	 * @throws NullPointerException if input is null.
	 */
	public static Pair<Map<String, List<String>>, Map<String, String>> readMappingCSV(IInputStreamHandler input)
			throws NullPointerException {
		try {
			checkInput(input);
		} catch (EOFException e) {
			LogHandler.err_println("Input file did not contain any data.");
			LogHandler.print_exception(e, "read a mappings file", "Input Stream Handler: %s", input);
			return null;
		}

		Map<String, List<String>> first = new LinkedHashMap<>();
		Map<String, String> second = new HashMap<>();
		boolean last_failed = false;
		while (!input.done()) {
			try {
				String line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Read an empty line from Input Stream Handler : " + input.toString(), true);
					LogHandler.print_debug_info("Input Stream Handler: %s, Line: \"%s\"", input.toString(), line);
					continue;
				}

				String tokens[] = SEPARATOR_REGEX.split(line.trim());
				if (tokens.length < 2) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain at least two tokens. Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				}

				if (tokens[0].equalsIgnoreCase("bereich") || tokens[0].equalsIgnoreCase("tier")) {
					LogHandler.out_println("Read header line \"" + line + "\".", true);
					continue;
				}

				if (first.containsKey(tokens[0])) {
					LogHandler.err_println("Found duplicate entity id \"" + tokens[0] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				} else if (!ID_REGEX.matcher(tokens[0]).matches()) {
					LogHandler.err_println("Found invalid entity id \"" + tokens[0] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				}

				List<String> list = new ArrayList<>();
				for (int i = 1; i < tokens.length; i++) {
					if (second.containsKey(tokens[i])) {
						LogHandler.err_println("Found duplicate id \"" + tokens[i] + "\". Ignoring this occurrence.");
						LogHandler.print_debug_info(
								"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
								input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					} else if (tokens[i].isEmpty()) {
						LogHandler.err_println("Found empty value in line \"" + line + "\". Skipping.", true);
						LogHandler.print_debug_info(
								"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
								input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					} else if (!ID_REGEX.matcher(tokens[i]).matches()) {
						LogHandler.err_println("Found invalid id \"" + tokens[i] + "\". Skipping.");
						LogHandler.print_debug_info(
								"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
								input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					} else {
						list.add(tokens[i]);
						second.put(tokens[i], tokens[0]);
					}
				}

				if (list.isEmpty()) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain at least one valid value. Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				}

				first.put(tokens[0], list);
				last_failed = false;
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Returning current mappings set.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read a mappings file", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX);

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

		if (first.isEmpty() && second.isEmpty()) {
			LogHandler.err_println("Input file did not contain any valid data.");
			LogHandler.print_debug_info("Input Stream Handler: %s", input);
			return null;
		} else {
			return new Pair<>(first, second);
		}
	}

	/**
	 * Writes the given list of {@link TurkeyInfo} objects to a turkeys input file.
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
		headers.append("Tier;");
		for (int i = 1; i <= numTransponders; i++) {
			headers.append("Transponder ");
			headers.append(i);
			if (i < numTransponders) {
				headers.append(DEFAULT_SEPARATOR);
			}
		}

		output.println(headers.toString());

		for (TurkeyInfo ti : turkeys) {
			StringBuilder sb = new StringBuilder();
			sb.append(ti.getId());

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
	 * Writes the given zones map to an input file.
	 * 
	 * @param zones  The zones to write to the file.
	 * @param output The output stream handler to write the zones to.
	 * @throws NullPointerException If one of the parameters if {@code null}.
	 */
	public static void writeZonesCSV(Map<String, List<String>> zones, IOutputStreamHandler output)
			throws NullPointerException {
		Objects.requireNonNull(zones, "The map of zones to write to the file cannot be null.");
		Objects.requireNonNull(output, "The output stream handler to write to cannot be null.");

		int numAntennas = 0;
		for (List<String> zone : zones.values()) {
			if (zone.size() > numAntennas) {
				numAntennas = zone.size();
			}
		}

		StringBuilder headers = new StringBuilder();
		headers.append("Bereich");
		for (int i = 1; i <= numAntennas; i++) {
			headers.append(DEFAULT_SEPARATOR);
			headers.append("Antenne ");
			headers.append(i);
		}

		output.println(headers.toString());

		for (String zone : zones.keySet()) {
			List<String> antennas = zones.get(zone);
			StringBuilder sb = new StringBuilder();
			sb.append(zone);

			for (String antenna : antennas) {
				sb.append(DEFAULT_SEPARATOR);
				sb.append(antenna);
			}

			for (int i = numAntennas - antennas.size(); i > 0; i--) {
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
			LogHandler.print_exception(e, "read a downtimes file", "Input Stream Handler: %s", input);
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
					LogHandler.err_println("Read an empty line from Input Stream Handler : " + input.toString(), true);
					LogHandler.print_debug_info("Input Stream Handler: %s, Line: \"%s\"", input.toString(), line);
					continue;
				}

				tokens = SEPARATOR_REGEX.split(line.trim());
				if (tokens.length != 4) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain exactly four tokens. Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
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
							"Start Date: %s, Start Time: %s, End Date: %s, End Time: %s, Input Stream Handler: %s, Line: \"%s\", Separator Chars: %s, Tokens: [%s]",
							TimeUtils.encodeDate(start), TimeUtils.encodeTime(TimeUtils.getMsOfDay(start)),
							TimeUtils.encodeDate(end), TimeUtils.encodeTime(TimeUtils.getMsOfDay(end)),
							input.toString(), line, SEPARATOR_REGEX, StringUtils.join(", ", tokens));
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
						input.toString(), SEPARATOR_REGEX);

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
						"Start Date: %s, Start Time: %s, Input Stream Handler: %s, Line: \"%s\", Separator Chars: %s, Tokens: [%s]",
						start == null ? "null" : TimeUtils.encodeDate(start),
						start == null ? "null" : TimeUtils.encodeTime(TimeUtils.getMsOfDay(start)), input.toString(),
						line, SEPARATOR_REGEX, StringUtils.join(", ", tokens));
			}
		}

		if (input instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println("Finished reading file " + ((FileInputStreamHandler) input).getInputFile().getPath(),
					true);
		}

		// Due to merging there can never be two pairs with the same key.
		Collections.sort(downtimes, new Comparator<Pair<Long, Long>>() {
			@Override
			public int compare(Pair<Long, Long> o1, Pair<Long, Long> o2) {
				return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, o1.getKey() - o2.getKey()));
			}
		});

		if (downtimes.isEmpty()) {
			LogHandler.err_println("Input file did not contain any valid data.");
			LogHandler.print_debug_info("Input Stream Handler: %s", input);
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

				tokens = SEPARATOR_REGEX.split(line.trim());
				if (tokens.length != 4) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain exactly four tokens. Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
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
									"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
									input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
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
								"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
								input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
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
								"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
								input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
						continue main_loop;
					}
				}

				if (!ID_REGEX.matcher(tokens[tokenOrder[0]]).matches()) {
					LogHandler.err_println("Input line \"" + line + "\" contains invalid transponder id \""
							+ tokens[tokenOrder[0]] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				} else if (!ID_REGEX.matcher(tokens[tokenOrder[3]]).matches()) {
					LogHandler.err_println("Input line \"" + line + "\" contains invalid antenna id \""
							+ tokens[tokenOrder[3]] + "\". Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				}

				result = new AntennaRecord(tokens[tokenOrder[0]], tokens[tokenOrder[1]], tokens[tokenOrder[2]],
						tokens[tokenOrder[3]]);
				break;
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Returning current mappings set.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read data input", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX);

				if (last_failed) {
					break;
				} else {
					last_failed = true;
				}
			} catch (IllegalArgumentException e) {
				LogHandler.err_println("Parsing time of day or date of line \"" + line + "\" failed. Skipping line.");
				LogHandler.print_exception(e, "parse record time",
						"Input Stream Handler: %s, Line: \"%s\", Separator Chars: %s, Tokens: [%s]", input.toString(),
						line, SEPARATOR_REGEX, StringUtils.join(", ", tokens));
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

		String result = StringUtils.join(DEFAULT_SEPARATOR, "Tier", "Datum", "Bereichswechsel");
		for (String zone : zones) {
			result += DEFAULT_SEPARATOR + "Aufenthalt in Zone " + zone;
		}

		return result;
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
			if (zoneTimes.containsKey(zone)) {
				Object time = zoneTimes.get(zone);
				if (time instanceof Long) {
					result.append(DEFAULT_SEPARATOR);
					result.append(TimeUtils.encodeTime((long) (Long) zoneTimes.get(zone)));
				} else if (time instanceof Integer) {
					result.append(DEFAULT_SEPARATOR);
					result.append(TimeUtils.encodeTime((int) (Integer) zoneTimes.get(zone)));
				}
			} else {
				result.append(DEFAULT_SEPARATOR);
				result.append(TimeUtils.encodeTime(0));
			}
		}

		return result.toString();
	}

	/**
	 * Reads a totals output csv generated by this program.<br/>
	 * The result contains two maps:<br/>
	 * The first map is {@code turkey -> date -> zone -> time}.<br/>
	 * The second map is {@code turkey -> date -> zoneChanges}.
	 * 
	 * @param input The input stream handler to read the data from.
	 * @return The parsed totals date. Or {@code null} if the file did not contain
	 *         any valid data.
	 * @throws IOException          If reading the headers line fails.
	 * @throws NullPointerException If {@code input} is {@code null}.
	 */
	public static Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> readTotalsCSV(
			IInputStreamHandler input) throws IOException, NullPointerException {
		try {
			checkInput(input);
		} catch (EOFException e) {
			LogHandler.err_println("Input file did not contain any data.");
			LogHandler.print_exception(e, "read a totals file", "Input Stream Handler: %s", input);
			return null;
		}

		String headerLine = input.readline().trim();
		if (!headerLine.toLowerCase().startsWith("tier")) {
			LogHandler.err_println("Totals file did not start with a valid header line.");
			LogHandler.print_debug_info("Input Stream Handler: %s, Separator Chars: %s, Line: \"%s\"", input.toString(),
					SEPARATOR_REGEX, headerLine);
			throw new IOException("Invalid input file");
		}

		String headers[] = SEPARATOR_REGEX.split(headerLine);

		if (headers.length < 4) {
			LogHandler.err_println("Header line \"" + headerLine + "\" did not contain at least four headers.");
			LogHandler.print_debug_info("Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
					input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", headers), headerLine);
			throw new IOException("Invalid input file");
		}

		Map<String, Map<String, Map<String, Long>>> turkeyTimes = new LinkedHashMap<String, Map<String, Map<String, Long>>>();
		Map<String, Map<String, Integer>> turkeyChanges = new LinkedHashMap<String, Map<String, Integer>>();
		boolean last_failed = false;
		while (!input.done()) {
			String line = null;
			String tokens[] = null;
			String time = null;
			try {
				line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Read an empty line from Input Stream Handler : " + input.toString(), true);
					continue;
				}

				tokens = SEPARATOR_REGEX.split(line.trim());
				if (tokens.length < 4) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain at least four tokens. Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				}

				String turkey = tokens[0];
				String day = tokens[1];
				int zoneChanges = Integer.parseInt(tokens[2]);

				if (!turkeyTimes.containsKey(turkey)) {
					turkeyTimes.put(turkey, new LinkedHashMap<String, Map<String, Long>>());
				}

				Map<String, Map<String, Long>> dateTimes = turkeyTimes.get(turkey);

				if (dateTimes.containsKey(day)) {
					LogHandler.err_println("Found already parsed turkey date combo. Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				}

				Map<String, Long> zoneTimes = new LinkedHashMap<String, Long>();
				for (int i = 3; i < tokens.length; i++) {
					time = tokens[i];
					// "Aufenthalt in Zone " = 19 chars.
					zoneTimes.put(headers[i].substring(19), TimeUtils.parseTime(time));
				}

				dateTimes.put(day, zoneTimes);

				if (!turkeyChanges.containsKey(turkey)) {
					turkeyChanges.put(turkey, new LinkedHashMap<String, Integer>());
				}

				turkeyChanges.get(turkey).put(day, zoneChanges);

				last_failed = false;
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Returning output data set.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read an output file", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX);

				if (last_failed) {
					break;
				} else {
					last_failed = true;
				}
			} catch (NumberFormatException e) {
				LogHandler.err_println("Parsing the zone changes number failed. Skipping line.");
				LogHandler.print_debug_info("Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
						input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
			} catch (IllegalArgumentException e) {
				LogHandler.err_println("Parsing time of day or date of line \"" + line + "\" failed. Skipping line.");
				LogHandler.print_exception(e, "parse record time of day or date",
						"Input Stream Handler: %s, Tokens: [%s], Line: \"%s\"", input.toString(),
						StringUtils.join(", ", tokens), line);
			}
		}

		if (input instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println("Finished reading file " + ((FileInputStreamHandler) input).getInputFile().getPath(),
					true);
		}

		if (turkeyTimes.isEmpty() && turkeyChanges.isEmpty()) {
			LogHandler.err_println("Input file did not contain any valid data.");
			LogHandler.print_debug_info("Input Stream Handler: %s", input);
			return null;
		} else {
			return new Pair<>(turkeyTimes, turkeyChanges);
		}
	}

	/**
	 * Returns the headers to be used for a stays csv.
	 * 
	 * @return The csv headers as a single string.
	 */
	public static String staysCsvHeader() {
		return StringUtils.join(DEFAULT_SEPARATOR, "Tier", "Bereich", "Startdatum", "Startzeit", "Enddatum", "Endzeit",
				"Aufenthaltszeit");
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

		return StringUtils.join(DEFAULT_SEPARATOR, stay.getTurkey(), stay.getZone(), stay.getEntryDate(),
				TimeUtils.encodeTime(stay.getEntryTime()), stay.getExitDate(), TimeUtils.encodeTime(stay.getExitTime()),
				TimeUtils.encodeTime(stay.getStayTime()));
	}

	/**
	 * Reads a stays csv generated by this program.<br/>
	 * Returns a {@code turkey -> stays} map with the parsed data.
	 * 
	 * @param input The input stream handler to read the file from.
	 * @return The parsed stays map. Or {@code null} if the file did not contain any
	 *         valid data.
	 * @throws IOException          If reading the header line fails.
	 * @throws NullPointerException If {@code input} is {@code null}.
	 */
	public static Map<String, List<ZoneStay>> readStaysCSV(IInputStreamHandler input)
			throws IOException, NullPointerException {
		try {
			checkInput(input);
		} catch (EOFException e) {
			LogHandler.err_println("Input file did not contain any data.");
			LogHandler.print_exception(e, "read a stays file", "Input Stream Handler: %s", input);
			return null;
		}

		String headerLine = input.readline().trim();
		if (!headerLine.toLowerCase().startsWith("tier")) {
			LogHandler.err_println("Stays file did not start with a valid header line.");
			LogHandler.print_debug_info("Input Stream Handler: %s, Separator Chars: %s, Line: \"%s\"", input.toString(),
					SEPARATOR_REGEX, headerLine);
		}

		Map<String, List<ZoneStay>> stays = new HashMap<String, List<ZoneStay>>();
		boolean last_failed = false;
		while (!input.done()) {
			String line = null;
			String tokens[] = null;
			try {
				line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Read an empty line from Input Stream Handler : " + input.toString(), true);
					continue;
				}

				tokens = SEPARATOR_REGEX.split(line.trim());
				if (tokens.length != 7) {
					LogHandler.err_println(
							"Input line \"" + line + "\" did not contain exactly seven tokens. Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				}

				String turkey = tokens[0];
				if (!stays.containsKey(turkey)) {
					stays.put(turkey, new ArrayList<ZoneStay>());
				}

				Calendar entryCal = TimeUtils.parseTime(tokens[2], tokens[3]);
				Calendar exitCal = TimeUtils.parseTime(tokens[4], tokens[5]);
				long fileStayTime = TimeUtils.parseTime(tokens[6]);
				long calcStayTime = exitCal.getTimeInMillis() - entryCal.getTimeInMillis();

				if (fileStayTime != calcStayTime) {
					LogHandler.err_println("Stay time did not match match entry and exit time. Skipping line.");
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, File Stay Time: %s, Calculated Stay Time: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), TimeUtils.encodeTime(fileStayTime), TimeUtils.encodeTime(calcStayTime),
							SEPARATOR_REGEX, StringUtils.join(", ", tokens), line);
					continue;
				}

				stays.get(turkey).add(new ZoneStay(turkey, tokens[1], entryCal, exitCal));
			} catch (IOException e) {
				if (last_failed) {
					LogHandler.err_println("Reading an input line failed. Returning output data set.");
				} else {
					LogHandler.err_println("Reading an input line failed. Trying another one.");
				}
				LogHandler.print_exception(e, "read an output file", "Input Stream Handler: %s, Separator Chars: %s",
						input.toString(), SEPARATOR_REGEX);

				if (last_failed) {
					break;
				} else {
					last_failed = true;
				}
			} catch (IllegalArgumentException e) {
				LogHandler.err_println("Parsing time of day or date in line \"" + line + "\" failed. Skipping line.");
				LogHandler.print_exception(e, "parse zone stay time of day or date",
						"Input Stream Handler: %s, Tokens: [%s], Line: \"%s\"", input.toString(),
						StringUtils.join(", ", tokens), line);
			}
		}

		if (input instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println("Finished reading file " + ((FileInputStreamHandler) input).getInputFile().getPath(),
					true);
		}

		if (stays.isEmpty()) {
			LogHandler.err_println("Input file did not contain any valid data.");
			LogHandler.print_debug_info("Input Stream Handler: %s", input);
			return null;
		} else {
			return stays;
		}
	}

}
