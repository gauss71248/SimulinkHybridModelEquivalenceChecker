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
 * Represents the content of one state chart.
 * 
 * @author Robert Reicherdt, Joachim Kuhnert
 */
public class StateflowChart extends StateflowItem {

	/**
	 * The name of the chart.
	 * 
	 * @uml.property name="name"
	 */
	private String name;
	/**
	 * All <code>Junctions</code> of the chart.
	 * 
	 * @uml.property name="junctions"
	 */
	private Set<Junction> junctions = new HashSet<Junction>();
	/**
	 * All <code>States</code> of the chart.
	 * 
	 * @uml.property name="states"
	 */
	private Set<State> states = new HashSet<State>();
	/**
	 * All <code>Transitions</code> of the chart.
	 * 
	 * @uml.property name="transitions"
	 */
	private Set<Transition> transitions = new HashSet<Transition>();
	/**
	 * All <code>Events</code> of the chart.
	 * 
	 * @uml.property name="events"
	 */
	private Set<Event> events = new HashSet<Event>();
	/**
	 * All <code>Data</code> of the chart.
	 * 
	 * @uml.property name="data"
	 */
	private Set<Data> data = new HashSet<Data>();

	/**
	 * All local <code>Data</code> of the chart.
	 * 
	 * @uml.property name="localData"
	 */
	private Set<Data> localData = new HashSet<Data>();

	/**
	 * All local <code>Event</code> of the chart.
	 * 
	 * @uml.property name="localEvents"
	 */
	private Set<Event> localEvents = new HashSet<Event>();

	/**
	 * All chartBlocks that reference this chart.
	 */
	private Set<ChartBlock> chartBlocks = new HashSet<ChartBlock>();

	/**
	 * Creates a new stateflow chart.
	 *
	 * @param section
	 *           The model section of the chart.
	 * @param id
	 *           The unique id for all model items.
	 */
	public StateflowChart(MDLSection section, int id) {
		super(id, Integer.parseInt(section.getParameter(StateflowParameterConstants.SF_ID_STRING)),
				section);
		this.name = section.getParameter(StateflowParameterConstants.SF_NAME_STRING);
	}

	/**
	 * Standard constructor for a stateflow chart. Chart will have id = -1, sfid
	 * = -1 and parameters = null.
	 * 
	 * @see StateflowItem#StateflowItem(int, int, MDLSection)
	 */
	public StateflowChart() {
		super(-1, -1, null);
	}

	/**
	 * @return The name of the chart.
	 * @uml.property name="name"
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set an new name for the chart.
	 *
	 * @param name
	 *           The new name.
	 * @uml.property name="name"
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get all junctions belonging to this chart.
	 *
	 * @return The set of junctions.
	 * @uml.property name="junctions"
	 */
	public Set<Junction> getJunctions() {
		return junctions;
	}

	/**
	 * Set the junctions of this chart.
	 *
	 * @param junctions
	 *           The new set of junctions.
	 * @uml.property name="junctions"
	 */
	public void setJunctions(Set<Junction> junctions) {
		this.junctions = junctions;
	}

	/**
	 * get all states belonging to this chart.
	 *
	 * @return The Set of states.
	 * @uml.property name="states"
	 */
	public Set<State> getStates() {
		return states;
	}

	/**
	 * Set the states belonging to this chart.
	 *
	 * @param states
	 *           The new set of states.
	 * @uml.property name="states"
	 */
	public void setStates(Set<State> states) {
		this.states = states;
	}

	/**
	 * Get the transitions belonging to this chart.
	 *
	 * @return The ste of transitions.
	 * @uml.property name="transitions"
	 */
	public Set<Transition> getTransitions() {
		return transitions;
	}

	/**
	 * Set the transitions belonging to this chart.
	 *
	 * @param transitions
	 *           The new set of transitions.
	 * @uml.property name="transitions"
	 */
	public void setTransitions(Set<Transition> transitions) {
		this.transitions = transitions;
	}

	/**
	 * get the events belonging to this chart.
	 *
	 * @return The events
	 * @uml.property name="events"
	 */
	public Set<Event> getEvents() {
		return events;
	}

	/**
	 * Set the events belonging to this chart.
	 *
	 * @param events
	 *           The new set of events.
	 * @uml.property name="events"
	 */
	public void setEvents(Set<Event> events) {
		this.events = events;
	}

	/**
	 * Get the data belonging to this chart.
	 *
	 * @return The data objects.
	 * @uml.property name="data"
	 */
	public Set<Data> getData() {
		return data;
	}

	/**
	 * Set the data objects belonging to this chart.
	 *
	 * @param data
	 *           The new set of data objects.
	 * @uml.property name="data"
	 */
	public void setData(Set<Data> data) {
		this.data = data;
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
	 * Set the local data objects of this chart.
	 *
	 * @param localData
	 *           the new localData to set
	 * @uml.property name="localData"
	 */
	public void setLocalData(Set<Data> localData) {
		this.localData = localData;
	}

	/**
	 * Get the local data objects of this chart.
	 *
	 * @return the localData
	 * @uml.property name="localData"
	 */
	public Set<Data> getLocalData() {
		return localData;
	}

	/**
	 * Set the local events of this chart.
	 *
	 * @param localEvents
	 *           the new localEvents to set
	 * @uml.property name="localEvents"
	 */
	public void setLocalEvents(Set<Event> localEvents) {
		this.localEvents = localEvents;
	}

	/**
	 * Get the local events belonging to this chart.
	 *
	 * @return the localEvents
	 * @uml.property name="localEvents"
	 */
	public Set<Event> getLocalEvents() {
		return localEvents;
	}

	/**
	 * Get the blocks representing this chart in the simulink model.
	 *
	 * @return the chartBlocks
	 */
	public Set<ChartBlock> getChartBlocks() {
		return chartBlocks;
	}

	/**
	 * Set the blocks representing this chart in the simulink model.
	 *
	 * @param chartBlocks
	 *           the new chartBlocks to set
	 */
	public void setChartBlocks(Set<ChartBlock> chartBlocks) {
		this.chartBlocks = chartBlocks;
	}

}
