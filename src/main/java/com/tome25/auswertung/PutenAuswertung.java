package com.tome25.auswertung;

import java.io.File;
import java.io.FileNotFoundException;

import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.FileOutputStreamHandler;
import com.tome25.auswertung.stream.IInputStreamHandler;
import com.tome25.auswertung.stream.IOutputStreamHandler;
import com.tome25.auswertung.stream.MultiOutputStreamHandler;
import com.tome25.auswertung.stream.SysOutStreamHandler;
import com.tome25.auswertung.utils.StringUtils;

/**
 * The main class primarily responsible for delegating the work to its
 * respective classes.
 * 
 * @author theodor
 */
public class PutenAuswertung {

	/**
	 * An array containing the names/relative paths of all the antenna record files
	 * to check if the antenna record input wasn't specified.<br/>
	 * Unlike specified inputs defaults are checked as they are and entirely
	 * lowercase.
	 */
	private static final String DEFAULT_INPUT_FILE[] = { "AntennenDaten.csv", "Input.csv" };

	/**
	 * The default input file for transponder ids to turkey id mappings.<br/>
	 * Unlike specified inputs defaults are checked as they are and entirely
	 * lowercase.
	 */
	private static final String DEFAULT_PUTEN_FILE = "Puten.csv";

	/**
	 * The default file for antenna ids to zone name mappings.<br/>
	 * Unlike specified inputs defaults are checked as they are and entirely
	 * lowercase.
	 */
	private static final String DEFAULT_BEREICHE_FILE = "Bereiche.csv";

	/**
	 * The default output file for the generated analysis data.
	 */
	private static final String DEFAULT_OUTPUT_FILE = "PutenAuswertung.csv";

	public static void main(String... args) {
		File antennaFile = null;
		for (String in : DEFAULT_INPUT_FILE) {
			antennaFile = new File(in);
			if (antennaFile.exists() && antennaFile.isFile()) {
				break;
			}

			antennaFile = new File(in.toLowerCase());
			if (antennaFile.exists() && antennaFile.isFile()) {
				break;
			}

			antennaFile = null;
		}

		if (antennaFile == null) {
			LogHandler.err_println(
					"No antenna records input file found. This program looks for a file called \"AntennenDaten.csv\" in the directory you are executing this command in.");
			return;
		} else {
			LogHandler.out_println(
					String.format("Reading antenna records input file \"%s\".", antennaFile.getAbsolutePath()));
		}

		File turkeyFile = new File(DEFAULT_PUTEN_FILE);
		if (!turkeyFile.exists() || !turkeyFile.isFile()) {
			turkeyFile = new File(DEFAULT_PUTEN_FILE.toLowerCase());
			if (!turkeyFile.exists() || !turkeyFile.isFile()) {
				LogHandler.err_println(String.format(
						"No turkey transponder mappings file found. This program expects a file called \"%s\" in the directory you are executing this command in.",
						DEFAULT_PUTEN_FILE));
				return;
			}
		}

		LogHandler
				.out_println(String.format("Reading turkey mappings input file \"%s\".", turkeyFile.getAbsolutePath()));

		File zoneFile = new File(DEFAULT_BEREICHE_FILE);
		if (!zoneFile.exists() || !zoneFile.isFile()) {
			zoneFile = new File(DEFAULT_BEREICHE_FILE.toLowerCase());
			if (!zoneFile.exists() || !zoneFile.isFile()) {
				LogHandler.err_println(String.format(
						"No zone mappings file found. This program expects a file called \"%s\" in the directory you are executing this command in.",
						DEFAULT_BEREICHE_FILE));
				return;
			}
		}

		LogHandler.out_println(String.format("Reading zone mappings input file \"%s\".", zoneFile.getAbsolutePath()));

		IInputStreamHandler antennaHandler = null;
		try {
			antennaHandler = new FileInputStreamHandler(antennaFile);
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Failed to open input stream for antenna record input file.");
			LogHandler.print_exception(e, "init antenna record file input stream handler",
					"Antenna record file: \"%s\", Turkey mapping file: \"%s\", Zone mapping file: \"%s\", Arguments: [%s]",
					antennaFile.getAbsolutePath(), turkeyFile.getAbsolutePath(), zoneFile.getAbsolutePath(),
					StringUtils.join(", ", (Object[]) args));
		}

		IInputStreamHandler turkeyHandler = null;
		try {
			turkeyHandler = new FileInputStreamHandler(turkeyFile);
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Failed to open input stream for turkey mappings input file.");
			LogHandler.print_exception(e, "init turkey mappings file input stream handler",
					"Antenna record file: \"%s\", Turkey mapping file: \"%s\", Zone mapping file: \"%s\", Arguments: [%s]",
					antennaFile.getAbsolutePath(), turkeyFile.getAbsolutePath(), zoneFile.getAbsolutePath(),
					StringUtils.join(", ", (Object[]) args));
		}

		IInputStreamHandler zoneHandler = null;
		try {
			zoneHandler = new FileInputStreamHandler(zoneFile);
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Failed to open input stream for zone mappings input file.");
			LogHandler.print_exception(e, "init zone mappings file input stream handler",
					"Antenna record file: \"%s\", Turkey mapping file: \"%s\", Zone mapping file: \"%s\", Arguments: [%s]",
					antennaFile.getAbsolutePath(), turkeyFile.getAbsolutePath(), zoneFile.getAbsolutePath(),
					StringUtils.join(", ", (Object[]) args));
		}

		IOutputStreamHandler outputHandler = null;
		try {
			FileOutputStreamHandler fiout = new FileOutputStreamHandler(new File(DEFAULT_OUTPUT_FILE));
			SysOutStreamHandler sysout = new SysOutStreamHandler();
			outputHandler = new MultiOutputStreamHandler(fiout, sysout);
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Faild to open file output stream for generated data.");
		}

		if (antennaHandler == null || turkeyHandler == null || zoneHandler == null || outputHandler == null) {
			return;
		}

		DataHandler.handleStreams(antennaHandler, turkeyHandler, zoneHandler, outputHandler, false);
	}
}
