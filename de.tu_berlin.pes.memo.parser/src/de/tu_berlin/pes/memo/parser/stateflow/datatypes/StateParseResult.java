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
 * A State String has the form <br>
 * <br>
 * <code>StateName/ \n <br>
 * action \n <br>
 * action \n <br>
 * ...<br>
 * action \n </code><br>
 * <br>
 * where the slash after the name of the state is optional and every action of
 * the list could be an <code>entry action, during action,
 * exit action, on event_name action</code> or a <code>bind action</code>.
 *
 * @see http
 *      ://www.mathworks.com/help/toolbox/stateflow/ug/f0-76034.html#f0-128473
 *
 * @author Joachim Kuhnert
 *
 */
public class StateParseResult {

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the actions
	 */
	public ArrayList<StateAction> getActions() {
		return actions;
	}

	/**
	 * @return the entryActions
	 */
	public ArrayList<StateAction> getEntryActions() {
		return entryActions;
	}

	/**
	 * @return the duringActions
	 */
	public ArrayList<StateAction> getDuringActions() {
		return duringActions;
	}

	/**
	 * @return the exitActions
	 */
	public ArrayList<StateAction> getExitActions() {
		return exitActions;
	}

	/**
	 * @return the onActions
	 */
	public ArrayList<StateAction> getOnActions() {
		return onActions;
	}

	/**
	 * @return the bindActions
	 */
	public ArrayList<StateAction> getBindActions() {
		return bindActions;
	}

	/** The name of the state */
	String name = "";
	/** All available actions of the state */
	ArrayList<StateAction> actions = new ArrayList<StateAction>();

	/** All entry actions of the state */
	ArrayList<StateAction> entryActions = new ArrayList<StateAction>();
	/** All during actions of the state */
	ArrayList<StateAction> duringActions = new ArrayList<StateAction>();
	/** All exit actions of the state */
	ArrayList<StateAction> exitActions = new ArrayList<StateAction>();
	/** All on actions of the state */
	ArrayList<StateAction> onActions = new ArrayList<StateAction>();
	/** All bind actions of the state */
	ArrayList<StateAction> bindActions = new ArrayList<StateAction>();

	/**
	 * Creates a new completely parsed state object.
	 *
	 * @param id
	 *           The name of the state.
	 * @param actions
	 *           The list of actions of the state.
	 */
	public StateParseResult(String id, ArrayList<StateAction> actions) {
		name = id;

		if (actions == null) {
			return;
		}

		this.actions = actions;

		for (StateAction action : actions) {
			switch (action.getActionType()) {
				case ENTRY:
					onActions.add(action);
					break;
				case DURING:
					onActions.add(action);
					break;
				case EXIT:
					onActions.add(action);
					break;
				case ON:
					onActions.add(action);
					break;
				case BIND:
					onActions.add(action);
					break;
			}
		}

	}

	@Override
	public String toString() {
		String result = "";
		if (name != null) {
			result += name + "\n";
		}
		if (actions != null) {
			for (StateAction sa : actions) {
				result += sa.toString() + "; \n";
			}
		}
		return result;
	}

}
