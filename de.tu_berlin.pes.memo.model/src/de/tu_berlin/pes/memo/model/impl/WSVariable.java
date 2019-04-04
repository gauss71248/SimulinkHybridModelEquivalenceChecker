// COPYRIGHT NOTICE (NOT TO BE REMOVED):
//
// This file, or parts of it, or modified versions of it, may not be copied,
// reproduced or transmitted in any form, including reprinting, translation,
// photocopying or microfilming, or by any means, electronic, mechanical or
// otherwise, or stored in a retrieval system, or used for any purpose, without
// the prior written permission of all Owners unless it is explicitly marked as
// having Classification `Public'.
//   Classification: Restricted.
//
// Owners of this file give notice:
//   (c) Copyright 2010-2011 PES Software Engineering for Embedded Systems, TU Berlin
//
// Authors:
//		Sabine Glesner
//		Robert Reicherdt
//		Elke Salecker
//		Volker Seeker
//		Joachim Kuhnert
// 		Roman Busse
//		Alexander Reinicke
//
// All rights, including copyrights, reserved.
//
// This file contains or may contain restricted information and is UNPUBLISHED
// PROPRIETARY SOURCE CODE OF THE Owners.  The Copyright Notice(s) above do not
// evidence any actual or intended publication of such source code.  This file
// is additionally subject to the conditions listed in the RESTRICTIONS file
// and is with NO WARRANTY.
//
// END OF COPYRIGHT NOTICE

package de.tu_berlin.pes.memo.model.impl;

import de.tu_berlin.pes.memo.model.util.SimulinkParameterNames;

/**
 * Represents a variable available in workspace
 *
 * @author Joachim Kuhnert
 */
public class WSVariable extends SimulinkItem {

	private String name = "";
	private String value = "";

	public WSVariable() {
		super(-1, null);
	}

	public WSVariable(int id) {
		super(id, null);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *           the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @param value
	 *           the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Converts the value String of the variable to an Array. The value is
	 * defined by its size parameter. A simple constant is of size 1x1. The
	 * returned array has the form String[column][row].
	 *
	 * @return The 2-dimensional String Array
	 */
	public String[][] valueToArray() {
		String[][] result;
		String size = getParameter().get(SimulinkParameterNames.VAR_SIZE); // "[ y x ]"
		size = size.substring(1, size.length()).trim(); // remove [...]
		String[] sizeSplit = size.split("\\x20");
		int x = Integer.parseInt(sizeSplit[1].replace(".0", "")); // the row
																						// length is
																						// the second
																						// value
		int y = Integer.parseInt(sizeSplit[0].replace(".0", "")); // the column
																						// length the
																						// first value
		result = new String[x][y];
		String[] valueSplit = value.substring(1, value.length()).trim().split("\\x20");

		for (int i = 0; i < x; i++) {
			for (int j = 0; j < y; j++) {
				result[i][j] = valueSplit[(i * y) + j];
			}
		}

		return result;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(name + "\n");
		result.append(getParameter() + "\n");

/*		TODO comment back in
		String[][] values = valueToArray();
		for (int j = 0; j < values[0].length; j++) {
			for (String[] value2 : values) {
				result.append(value2[j] + " ");
			}
			result.append("\n");
		}
*/
		result.append(this.value); //TODO remove
		return result.toString();
	}

}
