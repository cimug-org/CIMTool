package au.com.langdale.cimtoole.pandoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class PandocConverter {

	/**
	 * Converts a file from one format to another using Pandoc.
	 *
	 * @param inputFile  the file to be converted
	 * @param outputFile the file where converted content will be saved
	 * @param fromFormat the input format (e.g., "markdown")
	 * @param toFormat   the output format (e.g., "asciidoc")
	 * @throws IOException if there is an issue running the Pandoc command
	 */
	public static void convertFile(File inputFile, File outputFile, String fromFormat, String toFormat)
			throws IOException {

		File pandocExe = PandocPathResolver.getPandocExecutablePath();

		// Build the Pandoc command
		ProcessBuilder processBuilder = new ProcessBuilder( //
				pandocExe.getAbsolutePath(), //
				"-f", fromFormat, // Input format
				"-t", toFormat, // Output format
				inputFile.getAbsolutePath(), // Input file
				"-o", outputFile.getAbsolutePath() // Output file
		);
		
		StringBuffer command = new StringBuffer();
		processBuilder.command().forEach(new Consumer<String>() {
			@Override
			public void accept(String t) {
				command.append(t).append(" ");
			}
		});
		
		System.out.println(String.format("Executing Pandoc command line:  %s", command.toString()));

		// Start the Pandoc process...
		Process process = processBuilder.start();

		// Capture the output and errors (if any) from the Pandoc process
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

			// Print any standard output from Pandoc
			reader.lines().forEach(System.out::println);

			// Since we want to reference any error output later  
			// we are processing it as a list first...
			List<String> errors = new LinkedList();
			errorReader.lines().forEach(new Consumer<String>() {
				@Override
				public void accept(String error) {
					errors.add(error);
				}
			});
			
			// Print any errors that occurred to standard err
			errors.forEach(new Consumer<String>() {
					@Override
					public void accept(String error) {
						System.err.println(error);
					}
				});
			
			// Wait for the process to complete
			int exitCode = process.waitFor();

			// Pandoc non-zero exit codes indicate failures of some sort..
			if (exitCode != 0) {
				StringBuffer errorMsg = new StringBuffer();
				errorReader.lines().forEach(new Consumer<String>() {
					@Override
					public void accept(String t) {
						errorMsg.append(t).append(" ");
					}
				});
				switch (exitCode) {
					case 1: // 1 — General Errors
						throw new IOException(String.format("A general error has occurred while running Pandoc. This may be due to command syntax issues, missing files, or other unspecified problems. [Error Message: %s]", errorMsg));
					case 2: // 2 — Parse Error
						throw new IOException(String.format("The input file or command contains syntax that Pandoc could not parse. [Error Message: %s]", errorMsg));
					case 3: // 3 — Option Parsing Error
						throw new IOException(String.format("An error occurred while parsing options or arguments passed to Pandoc. This is often due to an unrecognized command-line option or argument. [Error Message: %s]", errorMsg));
					case 4: // 4 — File Handling Error
						throw new IOException(String.format("An error occurred in reading or writing of the files specified in the command line. This can happen if the specified file does not exist, there are permission issues, or a file path is invalid. [Error Message: %s]", errorMsg));
					case 5: // 5 — Unsupported Feature
						throw new IOException(String.format("The input format or feature requested is not supported by Pandoc. This might occur if a feature has been requested that isn’t implemented for the input/output format specified. [Error Message: %s]", errorMsg));
					case 6: // 6 — Pandoc Resource Error
						throw new IOException(String.format("An error occurred due to a resource limitation, such as running out of memory during the conversion process. [Error Message: %s]", errorMsg));
					case 21: // 21 — Unrecognized Input or Output Format
						throw new IOException(String.format("The format specified for input or output (-f or -t) was not recognized by Pandoc. [Error Message: %s]", errorMsg));
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Pandoc process was interrupted", e);
		}
	}

}
