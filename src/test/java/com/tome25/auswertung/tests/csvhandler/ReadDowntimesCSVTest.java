package com.tome25.auswertung.tests.csvhandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.tome25.auswertung.CSVHandler;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.tests.rules.ErrorLogRule;
import com.tome25.auswertung.tests.rules.TempFileStreamHandler;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.TimeUtils;

import net.jcip.annotations.NotThreadSafe;

/**
 * A class containing the unit tests for {@link CSVHandler#readDowntimesCSV}.
 * 
 * @author theodor
 */
@NotThreadSafe
public class ReadDowntimesCSVTest {

	@Rule
	public TempFileStreamHandler tempFolder = new TempFileStreamHandler();

	@Rule
	public ErrorLogRule errorLog = new ErrorLogRule();

	/**
	 * A test reading and parsing a very basic downtimes file.
	 * 
	 * @throws IOException If reading, creating, or writing the temporary file
	 *                     fails.
	 */
	@Test
	public void readBasic() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("basic_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		pout.println("12.02.2021;01:03:12.54;12.02.2021;02:05:07.00");
		pout.println("13.02.2021;17:12:00.12;14.02.2021;05:00:00.00");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading a basic downtimes file returned null.", downtimes);
		assertEquals("Reading a downtimes file with two lines didn't return two pairs.", 2, downtimes.size());
		assertEquals("The first pair of a basic downtimes file didn't match.",
				new Pair<Long, Long>(1613091792540l, 1613095507000l), downtimes.get(0));
		assertEquals("The second pair of a basic downtimes file didn't match.",
				new Pair<Long, Long>(1613236320120l, 1613278800000l), downtimes.get(1));
	}

	/**
	 * A test parsing a somewhat longer basic downtimes csv.
	 * 
	 * @throws IOException If reading, creating, or writing the temporary file
	 *                     fails.
	 */
	@Test
	public void readLongBasic() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("long_basic_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		List<Pair<Long, Long>> refDowntimes = new ArrayList<Pair<Long, Long>>();
		long now = TimeUtils.parseDate("05.05.2022").getTimeInMillis();
		for (int i = 0; i < 150; i++) {
			long start = (now += 8319420);
			long end = (now += 5701560);

			Calendar startCal = new GregorianCalendar();
			startCal.setTimeInMillis(start);
			Calendar endCal = new GregorianCalendar();
			endCal.setTimeInMillis(end);
			refDowntimes.add(new Pair<Long, Long>(start, end));
			pout.printf("%s,%s,%s,%s%n", TimeUtils.encodeDate(start),
					TimeUtils.encodeTime(TimeUtils.getMsOfDay(startCal)), TimeUtils.encodeDate(end),
					TimeUtils.encodeTime(TimeUtils.getMsOfDay(endCal)));
		}

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading a longer basic downtimes csv returned null.", downtimes);
		assertEquals("The number of downtimes parsed from a longer basic downtimes csv didn't match.",
				refDowntimes.size(), downtimes.size());
		for (int i = 0; i < refDowntimes.size(); i++) {
			assertEquals("Downtime nr " + i + " didn't match.", refDowntimes.get(i), downtimes.get(i));
		}
	}

	/**
	 * A test reading a downtimes csv with a header line.
	 * 
	 * @throws IOException If reading, writing, or creating the temp file fails.
	 */
	@Test
	public void readHeader() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("header_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		pout.println("Start Date;Start Time;End Date;End Time");
		pout.println("15.11.2023;21:00:45.62;15.11.2023;23:54:12.39");
		pout.println("11.12.2023;05:31:53.81;31.12.2024;23:45:56.67");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading a downtimes csv with a header line returned null.", downtimes);
		assertEquals("The number of downtimes from a csv with a header line didn't match.", 2, downtimes.size());
		assertEquals("The first downtime of a downtimes csv with a header didn't match.",
				new Pair<Long, Long>(1700082045620l, 1700092452390l), downtimes.get(0));
		assertEquals("The second downtime of a downtimes csv with a header didn't match.",
				new Pair<Long, Long>(1702272713810l, 1735688756670l), downtimes.get(1));
	}

	/**
	 * A test parsing downtimes that aren't ordered correctly.
	 * 
	 * @throws IOException If reading, writing, or creating the temporary file
	 *                     fails.
	 */
	@Test
	public void readUnordered() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("unordered_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		pout.println("01.03.2022;03:05:12.01;05.03.2022;12:31:02.93");
		pout.println("01.01.2022;01:01:01.01;02.01.2022;23:21:53.00");
		pout.println("05.03.2022;13:13:02.00;06.04.2022;01:54:37.12");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading an unordered downtimes csv returned null.", downtimes);
		assertEquals("The number of downtimes parsed from an unordered downtimes csv didn't match.", 3,
				downtimes.size());
		assertEquals("The first downtime from an unordered file didn't match.",
				new Pair<Long, Long>(1640998861010l, 1641165713000l), downtimes.get(0));
		assertEquals("The second downtime from an unordered file didn't match.",
				new Pair<Long, Long>(1646103912010l, 1646483462930l), downtimes.get(1));
		assertEquals("The third downtime from an unordered file didn't match.",
				new Pair<Long, Long>(1646485982000l, 1649210077120l), downtimes.get(2));
	}

	/**
	 * A test parsing a downtimes csv with mixed separators.
	 * 
	 * @throws IOException If reading, writing, or creating the temp csv fails.
	 */
	@Test
	public void readMixedSeparators() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder
				.newTempInputFile("mixed_separator_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		pout.println("02.01.2022\t15:57:36.70,02.01.2022;17:01:51.00");
		pout.println("03.03.2022;19:27:21.15\t05.03.2022,06:35:51.87;");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading a downtimes csv with mixed separators returned null.", downtimes);
		assertEquals("The number of downtimes from a csv with mixed separators didn't match.", 2, downtimes.size());
		assertEquals("The first downtime from a csv with mixed separators didn't match.",
				new Pair<Long, Long>(1641139056700l, 1641142911000l), downtimes.get(0));
		assertEquals("The second downtime from a csv with mixed separators didn't match.",
				new Pair<Long, Long>(1646335641150l, 1646462151870l), downtimes.get(1));
	}

	/**
	 * A test reading an empty downtimes file.
	 * 
	 * @throws IOException If creating or reading the temporary file fails.
	 */
	@Test
	public void readEmpty() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("empty_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();

		assertTrue("File input stream handle for an empty file wasn't done.", fiin.done());
		assertNull("The result of reading an empty downtimes file was not null.", CSVHandler.readDowntimesCSV(fiin));

		errorLog.checkLine("Input file did not contain any data.", 0);
	}

	/**
	 * A test attempting to read a {@code null} downtimes file.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void readNull() throws NullPointerException {
		CSVHandler.readDowntimesCSV(null);
	}

	/**
	 * A test parsing a csv containing an invalid date.
	 * 
	 * @throws IOException If reading, creating, or writing the temp file fails.
	 */
	@Test
	public void readInvalidDate() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_date_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		pout.println("05.07.2023;01:43:12.53;06.08.2023;23:51:32.85");
		pout.println("12.421;01:02:03.04;12.09.2023;21:16:39.91");
		pout.println("24.08.2023;12:31:17.93;24.08.2023;15:15:46.51");
		pout.println("32.12.2022;01:02:03.04;01.02.2023;02:03:04.05");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading a downtimes csv with an invalid date returned null.", downtimes);
		assertEquals("The number of downtimes parsed from a csv with an invalid date didn't match.", 2,
				downtimes.size());
		assertEquals("The first downtime from a csv with an invalid date didn't match.",
				new Pair<Long, Long>(1688521392530l, 1691365892850l), downtimes.get(0));
		assertEquals("The second downtime from a csv with an invalid date didn't match.",
				new Pair<Long, Long>(1692880277930l, 1692890146510l), downtimes.get(1));

		errorLog.checkLine(
				"Failed to parse start time or date of line \"12.421;01:02:03.04;12.09.2023;21:16:39.91\". Skipping line.",
				0);
		errorLog.checkLine(
				"Failed to parse start time or date of line \"32.12.2022;01:02:03.04;01.02.2023;02:03:04.05\". Skipping line.");
	}

	/**
	 * A test parsing a downtimes csv containing an invalid time of day.
	 * 
	 * @throws IOException If reading, writing, or creating the temp file fails.
	 */
	@Test
	public void readInvalidTime() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("invalid_time_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		pout.println("02.10.2022,22:12.xx,03.11.2022,12:23:34.45");
		pout.println("03.05.2022,12:05:02.12,04.05.2022,21:12:54.79");
		pout.println("01.09.2022,02:12:51.91,12.09.2022,36:99:12.02");
		pout.println("05.01.2023,01:05:12.03,05.01.2023,12:12:12.12");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading a downtimes csv with an invalid time returned null.", downtimes);
		assertEquals("The number of downtimes parsed from a csv with an invalid time didn't match.", 2,
				downtimes.size());
		assertEquals("The first downtime from a csv with an invalid time didn't match.",
				new Pair<Long, Long>(1651579502120l, 1651698774790l), downtimes.get(0));
		assertEquals("The second downtime from a csv with an invalid time didn't match.",
				new Pair<Long, Long>(1672880712030l, 1672920732120l), downtimes.get(1));

		errorLog.checkLine(
				"Failed to parse start time or date of line \"02.10.2022,22:12.xx,03.11.2022,12:23:34.45\". Skipping line.",
				0);
		errorLog.checkLine(
				"Failed to parse end time or date of line \"01.09.2022,02:12:51.91,12.09.2022,36:99:12.02\". Skipping line.");
	}

	/**
	 * A test parsing a downtimes csv with a missing date.
	 * 
	 * @throws IOException If creating, writing, or reading the csv fails.
	 */
	@Test
	public void readMissingDate() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("missing_date_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		pout.println(";05:07:12.91;20.02.2022;13:51:01.94");
		pout.println("05.07.2022;13:59:01.00;07.12.2022;07:15:06.75");
		pout.println("07.06.2023;15:01:10.51;;12:21:04.82");
		pout.println("01.10.2023;18:41:57.38;15.10.2023;03:28:01.47");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading a downtimes csv with a missing date returned null.", downtimes);
		assertEquals("The number of downtimes parsed from a csv with a missing date didn't match.", 2,
				downtimes.size());
		assertEquals("The first downtime from a csv with a missing date didn't match.",
				new Pair<Long, Long>(1657029541000l, 1670397306750l), downtimes.get(0));
		assertEquals("The second downtime from a csv with a missing date didn't match.",
				new Pair<Long, Long>(1696185717380l, 1697340481470l), downtimes.get(1));

		errorLog.checkLine(
				"Failed to parse start time or date of line \";05:07:12.91;20.02.2022;13:51:01.94\". Skipping line.",
				0);
		errorLog.checkLine(
				"Failed to parse end time or date of line \"07.06.2023;15:01:10.51;;12:21:04.82\". Skipping line.");
	}

	/**
	 * A test parsing a downtimes file with a missing time.
	 * 
	 * @throws IOException If creating, writing, or reading the csv fails.
	 */
	@Test
	public void readMissingTime() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("missing_time_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		pout.println("02.01.2022;12:03:58.91;12.02.2022;05:17:31.85");
		pout.println("01.02.2022;21:12:02.61;12.12.2022;");
		pout.println("13.02.2022;00:00:00.00;04.03.2022;15:42:13.47");
		pout.println("02.04.2022;;03.04.2022;17:12:01.00");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading a downtimes csv with a missing time returned null.", downtimes);
		assertEquals("The number of downtimes parsed from a csv with a missing time didn't match.", 2,
				downtimes.size());
		assertEquals("The first downtime from a csv with a missing time didn't match.",
				new Pair<Long, Long>(1641125038910l, 1644643051850l), downtimes.get(0));
		assertEquals("The second downtime from a csv with a missing time didn't match.",
				new Pair<Long, Long>(1644710400000l, 1646408533470l), downtimes.get(1));

		errorLog.checkLine(
				"Input line \"01.02.2022;21:12:02.61;12.12.2022;\" did not contain exactly four tokens. Skipping line.",
				0);
		errorLog.checkLine(
				"Failed to parse start time or date of line \"02.04.2022;;03.04.2022;17:12:01.00\". Skipping line.");
	}

	/**
	 * A test parsing a csv containing reversed downtimes.
	 * 
	 * @throws IOException If reading, writing, or creating the temp csv fails.
	 */
	@Test
	public void readReversed() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("reversed_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		pout.println("03.01.2022;15:21:50.12;01.01.2022;16:19:42.94");
		pout.println("05.02.2022;21:15:02.58;05.02.2022;07:09:11.84");
		pout.println("06.02.2022;09:17:31.87;07.02.2022;21:56:21.00");
		pout.println("07.02.2022;22:00:05.71;07.02.2022;23:45:19.57");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Reading a downtimes csv with reversed downtimes returned null.", downtimes);
		assertEquals("The number of downtimes from a csv with reversed downtimes didn't match.", 2, downtimes.size());
		assertEquals("The first downtime from a csv with reversed downtimes didn't match.",
				new Pair<Long, Long>(1644139051870l, 1644270981000l), downtimes.get(0));
		assertEquals("The second downtime from a csv with reversed downtimes didn't match.",
				new Pair<Long, Long>(1644271205710l, 1644277519570l), downtimes.get(1));

		errorLog.checkLine("Downtime end wasn't after its start. Skipping line.", 0);
		errorLog.checkLine("Downtime end wasn't after its start. Skipping line.");
	}

	/**
	 * A test parsing a downtimes csv with overlapping downtimes.
	 * 
	 * @throws IOException If reading, writing, or creating the temp file fails.
	 */
	@Test
	public void readOverlapping() throws IOException {
		Pair<FileInputStreamHandler, PrintStream> tempFile = tempFolder.newTempInputFile("overlapping_downtimes.csv");
		FileInputStreamHandler fiin = tempFile.getKey();
		PrintStream pout = tempFile.getValue();

		// Second starting in first
		pout.println("01.01.2023;05:51:32.91;01.01.2023;15:07:12.01");
		pout.println("01.01.2023;10:21:51.62;01.01.2023;23:00:00.07");

		// First starting in second
		pout.println("03.02.2023;07:15:41.35;07.02.2023;09:40:27.67");
		pout.println("02.02.2023;19:51:00.12;04.02.2023;12:15:23.00");

		// Three being merged by last
		pout.println("07.03.2023;14:39:11.75;10.03.2023;01:00:00.00");
		pout.println("04.03.2023;13:58:34.37;05.03.2023;22:41:14.69");
		pout.println("05.03.2023;10:15:36.51;08.03.2023;16:07:47.86");

		// Second entirely in first
		pout.println("01.04.2023;01:02:03.04;05.04.2023;18:28:57.83");
		pout.println("02.04.2023;08:16:45.99;04.04.2023;17:52:09.46");

		// First entirely in second
		pout.println("02.05.2023;06:09:42.11;02.05.2023;19:56:38.08");
		pout.println("01.05.2023;21:28:30.87;03.05.2023;03:27:42.88");

		List<Pair<Long, Long>> downtimes = CSVHandler.readDowntimesCSV(fiin);
		assertNotNull("Parsing a downtimes csv with overlapping downtimes returned null.", downtimes);
		assertEquals("The number of downtimes parsed from a csv with overlapping downtimes didn't match.", 5,
				downtimes.size());
		assertEquals("The first downtime from a csv with overlapping downtimes didn't match.",
				new Pair<Long, Long>(1672552292910l, 1672614000070l), downtimes.get(0));
		assertEquals("The second downtime from a csv with overlapping downtimes didn't match.",
				new Pair<Long, Long>(1675367460120l, 1675762827670l), downtimes.get(1));
		assertEquals("The third downtime from a csv with overlapping downtimes didn't match.",
				new Pair<Long, Long>(1677938314370l, 1678410000000l), downtimes.get(2));
		assertEquals("The fourth downtime from a csv with overlapping downtimes didn't match.",
				new Pair<Long, Long>(1680310923040l, 1680719337830l), downtimes.get(3));
		assertEquals("The fifth downtime from a csv with overlapping downtimes didn't match.",
				new Pair<Long, Long>(1682976510870l, 1683084462880l), downtimes.get(4));
	}
}
