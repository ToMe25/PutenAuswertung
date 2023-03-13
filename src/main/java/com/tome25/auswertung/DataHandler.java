package com.tome25.auswertung;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.log.LogHandler;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.IInputStreamHandler;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.utils.IntOrStringComparator;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.StringUtils;
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
	 * @param antennaStream   The stream handler to read the antenna records from.
	 * @param turkeyStream    The stream handler to read
	 *                        {@code turkey id -> transponder ids} mappings from.
	 * @param zonesStream     The stream handler to read zone definitions from.
	 * @param downtimesStream The stream handler to read the downtimes from. Can be
	 *                        {@code null}.
	 * @param totalsStream    The output stream handler to write the daily total
	 *                        times per zone and zone changes to.
	 * @param staysStream     The output stream handler to write the individual zone
	 *                        stays to.
	 * @param args            The arguments to be used for this data analysis.
	 * @throws NullPointerException If {@code antennaStream}, {@code turkeyStream},
	 *                              {@code zonesStream}, {@code totalsStream},
	 *                              {@code staysStream}, or {@code args} is
	 *                              {@code null}.
	 */
	public static void handleStreams(IInputStreamHandler antennaStream, IInputStreamHandler turkeyStream,
			IInputStreamHandler zonesStream, IInputStreamHandler downtimesStream, IOutputStreamHandler totalsStream,
			IOutputStreamHandler staysStream, Arguments args) throws NullPointerException {
		Objects.requireNonNull(antennaStream, "The stream handler to read antenna data from can't be null.");
		Objects.requireNonNull(turkeyStream, "The stream handler to read turkey mappings from can't be null.");
		Objects.requireNonNull(zonesStream, "The stream handler to read zone mappings from can't be null.");
		Objects.requireNonNull(totalsStream, "The stream handler to write totals to can't be null.");
		Objects.requireNonNull(staysStream, "The stream handler to write stays to can't be null.");
		Objects.requireNonNull(args, "The arguments to use cannot be null.");

		Pair<Map<String, List<String>>, Map<String, String>> zones = CSVHandler.readMappingCSV(zonesStream);
		if (zones == null) {
			LogHandler.err_println("Failed to read zone mappings from the input file.");
			LogHandler.print_debug_info("Zones Input Stream Handler: %s", zonesStream);
		}

		Pair<Map<String, List<String>>, Map<String, String>> turkeys = CSVHandler.readMappingCSV(turkeyStream);
		if (turkeys == null) {
			LogHandler.err_println("Failed to read turkey mappings from the input file.");
			LogHandler.print_debug_info("Turkey Input Stream Handler: %s", turkeyStream);
		}

		if (zones == null || turkeys == null) {
			return;
		}

		List<Pair<Long, Long>> downtimes = null;
		if (downtimesStream != null) {
			downtimes = CSVHandler.readDowntimesCSV(downtimesStream);
		}

		totalsStream.println(CSVHandler.turkeyCsvHeader(zones.getKey().keySet()));
		staysStream.println(CSVHandler.staysCsvHeader());

		if (antennaStream instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println(
					"Started reading file " + ((FileInputStreamHandler) antennaStream).getInputFile().getPath(), true);
		}

		Map<String, TurkeyInfo> turkeyInfos = new TreeMap<>(IntOrStringComparator.INSTANCE);
		String lastDate = null;
		Map<String, Calendar> lastTimes = new HashMap<String, Calendar>();
		List<String> dates = new ArrayList<String>();
		short[] tokenOrder = new short[] { 0, 1, 2, 3 };
		Calendar startTime = null;

		read_loop: while (!antennaStream.done()) {
			AntennaRecord record = CSVHandler.readAntennaRecord(antennaStream, tokenOrder);
			if (record == null) {
				LogHandler.err_println("Reading an antenna record from the input file failed.", true);
				LogHandler.print_debug_info("Antenna Input Stream Handler: %s", antennaStream);
				continue;
			}

			String turkeyId = record.transponder;
			if (turkeys.getValue().containsKey(record.transponder)) {
				turkeyId = turkeys.getValue().get(record.transponder);
			} else {
				LogHandler.err_println(String.format(
						"Received antenna record for unknown transponder id \"%s\" on day %s at %s. Considering it a separate turkey.",
						record.transponder, record.date, TimeUtils.encodeTime(record.tod)));
				LogHandler.print_debug_info("Antenna Record: %s, Arguments: %s", record, args);
			}

			if (!zones.getValue().containsKey(record.antenna)) {
				LogHandler.err_println(String.format(
						"Received antenna record from unknown antenna id \"%s\" on day %s at %s. Skipping line.",
						record.antenna, record.date, TimeUtils.encodeTime(record.tod)));
				LogHandler.print_debug_info("Antenna Record: %s, Arguments: %s", record, args);
				continue;
			}

			long recordMs = record.cal.getTimeInMillis();
			Calendar downtimeStart = null;
			Calendar downtimeEnd = null;
			if (downtimes != null) {
				for (Pair<Long, Long> downtime : downtimes) {
					if (recordMs > downtime.getValue()) {
						// Last record was before the downtime, current one is after.
						if (lastDate != null && lastTimes.get(lastDate).getTimeInMillis() < downtime.getKey()) {
							downtimeStart = new GregorianCalendar();
							downtimeStart.setTimeInMillis(downtime.getKey());
							downtimeEnd = new GregorianCalendar();
							downtimeEnd.setTimeInMillis(downtime.getValue());
						}
						continue;
					}

					if (recordMs > downtime.getKey()) {
						downtimeStart = new GregorianCalendar();
						downtimeStart.setTimeInMillis(downtime.getKey());
						downtimeEnd = new GregorianCalendar();
						downtimeEnd.setTimeInMillis(downtime.getValue());
						LogHandler.err_println(String.format(
								"Received antenna record for time %s %s, which is during the downtime from %s %s to %s %s. Skipping record.",
								record.date, TimeUtils.encodeTime(record.tod), TimeUtils.encodeDate(downtimeStart),
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeStart)),
								TimeUtils.encodeDate(downtimeEnd),
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeEnd))));
						LogHandler.print_debug_info(
								"Antenna Record: %s, Downtime Start Date: %s, Downtime Start Time: %s, Downtime End Date: %s, Downtime End Time: %s, Arguments: %s",
								record, TimeUtils.encodeDate(downtimeStart),
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeStart)),
								TimeUtils.encodeDate(downtimeEnd),
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeEnd)), args);
						continue read_loop;
					} else {
						break;
					}
				}
			}

			// Check if there were missing days, indicating an unrecorded downtime.
			if (downtimeStart == null && downtimeEnd == null && lastDate != null && !record.date.equals(lastDate)
					&& !TimeUtils.isNextDay(lastDate, record.date)) {
				downtimeStart = lastTimes.get(lastDate);
				downtimeEnd = record.cal;
				LogHandler.out_println(String.format("Skipping days from %s %s to %s %s because there are no records.",
						TimeUtils.encodeDate(downtimeStart), TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeStart)),
						record.date, TimeUtils.encodeTime(record.tod)), true);
				LogHandler.print_debug_info("Antenna Record: %s, Last Date: %s, Last Time: %s, Arguments: %s", record,
						TimeUtils.encodeDate(downtimeStart), TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeStart)),
						args);
			}

			if (startTime == null) {
				startTime = record.cal;
			}

			if (downtimeStart != null && downtimeEnd != null) {
				if (TimeUtils.isSameDay(downtimeStart, downtimeEnd)) {
					if (!args.fillDays) {
						for (TurkeyInfo ti : turkeyInfos.values()) {
							if (ti.getCurrentCal().after(startTime) || ti.getCurrentCal().equals(startTime)) {
								// FIXME what if lastDate isn't the same day as downtimeStart?
								ti.changeZone(ti.getCurrentZone(), downtimeStart);
								ti.printCurrentStay(false);
							}
							ti.setStartTime(downtimeEnd);
						}
					}
				} else {
					if (!args.fillDays) {
						for (TurkeyInfo ti : turkeyInfos.values()) {
							if (ti.getCurrentCal().after(startTime) || ti.getCurrentCal().equals(startTime)) {
								// FIXME what if lastDate isn't the same day as downtimeStart?
								ti.changeZone(ti.getCurrentZone(), downtimeStart);
								ti.endDay(lastDate);
								ti.printCurrentStay(false);
							}
							ti.setStartTime(downtimeEnd);
						}
					} else {
						for (TurkeyInfo ti : turkeyInfos.values()) {
							ti.endDay(ti.getCurrentDate());
							ti.printCurrentStay(false);
						}
					}

					startTime = downtimeEnd;

					for (String date : dates) {
						printDayOutput(totalsStream, turkeyInfos.values(), date, zones.getKey().keySet(), true);
					}
					dates.clear();

					dates.add(record.date);
				}
			}

			if (record.cal.before(lastTimes.get(record.date))) {
				LogHandler.err_println("New antenna record is before the last one. Skipping line.");
				LogHandler.print_debug_info(
						"Antenna Record: %s, New Time of Day: %s, New Date: %s, Current Time of Day: %s, Current Date: %s, Arguments: %s",
						record, record.time, record.date,
						TimeUtils.encodeTime(TimeUtils.getMsOfDay(lastTimes.get(record.date))), lastDate, args);
				continue;
			}

			if (!record.date.equals(lastDate)) {
				if (totalsStream.printsTemporary() && lastDate != null) {
					printDayOutput(totalsStream, turkeyInfos.values(), lastDate, zones.getKey().keySet(), false);
				}

				lastDate = record.date;
				if (!dates.contains(lastDate)) {
					dates.add(lastDate);
				}
				lastTimes.put(record.date, record.cal);
			} else if (record.cal.after(lastTimes.get(record.date))) {
				lastTimes.put(record.date, record.cal);
			}

			if (!turkeyInfos.containsKey(turkeyId)) {
				try {
					turkeyInfos.put(turkeyId, new TurkeyInfo(turkeyId, turkeys.getKey().get(turkeyId), staysStream,
							zones.getValue().get(record.antenna), record.cal, args.fillDays ? null : startTime, args));
				} catch (NullPointerException e) {
					LogHandler.err_println("Creating a new TurkeyInfo object failed. Terminating.");
					LogHandler.print_exception(e, "create a new TurkeyInfo",
							"Turkey id: %s, Transponders: [%s], Stays Stream Handler: %s, Initial Zone: %s, Initial Date: %s, Initial Time: %s, Start Date: %s, Start Time %s, Arguments: %s",
							turkeyId, StringUtils.collectionToString(", ", turkeys.getKey().get(turkeyId)), staysStream,
							zones.getValue().get(record.antenna), TimeUtils.encodeDate(record.cal),
							TimeUtils.encodeTime(TimeUtils.getMsOfDay(record.cal)),
							startTime == null ? "null" : TimeUtils.encodeDate(startTime),
							startTime == null ? "null" : TimeUtils.encodeTime(TimeUtils.getMsOfDay(startTime)), args);
					break;
				}
			} else {
				try {
					turkeyInfos.get(turkeyId).changeZone(zones.getValue().get(record.antenna), record.cal);
				} catch (IllegalArgumentException e) {
					LogHandler.err_println(
							"New antenna record is before the last one for the same turkey. Skipping line.");
					LogHandler.print_exception(e, "update turkey zone",
							"Antenna Record: %s, New Time of Day: %s, New Date: %s, Current Time of Day: %s, Current Date: %s, Arguments: %s",
							record, record.time, record.date,
							TimeUtils.encodeTime(turkeyInfos.get(turkeyId).getCurrentTime()),
							turkeyInfos.get(turkeyId).getCurrentDate(), args);
				}
			}
		}

		for (TurkeyInfo ti : turkeyInfos.values()) {
			if (!args.fillDays) {
				if (ti.getCurrentCal().after(startTime) || ti.getCurrentCal().equals(startTime)) {
					ti.changeZone(ti.getCurrentZone(), lastTimes.get(lastDate));
					ti.endDay(lastDate);
					ti.printCurrentStay(false);
				}
			} else {
				ti.endDay(ti.getCurrentDate());
				ti.printCurrentStay(false);
			}
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

		if (antennaStream instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println(
					"Finished reading file " + ((FileInputStreamHandler) antennaStream).getInputFile().getPath(), true);
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
