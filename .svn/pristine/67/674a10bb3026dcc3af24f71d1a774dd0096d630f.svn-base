package nlp.evaluation;

import static nlp.evaluation.TaggedFile.Status.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import nlp.model.LabeledDataset;

public class Evaluator {
	List<EvalStatistics> stats;
	
	public Evaluator(String goldFile, String taggedFile, List<EvalStatistics> stats) {
		this.stats = stats;
		for (EvalStatistics stat : stats) {
			stat.reset();
		}
		try {
			TaggedFile gold = new TaggedFile(goldFile);
			TaggedFile tagged = new TaggedFile(taggedFile);
			
			while (gold.getStatus() != END_OF_FILE && tagged.getStatus() != END_OF_FILE) {
				gold.advance();
				tagged.advance();

				evaluateLine(gold, tagged, stats);
			}
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}
	
	public List<EvalStatistics> getStats() {
		return stats;
	}
	
	public void printShortSummary() {
		for (EvalStatistics measure : stats) {
			measure.printShortSummary();
		}
	}
	
	public void printSummary() {
		for (EvalStatistics measure : stats) {
			measure.printSummary();
		}
	}
	
	
	public static LabeledDataset loadTrainingSet(String fileName) throws IOException  {
		LabeledDataset parsedTrainingSet = new LabeledDataset();
		String[] tmpFiles = { fileName };
		parsedTrainingSet.parse(tmpFiles);
		return parsedTrainingSet;
	}
	
	public static List<EvalStatistics> loadStats(LabeledDataset trainingSet) throws IOException {
		ArrayList<EvalStatistics> stats = new ArrayList<EvalStatistics>();
		stats.add(new Accuracies());
		stats.add(new ClassBased());
		
		if (trainingSet != null) {
			stats.add(new UnknownWordFilter(new Accuracies(), trainingSet));
			stats.add(new UnknownWordFilter(new ClassBased(), trainingSet));
		}
		return stats;
	}

	private void evaluateLine(TaggedFile gold, TaggedFile tagged, List<EvalStatistics> stats)
			throws ParseException {
		boolean bothHaveData = (gold.getStatus() == DATA_AVAILABLE && tagged.getStatus() == DATA_AVAILABLE);
		boolean bothHaveEmptyLines = (gold.getStatus() == END_OF_FILE && tagged.getStatus() == END_OF_FILE)
				|| (gold.getStatus() == EMPTY_LINE && tagged.getStatus() == EMPTY_LINE);
		boolean oneHasEmptyLine = ((gold.getStatus() == EMPTY_LINE && tagged.getStatus() == END_OF_FILE) || (tagged
				.getStatus() == EMPTY_LINE && gold.getStatus() == END_OF_FILE));
		if (bothHaveData) {
			// Evaluate statistics
			for (EvalStatistics measure : stats) {
				measure.addLine(gold, tagged);
			}
		} else if (!bothHaveEmptyLines && !oneHasEmptyLine) {
			throw new ParseException("Lines of both files are not aligned at lines " 
					+ gold.getLineNumber(), gold.getLineNumber());
		}

	}

	public static void main(String[] args) throws IOException {
		evalWeb();
	}
		
	public static void evalWeb() throws IOException {
		Evaluator test;
		LabeledDataset trainingSet = loadTrainingSet("F:/Diplomarbeit/input/Google Task/source/ontonotes-wsj-train");
		
		String[] domains = {"newsgroups", "reviews", "weblogs", "answers", "emails", "wsj"};

		for (String domain : domains) {
			String goldFile = "F:/Diplomarbeit/input/Google Task/target/" + domain + "/gweb-" + domain + "-dev";
			String predictedFile = "F:/DeepLearning/baselines/choi & palmer/results/gweb-" + domain + "-dev.tagged";
			test = new Evaluator(goldFile, predictedFile, loadStats(trainingSet));
			test.printShortSummary();
		}
	}
	
	public static void evalBio() throws IOException {
		Evaluator test;
		LabeledDataset trainingSet = loadTrainingSet("F:/Diplomarbeit/input/train-wsj-02-21");
		
		String[] domains = {"bio", "bio-nnptonn"};
		for (String domain : domains) {
			String goldFile = "F:/Diplomarbeit/input/onco_train.500";
			String predictedFile = "F:/DeepLearning/results/w=2;dist=cw;suffixes=all;shapes/" + domain + ".tagged";
			test = new Evaluator(goldFile, predictedFile, loadStats(trainingSet));
			test.printShortSummary();
		}
	}
}
