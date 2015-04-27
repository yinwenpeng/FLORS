package mypack;
import java.util.ArrayList;

import nlp.data.Pair;
import nlp.processing.features.Vector;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;
import de.bwaldvogel.liblinear.Feature;

public class matureFeatures{
	//public DoubleArrayList[] values;
	public Feature[][] values;
	public ArrayList names;
	
	public matureFeatures(int dim)
	{
		int init=10;
		//this.values=new DoubleArrayList[dim];
		this.values=new Feature[dim][];
		/*
		for(int i=0;i<dim;i++)
		{
			this.values[i]=new DoubleArrayList(init);
		}
		*/
		this.names=new ArrayList(init);
	}
}