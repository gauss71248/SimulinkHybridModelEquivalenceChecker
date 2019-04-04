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
 * On actions are only executed if the on event occurs. The form of an on action
 * is <code>on event_name: Action</code> unlike the other Actions.
 *
 * @see StateAction
 * @see http
 *      ://www.mathworks.com/help/toolbox/stateflow/ug/f0-76034.html#f0-128473
 * @author Joachim Kuhnert
 */
public class StateOnAction extends StateAction {
	Expression onExp;

	/**
	 * Creates a new state on-action.
	 *
	 * @param onExp
	 *           The guard of the action.
	 * @param expr
	 *           The expression what the action should do
	 */
	public StateOnAction(Expression onExp, Expression expr) {
		super(StateActionTypes.ON, expr);
		this.onExp = onExp;
	}

	@Override
	public String toString() {
		String result = "";
		result += type.toString() + " Expression ON{" + onExp.toString() + "}: " + expr.toString();
		return result;
	}

}
