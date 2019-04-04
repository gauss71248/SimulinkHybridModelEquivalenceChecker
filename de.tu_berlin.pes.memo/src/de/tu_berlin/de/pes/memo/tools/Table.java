/**
 * 
 */
package de.tu_berlin.de.pes.memo.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author marcus
 *
 */
public class Table<T1, T2, T3> {

	/**
	 * 
	 */
	private Map<T1, Map<T2, T3>> internalRepresentation;
	
	/**
	 * useless constructor
	 */
	public Table() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param internalRepresentation
	 */
	public Table(Map<T1, Map<T2, T3>> internalRepresentation) {
		this.internalRepresentation = internalRepresentation;
	}

	/**
	 * 
	 * @param firstKeySet
	 * @param secondKeySet
	 * @param entry
	 */
	public Table(Set<T1> firstKeySet, Set<T2> secondKeySet, T3 entry) {
		this.internalRepresentation = new HashMap<T1, Map<T2, T3>>();
		
		for (T1 currentFirstKey : firstKeySet) {
			Map<T2, T3> currentInnerMap = new HashMap<T2, T3>();
			for (T2 currentSecondKey : secondKeySet) {
				currentInnerMap.put(currentSecondKey, null);
			}
			this.internalRepresentation.put(currentFirstKey, currentInnerMap);
		}
				
	}
	
	
	/**
	 * 
	 * @param firstIndex
	 * @param secondIndex
	 * @return
	 */
	public T3 getElement(T1 firstIndex, T2 secondIndex) {
		
		return this.internalRepresentation.get(firstIndex).get(secondIndex);
	}
	
	/**
	 * 
	 * @return
	 */
	public Integer getInnerSize() {
		int returnValue = -1;
		for (T1 currentKey : this.internalRepresentation.keySet()) {
			returnValue = this.internalRepresentation.get(currentKey).size();
		}
		return returnValue;
	}

	/**
	 * 
	 * @return
	 */
	public Integer getOuterSize() {
		return this.internalRepresentation.size();
	}
	
	/**
	 * 
	 * @param firstIndex
	 * @param secondIndex
	 * @param element
	 */
	public Boolean setElement(T1 firstIndex, T2 secondIndex, T3 element) {
		if (this.internalRepresentation.get(firstIndex) != null) {
			this.internalRepresentation.get(firstIndex).put(secondIndex, element);
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Table [internalRepresentation=" + internalRepresentation + "]";
	}
}
