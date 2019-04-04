package de.tu_berlin.pes.memo.model.impl;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
/**
 * 
 * @author Feras
 *
 */
public class ProbabilisticBlock {

	private float probability = 1.0f;
	private float conditionalProbability;
	private float inportProbability;
	private Block block;
	private int pBlockId;
	private Color color;
	private boolean isBlocked;
	private int depth;
	private float oldProbability = 1.0f;
	private Set<ProbabilisticBlock> parentBranchedBlocks; //unique set of outport blocks of control blocks which influenced probability of current block's parents, starting block can also be in this set
	private Set<ProbabilisticBlock> predecessorOutports; //set of all predecessorOutports(like a backward slice)
	private boolean loopInfluenced; //true if block's probability is influenced by a loop, the change of the probability will this way be flooded throughout the network
	private boolean conditionalInfluenced; //conditional subsystem's blocks have product probability of input and conditional block, is true if conditional block prob. was included
	

	public ProbabilisticBlock(Block block, float probability, Color color) {
		this.setBlock(block);
		this.probability = probability;
		this.setpBlockId(block.getId());
		this.color = color;
		this.isBlocked = false;
		this.depth = 0;
		this.oldProbability = 0.0f;
		this.parentBranchedBlocks = new HashSet<ProbabilisticBlock>();
		this.setLoopInfluenced(false);
		this.setConditionalProbability(0.0f);
		this.setInportProbability(0.0f);
	}

	public float getOldProbability() {
		return oldProbability;
	}

	public void setOldProbability(float oldProbability) {
			this.oldProbability = oldProbability;		
	}
	
	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}
	
	private void setProbability() {
		if(conditionalProbability != 0.0f && inportProbability != 0.0f){
			this.probability = conditionalProbability * inportProbability;
		}
		else{
			this.probability = conditionalProbability + inportProbability; //one of them is zero, so that multiplying would be 0
		}
	}

	public float getProbability() {
		return this.probability;
	}

	public Block getBlock() {
		return block;
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public int getBlockId() {
		return pBlockId;
	}

	public void setpBlockId(int pBlockId) {
		this.pBlockId = pBlockId;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}

	public boolean isBlocked() {
		return isBlocked;
	}

	public void setBlocked(boolean isBlocked) {
		this.isBlocked = isBlocked;
	}

	public Set<ProbabilisticBlock> getParentBranchedBlocks() {
		return parentBranchedBlocks;
	}
	
	@Override
	public String toString() {
		return this.block.toString();
	}

	public void setParentBranchedBlocks(Set<ProbabilisticBlock> parentBranchedBlocks) {
		this.parentBranchedBlocks = parentBranchedBlocks;
	}

	public boolean isLoopInfluenced() {
		return loopInfluenced;
	}

	public void setLoopInfluenced(boolean loopInfluenced) {
		this.loopInfluenced = loopInfluenced;
	}

	public boolean isConditionalInfluenced() {
		return conditionalInfluenced;
	}

	public void setConditionalInfluenced(boolean conditionalInfluenced) {
		this.conditionalInfluenced = conditionalInfluenced;
	}

	public float getConditionalProbability() {
		return conditionalProbability;
	}

	public void setConditionalProbability(float conditionalProbability) {
		this.conditionalProbability = conditionalProbability;
		setProbability();
	}

	public float getInportProbability() {
		return inportProbability;
	}

	public void setInportProbability(float inportProbability) {
		this.inportProbability = inportProbability;
		setProbability();
	}
}
