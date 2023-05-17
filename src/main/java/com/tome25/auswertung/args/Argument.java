package com.tome25.auswertung.args;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Manifest;

import com.tome25.auswertung.log.LogHandler;
import com.tome25.auswertung.utils.FileUtils;
import com.tome25.auswertung.utils.TimeUtils;

/**
 * The enum specifying the possible arguments that this program can handle.
 * 
 * @author Theodor Meyer zu Hörste
 */
public enum Argument {
	DEBUG('d', (short) 7, "debug") {
		@Override
		public void onReceived(Arguments inst, String val) {
			inst.debug = true;
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Enables additional log output about issues and the current program state.",
					"Useful for debugging issues, but likely not useful for the average user.",
					"Verbose and debug are aliases of each other." };
		}
	},
	VERBOSE('v', (short) 7, "verbose") {// Verbose and debug do the same thing.
		@Override
		public void onReceived(Arguments inst, String val) {
			DEBUG.onReceived(inst, val);
		}

		@Override
		public String[] getDescription() {
			return DEBUG.getDescription();
		}
	},
	SILENT('s', (short) 7, "silent") {
		@Override
		public void onReceived(Arguments inst, String val) {
			inst.silent = true;
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Disables all log output from this program.",
					"This includes both the standard out/err(shown in your terminal) as well as log files.",
					"The help text and version info still get written." };
		}
	},
	HELP('h', (short) 6, "help") {
		@Override
		public void onReceived(Arguments inst, String val) {
			if (inst.arguments.contains(LOGFILE)) {
				LogHandler.removeLogCache(inst.logFile, inst.logFile);
			} else {
				LogHandler.removeLogCache(null, null);
			}

			HELP.printHelp();
			System.exit(0);
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Prints this help text and exits.", "Gets written even with --silent." };
		}
	},
	VERSION('V', (short) 6, "version") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (inst.arguments.contains(LOGFILE)) {
				LogHandler.removeLogCache(inst.logFile, inst.logFile);
			} else {
				LogHandler.removeLogCache(null, null);
			}

			VERSION.printVersion();
			System.exit(0);
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Prints version info about this program and exits.",
					"Gets written even with --silent." };
		}
	},
	DOCS('D', ArgumentValue.OPTIONAL, "DIRECTORY", (short) 6, "docs") {
		@Override
		public void onReceived(Arguments inst, String val) {
			File target = new File("PutenAuswertung-docs");

			if (val != null && val.trim().isEmpty()) {
				val = null;
				LogHandler
						.err_println("Docs target directory only contained whitespace characters. Using current dir.");
			}

			if (val != null) {
				target = new File(new File(val), "PutenAuswertung-docs");
			}

			LogHandler.out_println("Extracting documentation to \"" + target.toString() + "\".");
			if (inst.arguments.contains(OVERRIDE)) {
				LogHandler.out_println("Overriding existing files in target directory.");
			}

			try {
				FileUtils.extract("/docs/", target, inst.arguments.contains(OVERRIDE));
			} catch (Exception e) {
				LogHandler.err_println("Failed to extract documentation: " + e.getMessage());
				LogHandler.print_exception(e, "extract documentation", "Target: %s", target.toString());
			}

			System.exit(0);
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Extracts this programs documentation from the jar, then exits.",
					"Puts the documentation in the specified directory, if any.",
					"If none is specified, puts it in the current directory." };
		}
	},
	ANTENNADATA('a', ArgumentValue.REQUIRED, "FILE", (short) 5, "antenna-data", "antennadata") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (val == null || val.trim().isEmpty()) {
				throw new IllegalArgumentException("Antenna Data input file name was empty.");
			}

			inst.antennaDataInput = val;
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Sets the file this program should read antenna data records from." };
		}
	},
	TURKEYS('t', ArgumentValue.REQUIRED, "FILE", (short) 5, "turkeys") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (val == null || val.trim().isEmpty()) {
				throw new IllegalArgumentException("Turkeys input file name was empty.");
			}

			inst.turkeysInput = val;
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Sets the file to read the turkey to transponder mappings from.",
					"These mappings tell the program which transponders are attached to the same turkey." };
		}
	},
	ZONES('z', ArgumentValue.REQUIRED, "FILE", (short) 5, "zones", "areas") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (val == null || val.trim().isEmpty()) {
				throw new IllegalArgumentException("Zones input file name was empty.");
			}

			inst.zonesInput = val;
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Sets the file to read zone to antenna mappings from.",
					"These mappings tell the program which antennas are part of which zone." };
		}
	},
	DOWNTIMES('o', ArgumentValue.OPTIONAL, "FILE", (short) 5, "downtimes", "offline-times", "offlinetimes", "off-times",
			"offtimes") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (val == null || val.trim().isEmpty()) {
				LogHandler.out_println("Downtimes argument without value received, disabling downtimes file.");
				inst.downtimesInput = null;
				inst.hasDowntimesInput = false;
			} else {
				inst.downtimesInput = val;
				inst.hasDowntimesInput = true;
			}
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Sets the file to read the system downtimes from.",
					"System downtimes are the times in which data could not be recorded,",
					"which thus shouldn't be part of the evaluation.",
					"Use without a value to not read any downtimes file." };
		}
	},
	TOTALS('T', ArgumentValue.REQUIRED, "FILE", (short) 5, "totals") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (val == null || val.trim().isEmpty()) {
				throw new IllegalArgumentException("Totals output file name was empty.");
			}

			inst.totalsOutput = val;
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Sets the file to write the total zone times to." };
		}
	},
	STAYS('S', ArgumentValue.REQUIRED, "FILE", (short) 5, "stays") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (val == null || val.trim().isEmpty()) {
				throw new IllegalArgumentException("Stays output file name was empty.");
			}

			inst.staysOutput = val;
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Sets the file to write the individual zone stays to." };
		}
	},
	LOGFILE('l', ArgumentValue.OPTIONAL, "FILE", (short) 6, "log-file", "logfile") {// TODO split into error and output
																					// one
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (val == null || val.trim().isEmpty()) {
				inst.logFile = null;
				LogHandler.out_println("Log file disabled. No logging/error information will be written to a file.");
			} else {
				inst.logFile = new File(val);
			}
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Sets the file to write the logging messages to.",
					"Use without a value to disable creating a log file entirely." };
		}
	},
	FILLDAYS('f', (short) 5, "fill-days", "filldays") {
		@Override
		public void onReceived(Arguments inst, String val) {
			inst.fillDays = true;
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Makes the program pad the recording time to full days.",
					"This means it will assume that all turkeys were in the zone they were first recorded in until their first record.",
					"And that they all were in their last recorded zone until the end of each day." };
		}
	},
	MINTIME('m', ArgumentValue.REQUIRED, "TIME", (short) 5, "min-time", "mintime") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (val == null || val.trim().isEmpty()) {
				throw new IllegalArgumentException("No minimum zone time specified.");
			}

			int min_time = 0;
			try {
				min_time = Integer.parseInt(val);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Minimum zone time couldn't be parsed to an integer.", e);
			}

			if (min_time < 0) {
				throw new IllegalArgumentException("Minimum zone time cannot be negative.");
			}

			inst.minTime = min_time;
		}

		@Override
		public String[] getDescription() {
			return new String[] {
					"Sets the minimum time a turkey has to spend in a zone at a time for the stay to be counted.",
					"Set to 0 to disable the check entirely.",
					"Stays shorter than this are counted towards the last stay longer than this value.",
					"The value is in seconds.", "The default is 300 seconds, aka 5 minutes." };
		}
	},
	OVERRIDE('O', (short) 5, "override", "override-outputs", "overrideoutputs") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			inst.overrideOutput = true;
			LogHandler.out_println("Override argument received, overriding output files without confirmation.", true);
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Tells the program to override output files if they already exist.",
					"If this argument is not specified the program will ask what to do if output files already exist.",
					"If the program is run non-interactively(in a script or by double-clicking) it overrides output files even without this argument." };
		}
	},
	DECIMALSEPARATOR('c', ArgumentValue.REQUIRED, "SEPARATOR", (short) 5, "decimal-separator", "decimalseparator",
			"decimal-comma", "decimalcomma", "comma") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (val.length() != 1) {
				throw new IllegalArgumentException("Decimal Separator has to be a single character.");
			}

			TimeUtils.setDecimalSeparator(val.charAt(0));
			inst.decimalSeparator = val.charAt(0);
			LogHandler.out_println("Using decimal separator '" + inst.decimalSeparator + "' for output.");
		}

		@Override
		public String[] getDescription() {
			return new String[] {
					"Sets the character to be used to separate the seconds from hundredth in output times.",
					"This character will be used in both the output files, and log messages.",
					"Can only be either a dot or a comma." };
		}

	},
	INTERACTIVE('i', (short) 5, "interactive") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (inst.arguments.contains(BATCH)) {
				LogHandler.err_println("Interactive and Batch arguments detected. Please use only one of the two.");
				LogHandler.print_debug_info("Arguments: %s", inst);
				System.exit(5);
			}

			inst.interactive = true;
			LogHandler.out_println("Interactive mode enabled.");
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Enables the interactive mode of this program.",
					"In interactive mode the program will ask before ignoring unknown antennas or overriding output files.",
					"Interactive mode can be enabled using --batch.",
					"If neither is specified, the program will attempt to detect whether its being run in an interactive environment." };
		}

	},
	BATCH('b', (short) 5, "batch") {
		@Override
		public void onReceived(Arguments inst, String val) throws IllegalArgumentException {
			if (inst.arguments.contains(INTERACTIVE)) {
				LogHandler.err_println("Interactive and Batch arguments detected. Please use only one of the two.");
				LogHandler.print_debug_info("Arguments: %s", inst);
				System.exit(5);
			}

			inst.interactive = false;
			LogHandler.out_println("Interactive mode disabled.");
		}

		@Override
		public String[] getDescription() {
			return new String[] { "Disables the interactive mode of this program.",
					"In interactive mode the program will ask before ignoring unknown antennas or overriding output files.",
					"Interactive mode can be enabled using --interactive.",
					"If neither is specified, the program will attempt to detect whether its being run in an interactive environment." };
		}

	};

	/**
	 * Creates a new Argument instance without a value to handle.
	 * 
	 * @param shortArg The single character argument, aka the short arg, to be given
	 *                 for this argument.
	 * @param priority The priority with which this argument is handled.<br/>
	 *                 Higher means its handled earlier.<br/>
	 *                 Range: 0 - 10.
	 * @param longArgs The text arguments, aka long args, for this argument.
	 * @throws IllegalArgumentException If {@code priority} isn't in the valid
	 *                                  range, or shortArg isn't a valid character.
	 * @throws NullPointerException     If {@code value} is {@code null}.
	 */
	private Argument(char shortArg, short priority, String... longArgs)
			throws IllegalArgumentException, NullPointerException {
		this(shortArg, ArgumentValue.NONE, null, priority, longArgs);
	}

	/**
	 * Creates a new Argument instance.
	 * 
	 * @param shortArg The single character argument, aka the short arg, to be given
	 *                 for this argument.
	 * @param value    Whether this argument handles a value, as well as whether
	 *                 this value is optional.
	 * @param valName  The name of the value for this argument.<br/>
	 *                 Ignored if {@code value} is {@link ArgumentValue#NONE}.<br/>
	 *                 Cannot be {@code null} or empty if {@code value} isn't
	 *                 {@link ArgumentValue#NONE}.<br/>
	 *                 Converted to be fully uppercase internally.
	 * @param priority The priority with which this argument is handled.<br/>
	 *                 Higher means its handled earlier.<br/>
	 *                 Range: 0 - 10.
	 * @param longArgs The text arguments, aka long args, for this argument.
	 * @throws IllegalArgumentException If {@code priority} isn't in the valid
	 *                                  range, {@code shortArg} isn't a valid
	 *                                  character, or {@code valName} is
	 *                                  {@code null} or empty and {@code value}
	 *                                  isn't {@link ArgumentValue#NONE}.
	 * @throws NullPointerException     If {@code value} is {@code null}.
	 */
	private Argument(char shortArg, ArgumentValue value, String valName, short priority, String... longArgs)
			throws IllegalArgumentException, NullPointerException {
		if (priority < 0 || priority > 10) {
			throw new IllegalArgumentException("Argument priority outside range 0 - 10 received.");
		}

		if (!Character.isLetterOrDigit(shortArg)) {
			throw new IllegalArgumentException("Received a non alpha-numeric short arg.");
		}

		this.shortArg = shortArg;
		this.longArgs = longArgs == null ? new String[0] : longArgs;
		this.prio = priority;
		this.val = Objects.requireNonNull(value, "The value requirement can't be null.");

		if (val != ArgumentValue.NONE) {
			valName = valName.trim();
			if (valName == null || valName.isEmpty()) {
				throw new IllegalArgumentException("The value name can't be null or empty if the value isn't none.");
			}
			this.valName = valName.toUpperCase();
		} else {
			this.valName = null;
		}
	}

	/**
	 * The single char argument, aka short arg, for this argument.
	 */
	public final char shortArg;

	/**
	 * The possible string arguments, aka long args, for this argument.
	 */
	public final String[] longArgs;

	/**
	 * The priority of this argument. Higher means its processed earlier.<br/>
	 * Arguments with the same priority are processed in the order they are
	 * specified in.
	 */
	public final short prio;

	/**
	 * Whether this argument can optionally handle a value, requires one, or doesn't
	 * handle one at all.
	 */
	public final ArgumentValue val;

	/**
	 * The name of the value for this argument, if any.
	 */
	public final String valName;

	/**
	 * The method handling everything that needs to be done if this argument is
	 * found.
	 * 
	 * @param inst The {@link Arguments} instance to use.<br/>
	 *             Potentially changed by this method.
	 * @param val  The value for this argument, or {@code null} if it doesn't have
	 *             one.<br/>
	 *             Also {@code null} if this arg has an optional value, and none was
	 *             specified.<br/>
	 *             This is only an empty string if it was explicitly specified as
	 *             such, by passing an empty pair of quotes.
	 * @throws IllegalArgumentException If {@code val} does not match the
	 *                                  requirements for this argument.
	 */
	public abstract void onReceived(Arguments inst, String val) throws IllegalArgumentException;

	/**
	 * Gets the description for this argument.<br/>
	 * Each string is handled as a separate line.<br/>
	 * Might be line wrapped in the future.
	 * 
	 * @return The description for this argument.
	 */
	public abstract String[] getDescription();

	/**
	 * An enum used to specify whether an argument gets a value when given.
	 * 
	 * @author Theodor Meyer zu Hörste
	 */
	public enum ArgumentValue {
		/**
		 * The argument cannot handle a value.
		 */
		NONE,
		/**
		 * The argument can handle a value, but does not require one.
		 */
		OPTIONAL,
		/**
		 * The argument requires a value.
		 */
		REQUIRED;

		/**
		 * Generates the display string for the given value name and this value type.
		 * 
		 * @param valueName The name for the display string.
		 * @return The generated display string.
		 * @throws NullPointerException If {@code valueName} is {@code null} and this
		 *                              isn't {@link #NONE}.
		 */
		public String getDisplayString(String valueName) throws NullPointerException {
			if (this == NONE) {
				return "";
			}

			Objects.requireNonNull(valueName, "The value name can't be null if the type isn't NONE.");

			StringBuilder displayString = new StringBuilder();
			displayString.append(this == OPTIONAL ? '[' : '<');
			displayString.append(valueName);
			displayString.append(this == OPTIONAL ? ']' : '>');

			return displayString.toString();
		}
	}

	/**
	 * Prints the help text for this program to the system output.
	 */
	private void printHelp() {
		// Generate usage line.
		String classPath = System.getProperty("java.class.path");

		boolean jar = false;
		String classRes = getClass().getName().replace('.', '/') + ".class";
		URL resource = getClass().getClassLoader().getResource(classRes);
		if (resource != null) {
			String protocol = resource.getProtocol();
			jar = protocol.equals("jar");
			if (!protocol.equals("jar") && !protocol.equals("file")) {
				LogHandler.err_println("Resource protocol is unknown protocol \"" + protocol + "\".", true);
				LogHandler.print_debug_info("Resource url: %s, protocol: %s", resource.toString(), protocol);
			}
		} else {
			LogHandler.err_println("Couldn't get resource url for this class.");
			LogHandler.print_debug_info("Class: %s", classRes);
		}

		// This only works if this is run in the main thread.
		String mainClassName = null;
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		if (stack.length > 0) {
			StackTraceElement mainElement = stack[stack.length - 1];
			if (mainElement.getMethodName().equals("main")) {
				mainClassName = mainElement.getClassName();

				if (jar) {
					try {
						JarURLConnection jarConnection = (JarURLConnection) resource.openConnection();
						Manifest manifest = jarConnection.getManifest();
						String manifestMainClass = manifest.getMainAttributes().getValue("Main-Class");
						if (mainClassName.equals(manifestMainClass)) {
							mainClassName = null;
						} else if (manifestMainClass == null) {
							LogHandler.err_println("Couldn't read MANIFEST.MF Main-Class.", true);
						}
					} catch (IOException e) {
						LogHandler.err_println("An IOException occurred while trying to read the jar MANIFEST.MF.");
						LogHandler.print_exception(e, "read jar MANIFEST.MF", "Main class: %s", mainClassName);
					}
				}
			} else {
				LogHandler.err_println("The first element of the current threads stack trace isn't a main method.",
						true);
				LogHandler.err_println("This probably means the argument parsing was handled on a non main thread.",
						true);
				LogHandler.print_debug_info("Thread name: %s, Thread id: %d, Init class: %s, Init method: %s",
						Thread.currentThread().getName(), Thread.currentThread().getId(), mainElement.getClassName(),
						mainElement.getMethodName());
			}
		} else {
			LogHandler.err_println("StackTrace is empty.");
			LogHandler.print_debug_info("Thread name: %s Thread id: %d", Thread.currentThread().getName(),
					Thread.currentThread().getId());
		}

		// Assume the command is always "java" for now, since getting the actual cmd is
		// way too much effort in Java 7.
		if (jar && mainClassName == null) {
			System.out.printf("Usage: java -jar %s [OPTION]...%n", classPath);
		} else {
			System.out.printf("Usage: java -cp %s %s [OPTION]...%n", classPath,
					mainClassName != null ? mainClassName : "UNKNOWN");
		}

		// Static help text.
		System.out.println();
		System.out.println("Values for long options also apply to short options.");
		System.out.println();
		System.out.println("Options:");

		// Generate argument help.
		int maxLen = 0;
		Map<Argument, StringBuilder> argStrs = new LinkedHashMap<Argument, StringBuilder>();
		for (Argument arg : Argument.values()) {
			StringBuilder argStr = new StringBuilder();
			argStr.append(" -");
			argStr.append(arg.shortArg);

			if (arg.longArgs.length > 0) {
				argStr.append(", --");
				argStr.append(arg.longArgs[0]);
				if (arg.longArgs.length > 1) {
					argStr.append(',');
				}
			}

			if (arg.val != ArgumentValue.NONE) {
				argStr.append(' ');
				argStr.append(arg.val.getDisplayString(arg.valName));
			}

			int argLen = argStr.length();
			int mlal = 0;
			for (int i = 0; i < arg.longArgs.length; i++) {
				String longArg = arg.longArgs[i];
				if (longArg.length() > mlal) {
					if (mlal > 0) {
						argLen += longArg.length() - mlal;
						if (i == arg.longArgs.length - 1) {
							argLen -= 1;
						}
					}
					mlal = longArg.length();
				}
			}

			if (argLen > maxLen) {
				maxLen = argLen;
			}
			argStrs.put(arg, argStr);
		}

		// Indent after the longest line
		maxLen += 2;

		// Indent string for additional description lines.
		StringBuilder indent = new StringBuilder();
		for (int i = 0; i < maxLen; i++) {
			indent.append(' ');
		}

		for (Argument arg : argStrs.keySet()) {
			StringBuilder argStr = argStrs.get(arg);
			while (argStr.length() < maxLen) {
				argStr.append(' ');
			}

			System.out.print(argStr.toString());
			if (arg.getDescription().length > 0) {
				System.out.println(arg.getDescription()[0]);
				for (int i = 1; i < arg.getDescription().length || i < arg.longArgs.length; i++) {
					if (i < arg.longArgs.length) {
						StringBuilder line = new StringBuilder();
						line.append("     --");
						line.append(arg.longArgs[i]);
						if (i + 1 < arg.longArgs.length) {
							line.append(',');
						}

						if (arg.val != ArgumentValue.NONE) {
							line.append(' ');
							line.append(arg.val.getDisplayString(arg.valName));
						}

						if (i < arg.getDescription().length) {
							for (int j = line.length(); j < maxLen; j++) {
								line.append(' ');
							}
							System.out.print(line.toString());
						} else {
							System.out.println(line.toString());
						}
					} else {
						System.out.print(indent.toString());
					}

					if (i < arg.getDescription().length) {
						System.out.println(arg.getDescription()[i]);
					}
				}
			}
		}
	}

	/**
	 * Prints the version text for this program to the log.
	 */
	private void printVersion() {
		String name = this.getClass().getPackage().getImplementationTitle();
		if (name == null || name.trim().isEmpty()) {
			name = System.getProperty("java.class.path");
			if (name == null) {
				name = "";
			}

			while (name.endsWith(File.separator)) {
				name = name.substring(0, name.length() - 1);
			}

			name = name.substring(name.lastIndexOf(File.separatorChar) + 1);

			if (name.trim().isEmpty()) {
				name = "PutenAuswertung";
			}
		}

		String version = this.getClass().getPackage().getImplementationVersion();
		if (version == null || version.trim().isEmpty()) {
			version = "UNKNOWN";
		}

		System.out.print(name);
		System.out.print(' ');
		System.out.println(version);
		System.out.println();
		System.out.println("Written by Theodor MzH");
	}
}
