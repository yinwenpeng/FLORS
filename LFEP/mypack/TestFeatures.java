package mypack;
import java.io.IOException;

import nlp.model.LabeledDataset;
import nlp.model.UnlabeledDataset;
import nlp.processing.Features;
import nlp.processing.NgramGenerator;
import nlp.processing.features.DistFeatures;
import nlp.processing.features.MatlabSparse;
import nlp.processing.features.MorphFeatures;
import nlp.processing.features.WordSignatures;


public class TestFeatures {
	public static void main(String[] args) throws IOException {
		UnlabeledDataset data = new UnlabeledDataset();
		String[] unlabeledFiles = {"../../datasets/Google Task/target/emails/gweb-emails.unlabeled"};
		String[] labeledFiles = {"../../datasets/Google Task/source/ontonotes-wsj-train", "F:/Diplomarbeit/input/Google Task/target/emails/gweb-emails-dev"};
		
		data.parse(labeledFiles, unlabeledFiles);
		
		LabeledDataset labeledData = new LabeledDataset();
		String[] trainingFile = {"../../datasets/Google Task/target/emails/gweb-emails-dev"};
		labeledData.parse(trainingFile);
				
		DistFeatures dFeatures = new DistFeatures(data, 250);	
		MorphFeatures wFeatures = new MorphFeatures(labeledData);
		WordSignatures signFeatures = new WordSignatures(labeledData);
		Features allFeatures[] = {wFeatures, dFeatures, signFeatures};
		
		NgramGenerator gen = new NgramGenerator(labeledData, true);
		MatlabSparse sparse = gen.getFeatures(1000, allFeatures, 5);
		System.out.println("Components generated: " + sparse.featureNames.length);
	}
	
	
	
	
}
