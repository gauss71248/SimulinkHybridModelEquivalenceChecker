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

import org.conqat.lib.simulink.builder.MDLSection;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.util.ModelHelper;
import de.tu_berlin.pes.memo.model.util.SimulinkSectionConstants;
import de.tu_berlin.pes.memo.model.util.StateflowParameterConstants;

/**
 * The directed link between states. A condition indicates, if a
 * <code>Transition</code> could be taken.
 * 
 * @author Joachim Kuhnert
 */
public class Transition extends StateflowItem {

	/**
	 * The label of the transition and at once the guard.
	 * 
	 * @uml.property name="name"
	 */
	private String label;
	/**
	 * The hash of the overlaying state, that covers the transition.
	 * 
	 * @uml.property name="parentId"
	 */
	private int parentId;
	/**
	 * @uml.property name="dst"
	 */
	private StateflowItem dst = null;
	/**
	 * @uml.property name="src"
	 */
	private StateflowItem src = null;

	/**
	 * Standard constructor for a transition. Transition will have id = -1, sfid
	 * = -1 and parameters = null.
	 * 
	 * @see StateflowItem#StateflowItem(int, int, MDLSection)
	 */
	public Transition() {
		super(-1, -1, null);
	}

	/**
	 * Creates a new Transition.
	 *
	 * @param section
	 *           The model section of the transition.
	 * @param id
	 *           The unique id for all model items.
	 */
	public Transition(MDLSection section, int id) {
		super(id, Integer.parseInt(section.getParameter(StateflowParameterConstants.SF_ID_STRING)),
				section);
		this.parentId = ModelHelper.getParentID(section
				.getParameter(StateflowParameterConstants.SF_LINKNODE_STRING));

		this.label = section.getParameter(StateflowParameterConstants.SF_LABEL_STRING);
	}

	/**
	 * Get the label of the transition.
	 *
	 * @return The label of the transition
	 */
	public String getLabel() {
		if (label == null) {
			label = getParameter().get(StateflowParameterConstants.SF_LABEL_STRING);
		}
		return label;
	}

	/**
	 * Set the label of the Transition.
	 *
	 * @param label
	 *           The new label.
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	

	@Override
	public String toString() {
		return "Transition [label=" + label + ", " + src + " -> " + dst + "]";
	}

	/**
	 * Get the sfid of the chart this transition belongs to.
	 *
	 * @return The chart id.
	 */
	public int getChartId() {

		return Integer.parseInt(getParameter().get(SimulinkSectionConstants.SF_CHART_SECTION_TYPE));
	}

	/**
	 * Get the id of the parent of the transition. Could be the same as the
	 * chartID but also can be the id of a state that contains the transition.
	 *
	 * @return
	 */
	public int getParentId() {
		if (parentId == 0) {
			this.parentId = ModelHelper.getParentID(getParameter().get(
					StateflowParameterConstants.SF_LINKNODE_STRING));
		}
		return parentId;
	}

	/**
	 * Set the parent id.
	 *
	 * @param parentId
	 *           The parent id.
	 */
	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

	/**
	 * Get the target of the transition.
	 *
	 * @return the transition destination.
	 */
	public StateflowItem getDst() {
		return dst;
	}

	/**
	 * @param dst
	 *           the target to set
	 */
	public void setDst(StateflowItem dst) {
		this.dst = dst;
	}

	/**
	 * @return the origin of the transition
	 */
	public StateflowItem getSrc() {
		return src;
	}

	/**
	 * @param src
	 *           the origin of the transition to set
	 */
	public void setSrc(StateflowItem src) {
		this.src = src;
	}

	/**
	 * Get the id of the origin stateflow object of the transition.
	 *
	 * @return The origin id
	 */
	public int getSrcId() {
		return src != null ? src.getStateFlowId() : 0;
	}

	/**
	 * Get the id of the target stateflow object of the transition.
	 *
	 * @return The target id
	 */
	public int getDstId() {
		return dst != null ? dst.getStateFlowId() : 0;
	}
	
//	/**
//	 * @return
//	 * @uml.property  name="name"
//	 */
//	public String getName() {
//		if(name == null)
//			name = getParameter().get(StateflowParameterConstants.SF_LABEL_STRING);
//		return name;
//	}


}
