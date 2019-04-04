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
 * A transition String has the form
 * <code> events[conditions]{condition_actions}/transition_actions </code>. All
 * parts of the transition string are optional.
 *
 * @see http
 *      ://www.mathworks.com/help/toolbox/stateflow/ug/f0-76034.html#f0-123134
 *
 * @author Joachim Kuhnert
 */
public class TransitionParseResult {

	/** All events that causes the transition to be taken */
	private ArrayList<Expression> eventtriggers;
	/** All guard of the transition */
	private Expression condition;
	/** All actions are executed if the guard evaluates to true */
	private ArrayList<Expression> condition_actions;
	/** All actions which will be executed after the transaction was really taken */
	private ArrayList<Expression> transition_actions;

	/**
	 * Creates an new parsed transition.
	 *
	 * @param events
	 *           all trigger events of the transition
	 * @param cond
	 *           the guard of the transition
	 * @param condActions
	 *           the condition actions oft the transition
	 * @param transAction
	 *           the transition actions of the transition
	 */
	public TransitionParseResult(ArrayList<Expression> events, Expression cond,
			ArrayList<Expression> condActions, ArrayList<Expression> transAction) {
		eventtriggers = events;
		condition = cond;
		condition_actions = condActions;
		transition_actions = transAction;
	}

	/**
	 * @return the eventtriggers
	 */
	public ArrayList<Expression> getEventtriggers() {
		return eventtriggers;
	}

	/**
	 * @return the conditions
	 */
	public Expression getConditions() {
		return condition;
	}

	/**
	 * @return the condition_actions
	 */
	public ArrayList<Expression> getCondition_actions() {
		if (condition_actions == null) {
			condition_actions = new ArrayList<Expression>();
		}
		return condition_actions;
	}

	/**
	 * @return the transition_actions
	 */
	public ArrayList<Expression> getTransition_actions() {
		if (transition_actions == null) {
			transition_actions = new ArrayList<Expression>();
		}
		return transition_actions;
	}

	@Override
	public String toString() {
		String res = "Transition \n" + "Events: \n";
		for (Expression e : eventtriggers) {
			res += "\t" + e + "\n";
		}
		res += "Condition: \n";
		if (condition == null) {
			res += "\t null \n";
		} else {
			res += "\t" + condition + "\n";
		}
		res += "Condition Actions: \n";
		for (Expression e : condition_actions) {
			res += "\t" + e + "\n";
		}
		res += "Transition Actions: \n";
		for (Expression e : transition_actions) {
			res += "\t" + e + "\n";
		}
		return res;
	}

}
