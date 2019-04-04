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

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.conqat.lib.simulink.builder.MDLSection;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.util.ModelHelper;
import de.tu_berlin.pes.memo.model.util.SimulinkSectionConstants;
import de.tu_berlin.pes.memo.model.util.StateflowParameterConstants;

/**
 * Join and branch points of transitions.
 * 
 * @author Robert Reicherdt, Joachim Kuhnert
 */
public class Junction extends StateflowItem {

	/**
	 * Overlaying State
	 * 
	 * @uml.property name="parentId"
	 */
	private int parentId;
	/**
	 * Incoming <code>Transitions</code>.
	 * 
	 * @uml.property name="inTransitions"
	 */
	private Set<Transition> inTransitions = new HashSet<Transition>();
	/**
	 * Outgoing transitions.
	 * 
	 * @uml.property name="outTransitions"
	 */
	private Set<Transition> outTransitions = new HashSet<Transition>();

	/**
	 * Standard constructor creates a Junction with sfid -1 and id -1.
	 * 
	 * @see StateflowItem#StateflowItem(int, int, MDLSection)
	 */
	public Junction() {
		super(-1, -1, null);

	}

	/**
	 * Creates a new junction with the parameters given by the MDLSection.
	 *
	 * @param section
	 *           the section of the junction of the AST
	 */
	public Junction(MDLSection section, int id) {
		super(id, Integer.parseInt(section.getParameter(StateflowParameterConstants.SF_ID_STRING)),
				section);
		this.parentId = ModelHelper.getParentID(section
				.getParameter(StateflowParameterConstants.SF_LINKNODE_STRING));
	}

	/**
	 * Get the id of the parent of the junction.
	 *
	 * @return the parent id
	 * @uml.property name="parentId"
	 */
	public int getParentId() {
		return parentId;
	}

	/**
	 * Set the parent id.
	 *
	 * @param parentId
	 *           The new parent id.
	 * @uml.property name="parentId"
	 */
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

	/**
	 * Get all at this junction joining transitions.
	 *
	 * @return The incoming transitions.
	 * @uml.property name="inTransitions"
	 */
	public Set<Transition> getInTransitions() {
		return inTransitions;
	}

	/**
	 * Set the incoming transitions.
	 *
	 * @param inTransitions
	 *           The new incoming transitions.
	 * @uml.property name="inTransitions"
	 */
	public void setInTransitions(Set<Transition> inTransitions) {
		this.inTransitions = inTransitions;
	}

	/**
	 * Get the outgoing transitions.
	 *
	 * @return The leaving transitions.
	 * @uml.property name="outTransitions"
	 */
	public Set<Transition> getOutTransitions() {
		return outTransitions;
	}

	/**
	 * Set the outgoing transitions.
	 *
	 * @param outTransitions
	 *           The new leaving transitions.
	 * @uml.property name="outTransitions"
	 */
	public void setOutTransitions(Set<Transition> outTransitions) {
		this.outTransitions = outTransitions;
	}

	@Override
	public String toString() {
		String rslt = this.getClass().getSimpleName() + ":";
		for (Field f : this.getClass().getDeclaredFields()) {
			try {
				rslt += "(" + f.getName() + "," + f.get(this) + ")";
			} catch (IllegalArgumentException e) {
				MeMoPlugin.logException(e.toString(), e);
			} catch (IllegalAccessException e) {
				MeMoPlugin.logException(e.toString(), e);
			}
		}
		return rslt;
	}

	/**
	 * Get the id of the chart this junction belongs to.
	 *
	 * @return
	 */
	public int getChartId() {
		return Integer.parseInt(getParameter().get(SimulinkSectionConstants.SF_CHART_SECTION_TYPE));
	}

}
