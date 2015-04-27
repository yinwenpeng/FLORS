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
public class Irina2 extends Dataset{
	Model model;
	DistFeatures distFeature;
	MorphFeatures morphFeature;
	WordSignatures shape;
	Map<Integer, String> id2Tag;
	public static final int MAX_WORDS = 1000000;
	public String fileName;  // the file name of new corpus
	public String featureFile;
	public FileWriter fw;  // store the predicted "word-> tag"
	public int distLength, morphLength, shapeLength, countContext;
	public boolean update=true;
	LinkedHashMap<String, Integer> wordToFeatureIndex; // find the index of feature name given string
	HashMap<String, Integer>String2Index; // used to judge unknown word
	public int window=1;
	//String pwd="";//need modification

	Map<String, int[]>leftContexts;
	Map<String, int[]>rightContexts;
	Set<String>modeWords;
	
	Irina2() throws IOException
	{
		this.featureFile="standardFeatureName.txt";
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
		fw = new FileWriter("taggedFile.txt");
		//fw = new FileWriter(this.pwd+"/testTaggedFile.txt");
		//wordToId = new LinkedHashMap<String, Integer>();
		File modelFile=new File("standardModel.txt");
		//File modelFile=new File(this.pwd+"/testModel.txt");
		this.model=Model.load(modelFile);
		
		loadMatrix();  // load left and right bigrams
		loadIndexMap();  //load the index transfer information 
		//System.out.println("load index map completed!\n");
	}
	public void loadIndexMap() throws IOException
	{
		wordToFeatureIndex = new LinkedHashMap<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader("wordToRowCol.txt"));
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
		BufferedReader br = new BufferedReader(new FileReader("leftVector.txt"));
		String currentLine;
		String deli=" ";
		while ((currentLine = br.readLine()) != null) 
		{
			String[] tokens=currentLine.split(deli);
			if(!leftContexts.containsKey(tokens[0]))
			{
				int[] list=new int[this.distFeature.getFeatureNames().size()];
				for(int i=1;i<tokens.length;i++)
				{
					int position=tokens[i].indexOf("-");
					int index= Integer.parseInt(tokens[i].substring(0,position));
					int value= Double.valueOf(tokens[i].substring(position+1)).intValue();
					//note that the index of "other content" is 500
					list[index]=value;
							
				}
				leftContexts.put(tokens[0], list);
			}
		}
		br.close();
		modeWords=leftContexts.keySet();// remember which words are stored for the training data
		//System.out.println("load left Bigram completed!\n");
		//load the right context matrix
		rightContexts=new HashMap<String, int[]>();
		BufferedReader br1 = new BufferedReader(new FileReader("rightVector.txt"));
		String Line;
		while ((Line = br1.readLine()) != null) 
		{
			String[] tokens=Line.split(deli);
			if(!rightContexts.containsKey(tokens[0]))
			{
				int[] list=new int[this.distFeature.getFeatureNames().size()];
				for(int i=1;i<tokens.length;i++)
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
			sumTmp+=Math.pow(1+Math.log(node.getValue()), 2);
		sumTmp=Math.sqrt(sumTmp);
		double newValue;
		for(FeatureNode node: list)
		{
			newValue=1+Math.log(node.getValue());
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
			if(!leftContexts.containsKey(token))// is a unknown word
				System.out.println(token);
			int[] values=leftContexts.get(token);
			for(int i=0;i<values.length;i++)
			{
				if(values[i]!=0)
				{
					FeatureNode newNode=new FeatureNode(i+1+startIndex, values[i]);
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
					FeatureNode newNode=new FeatureNode(i+1+distLength+startIndex, values[i]);
					result.add(newNode);
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
		ArrayList<FeatureNode> leftDist=getDist(token, 1);
		ArrayList<FeatureNode> rightDist=getDist(token, 0);
		ArrayList<FeatureNode> morph=getMorph(token);
		ArrayList<FeatureNode> shapeFeature=getShape(token, rawToken, pos);
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
		int featureIndex;
		if(!wordToFeatureIndex.containsKey(col))  // a new context word
		{
			featureIndex=500;
		}
		else featureIndex=wordToFeatureIndex.get(col);

		if(!matrix.containsKey(row)) // this is a unknown word
		{
			int[] list=new int[this.distFeature.getFeatureNames().size()];
			list[featureIndex]=1;
			matrix.put(row, list);
		}		
		// is a registered word
		else if(update==true||(update==false&&!modeWords.contains(row))){matrix.get(row)[featureIndex]+=1;}	
	
		
			
	}
	public void LiblinearPredictUnlabeledData(Feature[] values, String token) throws IOException
	{
		double label=Linear.predict(this.model, values);
		fw.write(token+"\t"+id2Tag.get((int)label)+"\n");
	}
	public String LiblinearPredictLabeledData(Feature[] values, String token) throws IOException
	{
		//System.out.print(model.);
		double label=Linear.predict(this.model, values);
		fw.write(token+"\t"+id2Tag.get((int)label)+"\n");
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
	}
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
			fw.write("\n");
		}
		br.close();
		//featureWrite.close();
		fw.close();
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
			fw = new FileWriter(writeFileName);
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
							String predictedLabel=LiblinearPredictLabeledData(values, currentSentence.getRawToken(i));
							if(predictedLabel.equals(currentSentence.getTag(i))) correct+=1;
						}
						double accu_tmp=correct*1.0/total;
						accuracy.write(countValidSentences+"\t"+accu_tmp+"\n");
						fw.write("\n");
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
			fw.close();
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
		Data.parse(fileNames);
		FileWriter featureFile=new FileWriter("featureValues.txt");
		double[] accu=new double[Data.getCorpus().size()];

			int countValidSentences=0;
			//int total=0, correct=0;
			//int[] order=randomArray(0,Data.getCorpus().size()-1,Data.getCorpus().size());
			//int cut=0;
			for(int curr=0;curr<Data.getCorpus().size();curr++)
			//for(int curr: order)
			{
				//if(cut++>2000) break;
				Sentence currentSentence=Data.getCorpus().get(curr);// repete the first sentence
				countValidSentences++;
				int correct=0;
				printProgress(countValidSentences, Data.getCorpus().size());
				int total=currentSentence.size();
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
					// write the feature into file
					featureFile.write(currentSentence.getRawToken(i)+" ");
					for(FeatureNode node: features)
						featureFile.write(node.getIndex()+" "+node.getValue()+" ");
					featureFile.write("\n");
					/*
					//next, convert the ArrayList<FeatureNode> into Feature[] for liblinear prediction
					Feature[] values;
					values=(Feature[])features.toArray(new Feature[features.size()]);	
					//predict its label
					String predictedLabel=LiblinearPredictLabeledData(values, currentSentence.getRawToken(i));
					if(predictedLabel.equals(currentSentence.getTag(i))) correct+=1;
					*/
				}
				/*
				double accu_tmp=correct*1.0/total;
				accu[countValidSentences-1]=accu_tmp; //
				*/
				//fw.write("\n");
			}
			featureFile.close();
		return accu;
	}
	
	public  double[] FLORS_predict(HashMap<String, String[]> parameters) throws IOException 
	{
		if(parameters.get("-update")[0].equals("0")) update=false;
		
		if(parameters.get("-labeled")[0].equals("1"))
		{
			//predictLabeledData(parameters.get("-predictFile"));
			return randomPredictLabeledData(parameters.get("-predictFile"));
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
