package nlp.model;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import nlp.data.Parser;
import nlp.stats.TagFreqs;
import nlp.stats.UnigramFreqs;


public class LabeledDataset extends Dataset implements SentenceData {
	protected Parser parser = null;
	protected TagFreqs tags = null;
	
	public void parse(String[] labeledFiles) throws IOException {
		parser = getParser();
				
		for (String file : replaceIfNull(labeledFiles)) {
			parser.parseLabeledFile(file); //here, file is the filename
			printMessage("Parsed labeled file: " + file);
		}	
	}
	
	public ArrayList<Sentence> getCorpus() {
		if (parser == null) {
			return new ArrayList<Sentence>();
		} else {
			return parser.getCorpus();
		}
	}
	
	public Set<String> getRawVocabulary() {
		HashSet<String> vocabulary = new HashSet<String>();
		for (Sentence sentence : getCorpus()) {
			//addAll is the copy of parameter's elements into vocabulary list
			vocabulary.addAll(sentence.getRawTokens());
		}
		return vocabulary;
	}
	
	@Override
	public UnigramFreqs getUnigrams() {
		if (unigrams == null) {
			unigrams = new UnigramFreqs();
			unigrams.addCorpus(parser.getCorpus());
		}
		return unigrams;
	}
	//to get the set of unknownwords, which do not exist in the trainingData
	public Set<String> getUnknownWords(LabeledDataset trainingData) {
		//first get all the words in current dataset, using getRawVocabulary()
		Set<String> unknownWords = new HashSet<String>(getRawVocabulary());
		//remove the words in the training data,注意这里使用的都是raw words
		unknownWords.removeAll(trainingData.getRawVocabulary());
		unknownWords.remove("-LRB-");
		unknownWords.remove("-RRB-");
		return unknownWords;
	}

	public int getNoOfTokens() {
		int totalCount = 0;
		for (Sentence sentence : getCorpus()) {
			totalCount += sentence.size();
		}
		
		return totalCount;
	}

	public String getMajorityTag(String curWord) {
		if (tags == null) {
			tags = new TagFreqs(this);
		}
		return tags.getMostFrequentTag(curWord);
		
	}
}
