package nlp.evaluation;

import java.util.Set;

import nlp.model.LabeledDataset;

public class UnknownWordFilter implements EvalStatisticsVal {
	EvalStatistics wrappedStatistics;
	Set<String> vocabulary;

	public UnknownWordFilter(EvalStatistics statistics, LabeledDataset trainingSet) {
		wrappedStatistics = statistics;
		vocabulary = trainingSet.getRawVocabulary();
	}
	
	public UnknownWordFilter(EvalStatistics statistics, Set<String> vocabulary) {
		wrappedStatistics = statistics;
		this.vocabulary = vocabulary;
	}

	@Override
	public void addLine(TaggedFile gold, TaggedFile tagged) {
		if (!vocabulary.contains(gold.getToken())) {
			wrappedStatistics.addLine(gold, tagged);
		}
	}

	@Override
	public void printSummary() {
		System.out.print("Unknown words\n");
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
	public double getValue() {
		if (wrappedStatistics instanceof EvalStatisticsVal) {
			return ((EvalStatisticsVal) wrappedStatistics).getValue();
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void reset() {
		wrappedStatistics.reset();
	}
	
	@Override
	public EvalStatistics getSignificanceTest() {
		return new UnknownWordFilter(new BinomialSignTest(), vocabulary);
	}
}

