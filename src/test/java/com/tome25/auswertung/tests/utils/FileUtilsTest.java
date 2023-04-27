package com.tome25.auswertung.tests.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.tome25.auswertung.utils.FileUtils;

/**
 * A class containing unit tests pertaining to the {@link FileUtils} class.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class FileUtilsTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	/**
	 * A simple test copying an empty directory to a not existing target directory.
	 * 
	 * @throws IOException If creating/deleting a directory failed.
	 */
	@Test
	public void copyEmptyDir() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		target.delete();
		assertFalse("The target folder existed before copying.", target.exists());

		FileUtils.copyDirectory(source, target);
		assertTrue("The target folder didn't exist after copying an empty folder.", target.exists());
		assertTrue("The target wasn't a directory.", target.isDirectory());
	}

	/**
	 * Tests copying a directory that contains a file to a not existing target
	 * directory.
	 * 
	 * @throws IOException If an I/O operation fails.
	 */
	@Test
	public void copyFileInDir() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		target.delete();
		assertFalse("The target folder existed before copying.", target.exists());
		File srcFile = new File(source, "test.txt");
		FileOutputStream fiout = new FileOutputStream(srcFile);
		fiout.write("test".getBytes());
		fiout.close();

		FileUtils.copyDirectory(source, target);
		assertTrue("The target directory didn't exist after copying.", target.exists());
		assertTrue("The target directory isn't a directory.", target.isDirectory());
		File tgtFile = new File(target, "test.txt");
		assertTrue("The test.txt file in the target directory didn't exist.", tgtFile.exists());
		assertTrue("The file in the target directory isn't a file.", tgtFile.isFile());
		assertArrayEquals("The content of the target file doesn't match the source file.",
				Files.readAllBytes(srcFile.toPath()), Files.readAllBytes(tgtFile.toPath()));
	}

	/**
	 * Test copying an empty directory to an already existing directory.
	 * 
	 * @throws IOException If an I/O error occurs.
	 */
	@Test
	public void copyEmptyDirToExistingDir() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		assertTrue("The target folder didn't exist before copying.", target.exists());

		FileUtils.copyDirectory(source, target);
		assertTrue("The target folder didn't exist after copying an empty folder.", target.exists());
		assertTrue("The target wasn't a directory.", target.isDirectory());
	}

	/**
	 * Tests copying a directory containing a file to an existing empty directory.
	 * 
	 * @throws IOException If an I/O error occurs.
	 */
	@Test
	public void copyFileInDirToExistingDir() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		assertTrue("The target folder didn't exist before copying.", target.exists());
		File srcFile = new File(source, "test.txt");
		FileOutputStream fiout = new FileOutputStream(srcFile);
		fiout.write("Some Test String".getBytes());
		fiout.close();

		FileUtils.copyDirectory(source, target);
		assertTrue("The target directory didn't exist after copying.", target.exists());
		assertTrue("The target directory isn't a directory.", target.isDirectory());
		File tgtFile = new File(target, "test.txt");
		assertTrue("The test.txt file in the target directory didn't exist.", tgtFile.exists());
		assertTrue("The file in the target directory isn't a file.", tgtFile.isFile());
		assertArrayEquals("The content of the target file doesn't match the source file.",
				Files.readAllBytes(srcFile.toPath()), Files.readAllBytes(tgtFile.toPath()));
	}

	/**
	 * Test copying a file in a directory in the source directory.
	 * 
	 * @throws IOException If an I/O operation fails.
	 */
	@Test
	public void copyFileInDirInDir() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		target.delete();
		assertFalse("The target folder existed before copying.", target.exists());
		File srcFile = new File(source, "test.txt");
		FileOutputStream fiout = new FileOutputStream(srcFile);
		fiout.write("Some Test String".getBytes());
		fiout.close();
		File innerDir = new File(source, "dir");
		innerDir.mkdir();
		File innerFile = new File(innerDir, "inner.txt");
		innerFile.createNewFile();
		assertTrue("The inner file didn't exist before copying.", innerFile.exists());

		FileUtils.copyDirectory(source, target);
		File tgtFile = new File(target, "test.txt");
		File tgtInDir = new File(target, "dir");
		File tgtInF = new File(tgtInDir, "inner.txt");
		assertTrue("The target directory didn't exist after copying.", target.exists());
		assertTrue("The test.txt file in the target directory didn't exist.", tgtFile.exists());
		assertTrue("The inner directory didn't get copied.", tgtInDir.exists());
		assertTrue("The inner dir wasn't a dir after copying.", tgtInDir.isDirectory());
		assertTrue("The inner file didn't exist after copying.", tgtInF.exists());
		assertTrue("The inner file in the target directory isn't a file.", tgtInF.isFile());
		assertArrayEquals("The content of the target file doesn't match the source file.",
				Files.readAllBytes(innerFile.toPath()), Files.readAllBytes(tgtInF.toPath()));
	}

	/**
	 * Copy a directory containing a file that already exists in the target
	 * directory.
	 * 
	 * @throws IOException If an I/O operation fails.
	 */
	@Test
	public void copyFileToExisting() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		File srcFile = new File(source, "test.txt");
		FileOutputStream fiout = new FileOutputStream(srcFile);
		fiout.write("Some Test String".getBytes());
		fiout.close();
		File tgtFile = new File(target, "test.txt");
		fiout = new FileOutputStream(tgtFile);
		fiout.write("Target file".getBytes());
		fiout.close();

		FileUtils.copyDirectory(source, target);
		assertTrue("The target directory didn't exist after copying.", target.exists());
		assertTrue("The target test file didn't exist.", tgtFile.exists());
		assertTrue("The target file wasn't a file.", tgtFile.isFile());
		assertArrayEquals("The content of the target file was changed.", "Target file".getBytes(),
				Files.readAllBytes(tgtFile.toPath()));
	}

	/**
	 * Test copying a directory to itself.
	 * 
	 * @throws IOException If creating the dir fails.
	 */
	@Test
	public void copyToSelf() throws IOException {
		File source = tempFolder.newFolder("tmp");

		FileUtils.copyDirectory(source, source);
	}

	/**
	 * A test overriding an existing target file with a directory.
	 * 
	 * @throws IOException If an I/O operation fails.
	 */
	@Test
	public void overrideFileWithDir() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFile("tgt");
		File srcFile = new File(source, "test.txt");
		FileOutputStream fiout = new FileOutputStream(srcFile);
		fiout.write("test".getBytes());
		fiout.close();

		FileUtils.copyDirectory(source, target, true);
		File tgtFile = new File(target, "test.txt");
		assertTrue("The target directory didn't exist.", target.exists());
		assertTrue("The target directory wasn't a directory.", target.isDirectory());
		assertTrue("The file in the source directory wasn't copied.", tgtFile.exists());
		assertTrue("The file in the target dir wasn't a file.", tgtFile.isFile());
	}

	/**
	 * Override a file in the target directory with a different file.
	 * 
	 * @throws IOException If an I/O operation fails.
	 */
	@Test
	public void overrideExistingFile() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		File srcFile = new File(source, "test.txt");
		FileOutputStream fiout = new FileOutputStream(srcFile);
		fiout.write("Some Test String".getBytes());
		fiout.close();
		File tgtFile = new File(target, "test.txt");
		fiout = new FileOutputStream(tgtFile);
		fiout.write("Target file".getBytes());
		fiout.close();

		FileUtils.copyDirectory(source, target, true);
		assertTrue("The target directory didn't exist after copying.", target.exists());
		assertTrue("The target test file didn't exist.", tgtFile.exists());
		assertTrue("The target file wasn't a file.", tgtFile.isFile());
		assertArrayEquals("The content of the target file wasn't changed.", Files.readAllBytes(srcFile.toPath()),
				Files.readAllBytes(tgtFile.toPath()));
	}

	/**
	 * Test overriding a file in the target dir with a directory containing a file.
	 * 
	 * @throws IOException If creating/writing a file or directory failed.
	 */
	@Test
	public void overrideFileInDirWithDir() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		File srcDir = new File(source, "test");
		srcDir.mkdir();
		File srcFile = new File(srcDir, "test.txt");
		FileOutputStream fiout = new FileOutputStream(srcFile);
		fiout.write("Some Test String".getBytes());
		fiout.close();
		File tgtFile = new File(target, "test");
		fiout = new FileOutputStream(tgtFile);
		fiout.write("Target file".getBytes());
		fiout.close();

		FileUtils.copyDirectory(source, target, true);
		assertTrue("The target directory didn't exist after copying.", target.exists());
		assertTrue("The target test file didn't exist.", tgtFile.exists());
		assertTrue("The target file wasn't a directory.", tgtFile.isDirectory());
		assertTrue("The file in the source directory wasn't copied.", new File(tgtFile, "test.txt").exists());
	}

	/**
	 * Tests trying to override a non empty directory with a file.<br/>
	 * This should not work.
	 * 
	 * @throws IOException If an I/O error occurs.
	 */
	@Test
	public void copyFileToNonEmptyDir() throws IOException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		File srcFile = new File(source, "test");
		FileOutputStream fiout = new FileOutputStream(srcFile);
		fiout.write("Some Test String".getBytes());
		fiout.close();
		File tgtDir = new File(target, "test");
		tgtDir.mkdir();
		File tgtFile = new File(tgtDir, "test.txt");
		fiout = new FileOutputStream(tgtFile);
		fiout.write("Target file".getBytes());
		fiout.close();

		FileUtils.copyDirectory(source, target, true);
		assertTrue("The target directory didn't exist after copying.", target.exists());
		assertTrue("The target test directory didn't exist.", tgtFile.exists());
		assertTrue("The target file wasn't a directory after copying.", tgtDir.isDirectory());
		assertTrue("The file in the target dir was deleted.", tgtFile.exists());
	}

	/**
	 * Copy a missing directory to a not existing target.
	 * 
	 * @throws IOException           If creating or deleting a temp folder fails.
	 * @throws FileNotFoundException Always.
	 */
	@Test(expected = FileNotFoundException.class)
	public void copyMissingDir() throws IOException, FileNotFoundException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFolder("tgt");
		source.delete();
		target.delete();
		assertFalse("The source directory existed, while it shouldn't.", source.exists());
		assertFalse("The target directory existed, when it should have been deleted.", target.exists());

		FileUtils.copyDirectory(source, target);
	}

	/**
	 * Test copying a {@code null} file to a target directory.
	 * 
	 * @throws IOException          Probably never.
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void copyNullDir() throws IOException, NullPointerException {
		File target = tempFolder.newFolder("tgt");

		FileUtils.copyDirectory(null, target);
	}

	/**
	 * Test copying a directory to a {@code null} target.
	 * 
	 * @throws IOException          Probably never.
	 * @throws NullPointerException Always.
	 */
	@Test(expected = NullPointerException.class)
	public void copyToNullDir() throws IOException, NullPointerException {
		File source = tempFolder.newFolder("src");

		FileUtils.copyDirectory(source, null);
	}

	/**
	 * Test copying a file to a directory.
	 * 
	 * @throws IOException           If creating the temp file or dir fails.
	 * @throws FileNotFoundException Always.
	 */
	@Test(expected = FileNotFoundException.class)
	public void copyFile() throws IOException, FileNotFoundException {
		File source = tempFolder.newFile("src.txt");
		File target = tempFolder.newFolder("tgt");

		FileUtils.copyDirectory(source, target);
	}

	/**
	 * Test copying an empty directory to an already existing file.
	 * 
	 * @throws IOException                If creating a temporary file or directory
	 *                                    failed.
	 * @throws FileAlreadyExistsException Always.
	 */
	@Test(expected = FileAlreadyExistsException.class)
	public void copyToFile() throws IOException, FileAlreadyExistsException {
		File source = tempFolder.newFolder("src");
		File target = tempFolder.newFile("tgt.txt");

		FileUtils.copyDirectory(source, target);
	}

	/**
	 * Test creating a file that already exists.
	 * 
	 * @throws IOException If the file creation fails.
	 */
	@Test
	public void createExistingFile() throws IOException {
		File target = tempFolder.newFile("existing.txt");
		assertTrue("Creating an already existing file returned false.", FileUtils.createFile(target));
	}

	/**
	 * Test creating a file that doesn't exist.
	 * 
	 * @throws IOException If creating the file failed.
	 */
	@Test
	public void createNotExistingFile() throws IOException {
		File target = tempFolder.newFile("missing.txt");
		target.delete();
		assertTrue("Creating a not existing file returned false.", FileUtils.createFile(target));
	}

	/**
	 * Test creating a file in a directory that doesn't exist.
	 * 
	 * @throws IOException If something fails.
	 */
	@Test
	public void createFileAndParentDir() throws IOException {
		File target = tempFolder.newFolder("missing");
		target.delete();
		target = new File(target, "test.txt");
		assertTrue("Creating a file and a dir returned false.", FileUtils.createFile(target));
	}

	/**
	 * Test creating a file in a directory that exists, but is a file.
	 * 
	 * @throws IOException If something fails, idk.
	 */
	@Test
	public void createFileInFile() throws IOException {
		File target = tempFolder.newFile("missing");
		target = new File(target, "test.txt");
		assertFalse("Creating a file in dir that was a file returned true.", FileUtils.createFile(target));
	}

	/**
	 * Test creating a file in a directory in a missing directory.
	 * 
	 * @throws IOException If file or directory creation fails.
	 */
	@Test
	public void createParentDirInMissing() throws IOException {
		File target = tempFolder.newFolder("missing");
		target.delete();
		target = new File(target, "parent");
		target = new File(target, "file.txt");
		assertTrue("Creating a file and two directories returned false.", FileUtils.createFile(target));
	}

	/**
	 * Test creating a file in a directory in a file.
	 * 
	 * @throws IOException If something fails.
	 */
	@Test
	public void createParentDirInFile() throws IOException {
		File target = tempFolder.newFile("missing");
		target = new File(target, "parent");
		target = new File(target, "file.txt");
		assertFalse("Creating a file in a dir in a file returned true.", FileUtils.createFile(target));
	}

}
