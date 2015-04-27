package mypack;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import nlp.data.Pair;
import nlp.model.Dataset;
import nlp.model.LabeledDataset;
import nlp.model.Sentence;
import nlp.processing.features.DistFeatures;
import nlp.processing.features.MorphFeatures;
import nlp.processing.features.Vector;
import nlp.processing.features.WordSignatures;
import nlp.stats.BigramFreqs;
import nlp.stats.UnigramFreqs;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;

//assume the top 500 words, 91161 suffix features, and 50 shape feature names are all kept in a file named standardFeatureName.txt; Each kind of
//feature own a single line
// also, the bigram statistics on training data is stored
public class newCorpus extends Dataset{
	Model model;
	DistFeatures distFeature;
	MorphFeatures morphFeature;
	WordSignatures shape;
	Map<Integer, String> id2Tag;
	public static final int MAX_WORDS = 1000000;
	public String fileName;  // the file name of new corpus
	public String featureFile;
	public FileWriter tagedFile;  // store the predicted "word-> tag"
	public int distLength, morphLength, shapeLength, countContext;
	public boolean update=true;
	LinkedHashMap<String, Integer> wordToFeatureIndex; // find the index of feature name given string
	HashMap<String, Integer>String2Index; // used to judge unknown word
	public int window=5;
	//String pwd="//mounts/Users/student/wenpeng/workspace/FLORS";//need modification

	Map<String, int[]>leftContexts;
	Map<String, int[]>rightContexts;
	Map<String, double[]>leftContexts_weakenTarget;
	Map<String, double[]>rightContexts_weakenTarget;
	
	
	Set<String>knownWords=new HashSet<String>();
	Map<String,ArrayList<String>> word2TagList=new HashMap<String,ArrayList<String>>();
	//int unknownWords=0;
	Set<String>unknownWords=new HashSet<String>();
	Set<String> registeredWords=new HashSet<String>();// it include the words in training file and big data
	
	newCorpus() throws IOException
	{
		this.featureFile=Entrance.file_prefix_store+"standardFeatureName.txt";
		BufferedReader br = new BufferedReader(new FileReader(this.featureFile));
		String currentLine;
		int lineCount=0;
		while ((currentLine = br.readLine()) != null) 
		{
			//read feature names to initialize distFeature, morphFeature and shape
			if(lineCount==0)
			{//this is the feature names of distributional feature
				distFeature=new DistFeatures(currentLine);
				this.distLength=this.distFeature.getFeatureNames().size();
				//System.out.print(distLength+"\n");
			}
			if(lineCount==1)
			{
				morphFeature=new MorphFeatures();
				morphFeature.convert2featureIndex(currentLine);
				this.morphLength=this.morphFeature.getFeatureIndex().size();
				//System.out.print(morphLength+"\n");
				//System.exit(0);
			}
			if(lineCount==2)
			{
				shape=new WordSignatures();
				shape.convert2allSignature(currentLine);
				this.shapeLength=this.shape.getAllSignatures().size();
				//System.out.print(shapeLength+"\n");
			}
			if(lineCount==3)
			{
				id2Tag=new HashMap<Integer, String>();
				for(String pair:currentLine.split(" "))
				{
					int position=pair.indexOf("_");
					int label=Integer.parseInt(pair.substring(0, position));
					String tag=pair.substring(position+1);
					id2Tag.put(label, tag);
				}
			}
			lineCount++;
		}
		//System.out.println("reading the feature names completes!\n");
		//System.out.print(distLength*2+morphLength+shapeLength);
		//System.exit(0);
		br.close();
		//tagedFile = new FileWriter("taggedFile.txt");
		//fw = new FileWriter(this.pwd+"/testTaggedFile.txt");
		//wordToId = new LinkedHashMap<String, Integer>();
		File modelFile=new File(Entrance.file_prefix_store+"standardModel.txt");
		//File modelFile=new File(this.pwd+"/testModel.txt");
		this.model=Model.load(modelFile);
		
		loadMatrix();  // load left and right bigrams
		// for weaken target, because we need update with double values, int matrix can not add by double values
		intMatrix2double();
		loadIndexMap();  //load the index transfer information 
		//System.out.println("load index map completed!\n");
	}
	public void intMatrix2double()
	{
		this.leftContexts_weakenTarget=new HashMap<String, double[]>();
		this.rightContexts_weakenTarget=new HashMap<String, double[]>();
		
		for(String token:this.leftContexts.keySet())
		{
			double[] values=new double[leftContexts.get(token).length];
			for(int i=0;i<leftContexts.get(token).length;i++)
			{
				values[i]=leftContexts.get(token)[i]*1.0;
			}
			this.leftContexts_weakenTarget.put(token, values);
		}
		for(String token:this.rightContexts.keySet())
		{
			double[] values=new double[rightContexts.get(token).length];
			for(int i=0;i<rightContexts.get(token).length;i++)
			{
				values[i]=rightContexts.get(token)[i]*1.0;
			}
			this.rightContexts_weakenTarget.put(token, values);
		}
		// i think the int matric can be released
		this.leftContexts.clear();
		this.rightContexts.clear();
	}
	public void loadIndexMap() throws IOException
	{
		wordToFeatureIndex = new LinkedHashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(Entrance.file_prefix_store+"wordToRowCol.txt"));
		//BufferedReader br = new BufferedReader(new FileReader(pwd+"/testwordToRowCol.txt"));
		String currentLine;
		String deli=" ";
		int line=0;
		String2Index=new HashMap<String, Integer>();
		HashMap<Integer, Integer>vocabIndex2Index=new HashMap<Integer, Integer>();
		while ((currentLine = br.readLine()) != null) 
		{
			if(line==0)// means the unigram.wordToId
			{
				//String[] tokens=currentLine.split(deli);
				for(String token:currentLine.split(deli))
				{
					//System.out.println(token);
					int position=token.indexOf("||");
					String word=token.substring(0, position);
					position=token.lastIndexOf("|");
					int index=Integer.parseInt(token.substring(position+1));// || is two bits
					String2Index.put(word, index);
				}
			}
			if(line==1)//means the vocab index to index of feature name
			{
				for(String token:currentLine.split(deli))
				{
					int position=token.indexOf("||");
					int vocabIndex=Integer.parseInt(token.substring(0, position));
					position=token.lastIndexOf("|");
					int index=Integer.parseInt(token.substring(position+1));
					vocabIndex2Index.put(vocabIndex, index);
				}
			}
			if(line==2) break;
			line++;
		}
		br.close();
		for(String word:String2Index.keySet())
		{
			if(vocabIndex2Index.get(String2Index.get(word))!=-1)
			{
				wordToFeatureIndex.put(word, vocabIndex2Index.get(String2Index.get(word)));
			}
			else
				wordToFeatureIndex.put(word, 500);
		}
		
	}
	public void loadMatrix() throws IOException
	{
		leftContexts=new HashMap<String, int[]>();
		//read the content of leftVector.txt and rightVector.txt into leftContexts and rightContexts respectively
		BufferedReader br = new BufferedReader(new FileReader(Entrance.file_prefix_store+"leftVector.txt"));
		String currentLine;
		String deli=" ";
		while ((currentLine = br.readLine()) != null) 
		{
			String[] tokens=currentLine.split(deli);
			if(!leftContexts.containsKey(tokens[0]))
			{
				int[] list=new int[this.distFeature.getFeatureNames().size()];
				for(int i=1;i<tokens.length-1;i++)
				{
					int position=tokens[i].indexOf("-");
					int index= Integer.parseInt(tokens[i].substring(0,position));
					int value= Double.valueOf(tokens[i].substring(position+1)).intValue();
					//note that the index of "other content" is 500
					list[index]=value;
							
				}
				leftContexts.put(tokens[0], list);
			}
			if(tokens[tokens.length-1].equals("1")) knownWords.add(tokens[0]); // collect known words
		}
		//System.out.println(knownWords.size());
		br.close();
		//remember those registered words in case of compute the accuracy of unregistered words
		registeredWords.addAll(leftContexts.keySet());
		//load the right context matrix
		rightContexts=new HashMap<String, int[]>();
		BufferedReader br1 = new BufferedReader(new FileReader(Entrance.file_prefix_store+"rightVector.txt"));
		String Line;
		while ((Line = br1.readLine()) != null) 
		{
			String[] tokens=Line.split(deli);
			if(!rightContexts.containsKey(tokens[0]))
			{
				int[] list=new int[this.distFeature.getFeatureNames().size()];
				for(int i=1;i<tokens.length-1;i++)
				{
					int position=tokens[i].indexOf("-");
					int index= Integer.parseInt(tokens[i].substring(0,position));
					int value= Double.valueOf(tokens[i].substring(position+1)).intValue();
					list[index]=value;				
				}
				rightContexts.put(tokens[0], list);
			}
			
		}
		br1.close();
		//System.out.println("load right Bigram completed!\n");
	}
	public ArrayList<FeatureNode> normalization(ArrayList<FeatureNode> list)
	{
		double sumTmp=0;		
		for(FeatureNode node: list)
			sumTmp+=Math.pow(node.getValue(), 2);
		sumTmp=Math.sqrt(sumTmp);
		double newValue;
		for(FeatureNode node: list)
		{
			newValue=node.getValue();
			node.setValue((newValue)/(sumTmp));
		}
		return list;
	}
	public ArrayList<FeatureNode> getDist(String token, int left)
	{
		int startIndex=this.countContext*(distLength*2+morphLength+shapeLength);
		ArrayList<FeatureNode> result=new ArrayList<FeatureNode>();
		//double sumTmp=0;

		if(left==1)
		{
			/* following two lines seem to be not useful. Because after updating the matrix, each word should have a row in the matrix
			if(!leftContexts.containsKey(token))// is a unknown word
				System.out.println(token);
			*/
			int[] values=leftContexts.get(token);
			for(int i=0;i<values.length;i++)
			{
				if(values[i]!=0)
				{
					FeatureNode newNode=new FeatureNode(i+1+startIndex, 1+Math.log(values[i]*1.0));
					result.add(newNode);
				}
				
			}
		}
		else{
			int[] values=rightContexts.get(token);
			for(int i=0;i<values.length;i++)
			{
				if(values[i]!=0)
				{
					FeatureNode newNode=new FeatureNode(i+1+distLength+startIndex, 1+Math.log(values[i]*1.0));
					result.add(newNode);
				}
				
			}
		}
		//normalization

		return normalization(result);
	}
	public ArrayList<FeatureNode> getDist_weakenTarget(String token, int left)
	{
		int startIndex=this.countContext*(distLength*2+morphLength+shapeLength);
		ArrayList<FeatureNode> result=new ArrayList<FeatureNode>();
		//double sumTmp=0;

		if(left==1)
		{
			/* following two lines seem to be not useful. Because after updating the matrix, each word should have a row in the matrix
			if(!leftContexts.containsKey(token))// is a unknown word
				System.out.println(token);
			*/
			double[] values=this.leftContexts_weakenTarget.get(token);
			for(int i=0;i<values.length;i++)
			{
				if(Math.abs(values[i]-0.0)>0.00001)
				{
					if(values[i]>1.0)
					{
						FeatureNode newNode=new FeatureNode(i+1+startIndex, 1+Math.log(values[i]));
						result.add(newNode);
					}
					else{
						FeatureNode newNode=new FeatureNode(i+1+startIndex, 1.0);
						result.add(newNode);
					}
				}
				
			}
		}
		else{
			double[] values=this.rightContexts_weakenTarget.get(token);
			for(int i=0;i<values.length;i++)
			{
				if(Math.abs(values[i]-0.0)>0.00001)
				{
					if(values[i]>1.0)
					{
						FeatureNode newNode=new FeatureNode(i+1+distLength+startIndex, 1+Math.log(values[i]));
						result.add(newNode);
					}
					else{
						FeatureNode newNode=new FeatureNode(i+1+distLength+startIndex, 1.0);
						result.add(newNode);
					}
				}
				
			}
		}
		//normalization

		return normalization(result);
	}
	public ArrayList<FeatureNode> getMorph(String token)
	{
		int startIndex=this.countContext*(distLength*2+morphLength+shapeLength);
		ArrayList<FeatureNode> result=new ArrayList<FeatureNode>();
		int[] tmp=new int[morphLength];
		//next, transform the morph into ArrayList<FeatureNode>
		for (int i = 0; i < token.length(); i++) {
			String suffix = token.substring(i);
			if (morphFeature.getFeatureIndex().containsKey(suffix)) 
			{
				int index = morphFeature.getFeatureIndex().get(suffix);//index might be 0
				tmp[index]=1;
			}
		}
		for(int i=0;i<tmp.length;i++)
		{
			if(tmp[i]!=0)
			{
				FeatureNode newNode=new FeatureNode(i+1+distLength*2+startIndex, 1);
				result.add(newNode);
			}
		}
		
		return normalization(result);
	}
	public ArrayList<FeatureNode>  getShape(String token, String rawToken, int pos)
	{
		int startIndex=this.countContext*(distLength*2+morphLength+shapeLength);
		//Vector[] shapeFeature=shape.getFeatureVector(token, rawToken, pos);
		//next, convert the shapeFeature into ArrayList<FeatureNode>
		ArrayList<FeatureNode> result=new ArrayList<FeatureNode>();
		boolean known;
		if(String2Index.containsKey(token)) known=true;
		else known=false;		
		String signature =shape.getSignature(rawToken, pos, true, known);
		if (shape.getAllSignatures().containsKey(signature)) {
			FeatureNode newNode=new FeatureNode(shape.getAllSignatures().get(signature)+1+distLength*2+morphLength+startIndex, 1);
			result.add(newNode);
		}
		return normalization(result);
	}
	//compute the distfeature, suffix and shapa feature of token
	public ArrayList<FeatureNode> getFeature(String token, String rawToken, int pos)
	{
		//Feature[] result;
		//first get the disfeature
		//ArrayList<FeatureNode> leftDist=getDist(token, 1); //getDist_weakenTarget
		//ArrayList<FeatureNode> rightDist=getDist(token, 0);
		
		ArrayList<FeatureNode> leftDist=getDist_weakenTarget(token, 1); //getDist_weakenTarget
		ArrayList<FeatureNode> rightDist=getDist_weakenTarget(token, 0);
		ArrayList<FeatureNode> morph=getMorph(token);
		ArrayList<FeatureNode> shapeFeature=getShape(token, rawToken, pos); //pos is used to tell if the word is a first word in a sentence
		//concatenateFeatures();
		leftDist.addAll(rightDist);     rightDist.clear();
		leftDist.addAll(morph);         morph.clear();
		leftDist.addAll(shapeFeature);  shapeFeature.clear();
		return leftDist;
	}
	
	//output the features of whole window with middle word in position
	public ArrayList<FeatureNode> combineContext(Sentence currentSentence, int position)
	{
		ArrayList<FeatureNode> result=new ArrayList<FeatureNode>();
		int countContext=0;
		for(int i=position-window/2;i<=position+window/2;i++)
		{
			String token, rawToken;
			if(i<0||i>currentSentence.size()-1)
			{
				token="<BOUNDARY>";
				rawToken="<BOUNDARY>";
			}
			else{
				token=currentSentence.getToken(i);
				rawToken=currentSentence.getRawToken(i);
			}
			//compute the distfeature, suffix and shapa feature of token
			this.countContext=countContext;
			ArrayList<FeatureNode> contextFeature=getFeature(token, rawToken, i);
			result.addAll(contextFeature);
			contextFeature.clear();
			countContext++;
		}
		return result;
	}
	public void updateTable(Map<String, int[]> matrix, String row, String col)
	{
		if(update)
		{
			int featureIndex;
			if(!wordToFeatureIndex.containsKey(col))  // a new context word
			{
				featureIndex=500;
			}
			else featureIndex=wordToFeatureIndex.get(col);
			if(!matrix.containsKey(row)) // this is the first occurrence of an unknown word
			{
				int[] list=new int[this.distFeature.getFeatureNames().size()];
				list[featureIndex]=1;
				matrix.put(row, list); // add a new row for the matrix
			}		
			// if this is a known word or is the repeated occurrence of an unknown word
			else  matrix.get(row)[featureIndex]+=1;	
		}
		// not update
		else{
			if(!matrix.containsKey(row))// is the first occurrence of an unknown word
			{
				int[] list=new int[this.distFeature.getFeatureNames().size()];
				list[500]=1;
				matrix.put(row, list);
			}
		}		
	}
	
	public void updateTable_WeakenTarget(Map<String, double[]> matrix, String row, String col)
	{
		int trainingLength=989860;
		//int trainingLength=5000;
		int testLength=26925;
		//int testlength=731678;
		if(update)
		{
			int featureIndex;
			if(!wordToFeatureIndex.containsKey(col))  // a new context word
			{
				featureIndex=500;
			}
			else featureIndex=wordToFeatureIndex.get(col);
			if(!matrix.containsKey(row)) // this is the first occurrence of an unknown word
			{
				double[] list=new double[this.distFeature.getFeatureNames().size()];
				//list[featureIndex]=2000;
				list[featureIndex]=1.0;
				matrix.put(row, list); // add a new row for the matrix
			}	
			else{
				if (word2TagList.containsKey(row))// update slightly for known words
					//matrix.get(row)[featureIndex]+=5.0;	
					matrix.get(row)[featureIndex]+=1.0;	
				else // update heavily for OOV
					//matrix.get(row)[featureIndex]+=2000;
					matrix.get(row)[featureIndex]+=1.0;
			}
		}
		// not update
		else{
			if(!matrix.containsKey(row))// is the first occurrence of an unknown word
			{
				double[] list=new double[this.distFeature.getFeatureNames().size()];
				//list[500]=(trainingLength*1.0)/testlength;
				list[500]=1.0;
				matrix.put(row, list);
			}
		}		
	}
	
	public void LiblinearPredictUnlabeledData(Feature[] values, String token) throws IOException
	{
		double label=Linear.predict(this.model, values);
		tagedFile.write(token+"\t"+id2Tag.get((int)label)+"\n");
	}
	public void loadTagList() throws IOException
	{// note that we only consider the lowercase and digit replaced word
		
		BufferedReader br = new BufferedReader(new FileReader(Entrance.file_prefix_store+"tagList.txt"));
		String currentLine, deli=" ";
		//FileWriter featureWrite=new FileWriter("WordFeatureOnTest.txt");
		while ((currentLine = br.readLine()) != null) 
		{
			String[] tokens=currentLine.split(deli);
			ArrayList<String> tagList=new ArrayList<String>();
			for(int i=1; i<tokens.length;i++)
			{
				tagList.add(tokens[i]);
			}
			word2TagList.put(tokens[0], tagList);
		}
		br.close();
		System.out.println("Tag list has been read sucessfully.");
	}
	public String LiblinearPredictLabeledData(Feature[] values, String rawToken, String token, String goldTag) throws IOException
	{
		//System.out.print(token+"\n");
		/*
		double label=Linear.predict(this.model, values);
		int known;
		if(knownWords.contains(token)) known=1;
		else {
			unknownWords++;
			known=0;
		}
		
		if(goldTag.equals(id2Tag.get((int)label)))
			tagedFile.write(rawToken+"\t"+known+"\t"+goldTag+"\t"+id2Tag.get((int)label)+"\n");
		else
			tagedFile.write(rawToken+"\t"+known+"\t"+goldTag+"\t"+id2Tag.get((int)label)+"\t\twrong"+"\n");
		return id2Tag.get((int)label);
		*/
		// in tag list situation
		
		double label=Linear.predict(this.model, values);
		int known;
		String tagFlag;
		if(word2TagList.containsKey(token)) {
			known=1;// is a known word
			if(!word2TagList.get(token).contains(goldTag))
				tagFlag="u"; // unknown tag
			else tagFlag="k";  // known tag
		}
		else {
			unknownWords.add(token);
			known=0;
			tagFlag="0"; // 0 means this is a unknown word, need not consider its tag
		}
		// following code are for tag list cases
		
		if(goldTag.equals(id2Tag.get((int)label)))
			tagedFile.write(rawToken+"\t"+known+"\t"+goldTag+"\t"+id2Tag.get((int)label)+"\t"+tagFlag+"\n");
		else
			tagedFile.write(rawToken+"\t"+known+"\t"+goldTag+"\t"+id2Tag.get((int)label)+"\t"+tagFlag+"\t"+"wrong"+"\n");
		
		// following code are for update 100
		/*
		if(goldTag.equals(id2Tag.get((int)label)))
			tagedFile.write(rawToken+"\t"+known+"\t"+goldTag+"\t"+id2Tag.get((int)label)+"\n");
		else
			tagedFile.write(rawToken+"\t"+known+"\t"+goldTag+"\t"+id2Tag.get((int)label)+"\t"+"wrong"+"\n");
		*/
		// following code should be for the "noupdate" case
		/*
		if(goldTag.equals(id2Tag.get((int)label)))
			tagedFile.write(rawToken+"\t"+known+"\t"+goldTag+"\t"+id2Tag.get((int)label)+"\n");
		else
			tagedFile.write(rawToken+"\t"+known+"\t"+goldTag+"\t"+id2Tag.get((int)label)+"\twrong"+"\n");
		*/
		return id2Tag.get((int)label);
	}
	public void updateBigram(Sentence currentSentence)
	{
		String token;
		for(int i=-1; i<=currentSentence.size();i++)
		{
			// first get the current word
			if(i==-1||i==currentSentence.size())
			{
				token ="<BOUNDARY>";
			}
			else{
				token = currentSentence.getToken(i);
				
			}
			//update left context
			if (i==0){
				updateTable(leftContexts, token, "<BOUNDARY>");
			}
			if (i > 0) {
				updateTable(leftContexts, token, currentSentence.getToken(i-1));
			}
			//update right context
			if (i < currentSentence.size()-1) {
				updateTable(rightContexts, token, currentSentence.getToken(i+1));
			}
			if (i == currentSentence.size()-1){
				updateTable(rightContexts, token, "<BOUNDARY>");
			}
		}
	}//updateTable_WeakenTarget
	public void updateBigram_weakenTarget(Sentence currentSentence)
	{
		String token;
		for(int i=-1; i<=currentSentence.size();i++)
		{
			// first get the current word
			if(i==-1||i==currentSentence.size())
			{
				token ="<BOUNDARY>";
			}
			else{
				token = currentSentence.getToken(i);
				
			}
			//update left context
			if (i==0){
				updateTable_WeakenTarget(this.leftContexts_weakenTarget, token, "<BOUNDARY>");
			}
			if (i > 0) {
				updateTable_WeakenTarget(leftContexts_weakenTarget, token, currentSentence.getToken(i-1));
			}
			//update right context
			if (i < currentSentence.size()-1) {
				updateTable_WeakenTarget(this.rightContexts_weakenTarget, token, currentSentence.getToken(i+1));
			}
			if (i == currentSentence.size()-1){
				updateTable_WeakenTarget(this.rightContexts_weakenTarget, token, "<BOUNDARY>");
			}
		}
	}//updateTable_WeakenTarget
	
	public void predictUnlabeledData(String fileName) throws IOException
	{
		this.fileName=fileName;
		String delimiter=" ";
		BufferedReader br = new BufferedReader(new FileReader(this.fileName));
		String currentLine;
		//FileWriter featureWrite=new FileWriter("WordFeatureOnTest.txt");
		while ((currentLine = br.readLine()) != null) 
		{
			if (!currentLine.trim().isEmpty()) 
			{
				//System.out.print(line++);
				ArrayList<String> words = new ArrayList<String>(Arrays.asList(currentLine.split(delimiter)));
				Sentence currentSentence = new Sentence();
				//because of unlabelddata, only set the tokens, not tags
				currentSentence.setTokens(words);
				currentSentence.setRawTokens(new ArrayList<String>(words));
				//if (Parser parser.replaceDigits)
				currentSentence.replaceDigits();
				//if (ignoreCapitalization)
				currentSentence.convertToLowerCase();
				//update the bigram
				updateBigram(currentSentence);
				//compute the feature
				
				for(int i=0;i<currentSentence.size();i++)
				{
					//induce its feature
					ArrayList<FeatureNode> features=combineContext(currentSentence, i);
					//next, convert the ArrayList<FeatureNode> into Feature[] for liblinear prediction
					Feature[] values;
					values=(Feature[])features.toArray(new Feature[features.size()]);	
					//predict its label
					LiblinearPredictUnlabeledData(values, currentSentence.getRawToken(i));
					/*
					featureWrite.write(currentSentence.getRawToken(i)+" ");
					for(Feature node:values)
					{
						featureWrite.write(node.getIndex()+"|"+node.getValue()+" ");
					}
					featureWrite.write("\n");
					*/
				}
			}		
			tagedFile.write("\n");
		}
		br.close();
		//featureWrite.close();
		tagedFile.close();
		System.out.println("finished\n");
	}
	public int countLines(String fileName) throws IOException
	{
		String currentLine;
		int i=0;
		BufferedReader br = new BufferedReader(new FileReader(fileName));
		while ((currentLine = br.readLine()) != null) i++;
		br.close();
		return i;

	}
	public void printProgress(int line_tmp, int totalLine)
	{

			   
			    double i = (line_tmp*100.0)/totalLine;
			    System.out.printf("%.1f%s", i, "%");
			     //System.out.print(i + "%");
			     if(i<1.0){
			      System.out.print("\b");
			      System.out.print("\b");
			      System.out.print("\b");
			      System.out.print("\b");
			     }else if(i<99.0){
			      System.out.print("\b");
			      System.out.print("\b");
			      System.out.print("\b");
			      System.out.print("\b");
			      System.out.print("\b");
			     }
			     else{
			    	 System.out.print("\b");
				      System.out.print("\b");
				      
				      System.out.print("\b");
				      System.out.print("\b");
				      System.out.print("\b");
				      System.out.print("\b");
				      System.out.print("\b");
				      
			     }
			     //Thread.sleep(100);
			    // i++;

	}
	public String[] replaceIfNull(String[] input) {
		if (input == null) {
			return new String[0];
		} else {
			return input;
		}
	}
	public String formWriteFileName(String fileName)
	{
		int position=fileName.lastIndexOf("/");
		if(position==-1) return fileName+"_Tagged";
		else return fileName.substring(position+1)+"_Tagged";
		
			
	}
	public void predictLabeledData(String[] fileNames) throws IOException
	{
		int totalLines=0;
		for (String fileName : replaceIfNull(fileNames)) {
			totalLines+=countLines(fileName);
		}
		int line_tmp=0;
		int total=0, correct=0;
		FileWriter accuracy = new FileWriter("accuracies.txt");
		int countValidSentences=0;
		for (String fileName : replaceIfNull(fileNames)) {
			String writeFileName=formWriteFileName(fileName);
			tagedFile = new FileWriter(writeFileName); //注意之前这个已经被new过了,需要检查问题
			this.fileName=fileName;
			String delimiter="\t";
			
			BufferedReader br = new BufferedReader(new FileReader(this.fileName));

			String currentLine;
			Sentence currentSentence = new Sentence();
			//int totalLine=countLines(fileName);
			
			System.out.println("\npredicting...\n");
			while ((currentLine = br.readLine()) != null) {
				line_tmp++;
				//if(line_tmp<1000) continue;
				printProgress(line_tmp, totalLines);
				//double j = (line_tmp*100.0)/totalLine;
				//System.out.println("............."+line_tmp*100+"--"+totalLine+"\n");
			    //System.out.printf("%.1f%s\n", j, "%");
				if (currentLine.isEmpty()) { // meet the end of a sentence
					if (currentSentence.size() > 0) {
						countValidSentences++;
						total+=currentSentence.size();
						//if (replaceDigits)
						currentSentence.replaceDigits();
						//if (ignoreCapitalization)
						currentSentence.convertToLowerCase();
						// do something with current sentence
						updateBigram(currentSentence);
						for(int i=0;i<currentSentence.size();i++)
						{
							//induce its feature
							ArrayList<FeatureNode> features=combineContext(currentSentence, i);
							//next, convert the ArrayList<FeatureNode> into Feature[] for liblinear prediction
							Feature[] values;
							values=(Feature[])features.toArray(new Feature[features.size()]);	
							//predict its label
							String predictedLabel=LiblinearPredictLabeledData(values, currentSentence.getRawToken(i), currentSentence.getToken(i),currentSentence.getTag(i));
							if(predictedLabel.equals(currentSentence.getTag(i))) correct+=1;
						}
						double accu_tmp=correct*1.0/total;
						accuracy.write(countValidSentences+"\t"+accu_tmp+"\n");
						tagedFile.write("\n");
						currentSentence = new Sentence();
					}
				} else {
					String[] splitted = currentLine.split(delimiter);
					if (splitted.length != 2) {
						br.close();
						//throw new IOException("Malformed line: " + currentLine);
						System.out.println("Error: you are predicting for a tagged corpus, please make sure that each line contains a token and a tag separated by tab space.");
						System.exit(0);
					} else {
						String token = splitted[0];
						String tag = splitted[1];
						currentSentence.add(token, tag);
						currentSentence.addRawToken(token);
					}
				}
			}
			br.close();
			tagedFile.close();
		}
		accuracy.close();
		
		//compute the final accuracy
		double accu=correct*1.0/total;
		System.out.println("\n\nfinished, the accuracy is: "+accu+"\n");
	}
	public int[] randomArray(int min,int max,int n){  
	    int len = max-min+1;  
	      
	    if(max < min || n > len){  
	        return null;  
	    }  
	      
	    //初始化给定范围的待选数组  
	    int[] source = new int[len];  
	       for (int i = min; i < min+len; i++){  
	        source[i-min] = i;  
	       }  
	         
	       int[] result = new int[n];  
	       Random rd = new Random();  
	       int index = 0;  
	       for (int i = 0; i < result.length; i++) {  
	        //待选数组0到(len-2)随机一个下标  
	           index = Math.abs(rd.nextInt() % len--);  
	           //将随机到的数放入结果集  
	           result[i] = source[index];  
	           //将待选数组中被随机到的数，用待选数组(len-1)下标对应的数替换  
	           source[index] = source[len];  
	       }  
	       return result;  
	}  
	public double[] randomPredictLabeledData(String[] fileNames) throws IOException
	{
		
		LabeledDataset Data=new LabeledDataset();
		Data.parse(fileNames); // note whether the parse has been changed
		tagedFile = new FileWriter(Entrance.output_file);
		//tagedFile = new FileWriter("//mounts/data/proj/wenpeng/public/trainOnReviews/noUpdate_5k.txt");
		double[] accu=new double[Data.getCorpus().size()];

			int countValidSentences=0;
			int total_OOV=0, correct_OOV=0;
			int total=0, correct=0;
			//int[] order=randomArray(0,Data.getCorpus().size()-1,Data.getCorpus().size());
			//int cut=0;
			System.out.println("Online tagging:");
			for(int curr=0;curr<Data.getCorpus().size();curr++) // for prediction sequentially
			//for(int curr: order) // for random prediction
			{
				//if(cut++>2000) break;
				Sentence currentSentence=Data.getCorpus().get(curr);// repete the first sentence
				countValidSentences++;
				//int correct=0;
				printProgress(countValidSentences, Data.getCorpus().size());
				total+=currentSentence.size();
				//if (replaceDigits)
				currentSentence.replaceDigits();
				//if (ignoreCapitalization)
				currentSentence.convertToLowerCase();
				// do something with current sentence
				//updateBigram(currentSentence);
				updateBigram_weakenTarget(currentSentence);
				for(int i=0;i<currentSentence.size();i++)
				{
					//induce its feature
					ArrayList<FeatureNode> features=combineContext(currentSentence, i);
					//next, convert the ArrayList<FeatureNode> into Feature[] for liblinear prediction
					Feature[] values;
					values=(Feature[])features.toArray(new Feature[features.size()]);	
					//predict its label
					String predictedLabel=LiblinearPredictLabeledData(values, currentSentence.getRawToken(i), currentSentence.getToken(i),currentSentence.getTag(i));
					if(predictedLabel.equals(currentSentence.getTag(i))) correct++;
					if(!knownWords.contains(currentSentence.getToken(i)))//if(word2TagList.containsKey(token))
					{
						total_OOV++;
						if(predictedLabel.equals(currentSentence.getTag(i))) correct_OOV++;
					}
				}
				//double accu_tmp=correct*1.0/total;
				accu[countValidSentences-1]=0; // now, it stored the accumulative accuracy
				tagedFile.write("\n"); // add an empty line after a sentence
			}
			tagedFile.close();
			System.out.println("unknown words:"+unknownWords.size()+", and OOV acc: "+(correct_OOV*1.0/total_OOV)+" overall acc: "+(correct*1.0/total));
		return accu;
	}
	public void countUnknownWords(LabeledDataset Data, Map<String,ArrayList<String>> word2TagList)
	{
		int count=0;
		for(String word:Data.getVocabulary())
		{
			if(!word2TagList.containsKey(word)) count++;
		}
		System.out.println("unknown words:"+ count+" total words:"+Data.getVocabularySize());
	}
	
	public  double[] batchMode(String[] fileNames) throws IOException
	{
		LabeledDataset Data=new LabeledDataset();
		Data.parse(fileNames); // note whether the parse has been changed
		//countUnknownWords(Data, word2TagList);
		//System.exit(0);
		//tagedFile = new FileWriter("//mounts/data/proj/wenpeng/public/taggedFiles_withBigData/batchMode/taggedFile.txt");
		//tagedFile = new FileWriter("//mounts/data/proj/wenpeng/public/trainOnReviews/batchUpdate_5k.txt");
		tagedFile = new FileWriter(Entrance.output_file);
		//tagedFile = new FileWriter("//mounts/data/proj/wenpeng/public/weakenTarget_Batch100/batch_5kWithBigdata.txt");
		//tagedFile = new FileWriter("//mounts/data/proj/wenpeng/public/weakenTarget_Batch100/batch_SANCLNoBigdata.txt");
		//tagedFile = new FileWriter("//mounts/data/proj/wenpeng/public/weakenTarget_Batch100/batch_SANCLWithBigdata.txt");
		
			int countValidSentences=0;
			System.out.println("Batch updating:");
			for(int curr=0;curr<Data.getCorpus().size();curr++)
			{
				//if(cut++>2000) break;
				Sentence currentSentence=Data.getCorpus().get(curr);// repete the first sentence
				countValidSentences++;
				int correct=0;
				printProgress(countValidSentences, Data.getCorpus().size());
				//int total=currentSentence.size();
				//if (replaceDigits)
				currentSentence.replaceDigits();
				//if (ignoreCapitalization)
				currentSentence.convertToLowerCase();
				// do something with current sentence
				//updateBigram(currentSentence);
				updateBigram_weakenTarget(currentSentence);
			}
			// next, compute feature vectors
			System.out.println("POS tagging:");
			countValidSentences=0;
			double[] accu=new double[Data.getCorpus().size()];
			int correct=0, total=0;
			int correct_OOV=0, total_OOV=0;
			for(int curr=0;curr<Data.getCorpus().size();curr++)
			{
				//if(cut++>2000) break;
				Sentence currentSentence=Data.getCorpus().get(curr);// repete the first sentence
				countValidSentences++;
				printProgress(countValidSentences, Data.getCorpus().size());
				total+=currentSentence.size();
				//if (replaceDigits)
				currentSentence.replaceDigits();
				//if (ignoreCapitalization)
				currentSentence.convertToLowerCase();
				// start to concatenate feature vectors and predict			
				for(int i=0;i<currentSentence.size();i++)
				{
					//induce its feature
					ArrayList<FeatureNode> features=combineContext(currentSentence, i);
					//next, convert the ArrayList<FeatureNode> into Feature[] for liblinear prediction
					Feature[] values;
					values=(Feature[])features.toArray(new Feature[features.size()]);	
					//predict its label
					String predictedLabel=LiblinearPredictLabeledData(values, currentSentence.getRawToken(i), currentSentence.getToken(i),currentSentence.getTag(i));
					if(!knownWords.contains(currentSentence.getToken(i)))
					{
						total_OOV++;
					}
					if(predictedLabel.equals(currentSentence.getTag(i))) {
						correct++;
						if(!knownWords.contains(currentSentence.getToken(i)))
						{
							correct_OOV++;
						}
					}
					
				}
				//double accu_tmp=correct*1.0/total;
				accu[countValidSentences-1]=0; // the accumulative accuracy 
				tagedFile.write("\n"); // add an empty line after a sentence
				
			}
			tagedFile.close();
			System.out.println("ALL acc: "+(correct*1.0/total)+" OOV acc: "+(correct_OOV*1.0/total_OOV)+" total_OOV: "+total_OOV);
			//System.out.println("unknown words:"+unknownWords.size()+" total words:"+Data.getVocabularySize()+", and accuracy of unregistered is "+(correct*1.0/total));
		return accu;
	}
	
	public  double[] loopBatchMode(String[] fileNames) throws IOException
	{
		LabeledDataset Data=new LabeledDataset();
		Data.parse(fileNames); 

		//tagedFile = new FileWriter("//mounts/data/proj/wenpeng/public/batchLoop/weakenTargetDomain/SANCL_bigdata/"+Entrance.loopTimes+"_taggedFile.txt");
		tagedFile = new FileWriter("//mounts/data/proj/wenpeng/public/test.txt");
		int[] order=randomArray(0,Data.getCorpus().size()-1,Data.getCorpus().size());
		int usedSentences=order.length/2;    // it seems only used half of sentences to do batchUpdating
			int countValidSentences=0;
			System.out.println("Batch updating:");
			for(int curr=0;curr<usedSentences;curr++)
			{
				Sentence currentSentence=Data.getCorpus().get(order[curr]);// repeat the first sentence
				countValidSentences++;
				printProgress(countValidSentences, usedSentences);
				currentSentence.replaceDigits();
				//if (ignoreCapitalization)
				currentSentence.convertToLowerCase();
				// do something with current sentence
				//updateBigram(currentSentence);
				updateBigram_weakenTarget(currentSentence);
			}
			// next, compute feature vectors
			System.out.println("POS tagging:");
			countValidSentences=0;
			double[] accu=new double[Data.getCorpus().size()];
			int correct=0, total=0;
			for(int curr=0;curr<usedSentences;curr++)
			{
				Sentence currentSentence=Data.getCorpus().get(order[curr]);// repete the first sentence
				countValidSentences++;
				printProgress(countValidSentences, usedSentences);
				total+=currentSentence.size();
				//if (replaceDigits)
				currentSentence.replaceDigits();
				//if (ignoreCapitalization)
				currentSentence.convertToLowerCase();
				// start to concatenate feature vectors and predict			
				for(int i=0;i<currentSentence.size();i++)
				{
					//induce its feature
					ArrayList<FeatureNode> features=combineContext(currentSentence, i);
					//next, convert the ArrayList<FeatureNode> into Feature[] for liblinear prediction
					Feature[] values;
					values=(Feature[])features.toArray(new Feature[features.size()]);	
					//predict its label
					String predictedLabel=LiblinearPredictLabeledData(values, currentSentence.getRawToken(i), currentSentence.getToken(i),currentSentence.getTag(i));

					if(predictedLabel.equals(currentSentence.getTag(i))) correct++;

				}
				//double accu_tmp=correct*1.0/total;
				accu[countValidSentences-1]=0; // the accumulative accuracy 
				tagedFile.write("\n"); // add an empty line after a sentence
				
			}
			tagedFile.close();
			System.out.println("Accuracy is "+(correct*1.0/total));
		return accu;
	}
	
	public  double[] FLORS_predict(HashMap<String, String[]> parameters) throws IOException 
	{
		if(parameters.get("-update")[0].equals("0")) update=false;
		
		if(parameters.get("-labeled")[0].equals("1"))
		{
			//predictLabeledData(parameters.get("-predictFile")); //seems not useful again
			loadTagList();
			//return randomPredictLabeledData(parameters.get("-predictFile"));//include online and random online
			return batchMode(parameters.get("-predictFile")); //it includes batch and noDA, controled by para -update
			//return loopBatchMode(parameters.get("-predictFile"));
		}
		else 
		{
			predictUnlabeledData(parameters.get("-predictFile")[0]);
			double[] result=new double[0];
			return result;
		}
		//newCorpus corpus=new newCorpus();
		//corpus.predictUnlabeledData("corpus.txt");
		////mounts/Users/student/wenpeng/datasets/Google Task/target/emails/gweb-emails-dev
		//corpus.predictLabeledData("//mounts/Users/student/wenpeng/datasets/Choi & Palmer/converted datasets/msnbc.converted");
		
	}
}
