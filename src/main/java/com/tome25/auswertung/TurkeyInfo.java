package com.tome25.auswertung;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
	 * 1000 milliseconds per second.
	 */
	private static final int DAY_END = 24 * 60 * 60 * 1000;

	/**
	 * The minimum time a turkey has to spend in a zone for it to be counted at all.
	 */
	private static final int MIN_ZONE_TIME = 5 * 60 * 1000;

	/**
	 * Whether beginnings and ends of days where the adjacent day is not known
	 * should be assumed to be like the first/last record on that day.
	 */
	private final boolean fillDay;

	/**
	 * The string id of the turkey represented by this info object.
	 */
	private final String id;

	/**
	 * A list containing all transponders that represent this turkey.
	 */
	private final List<String> transponders;

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
	 * The date used for the the today values.
	 */
	private String currentDate;

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
	private int currentTime;

	/**
	 * The last time the turkey represented by this object changed the zone it is
	 * in.
	 */
	private int lastZoneChange;

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
	 * Creates a new TurkeyInfo object representing the state of a turkey.
	 * 
	 * @param id           The string id used to represent this turkey.
	 * @param transponders A list containing the string ids of the transponders
	 *                     tracking this turkey.
	 * @param currentZone  The zone this turkey is currently in.
	 * @param date         The day of the first record of this turkey. Used for
	 *                     current day time counting.
	 * @param time         The time of the first record of this turkey.
	 * @param fillDay      Whether beginnings and ends of days where the adjacent
	 *                     day is not known should be assumed to be like the
	 *                     first/last record on that day.
	 */
	public TurkeyInfo(String id, List<String> transponders, String currentZone, String date, int time,
			boolean fillDay) {
		this.id = id;
		this.transponders = transponders;
		this.currentZone = this.lastZone = currentZone;
		this.currentDate = date;
		this.currentTime = this.lastZoneChange = time;
		this.fillDay = fillDay;

		dayZoneTimes.put(date, new HashMap<String, Integer>());
		if (fillDay) {
			dayZoneTimes.get(date).put(currentZone, time);
			totalZoneTimes.put(currentZone, (long) time);
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
	 * @param date    The date of the antenna record.
	 */
	public void changeZone(String newZone, int time, String date) {
		Objects.requireNonNull(newZone, "The zone the turkey moved into cannot be null.");
		Objects.requireNonNull(date, "The date at which the change occurred cannot be null.");

		// FIXME handle fillDay being false
		if (date != currentDate) {
			endDay(date);
		}

		int timeSpent = time - currentTime;
		addDayTime(date, currentZone, timeSpent);
		addTotalTime(currentZone, timeSpent);

		currentTime = time;

		if (newZone != currentZone) {
			int zoneTime = time - lastZoneChange;
			if (zoneTime < MIN_ZONE_TIME) {// TODO disable with argument
				// FIXME handle day borders
				addDayTime(date, currentZone, -zoneTime);
				addTotalTime(currentZone, -zoneTime);
				addDayTime(date, lastZone, zoneTime);
				addTotalTime(lastZone, zoneTime);

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

			lastZoneChange = time;
			currentZone = newZone;
		}
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
	 */
	private void addDayTime(String day, String zone, int time) {
		Objects.requireNonNull(day, "The day to add zone time to cannot be null.");
		Objects.requireNonNull(zone, "The zone to add time to cannot be null.");

		if (dayZoneTimes.get(day).containsKey(zone)) {
			dayZoneTimes.get(day).put(zone, Math.max(0, dayZoneTimes.get(day).get(zone) + time));
		} else {
			dayZoneTimes.get(day).put(zone, Math.max(0, time));
		}
	}

	/**
	 * Adds the given amount of time to the totalZoneTime for the given zone.<br/>
	 * Does not check whether the time to be added is positive, but does not allow
	 * for a negative total.
	 * 
	 * @param zone The zone in which the time was spent.
	 * @param time The amount of time that was spent in the given zone.
	 */
	private void addTotalTime(String zone, int time) {
		Objects.requireNonNull(zone, "The zone to add time to cannot be null.");

		if (totalZoneTimes.containsKey(zone)) {
			totalZoneTimes.put(zone, Math.max(0, totalZoneTimes.get(zone) + time));
		} else {
			totalZoneTimes.put(zone, (long) Math.max(0, time));
		}
	}

	/**
	 * Sets the current time for this object to the end of the current day, or the
	 * beginning of the given day.<br/>
	 * 
	 * If date is the current day it sets the time to the end of that day.<br/>
	 * Otherwise sets it to the start of the given day.<br/>
	 * 
	 * Adds the remaining time of the old day to the current zones zone time if
	 * fillDay is {@code true}.
	 * 
	 * @param date The date to which this object should be set.
	 */
	public void endDay(String date) {
		Objects.requireNonNull(date, "Date can't be null.");

		if (fillDay) {
			changeZone(currentZone, DAY_END, currentDate);
			if (!currentDate.equals(date)) {
				dayZoneChanges.put(currentDate, todayZoneChanges);
				if (!dayZoneTimes.containsKey(date)) {
					dayZoneTimes.put(date, new HashMap<String, Integer>());
				}
				todayZoneChanges = 0;
				currentTime = 0;
				currentDate = date;
				lastZoneChange = 0;
			}
		} else {
			// TODO implement this
			LogHandler.err_println("Trying to move to a later date without fillDay. This isn't implemented yet.");
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
		return currentDate;
	}

	/**
	 * Returns the time the turkey spent in each zone on the given day.<br/>
	 * Returns null if there are no records for the given date.<br/>
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
	 */
	public int getDayZoneChanges(String date) {
		if (date.equals(currentDate)) {
			return todayZoneChanges;
		} else if (dayZoneChanges.containsKey(date)) {
			return dayZoneChanges.get(date);
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

}
