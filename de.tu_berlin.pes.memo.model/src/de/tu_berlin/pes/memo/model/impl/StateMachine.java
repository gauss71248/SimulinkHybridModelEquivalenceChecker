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
import de.tu_berlin.pes.memo.model.util.StateflowParameterConstants;

/**
 * Covers all <code>StateCharts</code>.
 * 
 * @author Robert Reicherdt, Joachim Kuhnert
 */
public class StateMachine extends StateflowItem {

	/**
	 * The name of the state machine.
	 * 
	 * @uml.property name="name"
	 */
	private String name;
	/**
	 * All belonging <code>StateCharts</code>.
	 * 
	 * @uml.property name="stateflowCharts"
	 */
	private Set<StateflowChart> stateflowCharts = new HashSet<StateflowChart>();

	/**
	 * Standard constructor for a StateMachine. The StateMahcine will have id =
	 * -1, sfid = -1 and parameters = null.
	 * 
	 * @see StateflowItem#StateflowItem(int, int, MDLSection)
	 */
	public StateMachine() {
		super(-1, -1, null);
	}

	/**
	 * Creates a new StateMachine.
	 *
	 * @param section
	 *           The model section of the StateMachine.
	 * @param id
	 *           The unique id for all model items.
	 */
	public StateMachine(MDLSection section, int id) {
		super(id, Integer.parseInt(section.getParameter(StateflowParameterConstants.SF_ID_STRING)),
				section);
		this.name = section.getParameter(StateflowParameterConstants.SF_NAME_STRING);
	}

	/**
	 * Get the name of the state machine
	 *
	 * @return The name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the state machine.
	 *
	 * @param name
	 *           the new name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get all state charts of the state machine.
	 *
	 * @return The Set of state charts.
	 */
	public Set<StateflowChart> getStateflowCharts() {
		return stateflowCharts;
	}

	/**
	 * Set the charts belonging to this state machine.
	 *
	 * @param stateflowCharts
	 *           The new set of charts
	 */
	public void setStateflowCharts(Set<StateflowChart> stateflowCharts) {
		this.stateflowCharts = stateflowCharts;
	}

	/**
	 * Adds a single chart to the set of charts belonging to the state machine.
	 *
	 * @param chart
	 *           The chart to add.
	 */
	public void addStateflowChart(StateflowChart chart) {
		this.stateflowCharts.add(chart);
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

}
