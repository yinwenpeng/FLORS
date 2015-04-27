package mypack;
import nlp.processing.features.MatlabSparse;

public class returnOfGetFeaturesOfTagger{
	public MatlabSparse rawFeatures;
	public matureFeatures matuFeatures;
	
	public returnOfGetFeaturesOfTagger(int dim)
	{
		//this.rawFeatures=new MatlabSparse(int n, int initialCapacity);
		this.matuFeatures=new matureFeatures(dim);
	}
}