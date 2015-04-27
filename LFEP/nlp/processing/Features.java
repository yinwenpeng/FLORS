package nlp.processing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import nlp.model.LabeledDataset;
import nlp.processing.features.Vector;

public abstract class Features {
	public static String saveFile="standardFeatureName.txt";//used to save feature names
	//public static String saveFile="testFeatureName.txt";//used to save feature names
	public abstract String[][] getFeatureNames(String pos);
	
	//by wenpeng
	public abstract void writeFeatures(LabeledDataset trainingData)throws IOException;

	protected abstract Vector[] getFeatureVector(String token);
	//by wenpeng
	public abstract void saveFeatureName()throws IOException;

	protected Vector[] getFeatureVector(String token, String rawToken, int pos) {
		return getFeatureVector(token);
	}

	protected boolean alwaysActive = true;
	
	public Vector[] toVector(HashMap<Integer, Integer> list) {
		Vector[] result = new Vector[1];//means multiple vectors(here is a container with only one)
		result[0] = new Vector(list.size()); //means the size of the first vector
		int index = 0;

		for (Map.Entry<Integer, Integer> entry : list.entrySet()) {
			result[0].indices[index] = entry.getKey();
			result[0].values[index] = entry.getValue();
			index++;
		}
		return result;
	}

	public Vector binAsGreaterEqualThan(int[] intervals, int value) {
		int i = 0;
		while (i < intervals.length && value >= intervals[i]) {
			i++;
		}

		Vector result = new Vector(i);
		for (int j = 0; j < i; j++) {
			result.indices[j] = j;
			result.values[j] = 1;
		}
		return result;
	}

	public int getNoOfGeneratedVectors() {
		return 1;
	}
	//add the entry in names with suffix and prefix, then return the generated names
	public static String[] rename(String[] names, String suffix, String prefix) {
		for (int i = 0; i < names.length; i++) {
			StringBuilder build = new StringBuilder(names[i].length() + suffix.length() + prefix.length());
			build.append(prefix);
			build.append(names[i]);
			build.append(suffix);
			names[i] = build.toString();
		}
		return names;
	}

	public static void printFeatures(String[] names, Vector fVector) {
		for (int i = 0; i < fVector.indices.length; i++) {
			System.out.println(names[fVector.indices[i]] + "\t" + fVector.values[i]);
		}
	}
	//基本上返回的就是feature.length*windowsize
	public static int getNoOfGeneratedVectors(Features[] features, int windowSize) {//????????????????????????????
		int sum = 0;
		// Middle word
		for (int i = 0; i < features.length; i++) {
			sum += features[i].getNoOfGeneratedVectors();
		}
		
		// Other words, in total, windowsiye-1
		for (int n = 0; n < windowSize - 1; n++) {
			for (int i = 0; i < features.length; i++) {
				if (features[i].alwaysActive()) {
					sum += features[i].getNoOfGeneratedVectors();
				}
			}
		}
		return sum;
	}

	protected void printVector(String token) {
		Vector[] vectors = getFeatureVector(token);
		for (Vector vector : vectors) {
			for (int i = 0; i < vector.indices.length; i++) {
				System.out.println(getFeatureNames("test")[0][vector.indices[i]]  
						+ "\t" + vector.values[i]);
			}
			System.out.println("");
		}
	}
	
	protected void printVector(String token, String rawToken, int pos) {
		Vector[] vectors = getFeatureVector(token, rawToken, pos);
		for (Vector vector : vectors) {
			for (int i = 0; i < vector.indices.length; i++) {
				System.out.println(getFeatureNames(Integer.toString(pos))[0][vector.indices[i]] 
						+ "\t" + vector.values[i]);
			}
			System.out.println("");
		}
	}

	public boolean alwaysActive() {
		return alwaysActive;
	}
	
	public void setAlwaysActive(boolean newVal) {
		alwaysActive = newVal;
	}
}
