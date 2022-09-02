package com.tome25.auswertung;

import java.util.Objects;

import com.tome25.auswertung.utils.TimeUtils;

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
	 * The time of day at which this record was recorded.<br/>
	 * The format is HH:MM:SS.2, aka up to hundredths of a second.
	 */
	public final String time;

	/**
	 * The numerical id of the antenna in string form.
	 */
	public final String antenna;

	/**
	 * The time of day of this record in milliseconds.
	 */
	public final int tod;

	/**
	 * Creates a new AntennaRecord containing all the given data.
	 * 
	 * @param transponder The string id of the transponder that was recorded.
	 * @param date        The date at which this data was recorded. Format
	 *                    "DD.MM.YYYY"
	 * @param time        The time of day at which this record was created. Format
	 *                    "HH:MM:SS.2".
	 * @param antenna     The antenna that recorded this data set.
	 * @throws NullPointerException     If one of the arguments if {@code null}.
	 * @throws IllegalArgumentException If parsing the time of day fails.
	 */
	public AntennaRecord(String transponder, String date, String time, String antenna)
			throws NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(transponder, "The transponder that was recorded can not be null.");
		Objects.requireNonNull(date, "The date the record was taken on can't be null.");
		Objects.requireNonNull(time, "The time at which the record was created cannot be null.");
		Objects.requireNonNull(antenna, "The antenna that recorded the record can't be null.");

		this.transponder = transponder;
		this.date = date;
		this.time = time;
		this.antenna = antenna;
		tod = (int) TimeUtils.parseTime(time);
	}

	@Override
	public String toString() {
		return String.format("AntennaRecord[transponder=%s, date=%s, time=%s, antenna=%s, time of day=%d]", transponder,
				date, time, antenna, tod);
	}

	@Override
	public int hashCode() {
		return Objects.hash(antenna, date, time, tod, transponder);
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

		if (!Objects.equals(date, other.date)) {
			return false;
		}

		if (!Objects.equals(time, other.time) || tod != other.tod) {
			return false;
		}

		if (!Objects.equals(transponder, other.transponder)) {
			return false;
		}

		return true;
	}

}
