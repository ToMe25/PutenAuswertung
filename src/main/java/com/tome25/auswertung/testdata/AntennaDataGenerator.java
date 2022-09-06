package com.tome25.auswertung.testdata;

import static com.tome25.auswertung.CSVHandler.DEFAULT_SEPARATOR;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.ZoneStay;
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
	 * This method returns three maps:<br/>
	 * The format of the first one is {@code turkey -> date -> zone -> time}.<br/>
	 * The format of the second one is {@code turkey -> date -> zoneChanges}.<br/>
	 * The format of the third map is {@code turkey -> zoneStays}.
	 * 
	 * @param turkeys    The turkeys to move in the example data.
	 * @param zones      The zones for the turkeys to move in.
	 * @param output     The output stream handler to write the data to.
	 * @param days       The number of days to generate example data for.
	 * @param continuous Whether there should be days missing in the example data.
	 * @param fillDays   Whether the time before the first and after the last
	 *                   measurement each day should be expected to be spent in the
	 *                   first/last zone.
	 * @return The three maps described above.
	 * @throws NullPointerException If one of the arguments is {@code null}.
	 */
	public static Pair<Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>>, Map<String, List<ZoneStay>>> generateAntennaData(
			List<TurkeyInfo> turkeys, Map<String, List<String>> zones, IOutputStreamHandler output, int days,
			boolean continuous, boolean fillDays) throws NullPointerException {
		Objects.requireNonNull(turkeys, "The turkeys to generate input data for cannot be null.");
		Objects.requireNonNull(zones, "The zones to use for the generated input can't be null.");
		Objects.requireNonNull(output, "The output to write the file to can not be null.");

		output.println("Transponder;Date;Time;Antenne");
		// turkey -> date -> zone -> time
		Map<String, Map<String, Map<String, Long>>> times = new LinkedHashMap<String, Map<String, Map<String, Long>>>();
		// turkey -> date -> zone changes
		Map<String, Map<String, Integer>> changes = new LinkedHashMap<String, Map<String, Integer>>();
		// turkey -> zone stays
		Map<String, List<ZoneStay>> stays = new LinkedHashMap<String, List<ZoneStay>>();

		Map<String, Long> lastZoneChange = new HashMap<String, Long>();
		Map<String, String> currentZone = new HashMap<String, String>();
		Map<String, String> lastZone = new HashMap<String, String>();
		Map<String, Long> lastRecord = new HashMap<String, Long>();

		Calendar cal = new GregorianCalendar();
		cal.set(2022, Calendar.FEBRUARY, 5, 0, 0, 0);
		for (int day = 0; day < days; day++) {
			String date = TimeUtils.encodeDate(cal);
			Pair<Pair<Map<String, Map<String, Integer>>, Map<String, Integer>>, Map<String, List<ZoneStay>>> dayData = generateDayAntennaData(
					turkeys, zones, date, output, lastZoneChange, currentZone, lastZone, lastRecord, fillDays);
			Map<String, Map<String, Integer>> dayTimes = dayData.getKey().getKey();
			Map<String, Integer> dayChanges = dayData.getKey().getValue();
			Map<String, List<ZoneStay>> dayStays = dayData.getValue();

			boolean skipNext = !continuous && RANDOM.nextInt(3) == 0;

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

				long lastChange = 0;
				for (Long change : lastRecord.values()) {
					if (change > lastChange) {
						lastChange = change;
					}
				}

				if (!fillDays) {
					String zone = currentZone.get(turkey);

					int zoneTime = 0;
					if (day == days - 1 || skipNext) {
						zoneTime = (int) (lastChange - lastZoneChange.get(turkey));
					} else {
						Calendar changeCal = new GregorianCalendar();
						changeCal.setTimeInMillis(lastZoneChange.get(turkey));
						zoneTime = 24 * 3600000 - TimeUtils.getMsOfDay(changeCal);
					}

					totalTimes.put(zone, totalTimes.get(zone) + zoneTime);
					if (zoneDayTimes.containsKey(zone)) {
						zoneDayTimes.put(zone, zoneDayTimes.get(zone) + zoneTime);
					} else {
						zoneDayTimes.put(zone, (long) zoneTime);
					}
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
				if (!stays.containsKey(turkey)) {
					stays.put(turkey, dayStays.get(turkey));
				} else {
					ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);
					ZoneStay firstDayStay = dayStays.get(turkey).get(0);

					if (lastStay.getZone().equals(firstDayStay.getZone())
							&& lastStay.getExitDate().equals(firstDayStay.getEntryDate())) {
						lastStay.setExitTime(firstDayStay.getExitCal());
						dayStays.get(turkey).remove(firstDayStay);
					}

					stays.get(turkey).addAll(dayStays.get(turkey));
				}

				if (day == days - 1 || skipNext) {
					Calendar lastChangeCal = new GregorianCalendar();
					lastChangeCal.setTimeInMillis(lastChange);
					Calendar endCal = null;
					if (fillDays) {
						endCal = TimeUtils.parseDate(date);
						endCal.add(Calendar.DATE, 1);
					} else {
						endCal = lastChangeCal;
					}

					ZoneStay lastStay = stays.get(turkey).get(stays.get(turkey).size() - 1);
					
					if (lastStay.getZone().equals(currentZone.get(turkey))) {
						lastStay.setExitTime(endCal);
					} else if (endCal.getTimeInMillis() > lastStay.getExitCal().getTimeInMillis()) {
						stays.get(turkey)
								.add(new ZoneStay(turkey, currentZone.get(turkey), lastStay.getExitCal(), endCal));
					}
				}
			}

			if (skipNext) {
				cal.add(Calendar.DATE, 2 + RANDOM.nextInt(5));

				lastZoneChange = new HashMap<String, Long>();
				currentZone = new HashMap<String, String>();
				lastZone = new HashMap<String, String>();
				lastRecord = new HashMap<String, Long>();
			} else {
				cal.add(Calendar.DATE, 1);
			}
		}

		Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> totals = new Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>>(
				times, changes);

		return new Pair<Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>>, Map<String, List<ZoneStay>>>(
				totals, stays);
	}

	/**
	 * Generates example antenna records for a single day, and writes them to a
	 * file.<br/>
	 * Also generates reference times for how long each turkey spent where.<br/>
	 * This methods returns three maps:<br/>
	 * The first resulting map format is {@code turkey -> zone -> time}.<br/>
	 * The second resulting map format is {@code turkey -> zoneChanges}.<br/>
	 * The third map format is {@code turkey -> zoneStays}.<br/>
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
	 * @param lastZone       The last zone each turkey was in for more than 5
	 *                       minutes.<br/>
	 *                       Will be mutated to be used as day to day storage.<br/>
	 *                       A new {@link HashMap} will be used if {@code null}.
	 * @param lastRecord     The time stamp of the last record of each turkey.<br/>
	 *                       Will be mutated to be used as day to day storage.<br/>
	 *                       A new {@link HashMap} will be used if {@code null}.
	 * @param fillDay        Whether the time before the first and after the last
	 *                       record should be filled.
	 * @return The times each turkey spent in each zone.
	 * @throws NullPointerException If one of the parameters is {@code null}.
	 */
	public static Pair<Pair<Map<String, Map<String, Integer>>, Map<String, Integer>>, Map<String, List<ZoneStay>>> generateDayAntennaData(
			List<TurkeyInfo> turkeys, Map<String, List<String>> zones, String date, IOutputStreamHandler output,
			Map<String, Long> lastZoneChange, Map<String, String> currentZone, Map<String, String> lastZone,
			Map<String, Long> lastRecord, boolean fillDay) throws NullPointerException {
		Objects.requireNonNull(turkeys, "The turkeys to generate input data for can't be null.");
		Objects.requireNonNull(zones, "The zones to use for the generated input can't be null.");
		Objects.requireNonNull(date, "The date to generate data for can't be null.");
		Objects.requireNonNull(output, "The output to write the file to can't be null.");

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

		// Generate 10-20 zone changes per transponder on average
		int perTrans = 10 + RANDOM.nextInt(11);
		int nTrans = 0;
		for (TurkeyInfo ti : turkeys) {
			nTrans += ti.getTransponders().size();
		}

		int numChanges = nTrans * perTrans;

		// Leave one hour free as buffer, and calculate the time per change for even
		// distribution.
		int timePerChange = (23 * 3600000) / numChanges;

		long startTime = -1;

		long lastTime = TimeUtils.parseDate(date).getTimeInMillis();
		List<String> zoneNames = new ArrayList<String>(zones.keySet());

		// Turkey -> Zone -> Time
		Map<String, Map<String, Integer>> zoneTimes = new HashMap<String, Map<String, Integer>>();
		Map<String, Integer> zoneChanges = new HashMap<String, Integer>(); // Turkey -> Zone Changes

		// Turkey -> zone stays
		Map<String, List<ZoneStay>> stays = new HashMap<String, List<ZoneStay>>();

		for (int i = 0; i < numChanges; i++) {
			TurkeyInfo turkey = turkeys.get(RANDOM.nextInt(turkeys.size()));
			String turkeyName = turkey.getId();
			String transponder = turkey.getTransponders().get(RANDOM.nextInt(turkey.getTransponders().size()));
			String zone = zoneNames.get(RANDOM.nextInt(zoneNames.size()));
			String antenna = zones.get(zone).get(RANDOM.nextInt(zones.get(zone).size()));

			long changeTime = lastTime + timePerChange + RANDOM.nextInt(timePerChange / 10) - timePerChange / 20;
			changeTime = ((changeTime + 5) / 10) * 10;// round to 10.
			changeTime = Math.max(lastTime, changeTime);

			if (startTime == -1) {
				startTime = changeTime;
			}

			Calendar changeCal = new GregorianCalendar();
			changeCal.setTimeInMillis(changeTime);

			StringBuilder line = new StringBuilder();
			line.append(transponder);
			line.append(DEFAULT_SEPARATOR);
			line.append(date);
			line.append(DEFAULT_SEPARATOR);
			line.append(TimeUtils.encodeTime(TimeUtils.getMsOfDay(changeCal)));
			line.append(DEFAULT_SEPARATOR);
			line.append(antenna);
			output.println(line.toString());

			int zoneTime = -1;
			// If this is the first record of this turkey
			if (!currentZone.containsKey(turkeyName)) {
				lastZoneChange.put(turkeyName, changeCal.getTimeInMillis());

				zoneTimes.put(turkeyName, new HashMap<String, Integer>());
				stays.put(turkeyName, new ArrayList<ZoneStay>());

				if (fillDay) {
					zoneTimes.get(turkeyName).put(zone, TimeUtils.getMsOfDay(changeCal));
					stays.get(turkeyName).add(new ZoneStay(turkeyName, zone, TimeUtils.parseDate(date)));
				} else {
					zoneTimes.get(turkeyName).put(zone, (int) (changeTime - startTime));
					Calendar startCal = new GregorianCalendar();
					startCal.setTimeInMillis(startTime);
					stays.get(turkeyName).add(new ZoneStay(turkeyName, zone, startCal));
				}

				currentZone.put(turkeyName, zone);
				lastZone.put(turkeyName, zone);
				zoneChanges.put(turkeyName, 0);
				lastRecord.put(turkeyName, changeTime);
			} else {
				if (!currentZone.get(turkeyName).equals(zone)) {
					zoneTime = (int) (changeTime - lastZoneChange.get(turkeyName));

					Calendar lastChangeCal = new GregorianCalendar();
					lastChangeCal.setTimeInMillis(lastZoneChange.get(turkeyName));

					if (zoneTime >= TurkeyInfo.MIN_ZONE_TIME) {
						String cZone = currentZone.get(turkeyName);

						if (!zoneTimes.containsKey(turkeyName)) {
							zoneTimes.put(turkeyName, new HashMap<String, Integer>());
							zoneTimes.get(turkeyName).put(cZone, TimeUtils.getMsOfDay(changeCal) - zoneTime);
							zoneChanges.put(turkeyName, 0);

							stays.put(turkeyName, new ArrayList<ZoneStay>());
							stays.get(turkeyName).add(new ZoneStay(turkeyName, cZone, lastChangeCal));
						} else if (!currentZone.get(turkeyName).equals(lastZone.get(turkeyName))) {
							stays.get(turkeyName).get(stays.get(turkeyName).size() - 1).setExitTime(lastChangeCal);
							stays.get(turkeyName).add(new ZoneStay(turkeyName, cZone, lastChangeCal));
						}

						if (zoneTimes.get(turkeyName).containsKey(cZone)) {
							zoneTimes.get(turkeyName).put(cZone, zoneTimes.get(turkeyName).get(cZone) + zoneTime);
						} else {
							zoneTimes.get(turkeyName).put(cZone, zoneTime);
						}

						zoneChanges.put(turkeyName, zoneChanges.get(turkeyName) + 1);
						lastZone.put(turkeyName, currentZone.get(turkeyName));
					} else {
						String lZone = lastZone.get(turkeyName);

						if (!zoneTimes.containsKey(turkeyName)) {
							zoneTimes.put(turkeyName, new HashMap<String, Integer>());
							zoneTimes.get(turkeyName).put(zone, TimeUtils.getMsOfDay(changeCal) - zoneTime);
							zoneChanges.put(turkeyName, 0);

							stays.put(turkeyName, new ArrayList<ZoneStay>());
							stays.get(turkeyName).add(new ZoneStay(turkeyName, zone, lastChangeCal));
						}

						if (zoneTimes.get(turkeyName).containsKey(lZone)) {
							zoneTimes.get(turkeyName).put(lZone, zoneTimes.get(turkeyName).get(lZone) + zoneTime);
						} else {
							zoneTimes.get(turkeyName).put(lZone, zoneTime);
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

		if (fillDay) {
			for (String turkey : zoneTimes.keySet()) {
				int zoneTime = (int) (TimeUtils.parseDate(date).getTimeInMillis() + (24 * 3600000)
						- lastZoneChange.get(turkey));

				String zone = currentZone.get(turkey);

				if (zoneTimes.get(turkey).containsKey(zone)) {
					zoneTimes.get(turkey).put(zone, zoneTimes.get(turkey).get(zone) + zoneTime);
				} else {
					zoneTimes.get(turkey).put(zone, zoneTime);
				}
			}
		}

		Pair<Map<String, Map<String, Integer>>, Map<String, Integer>> totals = new Pair<Map<String, Map<String, Integer>>, Map<String, Integer>>(
				zoneTimes, zoneChanges);

		return new Pair<Pair<Map<String, Map<String, Integer>>, Map<String, Integer>>, Map<String, List<ZoneStay>>>(
				totals, stays);
	}
}
