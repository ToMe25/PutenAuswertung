package com.tome25.auswertung.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import com.tome25.auswertung.log.LogHandler;

/**
 * A utility class for getting a console {@link BufferedReader reader} and
 * {@link BufferedWriter writer}.<br/>
 * Defaults to system streams when {@link System#console()} is {@code null}.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class ConsoleHelper {

	/**
	 * The reader using which console input can be read.
	 */
	private static BufferedReader inputReader;

	/**
	 * The writer using which console output can be written.
	 */
	private static BufferedWriter outputWriter;

	/**
	 * Gets a {@link BufferedReader} for the console input stream, or for
	 * {@link System#in} if {@link System#console()} is {@code null}.<br/>
	 * This {@link BufferedReader} is created on the first call, and then stored for
	 * later calls.
	 * 
	 * @return A console input {@link BufferedReader}.
	 */
	public static BufferedReader getConsoleReader() {
		if (inputReader == null) {
			Console cons = System.console();
			if (cons != null) {
				inputReader = new BufferedReader(cons.reader());
			} else {
				inputReader = new BufferedReader(new InputStreamReader(System.in));
			}
		}

		return inputReader;
	}

	/**
	 * Gets a {@link BufferedWriter} for the console output stream, or for
	 * {@link System#out} if {@link System#console()} is {@code null}.<br/>
	 * This {@link BufferedWriter} is created on the first call, and then stored for
	 * later calls.
	 * 
	 * @return A console output {@link BufferedWriter}.
	 */
	public static BufferedWriter getConsoleWriter() {
		if (outputWriter == null) {
			Console cons = System.console();
			if (cons != null) {
				outputWriter = new BufferedWriter(cons.writer());
			} else {
				outputWriter = new BufferedWriter(new OutputStreamWriter(LogHandler.getInitialSysOut()));
			}
		}

		return outputWriter;
	}

}
