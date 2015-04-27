package nlp.processing.features;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mypack.Entrance;
import nlp.model.LabeledDataset;
import nlp.model.UnlabeledDataset;
import nlp.processing.Features;
import nlp.stats.BigramFreqs;
import nlp.stats.UnigramFreqs;
import no.uib.cipr.matrix.VectorEntry;
import no.uib.cipr.matrix.sparse.SparseVector;

public class DistFeatures extends Features {
	private BigramFreqs bigrams;
	private UnigramFreqs unigrams;
	
	private ArrayList<String> featureNames;
	private int[] featureNameToId;//将unigram编号转化为top words降序编号
	public Map<Integer, Integer>printId;  //将top words降序编号转化为打印顺序编号
	private boolean useCutOffFeature = false;
		
	public DistFeatures(UnlabeledDataset data, int noOfFeatures, UnlabeledDataset representationSet) {
		this.bigrams = data.getBigrams();
		this.unigrams = data.getUnigrams();
		ArrayList<String> names = representationSet.getUnigrams().getNmostFrequentTokens(noOfFeatures);
		
		setFeatureNames(names);
	}
	
	public DistFeatures(UnlabeledDataset data) {
		this(data, data.getVocabularySize(), data);
	}
	
	public DistFeatures(UnlabeledDataset data, int noOfFeatures) {
		this(data, noOfFeatures, data);//call the first construction function
	}
	//read a line which contains all the feature vector
	public DistFeatures(String line)
	{
		String deli=" ";
		this.featureNames=new ArrayList<String>();
		for(String name:line.split(deli))
		{
			this.featureNames.add(name);
		}
	}
	public DistFeatures(ArrayList<String> unlabeledDataPath, int noOfFeatures) {
		//bigram
		
		//unigrams
		
		//featurenames
	}
	
	
	//注意输入是corpus里面最高频的n个token的string,然后每个token会被赋予一个编号,最高频的是0,其次是1,...
	private void setFeatureNames(ArrayList<String> newFeatureNames) {
		featureNames = newFeatureNames;//featureNames在这儿就没有变,还是原始的top词的string,只是在最后加了一个<other contexts>
		//featureNameToId长度是所有词,但是编号只是考虑top 500
		featureNameToId = new int[unigrams.getVocabulary().size()];
		Arrays.fill(featureNameToId, -1);//先将featureNameToId这个数组全部置为-1
		for (int i = 0; i < newFeatureNames.size(); i++) {
			//最高频的词的编号是0,然后是1,...非top500的编号为-1, featurenameToId本质上就是将unigram中的编号转化为top words 的descending 编号
			featureNameToId[unigrams.translate(newFeatureNames.get(i))] =  i;
		}
		//如果只是使用了一些高频词作为特征,那么在后面添加一个额外的特征叫"other_contexts"
		if (featureNames.size() < unigrams.getVocabulary().size()) {
			featureNames.add("<OTHER_CONTEXTS>");
			useCutOffFeature = true;//useCutOffFeature表示是不是使用了部分词,例如最高的500,而不是全部词作为context
		}
	}
	
	//获取一个token的特征向量,输出两个向量,分别是左边向量,右边向量
	public Vector[] getFeatureVector(String token) {
		Vector[] result = {getLeftFeatureVector(token), getRightFeatureVector(token)};
		return result;
	}
	
	public Vector getLeftFeatureVector(String token) {
		if (unigrams.getVocabulary().contains(token))
			//下面因为bigrams.getLeftFrequencies(token)的维度是整个vocabulary,所以需要convertToFeatureVector()来缩减到top 500维度
			return convertToFeatureVector(bigrams.getLeftFrequencies(token));
		else
			return new Vector(0);
	}

	public Vector getRightFeatureVector(String token) {
		if (unigrams.getVocabulary().contains(token))
			return convertToFeatureVector(bigrams.getRightFrequencies(token));
		else
			return new Vector(0);
	}
	
	//根据token返回的一个原始的包含所有词的sparsevector来计算最终的特征向量,例如只考虑500最高频的词
	public Vector convertToFeatureVector(SparseVector sparseVector) {
		if (useCutOffFeature) {
			return constructReducedVector(sparseVector);
		} else {
			return constructFullVector(sparseVector);
		}
	}
	//最终返回的向量的名称还是按照进入unigram先后的顺序,并不是给top words降序	
	public Vector constructReducedVector(SparseVector sparseVector) {
		ArrayList<Integer> indices = new ArrayList<Integer>();
		ArrayList<Integer> values = new ArrayList<Integer>();
		
		Iterator<VectorEntry> iterator = sparseVector.iterator();
		int cutOffValues = 0;
		while (iterator.hasNext()) 
		{
			VectorEntry entry = iterator.next();
			int featureId =featureNameToId[entry.index()];
			//if it is not one of top 500, sum its count 
			if (featureId == -1) {
				cutOffValues += entry.get(); 
			} else {
				//I find that the indices are not in ascending order.
				indices.add(featureNameToId[entry.index()]);
				//System.out.printf("distFeature Index:%d\n", featureNameToId[entry.index()]);
				//这儿没有判断value是否为1,也就是0值也保存了
				values.add((int)entry.get());
			}
		}
		//System.exit(0);
		//下面应该是将与非top frequen的词的发生总数放在vector的最后一项,但是有可能为0
		if (cutOffValues > 0.0d) {
			indices.add(featureNames.size() - 1);  //size=501
			values.add(cutOffValues);
		}
		return new Vector(indices, values);
	}
	
	//如果使用全部词作为特征,那么只需要将这个sparsevector稍微改变一下,原来是token-times,现在改为id-times
	public Vector constructFullVector(SparseVector sparseVector) {
		Vector result = new Vector(sparseVector.getUsed());
		
		Iterator<VectorEntry> iterator = sparseVector.iterator();
		int index = 0;

		while (iterator.hasNext()) {
			VectorEntry entry = iterator.next();

			result.indices[index]= featureNameToId[entry.index()];
			result.values[index]= (int) entry.get();
			index++;
		}
		
		return result;
	}
	
	@Override
	public int getNoOfGeneratedVectors() {
		return 2;
	}
	
	public String[][] getFeatureNames(String pos) {
		String[][] both = new String[2][0];
		//both [0] and both[1] are string list
		both[0]=getLeftFeatureNames(pos);
		both[1]=getRightFeatureNames(pos);
		
		return both;
	}
	//String[] rename(String[] names, String suffix, String prefix) 就是将names里面的每一个entry前后加一个字段.下面两个函数应该只加到后面
	public String[] getLeftFeatureNames(String pos) {
		//生成的新name可能是"host{l|-2}}"这种形式,表示这是窗口中第一个词(相对middle word的位置)的左边bigram,名字叫host
		//the following method"rename" does not change the content of "featureNames"
		return rename(featureNames.toArray(new String[featureNames.size()]),"","{l|" + pos + "}");
	}
	
	public String[] getRightFeatureNames(String pos) {
		return rename(featureNames.toArray(new String[featureNames.size()]),"","{r|" + pos + "}");
	}	
	//by wenpeng
	public ArrayList<String> getFeatureNames()
	{
		return this.featureNames;
	}
	//by wenpeng
	public void saveFeatureName() throws IOException
	{
		//distFeature names are stored in the first line, so not need to write to the end of the file
		FileWriter fw = new FileWriter(Entrance.file_prefix_store+saveFile);
		for(String name:getFeatureNames())
		{
			fw.write(name+" ");
		}
		fw.write("\n");
		fw.close();
	}
	//by wenpeng 
	public void computePrintId()
	{
		printId=new HashMap<Integer, Integer>();//string(unigram)id(featureNameToId)indexOfTopWords(printId)
		int showId=0;
		for(int i=0;i<unigrams.getVocabulary().size();i++) 
		{
			int featureId =featureNameToId[i];
			if (featureId != -1) {
				printId.put(featureId, showId++);//第一个遇到的top word打印为第0列
			}
		}
		printId.put(-1, featureNames.size() - 1);

	}
	//by wenpeng
	public void writeFeatures(LabeledDataset trainingData) throws IOException
	{
		Set<String>knownWords=new HashSet<String>(trainingData.getVocabulary());
		//store the feature vectors of each word index into a file
		FileWriter left = new FileWriter(Entrance.file_prefix_store+"leftVector.txt");
		FileWriter right = new FileWriter(Entrance.file_prefix_store+"rightVector.txt");

		//for each word in Unigram
		for(String token:unigrams.wordToId.keySet())
		{
			Vector vectorLeft=getLeftFeatureVector(token); 
			//write left first
			left.write(token+" ");
			for(int i=0;i<vectorLeft.length();i++)
			{
				left.write(vectorLeft.indices[i]+"-"+vectorLeft.values[i]+" ");
			}
			if(knownWords.contains(token)) left.write("1\n");  //use 1 to denote as a known word
			else left.write("0\n");
			Vector vectorRight=getRightFeatureVector(token);
			//write right then
			right.write(token+" ");
			for(int i=0;i<vectorRight.length();i++)
			{
				right.write(vectorRight.indices[i]+"-"+vectorRight.values[i]+" ");
			}
			if(knownWords.contains(token)) right.write("1\n");
			else right.write("0\n");
		}
		left.close();
		right.close();
		
		//next, should write from unigram.wordtoid, featureNameToId, printId,这样才知道给一个词,然后需要更新那一行的那一列
		FileWriter wordToRowCol = new FileWriter(Entrance.file_prefix_store+"wordToRowCol.txt");

		// the next two for loop can be wrote in "string, int, int" in a line
		for(Entry<String, Integer> entry: unigrams.wordToId.entrySet())
		{
			wordToRowCol.write(entry.getKey()+"||"+entry.getValue()+" ");
		}
		wordToRowCol.write("\n");
		for(int i=0;i<featureNameToId.length;i++)
		{
			wordToRowCol.write(i+"||"+featureNameToId[i]+" ");
		}
		wordToRowCol.write("\n");
		//下面的应该没用
		computePrintId();
		for(Entry<Integer, Integer> entry: printId.entrySet())
		{
			wordToRowCol.write(entry.getKey()+"||"+entry.getValue()+" ");
		}
		wordToRowCol.write("\n");
		wordToRowCol.close();
	}
}