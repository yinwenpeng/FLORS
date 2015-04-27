package nlp.model;
import java.io.IOException;
import java.util.ArrayList;

import nlp.data.Parser;
import nlp.stats.BigramFreqs;
import nlp.stats.UnigramFreqs;


public class UnlabeledDataset extends Dataset {
	protected BigramFreqs bigrams;
	
	public UnlabeledDataset() {
		unigrams = new UnigramFreqs();
		bigrams = new BigramFreqs(unigrams);
	}
	
	public BigramFreqs getBigrams() {
		return bigrams;
	}
		
	public void parse(String[] labeledFiles, String[] unlabeledFiles) throws IOException {
		Parser parser = getParser();
		
		for (String file : replaceIfNull(labeledFiles)) {
			parser.parseLabeledFile(file);
			printMessage("Parsed labeled file: " + file);
			
			updateStatistics(parser);
		}
		
		for (String file : replaceIfNull(unlabeledFiles)) {
			parser.parseUnlabeledFile(file);
			printMessage("Parsed unlabeled file: " + file);
			
			updateStatistics(parser);
		}
		
	}
	
	public void add(SentenceData data) {
		addCorpus(data.getCorpus());
	}
	
	//by wenpeng
	public void add (String[] filePath) throws IOException
	{
		for (String file : replaceIfNull(filePath)) {
			//更新unigram和bigram
			unigrams.addFile(file);
			System.out.println("suceed to update unigram of "+file+"\n");
			bigrams.addFile(file);
			System.out.println("suceed to update bigram of "+file+"\n");
		}	
	}
	
	
	protected void updateStatistics(Parser parser) {
		addCorpus(parser.getCorpus());
		parser.clear();
	}
	
	protected void addCorpus(ArrayList<Sentence> corpus) {
		System.out.println("Adding corpus with " + corpus.size() + " sentences");
		unigrams.addCorpus(corpus);
		bigrams.addCorpus(corpus);
	}
}
