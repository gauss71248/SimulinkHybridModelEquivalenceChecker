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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.conqat.lib.simulink.builder.MDLSection;

import de.tu_berlin.pes.memo.MeMoPlugin;

/**
 * Represents the whole simulink model with all its parts.
 * 
 * @author Robert Reicherd, Joachim Kuhnert
 */
public class Model extends SimulinkItem {

	// /**
	// * The id of the model.
	// * @uml.property name="modelId"
	// */
	// private int modelId = 1;
	/**
	 * The last assigned id.
	 */
	private int idcnt = 2;
	/**
	 * The name of the model.
	 * 
	 * @uml.property name="name"
	 */
	private String name;
	/**
	 * All blocks of the model.
	 * 
	 * @uml.property name="blocks"
	 */
	private Set<Block> blocks = new HashSet<Block>();
	/**
	 * All lines between blocks of the model.
	 * 
	 * @uml.property name="signalLines"
	 */
	private Set<SignalLine> signalLines = new HashSet<SignalLine>();
	/**
	 * The Configuration parts of the Model.
	 * 
	 * @uml.property name="configurations"
	 */
	private Set<Configuration> configurations = new HashSet<Configuration>();
	/**
	 * The statemachine of the model, if one present
	 * 
	 * @uml.property name="stateMachine"
	 * @uml.associationEnd
	 */
	private Set<StateMachine> stateMachines = new HashSet<StateMachine>();

	/**
	 * If this model is from a model reference, this value should be true.
	 */
	private boolean reference = false;
	/**
	 * All <code>SignalLines</code> of the model accessible by their hash code.
	 * 
	 * @uml.property name="signal"
	 */
	private HashMap<Integer, SignalLine> signalMap = new HashMap<Integer, SignalLine>();
	/**
	 * All <code>Blocks</code> of the model accessible by their hash code.
	 * 
	 * @uml.property name="block"
	 */
	private HashMap<Integer, Block> blockMap = new HashMap<Integer, Block>();
	/**
	 * All section of the AST with <code>DEFAULT_SECTION_TYPE_POSTFIX</code>
	 * 
	 * @uml.property name="defaultSections"
	 */
	private HashMap<String, MDLSection> defaultSections = new HashMap<String, MDLSection>();

	/**
	 * All the variables from the MATLab workspace.
	 */
	private Set<WSVariable> workspaceVariables = new HashSet<WSVariable>();

	/**
	 * Standard constructor creates a model with id -1.
	 */
	public Model() {
		super(-1, null);
	}
	
	/**
	 * A basic constructor, which is useful mainly as an entry-point for extensions of this class. //TODO
	 * 
	 * @param oldModel
	 */
	public Model(Model oldModel) {
		super(-1, null);
		this.idcnt = oldModel.idcnt;
		this.name = oldModel.name;
		this.blocks = oldModel.blocks;
		this.signalLines = oldModel.signalLines;
		this.configurations = oldModel.configurations;
		this.stateMachines = oldModel.stateMachines;
		this.reference = oldModel.reference;
		this.signalMap = oldModel.signalMap;
		this.blockMap = oldModel.blockMap;
		this.defaultSections = oldModel.defaultSections;
		this.workspaceVariables = oldModel.workspaceVariables;
	}

	// /**
	// * @return
	// * @uml.property name="modelId"
	// */
	// public int getModelId() {
	// return modelId;
	// }

	/**
	 * Set the id of the model.
	 *
	 * @param id
	 *            The id to set.
	 * @uml.property name="modelId"
	 */
	@Override
	public void setId(int id) {
		super.setId(id);
		this.idcnt = id + 1;
	}

	/**
	 * Get a fresh ID for a modelitem.
	 *
	 * The function just increases an internal counter by 1 and returns the
	 * value.
	 *
	 * @return last ID + 1
	 */
	public int nextID() {
		return idcnt++;
	}
	
	/**
	 * Get the SignalLines of the model mapped by their id.
	 *
	 * @return The SignalLines.
	 * @uml.property name="signal"
	 */
	public HashMap<Integer, SignalLine> getSignalMap() {
		if (signalMap.isEmpty()) {
			for (SignalLine sl : signalLines) {
				signalMap.put(sl.getId(), sl);
			}
		}
		return signalMap;
	}
	
	/**
	 * Get the Blocks of the model mapped by their id.
	 *
	 * @return All the blocks.
	 * @uml.property name="block"
	 */
	public HashMap<Integer, Block> getBlockMap() {
		if (blockMap.isEmpty()) {
			for (Block b : blocks) {
				blockMap.put(b.getId(), b);
			}
		}
		return blockMap;
	}

	/**
	 * Get the default sections of the model (only available directly after
	 * parsing).
	 *
	 * @return The Map from section name to the default section.
	 * @uml.property name="defaultSections"
	 */
	public HashMap<String, MDLSection> getDefaultSections() {
		return defaultSections;
	}

	/**
	 * Get the name of the model.
	 *
	 * @return The model name.
	 * @uml.property name="name"
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the model.
	 *
	 * @param name
	 *            The new model name.
	 * @uml.property name="name"
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the blocks.
	 *
	 * @return The blocks of the model.
	 * @uml.property name="blocks"
	 */
	public Set<Block> getBlocks() {
		return blocks;
	}

	/**
	 * Set the blocks of the model.
	 *
	 * @param blocks
	 *            The blocks to set.
	 * @uml.property name="blocks"
	 */
	public void setBlocks(Set<Block> blocks) {
		this.blocks = blocks;
	}

	/**
	 * Get the signal lines.
	 *
	 * @return The signal lines of the model.
	 * @uml.property name="signalLines"
	 */
	public Set<SignalLine> getSignalLines() {
		return signalLines;
	}

	/**
	 * Set the signal lines of the model.
	 *
	 * @param signalLines
	 *            The signal lines to set.
	 * @uml.property name="signalLines"
	 */
	public void setSignalLines(Set<SignalLine> signalLines) {
		this.signalLines = signalLines;
	}

	/**
	 * Get the statemachines of the model.
	 *
	 * @return The statemachines.
	 * @uml.property name="stateMachine"
	 */
	public Set<StateMachine> getStateMachines() {
		return stateMachines;
	}

	/**
	 * Set the state machines of the model.
	 *
	 * @param stateMachine
	 *            The statemachines to set.
	 * @uml.property name="stateMachine"
	 */
	public void setStateMachines(Set<StateMachine> stateMachines) {
		this.stateMachines = stateMachines;
	}

	// @Override
	// public String toString() {
	// return id + name;
	// }

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
	 * @param configurations
	 *            the configurations to set
	 * @uml.property name="configurations"
	 */
	public void setConfigurations(Set<Configuration> configurations) {
		this.configurations = configurations;
	}

	/**
	 * @return the configurations
	 * @uml.property name="configurations"
	 */
	public Set<Configuration> getConfigurations() {
		return configurations;
	}

	/**
	 * Get a specific block by its id.
	 *
	 * @param id
	 *            The unique id of the block.
	 * @return
	 */
	public Block getBlockById(int id) {
		return getBlockMap().get(id);
	}

	/**
	 * @return if the model is a referenced model
	 */
	public boolean isReference() {
		return reference;
	}

	/**
	 * @param isReference
	 *            is the a referenced one?
	 */
	public void setReference(boolean reference) {
		this.reference = reference;
	}

	/**
	 * The the id counter for the generation of new ids. To ensure uniqueness of
	 * the ids the new value will only be accepted if its greater than the
	 * current counter.
	 *
	 * @param counter
	 *            The new counter value.
	 * @return True if the new value is assigned, false otherwise.
	 */
	public boolean setIdCounter(int counter) {
		if (idcnt < counter) {
			idcnt = counter;
			return true;
		}
		return false;
	}

	/**
	 * Get a block specified by its {@link Block#getFullQualifiedName(boolean)}.
	 *
	 * @param path
	 *            The path string.
	 * @param withModelName
	 *            Is the first part of the path the model name?
	 * @return the found Block or null.
	 */
	public Block getBlockByPath(String path, boolean withModelName) {
		for (Block b : getBlockMap().values()) {
			if (b.getFullQualifiedName(withModelName).equals(path)) {
				return b;
			}
		}
		return null;
	}

	/**
	 * @return the workspaceVariables
	 */
	public Set<WSVariable> getWorkspaceVariables() {
		return workspaceVariables;
	}

	public WSVariable getWSVariableByName(String name){
		for (WSVariable variable : workspaceVariables){
			if (name.equals(variable.getName())){
				return variable;
			}
		}
		return null;
	}

	
	/**
	 * @param workspaceVariables
	 *            the workspaceVariables to set
	 */
	public void setWorkspaceVariables(Set<WSVariable> workspaceVariables) {
		this.workspaceVariables = workspaceVariables;
	}

}
