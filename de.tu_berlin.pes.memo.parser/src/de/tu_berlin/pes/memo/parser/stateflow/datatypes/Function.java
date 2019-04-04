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
import java.util.List;

/**
 * Represents a function call of the form
 * <code>functionName(paramter1, parameter2,...,parameterN)</code>. Zero
 * paramters are possible.
 *
 * @author Joachim Kuhnert
 */
public class Function extends Expression {

	/** The name of the function */
	Expression identifier;
	/** The function parameters */
	ArrayList<Expression> arguments = new ArrayList<Expression>();;

	/**
	 * Creates a function call with no parameter: functionName()
	 *
	 * @param name
	 *           The identifier of the function
	 */
	public Function(Expression name) {
		this.identifier = name;
	}

	/**
	 * Creates a function call with the given parameters.
	 *
	 * @param name
	 *           The identifier of the function
	 * @param args
	 *           The function parameters in order
	 */
	public Function(Expression name, ArrayList<Expression> args) {
		this.identifier = name;
		this.arguments = args;
	}

	/**
	 * @return The list of function parameters in order
	 */
	public List<Expression> getArguments() {
		return arguments;
	}

	/**
	 * @return The function name.
	 */
	public Expression getIdentifier() {
		return identifier;
	}

	@Override
	public String toString() {
		String result = "";
		if (identifier != null) {
			result = identifier.toString();
		}
		result += "<";
		if (!arguments.isEmpty()) {
			for (Expression exp : arguments) {
				result += exp.toString() + ", ";
			}
			result = result.substring(0, result.length() - 2);
		}
		result += ">";
		return result;
	}
}
