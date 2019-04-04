package de.tu_berlin.pes.memo.model.impl;

/**
 * Signalline with a probabilistic value, use for Probabilistic Slicing
 * 
 * @author Moritz
 *
 */
public class ProbabilisticSignalLine extends SignalLine {

	// TODO: default value of 1, this signal line will be always sliced
	private float probability = 1.0f;

	public ProbabilisticSignalLine() {
		super();
	}

	public ProbabilisticSignalLine(float probability) {
		this.probability = probability;
	}

	public ProbabilisticSignalLine(int id, int lvl, Block src, Block dst, Port srcPort,
			Port dstPort, String signalName, float probability) {
		super(id, lvl, src, dst, srcPort, dstPort, signalName);
		this.probability = probability;
	}

	public float getProbability() {
		return this.probability;
	}

	public void setProbability(float probability) {
		this.probability = probability;
	}
}
