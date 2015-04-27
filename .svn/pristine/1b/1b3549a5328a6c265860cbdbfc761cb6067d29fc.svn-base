package nlp.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Converter extends Parser {
	public void parseTreebankFile(String fileName, String outName) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"));
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outName, true), "UTF8"));

		Pattern pattern = Pattern.compile("\\(([^()]+)\\)");
		String currentLine;

		while ((currentLine = br.readLine()) != null) {
			// Skip empty lines
			if (!currentLine.trim().isEmpty()) {
				// Extract innermost brackets
				Matcher matcher = pattern.matcher(currentLine);
				while (matcher.find()) {
					// Add all matches
					String match = matcher.group(1);
					String token = match.split(" ")[1];
					String tag = match.split(" ")[0];
					if (!tag.equals("-NONE-")) {
						out.write(token + "\t" + tag + "\n");
					}
				}
			} else {
				out.write("\n");
			}
		}
		out.close();
		br.close();
	}

	private File[] getAllFilesInDir(String dirName) {
		File dir = new File(dirName);
		if (!dir.exists()) {
			throw new IllegalArgumentException("'" + dirName + "' doesn't exist");
		} else if (!dir.isDirectory()) {
			throw new IllegalArgumentException("'" + dirName + "' is not a valid directory");
		}

		return dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".parse");
			}
		});

	}

	public void parseDirectory(String dirName) {
		try {
			File dir = new File(dirName);
			File out = new File(new File(dir.getAbsolutePath()).getParent() + File.separatorChar + dir.getName() + ".converted");
			if (out.exists()) {
				out.delete();
			}

			for (File file : getAllFilesInDir(dirName)) {
				parseTreebankFile(file.getAbsolutePath(), out.getAbsolutePath());
				System.out.println("Parsing " + file.getName());
			}
			System.out.println("\n Written converted file to " + out.getAbsolutePath());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

	public static void main(String[] args) throws IOException {
		
		if (args.length == 0) {
			System.out.println("Format: java -jar treebankConverter.jar <dirname>");
			System.out.println("The input directory contains a list of *.parse files which will be converted and written to a single <dirname>.converted file.");
		} else {
			Converter p = new Converter();
			p.parseDirectory(args[0]);
		}
		
	}
}
