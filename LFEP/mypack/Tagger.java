package mypack;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import nlp.data.Pair;
import nlp.data.Parser;
import nlp.model.LabeledDataset;
import nlp.model.UnlabeledDataset;
import nlp.processing.Features;
import nlp.processing.NgramGenerator;
import nlp.processing.features.DistFeatures;
import nlp.processing.features.MatlabSparse;
import nlp.processing.features.MorphFeatures;
import nlp.processing.features.WordSignatures;

public class Tagger{
	private Parser parser;
	private Map<String, Integer> tag2Id=new HashMap<String, Integer>();
	private Map<Integer, String> id2Tag=new HashMap<Integer, String>();
	//private UnlabeledDataset unlabeledData; 这个量没用
	private LabeledDataset trainingData;
	public Features[] wordFeatures;
	public int no_of_ngrams_train;
	public int no_of_ngrams_test;
	/*
	public Tagger(UnlabeledDataset unlabeledData, LabeledDataset trainingData, WordFeatures[] wordFeatures)
	{
		//this.unlabeledData=unlabeledData;
		this.trainingData=trainingData;
		this.wordFeatures=new Features[wordFeatures.length];
		for(int i=0;i<wordFeatures.length;i++)
		{
			this.wordFeatures[i]=wordFeatures[i].featureType;
			this.wordFeatures[i].setAlwaysActive(wordFeatures[i].isAlwaysActive);				
		}
	}
	*/
	public Tagger(LabeledDataset trainingData, WordFeatures[] wordFeatures)
	{
		//this.unlabeledData=unlabeledData;
		this.trainingData=trainingData;
		this.wordFeatures=new Features[wordFeatures.length];
		for(int i=0;i<wordFeatures.length;i++)
		{
			this.wordFeatures[i]=wordFeatures[i].featureType;
			this.wordFeatures[i].setAlwaysActive(wordFeatures[i].isAlwaysActive);				
		}
	}
	public void sortedTags( String[] tags, int left, int right)
	{
		int i, j;
        String temp;
        i = left;
        j = right;
        if (left > right)
            return;
        temp = tags[left];
        while (i != j)/* 找到最终位置 */
        {
            while (tags[j].compareTo(temp)>0 && j > i)
                j--;
            if (j > i)
                tags[i++] = tags[j];
            while (tags[i].compareTo(temp)<0&& j > i)
                i++;
            if (j > i)
                tags[j--] = tags[i];

        }
        tags[i] = temp;
        sortedTags(tags, left, i - 1);/* 递归左边 */
        sortedTags(tags, i + 1, right);/* 递归右边 */
	}
	public returnOfTaggingTrainingData getTrainingSet(int length_window, String no_of_ngrams)
	{
		
		NgramGenerator gramGen=new NgramGenerator(trainingData, false);
		if (no_of_ngrams=="all")
		{
			//note that no_of_ngram is a string type
			no_of_ngrams=Integer.toString(gramGen.getTotalNoOfNgrams());
		}
		no_of_ngrams_train=Integer.valueOf(no_of_ngrams).intValue();
		returnOfTaggingTrainingData result=new returnOfTaggingTrainingData(Integer.valueOf(no_of_ngrams).intValue());
		returnOfGetFeaturesOfTagger twoFeatureType=new returnOfGetFeaturesOfTagger(Integer.valueOf(no_of_ngrams).intValue());
		//System.out.println("succeed before getFeatures!");
		twoFeatureType=getFeatures(gramGen,length_window, no_of_ngrams);
		//System.out.println("succeed after getFeatures!");
		result.features=twoFeatureType.matuFeatures;
		String[] tagTmp=new String[twoFeatureType.rawFeatures.tags.length];
		for(int i=0;i<twoFeatureType.rawFeatures.tags.length;i++)
		{
			tagTmp[i]=twoFeatureType.rawFeatures.tags[i];
		}
		//String[] tagTmp=twoFeatureType.rawFeatures.tags;
		sortedTags(tagTmp, 0, twoFeatureType.rawFeatures.tags.length-1);
		generateTagIds(tagTmp);
		result.labels=tags2Labels(twoFeatureType.rawFeatures.tags);
		//可能有问题
		//result.tokens=twoFeatureType.rawFeatures.ngrams;
		result.tokens=twoFeatureType.rawFeatures.tokens;
		return result;
	}
	public returnOfTaggingTestData getTestSet(int length_window, String no_of_ngrams, LabeledDataset testData)
	{
		
		NgramGenerator gramGen=new NgramGenerator(testData, false);
		if (no_of_ngrams=="all")
		{
			//note that no_of_ngram is a string type
			no_of_ngrams=Integer.toString(gramGen.getTotalNoOfNgrams());
		}
		no_of_ngrams_test=Integer.valueOf(no_of_ngrams).intValue();
		returnOfTaggingTestData result=new returnOfTaggingTestData(Integer.valueOf(no_of_ngrams).intValue());
		returnOfGetFeaturesOfTagger twoFeatureType=new returnOfGetFeaturesOfTagger(Integer.valueOf(no_of_ngrams).intValue());
		twoFeatureType=getFeatures(gramGen,length_window, no_of_ngrams);
		result.features=twoFeatureType.matuFeatures;
		result.labels=tags2Labels(twoFeatureType.rawFeatures.tags);
		
		//System.out.printf("previsou:%s----%f:\n", twoFeatureType.rawFeatures.tags[820],result.labels[820]);
		//System.exit(0);
		
		result.tokens=twoFeatureType.rawFeatures.tokens;
		result.sBoundaries=twoFeatureType.rawFeatures.sentenceBoundaries;
		Set<String>  unknownWords=testData.getUnknownWords(trainingData);
		for(int i=0;i<result.tokens.length;i++)
		{
			if(!unknownWords.contains(result.tokens[i]))
			{
				result.unknownTokens.add(0);
			}
			else
				result.unknownTokens.add(1);
		}
		return result;
	}
	public returnOfGetFeaturesOfTagger getFeatures(NgramGenerator gramGen, int length_window, String no_of_ngrams)
	{
		returnOfGetFeaturesOfTagger result=new returnOfGetFeaturesOfTagger(Integer.valueOf(no_of_ngrams).intValue());
		//System.out.println("no_of_ngrams");
		//System.out.printf("%d\n", Integer.valueOf(no_of_ngrams).intValue());
		result.rawFeatures=gramGen.getFeatures(Integer.valueOf(no_of_ngrams).intValue(), wordFeatures, length_window);
		//to check the length of rawFeatures.names[]
		/*
		for(int row=0;row<result.rawFeatures.featureNames.length;row++)
		{
			System.out.printf("%d\n", result.rawFeatures.featureNames[row].length);
		}
		*/
		//System.exit(0);
		
		result.matuFeatures=convertToMatlabMatrix(result.rawFeatures, Integer.valueOf(no_of_ngrams).intValue());
		//result.matuFeatures=consolidateFeatures(result.matuFeatures);
		return result;
	}
    public void quickSort(Feature a[], int left, int right) {
        int i, j;
        Feature temp;
        i = left;
        j = right;
        if (left > right)
            return;
        temp = a[left];
        while (i != j)/* 找到最终位置 */
        {
            while (a[j].getIndex() >= temp.getIndex() && j > i)
                j--;
            if (j > i)
                a[i++] = a[j];
            while (a[i].getIndex() <= temp.getIndex() && j > i)
                i++;
            if (j > i)
                a[j--] = a[i];

        }
        a[i] = temp;
        quickSort(a, left, i - 1);/* 递归左边 */
        quickSort(a, i + 1, right);/* 递归右边 */
    }
	public Feature[][] ascending(Feature[][] values)
	{
		//Feature[][] result=new Feature[values.length][];
		for(int i=0;i<values.length;i++)
		{
			//因为最后两个是自己添加的
			quickSort(values[i], 0, values[i].length-1);
		}
		return values;
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public matureFeatures convertToMatlabMatrix(MatlabSparse rawFeatures, int no_of_ngrams)
	{
		//first, we compute a vector length matrix for rawFeatures
 
		ArrayList[] Lists=new ArrayList[no_of_ngrams];
		for(int i=0;i<no_of_ngrams;i++)
		{
			Lists[i]=new ArrayList();
		}
		matureFeatures result=new matureFeatures(no_of_ngrams);
		//int no_of_ngrams=rawFeatures.ngrams.length;
		//the rawFeatures.values.length should be 20
		for(int row=0; row<rawFeatures.values.length;row++)
		{
			//ArrayList list=new ArrayList();
			for(int col=0; col<rawFeatures.values[row].size();col++)
			{
				//note the indicesX contains numbers from 1
				int sample_th=rawFeatures.indicesX[row].elements()[col]-1;
				if(rawFeatures.values[row].elements()[col]!=0.0)
				{

					//System.out.printf("sample_th:%d\n", sample_th);
					double v=(1+Math.log(rawFeatures.values[row].elements()[col]))/rawFeatures.sumPerVector[row].elements()[sample_th];
					int newIndex;
					if(row==0)
					{
						//indicesY里面的值是从1开始的,而且liblinear的要求也是特征index从1开始
						newIndex=rawFeatures.indicesY[row].get(col);
					}
					else{
						int sumLength=0;
						for(int i=0;i<row;i++)
						{
							sumLength+=rawFeatures.featureNames[i].length;
						}
						newIndex=rawFeatures.indicesY[row].get(col)+sumLength;
					}
					FeatureNode featureNode=new FeatureNode(newIndex, v);
					Lists[sample_th].add(featureNode);
				}				
			}					
		}
		

		for(int i=0;i<Lists.length;i++)
		{
			result.values[i]=(Feature[])Lists[i].toArray(new Feature[Lists[i].size()]);		
		}
		//next, make the values[i] in ascending order		
		result.values=ascending(result.values);		
		
		//next, do concatenate the name vectors
		for(int row=0;row<rawFeatures.values.length;row++)
		{
			for(int col=0;col<rawFeatures.featureNames[row].length; col++)
			{
				result.names.add(rawFeatures.featureNames[row][col]);
			}			
		}
		return result;
	}
	//这是为trainingdata的tag的
	public void generateTagIds(String[] tags)
	{
		for(int i=0; i<tags.length;i++)
		{
			//这个函数里面,tag2Id开始还是空的
			if(!tag2Id.containsKey(tags[i]))
			{
				//id从0开始
				tag2Id.put(tags[i], tag2Id.size()+1);
				//此时,tag2id的长度已经增1
				if(!id2Tag.containsKey(tag2Id.size()))
				{
					id2Tag.put(tag2Id.size(), tags[i]);
				}
			}
		}
		//打印tag2Id
		/*
		for (Map.Entry<String,Integer> entry : tag2Id.entrySet()) {
			System.out.printf("previsou:%s----%d:\n", entry.getKey(),entry.getValue());
		  }
		System.exit(0);
		*/
	}
	//这是转化testdata的tags的
	public double[] tags2Labels(String[] tags)
	{
		double[] result=new double[tags.length];
		for(int i=0;i<tags.length;i++)
		{
			if(tag2Id.containsKey(tags[i]))
			{
				result[i]=tag2Id.get(tags[i])*1.0; //convert the int to double
			}
			else{
				//下面应该是tag2Id里面最大id+1
				  int max= 0;
				  for (Map.Entry<String, Integer> entry : tag2Id.entrySet()) 
				  {
				    int id = entry.getValue();
				    if (id > max) {
				      max= id;
				    }
				  }
				result[i]=(max+1)*1.0;
			}
		}
		return result;
	}
	/*
	public void printFeatureVector(matureFeatures features, String[] tokens, int index)
	{
		//IntArrayList indexOfNon0;
		//indexOfNon0=new IntArrayList(features.values[index].size());
		for(int i=0;i<features.values[index].size();i++)
		{
			if(features.values[index].get(i).!=0)
			{
				//indexOfNon0.add(i);
				System.out.printf("%s\t%.4f\n", features.names.get(i).toString(), features.values[index].elements()[i]);
			}
		}
		System.out.printf("Input n-grams: %s\n", tokens[index]);
		//indexOfNon0.trimToSize();
	}
	*/
	public Map<Integer, String> getLabelsToTagMap()
	{
		//Map<Integer, String> result=new HashMap<Integer, String>();
		//result=id2Tag;
		//return result;
		return id2Tag;
	}
	public void writeFeature2File(returnOfTaggingTrainingData features)
	{
		
	}
	public String[] mapLabelsToTags(Map<Integer, String> map, double[] labels)
	{
		String[] result=new String[labels.length];
		for(int i=0;i<labels.length;i++)
		{
			//note the predicted labels in liblinear is double. not int
			if(map.containsKey((int)labels[i]))
			{
				result[i]=map.get((int)labels[i]);
			}
			else
				result[i]="UNKNOWN";
		}
		return result;
		
	}
	public double[] unknownWordAccuracy(double[] labelsGold, double[] labelsPredicted, IntArrayList oovIndices)
	{
		double[] result=new double[3];
		int amountOfOov=0;  //64
		int amountOfMatch=0;
		for(int i=0;i<oovIndices.size();i++)
		{
			if(oovIndices.elements()[i]==1)
			{
				amountOfOov++;
				if(labelsGold[i]==labelsPredicted[i])
				{
					amountOfMatch++;
				}
			}
		}
		result[0]=amountOfMatch*1.0/amountOfOov;
		result[1]=amountOfMatch*1.0;
		result[2]=amountOfOov*1.0;
		return result;
	}
	public double[] mostFreqElem(DoubleArrayList values) {
		  HashMap<Double,Integer> freqs = new HashMap<Double,Integer>();

		  for (double val : values.elements()) {
		    Integer freq = freqs.get(val);
		    freqs.put(val, (freq == null ? 1 : freq+1));
		  }

		  double[] mode = new double[2];
		  int maxFreq = 0;

		  for (Map.Entry<Double,Integer> entry : freqs.entrySet()) {
		    int freq = entry.getValue();
		    if (freq > maxFreq) {
		      maxFreq = freq;
		      mode[0] = entry.getKey();
		      mode[1] = freq;
		    }
		  }

		  return mode;
		}

	public double majBaseline(returnOfTaggingTestData return_test)
	{
		double result;
		DoubleArrayList list=new DoubleArrayList();
		for (int i=0;i<return_test.unknownTokens.size();i++)
		{
			if (return_test.unknownTokens.get(i)==1)
				list.add(return_test.labels[i]);
		}
		double[] mode=mostFreqElem(list);
		result=mode[1]/list.size();
		return result;
	}
	public void printErrors(double[] Goldlabels, double[] predictedlabels, IntArrayList unknownTokens, String[] tokens, String fileName, IntArrayList sBoundaries)
	{
		//int[] errorsOn=new int[Goldlabels.length];
		Map<Double, Integer>label_occTimes=new HashMap<Double, Integer>();
		for(int i=0;i<Goldlabels.length;i++)
		{
			if(unknownTokens.elements()[i]==1&&Goldlabels[i]!=predictedlabels[i])
			{
				/*
				if(Goldlabels[i]==18.0)
				{
					System.out.printf("第几个:%d--",i);
					System.exit(0);
				}
				*/
				//the occur times of ith elem in Goldlabels add 1
				Integer freq = label_occTimes.get(Goldlabels[i]);
				label_occTimes.put(Goldlabels[i], (freq == null ? 1 : freq+1));
			}
			//else 
				//errorsOn[i]=0;
		}
		//按照升序排列
		//Map<Double, Integer> sortedMap=MorphFeatures.sortByValues(label_occTimes);
		

		System.out.println("gold_tag\terrors\ttotal\trel.errors\tclass_recall\tclass_precision\n");
		
		// the first one has the most times
		double[] labelInDescending=new double[label_occTimes.size()];

		int iter=0;
		for(Map.Entry<Double,Integer> entry : label_occTimes.entrySet()) 
		{			    			    
			labelInDescending[iter]= entry.getKey();
			iter++;
		}
		String[] errorNames=mapLabelsToTags(getLabelsToTagMap(), labelInDescending);
		/*
		for(int i=0;i<labelInDescending.length;i++)
		{
			System.out.printf("%f--",labelInDescending[i]);
		}
		System.out.println("\n");
		
		
		for(int i=0;i<errorNames.length;i++)
		{
			System.out.printf("%s--",errorNames[i]);
		}

		System.exit(0);
		*/
		// count the number of "1" in unknownTokens
		int nonZeroInOOV=0;
		for (int i=0;i<unknownTokens.size();i++)
		{
			if (unknownTokens.get(i)!=0)
				nonZeroInOOV++;
		}
		//System.out.println(nonZeroInOOV);
		//每一种错误的label
		for(int classTag=0;classTag<errorNames.length;classTag++)
		{
			int classSize=0;
			int taggedAs=0;
			int taggedAsCorrectly=0;
			double precision=0.0;
			double recall=0.0;
			for(int i=0;i<unknownTokens.size();i++)
			{
				if(unknownTokens.get(i)==1)
				{
					if(Goldlabels[i]==labelInDescending[classTag])
					{
						classSize++;
					}
					if (predictedlabels[i]==labelInDescending[classTag])
					{
						taggedAs++;
					}
					if(predictedlabels[i]==labelInDescending[classTag]&&Goldlabels[i]==predictedlabels[i])
					{
						taggedAsCorrectly++;
					}
				}
			}
			precision=taggedAsCorrectly*1.0/taggedAs;
			recall=taggedAsCorrectly*1.0/classSize;
			System.out.printf("%s\t%d\t%d\t%.3f%%\t%.3f%%\t%.3f%%\n", errorNames[classTag], label_occTimes.get(labelInDescending[classTag]), classSize, label_occTimes.get(labelInDescending[classTag])*100.0/nonZeroInOOV, recall*100, precision*100);
			
		}
		System.out.println("\n");
		System.exit(0);
		//the file to write
		//File fid=new File(fileName, "w");
		//RandomAccessFile rf = new RandomAccessFile(fileName,"rw");   
		String[] gold=mapLabelsToTags(getLabelsToTagMap(),Goldlabels);
		String[] predicted=mapLabelsToTags(getLabelsToTagMap(),predictedlabels);
		String unk;
		for(int i=0;i<tokens.length;i++)
		{
			if(unknownTokens.get(i)==1)
			{
				unk="U";
			}
			else
				unk=" ";

			System.out.printf(fileName, "%s\t%s\t%s\t%s\n", tokens[i], predicted[i], gold[i], unk);
			if(sBoundaries.contains(i))
			{
				System.out.printf(fileName, "\n");
			}
		}			
	}
}