package nlp.processing.features;

import java.util.HashMap;

import nlp.model.Dataset;

public class MorphFeaturesMod extends MorphFeatures {
	
	public MorphFeaturesMod(Dataset data) {
		super();
		for (String token : data.getVocabulary())  {
			for (int i = -1; i < token.length(); i++) {
				String suffix;
				if (i == - 1) {
					suffix = "|" + token;//if it is |at, it means the word 'at' itself
				} else {
					suffix = token.substring(i);
				}
				if (!featureIndex.containsKey(suffix))  {
					featureIndex.put(suffix, featureIndex.size());
				}
			}
		}
	}
	
	@Override
	public Vector[] getFeatureVector(String token) {
		HashMap<Integer, Integer> result = new HashMap<Integer, Integer>();
		for (int i = -1; i < token.length(); i++) {
			String suffix;
			if (i == - 1) {
				suffix = "|" + token;
			} else {
				suffix = token.substring(i);
			}

			if (featureIndex.containsKey(suffix)) {
				int index = featureIndex.get(suffix);
				result.put(index, 1);
			}
		}

		return toVector(result);
	}
}
