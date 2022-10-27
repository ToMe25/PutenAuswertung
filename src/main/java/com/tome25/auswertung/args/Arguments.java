package com.tome25.auswertung.args;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tome25.auswertung.LogHandler;
import com.tome25.auswertung.args.Argument.ArgumentValue;
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
	 * Whether an argument disabling all system output was given.
	 */
	public boolean silent = false;

	/**
	 * Creates a new Arguments object parsing the string arguments given to the main
	 * method.<br/>
	 * Whether all args are separate strings, or one long string doesn't
	 * matter.<br/>
	 * All strings are concatenated separated by spaces before parsing.
	 * 
	 * @param mainArgs The arguments given to the main method.
	 * @throws IllegalStateException If parsing the arguments string(the
	 *                               {@code mainArgs} separated by spaces) fails.
	 */
	public Arguments(String... mainArgs) throws IllegalStateException {
		String args = StringUtils.join(' ', mainArgs);
		Map<Argument, String> arguments = parseArgs(args);
		boolean dbg = arguments.containsKey(Argument.DEBUG) || arguments.containsKey(Argument.VERBOSE);
		LogHandler.setDebug(dbg);
		LogHandler.setSilent(arguments.containsKey(Argument.SILENT));
		for (Argument arg : arguments.keySet()) {
			if (dbg) {
				LogHandler.out_println("Received argument " + arg.name().toLowerCase() + ".", true);
			}
			arg.onReceived(this, arguments.get(arg));
		}

		// In case another argument later implicitly sets one of them.
		LogHandler.setDebug(debug);
		LogHandler.setSilent(silent);
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
		// TODO replace exceptions with custom exception.
		final Pair<Map<Character, Argument>, Map<String, Character>> argsMaps = getArgsMap();

		boolean shortArgs = false;
		boolean longArg = false;
		boolean value = false;
		boolean quoted = false;
		boolean escaped = false;

		Argument currentArg = null;
		char quote = 0;
		StringBuilder current = new StringBuilder();

		Map<Argument, String> arguments = new LinkedHashMap<Argument, String>();

		char[] chars = args.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			char c = chars[i];
			switch (c) {
			case '-':
				if (value || longArg) {
					escaped = false;
					current.append(c);
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
										"Duplicate " + currentArg.name().toLowerCase() + " argument received.");
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
				if (quoted && quote == c && !escaped) {
					quoted = false;
				} else if (quoted || escaped) {
					escaped = false;
					current.append(c);
				} else {
					quoted = true;
					quote = c;
				}
				break;
			case '\\':
				if (quoted || escaped) {
					current.append(c);
					escaped = false;
				} else {
					escaped = true;
				}
				break;
			case ' ':
				if (value) {
					if (escaped) {
						current.append(c);
						escaped = false;
						break;
					} else if (quoted) {
						if (current.length() > 0 || chars[i - 1] == '"' || chars[i - 1] == '\'') {
							current.append(c);
							break;
						}
					} else if (current.length() > 0) {
						boolean nextArg = true;
						for (int j = i + 1; j < chars.length; j++) {
							if (chars[j] == '-') {
								nextArg = true;
								break;
							} else if (!Character.isWhitespace(chars[j])) {
								nextArg = false;
								break;
							}
						}

						if (!nextArg) {
							current.append(c);
							break;
						}

						if (currentArg == null) {
							throw new IllegalStateException("Received value \"" + current.toString() + "\" without key.");
						}

						if (currentArg.val == ArgumentValue.NONE) {
							throw new IllegalStateException(
									"Received value \"" + current.toString() + "\" for argument "
											+ currentArg.name().toLowerCase() + " which doesn't take a value.");
						}

						if (arguments.containsKey(currentArg)) {
							throw new IllegalStateException(
									"Duplicate " + currentArg.name().toLowerCase() + " argument received.");
						}

						arguments.put(currentArg, current.toString());
						current = new StringBuilder();
						currentArg = null;
						value = false;
					}
				} else if (escaped) {
					throw new IllegalStateException("The space separating the \""
							+ (longArg ? current.toString() : currentArg.name().toLowerCase())
							+ "\" argument from its value is escaped.");
				} else if (longArg) {
					Character shortArg = argsMaps.getValue().get(current.toString());
					if (shortArg == null) {
						throw new IllegalStateException("Received unknown argument \"" + current.toString() + "\".");
					}
					currentArg = argsMaps.getKey().get(shortArg);
					current = new StringBuilder();
					longArg = false;
				} else {
					shortArgs = false;
				}

				if (!value) {
					boolean nextArg = true;
					for (int j = i + 1; j < chars.length; j++) {
						if (chars[j] == '-') {
							nextArg = true;
							break;
						} else if (!Character.isWhitespace(chars[j]) && chars[j] != '"' && chars[j] != '\''
								&& chars[j] != '\\') {
							nextArg = false;
							break;
						}
					}

					if (!nextArg) {
						value = true;
					}
				}
				break;
			default:
				if (value || longArg) {
					if (escaped) {
						current.append('\\');
						escaped = false;
					}
					current.append(c);
				} else if (shortArgs) {
					if (currentArg != null && currentArg.val == ArgumentValue.REQUIRED) {
						throw new IllegalStateException(
								"Argument " + currentArg.name().toLowerCase() + " requires a value.");
					}

					if (!argsMaps.getKey().containsKey(c)) {
						throw new IllegalStateException("Received unknown argument " + c + '.');
					}

					if (currentArg != null) {
						if (arguments.containsKey(currentArg)) {
							throw new IllegalStateException(
									"Duplicate " + currentArg.name().toLowerCase() + " argument received.");
						}

						arguments.put(currentArg, null);
					}
					currentArg = argsMaps.getKey().get(c);
				} else {
					value = true;
					current.append(c);
				}
				break;
			}
		}

		if (quoted) {
			throw new IllegalStateException("Unterminated " + (char) quote + " in arguments.");
		}

		if (value && current.length() > 0) {
			if (escaped) {
				current.append('\\');
				escaped = false;
			}

			if (currentArg == null) {
				throw new IllegalStateException("Received value \"" + current.toString() + "\" without key.");
			}

			if (currentArg.val == ArgumentValue.NONE) {
				throw new IllegalStateException("Received value \"" + current.toString() + "\" for argument "
						+ currentArg.name().toLowerCase() + " which doesn't take a value.");
			}

			if (arguments.containsKey(currentArg)) {
				throw new IllegalStateException("Duplicate " + currentArg.name().toLowerCase() + " argument received.");
			}

			arguments.put(currentArg, current.toString());
			currentArg = null;
		} else if (longArg) {
			Character shortArg = argsMaps.getValue().get(current.toString());
			if (shortArg == null) {
				throw new IllegalStateException("Received unknown argument \"" + current.toString() + "\".");
			}

			currentArg = argsMaps.getKey().get(shortArg);
			current = new StringBuilder();
			longArg = false;
		}

		if (currentArg != null) {
			if (currentArg.val == ArgumentValue.REQUIRED) {
				throw new IllegalStateException("Argument " + currentArg.name().toLowerCase() + " requires a value.");
			}

			if (arguments.containsKey(currentArg)) {
				throw new IllegalStateException("Duplicate " + currentArg.name().toLowerCase() + " argument received.");
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
	private static Pair<Map<Character, Argument>, Map<String, Character>> getArgsMap() throws IllegalStateException {
		HashMap<Character, Argument> shortArgToArgument = new HashMap<Character, Argument>(
				(int) Math.ceil(Argument.values().length / 0.75));
		HashMap<String, Character> longArgToShortArg = new HashMap<String, Character>();

		for (Argument arg : Argument.values()) {
			if (shortArgToArgument.containsKey(arg.shortArg)) {
				throw new IllegalStateException(
						"There are multiple arguments with the short arg " + arg.shortArg + '.');
			}

			shortArgToArgument.put(arg.shortArg, arg);
			for (String longArg : arg.longArgs) {
				if (longArgToShortArg.containsKey(longArg)) {
					throw new IllegalStateException("Duplicate long arg \"" + longArg + "\".");
				}

				longArgToShortArg.put(longArg, arg.shortArg);
			}
		}

		return new Pair<Map<Character, Argument>, Map<String, Character>>(shortArgToArgument, longArgToShortArg);
	}

}
