package nlp.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ClassBased implements EvalStatistics {
	Map<String, Integer> occursAs = new TreeMap<String, Integer>();
	Map<String, Integer> taggedAs = new HashMap<String, Integer>();
	Map<String, Integer> taggedCorrectlyAs = new HashMap<String, Integer>();

	public void addLine(TaggedFile gold, TaggedFile tagged) {
		String goldTag = gold.getTag();
		String predictedTag = tagged.getTag();

		increment(occursAs, goldTag);
		increment(taggedAs, predictedTag);
		if (goldTag.equals(predictedTag)) {
			increment(taggedCorrectlyAs, goldTag);
		}
	}

	private void increment(Map<String, Integer> map, String tag) {
		map.put(tag, getValue(map, tag) + 1);
	}

	private int getValue(Map<String, Integer> map, String tag) {
		int oldVal = 0;
		if (map.containsKey(tag)) {
			oldVal = map.get(tag);
		}
		return oldVal;
	}

	public String getRecall(String tag) {
		double recall = (double) getValue(taggedCorrectlyAs, tag) / getValue(occursAs, tag);
		return String.format(Locale.ENGLISH, "%.2f", recall);
	}

	public String getPrecision(String tag) {
		int taggedAsFreq = getValue(taggedAs, tag);
		if (taggedAsFreq == 0) {
			return "n/a";
		} else {
			return String.format(Locale.ENGLISH, "%.2f", (double) getValue(taggedCorrectlyAs, tag) / taggedAsFreq);
		}
	}
	
	public void printShortSummary() {
	}
	
	@Override
	public EvalStatistics getInstance() {
		return this;
	}
	
	public void printSummary() {
		System.out.print("Tag\t#errors\t#occ.\t#tagged\trecall\tprecision\n");

		Set<String> complete = new HashSet<String>();
		complete.addAll(taggedAs.keySet());
		complete.addAll(occursAs.keySet());
		
		ArrayList<String> sortedTags = new ArrayList<String>(complete);
		Collections.sort(sortedTags);
		
		for (String tag : sortedTags) {
			int occursAsFreq = getValue(occursAs, tag);
			int taggedCorrectlyAsFreq = getValue(taggedCorrectlyAs, tag);
			int taggedAsFreq = getValue(taggedAs, tag);
			System.out.printf("%s\t%d\t%d\t%d\t%s\t%s\n", tag, occursAsFreq - taggedCorrectlyAsFreq, occursAsFreq, taggedAsFreq,
					getRecall(tag), getPrecision(tag));
		}
	}

	@Override
	public void reset() {
		occursAs = new TreeMap<String, Integer>();
		taggedAs = new HashMap<String, Integer>();
		taggedCorrectlyAs = new HashMap<String, Integer>();
		
	}
	
	@Override
	public EvalStatistics getSignificanceTest() {
		return new BinomialSignTest();
	}

}
