package nlp.processing.features;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

import nlp.model.LabeledDataset;
import nlp.processing.Features;
//正则表达式
public class RegexFeatures extends Features {
	public static final String patternString[][] = {
		{"iC",     	"[A-Z].+"},
		{"hD",     		".*\\d.*"},
		{"allCap",      "^[A-Z]+$"},  //the beginning of the line, A through Z, 
		{"twoCap",      "^[A-Z][^A-Z]+[A-Z].*$"}
	};
	public Pattern compiledPatterns[];

	/**
	 * @param m
	 */
	public RegexFeatures() {
		compiledPatterns = new Pattern[patternString.length];
		for (int i = 0; i < patternString.length; i++) {
			//Pattern.compile()把字符串变成一种正则表达式pattern
			compiledPatterns[i] = Pattern.compile(patternString[i][1]); //全是第二列
		}
	}

	protected Vector[] getFeatureVector(String token) {
		HashMap<Integer, Integer> features = new HashMap<Integer, Integer>();
		//在compiledPattern里面挨个挨个找,在能与token模式匹配的地方设一个1,得到一个向量
		for(int i = 0; i < compiledPatterns.length; i++) {
		    if (compiledPatterns[i].matcher(token).matches())
		    	features.put(i, 1);
		}
		return toVector(features);
	}
	
	@Override
	protected Vector[] getFeatureVector(String token, String rawToken, int pos) {
		return getFeatureVector(rawToken);
	}

	@Override
	public String[][] getFeatureNames(String pos) {
		String[][] out = new String[1][patternString.length];
		for (int i = 0; i < patternString.length; i++) {
			out[0][i] = "{reg|" + pos + "}" + patternString[i][0];
		}
		return out;
	}

	public static void main(String[] args) {
		RegexFeatures test = new RegexFeatures();
		Vector[] v = test.getFeatureVector("repaying");
		for (int i = 0; i < v[0].indices.length; i++) {
			System.out.println(test.getFeatureNames("")[0][v[0].indices[i]]);
		}
	}
	//by wenpeng
	public void saveFeatureName() throws IOException
	{
		
	}
	//by wenpeng
	public void writeFeatures(LabeledDataset trainingData){}
}