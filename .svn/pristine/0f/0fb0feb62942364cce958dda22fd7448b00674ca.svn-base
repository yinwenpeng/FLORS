package nlp.model;

import java.io.IOException;
import java.util.ArrayList;

import nlp.data.Parser;

public class ThinDataset extends Dataset implements SentenceData {
	ArrayList<Sentence> corpus = new ArrayList<Sentence>();
	
	public void parse(String[] labeledFiles, String[] unlabeledFiles) throws IOException {
		Parser parser = getParser();
		
		for (String file : replaceIfNull(labeledFiles)) {
			parser.parseLabeledFile(file);
			printMessage("Parsed labeled file: " + file);
		}
		
		for (String file : replaceIfNull(unlabeledFiles)) {
			parser.parseUnlabeledFile(file);
			printMessage("Parsed unlabeled file: " + file); //file is the file path
		}
		corpus = parser.getCorpus();//finally, add all the sentences into corpus
	}
	
	public void add(SentenceData data) {
		corpus.addAll(data.getCorpus());
	}
	
	@Override
	public ArrayList<Sentence> getCorpus() {
		return corpus;
	}

}
