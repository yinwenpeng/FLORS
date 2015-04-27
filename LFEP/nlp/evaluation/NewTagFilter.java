package nlp.evaluation;

import java.util.Set;

import nlp.model.LabeledDataset;
import nlp.stats.TagFreqs;

public class NewTagFilter implements EvalStatistics {
	EvalStatistics wrappedStatistics;
	Set<String> vocabulary;
	TagFreqs tagFreqs;
	int timesInvoked = 0;
	int totalCalls = 0;
	
	public NewTagFilter(EvalStatistics statistics, LabeledDataset trainingSet) {
		wrappedStatistics = statistics;
		vocabulary = trainingSet.getRawVocabulary();
		tagFreqs = new TagFreqs(trainingSet, true);
	}

	@Override
	public void addLine(TaggedFile gold, TaggedFile tagged) {
		if (vocabulary.contains(gold.getToken())) {
			if (!tagFreqs.getTagFrequencies(gold.getToken()).keySet().contains(gold.getTag())) {
				timesInvoked++;
				wrappedStatistics.addLine(gold, tagged);
			}
		}
		totalCalls++;
	}

	@Override
	public void printSummary() {
		System.out.print("Known words with new tags\n");
		System.out.printf("Times called (%d / %d) : %.3f%%\n", timesInvoked, totalCalls, (double)timesInvoked*100/totalCalls);
		wrappedStatistics.printSummary();
	}
	
	@Override
	public void printShortSummary() {
		wrappedStatistics.printShortSummary();
	}
	
	@Override
	public EvalStatistics getInstance() {
		return wrappedStatistics;
	}

	@Override
	public void reset() {
		timesInvoked = 0;
		totalCalls = 0;
		wrappedStatistics.reset();		
	}

	@Override
	public EvalStatistics getSignificanceTest() {
		return null;
	}
	
	
}