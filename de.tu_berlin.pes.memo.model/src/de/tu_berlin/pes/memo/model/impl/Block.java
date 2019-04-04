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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.conqat.lib.simulink.builder.MDLSection;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.util.SimulinkParameterNames;
import de.tu_berlin.pes.memo.model.util.SimulinkSectionConstants;

/**
 * Represents a Simulink Block.
 *
 * @author Robert Reicherdt, Joachim Kuhnert
 * @see ModelItem
 */
public class Block extends SimulinkItem {

	private static final List<String> CONTROL_BLOCKS = Arrays.asList(new String[]{"Switch", "MultiPortSwitch"});
	
	/**
	 * Layer to which the block belongs.
	 *
	 * @uml.property name="level"
	 */
	private Integer level = 0;
	/**
	 * Hash of the overlaying subsystem or 1 if on first layer (level = 1).
	 *
	 * @uml.property name="parent"
	 * @uml.associationEnd
	 */
	private ModelItem parent;

	private Model parentModel;

	/**
	 * The name of the block
	 *
	 * @uml.property name="name"
	 */
	private String name = "";
	/**
	 * The type of the block
	 *
	 * @uml.property name="type"
	 */
	private String type = "";

	private HashMap<Integer, Port> inPortsMap = new HashMap<Integer, Port>();
	private HashMap<Integer, Port> outPortsMap = new HashMap<Integer, Port>();
	private HashMap<Integer, Port> lConnPortsMap = new HashMap<Integer, Port>();
	private HashMap<Integer, Port> rConnPortsMap = new HashMap<Integer, Port>();

	/**
	 * @uml.property name="inPorts"
	 */
	private Set<Port> inPorts = new HashSet<Port>();
	/**
	 * @uml.property name="outPorts"
	 */
	private Set<Port> outPorts = new HashSet<Port>();
	/**
	 * @uml.property name="enablePort"
	 * @uml.associationEnd
	 */
	private Port enablePort = null;
	/**
	 * @uml.property name="triggerPort"
	 * @uml.associationEnd
	 */
	private Port triggerPort = null;
	/**
	 * @uml.property name="statePort"
	 * @uml.associationEnd
	 */
	private Port statePort = null;
	/**
	 * @uml.property name="lConnPorts"
	 */
	private Set<Port> lConnPorts = new HashSet<Port>();
	/**
	 * @uml.property name="rConnPorts"
	 */
	private Set<Port> rConnPorts = new HashSet<Port>();
	/**
	 * @uml.property name="ifactionPort"
	 * @uml.associationEnd
	 */
	private Port ifactionPort = null;

	/**
	 * @uml.property name="inSignals"
	 */
	private Set<SignalLine> inSignals = new HashSet<SignalLine>();
	/**
	 * @uml.property name="outSignals"
	 */
	private Set<SignalLine> outSignals = new HashSet<SignalLine>();

	// to which node of the AST belongs this block
	/**
	 * @uml.property name="blockSection"
	 * @uml.associationEnd
	 */
	private MDLSection blockSection;

	/**
	 * Is this block a (maybe substituted) reference block?
	 */
	private boolean reference = false;

	/**
	 * The DefaultObject containing the default parameters for this BlockType
	 *
	 * @uml.property name="defaultParamters"
	 * @uml.associationEnd
	 */
	private BlockDefault defaultParamters;

	public Block() {
		super(-1, null);
	}

	/**
	 * Creates a new block with the given parameters.
	 *
	 * @param id
	 *            the item id
	 * @param level
	 *            how deep buried in subsystems
	 * @param parent
	 *            the overlaying subsystem block hash or 1 if block is on the
	 *            first layer (level = 1)
	 * @param blockSection
	 *            The part of the AST to which the block belongs
	 */
	public Block(int id, int level, ModelItem parent, Model parentModel, MDLSection blockSection) {
		super(id, blockSection);
		this.level = level;
		this.parent = parent;
		this.parentModel = parentModel;
		this.blockSection = blockSection;
		this.name = blockSection.getParameter(SimulinkParameterNames.BLOCK_NAME_PARAMETER);
		this.type = blockSection.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER);
	}

	/**
	 * Creates a new block with the given parameters, especially with the given
	 * name. This is important if you substitute a reference block by the block
	 * of the library.
	 *
	 * @param id
	 *            the item id
	 * @param level
	 *            how deep buried in subsystems
	 * @param parent
	 *            the overlaying subsystem block hash or 1 if block is on the
	 *            first layer (level = 1)
	 * @param blockSection
	 *            The part of the AST to which the block belongs
	 * @param blockName
	 *            The name the block will have, regardless of the parameter name
	 *            in the block section.
	 */
	public Block(int id, int level, ModelItem parent, Model parentModel, MDLSection blockSection, String blockName) {
		super(id, blockSection);
		this.level = level;
		this.parent = parent;
		this.parentModel = parentModel;
		this.blockSection = blockSection;
		this.name = blockName;
		this.type = blockSection.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER);
	}

	/**
	 * Looks for all masked subsystem encapsulating this block and collects
	 * their parameters. These parameters are then imported into the block
	 * parameters.
	 * 
	 * @author nick
	 */
	public void spliceInMaskParameters() {
		Map<String, String> maskParameters = this.getParametersFromMaskedSubsystems();
		if (maskParameters != null && !maskParameters.isEmpty()) {
			Map<String, String> paramap = this.getParameter();
			for (String key : paramap.keySet()) {
				String value = paramap.get(key);
				String maskValue = maskParameters.get(value);
				if (maskValue != null) {
					paramap.put(key, maskValue);
				}
			}
		}
	}

	/**
	 * Finds all parameters from masked subsystems encapsulating the
	 * <code>Block</code> generated from the input <code>MDLSection</code>.
	 * 
	 * @param level
	 *            how deep buried in subsystems
	 * @param blockSection
	 *            The part of the AST to which the block belongs
	 * @param blockName
	 *            The name the block will have, regardless of the parameter name
	 *            in the block section.
	 * @return A map of parameters
	 * 
	 * @author nick
	 */
	private Map<String, String> getParametersFromMaskedSubsystems() {
		Map<String, String> result = new HashMap<String, String>();
		
		// move up in AST until first layer, while collecting mask parameters
		for (Block containerBlock : this.getPathOfBlocks()) {
			MDLSection maskSection = containerBlock.blockSection.getFirstSubSection(SimulinkSectionConstants.MASK_SECTION_TYPE);
			if (maskSection != null) { // masked subsystem
				for (MDLSection subSection : maskSection.getSubSections()) {
					if (subSection.getParameter("PropName") != null
							&& subSection.getParameter("PropName").equals("Parameters")) {
						for (MDLSection parameterObjectSection : subSection.getLeafSections()) {
							result.put(parameterObjectSection.getParameter("Name"),
									parameterObjectSection.getParameter("Value"));
						}
					}
				}
			}
		}

		return result;
	}
	
	/**
	 * Replace values of the enumeration data type with their number representation.
	 * 
	 * @param enumMapMap
	 * 			A mapping from the list of class names to maps from enumeration member names to numbers. 
	 * @return Whether this call replaced any values
	 * 
	 * @author nick
	 */
	public boolean spliceInEnumValues(Map<String, Map<String, String>> enumMapMap){
		// filter out scope blocks and such
		String blockValue = this.getParameter("Value");
		if (blockValue == null || blockValue.indexOf(".") == -1) {
			return false;
		}
		
		boolean bool = false;
		String enumClassName = blockValue.substring(0, blockValue.indexOf("."));
		Map<String, String> enumMap = enumMapMap.get(enumClassName);
		if (enumMap != null) {
			String enumMemberName = blockValue.substring(blockValue.indexOf(".")+1);
			String enumValue = enumMap.get(enumMemberName);
			this.getParameter().put("Value", enumValue);
			bool = true;
		}
		return bool;
	}

	/**
	 * @return
	 * @uml.property name="inSignals"
	 */
	public Set<SignalLine> getInSignals() {
		return inSignals;
	}

	/**
	 * @param signals
	 * @uml.property name="inSignals"
	 */
	public void setInSignals(Set<SignalLine> signals) {
		this.inSignals = signals;
	}

	/**
	 * @return
	 * @uml.property name="outSignals"
	 */
	public Set<SignalLine> getOutSignals() {
		return outSignals;
	}

	/**
	 * @param signals
	 * @uml.property name="outSignals"
	 */
	public void setOutSignals(Set<SignalLine> signals) {
		this.outSignals = signals;
	}

	/**
	 * @return
	 * @uml.property name="blockSection"
	 */
	public MDLSection getBlockSection() {

		return blockSection;
	}

	/**
	 * @return
	 * @uml.property name="level"
	 */
	public int getLevel() {
		return level;
	}

	/**
	 * @param level
	 * @uml.property name="level"
	 */
	public void setLevel(Integer level) {
		this.level = level;
	}

	/**
	 * @return
	 * @uml.property name="parent"
	 */
	public ModelItem getParent() {

		return parent;
	}

	/**
	 * @param parent
	 * @uml.property name="parent"
	 */
	public void setParent(ModelItem parent) {
		this.parent = parent;
	}

	/**
	 * @return
	 * @uml.property name="name"
	 */
	public String getName() {

		return name;
	}

	/**
	 * @param name
	 * @uml.property name="name"
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return
	 * @uml.property name="type"
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 * @uml.property name="type"
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return
	 * @uml.property name="inPorts"
	 */
	public HashMap<Integer, Port> getInPortsMap() {
		if (!inPortsMap.values().containsAll(inPorts)) {
			for (Port port : inPorts) {
				inPortsMap.put(port.getNumber(), port);
			}
		}
		return inPortsMap;
	}

	/**
	 * @param inPorts
	 * @uml.property name="inPorts"
	 */
	public void setInPortsMap(HashMap<Integer, Port> inPorts) {
		this.inPortsMap = inPorts;
		this.inPorts = new HashSet<Port>();
		this.inPorts.addAll(inPorts.values());
	}

	/**
	 * @return
	 * @uml.property name="outPorts"
	 */
	public HashMap<Integer, Port> getOutPortsMap() {
		if (!outPortsMap.values().containsAll(outPorts)) {
			for (Port port : outPorts) {
				outPortsMap.put(port.getNumber(), port);
			}
		}
		return outPortsMap;
	}

	/**
	 * @param outportPorts
	 * @uml.property name="outPorts"
	 */
	public void setOutPortsMap(HashMap<Integer, Port> outportPorts) {
		this.outPortsMap = outportPorts;
		outPorts = new HashSet<Port>();
		outPorts.addAll(outportPorts.values());
	}

	/**
	 * @param blockSection
	 * @uml.property name="blockSection"
	 */
	public void setBlockSection(MDLSection blockSection) {
		this.blockSection = blockSection;
	}

	// @Override
	// public String toString() {
	// return id + name;
	// }

	@Override
	public int hashCode() {

		return super.getId();
	}

	@Override
	public boolean equals(Object b) {
		if (b instanceof Block) {
			return b.hashCode() == this.hashCode();
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		String rslt = this.getClass().getSimpleName() + ":" + getId() + ":" + name + " (" + getType() + ")";
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

	@Deprecated
	public String getFullPath() {
		String path = "";
		MDLSection s = this.getBlockSection();
		while ((s != null) && (s.getParameter("Name") != null)) {
			if (s.getName().equals("Block") || s.getName().equals("Model")) {
				path = s.getParameter("Name").replace("/", "//") + path;
			}
			s = s.getParentSection();
			if ((s != null) && (s.getParameter("Name") != null)
					&& (s.getName().equals("Block") || s.getName().equals("Model"))) {
				path = "/" + path;
			}
		}
		return path;
	}

	/**
	 * @return the full Path/Name of the block without the model name at the
	 *         beginning.
	 */
	public String getFullQualifiedName() {
		return getFullQualifiedName(false);
	}

	/**
	 * @param withModelName
	 * @return the full Path/Name of the block without or without the model name
	 *         at the beginning.
	 */
	public String getFullQualifiedName(boolean withModelName) {

		String result = this.name.replace("/", "//");
		if (!(parent instanceof Model)) { // not at the highest layer?

			String refParam = ((Block) parent).getParameter(SimulinkParameterNames.MODEL_NAME_DIALOG_PARAMETER) != null
					? ((Block) parent).getParameter(SimulinkParameterNames.MODEL_NAME_DIALOG_PARAMETER)
					: ((Block) parent).getParameter(SimulinkParameterNames.MODEL_NAME_REF_PARAMETER);
			if (refParam != null) {
				refParam = refParam.replaceAll(".slx", "");
			}
			if ((refParam != null) && withModelName) { // ModelReference
				result = refParam.replace("/", "//") + "/" + result; // get the
				// ModelName
				// of the
				// referenced
				// model
			} else { // not a model reference
				result = ((Block) parent).getFullQualifiedName(withModelName) + "/" + result;
			}
		} else {
			if (withModelName) {
				result = ((Model) parent).getName().replace("/", "//") + "/" + result;
			}
		}

		return result;
	}

	/**
	 * Returns a List of Blocks that represents the path to this block. They are
	 * in the order you have to pass them from the base model to the block.
	 *
	 * @return The blocks representing the path.
	 */
	public List<Block> getPathOfBlocks() {
		List<Block> result = new ArrayList<Block>();

		ModelItem currentBlock = this;

		while (currentBlock instanceof Block) {
			result.add(0, (Block) currentBlock);
			currentBlock = ((Block) currentBlock).parent;
		}

		return result;
	}

	public String getModelName() {
		return parentModel.getName().replace("/", "//");
	}

	/**
	 * Returns the value of the given parameter. If the parameter is not
	 * specified by the block, it returns the default value for this block type.
	 *
	 * @param str
	 *            the parameter name
	 * @return the value of the parameter, <code>null</code> if the parameter is
	 *         not specified for this block even in the defaults
	 */
	public String getParameter(String str) {
		String result = this.getParameter().get(str);
		if ((result == null) || result.equals("null")) {
			result = this.defaultParamters.getParameter().get(str);
		}

		return result;
	}

	/**
	 * @param enablePort
	 *            the enablePort to set
	 * @uml.property name="enablePort"
	 */
	public void setEnablePort(Port enablePort) {
		this.enablePort = enablePort;
	}

	/**
	 * @return the enablePort
	 * @uml.property name="enablePort"
	 */
	public Port getEnablePort() {
		return enablePort;
	}

	/**
	 * @param triggerPort
	 *            the triggerPort to set
	 * @uml.property name="triggerPort"
	 */
	public void setTriggerPort(Port triggerPort) {
		this.triggerPort = triggerPort;
	}

	/**
	 * @return the triggerPort
	 * @uml.property name="triggerPort"
	 */
	public Port getTriggerPort() {
		return triggerPort;
	}

	/**
	 * @param ifactionPort
	 *            the ifactionPort to set
	 * @uml.property name="ifactionPort"
	 */
	public void setIfactionPort(Port ifactionPort) {
		this.ifactionPort = ifactionPort;
	}

	/**
	 * @return the ifactionPort
	 * @uml.property name="ifactionPort"
	 */
	public Port getIfactionPort() {
		return ifactionPort;
	}

	/**
	 * @param inPorts
	 *            the inPorts to set
	 * @uml.property name="inPorts"
	 */
	public void setInPorts(Set<Port> inPorts) {
		this.inPorts = inPorts;
		inPortsMap = new HashMap<Integer, Port>();
	}

	/**
	 * @return the inPorts
	 * @uml.property name="inPorts"
	 */
	public Set<Port> getInPorts() {
		return inPorts;
	}

	/**
	 * @param outPorts
	 *            the outPorts to set
	 * @uml.property name="outPorts"
	 */
	public void setOutPorts(Set<Port> outPorts) {
		this.outPorts = outPorts;
		outPortsMap = new HashMap<Integer, Port>();
	}

	/**
	 * @return the outPorts
	 * @uml.property name="outPorts"
	 */
	public Set<Port> getOutPorts() {
		return outPorts;
	}

	/**
	 * @param lConnPorts
	 *            the lConnPorts to set
	 * @uml.property name="lConnPorts"
	 */
	public void setlConnPorts(Set<Port> lConnPorts) {
		this.lConnPorts = lConnPorts;
		lConnPortsMap = new HashMap<Integer, Port>();
	}

	/**
	 * @return the lConnPorts
	 * @uml.property name="lConnPorts"
	 */
	public Set<Port> getlConnPorts() {
		return lConnPorts;
	}

	/**
	 * @param rConnPorts
	 *            the rConnPorts to set
	 * @uml.property name="rConnPorts"
	 */
	public void setrConnPorts(Set<Port> rConnPorts) {
		this.rConnPorts = rConnPorts;
		rConnPortsMap = new HashMap<Integer, Port>();
	}

	/**
	 * @return the rConnPorts
	 * @uml.property name="rConnPorts"
	 */
	public Set<Port> getrConnPorts() {
		return rConnPorts;
	}

	/**
	 * @param lConnPortsMap
	 *            the lConnPortsMap to set
	 * @uml.property name="lConnPorts"
	 */
	public void setlConnPortsMap(HashMap<Integer, Port> lConnPortsMap) {
		this.lConnPortsMap = lConnPortsMap;
		lConnPorts = new HashSet<Port>();
		lConnPorts.addAll(lConnPortsMap.values());
	}

	/**
	 * @return the lConnPortsMap
	 * @uml.property name="lConnPorts"
	 */
	public HashMap<Integer, Port> getlConnPortsMap() {
		if (!lConnPortsMap.values().containsAll(lConnPorts)) {
			for (Port port : lConnPorts) {
				lConnPortsMap.put(port.getNumber(), port);
			}
		}
		return lConnPortsMap;
	}

	/**
	 * @param rConnPortsMap
	 *            the rConnPortsMap to set
	 * @uml.property name="rConnPorts"
	 */
	public void setrConnPortsMap(HashMap<Integer, Port> rConnPortsMap) {
		this.rConnPortsMap = rConnPortsMap;
		rConnPorts = new HashSet<Port>();
		rConnPorts.addAll(rConnPortsMap.values());
	}

	/**
	 * @return the rConnPortsMap
	 * @uml.property name="rConnPorts"
	 */
	public HashMap<Integer, Port> getrConnPortsMap() {
		if (!rConnPortsMap.values().containsAll(rConnPorts)) {
			for (Port port : rConnPorts) {
				rConnPortsMap.put(port.getNumber(), port);
			}
		}
		return rConnPortsMap;
	}

	/**
	 * @param defaultParamters
	 *            the defaultParamters to set
	 * @uml.property name="defaultParamters"
	 */
	public void setDefaultParamters(BlockDefault defaultParamters) {
		this.defaultParamters = defaultParamters;
	}

	/**
	 * @return the defaultParamters
	 * @uml.property name="defaultParamters"
	 */
	public BlockDefault getDefaultParamters() {
		return defaultParamters;
	}

	/**
	 * @param statePort
	 *            the outPorts to set
	 * @uml.property name="statePort"
	 */
	public void setStatePort(Port statePort) {
		this.statePort = statePort;
	}

	/**
	 * @return the outPorts
	 * @uml.property name="statePort"
	 */
	public Port getStatePort() {
		return statePort;
	}

	/**
	 * Every InPort is connected to exactly one or no <code>SignalLine</code>.
	 * This method returns this <code>SignalLine</code> or null.
	 *
	 * @param port
	 *            The InPort which <code>SignalLine</code> shall be returned.
	 * @return The incoming <code>SignalLine</code> on this port or null.
	 */
	public SignalLine getInSignalLineByInPort(Port port) {
		for (SignalLine s : getInSignals()) {
			if (s.getDstPort().equals(port)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Every InPort is connected to exactly one or no <code>SignalLine</code>.
	 * This method returns this <code>SignalLine</code> or null.
	 *
	 * @param portNr
	 *            The number of the InPort.
	 * @return The incoming <code>SignalLine</code> on this port or null.
	 */
	public SignalLine getInSignalLineByInPort(int portNr) {
		Port port = getInPortsMap().get(portNr);
		for (SignalLine s : getInSignals()) {
			if (s.getDstPort().equals(port)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * OutPorts can be connected to multiple <code>SignalLine</code>. This
	 * method returns these <code>SignalLines</code> or null.
	 *
	 * @param port
	 *            The OutPort which <code>SignalLines</code> shall be returned.
	 * @return The outgoing <code>SignalLines</code> on this port or null.
	 */
	public List<SignalLine> getOutSignalLinesByOutPort(Port port) {
		ArrayList<SignalLine> result = new ArrayList<SignalLine>();
		for (SignalLine s : getOutSignals()) {
			if (s.getSrcPort().equals(port)) {
				result.add(s);
			}
		}
		return result;
	}

	/**
	 * OutPorts can be connected to multiple <code>SignalLine</code>. This
	 * method returns these <code>SignalLines</code> or null.
	 *
	 * @param portNr
	 *            The number of the OutPort.
	 * @return The outgoing <code>SignalLines</code> on this port or null.
	 */
	public List<SignalLine> getOutSignalLinesByOutPort(int portNr) {
		Port port = getOutPortsMap().get(portNr);
		ArrayList<SignalLine> result = new ArrayList<SignalLine>();
		for (SignalLine s : getOutSignals()) {
			if (s.getSrcPort().equals(port)) {
				result.add(s);
			}
		}
		return result;
	}

	public SignalLine getInSignalLineByEnablePort() {
		for(SignalLine s : getInSignals()){
			if(s.getDstPort().equals(enablePort)){
				return s;
			}
		}
		return null;
	}

	
	/**
	 * Was this block in the original model a reference and therefore replaced
	 * by this block? <br>
	 * Only the block on the highest level of a reference is marked as a
	 * reference, so that references in references can also be marked.
	 *
	 * @return Is this block marked as reference?
	 */
	public boolean isReference() {
		return reference;
	}

	/**
	 * (Un)Mark this block as a reference.
	 *
	 * @param reference
	 *            The reference value.
	 */
	public void setReference(boolean reference) {
		this.reference = reference;
	}

	/**
	 * @return the parentModel
	 */
	public Model getParentModel() {
		return parentModel;
	}

	/**
	 * @param parentModel
	 *            the parentModel to set
	 */
	public void setParentModel(Model parentModel) {
		this.parentModel = parentModel;
	}

	public boolean isControlBlock() {
		return CONTROL_BLOCKS.contains(this.getType());
	}

	public Integer getControlPort() {
		switch (this.getType()) {
		case "Switch":
			return 2;
		case "MultiPortSwitch":
			return 1;
		default:
			return null;
		}
	}

	public Integer getPortNumberOfSignalline(SignalLine signalLine) {
		for (int i=0;i<=this.getInSignals().size();i++) {
			if (signalLine.equals(this.getInSignalLineByInPort(i)))  {
				return i;
			}
		}
		return null;
	}
}
