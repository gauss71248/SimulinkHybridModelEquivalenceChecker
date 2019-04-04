package de.tu_berlin.pes.memo.model.impl;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper class, because Hibernate can't map nested collections. A SignalLine
 * in a bus system that get signals from a switch has multiple possible origins
 * according to the signal forwarded by the switch.
 *
 * So the
 *
 * @author Joachim Kuhnert
 *
 */
public class ConcurrentSignalLineOrigins implements Serializable {
	public Set<SignalLine> concurrentOrigins = new HashSet<SignalLine>();

	@Override
	public String toString() {
		String result = "";
		for (SignalLine sl : concurrentOrigins) {
			result += sl + " || ";
		}
		if (result.length() > 0) {
			result = result.substring(0, result.length() - 4);
		}

		return result;
	}

	/**
	 * Get all possible signal line origins.
	 *
	 * @return
	 */
	public Set<SignalLine> getConcurrentOrigins() {
		return concurrentOrigins;
	}

	/**
	 * Set all possible signal line origins.
	 *
	 * @param concurrentOrigins
	 *           The signal line origins to set.
	 */
	public void setConcurrentOrigins(Set<SignalLine> concurrentOrigins) {
		this.concurrentOrigins = concurrentOrigins;
	}

}
