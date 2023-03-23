package com.tome25.auswertung.testdata;

import static com.tome25.auswertung.CSVHandler.DEFAULT_SEPARATOR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.ZoneStay;
import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * The main class for generating test data for this program.
 * 
 * @author theodor
 */
public class AntennaDataGenerator {

	/**
	 * The random number generator to be used for all classes generating random test
	 * data.
	 */
	public static final Random RANDOM = new Random(85412307);// Fixed seed for deterministic output.

	/**
	 * Generates example antenna record date and writes it to a file.<br/>
	 * Also calculates how long each turkey should have spent where.<br/>
	 * <br/>
	 * Can also generate a downtimes file.
	 * 
	 * @param turkeys         The turkeys to move in the example data.
	 * @param zones           The zones for the turkeys to move in.
	 * @param antennaOutput   The output stream handler to write the data to.
	 * @param downtimesOutput The output stream handler to write downtimes to.<br/>
	 *                        Set to {@code null} to disable the downtimes file.
	 * @param args            The object containing the settings for the data to be
	 *                        generated.
	 * @param days            The number of days to generate example data for.
	 * @param continuous      Whether there should be days missing in the example
	 *                        data.
	 * @param complete        Whether every turkey should get at least one record
	 *                        every day.<br/>
	 *                        Note that setting this to {@code true} does not
	 *                        guarantee that every turkey gets at least one record
	 *                        each day, it is just very likely.<br/>
	 *                        {@code false} however guarantees that some are
	 *                        ignored.
	 * @return A {@link TestData} object.
	 * @throws NullPointerException     If one of the arguments, except
	 *                                  {@code downtimesOutput}, is {@code null}.
	 * @throws IllegalArgumentException If days is less than 1.
	 */
	public static TestData generateAntennaData(final List<TurkeyInfo> turkeys, final Map<String, List<String>> zones,
			IOutputStreamHandler antennaOutput, IOutputStreamHandler downtimesOutput, Arguments args, int days,
			boolean continuous, boolean complete) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(turkeys, "The turkeys to generate input data for cannot be null.");
		Objects.requireNonNull(zones, "The zones to use for the generated input can't be null.");
		Objects.requireNonNull(antennaOutput, "The antenna data output to write the file to can not be null.");
		Objects.requireNonNull(args, "The arguments to use cannot be null.");

		if (days < 1) {
			throw new IllegalArgumentException("Can't generate less than one day of data.");
		}

		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		antennaOutput.println("Transponder;Date;Time;Antenne");
		// turkey -> date -> zone -> time
		Map<String, Map<String, Map<String, Long>>> times = new LinkedHashMap<String, Map<String, Map<String, Long>>>();
		// turkey -> date -> zone changes
		Map<String, Map<String, Integer>> changes = new LinkedHashMap<String, Map<String, Integer>>();
		// turkey -> [zone stay]
		Map<String, List<ZoneStay>> stays = new LinkedHashMap<String, List<ZoneStay>>();
		// [start -> end]
		List<Pair<Long, Long>> downtimes = downtimesOutput == null ? null : new ArrayList<Pair<Long, Long>>();

		Map<String, Long> lastZoneChange = new HashMap<String, Long>();
		Map<String, String> currentZone = new HashMap<String, String>();
		Map<String, String> lastZone = new HashMap<String, String>();
		Map<String, Long> lastRecord = new HashMap<String, Long>();

		// turkey -> zone -> time
		Map<String, Pair<String, Integer>> unknownTimes = new HashMap<String, Pair<String, Integer>>();

		Calendar cal = new GregorianCalendar();
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		cal.setTimeInMillis(0);
		cal.set(2022, Calendar.FEBRUARY, 5, 0, 0, 0);
		long startTime = -1;
		for (int day = 0; day < days; day++) {
			String date = TimeUtils.encodeDate(cal);
			Pair<Pair<Map<String, Map<String, Integer>>, Map<String, Integer>>, Pair<Map<String, List<ZoneStay>>, Pair<Long, Long>>> dayData = generateDayAntennaData(
					turkeys, zones, date, antennaOutput, lastZoneChange, currentZone, lastZone, lastRecord, args,
					startTime, complete, downtimesOutput != null);
			Map<String, Map<String, Integer>> dayTimes = dayData.getKey().getKey();
			Map<String, Integer> dayChanges = dayData.getKey().getValue();
			Map<String, List<ZoneStay>> dayStays = dayData.getValue().getKey();
			Pair<Long, Long> downtime = dayData.getValue().getValue();

			if (downtime != null) {
				downtimes.add(downtime);
				StringBuilder line = new StringBuilder();
				line.append(date);
				line.append(DEFAULT_SEPARATOR);
				line.append(TimeUtils.encodeTime(downtime.getKey() - cal.getTimeInMillis()));
				line.append(DEFAULT_SEPARATOR);
				line.append(date);
				line.append(DEFAULT_SEPARATOR);
				line.append(TimeUtils.encodeTime(downtime.getValue() - cal.getTimeInMillis()));
				downtimesOutput.println(line.toString());
			}

			boolean skipNext = !continuous && RANDOM.nextInt(3) == 0;

			long lastChange = 0;
			for (Long change : lastRecord.values()) {
				if (change > lastChange) {
					lastChange = change;
				}
			}
			Calendar lastChangeCal = new GregorianCalendar();
			lastChangeCal.setTimeInMillis(lastChange);

			long downtimeStart = 0;
			if (downtimesOutput != null && skipNext) {
				downtimeStart = lastChange + RANDOM.nextInt(24 * 3600000 - TimeUtils.getMsOfDay(lastChangeCal));
				downtimeStart = downtimeStart / 10 * 10;
			}

			for (String turkey : dayTimes.keySet()) {
				// Times handling below
				if (!times.containsKey(turkey)) {
					times.put(turkey, new LinkedHashMap<String, Map<String, Long>>());
				}

				Map<String, Long> zoneDayTimes = new HashMap<String, Long>();
				for (String zone : dayTimes.get(turkey).keySet()) {
					zoneDayTimes.put(zone, (long) (int) dayTimes.get(turkey).get(zone));
				}
				times.get(turkey).put(date, zoneDayTimes);

				if (!times.get(turkey).containsKey("total")) {
					times.get(turkey).put("total", new HashMap<String, Long>());
				}

				Map<String, Long> totalTimes = times.get(turkey).get("total");
				for (String zone : dayTimes.get(turkey).keySet()) {
					if (totalTimes.containsKey(zone)) {
						totalTimes.put(zone, totalTimes.get(zone) + dayTimes.get(turkey).get(zone));
					} else {
						totalTimes.put(zone, (long) (int) dayTimes.get(turkey).get(zone));
					}
				}

				if (!args.fillDays) {
					String zone = currentZone.get(turkey);

					if (downtime != null && unknownTimes.containsKey(turkey) && dayStays.containsKey(turkey)
							&& dayStays.get(turkey).size() > 0) {
						Calendar yesterCal = (Calendar) cal.clone();
						yesterCal.add(Calendar.DATE, -1);
						String yesterday = TimeUtils.encodeDate(yesterCal);
						if (times.get(turkey).containsKey(yesterday)
								&& dayStays.get(turkey).get(0).getEntryCal().getTimeInMillis() == downtime.getValue()) {
							String uZone = unknownTimes.get(turkey).getKey();
							long uTime = unknownTimes.get(turkey).getValue();
							if (times.get(turkey).get(yesterday).get(uZone) > uTime) {
								times.get(turkey).get(yesterday).put(uZone, times.get(turkey).get(yesterday).get(uZone)
										- unknownTimes.get(turkey).getValue());
							} else {
								times.get(turkey).get(yesterday).remove(uZone);
								if (times.get(turkey).get(yesterday).size() == 0) {
									times.get(turkey).remove(yesterday);
								}
							}
							totalTimes.put(uZone, totalTimes.get(uZone) - uTime);
							stays.get(turkey).remove(stays.get(turkey).size() - 1);
						}
					}

					unknownTimes.remove(turkey);
					int zoneTime = 0;
					if (day == days - 1 || skipNext) {
						if (lastRecord.get(turkey) != -1) {
							if (downtime == null || day == days - 1) {
								zoneTime = (int) (lastChange - lastZoneChange.get(turkey));
							} else {
								zoneTime = (int) (downtimeStart - lastZoneChange.get(turkey));
							}
						}
					} else {
						Calendar changeCal = new GregorianCalendar();
						changeCal.setTimeInMillis(lastZoneChange.get(turkey));
						zoneTime = 24 * 3600000 - TimeUtils.getMsOfDay(changeCal);
						if (lastRecord.get(turkey) == -1) {
							unknownTimes.put(turkey, new Pair<String, Integer>(zone, zoneTime));
						}
					}

					if (totalTimes.containsKey(zone)) {
						totalTimes.put(zone, totalTimes.get(zone) + zoneTime);
					} else {
						totalTimes.put(zone, (long) zoneTime);
					}

					if (zoneDayTimes.containsKey(zone)) {
						zoneDayTimes.put(zone, zoneDayTimes.get(zone) + zoneTime);
					} else if (zoneTime > 0) {
						zoneDayTimes.put(zone, (long) zoneTime);
					}

					if (lastRecord.get(turkey) == -1 && (day == days - 1 || skipNext)) {
						Calendar dayCal = new GregorianCalendar();
						dayCal.setTimeInMillis(lastZoneChange.get(turkey));
						while (times.get(turkey).containsKey(TimeUtils.encodeDate(dayCal))) {
							Map<String, Long> zoneTimes = times.get(turkey).get(TimeUtils.encodeDate(dayCal));
							if (downtime == null || lastZoneChange.get(turkey) > downtime.getValue()) {
								for (String tZone : zoneTimes.keySet()) {
									totalTimes.put(tZone, totalTimes.get(tZone) - zoneTimes.get(tZone));
								}
								times.get(turkey).remove(TimeUtils.encodeDate(dayCal));
							} else if (lastZoneChange.get(turkey) == downtime.getValue()) {
								break;
							}
							dayCal.add(Calendar.DATE, -1);
						}
					}
				}

				if (zoneDayTimes.isEmpty()) {
					times.get(turkey).remove(date);
				}

				// Changes handling below
				if (!changes.containsKey(turkey)) {
					changes.put(turkey, new HashMap<String, Integer>());
				}

				if (changes.get(turkey).containsKey(date)) {
					changes.get(turkey).put(date, changes.get(turkey).get(date) + dayChanges.get(turkey));
				} else {
					changes.get(turkey).put(date, dayChanges.get(turkey));
				}

				if (changes.get(turkey).containsKey("total")) {
					changes.get(turkey).put("total", changes.get(turkey).get("total") + dayChanges.get(turkey));
				} else {
					changes.get(turkey).put("total", dayChanges.get(turkey));
				}

				// Stays handling below
				if (!stays.containsKey(turkey) || stays.get(turkey).size() == 0) {
					stays.put(turkey, dayStays.get(turkey));
				} else {
					ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);
					ZoneStay firstDayStay = dayStays.get(turkey).get(0);

					if (lastStay.getZone().equals(firstDayStay.getZone())
							&& (lastStay.getExitDate().equals(firstDayStay.getEntryDate())
									|| lastStay.getExitCal().after(firstDayStay.getEntryCal()))) {
						lastStay.setExitTime(firstDayStay.getExitCal());
						dayStays.get(turkey).remove(firstDayStay);
					}

					stays.get(turkey).addAll(dayStays.get(turkey));
				}

				if (day == days - 1 || skipNext) {
					Calendar endCal = null;
					if (args.fillDays) {
						endCal = TimeUtils.parseDate(date);
						endCal.add(Calendar.DATE, 1);
					} else if (skipNext && downtimesOutput != null && day < days - 1) {
						endCal = new GregorianCalendar();
						endCal.setTimeInMillis(downtimeStart);
					} else {
						endCal = lastChangeCal;
					}

					ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);

					if (lastStay.getZone().equals(currentZone.get(turkey))) {
						if (downtime == null || lastStay.getEntryCal().getTimeInMillis() >= downtime.getValue()) {
							lastStay.setExitTime(endCal);
						}
					} else if (endCal.getTimeInMillis() > lastStay.getExitCal().getTimeInMillis()) {
						stays.get(turkey)
								.add(new ZoneStay(turkey, currentZone.get(turkey), lastStay.getExitCal(), endCal));
					}

					if (!args.fillDays && lastRecord.get(turkey) == -1 && stays.get(turkey).size() > 0) {
						if (downtime == null || stays.get(turkey).get(stays.get(turkey).size() - 1).getExitCal()
								.getTimeInMillis() > downtime.getKey()) {
							stays.get(turkey).remove(stays.get(turkey).size() - 1);
						}
					}
				}
			}

			startTime = -1;
			if (skipNext) {
				cal.add(Calendar.DATE, 2 + RANDOM.nextInt(5));

				if (downtimesOutput != null) {
					Calendar dtsCal = new GregorianCalendar();
					dtsCal.setTimeInMillis(downtimeStart);
					Calendar dteCal = (Calendar) cal.clone();
					int downtimeEnd = RANDOM.nextInt(2 * 3600000);
					downtimeEnd = downtimeEnd / 10 * 10;
					dteCal.add(Calendar.MILLISECOND, downtimeEnd);

					startTime = dteCal.getTimeInMillis();
					downtimes.add(new Pair<Long, Long>(downtimeStart, startTime - 1));
					StringBuilder line = new StringBuilder();
					line.append(date);
					line.append(DEFAULT_SEPARATOR);
					line.append(TimeUtils.encodeTime(TimeUtils.getMsOfDay(dtsCal)));
					line.append(DEFAULT_SEPARATOR);
					line.append(TimeUtils.encodeDate(cal));
					line.append(DEFAULT_SEPARATOR);
					line.append(TimeUtils.encodeTime(TimeUtils.getMsOfDay(dteCal)));
					downtimesOutput.println(line.toString());
				}

				lastZoneChange = new HashMap<String, Long>();
				currentZone = new HashMap<String, String>();
				lastZone = new HashMap<String, String>();
				lastRecord = new HashMap<String, Long>();
			} else {
				cal.add(Calendar.DATE, 1);
			}
		}

		return new TestData(times, changes, stays, downtimes, turkeys, zones);
	}

	/**
	 * Generates example antenna records for a single day, and writes them to a
	 * file.<br/>
	 * Also generates reference times for how long each turkey spent where.<br/>
	 * This methods returns four values:<br/>
	 * <ol>
	 * <li>The time each turkey spent in each zone. Its format is
	 * {@code turkey -> zone -> time}.</li>
	 * <li>The number of zone changes for each turkey. With a format of
	 * {@code turkey -> zoneChanges}.</li>
	 * <li>The {@link ZoneStay ZoneStays} for each turkey. Its format is
	 * {@code turkey -> [zoneStay]}.</li>
	 * <li>A downtime, if one was added for this day. Format:
	 * {@code start -> end}.</li>
	 * </ol>
	 * Writes valid data, not suitable for error handling tests.
	 * 
	 * @param turkeys        A list of turkeys to record in the antenna records
	 *                       file.
	 * @param zones          The zones of the area the turkeys can move in.
	 * @param date           The date for which to generate records.
	 * @param output         The {@link IOutputStreamHandler} to write the data to.
	 * @param lastZoneChange The last zone change of each turkey.<br/>
	 *                       Will be mutated to be used as day to day storage.<br/>
	 *                       A new {@link HashMap} will be used if {@code null}.
	 * @param currentZone    The current zone each turkey is in.<br/>
	 *                       Will be mutated to be used as day to day storage.<br/>
	 *                       A new {@link HashMap} will be used if {@code null}.
	 * @param lastZone       The last zone each turkey was in for more than the min
	 *                       zone time.<br/>
	 *                       Will be mutated to be used as day to day storage.<br/>
	 *                       A new {@link HashMap} will be used if {@code null}.
	 * @param lastRecord     The time stamp of the last record of each turkey.<br/>
	 *                       Will be mutated to be used as day to day storage.<br/>
	 *                       A new {@link HashMap} will be used if {@code null}.
	 * @param args           The arguments to use for the day to be generated.
	 * @param startTime      The time as of which generated antenna records should
	 *                       be considered for ouput data.<br/>
	 *                       For example the end of the last downtime.<br/>
	 *                       Set to -1 to use the first antenna record of that day.
	 * @param complete       Whether every turkey should get at least one record
	 *                       every day.<br/>
	 *                       Note that setting this to {@code true} does not
	 *                       guarantee that every turkey gets at least one record
	 *                       each day, it is just very likely.<br/>
	 *                       {@code false} however guarantees that some are ignored.
	 * @param downtime       Whether a downtime should be generated at some point on
	 *                       this day.<br/>
	 *                       During this downtime antenna records are still
	 *                       generated, but not considered for the output data.<br/>
	 *                       Every turkey is guaranteed to have at least one record
	 *                       after the downtime.
	 * @return Four individual values, basically.<br/>
	 *         <ol>
	 *         <li>The time each turkey spent in each zone.</li>
	 *         <li>The number of zone changes for each turkey.</li>
	 *         <li>The {@link ZoneStay ZoneStays} for each turkey.</li>
	 *         <li>A downtime, if one was added for this day.</li>
	 *         </ol>
	 * @throws NullPointerException If one of the parameters is {@code null}.
	 */
	public static Pair<Pair<Map<String, Map<String, Integer>>, Map<String, Integer>>, Pair<Map<String, List<ZoneStay>>, Pair<Long, Long>>> generateDayAntennaData(
			final List<TurkeyInfo> turkeys, final Map<String, List<String>> zones, final String date,
			IOutputStreamHandler output, Map<String, Long> lastZoneChange, Map<String, String> currentZone,
			Map<String, String> lastZone, Map<String, Long> lastRecord, final Arguments args, long startTime,
			final boolean complete, final boolean downtime) throws NullPointerException {
		Objects.requireNonNull(turkeys, "The turkeys to generate input data for can't be null.");
		Objects.requireNonNull(zones, "The zones to use for the generated input can't be null.");
		Objects.requireNonNull(date, "The date to generate data for can't be null.");
		Objects.requireNonNull(output, "The output to write the file to can't be null.");
		Objects.requireNonNull(args, "The arguments to use cannot be null.");

		if (lastZoneChange == null) {
			lastZoneChange = new HashMap<String, Long>();
		}

		if (currentZone == null) {
			currentZone = new HashMap<String, String>();
		}

		if (lastZone == null) {
			lastZone = new HashMap<String, String>();
		}

		if (lastRecord == null) {
			lastRecord = new HashMap<String, Long>();
		}

		// Turkey -> Zone -> Time
		Map<String, Map<String, Integer>> zoneTimes = new HashMap<String, Map<String, Integer>>();
		Map<String, Integer> zoneChanges = new HashMap<String, Integer>(); // Turkey -> Zone Changes

		// Turkey -> zone stays
		Map<String, List<ZoneStay>> stays = new HashMap<String, List<ZoneStay>>();

		List<String> zoneNames = new ArrayList<String>(zones.keySet());

		List<TurkeyInfo> usedTurkeys = new ArrayList<TurkeyInfo>(turkeys);
		List<String> ignoredTurkeys = new ArrayList<String>();
		if (!complete) {
			final int toRemove = RANDOM.nextInt(usedTurkeys.size() / 10) + 1;
			for (int i = 0; i < toRemove; i++) {
				TurkeyInfo removed = usedTurkeys.remove(RANDOM.nextInt(usedTurkeys.size()));
				ignoredTurkeys.add(removed.getId());
				if (args.fillDays) {
					currentZone.remove(removed.getId());
				} else if (!currentZone.containsKey(removed.getId())) {
					lastRecord.put(removed.getId(), -1l);
					currentZone.put(removed.getId(), zoneNames.get(RANDOM.nextInt(zoneNames.size())));
					if (startTime != -1) {
						lastZoneChange.put(removed.getId(), startTime);
					}
				}
			}
		}

		// Generate 10-20 zone changes per transponder on average
		final int perTrans = 10 + RANDOM.nextInt(11);
		int nTrans = 0;
		for (TurkeyInfo ti : usedTurkeys) {
			nTrans += ti.getTransponders().size();
		}

		int numChanges = nTrans * perTrans;

		// Leave one hour free as buffer, and calculate the time per change for even
		// distribution.
		int timePerChange = (23 * 3600000) / numChanges;

		long lastTime = TimeUtils.parseDate(date).getTimeInMillis();

		Pair<Long, Long> dt = null;
		if (downtime) {
			// The downtime should never end after 22:00
			long start = lastTime + RANDOM.nextInt(20 * 3600000);
			start = start / 10 * 10;
			// The downtime should be at least 5 mins long
			int dur = RANDOM.nextInt(2 * 3600000 - 300) + 300;
			dur = dur / 10 * 10;
			dt = new Pair<Long, Long>(start, start + dur);
		}

		Set<String> recordAfter = new HashSet<String>();

		for (int i = 0; i < numChanges; i++) {
			TurkeyInfo turkey = usedTurkeys.get(RANDOM.nextInt(usedTurkeys.size()));
			String turkeyName = turkey.getId();
			String transponder = turkey.getTransponders().get(RANDOM.nextInt(turkey.getTransponders().size()));
			String zone = zoneNames.get(RANDOM.nextInt(zoneNames.size()));

			// If there was no record for this turkey yet, but it was ignored.
			if (lastRecord.containsKey(turkeyName) && lastRecord.get(turkeyName) == -1) {
				zone = currentZone.get(turkeyName);
			}
			String antenna = zones.get(zone).get(RANDOM.nextInt(zones.get(zone).size()));

			long changeTime = lastTime + timePerChange + RANDOM.nextInt(timePerChange / 10) - timePerChange / 20;
			changeTime = ((changeTime + 5) / 10) * 10;// round to 10.
			changeTime = Math.max(lastTime, changeTime);

			Calendar changeCal = new GregorianCalendar();
			changeCal.setTimeInMillis(changeTime);

			if (!args.fillDays && startTime == -1) {
				startTime = changeTime;
				for (String t : ignoredTurkeys) {
					if (lastRecord.containsKey(t) && lastRecord.get(t) == -1 && !lastZoneChange.containsKey(t)) {
						lastZoneChange.put(t, startTime);
					}
				}
			}

			StringBuilder line = new StringBuilder();
			line.append(transponder);
			line.append(DEFAULT_SEPARATOR);
			line.append(date);
			line.append(DEFAULT_SEPARATOR);
			line.append(TimeUtils.encodeTime(TimeUtils.getMsOfDay(changeCal)));
			line.append(DEFAULT_SEPARATOR);
			line.append(antenna);
			output.println(line.toString());

			// Completely ignore records during the downtime.
			if (dt != null && changeTime >= dt.getKey() && changeTime <= dt.getValue()) {
				if (lastTime < dt.getKey()) {
					Calendar dtsCal = new GregorianCalendar();
					dtsCal.setTimeInMillis(dt.getKey());
					for (TurkeyInfo ti : usedTurkeys) {
						if (lastZoneChange.containsKey(ti.getId()) && currentZone.containsKey(ti.getId())) {
							if (lastRecord.containsKey(ti.getId()) && lastRecord.get(ti.getId()) == -1) {
								continue;
							}

							if (!stays.containsKey(ti.getId())) {
								stays.put(ti.getId(), new ArrayList<ZoneStay>());
							}

							Calendar lastChangeCal = new GregorianCalendar();
							lastChangeCal.setTimeInMillis(lastZoneChange.get(ti.getId()));
							if (stays.containsKey(ti.getId())) {
								if (stays.get(ti.getId()).size() > 0) {
									if (stays.get(ti.getId()).get(stays.get(ti.getId()).size() - 1)
											.getZone() == currentZone.get(ti.getId())) {
										stays.get(ti.getId()).get(stays.get(ti.getId()).size() - 1).setExitTime(dtsCal);
									} else {
										stays.get(ti.getId()).get(stays.get(ti.getId()).size() - 1)
												.setExitTime(lastChangeCal);
										stays.get(ti.getId()).add(new ZoneStay(ti.getId(), currentZone.get(ti.getId()),
												lastChangeCal, dtsCal));
									}
								} else {
									stays.get(ti.getId()).add(new ZoneStay(ti.getId(), currentZone.get(ti.getId()),
											lastChangeCal, dtsCal));
								}
							}

							if (!zoneTimes.containsKey(ti.getId())) {
								zoneTimes.put(ti.getId(), new HashMap<String, Integer>());
							}

							if (zoneTimes.containsKey(ti.getId())) {
								String cZone = currentZone.get(ti.getId());
								if (TimeUtils.isSameDay(lastChangeCal, changeCal)) {
									if (zoneTimes.get(ti.getId()).containsKey(cZone)) {
										zoneTimes.get(ti.getId()).put(cZone, (int) (zoneTimes.get(ti.getId()).get(cZone)
												+ dt.getKey() - lastZoneChange.get(ti.getId())));
									} else {
										zoneTimes.get(ti.getId()).put(cZone,
												(int) (dt.getKey() - lastZoneChange.get(ti.getId())));
									}
								} else {
									if (zoneTimes.get(ti.getId()).containsKey(cZone)) {
										zoneTimes.get(ti.getId()).put(cZone, (int) (zoneTimes.get(ti.getId()).get(cZone)
												+ TimeUtils.getMsOfDay(dtsCal)));
									} else {
										zoneTimes.get(ti.getId()).put(cZone, (int) TimeUtils.getMsOfDay(dtsCal));
									}
								}
							}
						}
					}
				}
				lastTime = changeTime;
				continue;
			}

			if (changeTime < startTime) {
				lastTime = changeTime;
				continue;
			}

			if (dt != null && changeTime > dt.getValue() && !recordAfter.contains(turkeyName)) {
				recordAfter.add(turkeyName);
				currentZone.remove(turkeyName);
			}

			int zoneTime = -1;
			// If this is the first record of this turkey
			if (!currentZone.containsKey(turkeyName)) {
				lastZoneChange.put(turkeyName, changeCal.getTimeInMillis());

				Calendar startCal;
				if (startTime == -1) {
					startCal = TimeUtils.parseDate(date);
				} else {
					startCal = new GregorianCalendar();
					startCal.setTimeInMillis(startTime);
				}

				if (!zoneTimes.containsKey(turkeyName)) {
					zoneTimes.put(turkeyName, new HashMap<String, Integer>());
				}

				if (!stays.containsKey(turkeyName)) {
					stays.put(turkeyName, new ArrayList<ZoneStay>());
				}

				if (dt != null && changeTime > dt.getValue()) {
					startCal = new GregorianCalendar();
					startCal.setTimeInMillis(dt.getValue());
				}

				if (zoneTimes.get(turkeyName).containsKey(zone)) {
					zoneTimes.get(turkeyName).put(zone,
							zoneTimes.get(turkeyName).get(zone) + (int) (changeTime - startCal.getTimeInMillis()));
				} else {
					zoneTimes.get(turkeyName).put(zone, (int) (changeTime - startCal.getTimeInMillis()));
				}
				stays.get(turkeyName).add(new ZoneStay(turkeyName, zone, startCal, changeCal));

				currentZone.put(turkeyName, zone);
				lastZone.put(turkeyName, zone);
				if (!zoneChanges.containsKey(turkeyName)) {
					zoneChanges.put(turkeyName, 0);
				} else {
					// Since this only happens after a downtime, the zoneChanges shouldn't change.
				}
				lastRecord.put(turkeyName, changeTime);
			} else {
				if (!currentZone.get(turkeyName).equals(zone)) {
					zoneTime = (int) (changeTime - lastZoneChange.get(turkeyName));

					Calendar lastChangeCal = new GregorianCalendar();
					lastChangeCal.setTimeInMillis(lastZoneChange.get(turkeyName));

					if (args.minTime <= 0 || zoneTime >= args.minTime * 1000) {
						String cZone = currentZone.get(turkeyName);

						if (!zoneTimes.containsKey(turkeyName)) {
							zoneTimes.put(turkeyName, new HashMap<String, Integer>());
							zoneTimes.get(turkeyName).put(cZone, TimeUtils.getMsOfDay(changeCal) - zoneTime);

							stays.put(turkeyName, new ArrayList<ZoneStay>(
									Arrays.asList(new ZoneStay(turkeyName, cZone, lastChangeCal))));
						} else if (!currentZone.get(turkeyName).equals(lastZone.get(turkeyName))) {
							stays.get(turkeyName).get(stays.get(turkeyName).size() - 1).setExitTime(lastChangeCal);
							stays.get(turkeyName).add(new ZoneStay(turkeyName, cZone, lastChangeCal));
						}

						if (zoneTimes.get(turkeyName).containsKey(cZone)) {
							zoneTimes.get(turkeyName).put(cZone, zoneTimes.get(turkeyName).get(cZone) + zoneTime);
						} else {
							zoneTimes.get(turkeyName).put(cZone, zoneTime);
						}

						if (!zoneChanges.containsKey(turkeyName)) {
							zoneChanges.put(turkeyName, 1);
						} else {
							zoneChanges.put(turkeyName, zoneChanges.get(turkeyName) + 1);
						}
						lastZone.put(turkeyName, currentZone.get(turkeyName));
					} else {
						String lZone = lastZone.get(turkeyName);

						if (!zoneTimes.containsKey(turkeyName)) {
							zoneTimes.put(turkeyName, new HashMap<String, Integer>());
							zoneTimes.get(turkeyName).put(zone, TimeUtils.getMsOfDay(changeCal) - zoneTime);

							stays.put(turkeyName, new ArrayList<ZoneStay>(
									Arrays.asList(new ZoneStay(turkeyName, zone, lastChangeCal))));
						}

						if (zoneTimes.get(turkeyName).containsKey(lZone)) {
							zoneTimes.get(turkeyName).put(lZone, zoneTimes.get(turkeyName).get(lZone) + zoneTime);
						} else {
							zoneTimes.get(turkeyName).put(lZone, zoneTime);
						}

						if (!zoneChanges.containsKey(turkeyName)) {
							zoneChanges.put(turkeyName, 0);
						}

						if (zone.equals(lastZone.get(turkeyName))) {
							zoneChanges.put(turkeyName, zoneChanges.get(turkeyName) - 1);
						} else if (currentZone.get(turkeyName).equals(lastZone.get(turkeyName))) {
							zoneChanges.put(turkeyName, zoneChanges.get(turkeyName) + 1);
						}

					}

					stays.get(turkeyName).get(stays.get(turkeyName).size() - 1).setExitTime(changeCal);
					currentZone.put(turkeyName, zone);
					lastZoneChange.put(turkeyName, changeTime);
				}
				lastRecord.put(turkeyName, changeTime);
			}

			lastTime = changeTime;
		}

		if (args.fillDays) {
			for (String turkey : zoneTimes.keySet()) {
				int zoneTime = (int) (TimeUtils.parseDate(date).getTimeInMillis() + (24 * 3600000)
						- lastZoneChange.get(turkey));

				String zone = currentZone.get(turkey);

				if (zoneTimes.get(turkey).containsKey(zone)) {
					zoneTimes.get(turkey).put(zone, zoneTimes.get(turkey).get(zone) + zoneTime);
				} else {
					zoneTimes.get(turkey).put(zone, zoneTime);
				}

				if (stays.containsKey(turkey)) {
					ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);
					Calendar endCal = TimeUtils.parseDate(date);
					endCal.add(Calendar.DATE, 1);
					if (lastStay.getZone().equals(zone)) {
						lastStay.setExitTime(endCal);
					} else {
						stays.get(turkey).add(new ZoneStay(turkey, zone, lastStay.getExitCal(), endCal));
					}
				}
			}
		} else if (!complete) {
			Calendar startCal = new GregorianCalendar();
			startCal.setTimeInMillis(startTime);
			Calendar lastCal = new GregorianCalendar();
			lastCal.setTimeInMillis(lastTime);
			for (String turkey : ignoredTurkeys) {
				if (currentZone.containsKey(turkey)) {
					zoneTimes.put(turkey, new HashMap<String, Integer>());
					zoneChanges.put(turkey, 0);
					Calendar entryCal = new GregorianCalendar();
					entryCal.setTimeInMillis(lastZoneChange.get(turkey));
					if (lastRecord.containsKey(turkey) && lastRecord.get(turkey) == -1
							&& TimeUtils.isSameDay(startCal, entryCal)) {
						if (dt == null) {
							zoneTimes.get(turkey).put(currentZone.get(turkey),
									(int) (lastTime - Math.max(0, startTime)));
							stays.put(turkey, new ArrayList<ZoneStay>(
									Arrays.asList(new ZoneStay(turkey, currentZone.get(turkey), startCal, lastCal))));
							lastZoneChange.put(turkey, lastTime);
						} else {
							Calendar dtsCal = new GregorianCalendar();
							dtsCal.setTimeInMillis(dt.getKey());
							Calendar dteCal = new GregorianCalendar();
							dteCal.setTimeInMillis(dt.getValue());
							stays.put(turkey, new ArrayList<ZoneStay>());
							if (lastZone.containsKey(turkey)) {
								zoneTimes.get(turkey).put(currentZone.get(turkey),
										(int) (dt.getKey() - Math.max(0, startTime)));
								stays.get(turkey).add(new ZoneStay(turkey, currentZone.get(turkey), startCal, dtsCal));
							}
							stays.get(turkey).add(new ZoneStay(turkey, currentZone.get(turkey), dteCal, lastCal));
							lastZone.remove(turkey);
							lastZoneChange.put(turkey, dt.getValue());
						}
					} else {
						if (dt == null) {
							zoneTimes.get(turkey).put(currentZone.get(turkey), TimeUtils.getMsOfDay(lastCal));
							stays.put(turkey, new ArrayList<ZoneStay>(
									Arrays.asList(new ZoneStay(turkey, currentZone.get(turkey), entryCal, lastCal))));
							lastZoneChange.put(turkey, lastTime);
						} else {
							Calendar dtsCal = new GregorianCalendar();
							dtsCal.setTimeInMillis(dt.getKey());
							Calendar dteCal = new GregorianCalendar();
							dteCal.setTimeInMillis(dt.getValue());
							stays.put(turkey, new ArrayList<ZoneStay>());
							if (lastZone.containsKey(turkey)) {
								zoneTimes.get(turkey).put(currentZone.get(turkey),
										(int) (TimeUtils.getMsOfDay(dtsCal)));
								stays.get(turkey).add(new ZoneStay(turkey, currentZone.get(turkey), entryCal, dtsCal));
							}
							stays.get(turkey).add(new ZoneStay(turkey, currentZone.get(turkey), dteCal, lastCal));
							lastZone.remove(turkey);
							lastRecord.put(turkey, -1l);
							lastZoneChange.put(turkey, dt.getValue());
						}
					}
				}
			}
		}

		if (downtime && !args.fillDays) {
			for (TurkeyInfo ti : usedTurkeys) {
				String turkey = ti.getId();
				if (!recordAfter.contains(turkey)) {
					lastZoneChange.put(turkey, dt.getValue());
					lastRecord.put(turkey, -1l);
					currentZone.put(turkey, zoneNames.get(RANDOM.nextInt(zoneNames.size())));
				}
			}
		}

		Pair<Map<String, Map<String, Integer>>, Map<String, Integer>> totals = new Pair<Map<String, Map<String, Integer>>, Map<String, Integer>>(
				zoneTimes, zoneChanges);
		Pair<Map<String, List<ZoneStay>>, Pair<Long, Long>> other = new Pair<Map<String, List<ZoneStay>>, Pair<Long, Long>>(
				stays, dt);

		return new Pair<Pair<Map<String, Map<String, Integer>>, Map<String, Integer>>, Pair<Map<String, List<ZoneStay>>, Pair<Long, Long>>>(
				totals, other);
	}

	/**
	 * A class for storing the generated test data.<br/>
	 * This object contains all data relevant to the test, except the individual
	 * antenna records, and except the file paths.
	 * 
	 * @author theodor
	 */
	public static class TestData {

		/**
		 * A map containing the times each turkey spent in each zone each day.<br/>
		 * Format: {@code turkey -> date -> zone -> time}
		 */
		public final Map<String, Map<String, Map<String, Long>>> zoneTimes;

		/**
		 * A map containing the number of zone changes for each turkey each day.<br/>
		 * Format: {@code turkey -> date -> zoneChanges}
		 */
		public final Map<String, Map<String, Integer>> zoneChanges;

		/**
		 * A map containing the {@link ZoneStay ZoneStays} for each turkey for each
		 * day.<br/>
		 * Format: {@code turkey -> [stay]}
		 */
		public final Map<String, List<ZoneStay>> zoneStays;

		/**
		 * A list containing the downtimes contained in the downtimes file.<br/>
		 * If no donwtimes file is used this is {@code null}.<br/>
		 * Format: {@code [start -> end]}
		 */
		public final List<Pair<Long, Long>> downtimes;

		/**
		 * A list containing the turkeys used as part of this test.<br/>
		 * Format: {@code [turkey]}
		 */
		public final List<TurkeyInfo> turkeys;

		/**
		 * A map containing the zones used for this test.<br/>
		 * Format: {@code zone -> [antenna]}
		 */
		public final Map<String, List<String>> zones;

		/**
		 * Creates a new TestData object.
		 * 
		 * @param zoneTimes   The zone times map.
		 * @param zoneChanges The zone changes map.
		 * @param zoneStays   The zone stays map.
		 * @param downtimes   The values of the downtimes file. Can be {@code null}.
		 * @param turkeys     The turkeys used in the test.
		 * @param zones       The zones used for this test.
		 * @throws NullPointerException If one of the inputs, except {@code downtimes},
		 *                              is {@code null}.
		 */
		public TestData(final Map<String, Map<String, Map<String, Long>>> zoneTimes,
				final Map<String, Map<String, Integer>> zoneChanges, final Map<String, List<ZoneStay>> zoneStays,
				final List<Pair<Long, Long>> downtimes, final List<TurkeyInfo> turkeys,
				final Map<String, List<String>> zones) throws NullPointerException {
			Objects.requireNonNull(zoneTimes, "The zone times cannot be null.");
			Objects.requireNonNull(zoneChanges, "The zone changes cannot be null.");
			Objects.requireNonNull(zoneStays, "The zone stays cannot be null.");
			Objects.requireNonNull(turkeys, "The turkeys cannot be null.");
			Objects.requireNonNull(zones, "The zones cannot be null.");

			this.zoneTimes = zoneTimes;
			this.zoneChanges = zoneChanges;
			this.zoneStays = zoneStays;
			this.downtimes = downtimes;
			this.turkeys = turkeys;
			this.zones = zones;
		}
	}
}
