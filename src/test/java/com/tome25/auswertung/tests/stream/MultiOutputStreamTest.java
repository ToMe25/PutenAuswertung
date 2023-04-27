package com.tome25.auswertung.tests.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.tome25.auswertung.stream.MultiOutputStream;

/**
 * Some unit tests relating to the {@link MultiOutputStream} class.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class MultiOutputStreamTest {

	/**
	 * A simple test writing to a single {@link OutputStream}.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void writeSingle() throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		MultiOutputStream mout = new MultiOutputStream(bout);
		final String content = "Test message";
		mout.write(content.getBytes());
		assertEquals("The content of the target stream didn't match.", content, bout.toString());
		mout.close();
	}

	/**
	 * A test writing to multiple {@link OutputStream OutputStreams}.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void writeMulti() throws IOException {
		ByteArrayOutputStream bout1 = new ByteArrayOutputStream();
		ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
		ByteArrayOutputStream bout3 = new ByteArrayOutputStream();
		MultiOutputStream mout = new MultiOutputStream(bout1, bout2, bout3);
		final String content = "What should i write here?";
		mout.write(content.getBytes());
		assertEquals("The content of the first target stream didn't match.", content, bout1.toString());
		assertEquals("The content of the second target stream didn't match.", content, bout2.toString());
		assertEquals("The content of the third target stream didn't match.", content, bout3.toString());
		mout.close();
	}

	/**
	 * Test writing the a {@link MultiOutputStream} without a target stream.
	 * 
	 * @throws IOException Always.
	 */
	@Test(expected = IOException.class)
	public void writeNone() throws IOException {
		@SuppressWarnings("resource")
		MultiOutputStream mout = new MultiOutputStream();
		mout.write("test".getBytes());
	}

	/**
	 * A test creating a {@link MultiOutputStream} with a {@code null} target
	 * stream.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void createNullStream() throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		MultiOutputStream mout = new MultiOutputStream(null, bout);
		final String content = "Test message";
		mout.write(content.getBytes());
		assertEquals("The content of the target stream didn't match.", content, bout.toString());
		mout.close();
	}

	/**
	 * Test creating a {@link MultiOutputStream} with a {@code null}
	 * {@link OutputStream} array.
	 * 
	 * @throws IOException Never.
	 */
	@Test
	public void createNullArray() throws IOException {
		MultiOutputStream mout = new MultiOutputStream((OutputStream[]) null);
		mout.close();
	}

	/**
	 * Test creating a {@link MultiOutputStream} with a {@code null}
	 * {@link OutputStream} {@link Collection}.
	 * 
	 * @throws IOException Never.
	 */
	@Test
	public void createNullCollection() throws IOException {
		MultiOutputStream mout = new MultiOutputStream((Collection<OutputStream>) null);
		mout.close();
	}

	/**
	 * A test creating a {@link MultiOutputStream} from an {@link OutputStream}
	 * array.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void createArray() throws IOException {
		ByteArrayOutputStream streams[] = { new ByteArrayOutputStream(), new ByteArrayOutputStream() };
		MultiOutputStream mout = new MultiOutputStream(streams);
		final String content = "Test message";
		mout.write(content.getBytes());
		assertEquals("The content of the first target stream didn't match.", content, streams[0].toString());
		assertEquals("The content of the second target stream didn't match.", content, streams[1].toString());
		mout.close();
	}

	/**
	 * Test creating a {@link MultiOutputStream} from an {@link OutputStream}
	 * {@link ArrayList}.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void createList() throws IOException {
		List<OutputStream> streams = new ArrayList<OutputStream>();
		streams.add(new ByteArrayOutputStream());
		streams.add(new ByteArrayOutputStream());
		MultiOutputStream mout = new MultiOutputStream(streams);
		final String content = "Test message";
		mout.write(content.getBytes());
		assertEquals("The content of the first target stream didn't match.", content, streams.get(0).toString());
		assertEquals("The content of the second target stream didn't match.", content, streams.get(1).toString());
		mout.close();
	}

	/**
	 * Test creating a {@link MultiOutputStream} from a {@link Set}.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void createSet() throws IOException {
		Set<OutputStream> streams = new HashSet<OutputStream>();
		streams.add(new ByteArrayOutputStream());
		streams.add(new ByteArrayOutputStream());
		MultiOutputStream mout = new MultiOutputStream(streams);
		final String content = "Test message";
		mout.write(content.getBytes());
		assertEquals("The content of the first target stream didn't match.", content, streams.toArray()[0].toString());
		assertEquals("The content of the second target stream didn't match.", content, streams.toArray()[1].toString());
		mout.close();
	}

	/**
	 * A test creating a {@link MultiOutputStream} from an array containing a
	 * duplicate entry.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void createArrayDuplicate() throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ByteArrayOutputStream streams[] = { bout, bout };
		MultiOutputStream mout = new MultiOutputStream(streams);
		final String content = "Some Test message";
		mout.write(content.getBytes());
		assertEquals("The content of the target stream didn't match.", content, bout.toString());
		mout.close();

	}

	/**
	 * A test creating a {@link MultiOutputStream} from an {@link ArrayList}
	 * containing a duplicate entry.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void createListDuplicate() throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		List<OutputStream> streams = new ArrayList<OutputStream>();
		streams.add(bout);
		streams.add(bout);
		MultiOutputStream mout = new MultiOutputStream(streams);
		final String content = "Another test message.";
		mout.write(content.getBytes());
		assertEquals("The content of the target stream didn't match.", content, bout.toString());
		mout.close();
	}

	/**
	 * Test writing to an already closed {@link MultiOutputStream}.
	 * 
	 * @throws IOException Always.
	 */
	@Test(expected = IOException.class)
	public void writeAfterClose() throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		MultiOutputStream mout = new MultiOutputStream(bout);
		mout.close();
		mout.write("test".getBytes());
	}

	/**
	 * A test adding a new {@link OutputStream} to a {@link MultiOutputStream}.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void addNew() throws IOException {
		MultiOutputStream mout = new MultiOutputStream();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		assertTrue("Adding a new OutputStream failed.", mout.addStream(bout));
		final String content = "Some test data.";
		mout.write(content.getBytes());
		mout.close();
		assertEquals("The content of the target stream didn't match.", content, bout.toString());
	}

	/**
	 * A test adding an {@link OutputStream} to a {@link MultiOutputStream} that it
	 * already uses.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void addDuplicate() throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		MultiOutputStream mout = new MultiOutputStream(bout);
		assertFalse("Adding a new OutputStream failed.", mout.addStream(bout));
		final String content = "Some test data.";
		mout.write(content.getBytes());
		mout.close();
		assertEquals("The content of the target stream didn't match.", content, bout.toString());
	}

	/**
	 * A test removing an {@link OutputStream} from a {@link MultiOutputStream}.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void removeExisting() throws IOException {
		ByteArrayOutputStream bout1 = new ByteArrayOutputStream();
		ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
		MultiOutputStream mout = new MultiOutputStream(bout1, bout2);
		assertTrue("Removing an OutputStream failed.", mout.removeStream(bout2));
		final String content = "Some test data.";
		mout.write(content.getBytes());
		mout.close();
		assertEquals("The content of the target stream didn't match.", content, bout1.toString());
		assertEquals("The removed OutputStream wasn't empty.", 0, bout2.size());
	}

	/**
	 * A test removing an {@link OutputStream} that wasn't used from a
	 * {@link MultiOutputStream}.
	 * 
	 * @throws IOException If writing to or closing a stream fails.
	 */
	@Test
	public void removeMissing() throws IOException {
		ByteArrayOutputStream bout1 = new ByteArrayOutputStream();
		MultiOutputStream mout = new MultiOutputStream(bout1);
		ByteArrayOutputStream bout2 = new ByteArrayOutputStream();
		assertFalse("Removing an OutputStream that wasn't added worked.", mout.removeStream(bout2));
		final String content = "Some test data.";
		mout.write(content.getBytes());
		mout.close();
		assertEquals("The content of the target stream didn't match.", content, bout1.toString());
		assertEquals("The removed OutputStream wasn't empty.", 0, bout2.size());
	}

}
