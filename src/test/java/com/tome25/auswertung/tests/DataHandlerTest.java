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
import com.tome25.auswertung.ZoneStay;
import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.FileOutputStreamHandler;
import com.tome25.auswertung.testdata.AntennaDataGenerator;
import com.tome25.auswertung.testdata.TurkeyGenerator;
import com.tome25.auswertung.testdata.ZoneGenerator;
import com.tome25.auswertung.tests.OutputDataTest.TestMappings;
import com.tome25.auswertung.tests.OutputDataTest.TestResults;
import com.tome25.auswertung.tests.rules.ErrorLogRule;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;

/**
 * {@link DataHandler} tests that do not test the output data.
 * 
 * @author theodor
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
	 * @throws IOException
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
		Map<String, List<String>> zones = ZoneGenerator.generateZones(2, 2);
		CSVHandler.writeZonesCSV(zones, zoneCSV.getValue());

		Arguments args = Arguments.empty();
		args.fillDays = true;
		AntennaDataGenerator.generateAntennaData(turkeys, zones, dataCSV.getValue(), args, 5, true);

		DataHandler.handleStreams(dataCSV.getKey(), turkeyCSV.getKey(), zoneCSV.getKey(), totalsCSV.getKey(),
				staysCSV.getKey(), args);

		assertFalse("The totals output file was not empty after reading an empty turkey mappings file.",
				totalsCSV.getValue().ready());
		assertFalse("The stays output file was not empty after reading an empty turkey mappings file.",
				staysCSV.getValue().ready());

		errorLog.checkLine("Failed to read turkey mappings from the input file.", 0);
	}

	/**
	 * Tests how well {@link DataHandler#handleStreams} handles an empty zones csv.
	 * 
	 * @throws IOException
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
		Map<String, List<String>> zones = ZoneGenerator.generateZones(2, 2);

		Arguments args = Arguments.empty();
		args.fillDays = true;
		AntennaDataGenerator.generateAntennaData(turkeys, zones, dataCSV.getValue(), args, 5, true);

		DataHandler.handleStreams(dataCSV.getKey(), turkeyCSV.getKey(), zoneCSV.getKey(), totalsCSV.getKey(),
				staysCSV.getKey(), args);

		assertFalse("The totals output file was not empty after reading an empty zone mappings file.",
				totalsCSV.getValue().ready());
		assertFalse("The stays output file was not empty after reading an empty zone mappings file.",
				staysCSV.getValue().ready());

		errorLog.checkLine("Failed to read zone mappings from the input file.", 0);
	}

	/**
	 * Tests reading an antenna data csv that ends with an empty line.
	 * 
	 * @throws IOException If reading/writing/creating a temporary file fails.
	 */
	@Test
	public void readEmptyLastRecord() throws IOException {
		TestMappings mappings = OutputDataTest.generateTestMappings(5, 2, tempFolder);

		Pair<FileInputStreamHandler, FileOutputStreamHandler> dataCSV = tempFolder
				.newTempIOFile("empty_last_record_antennadata.csv");
		Arguments args = Arguments.empty();
		args.fillDays = true;
		Pair<Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>>, Map<String, List<ZoneStay>>> antennaData = AntennaDataGenerator
				.generateAntennaData(mappings.turkeys, mappings.zones, dataCSV.getValue(), args, 5, true);
		dataCSV.getValue().println(null);

		Pair<FileInputStreamHandler, FileOutputStreamHandler> totalsCSV = tempFolder
				.newTempIOFile("empty_last_record_totals.csv");
		Pair<FileInputStreamHandler, FileOutputStreamHandler> staysCSV = tempFolder
				.newTempIOFile("empty_last_record_stays.csv");

		DataHandler.handleStreams(dataCSV.getKey(), mappings.turkeysIn, mappings.zonesIn, totalsCSV.getValue(),
				staysCSV.getValue(), args);

		Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> outputTotals = CSVHandler
				.readTotalsCSV(totalsCSV.getKey());

		Map<String, List<ZoneStay>> outputStays = CSVHandler.readStaysCSV(staysCSV.getKey());

		TestResults results = new TestResults(antennaData.getKey().getKey(), outputTotals.getKey(),
				antennaData.getKey().getValue(), outputTotals.getValue(), antennaData.getValue(), outputStays);

		OutputDataTest.validateResults(results, args);

		errorLog.checkLine("Reading an antenna record from the input file failed.");
	}

	/**
	 * Tests reading an antenna data csv that ends with an invalid line.
	 * 
	 * @throws IOException If reading/writing/creating a temporary file fails.
	 */
	@Test
	public void readInvalidLastRecord() throws IOException {
		TestMappings mappings = OutputDataTest.generateTestMappings(5, 2, tempFolder);

		Pair<FileInputStreamHandler, FileOutputStreamHandler> dataCSV = tempFolder
				.newTempIOFile("invalid_last_record_antennadata.csv");

		Arguments args = Arguments.empty();
		args.fillDays = true;
		Pair<Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>>, Map<String, List<ZoneStay>>> antennaData = AntennaDataGenerator
				.generateAntennaData(mappings.turkeys, mappings.zones, dataCSV.getValue(), args, 5, true);
		dataCSV.getValue().println("Test;Data;Here");

		Pair<FileInputStreamHandler, FileOutputStreamHandler> totalsCSV = tempFolder
				.newTempIOFile("invalid_last_record_totals.csv");
		Pair<FileInputStreamHandler, FileOutputStreamHandler> staysCSV = tempFolder
				.newTempIOFile("invalid_last_record_stays.csv");

		DataHandler.handleStreams(dataCSV.getKey(), mappings.turkeysIn, mappings.zonesIn, totalsCSV.getValue(),
				staysCSV.getValue(), args);

		Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> outputTotals = CSVHandler
				.readTotalsCSV(totalsCSV.getKey());

		Map<String, List<ZoneStay>> outputStays = CSVHandler.readStaysCSV(staysCSV.getKey());

		TestResults results = new TestResults(antennaData.getKey().getKey(), outputTotals.getKey(),
				antennaData.getKey().getValue(), outputTotals.getValue(), antennaData.getValue(), outputStays);

		OutputDataTest.validateResults(results, args);

		errorLog.checkLine("Reading an antenna record from the input file failed.");
	}

}
