package com.tome25.auswertung;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.utils.IntOrStringComparator;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * The class storing the current state of a single turkey.<br/>
 * Contains info about the current day and all days.<br/>
 * <br/>
 * The natural ordering for {@link TurkeyInfo} objects is to sort them by their
 * {@link #getId() id} using an {@link IntOrStringComparator}.<br/>
 * <br/>
 * Note: this class has a natural ordering that is inconsistent with equals.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class TurkeyInfo implements Comparable<TurkeyInfo> {

	/**
	 * The last possible timestamp of a day.<br/>
	 * 24 hours per day times 60 minutes per hour times 60 seconds per minute times
	 * 1000 milliseconds per second.<br/>
	 * Minus one, since 24:00:00.00 isn't a valid time.
	 */
	public static final int DAY_END = TimeUtils.DAY_MS - 1;

	/**
	 * The string id of the turkey represented by this info object.
	 */
	private final String id;

	/**
	 * The {@link Arguments} object storing the configuration to be used by this
	 * object.
	 */
	private final Arguments args;

	/**
	 * A list containing all transponders that represent this turkey.
	 */
	private final List<String> transponders;

	/**
	 * The output stream handler to write {@link ZoneStay ZoneStays} to after
	 * finishing them.
	 */
	private IOutputStreamHandler stayOut;

	/**
	 * The time at which records on the next day not immediately following another
	 * recorded one should start.<br/>
	 * Set to {@code null} to start at midnight.
	 */
	private Calendar startTime;

	/**
	 * The time at which this turkey stops existing.<br/>
	 * Use {@code null} if the end time is unknown.
	 */
	private final Calendar endTime;

	/**
	 * A map containing a map containing the time this turkey spent in each recorded
	 * zone for each day.
	 */
	private Map<String, Map<String, Integer>> dayZoneTimes = new HashMap<String, Map<String, Integer>>();

	/**
	 * A map containing the time this turkey spent in each recorded zone in all the
	 * evaluated data.
	 */
	private Map<String, Long> totalZoneTimes = new HashMap<String, Long>();

	/**
	 * A set containing all the dates at which this turkeys data was unreliable.
	 */
	private Set<String> unreliableDays = new HashSet<String>();

	/**
	 * The string name of the zone the turkey is currently in.
	 */
	private ZoneInfo currentZone;

	/**
	 * Whether the last zone change should be recorded to the current zone stay.
	 */
	private boolean updateStay;

	/**
	 * A {@link ZoneStay} representing where the turkey is currently counted as
	 * staying it, and since when.<br/>
	 * Written to {@code stayOut} once the turkey switches zones.
	 */
	private ZoneStay lastStay;

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
	 * @param stayOut      The stream handler to write {@link ZoneStay ZoneStays} to
	 *                     after they are finished.<br/>
	 *                     Settings this to {@code null} will prevent zone stays
	 *                     from being written.
	 * @param currentZone  The zone this turkey is currently in.<br/>
	 *                     Can only be {@code null} if {@code date} is also
	 *                     {@code null}.
	 * @param time         The time of the first record of this turkey.<br/>
	 *                     Both the date and the time of day.<br/>
	 *                     Set to {@code null} to mark as not yet known.
	 * @param startTime    The time of the first recorded day at which the records
	 *                     should start.<br/>
	 *                     Can only be {@code null} if {@link Arguments#fillDays
	 *                     args.fillDays} is {@code true}.
	 * @param endTime      The time at which the records for this turkey should
	 *                     end.<br/>
	 *                     If {@code null} the records end at the end of the
	 *                     recording.<br/>
	 *                     This is either the start of the next downtime,<br/>
	 *                     if {@link Arguments#fillDays args.fillDays} is
	 *                     {@code true} at the end of the last day, or<br/>
	 *                     at the time of the last record of any turkey if it is
	 *                     {@code false}.
	 * @param args         An {@link Arguments} instance containing the
	 *                     configuration for the current data analysis.
	 * @throws NullPointerException     If {@code id}, {@code transponders}, any
	 *                                  transponder id, or {@code args} is
	 *                                  {@code null}.<br/>
	 *                                  Also if {@code time} isn't {@code null}, but
	 *                                  {@code currentZone} is {@code null}.<br/>
	 *                                  As well as if {@code time} isn't
	 *                                  {@code null}, {@link Arguments#fillDays
	 *                                  args.fillDays} is {@code false} and
	 *                                  {@code startTime} is {@code null}.
	 * @throws IllegalArgumentException If {@code time} is before {@code startTime},
	 *                                  {@code time} is after {@code endTime},
	 *                                  {@code id} doesn't match
	 *                                  {@link CSVHandler#ID_REGEX the required
	 *                                  format}, or a transponder id doesn't match
	 *                                  {@link CSVHandler#ID_REGEX the required
	 *                                  format}.
	 */
	public TurkeyInfo(final String id, final List<String> transponders, IOutputStreamHandler stayOut,
			ZoneInfo currentZone, Calendar time, Calendar startTime, final Calendar endTime, final Arguments args)
			throws NullPointerException, IllegalArgumentException {
		this.id = Objects.requireNonNull(id, "The turkey id cannot be null.");
		this.args = Objects.requireNonNull(args, "The args object configuring this cannot be null.");

		if (!CSVHandler.ID_REGEX.matcher(id).matches()) {
			throw new IllegalArgumentException("The turkey id \"" + id + "\" does not match the required format.");
		}

		// If time is null this is just used as a static storage object.
		if (time != null) {
			Objects.requireNonNull(currentZone, "The current zone cannot be null when the current time isn't null.");

			if (!args.fillDays) {
				Objects.requireNonNull(startTime, "The start time cannot be null of args.fillDays is false.");
			}

			if (startTime != null && time.before(startTime)) {
				throw new IllegalArgumentException("Current time cannot be before start time.");
			}

			if (endTime != null && time.after(endTime)) {
				throw new IllegalArgumentException("Current time cannot be after end time.");
			}
		}

		this.transponders = Objects.requireNonNull(transponders, "The transponder list cannot be null.");
		for (String transponder : transponders) {
			Objects.requireNonNull(transponder, "The transponder ids cannot be null.");
			if (!CSVHandler.ID_REGEX.matcher(transponder).matches()) {
				throw new IllegalArgumentException(
						"The transponder id \"" + transponder + "\" does not match the required format.");
			}
		}

		this.stayOut = stayOut;
		this.currentZone = currentZone;
		this.currentTime = time;
		this.startTime = startTime;
		this.endTime = endTime;
		this.lastZoneChange = time == null ? 0 : time.getTimeInMillis();

		if (time != null) {
			Calendar dayStart = new GregorianCalendar(time.get(Calendar.YEAR), time.get(Calendar.MONTH),
					time.get(Calendar.DATE));
			lastStay = new ZoneStay(id, currentZone, args.fillDays ? dayStart : startTime);

			dayZoneTimes.put(TimeUtils.encodeDate(time), new HashMap<String, Integer>());
			if (args.fillDays && currentZone != null) {
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
	 * @throws IllegalArgumentException If {@code time} is before
	 *                                  {@link #getCurrentCal() current time},
	 *                                  before {@link #getStartCal() start time}, or
	 *                                  after {@link #getEndCal() end time}.
	 */
	public void changeZone(final ZoneInfo newZone, final Calendar time)
			throws NullPointerException, IllegalArgumentException {
		changeZone(newZone, time, true);
	}

	/**
	 * Handles the turkey represented by this object being detected by an
	 * antenna.<br/>
	 * Adds time since the last zone change to zone time counters.<br/>
	 * Increments zone changes counters if the new zone doesn't match the old
	 * zone.<br/>
	 * Updates the current time and the current zone.
	 * 
	 * @param newZone    The zone by which this turkey was detected.
	 * @param time       The time at which the antenna record was created.
	 * @param updateStay Whether the last record time of the current stay should be
	 *                   updated.
	 * @throws NullPointerException     If {@code newZone} or {@code date} is
	 *                                  {@code null}.
	 * @throws IllegalArgumentException If {@code time} is before
	 *                                  {@link #getCurrentCal() current time},
	 *                                  before {@link #getStartCal() start time}, or
	 *                                  after {@link #getEndCal() end time}.
	 */
	private void changeZone(final ZoneInfo newZone, final Calendar time, boolean updateStay)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(newZone, "The zone the turkey moved into cannot be null.");
		Objects.requireNonNull(time, "The time at which the change occurred cannot be null.");

		if (currentTime != null && time.before(currentTime)) {
			throw new IllegalArgumentException("New time was before old time.");
		}

		if (startTime != null && time.before(startTime)) {
			throw new IllegalArgumentException("New time was before start time.");
		}

		if (endTime != null && time.after(endTime)) {
			throw new IllegalArgumentException("New time was after end time.");
		}

		if (currentTime != null && !TimeUtils.isSameDay(currentTime, time)) {
			endDay(time, args.fillDays && (startTime == null || currentTime.after(startTime)));
		}

		boolean newRec = true;
		if (currentTime != null) {
			newRec = !TimeUtils.isSameDay(currentTime, time) && !TimeUtils.isNextDay(currentTime, time);
		}

		if (startTime != null && currentTime != null
				&& (!args.fillDays || TimeUtils.isSameDay(startTime, time) || TimeUtils.isNextDay(startTime, time))) {
			newRec = startTime.after(currentTime);
		}

		long timeMs = time.getTimeInMillis();

		if (currentZone != null && !newRec) {
			long recordTime = timeMs - currentTime.getTimeInMillis();
			addTime(time, currentZone, recordTime);

			if (this.updateStay && !newRec && lastStay != null && lastStay.getZone().equals(currentZone)) {
				if (!currentZone.hasFood() && currentTime.getTimeInMillis()
						- lastStay.getLastRecordCal().getTimeInMillis() > ZoneStay.UNRELIABLE_TIME) {
					markDaysUnreliable(lastStay.getLastRecordCal(), currentTime);
				}
				lastStay.setLastRecord(currentTime);
			}
		} else if (currentTime == null && currentZone != null) {
			Calendar startCal = startTime == null ? null : (Calendar) startTime.clone();
			if (args.fillDays && (startTime == null || !TimeUtils.isSameDay(startTime, time))) {
				startCal = new GregorianCalendar(time.get(Calendar.YEAR), time.get(Calendar.MONTH),
						time.get(Calendar.DATE));
			}
			if (time.equals(startCal)) {
				currentZone = newZone;
			}

			long recordTime = timeMs - startCal.getTimeInMillis();
			addTime(time, currentZone, recordTime);
			if (!currentZone.equals(newZone)) {
				lastZoneChange = timeMs;
			} else {
				lastZoneChange = startCal.getTimeInMillis();
			}
			lastStay = new ZoneStay(id, currentZone, startCal);
		} else if (args.fillDays && !(startTime != null && TimeUtils.isSameDay(startTime, time))) {
			addTime(time, newZone, TimeUtils.getMsOfDay(time));

			if (newRec && stayOut != null && lastStay != null && !lastStay.hasLeft()) {
				Calendar lastCal = new GregorianCalendar();
				lastCal.setTimeInMillis(lastZoneChange);
				// FIXME probably produces 1ms offsets
				Calendar cal = (Calendar) lastCal.clone();
				cal.set(Calendar.HOUR_OF_DAY, 23);
				cal.set(Calendar.MINUTE, 59);
				cal.set(Calendar.SECOND, 59);
				cal.set(Calendar.MILLISECOND, 999);

				if (!currentZone.equals(lastStay.getZone()) && lastZoneChange < cal.getTimeInMillis()) {
					printCurrentStay(lastCal, false);
					lastStay = new ZoneStay(id, currentZone, lastCal, cal);
					printCurrentStay(null, false);
				} else if (currentTime.after(lastStay.getEntryCal())) {
					printCurrentStay(cal, false);
				}
			}

			currentZone = newZone;
			Calendar dayStart = new GregorianCalendar(time.get(Calendar.YEAR), time.get(Calendar.MONTH),
					time.get(Calendar.DATE));
			lastZoneChange = dayStart.getTimeInMillis();
			lastStay = new ZoneStay(id, newZone, dayStart);
		} else {
			long recordTime = timeMs - startTime.getTimeInMillis();
			addTime(time, newZone, recordTime);

			if (newRec && stayOut != null && lastStay != null && !lastStay.hasLeft()) {
				if (!currentZone.equals(lastStay.getZone()) && lastZoneChange < currentTime.getTimeInMillis()) {
					Calendar lastCal = new GregorianCalendar();
					lastCal.setTimeInMillis(lastZoneChange);
					if (!lastCal.equals(lastStay.getEntryCal())) {
						printCurrentStay(lastCal, false);
					}
					lastStay = new ZoneStay(id, currentZone, lastCal, currentTime);
					printCurrentStay(null, false);
				} else if (currentTime.after(lastStay.getEntryCal())) {
					printCurrentStay(currentTime, false);
				}
			}

			currentZone = newZone;
			lastZoneChange = startTime.getTimeInMillis();
			lastStay = new ZoneStay(id, newZone, (Calendar) startTime.clone());
		}

		Calendar lastTime = currentTime;
		currentTime = time;

		long zoneTime = timeMs - lastZoneChange;
		if (currentZone != null && !newZone.equals(currentZone)) {
			if (args.minTime > 0 && zoneTime < args.minTime * 1000) {
				addTime(time, currentZone, -zoneTime);
				addTime(time, lastStay.getZone(), zoneTime);

				if (zoneTime > TimeUtils.getMsOfDay(time)) {
					Calendar yesterday = (Calendar) time.clone();
					yesterday.add(Calendar.DATE, -1);
					String yesterdayDate = TimeUtils.encodeDate(yesterday);

					// FIXME not sure how to handle if it doesn't
					if (dayZoneChanges.containsKey(yesterdayDate)) {
						dayZoneChanges.put(yesterdayDate, Math.max(0, dayZoneChanges.get(yesterdayDate) - 1));
						todayZoneChanges++;
					}
				}

				if (lastStay.getZone().equals(newZone)) {
					todayZoneChanges--;
					totalZoneChanges--;
				} else if (lastStay.getZone().equals(currentZone)) {
					todayZoneChanges++;
					totalZoneChanges++;
				}
			} else {
				todayZoneChanges++;
				totalZoneChanges++;

				if (lastStay == null || !lastStay.getZone().equals(currentZone)) {
					Calendar lastCal = new GregorianCalendar();
					lastCal.setTimeInMillis(lastZoneChange);
					if (lastStay != null && stayOut != null) {
						printCurrentStay(lastCal, false);
					}
					lastStay = new ZoneStay(id, currentZone, lastCal);
					if (this.updateStay && lastTime != null) {
						lastStay.setLastRecord(lastTime);
						if (!currentZone.hasFood()
								&& lastTime.getTimeInMillis() - lastCal.getTimeInMillis() > ZoneStay.UNRELIABLE_TIME) {
							markDaysUnreliable(lastCal, lastTime);
						}
					}
				}
			}

			lastZoneChange = timeMs;
		} else if ((args.minTime <= 0 || zoneTime >= args.minTime * 1000) && !lastStay.getZone().equals(currentZone)) {
			Calendar lastCal = new GregorianCalendar();
			lastCal.setTimeInMillis(lastZoneChange);
			if (stayOut != null) {
				printCurrentStay(lastCal, false);
			}
			lastStay = new ZoneStay(id, currentZone, lastCal);
			if (this.updateStay && lastTime != null) {
				lastStay.setLastRecord(lastTime);
				if (!currentZone.hasFood()
						&& lastTime.getTimeInMillis() - lastCal.getTimeInMillis() > ZoneStay.UNRELIABLE_TIME) {
					markDaysUnreliable(lastCal, lastTime);
				}
			}
		}

		this.updateStay = updateStay;
		currentZone = newZone;
	}

	/**
	 * Adds the given amount of time to the dayZoneTime for the given zone and
	 * day.<br/>
	 * Automatically adds time to previous days, if necessary. Does not check
	 * whether the time to be added is positive, but does not allow for a negative
	 * resulting day zone time.
	 * 
	 * @param now  The day for which to add the time to the given zone.
	 * @param zone The zone in which the time was spent.
	 * @param time The amount of time that was spent in the given zone.
	 * @throws NullPointerException if {@code day} or {@code zone} is {@code null}.
	 */
	private void addTime(final Calendar now, final ZoneInfo zone, long time) throws NullPointerException {
		Objects.requireNonNull(now, "The day to add zone time to cannot be null.");
		Objects.requireNonNull(zone, "The zone to add time to cannot be null.");

		String date = TimeUtils.encodeDate(now);

		if (!dayZoneTimes.containsKey(date)) {
			dayZoneTimes.put(date, new HashMap<String, Integer>());
		}

		if (time > 0) {
			if (time <= TimeUtils.getMsOfDay(now)) {
				if (dayZoneTimes.get(date).containsKey(zone.getId())) {
					dayZoneTimes.get(date).put(zone.getId(), dayZoneTimes.get(date).get(zone.getId()) + (int) time);
				} else {
					dayZoneTimes.get(date).put(zone.getId(), (int) time);
				}
			} else {
				if (dayZoneTimes.get(date).containsKey(zone.getId())) {
					dayZoneTimes.get(date).put(zone.getId(),
							dayZoneTimes.get(date).get(zone.getId()) + TimeUtils.getMsOfDay(now));
				} else {
					dayZoneTimes.get(date).put(zone.getId(), TimeUtils.getMsOfDay(now));
				}

				Calendar previousDay = (Calendar) now.clone();
				long previousTime = time - TimeUtils.getMsOfDay(now);
				while (previousTime > 0) {
					previousDay.add(Calendar.DATE, -1);
					String previousDate = TimeUtils.encodeDate(previousDay);

					if (dayZoneTimes.containsKey(previousDate)) {
						if (dayZoneTimes.get(previousDate).containsKey(zone.getId())) {
							dayZoneTimes.get(previousDate).put(zone.getId(),
									dayZoneTimes.get(previousDate).get(zone.getId())
											+ (int) Math.min(DAY_END + 1, previousTime));
						} else {
							dayZoneTimes.get(previousDate).put(zone.getId(), (int) Math.min(DAY_END + 1, previousTime));
						}
					} else {
						dayZoneTimes.put(previousDate, new HashMap<String, Integer>());
						dayZoneTimes.get(previousDate).put(zone.getId(), (int) Math.min(DAY_END + 1, previousTime));
						if (!dayZoneChanges.containsKey(previousDate)) {
							dayZoneChanges.put(previousDate, 0);
						}
					}

					previousTime = Math.max(0, previousTime - DAY_END - 1);
				}
			}
		} else if (time < 0) {
			if (-time <= TimeUtils.getMsOfDay(now)) {
				if (dayZoneTimes.get(date).containsKey(zone.getId())) {
					dayZoneTimes.get(date).put(zone.getId(),
							Math.max(0, dayZoneTimes.get(date).get(zone.getId()) + (int) time));
				} else {
					dayZoneTimes.get(date).put(zone.getId(), 0);
				}
			} else {
				if (dayZoneTimes.get(date).containsKey(zone.getId())) {
					dayZoneTimes.get(date).put(zone.getId(),
							Math.max(0, dayZoneTimes.get(date).get(zone.getId()) - TimeUtils.getMsOfDay(now)));
				} else {
					dayZoneTimes.get(date).put(zone.getId(), 0);
				}

				Calendar yesterday = (Calendar) now.clone();
				yesterday.add(Calendar.DATE, -1);
				long yesterdayTime = time + TimeUtils.getMsOfDay(now);
				String yesterdayDate = TimeUtils.encodeDate(yesterday);

				// FIXME not sure how to handle if it doesn't
				if (dayZoneTimes.containsKey(yesterdayDate)) {
					if (dayZoneTimes.get(yesterdayDate).containsKey(zone.getId())) {
						dayZoneTimes.get(yesterdayDate).put(zone.getId(),
								(int) Math.max(0, dayZoneTimes.get(yesterdayDate).get(zone.getId()) + yesterdayTime));
					} else {
						dayZoneTimes.get(yesterdayDate).put(zone.getId(), 0);
					}
				}
			}
		}

		if (totalZoneTimes.containsKey(zone.getId())) {
			totalZoneTimes.put(zone.getId(), Math.max(0, totalZoneTimes.get(zone.getId()) + time));
		} else {
			totalZoneTimes.put(zone.getId(), (long) Math.max(0, time));
		}
	}

	/**
	 * If {@link Arguments#fillDays args.fillDays} is {@code true}: Sets the time to
	 * the end of the current day, and adds the remaining time of the day to the
	 * current zone.<br/>
	 * <br/>
	 * If the given day is not the current day: Initializes some things for the new
	 * day.
	 * 
	 * @param date The date to which this object should be set.
	 * @throws NullPointerException if {@code date} is {@code null}.
	 */
	public void endDay(String date) throws NullPointerException {
		endDay(TimeUtils.parseDate(date));
	}

	/**
	 * If {@link Arguments#fillDays args.fillDays} is {@code true}: Sets the time to
	 * the end of the current day, and adds the remaining time of the day to the
	 * current zone.<br/>
	 * <br/>
	 * If the given day is not the current day: Initializes some things for the new
	 * day.
	 * 
	 * @param time The date to which this object should be set.
	 * @throws NullPointerException if {@code time} is {@code null}.
	 */
	public void endDay(Calendar time) throws NullPointerException {
		endDay(time, args.fillDays);
	}

	/**
	 * If {@code changeTime} is {@code true}: Sets the time to the end of the
	 * current day, and adds the remaining time of the day to the current zone.<br/>
	 * <br/>
	 * If the given day is not the current day: Initializes some things for the new
	 * day.
	 * 
	 * @param time       The date to which this object should be set.
	 * @param changeTime Whether a zone change at the end of the current day should
	 *                   be simulated.
	 * @throws NullPointerException if {@code time} is {@code null}.
	 */
	public void endDay(Calendar time, boolean changeTime) throws NullPointerException {
		Objects.requireNonNull(time, "Time can't be null.");

		if (changeTime) {
			// FIXME probably produces 1ms offsets
			Calendar cal = (Calendar) currentTime.clone();
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
			if (cal.after(currentTime)) {
				changeZone(currentZone, cal, false);
			}
		}

		if (!TimeUtils.isSameDay(currentTime, time)) {
			String today = TimeUtils.encodeDate(currentTime);
			if (!dayZoneChanges.containsKey(today)) {
				dayZoneChanges.put(today, todayZoneChanges);
			} else {
				dayZoneChanges.put(today, dayZoneChanges.get(today) + todayZoneChanges);
			}

			String date = TimeUtils.encodeDate(time);
			if (!dayZoneTimes.containsKey(date)) {
				dayZoneTimes.put(date, new HashMap<String, Integer>());
			}
			todayZoneChanges = 0;
		}
	}

	/**
	 * Sets the exit time of the {@link #lastStay current stay} and prints it to the
	 * output file.
	 * 
	 * @param temporary Whether the stay should be printed as temporary output.
	 * @throws NullPointerException If the {@link #lastStay current stay} is
	 *                              {@code null}.
	 */
	public void printCurrentStay(boolean temporary) throws NullPointerException {
		if (stayOut == null) {
			return;
		}

		Objects.requireNonNull(lastStay, "The current stay cannot be null.");
		if (!currentZone.equals(lastStay.getZone()) && lastZoneChange < currentTime.getTimeInMillis()) {
			Calendar lastCal = new GregorianCalendar();
			lastCal.setTimeInMillis(lastZoneChange);
			printCurrentStay(lastCal, temporary);
			lastStay = new ZoneStay(id, currentZone, lastCal, currentTime);
			printCurrentStay(null, temporary);
		} else if (currentTime.after(lastStay.getEntryCal())) {
			Calendar oldExit = lastStay.getExitCal();
			lastStay.setExitTime(currentTime);
			if (!args.fillDays || oldExit == null) {
				printCurrentStay(null, temporary);
			}
		}
	}

	/**
	 * Prints the current {@link #lastStay} to the {@link #stayOut stays output
	 * stream}.<br/>
	 * Also sets the stay exit time if {@code exitCal} isn't {@code null}.<br/>
	 * Does nothing if {@link #stayOut} is {@code null}.
	 * 
	 * @param exitCal   The exit time for the stay to print. Optional if it already
	 *                  has one.
	 * @param temporary Whether the stay should be printed as temporary output.
	 * @throws IllegalArgumentException If {@code exitCal} is before the
	 *                                  {@link ZoneStay#getEntryCal() entry time} of
	 *                                  the {@link #lastStay current stay}.
	 * @throws NullPointerException     If the {@link #lastStay current stay} is
	 *                                  {@code null}, or {@code exitCal} is
	 *                                  {@code null} and the {@link #lastStay
	 *                                  current stay} doesn't have an exit time yet.
	 */
	private void printCurrentStay(Calendar exitCal, boolean temporary)
			throws IllegalArgumentException, NullPointerException {
		if (stayOut == null) {
			return;
		}

		Objects.requireNonNull(lastStay, "The current stay cannot be null.");
		if (!lastStay.hasLeft()) {
			Objects.requireNonNull(exitCal,
					"The exit calendar cannot be null if the current stay does not have one yet.");
		}

		if (exitCal != null) {
			lastStay.setExitTime(exitCal);
		}

		if (!lastStay.getZone().hasFood() && lastStay.getExitCal().getTimeInMillis()
				- lastStay.getLastRecordCal().getTimeInMillis() > ZoneStay.UNRELIABLE_TIME) {
			markDaysUnreliable(lastStay.getLastRecordCal(), lastStay.getExitCal());
		}

		stayOut.println(CSVHandler.stayToCsvLine(lastStay), temporary);
	}

	/**
	 * Attempts to change the current time of this turkey to the given time.<br/>
	 * The time will be changed if these conditions are met:
	 * <ul>
	 * <li>The given time is after or equal to this turkeys {@link #getCurrentCal()
	 * current time}.</li>
	 * <li>This turkeys {@link #getCurrentCal() current time} is after or equal to
	 * this turkeys {@link #getStartCal() start time}.</li>
	 * <li>This turkeys {@link #getCurrentCal() current time} is before its
	 * {@link #getEndCal() end time}.</li>
	 * <li>If {@link Arguments#fillDays args.fillDays} is {@code true} and
	 * {@code time} is after this turkeys {@link #getEndCal() end time},
	 * {@code time} has to be on the same day as the {@link #getCurrentCal() current
	 * time}.</li>
	 * </ul>
	 * 
	 * If {@code time} is after this turkeys {@link #getEndCal() end time} its
	 * {@link #getCurrentCal() current time} is updated to that instead.
	 * 
	 * @param time The time to update this turkey to.
	 * @return {@code true} if this turkey was updated, {@code false} otherwise.
	 * @throws NullPointerException If {@code time} is {@code null}.
	 */
	public boolean tryUpdate(Calendar time) throws NullPointerException {
		Objects.requireNonNull(time, "The time to update to cannot be null.");

		if (currentTime == null || currentZone == null) {
			return false;
		}

		if (time.before(currentTime)) {
			return false;
		}

		if (startTime != null && currentTime.before(startTime)) {
			return false;
		}

		if (endTime != null && !currentTime.before(endTime)) {
			return false;
		}

		if (endTime != null && time.after(endTime)) {
			if (args.fillDays && !TimeUtils.isSameDay(currentTime, time)) {
				return false;
			}
			time = endTime;
		}

		changeZone(currentZone, time, false);
		return true;
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
		return new ArrayList<String>(transponders);
	}

	/**
	 * Returns the date for which the values of this object are currently
	 * calculated.
	 * 
	 * @return The current date of this object.
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
	 * Gets a copy of the {@link Calendar} representing the time for which this
	 * objects values are currently calculated.
	 * 
	 * @return The current time of this object.
	 */
	public Calendar getCurrentCal() {
		if (currentTime == null) {
			return null;
		} else {
			return (Calendar) currentTime.clone();
		}
	}

	/**
	 * Gets the zone this turkey is currently in.
	 * 
	 * @return The zone this turkey is currently in.
	 */
	public ZoneInfo getCurrentZone() {
		return currentZone;
	}

	/**
	 * Returns the time the turkey spent in each zone on the given day.<br/>
	 * Returns {@code null} if there are no records for the given date.<br/>
	 * Might not return the full day in one of these two cases:<br/>
	 * 1. {@link Arguments#fillDays args.fillDays} is set to true but this object is
	 * not yet set to the next day.<br/>
	 * 2. {@link Arguments#fillDays args.fillDays} is not set and this object did
	 * not receive a record of the next day.
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
		} else if (hasDay(date)) {
			return 0;
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
	 * @throws IllegalArgumentException If {@code date} is empty.
	 */
	public boolean hasDay(String date) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(date, "The date to check for can't be null.");

		if (date.trim().isEmpty()) {
			throw new IllegalArgumentException("The date to check for can't be empty.");
		}

		return dayZoneTimes.containsKey(date);
	}

	/**
	 * Checks whether this turkey has unreliable data.
	 * 
	 * @return {@code true} if this turkey has at least one day which is marked as
	 *         unreliable.
	 * 
	 * @see #isDayUnreliable(String)
	 * @see #markDaysUnreliable(Calendar, Calendar)
	 */
	public boolean hasUnreliableDay() {
		return !unreliableDays.isEmpty();
	}

	/**
	 * Checks whether the data for a given day is considered unreliable.
	 * 
	 * @param date The day for which to check.
	 * @return {@code true} if the data for the given day is considered unreliable.
	 * @throws NullPointerException     If {@code date} is {@code null}.
	 * @throws IllegalArgumentException If {@code date} is empty.
	 * 
	 * @see #hasUnreliableDay()
	 * @see #markDaysUnreliable(Calendar, Calendar)
	 */
	public boolean isDayUnreliable(String date) throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(date, "The date to check can't be null.");

		if (date.trim().isEmpty()) {
			throw new IllegalArgumentException("The date to check can't be empty.");
		}

		return unreliableDays.contains(date);
	}

	/**
	 * Marks all the days from the start time to the end time as unreliable.
	 * 
	 * @param start The {@link Calendar} representing the start of the unreliable
	 *              segment.
	 * @param end   The {@link Calendar} representing the end of the unreliable
	 *              segment.
	 * @throws NullPointerException     If one of the arguments is {@code null}.
	 * @throws IllegalArgumentException If the end time is before the start time.
	 * 
	 * @see #hasUnreliableDay()
	 * @see #isDayUnreliable(String)
	 */
	private void markDaysUnreliable(final Calendar start, final Calendar end)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(start, "The start time cannot be null.");
		Objects.requireNonNull(end, "The end time cannot be null.");
		if (end.before(start)) {
			throw new IllegalArgumentException("End time cannot be before start time.");
		}

		unreliableDays.add(TimeUtils.encodeDate(start));
		if (!TimeUtils.isSameDay(start, end)) {
			Calendar day = new GregorianCalendar(start.get(Calendar.YEAR), start.get(Calendar.MONTH),
					start.get(Calendar.DATE));
			day.add(Calendar.DATE, 1);
			while (day.before(end)) {
				unreliableDays.add(TimeUtils.encodeDate(day));
				day.add(Calendar.DATE, 1);
			}
		}
	}

	/**
	 * Gets a {@link Calendar} representing the time at which the current, or next
	 * recording starts.
	 * 
	 * @return The time where the records begin.
	 */
	public Calendar getStartCal() {
		if (startTime == null) {
			return null;
		} else {
			return (Calendar) startTime.clone();
		}
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
	 * Gets a {@link Calendar} representing the time after which no more data should
	 * be recorded.
	 * 
	 * @return The time where the records should end.
	 */
	public Calendar getEndCal() {
		if (endTime == null) {
			return null;
		} else {
			return (Calendar) endTime.clone();
		}
	}

	/**
	 * Sets the output stream handler to write {@link ZoneStay ZoneStays} to.
	 * 
	 * @param stayOut The new stays output stream handler.
	 */
	public void setStayOut(IOutputStreamHandler stayOut) {
		this.stayOut = stayOut;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TurkeyInfo[id=");
		builder.append(id);
		builder.append(", args=");
		builder.append(args);
		builder.append(", transponders=");
		builder.append(transponders);
		builder.append(", stayOut=");
		builder.append(stayOut);
		builder.append(", startTimeDate=");
		builder.append(startTime == null ? "null" : TimeUtils.encodeDate(startTime));
		builder.append(", startTimeOfDay=");
		builder.append(startTime == null ? "null" : TimeUtils.encodeTime(TimeUtils.getMsOfDay(startTime)));
		builder.append(", endTimeDate=");
		builder.append(endTime == null ? "null" : TimeUtils.encodeDate(endTime));
		builder.append(", endTimeOfDay=");
		builder.append(endTime == null ? "null" : TimeUtils.encodeTime(TimeUtils.getMsOfDay(endTime)));
		builder.append(", currentZone=");
		builder.append(currentZone);
		builder.append(", lastStay=");
		builder.append(lastStay);
		builder.append(", currentTimeDate=");
		builder.append(currentTime == null ? "null" : TimeUtils.encodeDate(currentTime));
		builder.append(", currentTimeOfDay=");
		builder.append(currentTime == null ? "null" : TimeUtils.encodeTime(TimeUtils.getMsOfDay(currentTime)));
		Calendar lastChange = new GregorianCalendar();
		lastChange.setTimeInMillis(lastZoneChange);
		builder.append(", lastZoneChangeDate=");
		builder.append(TimeUtils.encodeDate(lastChange));
		builder.append(", lastZoneChangeTimeOfDay=");
		builder.append(TimeUtils.encodeTime(TimeUtils.getMsOfDay(lastChange)));
		builder.append(", todayZoneChanges=");
		builder.append(todayZoneChanges);
		builder.append(", totalZoneChanges=");
		builder.append(totalZoneChanges);
		builder.append(", dayZoneTimes=");
		builder.append(dayZoneTimes);
		builder.append(", totalZoneTimes=");
		builder.append(totalZoneTimes);
		builder.append(", dayZoneChanges=");
		builder.append(dayZoneChanges);
		builder.append(", unreliableDays=");
		builder.append(unreliableDays);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(args, currentTime, currentZone, dayZoneChanges, dayZoneTimes, endTime, id, lastStay,
				lastZoneChange, startTime, stayOut, todayZoneChanges, totalZoneChanges, totalZoneTimes, transponders);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;

		if (getClass() != obj.getClass()) {
			return false;
		}

		TurkeyInfo other = (TurkeyInfo) obj;
		if (!Objects.equals(id, other.id) || !Objects.equals(transponders, other.transponders)) {
			return false;
		}

		if (lastZoneChange != other.lastZoneChange || todayZoneChanges != other.todayZoneChanges
				|| totalZoneChanges != other.totalZoneChanges) {
			return false;
		}

		return Objects.equals(args, other.args) && Objects.equals(currentTime, other.currentTime)
				&& Objects.equals(currentZone, other.currentZone)
				&& Objects.equals(dayZoneChanges, other.dayZoneChanges)
				&& Objects.equals(dayZoneTimes, other.dayZoneTimes) && Objects.equals(endTime, other.endTime)
				&& Objects.equals(lastStay, other.lastStay) && Objects.equals(startTime, other.startTime)
				&& Objects.equals(stayOut, other.stayOut) && Objects.equals(totalZoneTimes, other.totalZoneTimes);
	}

	/**
	 * Compares this object with the specified object for order. Returns a negative
	 * integer, zero, or a positive integer as this object is less than, equal to,
	 * or greater than the specified object.<br/>
	 * <br/>
	 * {@link TurkeyInfo} objects are compared by their {@link #getId() id}, using
	 * the {@link IntOrStringComparator}.<br/>
	 * <br/>
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 * 
	 * @param o The object to compare this object to.
	 * @return A negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 * @throws NullPointerException If {@code o} is {@code null}.
	 */
	@Override
	public int compareTo(TurkeyInfo o) throws NullPointerException {
		Objects.requireNonNull(o, "The object to compare this object to can't be null.");
		return IntOrStringComparator.INSTANCE.compare(id, o.getId());
	}

}
