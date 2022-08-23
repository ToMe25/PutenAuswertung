package com.tome25.auswertung;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.tome25.auswertung.stream.IInputStreamHandler;
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
	 * A regex string containing all the characters to split csv files at.
	 */
	private static final String SEPARATOR_REGEX = "[,;\t]";

	/**
	 * The default character to use as a value separator in produced csvs.
	 */
	public static final char DEFAULT_SEPARATOR = ';';

	/**
	 * Reads the data from the stream handler as a CSV file and converts it to a two
	 * maps.<br/>
	 * The format of the first map is
	 * {@code column 1 -> list(column 2, column 3...)}.<br/>
	 * The second map format is {@code column X -> column 1} for each column except
	 * the first one.
	 * 
	 * @param input The stream handler containing the data to be read.
	 * @return A pair containing the two maps described above.
	 * @throws NullPointerException if input is null.
	 */
	public static Pair<Map<String, List<String>>, Map<String, String>> readMappingCSV(IInputStreamHandler input)
			throws NullPointerException {
		Objects.requireNonNull(input, "input cannot be null.");

		Map<String, List<String>> first = new LinkedHashMap<>();
		Map<String, String> second = new HashMap<>();
		boolean last_failed = false;
		while (!input.done()) {
			try {
				String line = input.readline();
				if (line == null || line.isEmpty()) {
					LogHandler.err_println("Read an empty line from Input Stream Handler : " + input.toString(), true);
					continue;
				}

				String tokens[] = line.trim().split(SEPARATOR_REGEX);
				if (tokens.length < 2) {
					LogHandler.err_println(String
							.format("Input String \"%s\" did not contain at least two tokens. Skipping line.", line));
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, tokens: [%s], line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens), line);
					continue;
				}

				if (tokens[0].equalsIgnoreCase("bereich") || tokens[0].equalsIgnoreCase("tier")) {
					LogHandler.out_println("Read header line: " + line, true);
					continue;
				}

				if (first.containsKey(tokens[0])) {
					LogHandler
							.err_println(String.format("Found duplicate entity id \"%s\". Skipping line.", tokens[0]));
					LogHandler.print_debug_info("Input Stream Handler: %s, Separator Chars: %s, tokens: [%s]",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens));
					continue;
				}

				List<String> list = new ArrayList<>();
				for (int i = 1; i < tokens.length; i++) {
					if (second.containsKey(tokens[i])) {
						LogHandler.err_println(String.format(
								"Found duplicate id \"%s\". Ignoring this occurrence.",
								tokens[i]));
						LogHandler.print_debug_info("Input Stream Handler: %s, Separator Chars: %s, tokens: [%s]",
								input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens));
					} else {
						list.add(tokens[i]);
						second.put(tokens[i], tokens[0]);
					}
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

		return new Pair<>(first, second);
	}

	/**
	 * Reads the next {@link AntennaRecord} from the given input.<br/>
	 * Handles skipping the header line and unparsable lines.
	 * 
	 * @param input The stream handler to read from.
	 * @return The newly created {@link AntennaRecord}. Or null if there was none.
	 */
	public static AntennaRecord readAntennaRecord(IInputStreamHandler input) {
		AntennaRecord result = null;
		boolean last_failed = false;
		while (!input.done()) {
			String tokens[] = null;
			try {
				String line = input.readline();
				if (line == null || line.isEmpty()) {
					LogHandler.err_println("Read an empty line from Input Stream Handler: " + input.toString(), true);
					continue;
				}

				tokens = line.trim().split(SEPARATOR_REGEX);
				if (tokens.length != 4) {
					LogHandler.err_println(String
							.format("Input String \"%s\" did not contain exactly four tokens. Skipping line.", line));
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, tokens: [%s], line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens), line);
					continue;
				}

				if (tokens[0].equalsIgnoreCase("transponder")) {
					LogHandler.out_println("Read header line: " + line, true);
					continue;
				}

				result = new AntennaRecord(tokens[0], tokens[1], tokens[2], tokens[3]);
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
			} catch (NumberFormatException e) {
				LogHandler.err_println("Parsing time of day of antenna record failed. Skipping line.");
				LogHandler.print_exception(e, "parse record time of day",
						"Input Stream Handler: %s, Time String: \"%s\"", input.toString(), tokens[2]);
			}
		}

		if (result == null) {
			LogHandler.err_println("Failed to read an antenna record from the input file.", true);
		}

		return result;
	}

	/**
	 * Returns the headers to be used for a csv with the given zones.
	 * 
	 * @param zones The zones that can be found in the input data.
	 * @return The csv headers as a single string.
	 */
	public static String turkeyCsvHeader(Collection<String> zones) {
		String result = StringUtils.join(DEFAULT_SEPARATOR, "Tier", "Datum", "Bereichswechsel");
		for (String zone : zones) {
			result += DEFAULT_SEPARATOR + "Aufenthalt in zone " + zone;
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
	 *               If this is {@code null} or empty the potentially incomplete or
	 *               unordered zone list from the turkey is used instead.
	 * @return The newly generated output line.
	 */
	public static String turkeyToCsvLine(TurkeyInfo turkey, String date, Collection<String> zones) {
		Map<String, ?> zoneTimes = date == null ? turkey.getTotalZoneTimes() : turkey.getDayZoneTimes(date);

		if (zones == null || zones.isEmpty()) {
			zones = zoneTimes.keySet();
		}

		String result = null;
		if (date != null) {
			result = StringUtils.join(DEFAULT_SEPARATOR, turkey.getId(), date,
					Integer.toString(turkey.getDayZoneChanges(date)));
		} else {
			result = StringUtils.join(DEFAULT_SEPARATOR, turkey.getId(), "total",
					Integer.toString(turkey.getTotalZoneChanges()));
		}

		for (String zone : zones) {
			if (zoneTimes.containsKey(zone)) {
				Object time = zoneTimes.get(zone);
				if (time instanceof Long) {
					result += DEFAULT_SEPARATOR + TimeUtils.encodeTime((long) (Long) zoneTimes.get(zone));
				} else if (time instanceof Integer) {
					result += DEFAULT_SEPARATOR + TimeUtils.encodeTime((int) (Integer) zoneTimes.get(zone));
				}
			} else {
				result += DEFAULT_SEPARATOR + TimeUtils.encodeTime(0);
			}
		}

		return result;
	}

}
