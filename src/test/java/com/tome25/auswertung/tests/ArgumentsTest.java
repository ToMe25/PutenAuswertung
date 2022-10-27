package com.tome25.auswertung.tests;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.tome25.auswertung.LogHandler;
import com.tome25.auswertung.args.Argument;
import com.tome25.auswertung.args.Arguments;

/**
 * A class containing tests verifying the functionality of the command line
 * argument parser.
 * 
 * @author theodor
 */
public class ArgumentsTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * Test parsing a single short arg without a value.
	 */
	@Test
	public void shortArgNoneNoValue() {
		Map<Argument, String> args = Arguments.parseArgs("-h");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a short arg with a {@link Argument.ArgumentValue#NONE NONE} with
	 * a value.
	 * 
	 * @throws IllegalStateException Always.
	 */
	@Test
	public void shortArgNoneExistingValue() throws IllegalStateException {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("Received value \"Debug\" for argument debug which doesn't take a value.");
		Arguments.parseArgs("-d Debug");
	}

	/**
	 * Test parsing a short arg without an optional value.
	 */
	@Test
	public void shortArgOptionalNoValue() {
		Map<Argument, String> args = Arguments.parseArgs("-D");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a short arg optionally expecting a value with a value.
	 */
	@Test
	public void shortArgOptionalValue() {
		Map<Argument, String> args = Arguments.parseArgs("-D Test");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "Test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	// TODO add tests for short args with required values.

	/**
	 * Test parsing a single long arg without a value from a string.
	 */
	@Test
	public void longArgNoneNoValue() {
		Map<Argument, String> args = Arguments.parseArgs("--verbose");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.VERBOSE, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a single long arg with value type
	 * {@link Argument.ArgumentValue#NONE NONE} with a value.
	 * 
	 * @throws IllegalStateException Always.
	 */
	@Test
	public void longArgNoneValue() throws IllegalStateException {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("Received value \"Value\" for argument verbose which doesn't take a value.");
		Arguments.parseArgs("--verbose Value");
	}

	/**
	 * Test parsing a single argument without an optional argument.
	 */
	@Test
	public void longArgOptionalNoValue() {
		Map<Argument, String> args = Arguments.parseArgs("--docs");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a single argument with an optional argument.
	 */
	@Test
	public void longArgOptionalValue() {
		Map<Argument, String> args = Arguments.parseArgs("--docs Test");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "Test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	// TODO add tests for required values.

	/**
	 * Test parsing multiple short args without values after a single hyphen.
	 */
	@Test
	public void compoundShortArgsNoValue() {
		Map<Argument, String> args = Arguments.parseArgs("-dhv");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DEBUG, null);
		expected.put(Argument.VERBOSE, null);
		expected.put(Argument.HELP, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing multiple short args, the last of which has a value.
	 */
	@Test
	public void compoundShortArgsValue() {
		Map<Argument, String> args = Arguments.parseArgs("-dvD Test");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DEBUG, null);
		expected.put(Argument.VERBOSE, null);
		expected.put(Argument.DOCS, "Test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	// TODO add test with a required value in the middle

	/**
	 * Test parsing multiple short args as separate arguments.
	 */
	@Test
	public void separateShortArgsNoValue() {
		Map<Argument, String> args = Arguments.parseArgs("-d -v");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DEBUG, null);
		expected.put(Argument.VERBOSE, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing multiple separate short args of which an earlier one has a
	 * value.
	 */
	@Test
	public void separateShortArgsValue() {
		Map<Argument, String> args = Arguments.parseArgs("-D DOCS -v -h");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "DOCS");
		expected.put(Argument.VERBOSE, null);
		expected.put(Argument.HELP, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing multiple long args without a value.
	 */
	@Test
	public void multipleLongArgsNoValue() {
		Map<Argument, String> args = Arguments.parseArgs("--help --docs");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		expected.put(Argument.DOCS, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing multiple long args the first of which has a value.
	 */
	@Test
	public void multipleLongArgsValue() {
		Map<Argument, String> args = Arguments.parseArgs("--docs test --help");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test");
		expected.put(Argument.HELP, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a string containing short and long args.
	 */
	@Test
	public void mixedArgsNoValue() {
		Map<Argument, String> args = Arguments.parseArgs("-dD --verbose");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DEBUG, null);
		expected.put(Argument.VERBOSE, null);
		expected.put(Argument.DOCS, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing mixed short and long args one of which has a value.
	 */
	@Test
	public void mixedArgsValue() {
		Map<Argument, String> args = Arguments.parseArgs("--help -vD test");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		expected.put(Argument.VERBOSE, null);
		expected.put(Argument.DOCS, "test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Tests parsing an arguments string starting with spaces.
	 */
	@Test
	public void spacesBeginning() {
		Map<Argument, String> args = Arguments.parseArgs("  -hv");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		expected.put(Argument.VERBOSE, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * A test parsing an argument without a value with spaces after.
	 */
	@Test
	public void spacesEndNoValue() {
		Map<Argument, String> args = Arguments.parseArgs("--help    ");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test multiple spaces between arg and value.
	 */
	@Test
	public void multipleSpacesBetweenArgAndValue() {
		Map<Argument, String> args = Arguments.parseArgs("--docs   test");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing an argument with a value with spaces after it.
	 */
	@Test
	public void spacesEndValue() {
		Map<Argument, String> args = Arguments.parseArgs("--debug -D test    ");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DEBUG, null);
		expected.put(Argument.DOCS, "test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing value containing a space without quotes.
	 */
	@Test
	public void spaceMidValue() {
		Map<Argument, String> args = Arguments.parseArgs("-D test directory");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test directory");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test multiple unescaped spaces mid value.
	 */
	@Test
	public void spacesMidValue() {
		Map<Argument, String> args = Arguments.parseArgs("-D test  test");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test  test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a value ending with an escaped space.
	 */
	@Test
	public void spaceEndValueEscaped() {
		Map<Argument, String> args = Arguments.parseArgs("-D dir\\ ");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "dir ");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a value starting with an escaped space.
	 */
	@Test
	public void spaceBeginningValueEscaped() {
		Map<Argument, String> args = Arguments.parseArgs("-h -D \\ whatever");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		expected.put(Argument.DOCS, " whatever");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a short arg without a value in single quotes.
	 */
	@Test
	public void shortArgNoValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("'-v'");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.VERBOSE, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a short arg and its value in the same pair of double quotes.
	 */
	@Test
	public void shortArgAndValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("\"-D test\"");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a short arg with its value in single quotes.
	 */
	@Test
	public void shortArgValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("-D 'docs-dir'");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "docs-dir");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a quoted compound short arg without a value.
	 */
	@Test
	public void shortArgsCompoundNoValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("'-vD'");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.VERBOSE, null);
		expected.put(Argument.DOCS, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a quoted compound short arg with a value.
	 */
	@Test
	public void shortArgsCompoundValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("'-vD test'");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.VERBOSE, null);
		expected.put(Argument.DOCS, "test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test multiple single short args in the same pair of single quotes.
	 */
	@Test
	public void shortArgsNoValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("'-h -v -d'");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		expected.put(Argument.VERBOSE, null);
		expected.put(Argument.DEBUG, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a single long arg in double quotes.
	 */
	@Test
	public void longArgNoValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("\"--docs\"");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a long arg and its value in the same pair of double quotes.
	 */
	@Test
	public void longArgAndValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("\"--docs documentation\"");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "documentation");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a long arg with a quoted value.
	 */
	@Test
	public void longArgValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("--docs 'something'");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "something");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing multiple long args in a single pair of double quotes.
	 */
	@Test
	public void longArgsQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("\"--help --debug\"");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		expected.put(Argument.DEBUG, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a quoted long arg with spaces in front of it.
	 */
	@Test
	public void spacesBeginningQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("\"  --help\"");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a quoted long arg without a value with spaces after it.
	 */
	@Test
	public void spacesEndNoValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("'--docs   '");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, null);
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a quoted long arg with a value with multiple spaces between
	 * them.
	 */
	@Test
	public void spacesArgAndValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("'--docs  test'");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing spaces at the beginning of a quoted value.
	 */
	@Test
	public void spacesBeginningValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("-D \"  test\"");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "  test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing spaces at the end of a quoted value.
	 */
	@Test
	public void spacesEndValueQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("-D 'test  '");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test  ");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing an argument with a value followed by another argument in a
	 * single pair of quotes.<br/>
	 * This should be parsed as a single argument with a value.
	 */
	@Test
	public void argumentAfterValueInQuotes() {
		Map<Argument, String> args = Arguments.parseArgs("'-D test --help'");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test --help");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test an escaped hyphen after a space in a value.
	 */
	@Test
	public void argumentValueHyphenAfterSpaceEscaped() {
		Map<Argument, String> args = Arguments.parseArgs("-D test \\-test");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test -test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test a hyphen after a space in a quoted value.
	 */
	@Test
	public void argumentValueHyphenAfterSpaceQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("-D 'test -test'");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "test -test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing an escaped double quote in a value.
	 */
	@Test
	public void argumentValueQuoteEscaped() {
		Map<Argument, String> args = Arguments.parseArgs("-D some \\\"test");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "some \"test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a double quoted value containing a single quote.
	 */
	@Test
	public void argumentValueQuoteQuoted() {
		Map<Argument, String> args = Arguments.parseArgs("-D \"some 'test\"");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "some 'test");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test parsing a single backslash in the middle of a value.
	 */
	@Test
	public void argumentValueSingleBackslash() {
		Map<Argument, String> args = Arguments.parseArgs("-hD some\\dir");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.HELP, null);
		expected.put(Argument.DOCS, "some\\dir");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test a value ending with a single backslash.
	 */
	@Test
	public void argumentValueEndSingleBackslash() {
		Map<Argument, String> args = Arguments.parseArgs("-D something\\");
		Map<Argument, String> expected = new HashMap<Argument, String>();
		expected.put(Argument.DOCS, "something\\");
		assertEquals("The parsed arguments did not match expectations.", expected, args);
	}

	/**
	 * Test the space separating a short arg and its value being escaped.
	 * 
	 * @throws IllegalStateException Always.
	 */
	@Test
	public void shortArgValueSeparatorEscaped() throws IllegalStateException {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("The space separating the \"docs\" argument from its value is escaped.");
		Arguments.parseArgs("-D\\ test");
	}

	/**
	 * Test the space separating a long arg and its value being escaped.
	 * 
	 * @throws IllegalStateException Always.
	 */
	@Test
	public void longArgValueSeparatorEscaped() throws IllegalStateException {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("The space separating the \"docs\" argument from its value is escaped.");
		Arguments.parseArgs("--docs\\ test");
	}

	/**
	 * Test parsing a string containing an unterminated double quote.
	 * 
	 * @throws IllegalStateException Always.
	 */
	@Test
	public void unmatchedQuotes() throws IllegalStateException {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("Unterminated \" in arguments.");
		Arguments.parseArgs("\"-h -v");
	}

	/**
	 * Test an argument existing in both its long arg an short arg form.
	 * 
	 * @throws IllegalStateException Always.
	 */
	@Test
	public void duplicateArgument() throws IllegalStateException {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("Duplicate help argument received.");
		Arguments.parseArgs("-h --help");
	}

	/**
	 * Test parsing a {@code null} arguments string.
	 * 
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void parseNullArguments() throws NullPointerException {
		Arguments.parseArgs(null);
	}

	/**
	 * Test running the {@link Arguments} constructor without any args.
	 */
	@Test
	public void argumentsConstructorNoArg() {
		boolean oldDebug = LogHandler.isDebug();
		new Arguments();
		LogHandler.setDebug(oldDebug);
	}

	/**
	 * Test running the {@link Arguments} constructor with a {@code null} arguments
	 * array.
	 */
	@Test
	public void argumentsConstructorNull() {
		boolean oldDebug = LogHandler.isDebug();
		new Arguments((String[]) null);
		LogHandler.setDebug(oldDebug);
	}

}
