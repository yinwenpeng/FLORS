package nlp.processing.features;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;

import mypack.Entrance;
import nlp.model.LabeledDataset;
import nlp.model.Sentence;
import nlp.processing.Features;

public class WordSignatures extends Features {
	LabeledDataset trainingData;
	LinkedHashMap<String, Integer> allSignatures = new LinkedHashMap<String, Integer>();
	
	
	public WordSignatures(){};
	public WordSignatures(LabeledDataset trainingData) {
		this.trainingData = trainingData;
		createListOfSignatures();
	}
	//by wenpeng
	public void convert2allSignature(String line)
	{
		String deli=" ";
		for(String name: line.split(deli))
		{
			updateallSignatures(name);
		}
	}
	//by wenpeng
	public void updateallSignatures(String token) {		
		int position=token.indexOf("|");
		String name=token.substring(0, position);
		int index=Integer.parseInt(token.substring(position+1));
		allSignatures.put(name, index);	
}
	private void createListOfSignatures() {
		for (Sentence sentence : trainingData.getCorpus()) {
			for (int i = 0; i < sentence.size(); i++) {
				String signature = getSignature(sentence.getRawToken(i), i);
				if (!allSignatures.containsKey(signature)) {
					allSignatures.put(signature, allSignatures.size());
				}
			}
		}
	}
	public void createListOfSignatures(Sentence sentence)
	{
		for (int i = 0; i < sentence.size(); i++) {
			String signature = getSignature(sentence.getRawToken(i), i);//this further proves that the second para in "getSignature" is the position in a sentence
			if (!allSignatures.containsKey(signature)) {
				allSignatures.put(signature, allSignatures.size());
			}
		}
	}
	//第二个参数是指这个词在句子中的位置
	public  String getSignature(String word, int loc) {
		StringBuffer sb = new StringBuffer();

		if (word.length() == 0)
			return sb.toString();

		// Reformed Mar 2004 (cdm); hopefully much better now.
		// { -CAPS, -INITC ap, -LC lowercase, 0 } +
		// { -KNOWNLC, 0 } + [only for INITC]
		// { -NUM, 0 } +
		// { -DASH, 0 } +
		// { -last lowered char(s) if known discriminating suffix, 0}
		int wlen = word.length();
		int numCaps = 0;
		boolean hasDigit = false;
		boolean hasDash = false;
		boolean hasLower = false;
		//先判断这个word是否有数字,破折号,大写和小写
		for (int i = 0; i < wlen; i++) {
			char ch = word.charAt(i);
			if (Character.isDigit(ch)) {
				hasDigit = true;
			} 
			else if (ch == '-') 
			{
				hasDash = true;
			} 
			else if (Character.isLetter(ch)) //如果是字母
			{
				if (Character.isLowerCase(ch)) 
				{
					hasLower = true;
				} 
				else if (Character.isTitleCase(ch)) 
				{
					hasLower = true;//是否有(小写)字母?
					numCaps++;
				} 
				else 
				{
					numCaps++;
				}
			}
		}
		char ch0 = word.charAt(0);
		String lowered = word.toLowerCase();
		if (Character.isUpperCase(ch0) || Character.isTitleCase(ch0)) {
			if (loc == 0 && numCaps == 1) //if it is the first word of a sentence and it has only one capital char.
			{
				sb.append("-INITC");
				if (trainingData.getUnigrams().getVocabulary().contains(lowered)) 
				{
					sb.append("-KNOWNLC");
				}
			} else {
				sb.append("-CAPS");
			}
		} else if (!Character.isLetter(ch0) && numCaps > 0) {
			sb.append("-CAPS");
		} else if (hasLower) { // (Character.isLowerCase(ch0)) {
			sb.append("-LC");
		}
		if (hasDigit) {
			sb.append("-NUM");
		}
		if (hasDash) {
			sb.append("-DASH");
		}
		if (lowered.endsWith("s") && wlen >= 3) {
			// here length 3, so you don't miss out on ones like 80s
			char ch2 = lowered.charAt(wlen - 2);
			// not -ess suffixes or greek/latin -us, -is
			if (ch2 != 's' && ch2 != 'i' && ch2 != 'u') {
				sb.append("-s");
			}
		} else if (word.length() >= 5 && !hasDash && !(hasDigit && numCaps > 0)) {
			// don't do for very short words;
			// Implement common discriminating suffixes
			if (lowered.endsWith("ed")) {
				sb.append("-ed");
			} else if (lowered.endsWith("ing")) {
				sb.append("-ing");
			} else if (lowered.endsWith("ion")) {
				sb.append("-ion");
			} else if (lowered.endsWith("er")) {
				sb.append("-er");
			} else if (lowered.endsWith("est")) {
				sb.append("-est");
			} else if (lowered.endsWith("ly")) {
				sb.append("-ly");
			} else if (lowered.endsWith("ity")) {
				sb.append("-ity");
			} else if (lowered.endsWith("y")) {
				sb.append("-y");
			} else if (lowered.endsWith("al")) {
				sb.append("-al");
			}
		}
		return sb.toString();
	}
	//下面是改写getSignature函数
		public  String getSignature(String word, int loc, boolean flag, boolean known) {
			StringBuffer sb = new StringBuffer();

			if (word.length() == 0)
				return sb.toString();

			// Reformed Mar 2004 (cdm); hopefully much better now.
			// { -CAPS, -INITC ap, -LC lowercase, 0 } +
			// { -KNOWNLC, 0 } + [only for INITC]
			// { -NUM, 0 } +
			// { -DASH, 0 } +
			// { -last lowered char(s) if known discriminating suffix, 0}
			int wlen = word.length();
			int numCaps = 0;
			boolean hasDigit = false;
			boolean hasDash = false;
			boolean hasLower = false;
			//先判断这个word是否有数字,破折号,大写和小写
			for (int i = 0; i < wlen; i++) {
				char ch = word.charAt(i);
				if (Character.isDigit(ch)) {
					hasDigit = true;
				} 
				else if (ch == '-') 
				{
					hasDash = true;
				} 
				else if (Character.isLetter(ch)) //如果是字母
				{
					if (Character.isLowerCase(ch)) 
					{
						hasLower = true;
					} 
					else if (Character.isTitleCase(ch)) 
					{
						hasLower = true;//是否有(小写)字母?
						numCaps++;
					} 
					else 
					{
						numCaps++;
					}
				}
			}
			char ch0 = word.charAt(0);
			String lowered = word.toLowerCase();
			if (Character.isUpperCase(ch0) || Character.isTitleCase(ch0)) {
				if (loc == 0 && numCaps == 1) //if it is the first word of a sentence and it has only one capital char.
				{
					sb.append("-INITC");
					if (known) 
					{
						sb.append("-KNOWNLC");
					}
				} else {
					sb.append("-CAPS");
				}
			} else if (!Character.isLetter(ch0) && numCaps > 0) {
				sb.append("-CAPS");
			} else if (hasLower) { // (Character.isLowerCase(ch0)) {
				sb.append("-LC");
			}
			if (hasDigit) {
				sb.append("-NUM");
			}
			if (hasDash) {
				sb.append("-DASH");
			}
			if (lowered.endsWith("s") && wlen >= 3) {
				// here length 3, so you don't miss out on ones like 80s
				char ch2 = lowered.charAt(wlen - 2);
				// not -ess suffixes or greek/latin -us, -is
				if (ch2 != 's' && ch2 != 'i' && ch2 != 'u') {
					sb.append("-s");
				}
			} else if (word.length() >= 5 && !hasDash && !(hasDigit && numCaps > 0)) {
				// don't do for very short words;
				// Implement common discriminating suffixes
				if (lowered.endsWith("ed")) {
					sb.append("-ed");
				} else if (lowered.endsWith("ing")) {
					sb.append("-ing");
				} else if (lowered.endsWith("ion")) {
					sb.append("-ion");
				} else if (lowered.endsWith("er")) {
					sb.append("-er");
				} else if (lowered.endsWith("est")) {
					sb.append("-est");
				} else if (lowered.endsWith("ly")) {
					sb.append("-ly");
				} else if (lowered.endsWith("ity")) {
					sb.append("-ity");
				} else if (lowered.endsWith("y")) {
					sb.append("-y");
				} else if (lowered.endsWith("al")) {
					sb.append("-al");
				}
			}
			return sb.toString();
		}

	@Override
	public String[][] getFeatureNames(String pos) {
		String[][] out = { rename(allSignatures.keySet().toArray(new String[allSignatures.size()]), "", "{wsign.|" + pos + "}") };
		return out;
	}

	@Override
	public Vector[] getFeatureVector(String token, String rawToken, int pos) {
		Vector[] result = new Vector[1];
		result[0] = new Vector(1);//只创建一个长度的向量,说白了就是int-int
		String signature = getSignature(rawToken, pos);
		if (allSignatures.containsKey(signature)) {
			result[0].indices[0] = allSignatures.get(signature);
			result[0].values[0] = 1;
		}
		return result;
	}

	@Override
	protected Vector[] getFeatureVector(String token) {
		// TODO Auto-generated method stub
		return null;
	}
	public LinkedHashMap<String, Integer> getAllSignatures()
	{
		return this.allSignatures;
	}
	public void saveFeatureName() throws IOException
	{
		FileWriter fw = new FileWriter(Entrance.file_prefix_store+saveFile, true);
		for(String name:getAllSignatures().keySet())
		{
			fw.write(name+"|"+getAllSignatures().get(name)+" ");
		}
		fw.write("\n");
		fw.close();
	}
	//by wenpeng
	public void writeFeatures(LabeledDataset trainingData){}
}
