package com.tome25.auswertung;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.tome25.auswertung.utils.MapUtils;
import com.tome25.auswertung.utils.Pair;
import com.tome25.auswertung.utils.StringUtils;

/**
 * The class handling the parsing of command line arguments.<br/>
 * Also contains the info about all possible arguments.
 * 
 * @author theodor
 */
public class Arguments {

	/**
	 * Whether an argument enabling debug output was given.
	 */
	public boolean debug = false;

	/**
	 * An enum used to specify whether an argument gets a value when given.
	 * 
	 * @author theodor
	 */
	private enum ArgumentValue {
		NONE, OPTIONAL, REQUIRED;
	}

	/**
	 * The enum specifying the possible arguments that this program can handle.
	 * 
	 * @author theodor
	 */
	private enum Argument {
		DEBUG('d', ArgumentValue.NONE, (short) 7, "debug") {
			@Override
			public void onReceived(Arguments inst, String val) {
				inst.debug = true;
			}

			@Override
			public String[] getDescription() {
				return new String[] { "Enables additional log output about issues and the current program state.",
						"Usefull for debugging issues, but likely not useful for the average user." };
			}
		};

		/**
		 * Creates a new Argument instance.
		 * 
		 * @param shortArg The single character argument, aka the short arg, to be given
		 *                 for this argument.
		 * @param value    Whether this argument handles a value, as well as whether
		 *                 this value is optional.<br/>
		 *                 Range: 0 - 10.
		 * @param priority The priority with which this argument is handled.<br/>
		 *                 Higher means its handled earlier.
		 * @param longArgs The text arguments, aka long args, for this argument.
		 * @throws IllegalArgumentException If {@code priority} isn't in the valid
		 *                                  range, or shortArg isn't a valid character.
		 * @throws NullPointerException     If {@code value} is {@code null}.
		 */
		private Argument(char shortArg, ArgumentValue value, short priority, String... longArgs)
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
		 * The method handling everything that needs to be done if this argument is
		 * found.
		 * 
		 * @param val  The value for this argument, or {@code null} if it doesn't have
		 *             one.<br/>
		 *             Also {@code null} if this arg has an optional value, and none was
		 *             specified.
		 * @param inst The {@link Arguments} instance to use.<br/>
		 *             Potentially changed by this method.
		 */
		public abstract void onReceived(Arguments inst, String val);

		/**
		 * Gets the description for this argument.<br/>
		 * Each string is handled as a separate line.<br/>
		 * Might be line wrapped in the future.
		 * 
		 * @return The description for this argument.
		 */
		public abstract String[] getDescription();
	}

	/**
	 * Creates a new Arguments object parsing the string arguments given to the main
	 * method.<br/>
	 * Whether all args are separate strings, or one long string doesn't
	 * matter.<br/>
	 * All strings are concatenated separated by spaces before parsing.
	 * 
	 * @param mainArgs The arguments given to the main method.
	 */
	public Arguments(String... mainArgs) {
		String args = StringUtils.join(' ', mainArgs);
		Map<Argument, String> arguments = parseArgs(args);
		for (Argument arg : arguments.keySet()) {
			arg.onReceived(this, arguments.get(arg));
		}

	}

	/**
	 * Parses the given arguments string.<br/>
	 * Something is considered the value of the argument before if either:
	 * <ul>
	 * <li>the argument has an optional value, and the value doesn't start with a
	 * hyphen, or</li>
	 * <li>the argument requires a value.</li>
	 * </ul>
	 * Strings starting with a single hyphen are considered a short arg group,
	 * containing one or more short args.<br/>
	 * Strings starting with two hyphens are considered a long arg.
	 * 
	 * Values can be quoted to allow spaces in them.<br/>
	 * Spaces and quotes can be escaped using backslashes.
	 * 
	 * @param args A string containing all the args to parse.
	 * @return A map containing the received arguments, and their values, if
	 *         applicable.
	 * @throws IllegalStateException If one if the following happened:
	 *                               <ul>
	 *                               <li>A value without an argument was found</li>
	 *                               <li>An argument that requires a value didn't
	 *                               have one.</li>
	 *                               <li>An argument that cannot have a value had
	 *                               one.</li>
	 *                               <li>An argument was found twice.</li>
	 *                               <li>An unknown argument was received.</li>
	 *                               <li>A quoted segment was opened but not
	 *                               closed.</li>
	 *                               </ul>
	 */
	public static Map<Argument, String> parseArgs(String args) throws IllegalStateException {
		final Pair<Map<Byte, Argument>, Map<String, Byte>> argsMaps = getArgsMap();

		boolean shortArgs = false;
		boolean longArg = false;
		boolean value = false;
		boolean quoted = false;
		boolean escaped = false;

		Argument currentArg = null;
		byte quote = 0;
		StringBuilder current = new StringBuilder();

		Map<Argument, String> arguments = new LinkedHashMap<Argument, String>();

		for (byte b : args.getBytes()) {
			switch (b) {
			case '-':
				if (value || longArg) {
					if (escaped) {
						current.append('\\');
						escaped = false;
					}
					current.append((char) b);
				} else if (shortArgs) {
					shortArgs = false;
					longArg = true;
				} else {
					if (currentArg != null) {
						if (currentArg.val == ArgumentValue.REQUIRED) {
							value = true;
						} else {
							if (arguments.containsKey(currentArg)) {
								throw new IllegalStateException(
										"Received duplicate argument " + currentArg.shortArg + '.');
							}

							arguments.put(currentArg, null);
							currentArg = null;
						}
					}
					shortArgs = true;
				}
				break;
			case '"':
			case '\'':
				if (quoted && quote == b && !escaped) {
					quoted = false;
				} else if (quoted) {
					escaped = false;
					current.append((char) b);
				} else {
					quoted = true;
					quote = b;
				}
				break;
			case '\\':
				if (escaped) {
					current.append((char) b);
					escaped = false;
				} else {
					escaped = true;
				}
				break;
			case ' ':
				if (value) {
					if (escaped || quoted) {
						current.append((char) b);
						escaped = false;
					} else if (current.length() > 0) {
						if (currentArg == null) {
							throw new IllegalStateException("Parsed value \"" + current.toString() + "\" without key.");
						}

						if (currentArg.val == ArgumentValue.NONE) {
							throw new IllegalStateException("Received value \"" + current.toString()
									+ "\" for argument " + currentArg.shortArg + " which doesn't take a value.");
						}

						if (arguments.containsKey(currentArg)) {
							throw new IllegalStateException("Received duplicate argument " + currentArg.shortArg + '.');
						}

						arguments.put(currentArg, current.toString());
						currentArg = null;
					}
				} else if (longArg) {
					Byte shortArg = argsMaps.getValue().get(current.toString());
					if (shortArg == null) {
						throw new IllegalStateException("Received unknown argument \"" + current.toString() + "\".");
					}
					currentArg = argsMaps.getKey().get(shortArg);
					current = new StringBuilder();
					longArg = false;
				} else {
					shortArgs = false;
				}
				break;
			default:
				if (value || longArg) {
					if (escaped) {
						current.append('\\');
						escaped = false;
					}
					current.append((char) b);
				} else if (shortArgs) {
					if (currentArg != null && currentArg.val == ArgumentValue.REQUIRED) {
						throw new IllegalStateException("Argument " + currentArg.shortArg + " requires a value.");
					}

					if (!argsMaps.getKey().containsKey(b)) {
						throw new IllegalStateException("Received unknown argument " + (char) b + '.');
					}

					if (currentArg != null) {
						if (arguments.containsKey(currentArg)) {
							throw new IllegalStateException("Received duplicate argument " + currentArg.shortArg + '.');
						}

						arguments.put(currentArg, null);
					}
					currentArg = argsMaps.getKey().get(b);
				} else {
					value = true;
					current.append((char) b);
				}
				break;
			}
		}

		if (quoted) {
			throw new IllegalStateException("Unterminated " + (char) quote + " in arguments.");
		}

		if (value) {
			if (escaped) {
				current.append('\\');
				escaped = false;
			}

			if (currentArg == null) {
				throw new IllegalStateException("Parsed value \"" + current.toString() + "\" without key.");
			}

			if (currentArg.val == ArgumentValue.NONE) {
				throw new IllegalStateException("Received value \"" + current.toString() + "\" for argument "
						+ currentArg.shortArg + " which doesn't take a value.");
			}

			if (arguments.containsKey(currentArg)) {
				throw new IllegalStateException("Received duplicate argument " + currentArg.shortArg + '.');
			}

			arguments.put(currentArg, current.toString());
			currentArg = null;
		} else if (longArg) {
			Byte shortArg = argsMaps.getValue().get(current.toString());
			if (shortArg == null) {
				throw new IllegalStateException("Received unknown argument \"" + current.toString() + "\".");
			}

			currentArg = argsMaps.getKey().get(shortArg);
			current = new StringBuilder();
			longArg = false;
		}

		if (currentArg != null) {
			if (currentArg.val == ArgumentValue.REQUIRED) {
				throw new IllegalStateException("Argument " + currentArg.shortArg + " requires a value.");
			}

			if (arguments.containsKey(currentArg)) {
				throw new IllegalStateException("Received duplicate argument " + currentArg.shortArg + '.');
			}

			arguments.put(currentArg, null);
			currentArg = null;
		}

		return MapUtils.sortByKey(arguments, Collections.reverseOrder(ArgumentPriorityComparator.INSTANCE));
	}

	/**
	 * Creates a short arg to {@link Argument} map and a long arg to short arg map.
	 * 
	 * @return A {@link Pair} containing the two maps.
	 * @throws IllegalStateException If there are two {@link Argument Arguments}
	 *                               with the same short arg or long arg.
	 */
	private static Pair<Map<Byte, Argument>, Map<String, Byte>> getArgsMap() throws IllegalStateException {
		HashMap<Byte, Argument> shortArgToArgument = new HashMap<Byte, Argument>(
				(int) Math.ceil(Argument.values().length / 0.75));
		HashMap<String, Byte> longArgToShortArg = new HashMap<String, Byte>();

		for (Argument arg : Argument.values()) {
			if (shortArgToArgument.containsKey((byte) arg.shortArg)) {
				throw new IllegalStateException(
						"There are multiple arguments with the short arg " + arg.shortArg + '.');
			}

			shortArgToArgument.put((byte) arg.shortArg, arg);
			for (String longArg : arg.longArgs) {
				if (longArgToShortArg.containsKey(longArg)) {
					throw new IllegalStateException("Duplicate long arg \"" + longArg + "\".");
				}

				longArgToShortArg.put(longArg, (byte) arg.shortArg);
			}
		}

		return new Pair<Map<Byte, Argument>, Map<String, Byte>>(shortArgToArgument, longArgToShortArg);
	}

	/**
	 * A {@link Comparator} comparing {@link Argument Arguments} by their priority.
	 * 
	 * @author theodor
	 */
	private static class ArgumentPriorityComparator implements Comparator<Argument> {

		/**
		 * The only instance of this comparator to be used.
		 */
		public static final ArgumentPriorityComparator INSTANCE = new ArgumentPriorityComparator();

		/**
		 * The only constructor to create a new instance.<br/>
		 * Private to prevent other instances being created..
		 */
		private ArgumentPriorityComparator() {
		}

		@Override
		public int compare(Argument a1, Argument a2) {
			return Short.compare(a1.prio, a2.prio);
		}

	}

}
