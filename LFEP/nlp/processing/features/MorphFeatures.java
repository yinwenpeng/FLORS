package nlp.processing.features;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import mypack.Entrance;
import nlp.model.Dataset;
import nlp.model.LabeledDataset;
import nlp.model.Sentence;
import nlp.processing.Features;
import nlp.stats.UnigramFreqs;

public class MorphFeatures extends Features {
	protected HashMap<String, Integer> featureIndex = new HashMap<String, Integer>();
	
	
	public MorphFeatures(Dataset data, int n) {
		HashMap<String, Integer> suffixFrequencies = countSuffixes(data.getUnigrams());
		this.featureIndex = getNMostFrequentSuffixes(suffixFrequencies, n);
	}
	//输入dataset就是一个unigramfrequency
	public MorphFeatures(Dataset data) {
		for (String token : data.getVocabulary())  
		{
			for (int i = 0; i < token.length(); i++) 
			{
				String suffix = token.substring(i);  //here, 'i' is the fault begin position; to the end
				if (!featureIndex.containsKey(suffix)) { 
					featureIndex.put(suffix, featureIndex.size());
				}
			}
		}
	}
	//read a line, to store it into featureIndex
	public void convert2featureIndex(String line)
	{
		String deli=" ";
		for(String name:line.split(deli))
		{
			updateFeatureIndex(name);
		}
	}
	//update the featureIndex by token
	public void updateFeatureIndex(String token) {		
			int position=token.lastIndexOf("|");
			String name=token.substring(0, position);
			int index=Integer.parseInt(token.substring(position+1));
			featureIndex.put(name, index);	
	}
	// it seems have no use
	public MorphFeatures(String filePath) throws IOException
	{
		String delimiter = "\t";  //tab
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String currentLine;
		//Sentence currentSentence = new Sentence();

		while ((currentLine = br.readLine()) != null) {
			if (currentLine.isEmpty()) { // meet the end of a sentence
				/*
				if (currentSentence.size() > 0) {
					if (replaceDigits)
						currentSentence.replaceDigits();
					if (ignoreCapitalization)
						currentSentence.convertToLowerCase();
					// add current sentence into corpus
					corpus.add(currentSentence);
					currentSentence = new Sentence();
				}
				*/
				continue;
			} else {
				String[] splitted = currentLine.split(delimiter);
				if (splitted.length != 2) {
					br.close();
					throw new IOException("Malformed line: " + currentLine);
				} else {
					String token = splitted[0];
					/*
					String tag = splitted[1];
					currentSentence.add(token, tag);
					currentSentence.addRawToken(token);
					*/
					for (int i = 0; i < token.length(); i++) 
					{
						String suffix = token.substring(i);  //here, 'i' is the fault begin position; to the end
						if (!featureIndex.containsKey(suffix)) { 
							featureIndex.put(suffix, featureIndex.size());
						}
					}
				}
			}
		}
		
		br.close();

		
	}
	
	public  MorphFeatures() {
	}

	private HashMap<String, Integer> getNMostFrequentSuffixes(
			HashMap<String, Integer> suffixFrequencies, int n) {
		Map<String, Integer> sortedTable = sortByValues(suffixFrequencies);
		HashMap<String, Integer> topNTokens = new HashMap<String, Integer>();

		Iterator<String> it = sortedTable.keySet().iterator();
		int i = 0;
		while (it.hasNext() && topNTokens.size() < n) {
			topNTokens.put(it.next(), i++);
		}
		return topNTokens;
	}

	public static <K, V extends Comparable<V>> Map<K, V> sortByValues(
			final Map<K, V> map) {
		Comparator<K> valueComparator = new Comparator<K>() {
			public int compare(K k1, K k2) {
				int compare = map.get(k2).compareTo(map.get(k1));
				if (compare == 0)
					return 1;
				else
					return compare;
			}
		};

		Map<K, V> sortedByValues = new TreeMap<K, V>(valueComparator);
		sortedByValues.putAll(map);
		return sortedByValues;
	}
	//输入一个unigram预料,得到的是一个后缀出现频率的统计vector
	private HashMap<String, Integer> countSuffixes(UnigramFreqs unigrams) {
		HashMap<String, Integer> suffixFrequencies = new HashMap<String, Integer>();
		for (String word : unigrams.getVocabulary()) {
			for (int i = 0; i < word.length(); i++) {
				incrementTable(suffixFrequencies, word.substring(i));
			}
		}

		return suffixFrequencies;
	}

	private void incrementTable(HashMap<String, Integer> suffixFrequencies,
			String suffix) {
		Integer oldValue = 0;

		if (suffixFrequencies.containsKey(suffix)) {
			oldValue = suffixFrequencies.get(suffix);
		}

		suffixFrequencies.put(suffix, oldValue + 1);
	}
	
	@Override
	public String[][] getFeatureNames(String pos) {
		String[][] out = new String[1][featureIndex.size()];
		
		for (String c : featureIndex.keySet()) {
			out[0][featureIndex.get(c)] =  "{suf|" + pos + "}" + c;
		}		
		return out;
	}
	//输入是一个token,然后便利产生它的所有后缀,对每一个后缀计算在featureindex里面的编号,然后在自己的特征向量对应的entry里面设为1.
	//这个函数产生的向量确实是所有非0的特征值
	@Override
	public Vector[] getFeatureVector(String token) {
		HashMap<Integer, Integer> result = new HashMap<Integer, Integer>();
		for (int i = 0; i < token.length(); i++) {
			String suffix = token.substring(i);

			if (featureIndex.containsKey(suffix)) {
				//featureIndex comes from MorphFeatures(trainingData)
				int index = featureIndex.get(suffix);
				result.put(index, 1);
			}
		}
		//下面调用Features类里面的函数toVector()
		return toVector(result);
	}
	//by wenpeng
	public HashMap<String, Integer> getFeatureIndex()
	{
		return this.featureIndex;
	}
	//by wenpeng
	public void saveFeatureName() throws IOException
	{
		FileWriter fw = new FileWriter(Entrance.file_prefix_store+saveFile, true);
		for(String name:getFeatureIndex().keySet())
		{
			fw.write(name+"|"+getFeatureIndex().get(name)+" ");
		}
		fw.write("\n");
		fw.close();
	}
	//by wenpeng
	public void writeFeatures(LabeledDataset trainingData){}
}