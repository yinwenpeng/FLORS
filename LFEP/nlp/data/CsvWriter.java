package nlp.data;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class CsvWriter
{
  private String SEPARATOR = "\t";

  BufferedWriter file = null;

  public void setSeparator(String s)
  {
    this.SEPARATOR = s;
  }

  public CsvWriter(String fileName, Boolean append)
    throws IOException
  {
    this.file = new BufferedWriter(new FileWriter(fileName, append.booleanValue()));
  }

  public CsvWriter(String fileName) throws IOException {
    this.file = new BufferedWriter(new FileWriter(fileName));
  }

  public void outputLine(String output) throws IOException {
    this.file.write(output + "\n");
  }

  public void close() throws IOException {
    this.file.close();
  }

  public void output(String output) throws IOException {
    this.file.write(output);
  }

  public void output(double output) throws IOException {
    this.file.write(Double.toString(output));
  }

  public void append(String output) throws IOException {
    this.file.append(output);
  }

  public void output(ArrayList<?> output) throws IOException {
    if (output.size() > 0) {
      this.file.write(output.get(0).toString());
    }
    for (int i = 1; i < output.size(); i++)
      this.file.write(SEPARATOR + output.get(i).toString());
  }

  public void outputSparse(ArrayList<Double> output) throws IOException
  {
    ArrayList<Integer> nonzeroEntries = new ArrayList<Integer>();
    for (int i = 0; i < output.size(); i++) {
      if (output.get(i).doubleValue() != 0.0D) {
        nonzeroEntries.add(i);
      }
    }

    for (int i = 0; i < nonzeroEntries.size(); i++) {
      int index = nonzeroEntries.get(i).intValue();
      if (i > 0) {
        file.write(SEPARATOR);
      }
      file.write(Integer.toString(index + 1) + ":" + output.get(index).toString());
    }
  }

  public void outputSeparator() throws IOException {
    file.write(SEPARATOR);
  }

  public void flush() throws IOException {
    file.flush();
  }

  public void outputSparse(HashMap<Integer, Double> featureVector) throws IOException {
    boolean isNotFirstLine = false;
    for (Entry<Integer, Double> entry : featureVector.entrySet()) {
      int index = entry.getKey();
      Double value = entry.getValue();

      if (isNotFirstLine)
        file.write(SEPARATOR);
      else {
        isNotFirstLine = true;
      }

      file.write(Integer.toString(index + 1) + ":" + value.toString());
    }
  }
  public void outputForWapiti(HashMap<Integer, Double> featureVector, int dim) throws IOException {
	  for (int i = 0; i < dim; i++) {
		  if (featureVector.containsKey(i))
			  file.write("\t1");
		  else
			  file.write("\t0");
	  }

  }
  public void outputSparseForCRFsuite(HashMap<Integer, Double> featureVector, String prefix) throws IOException {
	  for (Integer dim : featureVector.keySet()) {
		  file.write(SEPARATOR);
		  file.write(prefix + dim);
	  }
  }
  
  public void outputForCRFsuite(HashMap<String, Double> featureVector) throws IOException {
	  for (Entry<String, Double> entry : featureVector.entrySet()) {
		  String featureName = entry.getKey();
		  featureName = featureName.replace("\\","\\\\");
		  featureName = featureName.replace(":","\\:");

		  file.write(SEPARATOR);

		  file.write(featureName);
	  }
  }
}