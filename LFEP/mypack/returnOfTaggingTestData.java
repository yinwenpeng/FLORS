package mypack;
import java.util.Set;

import cern.colt.list.IntArrayList;

public class returnOfTaggingTestData extends returnOfTagging{
	//public matureFeatures features;
	//public double[] labels;
	//public String[] tokens;
	public IntArrayList unknownTokens;
	public IntArrayList sBoundaries; 
	
	public returnOfTaggingTestData(int dim)
	{
		super(dim);
		//this.features=new matureFeatures(dim);
		this.unknownTokens=new IntArrayList();
		this.sBoundaries=new IntArrayList();
	}
}