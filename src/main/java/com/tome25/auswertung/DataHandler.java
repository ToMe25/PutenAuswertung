package com.tome25.auswertung;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.log.LogHandler;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.IInputStreamHandler;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.utils.MapUtils;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * The class handling the main data analysis/conversion.
 * 
 * @author Theodor Meyer zu Hörste
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

		Map<String, ZoneInfo> zones = CSVHandler.readZonesCSV(zonesStream);
		if (zones == null) {
			LogHandler.err_println("Failed to read zone mappings from the input file.");
			LogHandler.print_debug_info("Zones Input Stream Handler: %s", zonesStream);
		}

		Set<String> zoneIds = new HashSet<String>();
		if (zones != null) {
			for (ZoneInfo zone : zones.values()) {
				zoneIds.add(zone.getId());
			}
		}

		Map<String, TurkeyInfo> turkeys = CSVHandler.readTurkeyCSV(turkeyStream, args,
				zones == null ? new HashSet<ZoneInfo>() : zones.values());
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

		totalsStream.println(CSVHandler.turkeyCsvHeader(zoneIds));
		staysStream.println(CSVHandler.staysCsvHeader());

		if (antennaStream instanceof FileInputStreamHandler) {// TODO convert to some kind of generic getInputName
			LogHandler.out_println(
					"Started reading file " + ((FileInputStreamHandler) antennaStream).getInputFile().getPath(), true);
		}

		List<TurkeyInfo> turkeyInfos = new ArrayList<TurkeyInfo>(new LinkedHashSet<TurkeyInfo>(turkeys.values()));
		if (staysStream != null) {
			for (TurkeyInfo turkey : turkeyInfos) {
				turkey.setStayOut(staysStream);
			}
		}

		String lastDate = null;
		Map<String, Calendar> lastTimes = new HashMap<String, Calendar>();
		List<String> dates = new ArrayList<String>();
		short[] tokenOrder = new short[] { 0, 1, 2, 3 };
		Calendar startTime = null;
		Calendar prevStartTime = null;
		Calendar lastDts = null;

		read_loop: while (!antennaStream.done()) {
			AntennaRecord record = CSVHandler.readAntennaRecord(antennaStream, tokenOrder);
			if (record == null) {
				LogHandler.err_println("Reading an antenna record from the input file failed.", true);
				LogHandler.print_debug_info("Antenna Input Stream Handler: %s", antennaStream);
				continue;
			}

			TurkeyInfo turkey = null;
			if (turkeys.containsKey(record.transponder)) {
				turkey = turkeys.get(record.transponder);
			} else {
				LogHandler.err_println(String.format(
						"Received antenna record for unknown transponder id \"%s\" on day %s at %s. Considering it a separate turkey.",
						record.transponder, record.date, record.getTime()));
				LogHandler.print_debug_info("Antenna Record: %s, Arguments: %s", record, args);
			}

			if (!zones.containsKey(record.antenna)) {
				LogHandler.err_println(String.format(
						"Received antenna record from unknown antenna id \"%s\" on day %s at %s. Skipping line.",
						record.antenna, record.date, record.getTime()));
				LogHandler.print_debug_info("Antenna Record: %s, Arguments: %s", record, args);
				continue;
			}

			long recordMs = record.cal.getTimeInMillis();
			Calendar downtimeStart = null;
			Calendar downtimeEnd = null;
			if (downtimes != null) {
				for (Pair<Long, Long> downtime : downtimes) {
					if (recordMs > downtime.getValue()) {
						// Last record was before or during the downtime, current one is after.
						if (lastDate != null && lastTimes.get(lastDate).getTimeInMillis() <= downtime.getValue()) {
							if (downtimeStart == null) {
								downtimeStart = new GregorianCalendar();
								downtimeStart.setTimeInMillis(downtime.getKey());
							}
							downtimeEnd = new GregorianCalendar();
							downtimeEnd.setTimeInMillis(downtime.getValue());
						}
						continue;
					}

					if (recordMs >= downtime.getKey()) {
						downtimeStart = new GregorianCalendar();
						downtimeStart.setTimeInMillis(downtime.getKey());
						downtimeEnd = new GregorianCalendar();
						downtimeEnd.setTimeInMillis(downtime.getValue());
						LogHandler.err_println(String.format(
								"Received antenna record for time %s %s, which is during the downtime from %s %s to %s %s. Skipping record.",
								record.date, record.getTime(), TimeUtils.encodeDate(downtimeStart),
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeStart)),
								TimeUtils.encodeDate(downtimeEnd),
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeEnd))));
						LogHandler.print_debug_info(
								"Antenna Record: %s, Downtime Start Date: %s, Downtime Start Time: %s, Downtime End Date: %s, Downtime End Time: %s, Arguments: %s",
								record, TimeUtils.encodeDate(downtimeStart),
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeStart)),
								TimeUtils.encodeDate(downtimeEnd),
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeEnd)), args);
						if (lastTimes.containsKey(record.date) && record.cal.after(lastTimes.get(record.date))) {
							lastTimes.put(record.date, record.cal);
						}
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
						record.date, record.getTime()), true);
				LogHandler.print_debug_info("Antenna Record: %s, Last Date: %s, Last Time: %s, Arguments: %s", record,
						TimeUtils.encodeDate(downtimeStart), TimeUtils.encodeTime(TimeUtils.getMsOfDay(downtimeStart)),
						args);
			}

			if (startTime == null) {
				startTime = record.cal;
				if (!args.fillDays) {
					for (TurkeyInfo ti : turkeyInfos) {
						ti.setStartTime(startTime);
					}
				}
			}

			if (downtimeStart != null && downtimeEnd != null) {
				if (TimeUtils.isSameDay(downtimeStart, downtimeEnd)) {
					for (TurkeyInfo ti : turkeyInfos) {
						if (!args.fillDays) {
							if (ti.tryUpdate(downtimeStart)) {
								ti.printCurrentStay(false);
							}
						} else if (args.fillDays && ti.hasDay(record.date)) {
							if (ti.tryUpdate(downtimeStart)) {
								ti.printCurrentStay(false);
							}
						}
						ti.setStartTime(downtimeEnd);
					}
					prevStartTime = startTime;
					startTime = downtimeEnd;
					lastDts = downtimeStart;
				} else {
					for (TurkeyInfo ti : turkeyInfos) {
						if (ti.getCurrentCal() != null && ti.getCurrentCal().after(startTime)) {
							if (!args.fillDays) {
								if (ti.tryUpdate(downtimeStart)) {
									ti.endDay(ti.getCurrentCal());
									ti.printCurrentStay(false);
								}
							} else if (args.fillDays && ti.getCurrentCal() != null) {
								if (downtimes != null && ti.hasDay(lastDate)) {
									if (ti.tryUpdate(downtimeStart)) {
										ti.endDay(downtimeStart, false);
									}
								} else if (downtimes == null) {
									Calendar end = ti.getCurrentCal();
									end.set(Calendar.HOUR_OF_DAY, 23);
									end.set(Calendar.MINUTE, 59);
									end.set(Calendar.SECOND, 59);
									end.set(Calendar.MILLISECOND, 999);
									if (ti.tryUpdate(end)) {
										ti.endDay(ti.getCurrentCal(), false);
									}
								}
								ti.printCurrentStay(false);
							}
						}

						if (!args.fillDays || downtimes != null) {
							ti.setStartTime(downtimeEnd);
						}
					}

					prevStartTime = startTime;
					startTime = downtimeEnd;
					lastDts = downtimeStart;

					for (String date : dates) {
						printDayOutput(totalsStream, turkeyInfos, date, zoneIds, true);
					}
					dates.clear();

					dates.add(record.date);
				}
			}

			if (!record.date.equals(lastDate)) {
				if (lastDate != null) {
					if (TimeUtils.parseDate(lastDate).after(record.cal)) {
						LogHandler.err_println("New antenna record on date " + record.date
								+ " is on a day before the previous date " + lastDate + ". Skipping line.");
						LogHandler.print_debug_info(
								"New Antenna Record: %s, New Time of Day: %s, New Date: %s, Current Time of Day: %s, Current Date: %s, Arguments: %s",
								record, record.getTime(), record.date,
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(lastTimes.get(record.date))), lastDate, args);
						continue;
					}

					for (TurkeyInfo ti : turkeyInfos) {
						if (!args.fillDays && ti.getCurrentCal() != null && !ti.getCurrentCal().before(startTime)) {
							ti.endDay(ti.getCurrentCal());
						} else if (args.fillDays && ti.hasDay(lastDate)) {
							if (ti.getEndCal() != null && TimeUtils.isSameDay(ti.getEndCal(), ti.getCurrentCal())) {
								Calendar end = ti.getCurrentCal();
								end.set(Calendar.HOUR_OF_DAY, 23);
								end.set(Calendar.MINUTE, 59);
								end.set(Calendar.SECOND, 59);
								end.set(Calendar.MILLISECOND, 999);
								if (ti.getCurrentCal().before(startTime)) {
									end = lastDts;
									ti.setStartTime(prevStartTime);
								}
								if (ti.tryUpdate(end)) {
									ti.endDay(ti.getCurrentCal(), false);
								}
								ti.setStartTime(startTime);
								ti.printCurrentStay(false);
							} else if (downtimes != null && ti.getCurrentCal().before(startTime)) {
								ti.setStartTime(prevStartTime);
								if (ti.tryUpdate(lastDts)) {
									ti.endDay(ti.getCurrentCal(), false);
								}
								ti.setStartTime(startTime);
								ti.printCurrentStay(false);
							} else {
								ti.endDay(ti.getCurrentCal());
							}
						}
					}

					if (totalsStream.printsTemporary()) {
						printDayOutput(totalsStream, turkeyInfos, lastDate, zoneIds, false);
					}
				}

				lastDate = record.date;
				if (!dates.contains(lastDate)) {
					dates.add(lastDate);
				}
				lastTimes.put(record.date, record.cal);
			} else if (record.cal.after(lastTimes.get(record.date))) {
				lastTimes.put(record.date, record.cal);
			}

			// Only happens if the transponder is unknown.
			if (turkey == null) {
				try {
					LogHandler.out_println(
							"Creating a TurkeyInfo object for unknown id \"" + record.transponder + "\".", true);
					turkeys.put(record.transponder,
							new TurkeyInfo(record.transponder, Collections.singletonList(record.transponder),
									staysStream, zones.get(record.antenna), record.cal,
									args.fillDays ? null : startTime, null, args));
					// Adding a turkey could mess up the sorting, since self-sorting maps can't sort
					// by value.
					turkeys = MapUtils.sortByValue(turkeys, null);
				} catch (NullPointerException e) {
					LogHandler.err_println("Creating a new TurkeyInfo object failed. Terminating.");
					LogHandler.print_exception(e, "create a new TurkeyInfo",
							"Turkey id: \"%s\", Transponder: \"%s\", Stays Stream Handler: %s, Initial Zone: \"%s\", Initial Date: %s, Initial Time: %s, Start Date: %s, Start Time %s, Arguments: %s",
							record.transponder, record.transponder, staysStream, zones.get(record.antenna).getId(),
							record.date, record.getTime(), startTime == null ? "null" : TimeUtils.encodeDate(startTime),
							startTime == null ? "null" : TimeUtils.encodeTime(TimeUtils.getMsOfDay(startTime)), args);
					break;
				}
			} else {
				try {
					if (args.fillDays && lastDts != null && turkey.getCurrentCal() != null
							&& TimeUtils.isNextDay(turkey.getCurrentCal(), lastDts)
							&& TimeUtils.isSameDay(lastDts, record.cal) && prevStartTime != null) {
						turkey.setStartTime(prevStartTime);
						turkey.tryUpdate(lastDts);
						turkey.setStartTime(startTime);
					}

					if (turkey.getCurrentCal() != null && record.cal.before(turkey.getCurrentCal())) {
						LogHandler.err_println("New antenna record at " + record.date + ' ' + record.getTime()
								+ " for turkey \"" + turkey.getId()
								+ "\" is before the last one for the same turkey. Skipping line.");
						LogHandler.print_debug_info(
								"New Antenna Record: %s, New Time of Day: %s, New Date: %s, Current Time of Day: %s, Current Date: %s, Turkey: %s",
								record, record.getTime(), record.date, TimeUtils.encodeTime(turkey.getCurrentTime()),
								turkey.getCurrentDate(), turkey);
						continue;
					} else if (turkey.getEndCal() != null && record.cal.after(turkey.getEndCal())) {
						LogHandler.err_println("New antenna record at " + record.date + ' ' + record.getTime()
								+ " for turkey \"" + turkey.getId()
								+ "\" is after that turkeys end time. Updating to its end time instead.");
						LogHandler.print_debug_info(
								"New Antenna Record: %s, Record Time of Day: %s, Record Date: %s, End Time of Day: %s, End Date: %s, Turkey: %s",
								record, record.getTime(), record.date,
								TimeUtils.encodeTime(TimeUtils.getMsOfDay(turkey.getEndCal())),
								TimeUtils.encodeDate(turkey.getEndCal()), turkey);
						if (turkey.tryUpdate(record.cal)) {
							turkey.endDay(turkey.getCurrentCal(), false);
							turkey.printCurrentStay(false);
						} else if (args.fillDays && turkey.getCurrentCal() != null
								&& !TimeUtils.isSameDay(turkey.getCurrentCal(), turkey.getEndCal())) {
							turkey.printCurrentStay(false);
						}
						continue;
					} else {
						turkey.changeZone(zones.get(record.antenna), record.cal);
					}
				} catch (IllegalArgumentException e) {
					LogHandler.err_println(
							"An error occurred while updating turkey \"" + turkey.getId() + "\". Skipping line.");
					LogHandler.print_exception(e, "update turkey zone", "New Antenna Record: %s, Turkey: %s", record,
							turkey);
				}
			}
		}

		for (TurkeyInfo ti : turkeyInfos) {
			if (ti.getCurrentCal() == null) {
				continue;
			}

			if (!args.fillDays) {
				Calendar endCal = lastTimes.get(lastDate);
				if (downtimes != null) {
					for (Pair<Long, Long> downtime : downtimes) {
						if (downtime.getKey() <= endCal.getTimeInMillis()
								&& downtime.getValue() >= endCal.getTimeInMillis()) {
							endCal = new GregorianCalendar();
							endCal.setTimeInMillis(downtime.getKey());
							break;
						}
					}
				}

				if (ti.tryUpdate(endCal)) {
					ti.endDay(ti.getCurrentCal());
					ti.printCurrentStay(false);
				}
			} else {
				if (ti.getCurrentCal().before(startTime) && !ti.getCurrentCal().before(prevStartTime)) {
					ti.setStartTime(prevStartTime);
				}

				if (ti.getStartCal() == null || ti.getCurrentCal().after(ti.getStartCal())) {
					if (downtimes != null) {
						Calendar dtsCal = null;
						long current = ti.getCurrentCal().getTimeInMillis();
						for (Pair<Long, Long> downtime : downtimes) {
							if (downtime.getKey() >= current) {
								dtsCal = new GregorianCalendar();
								dtsCal.setTimeInMillis(downtime.getKey());
								if (!TimeUtils.isSameDay(ti.getCurrentCal(), dtsCal)) {
									dtsCal = null;
								}
								break;
							}
						}

						if (dtsCal != null) {
							if (ti.tryUpdate(dtsCal)) {
								ti.endDay(dtsCal, false);
							}
						} else {
							Calendar end = ti.getCurrentCal();
							end.set(Calendar.HOUR_OF_DAY, 23);
							end.set(Calendar.MINUTE, 59);
							end.set(Calendar.SECOND, 59);
							end.set(Calendar.MILLISECOND, 999);
							if (ti.tryUpdate(end)) {
								ti.endDay(ti.getCurrentCal(), false);
							}
						}
					} else {
						Calendar end = ti.getCurrentCal();
						end.set(Calendar.HOUR_OF_DAY, 23);
						end.set(Calendar.MINUTE, 59);
						end.set(Calendar.SECOND, 59);
						end.set(Calendar.MILLISECOND, 999);
						if (ti.tryUpdate(end)) {
							ti.endDay(ti.getCurrentCal(), false);
						}
					}
					ti.printCurrentStay(false);
				}
			}
		}

		for (String date : dates) {
			printDayOutput(totalsStream, turkeyInfos, date, zoneIds, true);
		}
		printDayOutput(totalsStream, turkeyInfos, null, zoneIds, true);

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
