package mypack;
/*   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/answers/gweb-answers-dev
 *   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/emails/gweb-emails-dev
 *   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/newsgroups/gweb-newsgroups-dev
 *   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/reviews/gweb-reviews-dev
 *   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/weblogs/gweb-weblogs-dev
 *   // unlabeled data
 *   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/answers/gweb-answers.unlabeled
 *   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/emails/gweb-emails.unlabeled
 *   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/newsgroups/gweb-newsgroups.unlabeled
 *   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/reviews/gweb-reviews.unlabeled
 *   //mounts/Users/student/wenpeng/datasets/GoogleTask/target/weblogs/gweb-weblogs.unlabeled
 *   // bigdata
 *   //mounts/data/proj/wenpeng/MC/firstCorpus.txt
 *   
 *   
 *   
 *   
 *   
 */

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;


public class Entrance {
	static int loopTimes;
	public static String file_prefix_store;
	public static String output_file;
	public HashMap<String, String[]>parameters;
	public Entrance()
	{
		parameters=new HashMap<String, String[]>();
		// without initializations
		parameters.put("-mode", new String[0]);
		parameters.put("-trainFile", new String[0]);
		parameters.put("-labeledData", new String[0]);
		parameters.put("-unlabeledData", new String[0]);
		parameters.put("-bigData", new String[0]);
		parameters.put("-predictFile", new String[0]);
		parameters.put("-h", new String[0]);
		// with initialization
		String[] window=new String[1];
		window[0]="5";
		parameters.put("-window",window);
		String[] sampleNo=new String[1];
		sampleNo[0]="all";
		parameters.put("-sampleNo", sampleNo);
		String[] labeled=new String[1];
		labeled[0]="0";
		parameters.put("-labeled", labeled);
		String[] update=new String[1];
		update[0]="1";
		parameters.put("-update", update);
		
		String[] file_prefix_store=new String[1];
		file_prefix_store[0]="";// default is nothing
		parameters.put("-pre", file_prefix_store);
		String[] output=new String[1];
		output[0]="output.txt";
		parameters.put("-out", output);
		
	}
	public  boolean isNumeric(String str){ 
	    Pattern pattern = Pattern.compile("[0-9]*"); 
	    return pattern.matcher(str).matches();    
	 } 
	
	public void isValidValues()// judge the length of string[], as well as each string element
	{
		boolean right=true;
		//-h
		if(parameters.get("-h").length!=0)
		{
			System.out.println("Error: option '-h' should not have a value.\n");
			System.exit(0);
		}
		//mode
		if(parameters.get("-mode").length <1)
		{
			System.out.println("Error: please first set option '-mode'\n"); 
			System.exit(0);
		}
		else if (parameters.get("-mode").length >1)
		{
			System.out.println("Error: wrong parameter numbers for option '-mode'\n"); 
			System.exit(0);
		}
		else{
			//judge the content
			if(!parameters.get("-mode")[0].equals("predict")&&!parameters.get("-mode")[0].equals("train"))
			{
				System.out.println("Error: wrong value for option '-mode'\n"); 
				System.exit(0);
			}
		}
		//trainFile
		if(parameters.get("-mode").length==1&&parameters.get("-mode")[0].equals("train"))
		{
			if(parameters.get("-trainFile").length==0) 
			{
				System.out.println("Error: please provide value for '-trainFile'\n"); 
				System.exit(0);
			}
		}
		//PredictFile
		if(parameters.get("-mode").length==1&&parameters.get("-mode")[0].equals("predict"))
		{
			if(parameters.get("-predictFile").length==0)
			{
				System.out.println("Error: please provide value for '-predictFile'\n");
				System.exit(0);
			}
		}
		//window
		if(parameters.get("-window").length!=1)
		{
			System.out.println("Error: please set only one value for '-window'\n");
			right=false;
		}
		else{
			if(!isNumeric(parameters.get("-window")[0]))
			{
				System.out.println("Error: the value of '-window' should be numeric\n");
				right=false;
			}
		}
		//sampleNo
		if(parameters.get("-sampleNo").length!=1)
		{
			System.out.println("Error: please set only one value for '-sampleNo'\n");
			right=false;
		}
		else{
			if(!isNumeric(parameters.get("-sampleNo")[0])&&!parameters.get("-sampleNo")[0].equals("all"))
			{
				System.out.println("Error: the value of '-sampleNo' should be numeric or 'all'\n");
				right=false;
			}
		}
		// labeled
		if(parameters.get("-labeled").length!=1)
		{
			System.out.println("Error: please set only one value for '-labeled'\n");
			right=false;
		}
		else{
			if(!parameters.get("-labeled")[0].equals("1")&&!parameters.get("-labeled")[0].equals("0"))
			{
				System.out.println("Error: the value of '-labeled' should be 1 or 0.\n");
				right=false;
			}
		}
		//update
		if(parameters.get("-update").length !=1)
		{
			System.out.println("Error: please give only one value for '-update'\n");
			right=false;
		}
		else{
			if(!parameters.get("-update")[0].equals("1")&&!parameters.get("-update")[0].equals("0"))
			{
				System.out.println("Error: the value of '-update' should be 1 or 0.\n");
				right=false;
			}
		}
		
		if(right==false) System.exit(0);
		
	}
	public int findNextOption(String[] args, int currentOption)
	{
		int next=currentOption+1;
		while(next<args.length&&!parameters.containsKey(args[next])) next++;
		return next;// next might be the boundary of args[]
	}
	public void updateParameters(String[] args)
	{
		// if without parameters
		if(args.length==0)
		{
			System.out.println("\nError: empty parameters. You can use '-h' for help\n");
			System.exit(0);
		}
		// only ask for help
		if(args.length==1&&args[0].equals("-h"))
		{
			System.out.println("\nUsage:\n");
			System.out.println("  -h: for help\n");
			System.out.println("  -mode: 'train' or 'predict'\n");
			System.out.println("  -trainFile: the names or paths for the training labeled files, separated by space\n");
			System.out.println("  -labeledData: optional; supporting tagged files for making the model more robust, separated by tab\n");
			System.out.println("  -unlabeledData: optional; supporting untagged files for making the model more robust, separated by tab\n");
			System.out.println("  -bigData: optional; supporting untagged files for making the model more robust, separated by space\n");
			System.out.println("  -window: optional; length of window (default is '5')\n");
			System.out.println("  -sampleNo: (any number vs. 'all'); optional; the number of tokens you want to use for training; default is 'all'\n");
			System.out.println("  -labeled: (1 vs. 0); optional; predict the POS taggs for tagged data? Default is '0'\n");
			System.out.println("  -predictFile: the name or path of the corpus to be predicted\n");
			System.out.println("  -update: (1 vs. 0); optional; whether to incrementally update; default is '1'\n");
			System.out.println("  -pre: (any string); optional; to mark the prefix of files to be written; default is empty string\n");
			System.out.println("  -out: (any string); optional; the path of output file; default is output.txt\n");
			
			System.exit(0);
		}
		int nextOption=findNextOption(args, -1);
		if(nextOption!=0)
		{
			System.out.println("\nError: wrong option usage. You can use '-h' for help\n");
			System.exit(0);
		}
		else 
		{
			for(int i=nextOption;i<args.length;i=nextOption)
			{
				nextOption=findNextOption(args, i);//nextOption might be args.length
				if(nextOption-i==1&&!args[i].equals("-h"))
				{
					System.out.println("\nError: "+args[i]+" is not provided a value\n");
					System.exit(0);
				}
				else{
					String[] values=new String[nextOption-i-1];
					int index=0;
					for(int start=i+1; start<nextOption; start++)
						values[index++]=args[start];
					parameters.put(args[i], values);
				}
					
			}
		}
		file_prefix_store=parameters.get("-pre")[0];
		output_file=parameters.get("-out")[0];
		
	}
	public  static void main(String[] args) throws IOException 
	{
		
		Entrance instance=new Entrance();
		//update the parameter settings
		instance.updateParameters(args);		
		// judge valid values
		instance.isValidValues();
		//all are correct, go to run
		if(instance.parameters.get("-mode")[0].equals("predict"))
		{
			double[] average=new double[0];
			System.out.println("\ninitializing...");
			newCorpus corpus=new newCorpus(); // will load the model files
			
			double[] accu=corpus.FLORS_predict(instance.parameters);

			/*
			FileWriter accuracy = new FileWriter("accuracies_1loop.txt");
				
			for(int i=1;i<=average.length;i++)
			{
				average[i-1]/=loop;
				accuracy.write(i+"\t"+average[i-1]+"\n");
			}
			accuracy.close();
			*/
			System.out.println("\nPredict over...");
			
		}
		else 
		{
			System.out.println("\ntraining...\n");
			run train=new run();
			train.FLORS_train(instance.parameters);
		}
		
	}
	
}
