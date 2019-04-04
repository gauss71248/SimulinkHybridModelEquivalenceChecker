/**
 * 
 */
package de.tu_berlin.de.pes.memo.tools.sequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author marcus
 *
 */
public class Sequence {

	private List<SequenceItem> items;

	private List<SequenceStep> steps;

	private Map<SequenceItem, SequenceStep> itemStepMap;

	public Sequence(List<SequenceItem> items, List<SequenceStep> steps) {
		super();
		this.items = items;
		this.steps = steps;
	}

	public Sequence() {
		this.items = new ArrayList<SequenceItem>();
		this.steps = new ArrayList<SequenceStep>();
	}

	public Boolean addItem(SequenceItem item) {
		Boolean result = false;
		if (this.steps.size() < this.items.size()) {
			result = false;
		} else {
			this.items.add(item);
			result = true;
		}

		return result;
	}

	public Boolean addStep(SequenceStep step) {
		Boolean result = false;
		if (this.items.size() == 0) {
			result = false;
		} else if (this.steps.size() < this.items.size()) {
			this.steps.add(step);
			result = true;
		} else if (this.steps.size() > this.items.size()) {
			result = false;
		}
		return result;
	}

	/**
	 * @return the items
	 */
	public List<SequenceItem> getItems() {
		return items;
	}

	/**
	 * @param items
	 *            the items to set
	 */
	public void setItems(List<SequenceItem> items) {
		this.items = items;
	}

	/**
	 * @return the steps
	 */
	public List<SequenceStep> getSteps() {
		return steps;
	}

	/**
	 * @param steps
	 *            the steps to set
	 */
	public void setSteps(List<SequenceStep> steps) {
		this.steps = steps;
	}

	/**
	 * @return the itemStepMap
	 */
	public Map<SequenceItem, SequenceStep> getItemStepMap() {
		return itemStepMap;
	}

	/**
	 * @param itemStepMap
	 *            the itemStepMap to set
	 */
	public void setItemStepMap(Map<SequenceItem, SequenceStep> itemStepMap) {
		this.itemStepMap = itemStepMap;
	}
	
	public String toString() {
		String result = new String();
		for (int i = 0; i < this.items.size(); i++) {
			result += this.items.get(i);
			if (this.steps.size() > 0) {
				result += this.steps.size() >= (i - 1) ? this.steps.get(i) : "";				
			}
		}
		return result;
	}

}
