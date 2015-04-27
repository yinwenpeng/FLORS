package mypack;
public class returnOfTagging{
	public matureFeatures features;
	public double[] labels;
	public String[] tokens;
	
	public returnOfTagging(){}
	
	public returnOfTagging(int dim)
	{
		this.features=new matureFeatures(dim);
	}
	
}