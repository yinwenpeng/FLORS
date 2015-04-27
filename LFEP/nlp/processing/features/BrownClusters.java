package nlp.processing.features;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;

import nlp.model.LabeledDataset;
import nlp.model.Sentence;
import nlp.processing.Features;

public class BrownClusters extends Features {
	LabeledDataset trainingData;
	LinkedHashMap<String, Integer> allPaths = new LinkedHashMap<String, Integer>();
	HashMap<String, String> wordToBrownCluster = new HashMap<String, String>();

	static int[] pathLengths = { 4, 6, 10, 20 };

	public BrownClusters(LabeledDataset trainingData, String pathFile) throws IOException {
		this.trainingData = trainingData;
		readBrownClusters(pathFile);
		createListOfPaths();
	}

	private void readBrownClusters(String fileName) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"));
		String rawLine;
		while ((rawLine = br.readLine()) != null) {
			String[] columns = rawLine.split("\t");
			//注意perczliang的brown clusters实现的输出每行先后是:10111111..., token, occur-times
			wordToBrownCluster.put(columns[1], columns[0]);
		}
		br.close();
	}

	private void createListOfPaths() {
		for (Sentence sentence : trainingData.getCorpus()) {
			for (int i = 0; i < sentence.size(); i++) {
				addPaths(sentence.getRawToken(i));
			}
		}
	}
	//将rawtoken的所有长度类型的path加入到allPath集合里面
	private void addPaths(String rawToken) {
		for (int pathLength : pathLengths) {
			String prefix = getPath(rawToken, pathLength);
			if (!allPaths.containsKey(prefix)) {
				allPaths.put(prefix, allPaths.size());//也是将prefix放置到allpaths的最后
			}
		}
	}
	//getPath就是从完整path里面从左到右依次截取pathlength长度
	private String getPath(String token, int pathLength) {
		String fullPath = wordToBrownCluster.get(token);
		return "l" + pathLength + "_" + fullPath.substring(0, Math.min(fullPath.length(), pathLength));
	}

	@Override
	public String[][] getFeatureNames(String pos) {
		String[][] out = { rename(allPaths.keySet().toArray(new String[allPaths.size()]), "", "{brown|" + pos + "}") };
		return out;
	}

	@Override
	protected Vector[] getFeatureVector(String token, String rawToken, int pos) {
		HashMap<Integer, Integer> result = new HashMap<Integer, Integer>();
		if (wordToBrownCluster.containsKey(rawToken)) {
			for (int pathLength : pathLengths) {
				String path = getPath(rawToken, pathLength);
				if (allPaths.containsKey(path)) {
					result.put(allPaths.get(path), 1);
				}
			}
		} else {
			System.err.println("Warning: Could not find Brown cluster of " + rawToken);
		}
		return toVector(result);
	}

	@Override
	protected Vector[] getFeatureVector(String token) {
		// TODO Auto-generated method stub
		return null;
	}
	//wenpeng 
	public void saveFeatureName() throws IOException
	{
		
	}
	public static void main(String[] args) throws IOException {
		LabeledDataset labeled = new LabeledDataset();
		String[] labeledFiles = {"F:/Diplomarbeit/input/Google Task/source/ontonotes-wsj-train"};//{"gold"};
		labeled.parse(labeledFiles);
		BrownClusters brown = new BrownClusters(labeled, "paths");
		brown.printVector("", "stevefarberextremeleadershipfreeteleseminar", 0);
		System.out.println("Done.");
	}
	//by wenpeng
	public void writeFeatures(LabeledDataset trainingData){}
}
