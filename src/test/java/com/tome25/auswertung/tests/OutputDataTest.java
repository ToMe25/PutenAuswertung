package com.tome25.auswertung.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.DataHandler;
import com.tome25.auswertung.TurkeyInfo;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.FileOutputStreamHandler;
import com.tome25.auswertung.testdata.AntennaDataGenerator;
import com.tome25.auswertung.testdata.TurkeyGenerator;
import com.tome25.auswertung.testdata.ZoneGenerator;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;

/**
 * A class containing tests to verify the correctness of the generated output
 * data.
 * 
 * @author theodor
 */
public class OutputDataTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	@Test
	public void basic() throws IOException {
		Pair<FileInputStreamHandler, FileOutputStreamHandler> turkeysPair = tempFolder.newTempIOFile("turkeys.csv");
		FileOutputStreamHandler turkeysOut = turkeysPair.getValue();
		FileInputStreamHandler turkeysIn = turkeysPair.getKey();

		List<TurkeyInfo> turkeys = TurkeyGenerator.generateTurkeys(200, 5);
		CSVHandler.writeTurkeyCSV(turkeys, turkeysOut);
		turkeysOut.close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> zonesPair = tempFolder.newTempIOFile("zones.csv");
		FileOutputStreamHandler zonesOut = zonesPair.getValue();
		FileInputStreamHandler zonesIn = zonesPair.getKey();

		Map<String, List<String>> zones = ZoneGenerator.generateZones(5);
		CSVHandler.writeZonesCSV(zones, zonesOut);
		zonesOut.close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> antennaPair = tempFolder.newTempIOFile("antenna.csv");
		FileOutputStreamHandler antennaOut = antennaPair.getValue();
		FileInputStreamHandler antennaIn = antennaPair.getKey();
		Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> antennaData = AntennaDataGenerator
				.generateAntennaData(turkeys, zones, antennaOut, 10, true, true);
		Map<String, Map<String, Map<String, Long>>> antennaTimes = antennaData.getKey();
		Map<String, Map<String, Integer>> antennaChanges = antennaData.getValue();
		antennaOut.close();

		Pair<FileInputStreamHandler, FileOutputStreamHandler> outputPair = tempFolder.newTempIOFile("output.csv");
		FileOutputStreamHandler outputOut = outputPair.getValue();
		FileInputStreamHandler outputIn = outputPair.getKey();
		DataHandler.handleStreams(antennaIn, turkeysIn, zonesIn, outputOut, true);

		Pair<Map<String, Map<String, Map<String, Long>>>, Map<String, Map<String, Integer>>> outputData = CSVHandler
				.readTotalsCSV(outputIn);
		Map<String, Map<String, Map<String, Long>>> outputTimes = outputData.getKey();
		Map<String, Map<String, Integer>> outputChanges = outputData.getValue();

		for (String turkey : outputTimes.keySet()) {
			assertTrue("There was no zone changes count output for the turkey \"" + turkey + "\".",
					outputChanges.containsKey(turkey));
			assertTrue("There are no generated zone times for the turkey \"" + turkey + "\".",
					outputChanges.containsKey(turkey));
			assertTrue("There are no generated zone changes count for the turkey \"" + turkey + "\".",
					outputChanges.containsKey(turkey));

			for (String date : outputTimes.get(turkey).keySet()) {
				assertTrue("Day " + date + " not found in output changes for turkey \"" + turkey + "\".",
						outputChanges.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated times for turkey \"" + turkey + "\".",
						antennaTimes.get(turkey).containsKey(date));
				assertTrue("Day " + date + " not found in generated changes for turkey \"" + turkey + "\".",
						antennaChanges.get(turkey).containsKey(date));

				assertEquals("Zone changes for turkey \"" + turkey + "\" on day " + date + " didn't match.",
						antennaChanges.get(turkey).get(date), outputChanges.get(turkey).get(date));
			}
		}
	}

}
