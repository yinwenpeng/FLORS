package nlp.stats;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import nlp.model.Sentence;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;

public class BigramFreqs {
	FlexCompRowMatrix leftContexts;
	FlexCompRowMatrix rightContexts;
	//public FlexCompRowMatrix leftContexts_newCorpus;
	//public FlexCompRowMatrix rightContexts_newCorpus;
	protected UnigramFreqs unigrams;
	
	

	public BigramFreqs(UnigramFreqs unigrams) {
		this.unigrams = unigrams;
		//在unigram类中,已将其max-words设定为1000000,这儿就是建立一个百万乘以百万的矩阵,然后应该每行表示对应列为该行字母左边或者右边邻居的次数
		leftContexts = new FlexCompRowMatrix(UnigramFreqs.MAX_WORDS, UnigramFreqs.MAX_WORDS);
		rightContexts = new FlexCompRowMatrix(UnigramFreqs.MAX_WORDS, UnigramFreqs.MAX_WORDS);
	}

	public void addCorpus(ArrayList<Sentence> corpus) {
		for (Sentence sentence : corpus) {
			if (sentence.size() > 0) {
				addBigramsFromSentence(sentence);
			}
		}
	}
	public void addFile(String filePath) throws IOException
	{
		String delimiter=" ";
		int token;
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String currentLine;
		int lineCo=0;
		String[] splitted;
		
		while ((currentLine = br.readLine()) != null) 
		{
			//System.out.println(lineCount++);
			// Skip empty lines
			if (!currentLine.trim().isEmpty()) 
			{
				System.out.println("bigram updating..."+lineCo+"...");
				lineCo++;
				splitted = currentLine.split(delimiter);  //"<BOUNDARY>"
				ArrayList<String> clearLine=new ArrayList<String>();
				//first clear the splitted
				for (String word : splitted)
				{
					clearLine.add(word.toLowerCase(Locale.ENGLISH).replaceAll("\\d", "0"));
				}
				for (int i = -1; i <= clearLine.size(); i++) {
					
					if(i==-1||i==clearLine.size())
					{
						token = unigrams.translate("<BOUNDARY>");
						//incrementTable(rightContexts, token, unigrams.translate(splitted[i+1]));
					}
					else{
						token = unigrams.translate(clearLine.get(i));
					}
					//update left context
					if (i==0){
						incrementTable(leftContexts, token, unigrams.translate("<BOUNDARY>"));
					}
					if (i > 0) {
						incrementTable(leftContexts, token, unigrams.translate(clearLine.get(i-1)));
					}
					//update right context
					if (i < clearLine.size()-1) {
						incrementTable(rightContexts, token, unigrams.translate(clearLine.get(i + 1)));
					}
					if (i == clearLine.size()-1){
						incrementTable(rightContexts, token, unigrams.translate("<BOUNDARY>"));
					}
				}
			}
			
		}
		br.close();
	}
	//这个函数就是每读取一个句子,就更新leftcontents和rightcontent矩阵
	protected void addBigramsFromSentence(Sentence sentence) {
		for (int i = -1; i <= sentence.size(); i++) {
			int token = unigrams.translate(sentence.getTokenSafe(i));
		
			if (i > -1) {
				incrementTable(leftContexts, token, unigrams.translate(sentence.getTokenSafe(i - 1)));
			}
			if (i < sentence.size()) {
				incrementTable(rightContexts, token, unigrams.translate(sentence.getTokenSafe(i + 1)));
			}
		}
	}

	private void incrementTable(FlexCompRowMatrix table, int token, int context) {
		table.add(token, context, 1.0d); //这里应该表示在这个table表的token行的context列加上一个1
	}

	public SparseVector getLeftFrequencies(String token) {
		int tokenId = unigrams.translate(token); //将token换算成数字编号id
		return leftContexts.getRow(tokenId);
	}

	public SparseVector getRightFrequencies(String token) {
		int tokenId = unigrams.translate(token);
		return rightContexts.getRow(tokenId);
	}
}
