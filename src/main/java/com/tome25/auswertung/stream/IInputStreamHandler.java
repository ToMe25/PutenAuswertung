package com.tome25.auswertung.stream;

import java.io.Closeable;
import java.io.IOException;

/**
 * The interface defining the common methods for all types of input handling.
 * Currently planned are one for System Input and one for a File.
 * 
 * @author theodor
 */
public interface IInputStreamHandler extends Closeable {

	/**
	 * Reads the next line of the input handled by this Object.<br/>
	 * Blocks until it got a line.<br/>
	 * Returns <code>null</code> if it can't read a line.
	 * 
	 * @return	The next line from this input.
	 * @throws	IOException if an I/O Error occurs.
	 */
	String readline() throws IOException;

	/**
	 * Checks whether more content is available on this input.<br/>
	 * While {@link done} only checks whether there will be input eventually this
	 * checks whether input is available right now.
	 * 
	 * @see done
	 * @return	<code>true</code> if more input is available right now, aka without
	 *         blocking.
	 */
	boolean available();

	/**
	 * Checks whether this input is read in its entirety.<br/>
	 * If this returns
	 * <code>true<code> there should be no more data to be read from this input.<br/>
	 * If this is the only input that means the program can finish.<br/>
	 * This returning <code>true</code> does not necessarily mean input is available
	 * right now.
	 * 
	 * @see available
	 * @return	<code>true</code> If more content is to be read from this input, now
	 *         or later.
	 */
	boolean done();

}
