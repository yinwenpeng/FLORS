package mypack;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import nlp.model.Sentence;


public class CountWordFreq {

	String delimiter = "\t";  //tab
	Map<String, int[]> wordFreq=new HashMap<String, int[]>();
	
	String topFiveK="/mounts/Users/student/wenpeng/datasets/GoogleTask/target/reviews/gweb-reviews-dev";
	String wsj[]={"/mounts/Users/student/wenpeng/datasets/GoogleTask/target/answers/gweb-answers-dev",
			"/mounts/Users/student/wenpeng/datasets/GoogleTask/target/emails/gweb-emails-dev",
			"/mounts/Users/student/wenpeng/datasets/GoogleTask/target/newsgroups/gweb-newsgroups-dev",
			"/mounts/Users/student/wenpeng/datasets/GoogleTask/target/reviews/gweb-reviews-dev",
			"/mounts/Users/student/wenpeng/datasets/GoogleTask/target/weblogs/gweb-weblogs-dev"};
	String bigdata="/mounts/data/proj/wenpeng/MC/firstCorpus.txt";
	String printFile="/mounts/data/proj/wenpeng/public/wordFreq/wordFreq.txt";
	
	public void readFiveK() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(topFiveK));
		String currentLine;
		//Sentence currentSentence = new Sentence();
		int line=0;
		while ((currentLine = br.readLine()) != null) {
			if (currentLine.isEmpty()) { // meet the end of a sentence
				if(line>=5000) break;
			} else {
				String[] splitted = currentLine.split(delimiter);
				if (splitted.length != 2) {
					br.close();
					throw new IOException("Malformed line: " + currentLine);
				} else {
					line++;
					System.out.println("Parsing 5k line "+line);
					String token = splitted[0].toLowerCase(Locale.ENGLISH).replaceAll("\\d", "0");
					if(!wordFreq.containsKey(token))
					{
						int[] values=new int[3];
						values[0]=1;
						wordFreq.put(token, values);
					}
					else{
						wordFreq.get(token)[0]+=1;
					}
				}
			}
		}		
		br.close();
	}
	public void readWSJ() throws IOException
	{
		for(String file: wsj)
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String currentLine;
			//Sentence currentSentence = new Sentence();
			int line=0;
			while ((currentLine = br.readLine()) != null) {
				if (!currentLine.isEmpty()) 
				{ 
					String[] splitted = currentLine.split(delimiter);
					if (splitted.length != 2) 
					{
						br.close();
						throw new IOException("Malformed line: " + currentLine);
					} else 
					{
						line++;
						System.out.println("Parsing wsj line "+line);
						String token = splitted[0].toLowerCase(Locale.ENGLISH).replaceAll("\\d", "0");
						if(!wordFreq.containsKey(token))
						{
							int[] values=new int[3];
							values[1]=1;
							wordFreq.put(token, values);
						}
						else{
							wordFreq.get(token)[1]+=1;
						}
					}
				}
			}		
			br.close();
		}
	}
	public void readBigData() throws IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(bigdata));
		String currentLine;
		//Sentence currentSentence = new Sentence();
		int line=0;
		String deli=" ";
		while ((currentLine = br.readLine()) != null) {
			if (!currentLine.isEmpty()) 
			{ 
				line++;
				System.out.println("Parsing bigdata line "+line);
				String[] splitted = currentLine.split(deli);
				for(String word: splitted)
				{
					String token = word.toLowerCase(Locale.ENGLISH).replaceAll("\\d", "0");
					if(!wordFreq.containsKey(token))
					{
						int[] values=new int[3];
						values[2]=1;
						wordFreq.put(token, values);
					}
					else{
						wordFreq.get(token)[2]+=1;
					}
				}
			}
		}		
		br.close();
	}
	public void print() throws IOException
	{
		FileWriter file = new FileWriter(printFile);
		for(String word: wordFreq.keySet())
		{
			file.write(word);
			for(int i=0;i<wordFreq.get(word).length;i++)
			{
				file.write(" "+wordFreq.get(word)[i]);
			}
			file.write("\n");
		}
		file.close();
	}
	public void check() throws NumberFormatException, IOException
	{
		BufferedReader br = new BufferedReader(new FileReader(printFile));
		String currentLine;
		//Sentence currentSentence = new Sentence();
		int fiveK=0;
		int five_wsj=0;
		int five_bigdata=0;
		String deli=" ";
		while ((currentLine = br.readLine()) != null) {
			String[] tokens=currentLine.split(deli);
			if(Integer.parseInt(tokens[1])!=0)
			{
				fiveK++;
				if(Integer.parseInt(tokens[2])!=0)
				{
					five_wsj++;					
				}
				if(Integer.parseInt(tokens[3])!=0)
				{
					five_bigdata++;
				}
			}
		}
		br.close();
		System.out.println("fiveK "+fiveK+" five_wsj "+five_wsj+" five_bigdata "+five_bigdata);
	}
	public  static void main(String[] args) throws IOException 
	{
		CountWordFreq instance=new CountWordFreq();
		//instance.readFiveK();
		//instance.readWSJ();
		//instance.readBigData();
		//instance.print();
		instance.check();
	}
}
