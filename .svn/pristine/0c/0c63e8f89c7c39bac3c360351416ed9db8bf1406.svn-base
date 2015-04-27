package nlp.evaluation;

import java.util.Locale;

public class Accuracies implements EvalStatisticsVal {
	int correct = 0;
	int total = 0;
	
	public void addLine(TaggedFile gold, TaggedFile tagged) {
		total++;
		
		if (gold.getTag().equals(tagged.getTag())) {
			correct++;
		}
		
	}
	private double getAccuracy() {
		return correct*100 / (double) total;
	}
	
	public void printSummary() {
		System.out.printf(Locale.ENGLISH, "Total accuracy: %.3f%% (%d / %d)\n", getAccuracy(), correct, total);
	}

	@Override
	public void printShortSummary() {
		System.out.printf(Locale.ENGLISH,"%.3f%%\t", getAccuracy());
	}
	@Override
	public EvalStatistics getInstance() {
		return this;
	}
	@Override
	public double getValue() {
		return getAccuracy();
	}
	@Override
	public void reset() {
		correct = 0;
		total = 0;
	}
	@Override
	public EvalStatistics getSignificanceTest() {
		return new BinomialSignTest();
	}

}
