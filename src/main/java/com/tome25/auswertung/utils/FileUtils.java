package com.tome25.auswertung.utils;

import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.tome25.auswertung.args.Arguments;
import com.tome25.auswertung.log.LogHandler;

/**
 * A class containing utilities related to file handling.
 * 
 * @author Theodor Meyer zu Hörste
 */
public class FileUtils {

	/**
	 * Extracts the given resource into the given directory.<br/>
	 * Can extract either a single file, or a directory including all its contents.
	 * 
	 * If this is run inside a jar it will extract the files from said jar.<br/>
	 * Otherwise it will just copy the files from the classpath.
	 * 
	 * Existing files will not be overridden.<br/>
	 * Any file except the target directory existing will not cause an exception,
	 * but just write a log message and skip said file or directory.<br/>
	 * The target directory existing only causes an exception if it is not a
	 * directory.
	 * 
	 * If the resource is a file it will be extracted into the target
	 * directory.<br/>
	 * If it is a directory all its contents will be extracted into the target
	 * directory.
	 * 
	 * @param resource  The file or directory to extract.
	 * @param directory The target directory to extract to.
	 * @throws FileNotFoundException      If the target directory doesn't exist and
	 *                                    cannot be created.<br/>
	 *                                    Also if this is run outside of a jar, and
	 *                                    the resource to extract doesn't exist.
	 * @throws FileAlreadyExistsException If the target file exists, but isn't a
	 *                                    directory.
	 * @throws IOException                If getting real/canonical paths fails.
	 * @throws NullPointerException       If one of the arguments is {@code null}.
	 */
	public static void extract(String resource, File directory) throws FileNotFoundException,
			FileAlreadyExistsException, IOException, NullPointerException, IllegalArgumentException {
		Objects.requireNonNull(resource, "The resource to extract can't be null.");
		URL res = FileUtils.class.getResource(resource);
		if (res == null) {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			if (cl == null) {
				cl = FileUtils.class.getClassLoader();
			}

			res = cl.getResource(resource);
			if (res == null) {
				throw new IllegalArgumentException("Resource \"" + resource + "\" can't be found.");
			}
		}

		extract(res, directory);
	}

	/**
	 * Extracts the given resource into the given directory.<br/>
	 * Can extract either a single file, or a directory including all its contents.
	 * 
	 * If this is run inside a jar it will extract the files from said jar.<br/>
	 * Otherwise it will just copy the files from the classpath.
	 * 
	 * Existing files will not be overridden.<br/>
	 * Any file except the target directory existing will not cause an exception,
	 * but just write a log message and skip said file or directory.<br/>
	 * The target directory existing only causes an exception if it is not a
	 * directory.
	 * 
	 * If the resource is a file it will be extracted into the target
	 * directory.<br/>
	 * If it is a directory all its contents will be extracted into the target
	 * directory.
	 * 
	 * @param resource  The file or directory to extract.
	 * @param directory The target directory to extract to.
	 * @throws FileNotFoundException      If the target directory doesn't exist and
	 *                                    cannot be created.<br/>
	 *                                    Also if this is run outside of a jar, and
	 *                                    the resource to extract doesn't exist.
	 * @throws FileAlreadyExistsException If the target file exists, but isn't a
	 *                                    directory.
	 * @throws IOException                If getting real/canonical paths fails.
	 * @throws NullPointerException       If one of the arguments is {@code null}.
	 */
	public static void extract(URL resource, File directory)
			throws FileNotFoundException, FileAlreadyExistsException, IOException, NullPointerException {
		Objects.requireNonNull(resource, "The resource to extract can't be null.");
		Objects.requireNonNull(directory, "The directory to extract the resource into can't be null.");

		if (directory.exists() && !directory.isDirectory()) {
			throw new FileAlreadyExistsException("The file \"" + directory + "\" exists, but isn't a directory.");
		} else if (!directory.exists()) {
			if (!directory.mkdirs()) {
				throw new FileNotFoundException("The directory \"" + directory + "\" could not be created.");
			}
		}

		if (resource.getProtocol().equals("jar")) {
			String path = resource.getPath();
			while (path.startsWith("jar:") || path.startsWith("file:")) {
				if (path.startsWith("jar:")) {
					path = path.substring(4);
				} else if (path.startsWith("file:")) {
					path = path.substring(5);
				}
			}

			// Assume path in jar doesn't contain an exclamation mark.
			String resPath = path.substring(path.lastIndexOf('!') + 1);
			if (resPath.charAt(0) == '/') {
				resPath = resPath.substring(1);
			}

			String jarPath = path.substring(0, path.lastIndexOf('!'));
			try (JarFile jar = new JarFile(jarPath)) {
				Enumeration<JarEntry> jarEntries = jar.entries();
				while (jarEntries.hasMoreElements()) {
					JarEntry entry = jarEntries.nextElement();
					if (entry.getName().startsWith(resPath)) {
						File target = new File(directory, entry.getName().substring(resPath.length()));

						if (entry.getName().equals(resPath) && !entry.isDirectory()) {
							target = new File(directory, new File(entry.getName()).getName());
						}

						if (entry.isDirectory()) {
							target.mkdir();
						} else {
							try {
								Files.copy(jar.getInputStream(entry), target.toPath());
							} catch (FileAlreadyExistsException e) {
								LogHandler.err_println(
										"Couldn't extract file \"" + entry.getName() + "\" because it already exists.");
								LogHandler.print_exception(e, "extract a file from jar",
										"Jar path: %s, Resource path: %s, Entry path: %s", jarPath, resPath,
										entry.getName());
							}
						}
					}
				}
			}
		} else if (resource.getProtocol().equals("file")) {
			File resFile = new File(resource.getFile());
			if (!resFile.exists()) {
				throw new FileNotFoundException(
						"Couldn't copy file \"" + resFile.toString() + "\" because it doesn't exist.");
			}

			if (resFile.isDirectory()) {
				copyDirectory(resFile, directory);
			} else {
				try {
					Files.copy(resFile.toPath(), new File(directory, resFile.getName()).toPath());
				} catch (FileAlreadyExistsException e) {
					LogHandler.err_println(
							"Couldn't copy file \"" + resFile.toString() + "\" because it already exists.");
					LogHandler.print_exception(e, "copy a file", "Source path: %s, Target path: %s",
							resFile.getCanonicalPath(), new File(directory, resFile.getName()).getCanonicalPath());
				}
			}
		} else {
			LogHandler.err_println("Resource has unknown protocol \"" + resource.getProtocol() + "\".");
			LogHandler.print_debug_info("Resource url: %s, Protocol: %s, Target: %s", resource.toString(),
					resource.getProtocol(), directory.toString());
		}
	}

	/**
	 * Copies all content from inside the given source directory into the given
	 * target directory.<br/>
	 * Does not override existing files.
	 * 
	 * @param source The directory to copy.
	 * @param target The directory to copy the files and directories into.
	 * @throws FileNotFoundException      If the source directory doesn't exist, or
	 *                                    isn't a directory.<br/>
	 *                                    Also if the target directory doesn't exist
	 *                                    and cannot be created.
	 * @throws FileAlreadyExistsException If the target directory exists, but is not
	 *                                    a directory.
	 * @throws IOException                If getting real/canonical paths fails.
	 * @throws NullPointerException       If {@code source} or {@code target} is
	 *                                    {@code null}.
	 */
	public static void copyDirectory(File source, File target)
			throws FileNotFoundException, FileAlreadyExistsException, IOException, NullPointerException {
		copyDirectory(source, target, false);
	}

	/**
	 * Copies all content from inside the given source directory into the given
	 * target directory.
	 * 
	 * @param source  The directory to copy.
	 * @param target  The directory to copy the files and directories into.
	 * @param replace Whether to replace files if they already exist.
	 * @throws FileNotFoundException      If the source directory doesn't exist, or
	 *                                    isn't a directory.<br/>
	 *                                    Also if the target directory doesn't exist
	 *                                    and cannot be created.
	 * @throws FileAlreadyExistsException If the target directory exists, but is not
	 *                                    a directory.<br/>
	 *                                    If replace is {@code true} only if said
	 *                                    file cannot be deleted.
	 * @throws IOException                If getting real/canonical paths fails.
	 * @throws NullPointerException       If {@code source} or {@code target} is
	 *                                    {@code null}.
	 */
	public static void copyDirectory(File source, File target, boolean replace)
			throws FileNotFoundException, FileAlreadyExistsException, IOException, NullPointerException {
		Objects.requireNonNull(source, "The directory to copy cannot be null.");
		Objects.requireNonNull(target, "The target directory cannot be null.");

		if (!source.exists()) {
			throw new FileNotFoundException("The directory to copy does not exist.");
		} else if (!source.isDirectory()) {
			throw new FileNotFoundException("The file to copy is not a directory.");
		}

		if (replace && target.exists() && !target.isDirectory()) {
			target.delete();
		}

		if (target.exists() && !target.isDirectory()) {
			throw new FileAlreadyExistsException("The target directory exists and is not a directory.");
		} else if (!target.exists() && !target.mkdirs()) {
			throw new FileNotFoundException("Failed to create the target directory.");
		}

		if (source.getCanonicalPath().equals(target.getCanonicalPath())) {
			return;
		}

		Path sourcePath = source.toPath();
		Path targetPath = target.toPath();
		Stack<File> directories = new Stack<File>();
		directories.push(source);
		while (!directories.empty()) {
			File current = directories.pop();
			Path currentTarget = targetPath.resolve(sourcePath.relativize(current.toPath()));

			if (replace && Files.exists(currentTarget) && !Files.isDirectory(currentTarget)) {
				Files.delete(currentTarget);
			}

			if (Files.exists(currentTarget) && !Files.isDirectory(currentTarget)) {
				LogHandler.err_println("Couldn't copy directory \"" + currentTarget.toString()
						+ "\" because its target exists and isn't a directory.");
				LogHandler.print_debug_info("Source: %s, Target: %s", current.getCanonicalPath(),
						currentTarget.toRealPath().toString());
				continue;
			} else if (!Files.exists(currentTarget)) {
				try {
					Files.createDirectory(currentTarget);
				} catch (IOException e) {
					LogHandler.err_println("Something went wrong while creating directory \"" + currentTarget + "\".");
					LogHandler.print_exception(e, "create target directory",
							"Copy source: %s, Copy target: %s, Current source: %s, current target: %s",
							source.getCanonicalPath(), target.getCanonicalPath(), current.getCanonicalPath(),
							currentTarget.toRealPath().toString());
				}
			}

			for (File content : current.listFiles()) {
				if (content.isDirectory()) {
					directories.push(content);
				} else {
					Path contentTgt = targetPath.resolve(sourcePath.relativize(content.toPath()));
					try {
						if (replace) {
							Files.copy(content.toPath(), contentTgt, StandardCopyOption.REPLACE_EXISTING);
						} else {
							Files.copy(content.toPath(), contentTgt);
						}
					} catch (IOException e) {
						if (e instanceof FileAlreadyExistsException) {
							LogHandler.err_println("Couldn't copy file \"" + content.toString()
									+ "\" because its target already exists.");
						} else if (e instanceof DirectoryNotEmptyException) {
							LogHandler.err_println("Couldn't copy file \"" + content.toString()
									+ "\" because its target is a non-empty directory.");
						} else {
							LogHandler.err_println("Failed to copy file \"" + content.toString() + "\".");
						}
						LogHandler.print_exception(e, "copy file to target", "Source file: %s, Target file: %s",
								content.getCanonicalPath(), contentTgt.toRealPath().toString());
					}
				}
			}
		}
	}

	/**
	 * Gets the file to use as an output file.
	 * 
	 * Always returns the default output file if either the override argument is
	 * given, or the program is run non-interactively.<br/>
	 * Unless the file exists but is not a file or cannot be written, in which case
	 * it returns {@code null}.<br/>
	 * Also returns the default file if it does not exist.
	 * 
	 * If none of these are true it asks the user what to do.<br/>
	 * The user then has three choices:
	 * <ul>
	 * <li>Override: The file is to be overridden. This method returns that
	 * file.</li>
	 * <li>Rename: The content is to be written to a different user specified file.
	 * This method returns that specified file.</li>
	 * <li>Cancel: Program execution is to be canceled. This method returns
	 * {@code null}.</li>
	 * </ul>
	 * 
	 * @param defaultFile The default file to be used.
	 * @param args        The commandline arguments given to this program.
	 * @return The file to write to instead of {@code defaultFile}, or {@code null}
	 *         if no valid file could be found.
	 * @throws IOException If something goes wrong with underlying IO methods.
	 */
	public static File getOutputFile(File defaultFile, Arguments args) throws IOException {
		if (defaultFile == null) {
			return defaultFile;
		}

		if (!defaultFile.exists()) {
			return defaultFile;
		}

		boolean allowOverride = true;
		if (!defaultFile.isFile() || !defaultFile.canWrite()) {
			allowOverride = false;
		}

		Console cons = System.console();
		if (cons == null) {
			if (allowOverride) {
				return defaultFile;
			} else {
				LogHandler.err_println(
						"Cannot override file \"" + defaultFile + "\" because it isn't a file or can't be written.");
				LogHandler.print_debug_info("File: %s, Interactive: %s, Arguments: %s", defaultFile,
						cons == null ? "false" : "true", args);
				return null;
			}
		}

		if (args.overrideOutput) {
			if (allowOverride) {
				return defaultFile;
			} else {
				LogHandler.err_println(
						"Cannot override file \"" + defaultFile + "\" because it isn't a file or can't be written.");
				LogHandler.print_debug_info("File: %s, Interactive: %s, Arguments: %s", defaultFile,
						cons == null ? "false" : "true", args);
				return null;
			}
		}

		Writer out = cons.writer();
		out.write("Output file \"" + defaultFile + "\" already exists." + System.lineSeparator());
		out.write("Do you want to ");
		if (allowOverride) {
			out.write("[O]verride, ");
		}
		out.write("[R]ename, or [C]ancel?" + System.lineSeparator());
		out.flush();
		out.close();

		String response = cons.readLine().trim();
		while (response.length() == 0) {
			response = cons.readLine().trim();
		}

		if (response.length() > 1) {
			LogHandler.err_println("Received invalid input \"" + response + "\".");
			return null;
		}

		char c = response.charAt(0);
		File file = null;
		if (c == 'o' || c == 'O') {
			file = defaultFile;
		} else if (c == 'r' || c == 'R') {
			String newName = cons.readLine("Enter new file name: ").trim();
			while (newName.length() == 0) {
				newName = cons.readLine().trim();
			}
			file = getOutputFile(new File(newName), args);
		} else if (c == 'c' || c == 'C') {
			LogHandler.out_println("Execution canceled.");
			return null;
		} else {
			LogHandler.err_println("Received invalid input " + c + '.');
			return null;
		}

		return file;
	}

	/**
	 * Creates the given file and all missing parent directories.
	 * 
	 * @param file The file to create.
	 * @return {@code true} if the file now exists and is a file. {@code false}
	 *         otherwise.
	 * @throws NullPointerException If {@code file} is {@code null}.
	 * @throws IOException          If something with file io goes wrong.
	 */
	public static boolean createFile(File file) throws NullPointerException, IOException {
		Objects.requireNonNull(file, "The file to create cannot be null.");

		if (file.exists() && file.isFile()) {
			return true;
		}

		if (file.exists() && !file.isFile()) {
			LogHandler
					.err_println("The file \"" + file + "\" could not be created because it exists, but isn't a file.");
			LogHandler.print_debug_info("File: %s", file);
			return false;
		}

		file = file.getAbsoluteFile();
		File parent = file.getParentFile();

		if (parent.exists() && !parent.isDirectory()) {
			LogHandler.err_println("The parent directory for the file \"" + file + "\" exists, but isn't a directory.");
			LogHandler.print_debug_info("File: %s, Parent Dir: %s", file, parent);
			return false;
		}

		if (!parent.exists()) {
			if (parent.mkdirs()) {
				LogHandler.out_println("Created output directory \"" + parent.getCanonicalPath() + "\".", true);
			} else {
				LogHandler.err_println("Failed to create the directory to put the file \"" + file + "\" into.");
				LogHandler.print_debug_info("File: %s, Parent Dir: %s", file, parent);
				return false;
			}
		}

		return file.createNewFile();
	}

}
