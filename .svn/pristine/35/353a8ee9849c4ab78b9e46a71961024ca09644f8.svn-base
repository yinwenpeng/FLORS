package nlp.processing;

public class ProgressBar {
	protected double maxVal;
	protected long lastOutput;
	final static int UPDATE_INTERVAL = 2000; // refresh every 2s
	
	public ProgressBar(double maxVal) {
		this.maxVal = maxVal;
		lastOutput = System.currentTimeMillis();
	}
	
	public void showProgress(double curVal) {
		if (System.currentTimeMillis() - lastOutput > UPDATE_INTERVAL || curVal >= maxVal) {
			System.out.printf("%.1f%% (%.0f / %.0f) \n", curVal*100/maxVal, curVal, maxVal);
			lastOutput = System.currentTimeMillis();
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		ProgressBar a = new ProgressBar(12343);
		for (int i = 0; i <= 12343; i++) {
			Thread.sleep(1);
			a.showProgress(i);
		}
	}
}
