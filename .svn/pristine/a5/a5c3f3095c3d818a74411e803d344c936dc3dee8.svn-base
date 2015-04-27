package nlp.stats;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nlp.model.LabeledDataset;
import nlp.model.Sentence;
//说白了,就两个映射表,一个是tag-frequencz表示出现次数,第二个是teg-index表示tag的编号
public class TagFreqs {
	//下面这个frequenczTable是token-(tag-times);而且一个token是可以对应多个tag的
	HashMap<String, HashMap<String, Integer>> frequencyTable = new HashMap<String, HashMap<String, Integer>>();
	HashMap<String, Integer> tagSet = new HashMap<String, Integer>();
	LabeledDataset data;

	public TagFreqs(LabeledDataset data, boolean useRawTokens) {
		this.data = data;
		construct(useRawTokens);
	}

	public TagFreqs(LabeledDataset data) {
		this(data, false);
	}
	
	private void construct(boolean useRawTokens) {
		for (Sentence sentence : data.getCorpus()) {
			for (int i = 0; i < sentence.size(); i++) {
				if (useRawTokens)
					incrementTable(sentence.getRawToken(i), sentence.getTag(i));
				else
					incrementTable(sentence.getToken(i), sentence.getTag(i));
				incrementTagSet(sentence.getTag(i));
			}
		}
	}

	private void incrementTagSet(String tag) {
		if (!tagSet.containsKey(tag)) {
			tagSet.put(tag, tagSet.size());
		}
	}

	public int mapTagToId(String tag) {
		return tagSet.get(tag);
	}
	//下面是将tagset里面tag-index这种映射倒过来,变为一个index-tag的映射
	public ArrayList<String> getTagSet() {
		ArrayList<String> orderedTags = new ArrayList<String>();
		for (int i = 0; i < tagSet.size(); i++) {
			orderedTags.add("");
		}
		for (Map.Entry<String, Integer> entry : tagSet.entrySet()) {
			orderedTags.set(entry.getValue(), entry.getKey());//分别是index, string
		}
		return orderedTags;
	}

	private void incrementTable(String token, String tag) {
		HashMap<String, Integer> tagFrequencies;
		//先看这个词token是不是已知的,如果是,先把其对应的tag-times调出来.如果不是则构造一个新的tag-times出来,初始化的int=0
		if (frequencyTable.containsKey(token)) {
			tagFrequencies = frequencyTable.get(token);//frequencyTable.get(token)返回的是一个string-int的元祖
		} else {
			tagFrequencies = new HashMap<String, Integer>();
			frequencyTable.put(token, tagFrequencies);
		}

		incrementTagTable(tagFrequencies, tag);
	}
	//把对应tag出现次数加1
	private void incrementTagTable(HashMap<String, Integer> tagFrequencies, String tag) {
		Integer oldValue = 0;

		if (tagFrequencies.containsKey(tag)) {
			oldValue = tagFrequencies.get(tag);
		}

		tagFrequencies.put(tag, oldValue + 1);
	}
	//查询token的所有tag的归一化次数
	public HashMap<String, Float> getTagFrequencies(String token) {
		HashMap<String, Float> result = new HashMap<String, Float>();
		HashMap<String, Integer> tagFrequencies = frequencyTable.get(token);

		if (tagFrequencies != null) {
			for (Map.Entry<String, Integer> entry : tagFrequencies.entrySet()) {
				String tag = entry.getKey();
				int frequency = entry.getValue();
				Float normalizedFrequency = (float) frequency / data.getUnigrams().getFrequency(token);
				result.put(tag, normalizedFrequency);
			}
		}
		return result;
	}

	public String getMostFrequentTag(String token) {
		String maxTag = "";
		Integer maxVal = -1;
		HashMap<String, Integer> tagFrequencies = frequencyTable.get(token);

		for (Map.Entry<String, Integer> entry : tagFrequencies.entrySet()) {
			String tag = entry.getKey();
			int frequency = entry.getValue();

			if (frequency > maxVal) {
				maxVal = frequency;
				maxTag = tag;
			}
		}
		return maxTag;
	}

	public String getTagFrequenciesAsDebugString(String token) {
		DecimalFormat df2 = new DecimalFormat("0.00");
		HashMap<String, Float> freqs = getTagFrequencies(token);
		String result = "";

		for (Map.Entry<String, Float> entry : freqs.entrySet()) {
			String tag = entry.getKey();
			Float normalizedFreq = entry.getValue();
			result += " " + tag + " " + df2.format(normalizedFreq);
		}
		return result;
	}
}
