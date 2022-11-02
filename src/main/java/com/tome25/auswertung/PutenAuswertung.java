package com.tome25.auswertung;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.stream.FileInputStreamHandler;
import com.tome25.auswertung.stream.FileOutputStreamHandler;
import com.tome25.auswertung.stream.IInputStreamHandler;
import com.tome25.auswertung.stream.IOutputStreamHandler;
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
	 * The default output file for the daily zone time and zone change totals.
	 */
	private static final String DEFAULT_TOTALS_FILE = "PutenAuswertungZeiten.csv";

	/**
	 * The default output file for the individual zone stays.
	 */
	private static final String DEFAULT_STAYS_FILE = "PutenAuswertungAufenthalte.csv";

	/**
	 * The default file to write the system log to.
	 */
	private static final String DEFAULT_LOG_FILE = "PutenAuswertung.log";

	/**
	 * The method initially called by the JVM on program startup.<br/>
	 * A wrapper calling {@link #run} and exiting with its returned int.
	 * 
	 * @param args The arguments given on program startup.
	 */
	public static void main(String... args) {
		System.exit(run(args));
	}

	/**
	 * The main method handling reading command line args, opening files, and
	 * initializing data parsing.
	 * 
	 * @param args The arguments for this program.
	 * @return The exit code of this program.
	 */
	public static int run(String... args) {
		File logFile = new File(DEFAULT_LOG_FILE);
		try {
			LogHandler.addLogFile(logFile, true, true);
			LogHandler.overrideSysErr();
			LogHandler.overrideSysOut();
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Failed to open log file.");
			LogHandler.print_exception(e, "open log file", "Log file: \"%s\"", logFile.getAbsolutePath());
		}

		Arguments argHandler = null;
		try {
			argHandler = new Arguments(args);
		} catch (IllegalStateException e) {
			LogHandler.err_println(e.getMessage());
			LogHandler.print_exception(e, "parse commandline args", "Arguments: [%s]", StringUtils.join(", ", args));
			return 1;
		}

		File antennaFile = null;
		if (argHandler.antennaDataInput != null) {
			antennaFile = new File(argHandler.antennaDataInput);

			if (!antennaFile.exists() || !antennaFile.isFile()) {
				antennaFile = null;
				LogHandler.err_println("The antenna data input file \"" + argHandler.antennaDataInput
						+ "\" doesn't exist or isn't a file.");
			}
		} else {
			for (String in : DEFAULT_INPUT_FILE) {
				antennaFile = new File(in);
				if (antennaFile.exists() && antennaFile.isFile()) {
					break;
				}

				antennaFile = new File(in.charAt(0) + in.substring(1).toLowerCase());
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
				LogHandler.err_println("No antenna records input file found. This program looks for a file called \""
						+ DEFAULT_INPUT_FILE[0] + "\" in the directory you are executing this command in.");
			}
		}

		if (antennaFile != null) {
			try {
				LogHandler.out_println(
						String.format("Using antenna records input file \"%s\".", antennaFile.getCanonicalPath()));
			} catch (IOException e) {
				LogHandler.err_println("An error occurred while getting a files canonical path.");
				LogHandler.print_exception(e, "get canonical path", "File: %s", antennaFile);
			}
		}

		File turkeyFile = null;
		if (argHandler.turkeysInput != null) {
			turkeyFile = new File(argHandler.turkeysInput);

			if (!turkeyFile.exists() || !turkeyFile.isFile()) {
				turkeyFile = null;
				LogHandler.err_println(
						"The turkey input file \"" + argHandler.turkeysInput + "\" doesn't exist or isn't a file.");
			}
		} else {
			turkeyFile = new File(DEFAULT_PUTEN_FILE);
			if (!turkeyFile.exists() || !turkeyFile.isFile()) {
				turkeyFile = new File(DEFAULT_PUTEN_FILE.toLowerCase());
				if (!turkeyFile.exists() || !turkeyFile.isFile()) {
					turkeyFile = null;
					LogHandler.err_println(
							"No turkey transponder mappings file found. This program expects a file called \""
									+ DEFAULT_PUTEN_FILE + "\" in the directory you are executing this command in.");
				}
			}
		}

		if (turkeyFile != null) {
			try {
				LogHandler.out_println(
						String.format("Using turkey mappings input file \"%s\".", turkeyFile.getCanonicalPath()));
			} catch (IOException e) {
				LogHandler.err_println("An error occurred while getting a files canonical path.");
				LogHandler.print_exception(e, "get canonical path", "File: %s", turkeyFile);
			}
		}

		File zoneFile = null;
		if (argHandler.zonesInput != null) {
			zoneFile = new File(argHandler.zonesInput);

			if (!zoneFile.exists() || !zoneFile.isFile()) {
				zoneFile = null;
				LogHandler.err_println(
						"The zone input file \"" + argHandler.zonesInput + "\" doesn't exist or isn't a file.");
			}
		} else {
			zoneFile = new File(DEFAULT_BEREICHE_FILE);
			if (!zoneFile.exists() || !zoneFile.isFile()) {
				zoneFile = new File(DEFAULT_BEREICHE_FILE.toLowerCase());
				if (!zoneFile.exists() || !zoneFile.isFile()) {
					zoneFile = null;
					LogHandler.err_println("No zone mappings file found. This program expects a file called \""
							+ DEFAULT_BEREICHE_FILE + "\" in the directory you are executing this command in.");
				}
			}
		}

		if (zoneFile != null) {
			try {
				LogHandler.out_println(
						String.format("Using zone mappings input file \"%s\".", zoneFile.getCanonicalPath()));
			} catch (IOException e) {
				LogHandler.err_println("An error occurred while getting a files canonical path.");
				LogHandler.print_exception(e, "get canonical path", "File: %s", zoneFile);
			}
		}

		if (antennaFile != null && !antennaFile.canRead()) {
			LogHandler.err_println("The antenna records input file cannot be read.");
			antennaFile = null;
		}

		if (turkeyFile != null && !turkeyFile.canRead()) {
			LogHandler.err_println("The turkey mappings input file cannot be read.");
			turkeyFile = null;
		}

		if (zoneFile != null && !zoneFile.canRead()) {
			LogHandler.err_println("The zone mappings input file cannot be read.");
			zoneFile = null;
		}

		if (antennaFile == null || turkeyFile == null || zoneFile == null) {
			return 2;
		}

		IInputStreamHandler antennaHandler = null;
		try {
			antennaHandler = new FileInputStreamHandler(antennaFile);
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Failed to open input stream for antenna record input file.");
			LogHandler.print_exception(e, "init antenna record file input stream handler",
					"Antenna record file: \"%s\", Turkey mapping file: \"%s\", Zone mapping file: \"%s\", Arguments: [%s]",
					antennaFile.getAbsolutePath(), turkeyFile.getAbsolutePath(), zoneFile.getAbsolutePath(),
					StringUtils.join(", ", args));
		}

		IInputStreamHandler turkeyHandler = null;
		try {
			turkeyHandler = new FileInputStreamHandler(turkeyFile);
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Failed to open input stream for turkey mappings input file.");
			LogHandler.print_exception(e, "init turkey mappings file input stream handler",
					"Antenna record file: \"%s\", Turkey mapping file: \"%s\", Zone mapping file: \"%s\", Arguments: [%s]",
					antennaFile.getAbsolutePath(), turkeyFile.getAbsolutePath(), zoneFile.getAbsolutePath(),
					StringUtils.join(", ", args));
		}

		IInputStreamHandler zoneHandler = null;
		try {
			zoneHandler = new FileInputStreamHandler(zoneFile);
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Failed to open input stream for zone mappings input file.");
			LogHandler.print_exception(e, "init zone mappings file input stream handler",
					"Antenna record file: \"%s\", Turkey mapping file: \"%s\", Zone mapping file: \"%s\", Arguments: [%s]",
					antennaFile.getAbsolutePath(), turkeyFile.getAbsolutePath(), zoneFile.getAbsolutePath(),
					StringUtils.join(", ", args));
		}

		IOutputStreamHandler totalHandler = null;
		try {
			totalHandler = new FileOutputStreamHandler(new File(DEFAULT_TOTALS_FILE), false, true);
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Faild to open file output stream for generated totals data.");
		}

		IOutputStreamHandler staysHandler = null;
		try {
			staysHandler = new FileOutputStreamHandler(new File(DEFAULT_STAYS_FILE), false, true);
		} catch (FileNotFoundException e) {
			LogHandler.err_println("Faild to open file output stream for generated stays data.");
		}

		if (antennaHandler == null || turkeyHandler == null || zoneHandler == null || totalHandler == null
				|| staysHandler == null) {
			return 2;
		}

		DataHandler.handleStreams(antennaHandler, turkeyHandler, zoneHandler, totalHandler, staysHandler, false);

		LogHandler.out_println("Finished data analysis. Exiting.");
		return 0;
	}
}
