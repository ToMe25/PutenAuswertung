package com.tome25.auswertung;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.tome25.auswertung.stream.IInputStreamHandler;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.utils.Pair;

/**
 * The class handling the main data analysis/conversion.
 * 
 * @author theodor
 */
public class DataHandler {

	/**
	 * Does all the primary data handling.<br/>
	 * Reads the data from the given streams, and generates output based on it.<br/>
	 * Calculates all the expected data.
	 * 
	 * @param antennaStream The stream handler to read the antenna records from.
	 * @param turkeyStream  The stream handler to read
	 *                      <code>turkey id -> transponder ids</code> mappings from.
	 * @param zonesStream   The stream handler to read zone definitions from.
	 * @param outputStream  THe output stream handler to write the generated data
	 *                      to.
	 * @param fillDays      Whether the beginning and end of days should be filled
	 *                      with the same value as the closest record for each
	 *                      turkey.
	 */
	public static void handleStreams(IInputStreamHandler antennaStream, IInputStreamHandler turkeyStream,
			IInputStreamHandler zonesStream, IOutputStreamHandler outputStream, boolean fillDays) {
		Pair<Map<String, List<String>>, Map<String, String>> zones = CSVHandler.readMappingCSV(zonesStream);
		Pair<Map<String, List<String>>, Map<String, String>> turkeys = CSVHandler.readMappingCSV(turkeyStream);

		Map<String, TurkeyInfo> turkeyInfos = new TreeMap<>(new IntOrStringComparator());
		String lastDate = null;
		LogHandler.out_println(CSVHandler.turkeyCsvHeader(zones.getKey().keySet()));

		while (!antennaStream.done()) {
			AntennaRecord record = CSVHandler.readAntennaRecord(antennaStream);
			String turkeyId = record.transponder;
			if (turkeys.getValue().containsKey(record.transponder)) {
				turkeyId = turkeys.getValue().get(record.transponder);
			} else {
				LogHandler.err_println(String.format(
						"Received antenna record for unknown transponder id \"%s\" on day %s. Considering it a separate turkey.",
						record.date, record.transponder));
				LogHandler.print_debug_info("Antenna Record: %s, fillDays: %s", record, fillDays ? "true" : "false");
			}

			if (!zones.getValue().containsKey(record.antenna)) {
				LogHandler.err_println(String.format(
						"Received antenna record from unknown antenna id \"%s\". Skipping line.", record.antenna));
				LogHandler.print_debug_info("Antenna Record: %s, fillDays: %s", record, fillDays ? "true" : "false");
			}

			if (!record.date.equals(lastDate)) {
				if (lastDate != null) {
					printDayOutput(outputStream, turkeyInfos.values(), lastDate, zones.getKey().keySet());
				}
				lastDate = record.date;
			}

			if (!turkeyInfos.containsKey(turkeyId)) {
				turkeyInfos.put(turkeyId, new TurkeyInfo(turkeyId, turkeys.getKey().get(turkeyId),
						zones.getValue().get(record.antenna), record.date, record.tod, fillDays));
			} else {
				turkeyInfos.get(turkeyId).changeZone(zones.getValue().get(record.antenna), record.tod, record.date);
			}
		}

		printDayOutput(outputStream, turkeyInfos.values(), null, zones.getKey().keySet());

		try {
			outputStream.close();
		} catch (IOException e) {
			LogHandler.err_println("An exception occurred while closing the output stream handler.", true);
			LogHandler.print_exception(e, "close output stream handler", "Output Stream Handler: %s", outputStream);
		}
	}

	/**
	 * Generates the CSV output for all turkeys that have antenna records on the
	 * given date.<br/>
	 * Prints a line for each turkey that has been updated on the given day.<br/>
	 * Or all turkeys of used for total output.
	 * 
	 * @param output  The {@link IOutputStreamHandler} to write the generated data
	 *                to.
	 * @param turkeys A collection of all the turkeys that are known.
	 * @param date    The date for which to generate output. Set to {@code null} to
	 *                produce total output.
	 * @param zones   A collection containing the names of all the zones to write.
	 * @return The newly generated output.
	 */
	private static void printDayOutput(IOutputStreamHandler output, Collection<TurkeyInfo> turkeys, String date,
			Collection<String> zones) {
		// TODO add printAll printing all turkeys.
		// TODO handle generating output for previous days.
		// TODO handle generating temporary output.
		boolean total = date == null;
		for (TurkeyInfo ti : turkeys) {
			if (total || ti.getCurrentDate().equals(date)) {
				ti.endDay(ti.getCurrentDate());
				output.println(CSVHandler.turkeyToCsvLine(ti, date, zones));
			}
		}
	}

}
