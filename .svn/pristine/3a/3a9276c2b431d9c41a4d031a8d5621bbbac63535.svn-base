package nlp.evaluation;

import java.util.ArrayList;

import cern.colt.Arrays;
import cern.jet.stat.Probability;

public class BinomialSignTest implements EvalStatistics {
	ArrayList<Boolean> signs = new ArrayList<Boolean>();

	@Override
	public void addLine(TaggedFile gold, TaggedFile tagged) {
		if (gold.getTag().equals(tagged.getTag())) {
			signs.add(true);
		} else {
			signs.add(false);
		}
	}

	public static double pValueRightTailed(BinomialSignTest firstModel, BinomialSignTest secondModel) {
		return pValues(firstModel.signs, secondModel.signs)[0];
	}

	public static double pValueLeftTailed(BinomialSignTest firstModel, BinomialSignTest secondModel) {
		return pValues(firstModel.signs, secondModel.signs)[1];
	}

	public double pValueTwoSided(BinomialSignTest secondModel) {
		double[] pValues = pValues(secondModel.signs, signs);
		return 2 * Math.min(pValues[0], pValues[1]);
	}

	public static double[] pValues(ArrayList<Boolean> x, ArrayList<Boolean> y) {
		if (x.size() != y.size()) {
			throw new IllegalArgumentException("Input lists must have the same length");
		}
		int firstModelWins = 0;
		int secondModelWins = 0;
		for (int i = 0; i < x.size(); i++) {
			if (x.get(i) && !y.get(i)) {
				firstModelWins++;
			} else if (!x.get(i) && y.get(i)) {
				secondModelWins++;
			}
		}
		//System.out.println("First model wins: " + firstModelWins);
		//System.out.println("Second model wins: " + secondModelWins);
		double[] pValues = { Probability.binomial(firstModelWins, firstModelWins + secondModelWins, 0.5),
				Probability.binomialComplemented(firstModelWins - 1, firstModelWins + secondModelWins, 0.5) };

		return pValues;
	}

	public static void main(String[] args) {
		ArrayList<Boolean> x = new ArrayList<Boolean>();
		ArrayList<Boolean> y = new ArrayList<Boolean>();
		for (int i = 1; i <= 36; i++) {
			x.add(i <= 13);
			y.add(i > 13);
		}
		System.out.println("p-Values (right, left): " + Arrays.toString(pValues(x, y)));
	}

	@Override
	public void printSummary() {
		System.out.println("Collected " +  " samples for binomial sign test.");
	}

	@Override
	public void printShortSummary() {
		
	}
	
	@Override
	public EvalStatistics getInstance() {
		return this;
	}

	@Override
	public void reset() {
		signs = new ArrayList<Boolean>();
	}
	@Override
	public EvalStatistics getSignificanceTest() {
		return null;
	}
}
