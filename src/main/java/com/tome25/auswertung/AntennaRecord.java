package com.tome25.auswertung;

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
	 * @throws NumberFormatException if parsing the time of day failed.
	 */
	public AntennaRecord(String transponder, String date, String time, String antenna) throws NumberFormatException {
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

}
