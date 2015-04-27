package nlp.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Sentence {
	ArrayList<String> tags;
	ArrayList<String> tokens;
	private List<String> rawTokens = new ArrayList<String>();
	public final static String BOUNDARY = "<BOUNDARY>";

	public Sentence() {
		tokens = new ArrayList<String>();
		tags = new ArrayList<String>();
	}

	public Sentence(ArrayList<String> tags, ArrayList<String> tokens) {
		this.tags = tags;
		this.tokens = tokens;
	}

	public int size() {
		return tokens.size();
	}

	public void add(String token, String tag) {
		tags.add(tag.trim());
		tokens.add(token.trim());
	}
	
	public void addRawToken(String token) {
		rawTokens.add(token);
	}
	
	public void setRawTokens(ArrayList<String> tokens) {
		rawTokens = tokens;
	}
	
	@Override
	public String toString() {
		String output = "";
		for (String token : tokens) {
			output += token + "\t";
		}
		if (!tags.isEmpty()) {
			output += "\n";
			for (String tag : tags) {
				output += tag + "\t";
			}
		}
		return output;
	}

	public ArrayList<String> getTags() {
		return tags;
	}

	public void setTags(ArrayList<String> tags) {
		if (tags.size() != tokens.size()) {
			throw new IllegalArgumentException(
					"Number of tags has to be equal to the number of tokens!");
		} else {
			this.tags = tags;
		}
	}

	public ArrayList<String> getTokens() {
		return tokens;
	}

	public String getToken(int i) {
		return tokens.get(i);
	}
	
	public String getRawToken(int i) {
		return rawTokens.get(i);
	}

	public String getTokenSafe(int i) {
		return getItemSafe(i, tokens);
	}

	public String getTagSafe(int i) {
		return getItemSafe(i, tags);
	}

	private String getItemSafe(int i, ArrayList<String> items) {
		if (i >= size() || i < 0) {
			return BOUNDARY;
		} else {
			return items.get(i);
		}
	}

	public String getTag(int i) {
		return tags.get(i);
	}

	public void setTokens(ArrayList<String> tokens) {
		this.tokens = tokens;
	}

	public void convertToLowerCase() {
		convertToLowerCase(tokens);
	}

	public void replaceDigits() {
		replaceDigits(tokens);
	}

	public static void replaceDigits(List<String> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			tokens.set(i, tokens.get(i).replaceAll("\\d", "0"));
		}
	}
	public static void convertToLowerCase(List<String> tokens) {
		for (int i = 0; i < tokens.size(); i++) {
			tokens.set(i, tokens.get(i).toLowerCase(Locale.ENGLISH));
		}
	}

	public List<String> getRawTokens() {
		return rawTokens ;
	}

}
