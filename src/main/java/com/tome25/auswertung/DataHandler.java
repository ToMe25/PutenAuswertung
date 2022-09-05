package com.tome25.auswertung;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.tome25.auswertung.stream.IInputStreamHandler;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * The class handling the main data analysis/conversion.
 * 
 * @author theodor
 */
public class DataHandler {

	/**
	 * Does all the primary data handling.<br/>
	 * Reads the data from the given streams, and generates output based on it.<br/>
	 * Calculates all the expected data.
	 * 
	 * @param antennaStream The stream handler to read the antenna records from.
	 * @param turkeyStream  The stream handler to read
	 *                      {@code turkey id -> transponder ids} mappings from.
	 * @param zonesStream   The stream handler to read zone definitions from.
	 * @param totalsStream  The output stream handler to write the daily total times
	 *                      per zone and zone changes to.
	 * @param staysStream   The output stream handler to write the individual zone
	 *                      stays to.
	 * @param fillDays      Whether the beginning and end of days should be filled
	 *                      with the same value as the closest record for each
	 *                      turkey.
	 * @throws NullPointerException If one of the stream handlers is {@code null}.
	 */
	public static void handleStreams(IInputStreamHandler antennaStream, IInputStreamHandler turkeyStream,
			IInputStreamHandler zonesStream, IOutputStreamHandler totalsStream, IOutputStreamHandler staysStream,
			boolean fillDays) throws NullPointerException {
		Objects.requireNonNull(antennaStream, "The stream handler to read antenna data from can't be null.");
		Objects.requireNonNull(turkeyStream, "The stream handler to read turkey mappings from can't be null.");
		Objects.requireNonNull(zonesStream, "The stream handler to read zone mappings from can't be null.");
		Objects.requireNonNull(totalsStream, "The stream handler to write totals to can't be null.");
		Objects.requireNonNull(staysStream, "The stream handler to write stays to can't be null.");

		Pair<Map<String, List<String>>, Map<String, String>> zones = CSVHandler.readMappingCSV(zonesStream);
		Pair<Map<String, List<String>>, Map<String, String>> turkeys = CSVHandler.readMappingCSV(turkeyStream);

		Map<String, TurkeyInfo> turkeyInfos = new TreeMap<>(IntOrStringComparator.INSTANCE);
		String lastDate = null;
		Map<String, Calendar> lastTimes = new HashMap<String, Calendar>();

		totalsStream.println(CSVHandler.turkeyCsvHeader(zones.getKey().keySet()));
		staysStream.println(CSVHandler.staysCsvHeader());

		List<String> dates = new ArrayList<String>();

		Calendar startTime = null;
		while (!antennaStream.done()) {
			AntennaRecord record = CSVHandler.readAntennaRecord(antennaStream);
			String turkeyId = record.transponder;
			if (turkeys.getValue().containsKey(record.transponder)) {
				turkeyId = turkeys.getValue().get(record.transponder);
			} else {
				LogHandler.err_println(String.format(
						"Received antenna record for unknown transponder id \"%s\" on day %s. Considering it a separate turkey.",
						record.date, record.transponder));
				LogHandler.print_debug_info("Antenna Record: %s, fillDays: %s", record, fillDays ? "true" : "false");
			}

			if (!zones.getValue().containsKey(record.antenna)) {
				LogHandler.err_println(String.format(
						"Received antenna record from unknown antenna id \"%s\". Skipping line.", record.antenna));
				LogHandler.print_debug_info("Antenna Record: %s, fillDays: %s", record, fillDays ? "true" : "false");
			}

			if (startTime == null) {
				startTime = record.cal;
			}

			if (!record.date.equals(lastDate)) {
				if (totalsStream.printsTemporary() && lastDate != null) {
					printDayOutput(totalsStream, turkeyInfos.values(), lastDate, zones.getKey().keySet(), false);
				}

				if (!fillDays && lastDate != null && !TimeUtils.isNextDay(lastDate, record.date)) {
					startTime = record.cal;
					for (TurkeyInfo ti : turkeyInfos.values()) {
						ti.changeZone(ti.getCurrentZone(), lastTimes.get(lastDate));
						ti.setStartTime(startTime);
					}
				}

				lastDate = record.date;
				dates.add(lastDate);
			}

			if (!lastTimes.containsKey(record.date) || record.cal.after(lastTimes.get(record.date))) {
				lastTimes.put(record.date, record.cal);
			} else if (!record.cal.equals(lastTimes.get(record.date))) {
				LogHandler.err_println("New antenna record is before the last one. Skipping line.");
				LogHandler.print_debug_info(
						"antenna record: %s, fillDays: %s, new time: %s, new date: %s, current time: %s, current date: %s",
						record, fillDays ? "true" : "false", record.time, record.date,
						TimeUtils.encodeTime(TimeUtils.getMsOfDay(lastTimes.get(record.date))), lastDate);
				continue;
			}

			if (!turkeyInfos.containsKey(turkeyId)) {
				turkeyInfos.put(turkeyId, new TurkeyInfo(turkeyId, turkeys.getKey().get(turkeyId), staysStream,
						zones.getValue().get(record.antenna), record.cal, fillDays ? null : startTime));
			} else {
				try {
					turkeyInfos.get(turkeyId).changeZone(zones.getValue().get(record.antenna), record.cal);
				} catch (IllegalArgumentException e) {
					LogHandler.err_println(
							"New antenna record is before the last one for the same turkey. Skipping line.");
					LogHandler.print_exception(e, "update turkey zone",
							"Antenna Record: %s, fillDays: %s, new time: %s, new date: %s, current time: %s, current date: %s",
							record, fillDays ? "true" : "false", record.time, record.date,
							TimeUtils.encodeTime(turkeyInfos.get(turkeyId).getCurrentTime()),
							turkeyInfos.get(turkeyId).getCurrentDate());
				}
			}
		}

		for (TurkeyInfo ti : turkeyInfos.values()) {
			if (!fillDays) {
				ti.changeZone(ti.getCurrentZone(), lastTimes.get(ti.getCurrentDate()));
			}
			ti.endDay(ti.getCurrentDate());
			ti.printCurrentStay(false);
		}

		for (String date : dates) {
			printDayOutput(totalsStream, turkeyInfos.values(), date, zones.getKey().keySet(), true);
		}
		printDayOutput(totalsStream, turkeyInfos.values(), null, zones.getKey().keySet(), true);

		try {
			totalsStream.close();
			staysStream.close();
		} catch (IOException e) {
			LogHandler.err_println("An exception occurred while closing an output stream handler.", true);
			LogHandler.print_exception(e, "close output stream handler",
					"Totals stream handler: %s, Stays stream handler: %s", totalsStream, staysStream);
		}
	}

	/**
	 * Generates the CSV output for all turkeys that have antenna records on the
	 * given date.<br/>
	 * Prints a line for each turkey that has been updated on the given day.<br/>
	 * Or all turkeys of used for total output.
	 * 
	 * @param output   The {@link IOutputStreamHandler} to write the generated data
	 *                 to.
	 * @param turkeys  A collection of all the turkeys that are known.
	 * @param date     The date for which to generate output. Set to {@code null} to
	 *                 produce total output.
	 * @param zones    A collection containing the names of all the zones to write.
	 * @param finished If {@code true} all data is handled as non temporary.
	 * @return The newly generated output.
	 */
	private static void printDayOutput(IOutputStreamHandler output, Collection<TurkeyInfo> turkeys, String date,
			Collection<String> zones, boolean finished) {
		boolean total = date == null;
		for (TurkeyInfo ti : turkeys) {
			if (total || ti.hasDay(date)) {
				if (finished) {
					output.println(CSVHandler.turkeyToCsvLine(ti, date, zones));
				} else {
					output.printDay(ti, date, zones);
				}
			}
		}
	}

}
