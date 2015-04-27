package mypack;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import de.bwaldvogel.liblinear.Feature;
import de.bwaldvogel.liblinear.FeatureNode;
import de.bwaldvogel.liblinear.Linear;
import de.bwaldvogel.liblinear.Model;
import de.bwaldvogel.liblinear.Parameter;
import de.bwaldvogel.liblinear.Problem;
import de.bwaldvogel.liblinear.SolverType;
import nlp.model.LabeledDataset;
import nlp.model.Sentence;
import nlp.model.ThinDataset;
import nlp.model.UnlabeledDataset;
import nlp.processing.Features;
import nlp.processing.NgramGenerator;
import nlp.processing.features.DistFeatures;
import nlp.processing.features.MatlabSparse;
import nlp.processing.features.MorphFeatures;
import nlp.processing.features.WordSignatures;
// command: java -Xmx200G -Xms200G -XX:-UseGCOverheadLimit -jar myJar.jar 
//java -Xmx300G -Xms300G -XX:-UseGCOverheadLimit -jar predictLabeledData.jar -mode train -trainFile /mounts/Users/student/wenpeng/datasets/GoogleTask/target/answers/gweb-answers-dev /mounts/Users/student/wenpeng/datasets/GoogleTask/target/emails/gweb-emails-dev /mounts/Users/student/wenpeng/datasets/GoogleTask/target/newsgroups/gweb-newsgroups-dev /mounts/Users/student/wenpeng/datasets/GoogleTask/target/reviews/gweb-reviews-dev /mounts/Users/student/wenpeng/datasets/GoogleTask/target/weblogs/gweb-weblogs-dev -unlabeledData /mounts/Users/student/wenpeng/datasets/GoogleTask/target/answers/gweb-answers.unlabeled /mounts/Users/student/wenpeng/datasets/GoogleTask/target/emails/gweb-emails.unlabeled /mounts/Users/student/wenpeng/datasets/GoogleTask/target/newsgroups/gweb-newsgroups.unlabeled /mounts/Users/student/wenpeng/datasets/GoogleTask/target/reviews/gweb-reviews.unlabeled /mounts/Users/student/wenpeng/datasets/GoogleTask/target/weblogs/gweb-weblogs.unlabeled -bigData /mounts/data/proj/wenpeng/MC/firstCorpus.txt
//this run.java means the file:testTagger.m 
public class run{
	//main() is the testTagger.m

	
	/*
	public static Feature[][] geneRequiredFeatureFormat(returnOfTagging returns, int no_of_ngrams)
	{
		Feature[][] requiredFeatures=returns.features.values;
		return requiredFeatures;
	}
	*/
	public  static Model svmTraining(int no_of_ngrams, int no_of_feature, Feature[][] features, double[] labels)throws IOException
	{
		
		
		Problem problem=new Problem();
		problem.l=no_of_ngrams;
		problem.n=no_of_feature;
		problem.x=features;
		problem.y=labels;  //double[]		
		//SolverType solver=SolverType.L2R_LR;
		SolverType solver=SolverType.L2R_L2LOSS_SVC_DUAL;
		double C=1.0;
		double eps=0.1;
		
		Parameter parameter=new Parameter(solver, C, eps);
		Model model=Linear.train(problem, parameter);
		//store the training result into the file "model"
		//File modelFile=new File("model");
		//model.save(modelFile);
		// load model or use it directly
		//model=Model.load(modelFile);
		
		
		//double accu_training=predict(return_training, model);
		
		return model;
	}
	
	public static resultOfPredict predict(Feature[][] dataFeatures, Model model, int no_of_ngrams, double[] gold_labels)
	{
		resultOfPredict result=new resultOfPredict();
		result.labels=new double[no_of_ngrams];
		int correct=0;
		for(int i=0;i<no_of_ngrams;i++)
		{		
			try {
				result.labels[i]=Linear.predict(model, dataFeatures[i]);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(result.labels[i]==gold_labels[i])
			{
				correct++;
			}
		}
		result.accu=correct*1.0/no_of_ngrams;
		return result;
	}
	public static void saveModelAndFeatureNames(Model model, Tagger myTagger, LabeledDataset trainingData) throws IOException
	{
		File modelFile=new File(Entrance.file_prefix_store+"standardModel.txt");
		model.save(modelFile);

		//save feature names, and feature values
		for(Features feat:myTagger.wordFeatures)
		{
			feat.saveFeatureName();
			feat.writeFeatures(trainingData);// actually, only write the dist feature
		}
		//save id2tag
		
		FileWriter fw = new FileWriter(Entrance.file_prefix_store+Features.saveFile, true);//true means append
		for (Map.Entry<Integer, String> entry : myTagger.getLabelsToTagMap().entrySet()) {  
            int key = entry.getKey();  
            String value = entry.getValue();
            fw.write(key+"_"+value+" ");	
        } 
		fw.write("\n");
		fw.close();
		
	}
	public static void saveWordFeature(returnOfTaggingTrainingData return_training) throws IOException
	{
		FileWriter fw = new FileWriter("testWordFeature.txt");
		int i=0;
		for(String token:return_training.tokens)
		{
			fw.write(token+" ");
			for(Feature node:return_training.features.values[i])
			{
				fw.write(node.getIndex()+"|"+node.getValue()+" ");
			}
			fw.write("\n");
			i++;
		}
		fw.close();
	}
	public void saveTagList(LabeledDataset trainingData) throws IOException
	{// note that we only consider the lowercase and digit replaced word
		Map<String,ArrayList<String>> word2TagList=new HashMap<String,ArrayList<String>>();
		FileWriter tagList = new FileWriter(Entrance.file_prefix_store+"tagList.txt");
		
		for(Sentence sentence:trainingData.getCorpus())
		{
			for(int i=0;i<sentence.size();i++)
			{
				//note that we store token, not rawToken, which will reduce vocab dramatically
				if(!word2TagList.containsKey(sentence.getToken(i))) // if this word has not been registered
				{
					ArrayList<String> newWordTag=new ArrayList<String>();
					newWordTag.add(sentence.getTag(i));
					word2TagList.put(sentence.getToken(i), newWordTag);
				}
				else if(!word2TagList.get(sentence.getToken(i)).contains(sentence.getTag(i))) // this is a registered word with unregistered tag
				{
					word2TagList.get(sentence.getToken(i)).add(sentence.getTag(i));
				}
			}			
		}
		// write into file
		for(String word:word2TagList.keySet())
		{
			tagList.write(word);
			for(String tag:word2TagList.get(word))
			{
				tagList.write(" "+tag);
			}
			tagList.write("\n");
		}
		tagList.close();
		System.out.println("Tag list written over. Vocab Tot: "+word2TagList.size());
	}
	public  void FLORS_train(HashMap<String, String[]> parameters) throws IOException {

		//String pwd="//mounts/Users/student/wenpeng";
		//String targetDomain="emails";
		String[] trainingFile=parameters.get("-trainFile");
		
		//trainingFile[0]=pwd+"/datasets/Google Task/source/ontonotes-wsj-train";
		//trainingFile[0]=pwd+"/datasets/onco_train.500";
		
		//String[] testFile=new String[1]; 
		//testFile[0]=pwd+"/datasets/Google Task/target/"+targetDomain+"/gweb-"+targetDomain+"-dev";		
		String[] ulTargetData=parameters.get("-unlabeledData"); 
		//ulTargetData[0]= pwd+"/datasets/Google Task/target/"+targetDomain+"/gweb-"+targetDomain+".unlabeled";		
		String[] lTargetData=parameters.get("-labeledData"); ; 
		//lTargetData[0] = pwd+ "/datasets/Google Task/target/"+targetDomain+"/gweb-"+targetDomain+"-test";		
		//String[] ulSourceData=new String[1];
		//ulSourceData[0] = pwd+"/datasets/wsj_ul.100k";
		/*
		if (targetDomain.equals("bio"))
		{
			trainingFile[0] =pwd+"/datasets/train-wsj-02-21";
			testFile[0]=pwd+"/datasets/onco_train.500";
			ulTargetData[0]=pwd+"/datasets/input/biomed_ul.100k";
			lTargetData[0]=pwd+"/datasets/input/onco_test.500";
			ulSourceData[0]=pwd+"/datasets/input/wsj_ul.100k";
		
		}
		*/
		int ngram_size=Integer.parseInt(parameters.get("-window")[0]);
		//int no_of_ngrams=1000;
		String no_of_ngrams=parameters.get("-sampleNo")[0];
		//int no_of_ngrams_test=1000;
		String no_of_ngrams_test="all";
		boolean saveToFile=false;
		
		
		LabeledDataset trainingData=new LabeledDataset();
		trainingData.parse(trainingFile);
		//  save tag list
		saveTagList(trainingData); // it has problem when "no_of_ngrams" is not "all", it will store the tags of all words

		//LabeledDataset testData=new LabeledDataset();
		//testData.parse(testFile);
		
		ThinDataset targetDomainData=new ThinDataset();
		targetDomainData.parse(lTargetData, ulTargetData); // parse the two files separately, finally add all the sentences
		//ThinDataset sourceDomainData=new ThinDataset();
		//sourceDomainData.parse(null, ulSourceData);
		UnlabeledDataset unlabeledData=new UnlabeledDataset();
		unlabeledData.add(trainingData);
		
		//unlabeledData.add(testData);
		unlabeledData.add(targetDomainData);
		//unlabeledData.add(sourceDomainData);
		
		if(parameters.get("-bigData").length!=0)
		{
			System.out.println("adding the bigdata......");
			unlabeledData.add(parameters.get("-bigData"));
		}
		
		
		/*
		ArrayList<String> unlabeledDataPath=new ArrayList<String>();
		unlabeledDataPath.add("../datasets/Google Task/source/ontonotes-wsj-train");
		unlabeledDataPath.add("../datasets/Google Task/target/"+targetDomain+"/gweb-"+targetDomain+"-dev");
		unlabeledDataPath.add("../datasets/Google Task/target/"+targetDomain+"/gweb-"+targetDomain+".unlabeled");
		unlabeledDataPath.add("../datasets/Google Task/target/"+targetDomain+"/gweb-"+targetDomain+"-test");
		unlabeledDataPath.add("../datasets/wsj_ul.100k");
		*/
		//int count_featureTypes=3;
		WordFeatures[] wordFeatures= {new WordFeatures(), new WordFeatures(), new WordFeatures()};
		wordFeatures[0].featureType=new DistFeatures(unlabeledData,500);
		wordFeatures[0].isAlwaysActive=true;
		
		wordFeatures[1].featureType=new MorphFeatures(trainingData);
		//wordFeatures[1].featureType=new MorphFeatures(trainingFile[0]);
		wordFeatures[1].isAlwaysActive=true;
		
		wordFeatures[2].featureType=new WordSignatures(trainingData);
		wordFeatures[2].isAlwaysActive=true;
		
		
		Tagger myTagger=new Tagger(trainingData,wordFeatures);
		returnOfTaggingTrainingData return_training=myTagger.getTrainingSet(ngram_size, no_of_ngrams); //no_of_ngrams cut the training file, only use the first part
		//returnOfTaggingTestData return_test=myTagger.getTestSet(ngram_size, no_of_ngrams_test, testData);
		
		
		
		//myTagger.printFeatureVector(return_training.features, return_training.tokens,1);

		//System.exit(0);
		//Map<Integer, String> id2Tag=myTagger.getLabelsToTagMap();
		//clear myTagger testData targetDomainData sourceDomainData unlabeledData trainingData;
		/*
		if saveToFile
	    save(sprintf('results/%s',targetDomain),'train_*','test_*','tagMap');
		*/
		
		System.out.printf("No. of training examples:%d\n", return_training.features.values.length);
		System.out.println("Traning SVM......");
		
		//returnOfTagging instance=new returnOfTagging(no_of_ngrams);
		//instance=return_training;
		//Feature[][] requiredFeatures_training=geneRequiredFeatureFormat(return_training, no_of_ngrams);
		
		//Feature[][] requiredFeatures_test=geneRequiredFeatureFormat(return_test, no_of_ngrams_test);
		
		Model model=svmTraining(myTagger.no_of_ngrams_train, return_training.features.names.size(), return_training.features.values,return_training.labels);
		
		//save the model, feature names into file
		System.out.println("save the model, feature names into file....");
		saveModelAndFeatureNames(model, myTagger, trainingData);
		
		//saveModelAndFeatureNames(myTagger);
		//saveWordFeature(return_training);
		System.out.println("finished!");
		/*
		//use the model the predict the labels in training data and compute the accuracy in training data
		System.out.println("SVM is predicting......");
		resultOfPredict result_training=predict(return_training.features.values, model, myTagger.no_of_ngrams_train, return_training.labels);
		resultOfPredict result_test=predict(return_test.features.values, model, myTagger.no_of_ngrams_test, return_test.labels);

		double[] test_unknown=myTagger.unknownWordAccuracy(return_test.labels, result_test.labels, return_test.unknownTokens);
		
		System.out.printf("Acc. on training set(all words): %f\n", result_training.accu);
		System.out.printf("Acc. on test set(all words): %f\n", result_test.accu);
		System.out.printf("Acc. on test set(unknownWords): %f (%d / %d)\n", test_unknown[0], (int)test_unknown[1],(int)test_unknown[2]);
		
		System.out.printf("unknown word baseline: %f\n\n", myTagger.majBaseline(return_test));

		myTagger.printErrors(return_test.labels, result_test.labels,return_test.unknownTokens, return_test.tokens, targetDomain, return_test.sBoundaries);
		*/
		

		
	}
}