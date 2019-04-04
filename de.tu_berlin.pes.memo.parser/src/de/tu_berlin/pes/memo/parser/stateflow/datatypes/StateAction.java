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

/**
 * A State action is one entry of the action list of a State. It could be an
 * <code>entry action, during action, exit action, on event_name
 *  action</code> or a <code>bind action</code>. The actions looks like
 * <code>eventtype: action</code> only on actions are slightly different.
 *
 * @see http://www.mathworks.com/help/toolbox/stateflow/ug/f0-76034.html
 * @see StateOnAction
 * @author Joachim Kuhnert
 *
 */
public class StateAction {

	/** What the action actually do */
	Expression expr;
	/** The kind of the action */
	StateActionTypes type;

	/**
	 * Creates a new state action object with the given parameters.
	 *
	 * @param type
	 *           The type of the action: Entry, During, Exit, Bind or On
	 * @param expr
	 *           The expression what the action should do
	 */
	public StateAction(StateActionTypes type, Expression expr) {
		this.expr = expr;
		this.type = type;
	}

	/**
	 * @return The type of the action.
	 */
	public StateActionTypes getActionType() {
		return type;
	}

	@Override
	public String toString() {
		String result = "";
		result += type.toString() + " Expression: " + expr.toString();
		return result;
	}

	public Expression getExpression() {
		return expr;
	}
}