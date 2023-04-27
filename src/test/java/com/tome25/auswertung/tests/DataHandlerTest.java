package com.tome25.auswertung.tests;

import static org.junit.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.DataHandler;
import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.ZoneInfo;
import com.tome25.auswertung.ZoneStay;
import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.FileOutputStreamHandler;
import com.tome25.auswertung.testdata.AntennaDataGenerator;
import com.tome25.auswertung.testdata.AntennaDataGenerator.TestData;
import com.tome25.auswertung.testdata.TurkeyGenerator;
import com.tome25.auswertung.testdata.ZoneGenerator;
import com.tome25.auswertung.tests.OutputDataTest.TestMappings;
import com.tome25.auswertung.tests.rules.ErrorLogRule;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;

/**
 * {@link DataHandler} tests that do not test the output data.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class DataHandlerTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	@Rule
	public ErrorLogRule errorLog = new ErrorLogRule();

	/**
	 * Tests how well {@link DataHandler#handleStreams} handles an empty turkeys
	 * csv.
	 * 
	 * @throws IOException If reading/writing/creating a temporary file fails.
	 */
	@Test
	public void readEmptyTurkeys() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> turkeyCSV = tempFolder.newTempInputFile("empty_turkeys_turkeys.csv");
		Pair<FileInputStreamHandler, FileOutputStreamHandler> zoneCSV = tempFolder
				.newTempIOFile("empty_turkeys_zones.csv");
		Pair<FileInputStreamHandler, FileOutputStreamHandler> dataCSV = tempFolder
				.newTempIOFile("empty_turkeys_antenna_data.csv");

		Pair<FileOutputStreamHandler, BufferedReader> totalsCSV = tempFolder
				.newTempOutputFile("empty_turkeys_totals.csv");
		Pair<FileOutputStreamHandler, BufferedReader> staysCSV = tempFolder
				.newTempOutputFile("empty_turkeys_stays.csv");
		List<TurkeyInfo> turkeys = TurkeyGenerator.generateTurkeys(5, 2);
		List<ZoneInfo> zones = ZoneGenerator.generateZones(2, 2);
		CSVHandler.writeZonesCSV(zones, zoneCSV.getValue());

		Arguments args = Arguments.empty();
		args.fillDays = true;
		AntennaDataGenerator.generateAntennaData(turkeys, zones, dataCSV.getValue(), null, args, "12.02.2023", 5, true,
				true);

		DataHandler.handleStreams(dataCSV.getKey(), turkeyCSV.getKey(), zoneCSV.getKey(), null, totalsCSV.getKey(),
				staysCSV.getKey(), args);

		assertFalse("The totals output file was not empty after reading an empty turkey mappings file.",
				totalsCSV.getValue().ready());
		assertFalse("The stays output file was not empty after reading an empty turkey mappings file.",
				staysCSV.getValue().ready());

		errorLog.checkLine("Input file did not contain any data.");
		errorLog.checkLine("Failed to read turkey mappings from the input file.");
	}

	/**
	 * Tests how well {@link DataHandler#handleStreams} handles an empty zones csv.
	 * 
	 * @throws IOException If reading/writing/creating a temporary file fails.
	 */
	@Test
	public void readEmptyZones() throws IOException {
		Pair<FileInputStreamHandler, FileOutputStreamHandler> turkeyCSV = tempFolder
				.newTempIOFile("empty_zones_turkeys.csv");
		Pair<FileInputStreamHandler, PrintStream> zoneCSV = tempFolder.newTempInputFile("empty_zones_zones.csv");
		Pair<FileInputStreamHandler, FileOutputStreamHandler> dataCSV = tempFolder
				.newTempIOFile("empty_zones_antenna_data.csv");

		Pair<FileOutputStreamHandler, BufferedReader> totalsCSV = tempFolder
				.newTempOutputFile("empty_zones_totals.csv");
		Pair<FileOutputStreamHandler, BufferedReader> staysCSV = tempFolder.newTempOutputFile("empty_zones_stays.csv");
		List<TurkeyInfo> turkeys = TurkeyGenerator.generateTurkeys(5, 2);
		CSVHandler.writeTurkeyCSV(turkeys, turkeyCSV.getValue());
		List<ZoneInfo> zones = ZoneGenerator.generateZones(2, 2);

		Arguments args = Arguments.empty();
		args.fillDays = true;
		AntennaDataGenerator.generateAntennaData(turkeys, zones, dataCSV.getValue(), null, args, "05.11.2022", 5, true,
				true);

		DataHandler.handleStreams(dataCSV.getKey(), turkeyCSV.getKey(), zoneCSV.getKey(), null, totalsCSV.getKey(),
				staysCSV.getKey(), args);

		assertFalse("The totals output file was not empty after reading an empty zone mappings file.",
				totalsCSV.getValue().ready());
		assertFalse("The stays output file was not empty after reading an empty zone mappings file.",
				staysCSV.getValue().ready());

		errorLog.checkLine("Input file did not contain any data.");
		errorLog.checkLine("Failed to read zone mappings from the input file.");
	}

	/**
	 * Test reading an empty downtimes csv.
	 * 
	 * @throws IOException If reading/writing/creating a temporary file fails.
	 */
	@Test
	public void readEmptyDowntimes() throws IOException {
		TestMappings mappings = OutputDataTest.generateTestMappings(5, 2, 5, false, 0, 0, tempFolder);

		Pair<FileInputStreamHandler, FileOutputStreamHandler> dataCSV = tempFolder
				.newTempIOFile("empty_downtimes_antennadata.csv");

		Arguments args = Arguments.empty();
		args.fillDays = true;
		final TestData generated = AntennaDataGenerator.generateAntennaData(mappings.turkeys, mappings.zones,
				dataCSV.getValue(), null, args, "17.08.2022", 5, true, true);

		Pair<FileInputStreamHandler, PrintStream> downtimesCSV = tempFolder
				.newTempInputFile("empty_downtimes_downtimes.csv");
		downtimesCSV.getValue().close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> totalsCSV = tempFolder
				.newTempIOFile("empty_downtimes_totals.csv");
		Pair<FileInputStreamHandler, FileOutputStreamHandler> staysCSV = tempFolder
				.newTempIOFile("empty_downtimes_stays.csv");

		DataHandler.handleStreams(dataCSV.getKey(), mappings.turkeysIn, mappings.zonesIn, downtimesCSV.getKey(),
				totalsCSV.getValue(), staysCSV.getValue(), args);

		final Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> outputTotals = CSVHandler
				.readTotalsCSV(totalsCSV.getKey());
		final Map<String, List<ZoneStay>> outputStays = CSVHandler.readStaysCSV(staysCSV.getKey(), mappings.zones);
		final TestData parsed = new TestData(outputTotals.getKey(), outputTotals.getValue(), outputStays,
				generated.downtimes, mappings.turkeys, mappings.zones);

		OutputDataTest.validateResults(generated, parsed, args);

		errorLog.checkLine("Input file did not contain any data.");
		errorLog.checkLine("An Exception occurred while trying to read a downtimes file.");
	}

	/**
	 * Tests reading an antenna data csv that ends with an empty line.
	 * 
	 * @throws IOException If reading/writing/creating a temporary file fails.
	 */
	@Test
	public void readEmptyLastRecord() throws IOException {
		TestMappings mappings = OutputDataTest.generateTestMappings(5, 2, 5, false, 0, 0, tempFolder);

		Pair<FileInputStreamHandler, FileOutputStreamHandler> dataCSV = tempFolder
				.newTempIOFile("empty_last_record_antennadata.csv");
		Arguments args = Arguments.empty();
		args.fillDays = true;
		final TestData generated = AntennaDataGenerator.generateAntennaData(mappings.turkeys, mappings.zones,
				dataCSV.getValue(), null, args, "30.06.2023", 5, true, true);
		dataCSV.getValue().println(null);

		Pair<FileInputStreamHandler, FileOutputStreamHandler> totalsCSV = tempFolder
				.newTempIOFile("empty_last_record_totals.csv");
		Pair<FileInputStreamHandler, FileOutputStreamHandler> staysCSV = tempFolder
				.newTempIOFile("empty_last_record_stays.csv");

		DataHandler.handleStreams(dataCSV.getKey(), mappings.turkeysIn, mappings.zonesIn, null, totalsCSV.getValue(),
				staysCSV.getValue(), args);

		final Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> outputTotals = CSVHandler
				.readTotalsCSV(totalsCSV.getKey());
		final Map<String, List<ZoneStay>> outputStays = CSVHandler.readStaysCSV(staysCSV.getKey(), mappings.zones);
		final TestData parsed = new TestData(outputTotals.getKey(), outputTotals.getValue(), outputStays,
				generated.downtimes, mappings.turkeys, mappings.zones);

		OutputDataTest.validateResults(generated, parsed, args);

		errorLog.checkLine("Reading an antenna record from the input file failed.");
	}

	/**
	 * Tests reading an antenna data csv that ends with an invalid line.
	 * 
	 * @throws IOException If reading/writing/creating a temporary file fails.
	 */
	@Test
	public void readInvalidLastRecord() throws IOException {
		TestMappings mappings = OutputDataTest.generateTestMappings(5, 2, 5, false, 0, 0, tempFolder);

		Pair<FileInputStreamHandler, FileOutputStreamHandler> dataCSV = tempFolder
				.newTempIOFile("invalid_last_record_antennadata.csv");

		Arguments args = Arguments.empty();
		args.fillDays = true;
		final TestData generated = AntennaDataGenerator.generateAntennaData(mappings.turkeys, mappings.zones,
				dataCSV.getValue(), null, args, "22.02.2022", 5, true, true);
		dataCSV.getValue().println("Test;Data;Here");

		Pair<FileInputStreamHandler, FileOutputStreamHandler> totalsCSV = tempFolder
				.newTempIOFile("invalid_last_record_totals.csv");
		Pair<FileInputStreamHandler, FileOutputStreamHandler> staysCSV = tempFolder
				.newTempIOFile("invalid_last_record_stays.csv");

		DataHandler.handleStreams(dataCSV.getKey(), mappings.turkeysIn, mappings.zonesIn, null, totalsCSV.getValue(),
				staysCSV.getValue(), args);

		final Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> outputTotals = CSVHandler
				.readTotalsCSV(totalsCSV.getKey());
		final Map<String, List<ZoneStay>> outputStays = CSVHandler.readStaysCSV(staysCSV.getKey(), mappings.zones);
		final TestData parsed = new TestData(outputTotals.getKey(), outputTotals.getValue(), outputStays,
				generated.downtimes, mappings.turkeys, mappings.zones);

		OutputDataTest.validateResults(generated, parsed, args);

		errorLog.checkLine("Reading an antenna record from the input file failed.");
	}

}
