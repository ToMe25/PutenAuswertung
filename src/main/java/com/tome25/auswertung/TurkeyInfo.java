package com.tome25.auswertung;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.tome25.auswertung.utils.TimeUtils;

/**
 * The class storing the current state of a single turkey.<br/>
 * Contains info about the current day and all days.
 * 
 * @author theodor
 */
public class TurkeyInfo {

	/**
	 * The last possible timestamp of a day.<br/>
	 * 24 hours per day times 60 minutes per hour times 60 seconds per minute times
	 * 1000 milliseconds per second.<br/>
	 * Minus one, since 24:00:00.00 isn't a valid time.
	 */
	public static final int DAY_END = 24 * 60 * 60 * 1000 - 1;

	/**
	 * The minimum time a turkey has to spend in a zone for it to be counted at all.
	 */
	public static final int MIN_ZONE_TIME = 5 * 60 * 1000;

	/**
	 * The string id of the turkey represented by this info object.
	 */
	private final String id;

	/**
	 * A list containing all transponders that represent this turkey.
	 */
	private final List<String> transponders;

	/**
	 * The time at which records on the next day not immediately following another
	 * recorded one should start.<br/>
	 * Set to {@code null} to start at midnight.
	 */
	private Calendar startTime;

	/**
	 * A map containing a map containing the time this turkey spent in each recorded
	 * zone for each day.
	 */
	private Map<String, Map<String, Integer>> dayZoneTimes = new HashMap<>();

	/**
	 * A map containing the time this turkey spent in each recorded zone in all the
	 * evaluated data.
	 */
	private Map<String, Long> totalZoneTimes = new HashMap<>();

	/**
	 * The string name of the zone the turkey is currently in.
	 */
	private String currentZone;

	/**
	 * The name of the zone this turkey was in before it entered the one it is
	 * currently in.<br/>
	 * Might match {@code currentZone} because of debouncing.
	 */
	private String lastZone;

	/**
	 * The time for which the other values are currently calculated.
	 */
	private Calendar currentTime;

	/**
	 * The last time the turkey represented by this object changed the zone it is
	 * in.
	 */
	private long lastZoneChange;

	/**
	 * The number of times the turkey changed the zone it is in today.
	 */
	private int todayZoneChanges = 0;

	/**
	 * The number the turkey represented by this object changed its current zone in
	 * all the evaluated data.
	 */
	private int totalZoneChanges = 0;

	/**
	 * A map containing the number of zone changes for this turkey, for each day
	 * before the current.
	 */
	private Map<String, Integer> dayZoneChanges = new HashMap<>();

	/**
	 * Creates a new TurkeyInfo object representing the state of a turkey.<br/>
	 * Set {@code date} to {@code null} to mark this turkey as not yet
	 * recorded.<br/>
	 * {@code currentZone} is handled as the start zone in this case, and can be
	 * {@code null} to represent the zone being unknown.
	 * 
	 * @param id           The string id used to represent this turkey.
	 * @param transponders A list containing the string ids of the transponders
	 *                     tracking this turkey.
	 * @param currentZone  The zone this turkey is currently in.<br/>
	 *                     Can only be {@code null} if {@code date} is also
	 *                     {@code null}.
	 * @param time         The time of the first record of this turkey.<br/>
	 *                     Both the date and the time of day.<br/>
	 *                     Set to {@code null} to mark as not yet known.
	 * @param startTime    The time of the first recorded day at which the records
	 *                     should start.<br/>
	 *                     Set to {@code null} to start at midnight of the first day
	 *                     at which they are recorded.
	 */
	public TurkeyInfo(String id, List<String> transponders, String currentZone, Calendar time, Calendar startTime) {
		if (time != null && (currentZone == null || currentZone.trim().isEmpty())) {
			throw new NullPointerException("The current zone cannot be null when the date isn't null.");
		}

		if (currentZone != null) {
			currentZone = currentZone.trim();
		}

		this.id = id;
		this.transponders = transponders;
		this.currentZone = this.lastZone = currentZone;
		this.currentTime = time;
		this.lastZoneChange = time == null ? 0 : time.getTimeInMillis();
		this.startTime = startTime;

		if (time != null) {
			dayZoneTimes.put(TimeUtils.encodeDate(time), new HashMap<String, Integer>());
			if (fillDays() && currentZone != null) {
				addTime(time, currentZone, TimeUtils.getMsOfDay(time));
			} else if (currentZone != null) {
				addTime(time, currentZone, (int) (time.getTimeInMillis() - startTime.getTimeInMillis()));
			}
		}
	}

	/**
	 * Handles the turkey represented by this object being detected by an
	 * antenna.<br/>
	 * Adds time since the last zone change to zone time counters.<br/>
	 * Increments zone changes counters if the new zone doesn't match the old
	 * zone.<br/>
	 * Updates the current time and the current zone.
	 * 
	 * @param newZone The zone by which this turkey was detected.
	 * @param time    The time at which the antenna record was created.
	 * @throws NullPointerException     If {@code newZone} or {@code date} is
	 *                                  {@code null}.
	 * @throws IllegalArgumentException If the new time is before the current time.
	 */
	public void changeZone(String newZone, Calendar time) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(newZone, "The zone the turkey moved into cannot be null.");
		Objects.requireNonNull(time, "The time at which the change occurred cannot be null.");

		if (currentTime != null && time.before(currentTime)) {
			throw new IllegalArgumentException("New time was before old time.");
		}

		if (currentTime != null && !TimeUtils.isSameDay(currentTime, time)) {
			endDay(time);
		}

		boolean newRec = !TimeUtils.isSameDay(currentTime, time) && !TimeUtils.isNextDay(currentTime, time);

		long timeMs = time.getTimeInMillis();

		if (currentZone != null && !newRec) {
			int timeSpent = (int) (timeMs - currentTime.getTimeInMillis());
			addTime(time, currentZone, timeSpent);
		} else if (fillDays()) {
			addTime(time, newZone, TimeUtils.getMsOfDay(time));
			currentZone = lastZone = newZone;
			lastZoneChange = timeMs;
		} else {
			int recordTime = (int) (timeMs - startTime.getTimeInMillis());
			addTime(time, newZone, recordTime);
			currentZone = lastZone = newZone;
			lastZoneChange = timeMs;
		}

		currentTime = time;

		int zoneTime = -1;
		if (currentZone != null && !newZone.equals(currentZone)) {
			zoneTime = (int) (timeMs - lastZoneChange);
			if (zoneTime < MIN_ZONE_TIME) {// TODO disable with argument
				addTime(time, currentZone, -zoneTime);
				addTime(time, lastZone, zoneTime);

				if (lastZone.equals(newZone)) {
					todayZoneChanges--;
					totalZoneChanges--;
				} else if (lastZone.equals(currentZone)) {
					todayZoneChanges++;
					totalZoneChanges++;
				}
			} else {
				todayZoneChanges++;
				totalZoneChanges++;
				lastZone = currentZone;
			}

			lastZoneChange = timeMs;
		}
		currentZone = newZone;
	}

	/**
	 * Adds the given amount of time to the dayZoneTime for the given zone and
	 * day.<br/>
	 * Does not check whether the time to be added is positive, but does not allow
	 * for a negative resulting day zone time.
	 * 
	 * @param day  The day for which to add the time to the given zone.
	 * @param zone The zone in which the time was spent.
	 * @param time The amount of time that was spent in the given zone.
	 * @throws NullPointerException if {@code day} or {@code zone} is {@code null}.
	 */
	private void addTime(Calendar now, String zone, int time) throws NullPointerException {
		Objects.requireNonNull(now, "The day to add zone time to cannot be null.");
		Objects.requireNonNull(zone, "The zone to add time to cannot be null.");

		String date = TimeUtils.encodeDate(now);

		if (!dayZoneTimes.containsKey(date)) {
			dayZoneTimes.put(date, new HashMap<String, Integer>());
		}

		if (time > 0) {
			if (time <= TimeUtils.getMsOfDay(now)) {
				if (dayZoneTimes.get(date).containsKey(zone)) {
					dayZoneTimes.get(date).put(zone, dayZoneTimes.get(date).get(zone) + time);
				} else {
					dayZoneTimes.get(date).put(zone, time);
				}
			} else {
				if (dayZoneTimes.get(date).containsKey(zone)) {
					dayZoneTimes.get(date).put(zone, dayZoneTimes.get(date).get(zone) + TimeUtils.getMsOfDay(now));
				} else {
					dayZoneTimes.get(date).put(zone, TimeUtils.getMsOfDay(now));
				}

				Calendar yesterday = (Calendar) now.clone();
				yesterday.add(Calendar.DATE, -1);
				int yesterdayTime = time - TimeUtils.getMsOfDay(now);
				String yesterdayDate = TimeUtils.encodeDate(yesterday);

				// FIXME not sure how to handle if it isn't
				if (dayZoneTimes.containsKey(yesterdayDate)) {
					if (dayZoneTimes.get(yesterdayDate).containsKey(zone)) {
						dayZoneTimes.get(yesterdayDate).put(zone,
								dayZoneTimes.get(yesterdayDate).get(zone) + yesterdayTime);
					} else {
						dayZoneTimes.get(yesterdayDate).put(zone, yesterdayTime);
					}
				}
			}
		} else if (time < 0) {
			if (-time <= TimeUtils.getMsOfDay(now)) {
				if (dayZoneTimes.get(date).containsKey(zone)) {
					dayZoneTimes.get(date).put(zone, Math.max(0, dayZoneTimes.get(date).get(zone) + time));
				} else {
					dayZoneTimes.get(date).put(zone, 0);
				}
			} else {
				if (dayZoneTimes.get(date).containsKey(zone)) {
					dayZoneTimes.get(date).put(zone,
							Math.max(0, dayZoneTimes.get(date).get(zone) - TimeUtils.getMsOfDay(now)));
				} else {
					dayZoneTimes.get(date).put(zone, 0);
				}

				Calendar yesterday = (Calendar) now.clone();
				yesterday.add(Calendar.DATE, -1);
				int yesterdayTime = time + TimeUtils.getMsOfDay(now);
				String yesterdayDate = TimeUtils.encodeDate(yesterday);

				// FIXME not sure how to handle if it isn't
				if (dayZoneTimes.containsKey(yesterdayDate)) {
					if (dayZoneTimes.get(yesterdayDate).containsKey(zone)) {
						dayZoneTimes.get(yesterdayDate).put(zone,
								Math.max(0, dayZoneTimes.get(yesterdayDate).get(zone) + yesterdayTime));
					} else {
						dayZoneTimes.get(yesterdayDate).put(zone, 0);
					}
				}
			}
		}

		if (totalZoneTimes.containsKey(zone)) {
			totalZoneTimes.put(zone, Math.max(0, totalZoneTimes.get(zone) + time));
		} else {
			totalZoneTimes.put(zone, (long) Math.max(0, time));
		}
	}

	/**
	 * Sets the current time for this object to the end of the current day, or the
	 * beginning of the given day if {@code fillDay} is {@code true}.<br/>
	 * Does not do anything if {@code fillDay} is {@code false}.<br/>
	 * 
	 * If time is on the current day it sets the time to the end of that day.<br/>
	 * Otherwise sets it to the start of the given day.<br/>
	 * 
	 * Adds the remaining time of the old day to the current zones zone time.
	 * 
	 * @param date The date to which this object should be set.
	 * @throws NullPointerException if {@code date} is {@code null}.
	 */
	public void endDay(String date) throws NullPointerException {
		endDay(TimeUtils.parseDate(date));
	}

	/**
	 * Sets the current time for this object to the end of the current day, or the
	 * beginning of the given day if {@code fillDay} is {@code true}.<br/>
	 * 
	 * If time is on the current day it sets the time to the end of that day.<br/>
	 * Otherwise sets it to the start of the given day.<br/>
	 * 
	 * Adds the remaining time of the old day to the current zones zone time.
	 * 
	 * @param time The date to which this object should be set.
	 * @throws NullPointerException if {@code time} is {@code null}.
	 */
	public void endDay(Calendar time) throws NullPointerException {
		Objects.requireNonNull(time, "Time can't be null.");

		if (fillDays()) {
			// FIXME probably produces 1ms offsets
			Calendar cal = (Calendar) currentTime.clone();
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			changeZone(currentZone, cal);
		}

		if (!TimeUtils.isSameDay(currentTime, time)) {
			dayZoneChanges.put(TimeUtils.encodeDate(currentTime), todayZoneChanges);
			String date = TimeUtils.encodeDate(time);
			if (!dayZoneTimes.containsKey(date)) {
				dayZoneTimes.put(date, new HashMap<String, Integer>());
			}
			todayZoneChanges = 0;
		}
	}

	/**
	 * Gets the string id representing the turkey represented by this object.
	 * 
	 * @return the string id representing the turkey represented by this object.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns a list containing the string ids of all the transponders tracking the
	 * turkey represented by this object.
	 * 
	 * @return a list containing the string ids of all the transponders tracking the
	 *         turkey represented by this object.
	 */
	public List<String> getTransponders() {
		return new ArrayList<>(transponders);
	}

	/**
	 * Returns the date for which the values of this object are currently
	 * calculated.
	 * 
	 * @return the current date.
	 */
	public String getCurrentDate() {
		return TimeUtils.encodeDate(currentTime);
	}

	/**
	 * Returns the time for which the values of this object are currently
	 * calculated.
	 * 
	 * @return The current time of this object.
	 */
	public int getCurrentTime() {
		return TimeUtils.getMsOfDay(currentTime);
	}

	/**
	 * Gets the zone this turkey is currently in.
	 * 
	 * @return The zone this turkey is currently in.
	 */
	public String getCurrentZone() {
		return currentZone;
	}

	/**
	 * Returns the time the turkey spent in each zone on the given day.<br/>
	 * Returns {@code null} if there are no records for the given date.<br/>
	 * Might not return the full day in one of these two cases:<br/>
	 * 1. fillDay is set to true but this object is not yet set to the next
	 * day.<br/>
	 * 2. fillDay is not set and this object did not receive a record of the next
	 * day.
	 * 
	 * @param date The date for which to get the zone times.
	 * @return The zone times for the given date.
	 */
	public Map<String, Integer> getDayZoneTimes(String date) {
		return new HashMap<>(dayZoneTimes.get(date));
	}

	/**
	 * Returns the time the turkey spent in each zone for all recorded days.
	 * 
	 * @return A map of the zone name to the time spent in that zone.
	 */
	public Map<String, Long> getTotalZoneTimes() {
		return new HashMap<>(totalZoneTimes);
	}

	/**
	 * Get the number of zone changes for the turkey on the given date.<br/>
	 * Returns -1 if there is no record for the given date.
	 * 
	 * @param date The date for which to check.
	 * @return The number of zone changes on the given date.
	 * @throws NullPointerException     If {@code date} is {@code null}.
	 * @throws IllegalArgumentException If {@code date} can't be parsed as a date.
	 */
	public int getDayZoneChanges(String date) {
		Objects.requireNonNull(date, "The date to check for can't be null.");

		if (currentTime == null) {
			return -1;
		} else if (dayZoneChanges.containsKey(date)) {
			return dayZoneChanges.get(date);
		} else if (TimeUtils.isSameDay(currentTime, TimeUtils.parseDate(date))) {
			return todayZoneChanges;
		} else {
			return -1;
		}
	}

	/**
	 * Gets the total number of zone changes for the turkey represented by this
	 * object.
	 * 
	 * @return the total number of zone changes.
	 */
	public int getTotalZoneChanges() {
		return totalZoneChanges;
	}

	/**
	 * Returns {@code true} if this object holds data for the given day.
	 * 
	 * @param date The day for which to check.
	 * @return {@code true} if this object holds data for the given day.
	 * @throws NullPointerException     If {@code date} is {@code null}.
	 * @throws IllegalArgumentException If {@code date} can't be parsed as a date.
	 */
	public boolean hasDay(String date) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(date, "The date to check for can't be null.");

		return dayZoneTimes.containsKey(date);
	}

	/**
	 * Sets the time at which records should start the next time there is a new
	 * recording start.<br/>
	 * A recording start is when zone times are recorded, after days where there
	 * were no records.
	 * 
	 * @param startTime The time where the records should begin.
	 */
	public void setStartTime(Calendar startTime) {
		this.startTime = startTime;
	}

	/**
	 * Whether the time before the first and after the last record of the first/last
	 * day should be counted towards the first/last zone.
	 * 
	 * @return {@code true} if those times should be filled.
	 */
	private boolean fillDays() {
		return startTime == null;
	}

}
