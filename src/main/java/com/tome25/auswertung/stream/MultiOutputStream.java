package com.tome25.auswertung.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An {@link OutputStream} wrapping multiple other {@link OutputStream
 * OutputStreams} and writing all the data it gets to all of them.<br/>
 * Every {@code write} call will throw an {@link IOException} if a
 * {@code MultiOutputStream} doesn't have an {@link OutputStream} to write
 * to.<br/>
 * Cannot contain the same {@link OutputStream} twice.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class MultiOutputStream extends OutputStream {

	/**
	 * The {@link Collection} of {@link OutputStream OutputStreams} to write
	 * to.<br/>
	 * Cannot contain the same {@link OutputStream} twice.
	 */
	private Collection<OutputStream> streams;

	/**
	 * Whether this {@code MultiOutputStream}s {@link MultiOutputStream#close()}
	 * method has already been called.
	 */
	private volatile boolean closed = false;

	/**
	 * Creates a new {@code MultiOutputStream} without any {@link OutputStream} to
	 * write to.<br/>
	 * Every {@code write} call will throw an {@link IOException} if a
	 * {@code MultiOutputStream} doesn't have an {@link OutputStream} to write to.
	 */
	public MultiOutputStream() {
		streams = new LinkedHashSet<OutputStream>();
	}

	/**
	 * Creates a new {@code MultiOutputStream} writing to the given
	 * {@link OutputStream OutputStreams}.<br/>
	 * The given {@link OutputStream OutputStreams} will automatically be
	 * deduplicated.
	 * 
	 * @param streams The {@link OutputStream OutputStreams} to write to.
	 */
	public MultiOutputStream(OutputStream... streams) {
		if (streams == null || streams.length == 0) {
			this.streams = new LinkedHashSet<OutputStream>();
		} else {
			this.streams = new LinkedHashSet<OutputStream>(Arrays.asList(streams));
		}
		this.streams.remove(null);
	}

	/**
	 * Creates a new {@code MultiOutputStream} writing to the given
	 * {@link OutputStream OutputStreams}.<br/>
	 * If {@code streams} is {@code null} a new {@link LinkedHashSet} is used as the
	 * internal {@code streams} {@link Collection}.<br/>
	 * If {@code streams} is a {@link Set} it is used as the internal
	 * {@link OutputStream} list.<br/>
	 * Otherwise a new {@link LinkedHashSet} with the content of {@code streams} is
	 * used.
	 * 
	 * @param streams The streams to write to.
	 */
	public MultiOutputStream(Collection<OutputStream> streams) {
		if (streams == null) {
			this.streams = new LinkedHashSet<OutputStream>();
		} else if (streams instanceof Set) {
			this.streams = (Set<OutputStream>) streams;
		} else {
			this.streams = new LinkedHashSet<OutputStream>(streams);
		}

		try {
			streams.remove(null);
		} catch (NullPointerException e) {
			// Some sets do not support null elements, and we don't care.
		}
	}

	/**
	 * Adds the given {@link OutputStream} to the list of streams to write to.
	 * 
	 * @param stream The stream to add.
	 * @return {@code true} if the stream was added successfully, aka if it was not
	 *         yet used.
	 * @throws NullPointerException If {@code stream} is {@code null}.
	 */
	public synchronized boolean addStream(OutputStream stream) throws NullPointerException {
		Objects.requireNonNull(stream, "The stream to add to this MultiOutputStream can't be null.");

		return streams.add(stream);
	}

	/**
	 * Removes the given {@link OutputStream} from the list of streams to write
	 * to.<br/>
	 * Should be called, for example, when one of the underlying streams was closed.
	 * 
	 * @param stream The stream to stop writing to.
	 * @return {@code true} if the stream was removed successfully.
	 * @throws NullPointerException If {@code stream} is {@code null}.
	 */
	public synchronized boolean removeStream(OutputStream stream) throws NullPointerException {
		Objects.requireNonNull(stream, "The stream to remove cannot be null.");

		return streams.remove(stream);
	}

	@Override
	public synchronized void write(int b) throws IOException {
		ensureOpen();

		List<IOException> exc = new ArrayList<IOException>();
		for (OutputStream str : streams) {
			if (str != null) {
				try {
					str.write(b);
				} catch (IOException e) {
					exc.add(e);
				}
			}
		}

		rethrow(exc);
	}

	@Override
	public synchronized void write(byte[] b) throws IOException {
		ensureOpen();

		List<IOException> exc = new ArrayList<IOException>();
		for (OutputStream str : streams) {
			if (str != null) {
				try {
					str.write(b);
				} catch (IOException e) {
					exc.add(e);
				}
			}
		}

		rethrow(exc);
	}

	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		ensureOpen();

		List<IOException> exc = new ArrayList<IOException>();
		for (OutputStream str : streams) {
			if (str != null) {
				try {
					str.write(b, off, len);
				} catch (IOException e) {
					exc.add(e);
				}
			}
		}

		rethrow(exc);
	}

	@Override
	public synchronized void flush() throws IOException {
		ensureOpen();

		List<IOException> exc = new ArrayList<IOException>();
		for (OutputStream str : streams) {
			if (str != null) {
				try {
					str.flush();
				} catch (IOException e) {
					exc.add(e);
				}
			}
		}

		rethrow(exc);
	}

	@Override
	public synchronized void close() throws IOException {
		if (closed) {
			return;
		}

		closed = true;
		List<IOException> exc = new ArrayList<IOException>();
		for (OutputStream str : streams) {
			if (str != null) {
				try {
					str.close();
				} catch (IOException e) {
					exc.add(e);
				}
			}
		}

		rethrow(exc);
	}

	/**
	 * Makes sure this {@code MultiOutputStream}
	 * <ol>
	 * <li>Hasn't been closed, and</li>
	 * <li>Has at least one {@link OutputStream} to write to.</li>
	 * </ol>
	 * 
	 * @throws IOException If this {@code MultiOutputStream} isn't open.
	 */
	private synchronized void ensureOpen() throws IOException {
		if (closed) {
			throw new IOException("Stream closed");
		} else if (streams.isEmpty()) {
			throw new IOException("No sink stream");
		}
	}

	/**
	 * Throws the first element of {@code exc} adding all following exceptions using
	 * {@link Throwable#addSuppressed}.
	 * 
	 * @param exc The exceptions to handle.
	 * @throws IOException          If {@code exc} contains one or more
	 *                              {@link IOException}.
	 * @throws NullPointerException If {@code exc} is {@code null}.
	 */
	private void rethrow(List<IOException> exc) throws IOException, NullPointerException {
		Objects.requireNonNull(exc, "The collection of exceptions to handle cannot be null.");

		if (!exc.isEmpty()) {
			IOException first = exc.get(0);
			for (int i = 1; i < exc.size(); i++) {
				first.addSuppressed(exc.get(i));
			}
			throw first;
		}
	}

}
