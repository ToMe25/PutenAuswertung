package com.tome25.auswertung;

import java.util.Calendar;
import java.util.Objects;

import com.tome25.auswertung.utils.TimeUtils;

/**
 * A class storing a single stay in a zone of a single turkey.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class ZoneStay {

	/**
	 * The minimum length required for a stay in a zone without food to be
	 * recognized as unreliable.
	 */
	public static final int UNRELIABLE_TIME = 12 * 60 * 60 * 1000;

	/**
	 * The id of the turkey which spent the time record by this object in the given
	 * zone.
	 */
	private final String turkey;

	/**
	 * The zone in which the record time was spent.
	 */
	private final ZoneInfo zone;

	/**
	 * The date and time at which the zone was entered.
	 */
	private final Calendar entry;

	/**
	 * The time and date at which the zone was left.
	 */
	private Calendar exit;

	/**
	 * Creates a new ZoneStay with the given values, without an exit time.
	 * 
	 * @param turkey The turkey for which the zone stay is being recorded.
	 * @param zone   The zone in which the turkey spent the time recorded.
	 * @param entry  The time at which the turkey entered the zone.
	 * @throws NullPointerException     If {@code turkey}, {@code zone}, or
	 *                                  {@code entry} is {@code null}.
	 * @throws IllegalArgumentException If the exit time is before the entry time.
	 */
	public ZoneStay(String turkey, ZoneInfo zone, Calendar entry)
			throws NullPointerException, IllegalArgumentException {
		this(turkey, zone, entry, null);
	}

	/**
	 * Creates a new ZoneStay with the given values, without an exit time.
	 * 
	 * @param turkey The turkey for which the zone stay is being recorded.
	 * @param zone   The zone in which the turkey spent the time recorded.
	 * @param entry  The time at which the turkey entered the zone.
	 * @param exit   The time at which the turkey left the zone. Can be
	 *               {@code null}.
	 * @throws NullPointerException     If {@code turkey}, {@code zone}, or
	 *                                  {@code entry} is {@code null}.
	 * @throws IllegalArgumentException If the exit time isn't after the entry time.
	 */
	public ZoneStay(final String turkey, final ZoneInfo zone, final Calendar entry, Calendar exit)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(turkey, "The turkey spending the time can't be null.");
		Objects.requireNonNull(zone, "The zone in which the turkey spends its time can't be null.");
		Objects.requireNonNull(entry, "The time at which the turkey entered the zone can't be null.");

		if (exit != null && !exit.after(entry)) {
			throw new IllegalArgumentException("Exit time wasn't after entry time.");
		}

		this.turkey = turkey;
		this.zone = zone;
		this.entry = entry;
		this.exit = exit;
	}

	/**
	 * Sets the time at which the turkey left the zone recorded in this object.
	 * 
	 * @param exit The new exit time.
	 * @throws IllegalArgumentException If the new exit time is before the entry
	 *                                  time.
	 */
	public void setExitTime(Calendar exit) throws IllegalArgumentException {
		if (exit == null) {
			this.exit = null;
		} else if (!exit.after(entry)) {
			throw new IllegalArgumentException("Exit time wasn't after entry time.");
		} else {
			this.exit = exit;
		}
	}

	/**
	 * Gets the id of the turkey this stay record is for.
	 * 
	 * @return This object's turkey.
	 */
	public String getTurkey() {
		return turkey;
	}

	/**
	 * The zone the recorded time was spent in.
	 * 
	 * @return This object's zone.
	 */
	public ZoneInfo getZone() {
		return zone;
	}

	/**
	 * Gets a {@link Calendar} representing the time at which the turkey entered
	 * this zone.
	 * 
	 * @return The zone entry time.
	 */
	public Calendar getEntryCal() {
		return (Calendar) entry.clone();
	}

	/**
	 * Gets the string representation of the date at which the turkey entered the
	 * given zone.
	 * 
	 * @return The entry date.
	 */
	public String getEntryDate() {
		return TimeUtils.encodeDate(entry);
	}

	/**
	 * Gets the time of day in milliseconds at which the turkey entered the zone.
	 * 
	 * @return The entry time of day in ms.
	 */
	public int getEntryTime() {
		return TimeUtils.getMsOfDay(entry);
	}

	/**
	 * Gets a {@link Calendar} representing the time at which the turkey left this
	 * zone.
	 * 
	 * @return The zone exit time.
	 */
	public Calendar getExitCal() {
		if (exit == null) {
			return null;
		} else {
			return (Calendar) exit.clone();
		}
	}

	/**
	 * Gets the date at which the turkey left the zone.<br/>
	 * {@code null} if the exit time has not been set yet.
	 * 
	 * @return The zone exit date.
	 */
	public String getExitDate() {
		if (exit == null) {
			return null;
		} else {
			return TimeUtils.encodeDate(exit);
		}
	}

	/**
	 * The time of day at which the turkey left the zone in milliseconds.<br/>
	 * -1 if the exit time was not yet set.
	 * 
	 * @return The exit time of day in ms.
	 */
	public int getExitTime() {
		if (exit == null) {
			return -1;
		} else {
			return TimeUtils.getMsOfDay(exit);
		}
	}

	/**
	 * Gets the time in milliseconds that the turkey spent in the zone.
	 * 
	 * @return The zone time in ms.
	 * @throws IllegalStateException If the stay has no end time yet.
	 */
	public long getStayTime() throws IllegalStateException {
		if (!hasLeft()) {
			throw new IllegalStateException("ZoneStay has no exit time.");
		}

		return exit.getTimeInMillis() - entry.getTimeInMillis();
	}

	/**
	 * Checks whether the turkey has already left the zone.
	 * 
	 * @return {@code true} if the exit time has been set already.
	 */
	public boolean hasLeft() {
		return exit != null;
	}

	/**
	 * Checks whether this stay is considered unreliable.<br/>
	 * A stay is considered unreliable if
	 * <ol>
	 * <li>The zone it is in has no food, and</li>
	 * <li>The duration of the stay is more than 12 hours.</li>
	 * </ol>
	 * 
	 * @return Whether this stay is considered unreliable.
	 * @throws IllegalStateException If the stay has no end time yet.
	 */
	public boolean isUnreliable() throws IllegalStateException {
		if (!hasLeft()) {
			throw new IllegalStateException("ZoneStay has no exit time.");
		}

		// TODO this warning should be time since the last record in that zone.
		return !zone.hasFood() && getStayTime() > UNRELIABLE_TIME;
	}

	@Override
	public int hashCode() {
		return Objects.hash(entry, exit, turkey, zone);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ZoneStay other = (ZoneStay) obj;
		return Objects.equals(entry, other.entry) && Objects.equals(exit, other.exit)
				&& Objects.equals(turkey, other.turkey) && Objects.equals(zone, other.zone);
	}

	@Override
	public String toString() {
		return String.format(
				"ZoneStay[turkey=%s, zone=%s, zone has food=%s, entry date=%s, entry time=%s, exit date=%s, exit time=%s, is unreliable=%s]",
				turkey, zone.getId(), zone.hasFood() ? "true" : "false", getEntryDate(),
				TimeUtils.encodeTime(getEntryTime()), exit == null ? "null" : getExitDate(),
				exit == null ? "null" : TimeUtils.encodeTime(getExitTime()),
				exit == null ? "false" : (isUnreliable() ? "true" : "false"));
	}

}
