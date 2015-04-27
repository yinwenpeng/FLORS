package nlp.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class TaggedFile {
	BufferedReader br;
	int tokenIndex;
	int tagIndex;
	int lineNumber;
	String[] currentLine;
	String fileName;
	
	public enum Status {
		DATA_AVAILABLE, EMPTY_LINE, END_OF_FILE
	};

	Status status = Status.DATA_AVAILABLE;

	private void init(String fileName, int tokenIndex, int tagIndex) throws FileNotFoundException,
			UnsupportedEncodingException {
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"));
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("Couldn't open file " + fileName);
		}
		this.fileName = new File(fileName).getName();
		this.tokenIndex = tokenIndex;
		this.tagIndex = tagIndex;
	}

	public TaggedFile(String goldFile) throws FileNotFoundException, UnsupportedEncodingException {
		String[] flags = goldFile.split("\\?");
		if (flags.length == 1) {
			init(flags[0],0,1);
		} else if (flags.length == 3) {
			init(flags[0], Integer.parseInt(flags[1]), Integer.parseInt(flags[2]));
		} else {
			throw new IllegalArgumentException("Error parsing '" + goldFile + "'. Expected format is <filename>?<token index>?<tag index> or <filename>");
		}
		
	}
	
	public int getLineNumber() {
		return lineNumber;
	}
	
	public String getTag() {
		return currentLine[tagIndex].trim();
	}
	
	public String getToken() {
		return currentLine[tokenIndex];
	}
	
	public void advance() throws IOException {
		String rawLine = br.readLine();
		lineNumber++;
		currentLine = null;

		if (rawLine == null) {
			status = Status.END_OF_FILE;
		} else if (rawLine.isEmpty()) {
			status = Status.EMPTY_LINE;
		} else {
			status = Status.DATA_AVAILABLE;
			currentLine = rawLine.split("\t");
			if (currentLine.length - 1 < Math.max(tokenIndex,  tagIndex)) {
				throw new IOException("Line " + lineNumber + " has too few columns in " + fileName + ": " + rawLine);
			}
		}
	}

	public Status getStatus() {
		return status;
	}

	public void close() throws IOException {
		br.close();
	}

}
