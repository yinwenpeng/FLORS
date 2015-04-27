package nlp.data;

public class Pair implements Comparable<Pair>{
	public int index;
	int value;
	//double value;
	
	@Override
	public String toString() {
		return "Pair [index=" + index + ", value=" + value + "]";
	}
	public Pair(int index, int value) {
		this.index = index;
		this.value = value;
	}
	@Override
	public int compareTo(Pair o) {
		if (value < o.value)
			return 1;
		else if (value == o.value) 
			return 0;
		else
			return -1;
	}

}
