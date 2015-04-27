package nlp.model;

import java.util.Set;

import nlp.data.Parser;
import nlp.stats.UnigramFreqs;

public class Dataset {
	protected UnigramFreqs unigrams = null;
	protected boolean replaceDigits = true;// previously is "false"
	protected boolean ignoreCapitalization = true;
	private boolean verbose = true;
	
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	protected Parser getParser() {
		Parser parser = new Parser();
		parser.replaceDigits(replaceDigits);
		parser.ignoreCapitalization(ignoreCapitalization);
		return parser;
	}
	
	public UnigramFreqs getUnigrams() {
		return unigrams;
	}

	public int getVocabularySize() {
		return getVocabulary().size();
	}

	public Set<String> getVocabulary() {
		return this.getUnigrams().getVocabulary();
	}
	
	protected void printMessage(String message) {
		if (verbose) {
			System.out.println(message);
		}
	}

	protected String[] replaceIfNull(String[] input) {
		if (input == null) {
			return new String[0];
		} else {
			return input;
		}
	}
	
}