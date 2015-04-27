package nlp.processing.features;

import java.util.List;

public class Vector {
	public int[] indices;
	public double[] values;
	
	public Vector(int n) {
		indices = new int[n];
		values = new double[n];
	}
	
	public Vector(List<Integer> indicesIn, List<Integer> valuesIn) {
		int n = indicesIn.size();
		indices = new int[n];
		values = new double[n];
		for (int i = 0; i < n; i++) {
			indices[i]=indicesIn.get(i);
			values[i]=valuesIn.get(i).intValue();
		}
	}
	
	public Vector(int[] indices, double[] values) {
		this.indices = indices;
		this.values = values;
	}	
	
	public int length() {
		return indices.length;
	}
}
