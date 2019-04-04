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
 * This class represents the state of a simulink stateflow.
 * 
 * @author Robert Reicherdt, Joachim Kuhnert
 */
public class State extends StateflowItem {

	/**
	 * The name of the state.
	 * 
	 * @uml.property name="name"
	 */
	private String name;
	/**
	 * The hash of the overlaying state
	 * 
	 * @uml.property name="parentId"
	 */
	private int parentId;
	/**
	 * All directly underlying <code>Transitions</code> of the state
	 * 
	 * @uml.property name="childTransitions"
	 */
	private Set<Transition> childTransitions = new HashSet<Transition>();
	/**
	 * All directly underlying <code>States</state> of the state
	 * 
	 * @uml.property name="childStates"
	 */
	private Set<State> childStates = new HashSet<State>();
	/**
	 * All directly underlying <code>Junctions</code> of the state
	 * 
	 * @uml.property name="childJunctions"
	 */
	private Set<Junction> childJunctions = new HashSet<Junction>();
	/**
	 * All incoming <code>Transitions</code>.
	 * 
	 * @uml.property name="inTransitions"
	 */
	private Set<Transition> inTransitions = new HashSet<Transition>();
	/**
	 * All outgoing <code>Transitions</code>.
	 * 
	 * @uml.property name="outTransitions"
	 */
	private Set<Transition> outTransitions = new HashSet<Transition>();
	/**
	 * The by the state triggered events.
	 * 
	 * @uml.property name="localEvents"
	 */
	private Set<Event> localEvents = new HashSet<Event>();
	/**
	 * The by the state used data.
	 * 
	 * @uml.property name="localData"
	 */
	private Set<Data> localData = new HashSet<Data>();

	/**
	 * The standard constructor creates an empty state with id = -1 and sfid =
	 * -1.
	 *
	 * @see StateflowItem#StateflowItem(int, int, MDLSection)
	 */
	public State() {
		super(-1, -1, null);

	}

	/**
	 * Creates a new State.
	 *
	 * @param section
	 *            The model section of the State.
	 * @param id
	 *            The unique id for all model items.
	 */
	public State(MDLSection section, int id) {
		super(id, Integer.parseInt(section.getParameter(StateflowParameterConstants.SF_ID_STRING)), section);
		this.name = section.getParameter(StateflowParameterConstants.SF_LABEL_STRING);
		this.parentId = ModelHelper.getParentID(section.getParameter(StateflowParameterConstants.SF_TREENODE_STRING));
	}

	/**
	 * Get the name.
	 *
	 * @return The name.
	 * @uml.property name="name"
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name.
	 *
	 * @param name
	 *            The new name.
	 * @uml.property name="name"
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the id of the parent of the State.
	 *
	 * @return The parent id.
	 * @uml.property name="parentId"
	 */
	public int getParentId() {
		return parentId;
	}

	/**
	 * Set the id of the parent of the State.
	 *
	 * @param parent
	 *            The new parent id.
	 * @uml.property name="parentId"
	 */
	public void setParentId(int parent) {
		this.parentId = parent;
	}

	/**
	 * Get the transitions covered by this state.
	 *
	 * @return
	 * @uml.property name="childTransitions"
	 */
	public Set<Transition> getChildTransitions() {
		return childTransitions;
	}

	/**
	 * Set the transistions that are covered by the state.
	 *
	 * @param childTransitions
	 *            The covered transistions.
	 * @uml.property name="childTransitions"
	 */
	public void setChildTransitions(Set<Transition> childTransitions) {
		this.childTransitions = childTransitions;
	}

	/**
	 * Get the states covered by this state.
	 *
	 * @return The covered states.
	 * @uml.property name="childStates"
	 */
	public Set<State> getChildStates() {
		return childStates;
	}

	/**
	 * Set the states covered by this state.
	 *
	 * @param childStates
	 *            the covered states to set.
	 * @uml.property name="childStates"
	 */
	public void setChildStates(Set<State> childStates) {
		this.childStates = childStates;
	}

	/**
	 * Get the Junctions covered by this state.
	 *
	 * @return The covered Junctions.
	 * @uml.property name="childJunctions"
	 */
	public Set<Junction> getChildJunctions() {
		return childJunctions;
	}

	/**
	 * Set the Junctions covered by this state.
	 *
	 * @param childJunctions
	 *            The covered Junctions to set.
	 * @uml.property name="childJunctions"
	 */
	public void setChildJunctions(Set<Junction> childJunctions) {
		this.childJunctions = childJunctions;
	}

	/**
	 * Get all incoming transitions.
	 *
	 * @return The transitions which have this state as target.
	 * @uml.property name="inTransitions"
	 */
	public Set<Transition> getInTransitions() {
		return inTransitions;
	}

	/**
	 * Set the Transitions which have this state as target.
	 *
	 * @param inTransitions
	 *            The incoming transitions to set.
	 * @uml.property name="inTransitions"
	 */
	public void setInTransitions(Set<Transition> inTransitions) {
		this.inTransitions = inTransitions;
	}

	/**
	 * Get the local events of this state.
	 *
	 * @return The local events.
	 * @uml.property name="localEvents"
	 */
	public Set<Event> getLocalEvents() {
		return localEvents;
	}

	/**
	 * Set the local events of the State.
	 *
	 * @param localEvents
	 *            The local events to set.
	 * @uml.property name="localEvents"
	 */
	public void setLocalEvents(Set<Event> localEvents) {
		this.localEvents = localEvents;
	}

	/**
	 * get the local data of the state.
	 *
	 * @return The local data.
	 * @uml.property name="localData"
	 */
	public Set<Data> getLocalData() {
		return localData;
	}

	/**
	 * Set the local Data of this state.
	 *
	 * @param localData
	 *            The local data to set.
	 * @uml.property name="localData"
	 */
	public void setLocalData(Set<Data> localData) {
		this.localData = localData;
	}

	/**
	 * Get all leaving transitions.
	 *
	 * @return The outgoing transitions.
	 * @uml.property name="outTransitions"
	 */
	public Set<Transition> getOutTransitions() {
		return outTransitions;
	}

	/**
	 * Set the leaving transitions.
	 *
	 * @param outTransitions
	 *            The outgoing transitions to set.
	 * @uml.property name="outTransitions"
	 */
	public void setOutTransitions(Set<Transition> outTransitions) {
		this.outTransitions = outTransitions;
	}

	@Override
	public String toString() {
		String rslt = this.getClass().getSimpleName() + ":" + this.getId() + ":" + this.getNameWithoutFunctionality();
		
		if (this.getAffectedVariables() != null) {
			rslt += " Variables: ";
			rslt += this.getAffectedVariables();
		}
//		rslt = rslt
		// for (Field f : this.getClass().getDeclaredFields()) {
		// try {
		// rslt += "(" + f.getName() + "," + f.get(this) + ")";
		// } catch (IllegalArgumentException e) {
		// MeMoPlugin.logException(e.toString(), e);
		// } catch (IllegalAccessException e) {
		// MeMoPlugin.logException(e.toString(), e);
		// }
		// }
		return rslt;
	}

	/**
	 * get the id of the chart this state belongs to.
	 *
	 * @return The chart id.
	 */
	public int getChartId() {
		return Integer.parseInt(getParameter().get(SimulinkSectionConstants.SF_CHART_SECTION_TYPE));
	}

	/**
	 * returns the name of the state until the first line break meaning: only
	 * the name is returned, not the functionality usually provided using entry,
	 * during, etc.
	 * 
	 * @return the name of the state only, no functionality syntax
	 */
	public String getNameWithoutFunctionality() {
		String[] resultSplit = this.getName().split("\\\\n");
		String rslt = null;
		if (resultSplit.length > 0) {
			rslt = this.getName().split("\\\\n")[0];
		}
		return rslt;
	}

	/**
	 * returns the set of variables set in a state
	 * 
	 * @param state
	 *            the state to work on
	 * @return the Set<String> of variables the state under analysis changes
	 */
	public Set<String> getAffectedVariables() {
		String[] resultSplit = this.getName().split("\\\\n");
		if (resultSplit.length > 0) {

			Set<String> resultSet = new HashSet<String>();
			for (String currentLine : resultSplit) {
				if (currentLine.contains("entry")) {
					// TODO parse properly
					String[] innerSplit = currentLine.split(":|=");
					if (innerSplit.length > 2) {
						resultSet.add(innerSplit[1].trim());
					}
				}
			}
			return resultSet;
		}
		return null;
	}

}
