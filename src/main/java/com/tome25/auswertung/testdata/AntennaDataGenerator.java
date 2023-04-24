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
	 * The seed to use for the random number generator used for test data.<br/>
	 * A fixed seed is used to allow for deterministic results.
	 */
	private static final int START_SEED = 85412307;

	/**
	 * The random number generator to be used for all classes generating random test
	 * data.<br/>
	 * Uses a fixed seed for deterministic output.<br/>
	 * Each {@link Thread} has its own instance, to make the results deterministic
	 * in multithreaded scenarios.
	 */
	private static final ThreadLocal<Random> RANDOM = new ThreadLocal<Random>() {
		protected Random initialValue() {
			return new Random(START_SEED);
		};
	};

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
	 * @param startDate       The first date to generate data for.
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
	 * @throws IllegalArgumentException If days is less than 1, or {@code startTime}
	 *                                  doesn't follow the required format.
	 */
	public static TestData generateAntennaData(final List<TurkeyInfo> turkeys, final Map<String, List<String>> zones,
			IOutputStreamHandler antennaOutput, IOutputStreamHandler downtimesOutput, final Arguments args,
			final String startDate, int days, boolean continuous, boolean complete)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(turkeys, "The turkeys to generate input data for cannot be null.");
		Objects.requireNonNull(zones, "The zones to use for the generated input can't be null.");
		Objects.requireNonNull(antennaOutput, "The antenna data output to write the file to can not be null.");
		Objects.requireNonNull(args, "The arguments to use cannot be null.");
		Objects.requireNonNull(startDate, "The start date cannot be null.");

		if (days < 1) {
			throw new IllegalArgumentException("Can't generate less than one day of data.");
		}

		if (turkeys.isEmpty()) {
			throw new IllegalArgumentException("Turkeys list cannot be empty.");
		}

		if (zones.isEmpty()) {
			throw new IllegalArgumentException("Zones map cannot be empty.");
		}

		if (startDate.trim().isEmpty()) {
			throw new IllegalArgumentException("Start date cannot be empty.");
		}

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

		// turkey -> end time
		Map<String, Calendar> endTime = new HashMap<String, Calendar>();

		for (TurkeyInfo turkey : turkeys) {
			if (turkey.getCurrentZone() != null) {
				currentZone.put(turkey.getId(), turkey.getCurrentZone());
			}
			if (turkey.getEndCal() != null) {
				endTime.put(turkey.getId(), turkey.getEndCal());
			}
		}

		Calendar cal = TimeUtils.parseDate(startDate);
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

			boolean skipNext = !continuous && nextInt(2) == 0;

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
				downtimeStart = lastChange + nextInt(24 * 3600000 - TimeUtils.getMsOfDay(lastChangeCal) - 1);
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

					if (downtime != null && unknownTimes.containsKey(turkey) && dayStays.containsKey(turkey)) {
						Calendar yesterCal = (Calendar) cal.clone();
						yesterCal.add(Calendar.DATE, -1);
						String yesterday = TimeUtils.encodeDate(yesterCal);
						if (times.get(turkey).containsKey(yesterday) && ((!dayStays.get(turkey).isEmpty()
								&& dayStays.get(turkey).get(0).getEntryCal().getTimeInMillis() == downtime.getValue())
								|| dayStays.get(turkey).isEmpty() && dayTimes.get(turkey).isEmpty())) {
							String uZone = unknownTimes.get(turkey).getKey();
							long uTime = unknownTimes.get(turkey).getValue();
							if (times.get(turkey).get(yesterday).get(uZone) > uTime) {
								times.get(turkey).get(yesterday).put(uZone,
										times.get(turkey).get(yesterday).get(uZone) - uTime);
							} else {
								times.get(turkey).get(yesterday).remove(uZone);
								if (times.get(turkey).get(yesterday).isEmpty()) {
									times.get(turkey).remove(yesterday);
								}
							}
							totalTimes.put(uZone, totalTimes.get(uZone) - uTime);
							if (totalTimes.get(uZone) == 0) {
								totalTimes.remove(uZone);
							}

							if (24 * 3600000 - TimeUtils.getMsOfDay(
									stays.get(turkey).get(stays.get(turkey).size() - 1).getEntryCal()) == uTime) {
								stays.get(turkey).remove(stays.get(turkey).size() - 1);
							}
						}
					}

					unknownTimes.remove(turkey);
					int zoneTime = 0;
					if (day == days - 1 || skipNext) {
						if (lastRecord.containsKey(turkey) && lastRecord.get(turkey) != -1) {
							if (endTime.containsKey(turkey) && TimeUtils.isSameDay(endTime.get(turkey), cal)
									&& endTime.get(turkey).before(lastChangeCal)) {
								zoneTime = (int) (endTime.get(turkey).getTimeInMillis() - lastZoneChange.get(turkey));
								if (zoneTime < 0) {
									zoneTime = 0;
								}
							} else if (downtime == null || day == days - 1) {
								long lzc = 0;
								for (Long change : lastZoneChange.values()) {
									if (change > lzc) {
										lzc = change;
									}
								}

								zoneTime = (int) (Math.max(lzc, lastChange) - lastZoneChange.get(turkey));
							} else if (endTime.containsKey(turkey) && TimeUtils.isSameDay(endTime.get(turkey), cal)
									&& endTime.get(turkey).getTimeInMillis() < downtimeStart) {
								zoneTime = (int) (endTime.get(turkey).getTimeInMillis() - lastZoneChange.get(turkey));
								if (zoneTime < 0) {
									zoneTime = 0;
								}
							} else {
								zoneTime = (int) (downtimeStart - lastZoneChange.get(turkey));
							}
						} else if (downtime != null && endTime.containsKey(turkey)
								&& TimeUtils.isSameDay(endTime.get(turkey), cal)
								&& endTime.get(turkey).getTimeInMillis() < downtime.getKey()) {
							zoneTime = (int) (endTime.get(turkey).getTimeInMillis() - lastZoneChange.get(turkey));
							String lZone = lastZone.get(turkey);

							if (totalTimes.containsKey(lZone)) {
								totalTimes.put(lZone, totalTimes.get(lZone) + zoneTime);
							} else if (zoneTime > 0) {
								totalTimes.put(zone, (long) zoneTime);
							}

							if (zoneDayTimes.containsKey(lZone)) {
								zoneDayTimes.put(lZone, zoneDayTimes.get(lZone) + zoneTime);
							} else if (zoneTime > 0) {
								zoneDayTimes.put(lZone, (long) zoneTime);
							}

							zoneTime = 0;
						} else if (day == days - 1 && lastZone.containsKey(turkey)) {
							long lzc = 0;
							for (Long change : lastZoneChange.values()) {
								if (change > lzc) {
									lzc = change;
								}
							}

							if (endTime.containsKey(turkey) && endTime.get(turkey).getTimeInMillis() < lzc) {
								lzc = endTime.get(turkey).getTimeInMillis();
							}

							zoneTime = (int) (lzc - lastZoneChange.get(turkey));
							if (zoneTime < 0) {
								zoneTime = 0;
							}
							String lZone = lastZone.get(turkey);

							if (totalTimes.containsKey(lZone)) {
								totalTimes.put(lZone, totalTimes.get(lZone) + zoneTime);
							} else if (zoneTime > 0) {
								totalTimes.put(zone, (long) zoneTime);
							}

							if (zoneDayTimes.containsKey(lZone)) {
								zoneDayTimes.put(lZone, zoneDayTimes.get(lZone) + zoneTime);
							} else if (zoneTime > 0) {
								zoneDayTimes.put(lZone, (long) zoneTime);
							}

							zoneTime = 0;
						}
					} else {
						Calendar changeCal = new GregorianCalendar();
						changeCal.setTimeInMillis(lastZoneChange.get(turkey));
						if (endTime.containsKey(turkey) && TimeUtils.isSameDay(endTime.get(turkey), changeCal)) {
							if (downtime == null || endTime.get(turkey).getTimeInMillis() < downtime.getKey()
									|| (endTime.get(turkey).getTimeInMillis() > downtime.getValue()
											&& changeCal.getTimeInMillis() > downtime.getValue())) {
								zoneTime = (int) (endTime.get(turkey).getTimeInMillis() - changeCal.getTimeInMillis());
								if (zoneTime < 0) {
									zoneTime = 0;
								}

								if (downtime != null && endTime.get(turkey).getTimeInMillis() < downtime.getKey()) {
									String lZone = lastZone.get(turkey);

									if (totalTimes.containsKey(lZone)) {
										totalTimes.put(lZone, totalTimes.get(lZone) + zoneTime);
									} else if (zoneTime > 0) {
										totalTimes.put(zone, (long) zoneTime);
									}

									if (zoneDayTimes.containsKey(lZone)) {
										zoneDayTimes.put(lZone, zoneDayTimes.get(lZone) + zoneTime);
									} else if (zoneTime > 0) {
										zoneDayTimes.put(lZone, (long) zoneTime);
									}

									zoneTime = 0;
								}
							} else {
								zoneTime = (int) (downtime.getKey() - changeCal.getTimeInMillis());
								if (zoneTime < 0) {
									zoneTime = 0;
								}

								if (lastRecord.containsKey(turkey) && lastRecord.get(turkey) == -1) {
									String lZone = lastZone.get(turkey);

									if (totalTimes.containsKey(lZone)) {
										totalTimes.put(lZone, totalTimes.get(lZone) + zoneTime);
									} else if (zoneTime > 0) {
										totalTimes.put(zone, (long) zoneTime);
									}

									if (zoneDayTimes.containsKey(lZone)) {
										zoneDayTimes.put(lZone, zoneDayTimes.get(lZone) + zoneTime);
									} else if (zoneTime > 0) {
										zoneDayTimes.put(lZone, (long) zoneTime);
									}
								}
								zoneTime = 0;
							}
						} else if (downtime != null && changeCal.getTimeInMillis() < downtime.getKey()) {
							if (dayTimes.containsKey(turkey) && !dayTimes.get(turkey).isEmpty()) {
								zoneTime = (int) (downtime.getKey() - changeCal.getTimeInMillis());
								String lZone = lastZone.get(turkey);

								if (totalTimes.containsKey(lZone)) {
									totalTimes.put(lZone, totalTimes.get(lZone) + zoneTime);
								} else if (zoneTime > 0) {
									totalTimes.put(zone, (long) zoneTime);
								}

								if (zoneDayTimes.containsKey(lZone)) {
									zoneDayTimes.put(lZone, zoneDayTimes.get(lZone) + zoneTime);
								} else if (zoneTime > 0) {
									zoneDayTimes.put(lZone, (long) zoneTime);
								}
							}

							Calendar dteCal = new GregorianCalendar();
							dteCal.setTimeInMillis(downtime.getValue());
							zoneTime = 24 * 3600000 - TimeUtils.getMsOfDay(dteCal);
						} else {
							zoneTime = 24 * 3600000 - TimeUtils.getMsOfDay(changeCal);
						}

						if (lastRecord.get(turkey) == -1) {
							unknownTimes.put(turkey, new Pair<String, Integer>(zone, zoneTime));
						}
					}

					if (totalTimes.containsKey(zone)) {
						totalTimes.put(zone, totalTimes.get(zone) + zoneTime);
					} else if (zoneTime > 0) {
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
									if (totalTimes.get(tZone) == 0) {
										totalTimes.remove(tZone);
									}
								}
								times.get(turkey).remove(TimeUtils.encodeDate(dayCal));
							} else if (lastZoneChange.get(turkey) == downtime.getValue()) {
								break;
							}
							dayCal.add(Calendar.DATE, -1);
						}
					}
				} else {
					if (downtimeStart != 0 && currentZone.containsKey(turkey)) {
						Calendar endCal = new GregorianCalendar();
						endCal.setTimeInMillis(downtimeStart);
						if (endTime.containsKey(turkey) && TimeUtils.isSameDay(endTime.get(turkey), cal)
								&& endTime.get(turkey).before(endCal)) {
							endCal = endTime.get(turkey);
						}
						zoneDayTimes.put(currentZone.get(turkey), Math.max(0, zoneDayTimes.get(currentZone.get(turkey))
								- 24 * 3600000 + TimeUtils.getMsOfDay(endCal)));
						totalTimes.put(currentZone.get(turkey), Math.max(0,
								totalTimes.get(currentZone.get(turkey)) - 24 * 3600000 + TimeUtils.getMsOfDay(endCal)));
					} else if (currentZone.containsKey(turkey) && endTime.containsKey(turkey)
							&& TimeUtils.isSameDay(endTime.get(turkey), cal)) {
						Calendar endCal = endTime.get(turkey);
						zoneDayTimes.put(currentZone.get(turkey), Math.max(0, zoneDayTimes.get(currentZone.get(turkey))
								- 24 * 3600000 + TimeUtils.getMsOfDay(endCal)));
						totalTimes.put(currentZone.get(turkey), Math.max(0,
								totalTimes.get(currentZone.get(turkey)) - 24 * 3600000 + TimeUtils.getMsOfDay(endCal)));
					} else if (downtimeStart != 0 && downtime != null && downtime.getKey() > downtimeStart
							&& lastZone.containsKey(turkey)) {
						int time = (int) (downtime.getKey() - downtimeStart);
						zoneDayTimes.put(lastZone.get(turkey),
								Math.max(0, zoneDayTimes.get(lastZone.get(turkey)) - time));
						totalTimes.put(lastZone.get(turkey), Math.max(0, totalTimes.get(lastZone.get(turkey)) - time));
					}

					if (!lastRecord.containsKey(turkey) || (lastRecord.get(turkey) != -1
							&& !TimeUtils.encodeDate(lastRecord.get(turkey)).equals(date))) {
						for (String zone : zoneDayTimes.keySet()) {
							totalTimes.put(zone, totalTimes.get(zone) - zoneDayTimes.get(zone));
						}
						zoneDayTimes.clear();
					}
				}

				if (zoneDayTimes.isEmpty()) {
					times.get(turkey).remove(date);
				}

				if (totalTimes.isEmpty()) {
					times.get(turkey).remove("total");
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

				if (changes.get(turkey).containsKey("total") && dayChanges.containsKey(turkey)) {
					changes.get(turkey).put("total", changes.get(turkey).get("total") + dayChanges.get(turkey));
				} else if (dayChanges.containsKey(turkey)) {
					changes.get(turkey).put("total", dayChanges.get(turkey));
				}

				// Stays handling below
				if (day < days - 1 && downtime != null && !dayStays.get(turkey).isEmpty()
						&& dayStays.get(turkey).get(dayStays.get(turkey).size() - 1).getExitCal()
								.getTimeInMillis() < downtime.getKey()
						&& (!endTime.containsKey(turkey)
								|| endTime.get(turkey).getTimeInMillis() > downtime.getKey())) {
					Calendar dtsCal = new GregorianCalendar();
					dtsCal.setTimeInMillis(downtime.getKey());
					ZoneStay lastStay = dayStays.get(turkey).get(dayStays.get(turkey).size() - 1);
					if (lastZone.get(turkey).equals(lastStay.getZone())) {
						lastStay.setExitTime(dtsCal);
					} else {
						dayStays.get(turkey)
								.add(new ZoneStay(turkey, lastZone.get(turkey), lastStay.getExitCal(), dtsCal));
					}
				}

				if (!stays.containsKey(turkey) || stays.get(turkey).isEmpty()) {
					stays.put(turkey, dayStays.get(turkey));
				} else if (!dayStays.get(turkey).isEmpty()) {
					ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);
					ZoneStay firstDayStay = dayStays.get(turkey).get(0);

					if (lastStay.getZone().equals(firstDayStay.getZone())
							&& !lastStay.getExitCal().before(firstDayStay.getEntryCal())) {
						lastStay.setExitTime(firstDayStay.getExitCal());
						dayStays.get(turkey).remove(firstDayStay);
					}

					stays.get(turkey).addAll(dayStays.get(turkey));
				}

				if (day == days - 1 || skipNext) {
					Calendar endCal = null;
					if (args.fillDays && (downtimesOutput == null || !skipNext)) {
						endCal = TimeUtils.parseDate(date);
						endCal.add(Calendar.DATE, 1);
					} else if (args.fillDays && skipNext && downtimesOutput != null && day == days - 1) {
						endCal = new GregorianCalendar();
						endCal.setTimeInMillis(downtimeStart);
					} else if (downtime != null && endTime.containsKey(turkey)
							&& endTime.get(turkey).getTimeInMillis() < downtime.getKey()) {
						endCal = endTime.get(turkey);
						ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);

						if (lastStay.getZone().equals(lastZone.get(turkey))) {
							lastStay.setExitTime(endCal);
						} else if (endCal.getTimeInMillis() > lastStay.getExitCal().getTimeInMillis()
								&& currentZone.containsKey(turkey)) {
							stays.get(turkey)
									.add(new ZoneStay(turkey, lastZone.get(turkey), lastStay.getExitCal(), endCal));
						}
					} else if (downtime != null && lastRecord.containsKey(turkey) && lastRecord.get(turkey) == -1
							&& lastZone.containsKey(turkey) && day == days - 1 && (!endTime.containsKey(turkey)
									|| endTime.get(turkey).getTimeInMillis() > downtime.getValue())) {
						long lzc = 0;
						for (Long change : lastZoneChange.values()) {
							if (change > lzc) {
								lzc = change;
							}
						}

						endCal = new GregorianCalendar();
						endCal.setTimeInMillis(lzc);
						ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);

						if (lastStay.getZone().equals(lastZone.get(turkey))) {
							lastStay.setExitTime(endCal);
						} else if (endCal.getTimeInMillis() > lastStay.getExitCal().getTimeInMillis()
								&& currentZone.containsKey(turkey)) {
							stays.get(turkey)
									.add(new ZoneStay(turkey, lastZone.get(turkey), lastStay.getExitCal(), endCal));
						}
					} else if (skipNext && downtimesOutput != null && day < days - 1) {
						endCal = new GregorianCalendar();
						endCal.setTimeInMillis(downtimeStart);
					} else {
						long lzc = 0;
						for (Long change : lastZoneChange.values()) {
							if (change > lzc) {
								lzc = change;
							}
						}

						endCal = new GregorianCalendar();
						endCal.setTimeInMillis(Math.max(lzc, lastChange));
					}

					if (endTime.containsKey(turkey) && endCal.after(endTime.get(turkey))) {
						endCal = endTime.get(turkey);
					}

					ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);

					if (currentZone.containsKey(turkey) && lastStay.getZone().equals(currentZone.get(turkey))) {
						if (downtime == null || lastStay.getEntryCal().getTimeInMillis() >= downtime.getValue()) {
							if (lastStay.getEntryCal().before(endCal)) {
								lastStay.setExitTime(endCal);
							} else {
								stays.get(turkey).remove(stays.get(turkey).size() - 1);
							}
						}
					} else if (endCal.getTimeInMillis() > lastStay.getExitCal().getTimeInMillis()
							&& currentZone.containsKey(turkey)) {
						stays.get(turkey)
								.add(new ZoneStay(turkey, currentZone.get(turkey), lastStay.getExitCal(), endCal));
					} else if (args.fillDays && !currentZone.containsKey(turkey) && lastZone.containsKey(turkey)
							&& lastStay.getZone().equals(lastZone.get(turkey)) && downtime != null && downtimeStart != 0
							&& downtimeStart < downtime.getKey()) {
						Calendar dtsCal = new GregorianCalendar();
						dtsCal.setTimeInMillis(downtimeStart);
						if (dtsCal.after(lastStay.getEntryCal())) {
							lastStay.setExitTime(dtsCal);
						} else {
							stays.get(turkey).remove(stays.get(turkey).size() - 1);
						}
					}

					if (!args.fillDays && lastRecord.get(turkey) == -1 && !stays.get(turkey).isEmpty()) {
						if (downtime == null || stays.get(turkey).get(stays.get(turkey).size() - 1).getExitCal()
								.getTimeInMillis() > downtime.getKey()) {
							stays.get(turkey).remove(stays.get(turkey).size() - 1);
						}
					}
				} else if (endTime.containsKey(turkey) && TimeUtils.isSameDay(cal, endTime.get(turkey))
						&& (downtime == null || endTime.get(turkey).getTimeInMillis() < downtime.getKey()
								|| (endTime.get(turkey).getTimeInMillis() > downtime.getValue()
										&& lastZoneChange.get(turkey) > downtime.getValue()))) {
					if (!endTime.get(turkey).after(stays.get(turkey).get(stays.get(turkey).size() - 1).getEntryCal())) {
						stays.get(turkey).remove(stays.get(turkey).size() - 1);
					} else if (stays.get(turkey).get(stays.get(turkey).size() - 1).getZone()
							.equals(currentZone.get(turkey))) {
						stays.get(turkey).get(stays.get(turkey).size() - 1).setExitTime(endTime.get(turkey));
					} else if (endTime.get(turkey)
							.after(stays.get(turkey).get(stays.get(turkey).size() - 1).getExitCal())) {
						String zone = currentZone.get(turkey);
						if (downtime != null && lastZone.containsKey(turkey)
								&& endTime.get(turkey).getTimeInMillis() < downtime.getKey()) {
							zone = lastZone.get(turkey);
						}

						ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);
						if (lastStay.getZone().equals(zone)) {
							lastStay.setExitTime(endTime.get(turkey));
						} else {
							stays.get(turkey)
									.add(new ZoneStay(turkey, zone, lastStay.getExitCal(), endTime.get(turkey)));
						}
					}
				}
			}

			startTime = -1;
			if (skipNext) {
				cal.add(Calendar.DATE, nextInt(6, 2));

				if (downtimesOutput != null) {
					Calendar dtsCal = new GregorianCalendar();
					dtsCal.setTimeInMillis(downtimeStart);
					Calendar dteCal = (Calendar) cal.clone();
					int downtimeEnd = nextInt(2 * 3600000 - 1);
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

				for (TurkeyInfo turkey : turkeys) {
					if (turkey.getCurrentZone() != null
							&& (!times.containsKey(turkey.getId()) || times.get(turkey.getId()).isEmpty())) {
						currentZone.put(turkey.getId(), turkey.getCurrentZone());
					}
				}
			} else {
				if (downtime != null) {
					for (String turkey : lastZoneChange.keySet()) {
						if (lastRecord.get(turkey) == -1) {
							lastZoneChange.put(turkey, downtime.getValue());
						}
					}
				}

				if (args.fillDays) {
					for (TurkeyInfo turkey : turkeys) {
						if (turkey.getCurrentZone() != null
								&& (!times.containsKey(turkey.getId()) || times.get(turkey.getId()).isEmpty())) {
							currentZone.put(turkey.getId(), turkey.getCurrentZone());
						}
					}
				}
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
			final int toRemove = nextInt(usedTurkeys.size() / 10, 1);
			for (int i = 0; i < toRemove; i++) {
				TurkeyInfo removed = usedTurkeys.remove(nextInt(usedTurkeys.size() - 1));
				ignoredTurkeys.add(removed.getId());
				if (args.fillDays) {
					currentZone.remove(removed.getId());
				} else if (!currentZone.containsKey(removed.getId()) || !lastZoneChange.containsKey(removed.getId())) {
					lastRecord.put(removed.getId(), -1l);
					if (!currentZone.containsKey(removed.getId()) || lastZoneChange.containsKey(removed.getId())) {
						currentZone.put(removed.getId(), zoneNames.get(nextInt(zoneNames.size() - 1)));
					}
					if (startTime != -1) {
						lastZoneChange.put(removed.getId(), startTime);
					}
				}
			}
		}

		final boolean initialStartTime = startTime != -1;

		// Generate 10-20 zone changes per transponder on average
		final int perTrans = nextInt(20, 10);
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
			long start = lastTime + nextInt(20 * 3600000 - 1);
			start = start / 10 * 10;
			// The downtime should be at least 5 mins long
			int dur = nextInt(2 * 3600000, 300000);
			dur = dur / 10 * 10;
			dt = new Pair<Long, Long>(start, start + dur);
		}

		Set<String> recordAfter = new HashSet<String>();

		for (int i = 0; i < numChanges; i++) {
			TurkeyInfo turkey = usedTurkeys.get(nextInt(usedTurkeys.size() - 1));
			String turkeyName = turkey.getId();
			String transponder = turkey.getTransponders().get(nextInt(turkey.getTransponders().size() - 1));
			String zone = zoneNames.get(nextInt(zoneNames.size() - 1));

			// If there was no record for this turkey yet, but it was ignored.
			if (lastRecord.containsKey(turkeyName) && lastRecord.get(turkeyName) == -1) {
				zone = currentZone.get(turkeyName);
			}
			String antenna = zones.get(zone).get(nextInt(zones.get(zone).size() - 1));

			long changeTime = lastTime + timePerChange + nextInt(timePerChange / 10) - timePerChange / 20;
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

			if (changeTime < startTime && initialStartTime) {
				lastTime = changeTime;
				continue;
			}

			if (!lastRecord.containsKey(turkeyName) || lastRecord.get(turkeyName) != -1) {
				lastRecord.put(turkeyName, changeTime);
			}

			if (turkey.getEndCal() != null && changeCal.after(turkey.getEndCal())) {
				if (!TimeUtils.isSameDay(turkey.getEndCal(), changeCal)
						&& (dt == null || changeTime <= dt.getKey() || changeTime >= dt.getValue())) {
					lastZoneChange.put(turkeyName, changeTime);
				} else if (lastZoneChange.containsKey(turkeyName)
						&& lastZoneChange.get(turkeyName) < turkey.getEndCal().getTimeInMillis()
						&& TimeUtils.isSameDay(changeCal, turkey.getEndCal())) {
					if (dt != null && lastZoneChange.get(turkeyName) == dt.getKey()) {
						continue;
					}

					if (args.fillDays && !TimeUtils.encodeDate(lastZoneChange.get(turkeyName)).equals(date)) {
						if (zoneTimes.containsKey(turkeyName)) {
							zoneTimes.get(turkeyName).clear();
						}
						zoneChanges.put(turkeyName, 0);
						if (stays.containsKey(turkeyName)) {
							stays.get(turkeyName).clear();
						}
						continue;
					}

					String cZone = currentZone.get(turkeyName);
					int zoneTime = (int) (turkey.getEndCal().getTimeInMillis() - lastZoneChange.get(turkeyName));
					Calendar lastChangeCal = new GregorianCalendar();
					lastChangeCal.setTimeInMillis(lastZoneChange.get(turkeyName));

					if (!zoneTimes.containsKey(turkeyName)) {
						zoneTimes.put(turkeyName, new HashMap<String, Integer>());
						zoneTimes.get(turkeyName).put(cZone, TimeUtils.getMsOfDay(turkey.getEndCal()) - zoneTime);

						stays.put(turkeyName,
								new ArrayList<ZoneStay>(Arrays.asList(new ZoneStay(turkeyName, cZone, lastChangeCal))));
					} else if (!currentZone.get(turkeyName).equals(lastZone.get(turkeyName))) {
						if (stays.get(turkeyName).get(stays.get(turkeyName).size() - 1).getEntryCal()
								.before(lastChangeCal)) {
							stays.get(turkeyName).get(stays.get(turkeyName).size() - 1).setExitTime(lastChangeCal);
						} else {
							stays.get(turkeyName).remove(stays.get(turkeyName).size() - 1);
						}
						stays.get(turkeyName).add(new ZoneStay(turkeyName, cZone, lastChangeCal));
					}

					if (zoneTimes.get(turkeyName).containsKey(cZone)) {
						zoneTimes.get(turkeyName).put(cZone, zoneTimes.get(turkeyName).get(cZone) + zoneTime);
					} else {
						zoneTimes.get(turkeyName).put(cZone, zoneTime);
					}

					if (!zoneChanges.containsKey(turkeyName)) {
						zoneChanges.put(turkeyName, 0);
					}

					lastZone.put(turkeyName, currentZone.get(turkeyName));
					stays.get(turkeyName).get(stays.get(turkeyName).size() - 1).setExitTime(turkey.getEndCal());
					currentZone.put(turkeyName, zone);
					lastZoneChange.put(turkeyName, turkey.getEndCal().getTimeInMillis());
				}
				continue;
			}

			if (dt != null && changeTime >= dt.getKey() && lastTime < dt.getKey()) {
				Calendar dtsCal = new GregorianCalendar();
				dtsCal.setTimeInMillis(dt.getKey());
				for (TurkeyInfo ti : usedTurkeys) {
					if (ti.getEndCal() != null && ti.getEndCal().getTimeInMillis() < dt.getKey()) {
						continue;
					}

					if (lastZoneChange.get(ti.getId()) == dt.getKey()) {
						continue;
					}

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
							if (!stays.get(ti.getId()).isEmpty()) {
								if (stays.get(ti.getId()).get(stays.get(ti.getId()).size() - 1).getZone() == currentZone
										.get(ti.getId())) {
									stays.get(ti.getId()).get(stays.get(ti.getId()).size() - 1).setExitTime(dtsCal);
								} else {
									stays.get(ti.getId()).get(stays.get(ti.getId()).size() - 1)
											.setExitTime(lastChangeCal);
									stays.get(ti.getId()).add(new ZoneStay(ti.getId(), currentZone.get(ti.getId()),
											lastChangeCal, dtsCal));
								}
							} else {
								stays.get(ti.getId()).add(
										new ZoneStay(ti.getId(), currentZone.get(ti.getId()), lastChangeCal, dtsCal));
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
						lastZoneChange.put(ti.getId(), dt.getKey());
					}
				}
			}

			// Completely ignore records during the downtime.
			if (dt != null && changeTime >= dt.getKey() && changeTime <= dt.getValue()) {
				lastTime = changeTime;
				continue;
			}

			if (dt != null && changeTime > dt.getValue() && !recordAfter.contains(turkeyName)) {
				recordAfter.add(turkeyName);
				if (lastRecord.containsKey(turkeyName) && lastRecord.get(turkeyName) != -1
						&& currentZone.containsKey(turkeyName) && lastZoneChange.containsKey(turkeyName)
						&& !zoneTimes.containsKey(turkeyName)) {
					Calendar dtsCal = new GregorianCalendar();
					dtsCal.setTimeInMillis(dt.getKey());
					zoneTimes.put(turkeyName, new HashMap<String, Integer>());
					zoneTimes.get(turkeyName).put(currentZone.get(turkeyName), TimeUtils.getMsOfDay(dtsCal));
					Calendar lastCal = new GregorianCalendar();
					lastCal.setTimeInMillis(lastZoneChange.get(turkeyName));
					stays.put(turkeyName, new ArrayList<ZoneStay>());
					stays.get(turkeyName).add(new ZoneStay(turkeyName, currentZone.get(turkeyName), lastCal, dtsCal));
				}
				if (lastZoneChange.containsKey(turkeyName)) {
					currentZone.remove(turkeyName);
				}
			}

			int zoneTime = -1;
			// If this is the first record of this turkey
			if (!currentZone.containsKey(turkeyName) || !lastZoneChange.containsKey(turkeyName)) {
				lastZoneChange.put(turkeyName, changeTime);

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

				if (dt != null && changeTime > dt.getValue() && dt.getValue() >= startTime) {
					startCal = new GregorianCalendar();
					startCal.setTimeInMillis(dt.getValue());
				}

				if (!currentZone.containsKey(turkeyName)) {
					if (zoneTimes.get(turkeyName).containsKey(zone)) {
						zoneTimes.get(turkeyName).put(zone,
								zoneTimes.get(turkeyName).get(zone) + (int) (changeTime - startCal.getTimeInMillis()));
					} else {
						zoneTimes.get(turkeyName).put(zone, (int) (changeTime - startCal.getTimeInMillis()));
					}

					if (startCal.before(changeCal)) {
						stays.get(turkeyName).add(new ZoneStay(turkeyName, zone, startCal, changeCal));
					} else if (startCal.equals(changeCal)) {
						stays.get(turkeyName).add(new ZoneStay(turkeyName, zone, startCal));
					}
					lastZone.put(turkeyName, zone);
				} else {
					String cZone = currentZone.get(turkeyName);
					if (zoneTimes.get(turkeyName).containsKey(cZone)) {
						zoneTimes.get(turkeyName).put(cZone,
								zoneTimes.get(turkeyName).get(cZone) + (int) (changeTime - startCal.getTimeInMillis()));
					} else {
						zoneTimes.get(turkeyName).put(cZone, (int) (changeTime - startCal.getTimeInMillis()));
					}

					if (startCal.before(changeCal)) {
						stays.get(turkeyName).add(new ZoneStay(turkeyName, cZone, startCal, changeCal));
						lastZone.put(turkeyName, cZone);
					} else if (startCal.equals(changeCal)) {
						stays.get(turkeyName).add(new ZoneStay(turkeyName, zone, startCal));
						lastZone.put(turkeyName, zone);
					}
				}

				if (!zoneChanges.containsKey(turkeyName)) {
					if (currentZone.containsKey(turkeyName) && !currentZone.get(turkeyName).equals(zone)
							&& startCal.before(changeCal)) {
						zoneChanges.put(turkeyName, 1);
					} else {
						zoneChanges.put(turkeyName, 0);
					}
				} else {
					// Since this only happens after a downtime, the zoneChanges shouldn't change.
				}
				currentZone.put(turkeyName, zone);
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
							if (stays.get(turkeyName).get(stays.get(turkeyName).size() - 1).getEntryCal()
									.before(lastChangeCal)) {
								stays.get(turkeyName).get(stays.get(turkeyName).size() - 1).setExitTime(lastChangeCal);
							} else {
								stays.get(turkeyName).remove(stays.get(turkeyName).size() - 1);
							}
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
			}

			if (lastRecord.get(turkeyName) == -1) {
				lastRecord.put(turkeyName, changeTime);
			}

			lastTime = changeTime;
		}

		Map<String, Calendar> endTime = new HashMap<String, Calendar>();
		for (TurkeyInfo turkey : turkeys) {
			if (turkey.getEndCal() != null) {
				endTime.put(turkey.getId(), turkey.getEndCal());
			}
		}

		if (args.fillDays) {
			for (String turkey : zoneTimes.keySet()) {
				String zone = currentZone.get(turkey);
				Calendar endCal = null;
				if (!downtime || recordAfter.contains(turkey)) {
					endCal = TimeUtils.parseDate(date);
					endCal.add(Calendar.DATE, 1);
				} else if (endTime.containsKey(turkey)
						&& TimeUtils.isSameDay(TimeUtils.parseDate(date), endTime.get(turkey))) {
					endCal = endTime.get(turkey);
					if (dt != null && dt.getKey() < endCal.getTimeInMillis()) {
						endCal = new GregorianCalendar();
						endCal.setTimeInMillis(dt.getKey());
					}
					lastZone.put(turkey, zone);
					currentZone.remove(turkey);
				} else {
					endCal = new GregorianCalendar();
					endCal.setTimeInMillis(dt.getKey());
					lastZone.put(turkey, zone);
					currentZone.remove(turkey);
				}

				int zoneTime = (int) (endCal.getTimeInMillis() - lastZoneChange.get(turkey));

				if (zoneTimes.get(turkey).containsKey(zone)) {
					zoneTimes.get(turkey).put(zone, zoneTimes.get(turkey).get(zone) + zoneTime);
				} else if (zoneTime > 0) {
					zoneTimes.get(turkey).put(zone, zoneTime);
				}

				if (stays.containsKey(turkey)) {
					ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);
					if (lastStay.getZone().equals(zone)) {
						lastStay.setExitTime(endCal);
					} else if (endCal.after(lastStay.getExitCal())) {
						stays.get(turkey).add(new ZoneStay(turkey, zone, lastStay.getExitCal(), endCal));
					}
				}
			}
		} else if (!complete) {
			Calendar startCal = new GregorianCalendar();
			startCal.setTimeInMillis(startTime);

			for (String turkey : ignoredTurkeys) {
				if (endTime.containsKey(turkey) && endTime.get(turkey).before(startCal)) {
					continue;
				}
				Calendar lastCal = new GregorianCalendar();
				lastCal.setTimeInMillis(lastTime);
				if (endTime.containsKey(turkey) && endTime.get(turkey).before(lastCal)) {
					lastCal = endTime.get(turkey);
				}

				if (currentZone.containsKey(turkey) && lastZoneChange.containsKey(turkey)) {
					zoneTimes.put(turkey, new HashMap<String, Integer>());
					zoneChanges.put(turkey, 0);
					Calendar entryCal = new GregorianCalendar();
					entryCal.setTimeInMillis(lastZoneChange.get(turkey));
					if (TimeUtils.isSameDay(startCal, entryCal)) {
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
								if (endTime.containsKey(turkey) && endTime.get(turkey).before(dtsCal)) {
									Calendar endCal = endTime.get(turkey);
									zoneTimes.get(turkey).put(currentZone.get(turkey),
											(int) (dt.getKey() - endCal.getTimeInMillis()));
									stays.get(turkey)
											.add(new ZoneStay(turkey, currentZone.get(turkey), startCal, endCal));
									lastZoneChange.put(turkey, endCal.getTimeInMillis());
								} else {
									zoneTimes.get(turkey).put(currentZone.get(turkey), (int) (dt.getKey() - startTime));
									stays.get(turkey)
											.add(new ZoneStay(turkey, currentZone.get(turkey), startCal, dtsCal));
									lastZoneChange.put(turkey, dt.getValue());
									lastRecord.put(turkey, -1l);
								}
							}

							if (endTime.containsKey(turkey) && !endTime.get(turkey).after(dteCal)) {
								continue;
							}

							if (dt == null || lastTime > dt.getValue()) {
								stays.get(turkey).add(new ZoneStay(turkey, currentZone.get(turkey), dteCal, lastCal));
								lastZone.remove(turkey);
								lastZoneChange.put(turkey, dt.getValue());
							}
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
							if (lastZone.containsKey(turkey)
									&& (!lastRecord.containsKey(turkey) || lastRecord.get(turkey) != -1)) {
								if (endTime.containsKey(turkey) && endTime.get(turkey).before(dtsCal)) {
									Calendar endCal = endTime.get(turkey);
									zoneTimes.get(turkey).put(currentZone.get(turkey),
											(int) (TimeUtils.getMsOfDay(endCal)));
									stays.get(turkey)
											.add(new ZoneStay(turkey, currentZone.get(turkey), entryCal, endCal));
									lastZoneChange.put(turkey, endCal.getTimeInMillis());
								} else {
									zoneTimes.get(turkey).put(currentZone.get(turkey),
											(int) (TimeUtils.getMsOfDay(dtsCal)));
									stays.get(turkey)
											.add(new ZoneStay(turkey, currentZone.get(turkey), entryCal, dtsCal));
									lastZoneChange.put(turkey, dt.getValue());
									lastRecord.put(turkey, -1l);
								}
							}

							if (endTime.containsKey(turkey) && !endTime.get(turkey).after(dteCal)) {
								continue;
							}

							if (dt == null || lastTime > dt.getValue()) {
								stays.get(turkey).add(new ZoneStay(turkey, currentZone.get(turkey), dteCal, lastCal));
								lastZone.remove(turkey);
								lastRecord.put(turkey, -1l);
								lastZoneChange.put(turkey, dt.getValue());
							}
						}
					}
				}
			}
		}

		if (downtime && !args.fillDays) {
			for (TurkeyInfo ti : usedTurkeys) {
				String turkey = ti.getId();
				if (!recordAfter.contains(turkey)) {
					lastZone.put(turkey, currentZone.get(turkey));
					currentZone.put(turkey, zoneNames.get(nextInt(zoneNames.size() - 1)));
					lastRecord.put(turkey, -1l);
					if (!recordAfter.isEmpty()
							&& (ti.getEndCal() == null || ti.getEndCal().getTimeInMillis() > dt.getValue())) {
						lastZoneChange.put(turkey, dt.getValue());
					}
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
	 * Gets a new random number from the internal {@link Random} instance.<br/>
	 * Numbers generated using this are roughly uniformly generated from
	 * {@link Integer#MIN_VALUE} to {@link Integer#MAX_VALUE}.
	 * 
	 * @return The randomly generated number.
	 */
	public static int nextInt() {
		return RANDOM.get().nextInt();
	}

	/**
	 * Gets a new random number from the internal {@link Random} instance.<br/>
	 * Numbers generated using this are roughly uniformly generated from
	 * {@code 0}(inclusive) and {@code max}(inclusive).
	 * 
	 * @param max The max number to generate. Can be any number between
	 *            {@code 0}(inclusive) and {@link Integer#MAX_VALUE}(inclusive).
	 * @return The randomly generated number.
	 * @throws IllegalArgumentException If {@code max} is less than 0.
	 */
	public static int nextInt(final int max) throws IllegalArgumentException {
		if (max < 0) {
			throw new IllegalArgumentException("Max cannot be less than zero.");
		} else if (max == 0) {
			return 0;
		} else if (max == Integer.MAX_VALUE) {
			return RANDOM.get().nextInt(0x40000000) + (RANDOM.get().nextBoolean() ? 0x40000000 : 0);
		} else {
			return RANDOM.get().nextInt(max + 1);
		}
	}

	/**
	 * Gets a new random number from the internal {@link Random} instance.<br/>
	 * Numbers generated using this are roughly uniformly generated from
	 * {@code min}(inclusive) and {@code max}(inclusive).
	 * 
	 * @param max The max number to generate. Can be any number between
	 *            {@link Integer#MIN_VALUE}(inclusive) and
	 *            {@link Integer#MAX_VALUE}(inclusive) as long as its more than
	 *            {@code min}.
	 * @param min The min number to generate. Can be any number between
	 *            {@link Integer#MIN_VALUE}(inclusive) and
	 *            {@link Integer#MAX_VALUE}(inclusive) as long as its less than
	 *            {@code max}.
	 * @return The randomly generated number.
	 * @throws IllegalArgumentException If {@code max} is less than 1.
	 */
	public static int nextInt(final int max, final int min) throws IllegalArgumentException {
		if (min >= max) {
			throw new IllegalArgumentException("Min cannot be greater than or equal to max.");
		}

		long possibilities = ((long) max) - ((long) min);
		double div = 4294967295d / (possibilities + 0.99999999d);
		long rand = RANDOM.get().nextInt();
		rand += Integer.MAX_VALUE + 1l;

		return (int) ((double) rand / div) + min;
	}

	/**
	 * Resets the current seed of the internal {@link Random} to its default value.
	 */
	public static void resetSeed() {
		RANDOM.get().setSeed(START_SEED);
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
