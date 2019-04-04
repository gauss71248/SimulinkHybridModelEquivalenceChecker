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
//   (c) Copyright 2010-2012 PES Software Engineering for Embedded Systems, TU Berlin
//
// Authors:
//		Sabine Glesner
//		Robert Reicherdt
//		Elke Salecker
//		Volker Seeker
//		Joachim Kuhnert
// 		Roman Busse
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

package de.tu_berlin.pes.memo.parser.stateflow.datatypes;

import java.util.ArrayList;

/**
 * This expression maps an array access of the form
 * <code>arrayname[firstParameter][secondParameter]...[nthParameter]</code>.
 *
 * @author Joachim Kuhnert
 */
public class Array extends Expression {

	/** The name of the array */
	Expression identifier;
	/** The parameters for the array access in order */
	ArrayList<Expression> arguments = new ArrayList<Expression>();

	/**
	 * Creates a new array access object.
	 *
	 * @param identifier
	 *           The array name
	 * @param args
	 *           The list of arguments in order
	 */
	public Array(Expression identifier, ArrayList<Expression> paramters) {
		this.identifier = identifier;
		this.arguments = paramters;
	}

	@Override
	public String toString() {
		String result = "Array: <";
		if (identifier != null) {
			result += identifier.toString();
		}
		result += "[";
		if (!arguments.isEmpty()) {
			for (Expression exp : arguments) {
				result += exp.toString() + ", ";
			}
			result = result.substring(0, result.length() - 2);
		}
		result += "]>";
		return result;
	}

	/**
	 * @return the identifier
	 */
	public Expression getIdentifier() {
		return identifier;
	}

	/**
	 * @return the arguments
	 */
	public ArrayList<Expression> getArguments() {
		return arguments;
	}

}
