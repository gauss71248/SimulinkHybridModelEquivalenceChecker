package de.tu_berlin.pes.memo.model.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Cycle {

	private ArrayList<Block> cycle;
	
	public Cycle(Set<Block> cycle){
		this.cycle = new ArrayList<Block>(cycle);
	}
	public Cycle(List<Block> cycle){
		this.cycle = new ArrayList<Block>(cycle);
	}
	public ArrayList<Block> getCycle() {
		return cycle;
	}
	public void setCycle(ArrayList<Block> cycle) {
		this.cycle = cycle;
	}
	
}
