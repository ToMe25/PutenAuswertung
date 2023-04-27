package com.tome25.auswertung;

import java.util.Calendar;
import java.util.Objects;

import com.tome25.auswertung.utils.TimeUtils;

/**
 * A class storing the data about a single time an antenna recorded a
 * transponder.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class AntennaRecord {

	/**
	 * The id of the transponder that the antenna recorded.
	 */
	public final String transponder;

	/**
	 * The string representation of the date(DD.MM.YYYY) at which this record was
	 * recorded.
	 */
	public final String date;

	/**
	 * The numerical id of the antenna in string form.
	 */
	public final String antenna;

	/**
	 * The time of day of this record in milliseconds.
	 */
	public final int tod;

	/**
	 * A {@link Calendar} encoding the date and time of day.
	 */
	public final Calendar cal;

	/**
	 * Creates a new AntennaRecord containing all the given data.
	 * 
	 * @param transponder The string id of the transponder that was recorded.
	 * @param date        The date at which this data was recorded. Format
	 *                    "DD.MM.YYYY".<br/>
	 *                    Shorter date, month, or year components will be prefixed
	 *                    with zeros.
	 * @param time        The time of day at which this record was created. Format
	 *                    "HH:MM:SS.2".
	 * @param antenna     The antenna that recorded this data set.
	 * @throws NullPointerException     If one of the arguments if {@code null}.
	 * @throws IllegalArgumentException If one of the arguments is empty, or the
	 *                                  {@code date} or {@code time} doesn't match
	 *                                  the required format.
	 */
	public AntennaRecord(String transponder, String date, String time, String antenna)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(transponder, "The transponder that was recorded can not be null.");
		Objects.requireNonNull(date, "The date the record was taken on can't be null.");
		Objects.requireNonNull(time, "The time at which the record was created cannot be null.");
		Objects.requireNonNull(antenna, "The antenna that recorded the record can't be null.");

		if (transponder.isEmpty()) {
			throw new IllegalArgumentException("The given transponder id was empty.");
		}
		this.transponder = transponder;

		if (date.isEmpty()) {
			throw new IllegalArgumentException("The given date string was empty.");
		}
		String dateSplit[] = date.split("\\.");
		if (dateSplit.length != 3) {
			throw new IllegalArgumentException("The date \"" + date + "\" did not match the required format.");
		} else if (dateSplit[0].isEmpty()) {
			throw new IllegalArgumentException("The day component of the date \"" + date + "\" was empty.");
		} else if (dateSplit[1].isEmpty()) {
			throw new IllegalArgumentException("The month component of the date \"" + date + "\" was empty.");
		} else if (dateSplit[2].isEmpty()) {
			throw new IllegalArgumentException("The year component of the date \"" + date + "\" was empty.");
		}

		StringBuilder dateBuilder = new StringBuilder();
		if (dateSplit[0].length() < 2) {
			dateBuilder.append('0');
		}
		dateBuilder.append(dateSplit[0]);
		dateBuilder.append('.');
		if (dateSplit[1].length() < 2) {
			dateBuilder.append('0');
		}
		dateBuilder.append(dateSplit[1]);
		dateBuilder.append('.');
		for (int i = dateSplit[2].length(); i < TimeUtils.YEAR_MIN_DIGITS; i++) {
			dateBuilder.append('0');
		}
		dateBuilder.append(dateSplit[2]);
		this.date = dateBuilder.toString();

		if (time.isEmpty()) {
			throw new IllegalArgumentException("The given time string was empty.");
		}

		if (antenna.isEmpty()) {
			throw new IllegalArgumentException("The given antenna id was empty.");
		}
		this.antenna = antenna;

		tod = (int) TimeUtils.parseTime(time);
		if (tod < 0) {
			throw new IllegalArgumentException("The time \"" + time + "\" represents the negative time of day " + tod
					+ ". Time of Day cannot be negative.");
		} else if (tod >= 24 * 3600000) {
			throw new IllegalArgumentException(
					"The time \"" + time + "\" is more than 23:59:59.99, which is not allowed.");
		}
		cal = TimeUtils.parseTime(date, tod);
	}

	/**
	 * Returns the string representation of the time of day at which this record was
	 * created.<br/>
	 * The format is HH:MM:SS.2, aka up to hundredths of a second.
	 * 
	 * @return The newly created time string.
	 */
	public String getTime() {
		return TimeUtils.encodeTime(tod);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AntennaRecord[transponder=");
		builder.append(transponder);
		builder.append(", antenna=");
		builder.append(antenna);
		builder.append(", date=");
		builder.append(date);
		builder.append(", tod ms=");
		builder.append(tod);
		builder.append(", tod=");
		builder.append(getTime());
		builder.append(", cal date=");
		builder.append(TimeUtils.encodeDate(cal));
		builder.append(", cal time=");
		builder.append(TimeUtils.encodeTime(TimeUtils.getMsOfDay(cal)));
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(antenna, date, tod, transponder, cal);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		AntennaRecord other = (AntennaRecord) obj;
		if (!Objects.equals(antenna, other.antenna)) {
			return false;
		}

		if (!Objects.equals(transponder, other.transponder)) {
			return false;
		}

		if (!Objects.equals(date, other.date)) {
			return false;
		}

		if (tod != other.tod) {
			return false;
		}

		if (!Objects.equals(cal, other.cal)) {
			return false;
		}

		return true;
	}

}
