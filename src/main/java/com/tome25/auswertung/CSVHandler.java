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
import com.tome25.auswertung.stream.IOutputStreamHandler;
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
							.format("Input line \"%s\" did not contain at least two tokens. Skipping line.", line));
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
						LogHandler.err_println(
								String.format("Found duplicate id \"%s\". Ignoring this occurrence.", tokens[i]));
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
	 * Reads the next {@link AntennaRecord} from the given input.<br/>
	 * Handles skipping the header line and unparsable lines.
	 * 
	 * @param input The stream handler to read from.
	 * @return The newly created {@link AntennaRecord}. Or null if there was none.
	 * @throws NullPointerException If the input stream handler to read from is
	 *                              {@code null}.
	 */
	public static AntennaRecord readAntennaRecord(IInputStreamHandler input) throws NullPointerException {
		Objects.requireNonNull(input, "The input stream handler to read can't be null.");

		AntennaRecord result = null;
		boolean last_failed = false;
		main_loop: while (!input.done()) {
			String line = null;
			String tokens[] = null;
			try {
				line = input.readline();
				if (line == null || line.trim().isEmpty()) {
					LogHandler.err_println("Skipped an empty line from input file.", true);
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens), line);
					continue;
				}

				tokens = line.trim().split(SEPARATOR_REGEX);
				if (tokens.length != 4) {
					LogHandler.err_println(String
							.format("Input line \"%s\" did not contain exactly four tokens. Skipping line.", line));
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens), line);
					continue;
				}

				if (tokens[0].equalsIgnoreCase("transponder")) {
					LogHandler.out_println("Read header line: " + line, true);
					continue;
				}

				for (String token : tokens) {
					if (token.trim().isEmpty()) {
						LogHandler.err_println(
								String.format("Input line \"%s\" contained an empty token. Skipping line.", line));
						LogHandler.print_debug_info(
								"Input Stream Handler: %s, Spearator Chars: %s, Tokens: [%s], Line: \"%s\"",
								input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens), line);
						continue main_loop;
					}
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
			} catch (IllegalArgumentException e) {
				LogHandler.err_println(
						String.format("Parsing time of day or date of line \"%s\" failed. Skipping line.", line));
				LogHandler.print_exception(e, "parse record time", "Input Stream Handler: %s, Tokens: [%s]",
						input.toString(), StringUtils.join(", ", (Object[]) tokens));
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
	 * @throws NullPointerException If {@code zones} is {@code null}.
	 */
	public static String turkeyCsvHeader(Collection<String> zones) throws NullPointerException {
		Objects.requireNonNull(zones, "The list of zones for the header line cannot be null.");

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
			((ArrayList<String>) zones).sort(IntOrStringComparator.INSTANCE);
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
	 * @return The parsed totals date.
	 * @throws IOException          If reading the headers line fails.
	 * @throws NullPointerException If {@code input} is {@code null}.
	 */
	public static Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> readTotalsCSV(
			IInputStreamHandler input) throws IOException, NullPointerException {
		Objects.requireNonNull(input, "The input stream handler to read from cannot be null.");

		String headerLine = input.readline().trim();
		if (!headerLine.toLowerCase().startsWith("tier")) {
			LogHandler.err_println("Output file did not start with a valid header line.");
			LogHandler.print_debug_info("Input Stream Handler: %s, Separator Chars: %s, Line: \"%s\"", input.toString(),
					SEPARATOR_REGEX, headerLine);
			throw new IOException("Invalid input file");
		}

		String headers[] = headerLine.split(SEPARATOR_REGEX);

		if (headers.length < 4) {
			LogHandler.err_println(
					String.format("Header line \"%s\" did not contain at least four headers.", headerLine));
			LogHandler.print_debug_info("Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
					input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) headers), headerLine);
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
				if (line == null || line.isEmpty()) {
					LogHandler.err_println("Read an empty line from Input Stream Handler : " + input.toString(), true);
					continue;
				}

				tokens = line.trim().split(SEPARATOR_REGEX);
				if (tokens.length < 4) {
					LogHandler.err_println(String
							.format("Input line \"%s\" did not contain at least four tokens. Skipping line.", line));
					LogHandler.print_debug_info(
							"Input Stream Handler: %s, Separator Chars: %s, Tokens: [%s], Line: \"%s\"",
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens), line);
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
							input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens), line);
					continue;
				}

				Map<String, Long> zoneTimes = new LinkedHashMap<String, Long>();
				for (int i = 3; i < tokens.length; i++) {
					time = tokens[i];
					// "Aufenthalt in zone " = 19 chars.
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
						input.toString(), SEPARATOR_REGEX, StringUtils.join(", ", (Object[]) tokens), line);
			} catch (IllegalArgumentException e) {
				LogHandler
						.err_println(String.format("Parsing time of day of line \"%s\" failed. Skipping line.", line));
				LogHandler.print_exception(e, "parse record time of day",
						"Input Stream Handler: %s, Time String: \"%s\"", input.toString(), tokens[2]);
			}
		}

		return new Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>>(turkeyTimes,
				turkeyChanges);
	}

}
