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
	 * Returns {@code null} if it can't read a line.
	 * 
	 * @return The next line from this input.
	 * @throws IOException if an I/O Error occurs.
	 */
	String readline() throws IOException;

	/**
	 * Checks whether more content is available on this input.<br/>
	 * While {@link #done} only checks whether there will be input eventually this
	 * checks whether input is available right now.<br/>
	 * {@code false} if this stream handler has already been closed.
	 * 
	 * @return {@code true} if more input is available right now, without blocking.
	 * 
	 * @see #done
	 */
	boolean available();

	/**
	 * Checks whether this input is read in its entirety.<br/>
	 * If this returns {@code true} there should be no more data to be read from
	 * this input.<br/>
	 * If this is the only input that means the program can finish.<br/>
	 * This returning {@code true} does not necessarily mean input is available
	 * right now.<br/>
	 * In some cases this can change from {@code true} to {@code false}, but
	 * generally it doesn't.<br/>
	 * For example, a file that has been completely read is considered done, but if
	 * more gets written into it later that could change it to not done.<br/>
	 * A closed stream handler is not necessarily considered done.
	 * 
	 * @return {@code true} If more content is to be read from this input, now or
	 *         later.
	 * 
	 * @see #available
	 */
	boolean done();

}
