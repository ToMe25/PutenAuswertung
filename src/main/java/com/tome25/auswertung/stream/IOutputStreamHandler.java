package com.tome25.auswertung.stream;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

import com.tome25.auswertung.TurkeyInfo;

/**
 * The interface defining the common methods for all data Output handling.<br/>
 * Currently planned are SystemOut(writing to the system output), File(writing
 * to a FileOutputStream), and Multi(printing to multiple others).
 * 
 * @author theodor
 */
public interface IOutputStreamHandler extends Closeable {

	/**
	 * Writes the given string as a separate line to this output handler.<br/>
	 * Basically writes the string and a line separator to the output stream.<br/>
	 * Writing temporary data to a stream that does not handle it may cause a log
	 * entry, may not however throw an exception.<br/>
	 * 
	 * Treats the string as non temporary.
	 * 
	 * @param line The line to write.
	 * @return Whether the string was actually written to the output stream.
	 */
	boolean println(String line);

	/**
	 * Writes the given string as a separate line to this output handler.<br/>
	 * Basically writes the string and a line separator to the output stream.<br/>
	 * Writing temporary data to a stream that does not handle it may cause a log
	 * entry, may not however throw an exception.
	 * 
	 * @param line      The line to write.
	 * @param temporary Whether the output is to be considered temporary.<br/>
	 *                  Data is considered temporary if it is not the data
	 *                  representing a full day.<br/>
	 *                  Some handlers may not write temporary data.
	 * @return Whether the string was actually written to the output stream.
	 */
	boolean println(String line, boolean temporary);

	/**
	 * Writes the info about the last day of the given turkey info to the output
	 * stream handled by this object.<br/>
	 * Data is handled as temporary if the current date of the turkey info is date,
	 * and its current time isn't the last ms of the day.
	 * 
	 * @param info  The turkey info for which to write data.
	 * @param date  The date to write the data for. Set to {@code null} to write
	 *              total data.
	 * @param zones A collection containing the names of all the zones to
	 *              write.<br/>
	 *              Used for ordering, selective printing, or printing zones this
	 *              turkey did not enter.<br/>
	 *              If this is {@code null} or empty the potentially incomplete or
	 *              unordered zone list from the turkey is used instead.
	 * @return Whether the info was actually written to the output stream.
	 */
	boolean printDay(TurkeyInfo info, String date, Collection<String> zones);

	/**
	 * Checks whether this output stream handler prints temporary output.<br/>
	 * Trying to print temporary input if this returns {@code false} is
	 * required to be ignored, may however cause a log message.
	 * 
	 * @return Whether or not this output handler writes temporary output.
	 */
	boolean printsTemporary();

	/**
	 * Flushes the underlying stream handled by this output stream handler.
	 * 
	 * @throws IOException If the underlying stream throwns an exception while
	 *                     flushing.
	 */
	void flush() throws IOException;

}
