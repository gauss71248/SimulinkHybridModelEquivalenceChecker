package de.tu_berlin.de.pes.memo.tools.sequence;

public class SequenceStep {
	private int step;

	/**
	 * @return the step
	 */
	public int getStep() {
		return step;
	}

	/**
	 * @param step the step to set
	 */
	public void setStep(int step) {
		this.step = step;
	}

	public SequenceStep(int step) {
		super();
		this.step = step;
	}
	
	public String toString() {
		String result = new String();
		result += "-> " + this.step + " ->";
		
		return result;
	}
}
