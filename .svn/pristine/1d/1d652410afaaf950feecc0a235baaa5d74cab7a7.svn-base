package nlp.processing.features;

import cern.colt.list.DoubleArrayList;
import cern.colt.list.IntArrayList;

public class MatlabSparse {//????????????????????????????????
	public IntArrayList[] indicesX; // list of list of int
	public IntArrayList[] indicesY;
	public DoubleArrayList[] values;
	public DoubleArrayList[] sumPerVector;// used to store the sum of the vector
	public IntArrayList sentenceBoundaries; // intArrazList is a list of int
	public String[][] featureNames;
	public String[] tags;
	public String[] ngrams;
	public String[] tokens;
	//MatlabSparse features = new MatlabSparse(noOfVectors, noOfSamples * 10);
	public MatlabSparse(int n, int initialCapacity) {
		indicesX = new IntArrayList[n];
		indicesY = new IntArrayList[n];
		values = new DoubleArrayList[n];
		sumPerVector = new DoubleArrayList[n];
		featureNames = new String[n][0];
		sentenceBoundaries = new IntArrayList();// an empty list
		
		for (int i = 0; i < n; i++) {
			indicesX[i] = new IntArrayList(initialCapacity);
			indicesY[i] = new IntArrayList(initialCapacity);
			values[i] = new DoubleArrayList(initialCapacity);
			sumPerVector[i] = new DoubleArrayList(initialCapacity);
		}
	}
	
	public void initializeOtherFields(int size) {
		tags = new String[size];
		tokens = new String[size];
		ngrams = new String[size];
	}
	
	public void compact() {
		for (int i = 0; i < values.length; i++) {
			//Trims the capacity of the receiver to be the receiver's current size.
			indicesX[i].trimToSize();
			indicesY[i].trimToSize();
			values[i].trimToSize();
			sumPerVector[i].trimToSize();
		}
		sentenceBoundaries.trimToSize();
	}
}
