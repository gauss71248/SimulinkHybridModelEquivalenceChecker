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

package de.tu_berlin.pes.memo.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.conqat.lib.commons.collections.UnmodifiableList;
import org.conqat.lib.simulink.builder.MDLSection;
import org.eclipse.core.resources.IProject;
import org.hibernate.criterion.Restrictions;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.impl.Block;
import de.tu_berlin.pes.memo.model.impl.Configuration;
import de.tu_berlin.pes.memo.model.impl.Data;
import de.tu_berlin.pes.memo.model.impl.Event;
import de.tu_berlin.pes.memo.model.impl.Junction;
import de.tu_berlin.pes.memo.model.impl.Model;
import de.tu_berlin.pes.memo.model.impl.ModelItem;
import de.tu_berlin.pes.memo.model.impl.Port;
import de.tu_berlin.pes.memo.model.impl.ProbabilisticSignalLine;
import de.tu_berlin.pes.memo.model.impl.SignalLine;
import de.tu_berlin.pes.memo.model.impl.State;
import de.tu_berlin.pes.memo.model.impl.StateMachine;
import de.tu_berlin.pes.memo.model.impl.StateflowChart;
import de.tu_berlin.pes.memo.model.impl.StateflowItem;
import de.tu_berlin.pes.memo.model.impl.Transition;
import de.tu_berlin.pes.memo.model.util.MemoFieldNames;
import de.tu_berlin.pes.memo.model.util.SimulinkBlockConstants;
import de.tu_berlin.pes.memo.model.util.SimulinkParameterNames;
import de.tu_berlin.pes.memo.model.util.SimulinkParameterValues;
import de.tu_berlin.pes.memo.model.util.SimulinkPortConstants;
import de.tu_berlin.pes.memo.model.util.SimulinkSectionConstants;
import de.tu_berlin.pes.memo.model.util.StateflowParameterConstants;
import de.tu_berlin.pes.memo.parser.matlab.MeMoMatlabManager;
import de.tu_berlin.pes.memo.parser.persistence.MeMoPersistenceManager;

/**
 * Encapsulate the functionality for creating a <code>Model</code> from a given
 * AST.
 *
 * @author Robert Reicherdt, Joachim Kuhnert
 * @since 28.01.2011
 */
public class ModelBuilder {

	/**
	 * Needed to get the information for the analysis at the MatlabEvaluation but
	 * start the analysis at the very end.
	 */
	private BusMuxAnalyzer busanalyzer;

	/**
	 * A library could contain stateflows and than contains a statemachine. This
	 * machine is known immediately a library is referenced, but for every
	 * library exist only one statemachine independent from the count of
	 * referenceblocks to this library, even if a statechart is referenced
	 * multiple times.
	 */
	private HashMap<MDLSection, StateMachine> lib2machine = new HashMap<MDLSection, StateMachine>();
	/**
	 * If a block from library is referenced, blocks of underlying charts aren't
	 * build when the stateflow is parsed. Store the paths to this blocks and the
	 * corresponding charts in this map.
	 */
	private HashMap<String, StateflowChart> path2libraryCharts = new HashMap<String, StateflowChart>();
	/**
	 * The Blockfactory for creating Blocks
	 */
	private BlockFactory blockFactory = new BlockFactory();

	/**
	 * The project that called this method (indirectly)
	 */
	//private IProject currentProject;

	/**
	 * Maps signalline names to their specific probability, which is defined in
	 * their src block
	 */
	private HashMap<Block, HashMap<String, Float>> probabilityMapping = new HashMap<Block, HashMap<String, Float>>();

	public ModelBuilder() {
		
	};

	/**
	 * Creates a <code>Model</code> from a given AST.
	 *
	 * @see edu.tum.cs.simulink.builder.MDLSection MDLSection
	 * @since 28.1.2011
	 *
	 * @param mdlSection
	 *           The root <code>MDLSection</code> of the AST
	 * @param matlabEvaluation
	 *           Shall parameters be updated by connecting to matlab?
	 * @return the Java-representation of the parsed Model
	 */
	public Model buildModel(MDLSection mdlSection, boolean matlabEvaluation) {

		// Be ready to parse a new model
		blockFactory.clear();
		lib2machine.clear();
		path2libraryCharts.clear();

		return buildModel(mdlSection, 1, matlabEvaluation, 0, null);
	}

	/**
	 * Creates a <code>Model</code> from a given AST.
	 *
	 * @see edu.tum.cs.simulink.builder.MDLSection MDLSection
	 * @since 28.1.2011
	 *
	 * @param mdlSection
	 *           The root <code>MDLSection</code> of the AST
	 * @param id
	 *           the model id and lowest id in the whole model
	 * @param matlabEvaluation
	 *           Shall parameters be updated by connecting to matlab?
	 * @param systemLevel
	 *           The current depth in the model. The topModel will be at level 0.
	 *           Important to give model references the right level.
	 * @param parent
	 *           The <code>Block</code> or <code>Model</code> one level higher,
	 *           under which this model lays. Important for referenced models.
	 * @param currentProject
	 *           The project that called this method (indirectly)
	 * @return the Java-representation of the parsed Model
	 */
	private Model buildModel(MDLSection mdlSection, int id, boolean matlabEvaluation,
			int systemLevel, ModelItem parent) {

		MeMoPlugin.out.println("[INFO] Start building model...");

		Model model = new Model();
		MDLSection modelSection;
		MDLSection stateflowSection = null;
		List<MDLSection> mSections;

		model.setId(id);

		// find modelSection
		mSections = mdlSection.getSubSections(SimulinkSectionConstants.MODEL_SECTION_TYPE);

		if (mSections.size() == 1) {
			modelSection = mSections.get(0);
			model.setName(modelSection.getParameter(SimulinkParameterNames.MODEL_NAME_PARAMETER));
		} else {
			MeMoPlugin.out.println("[ERROR] No Model Section found!");
			return null;
		}

		// find stateflow sections
		mSections = mdlSection.getSubSections(SimulinkSectionConstants.SF_SECTION_TYPE);
		if (mSections.size() == 1) {
			stateflowSection = mSections.get(0);
		} else {
			MeMoPlugin.out.println("[WARNING] No Stateflow Section found!");
		}

		// collect configurations

		for (MDLSection childSection : modelSection.getSubSections()) {
			if (childSection.getName().equals(
					SimulinkSectionConstants.BLOCK_PARAMETER_DEFAULTS_SECTION_TYPE)) {
				for (MDLSection defaultSection : childSection
						.getSubSections(SimulinkSectionConstants.BLOCK_SECTION_TYPE)) {
					blockFactory.addDefaults(model.getName(),
							defaultSection.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER),
							defaultSection, model);
				}
			} else if (childSection.getName().equals(SimulinkSectionConstants.SYSTEM_SECTION_TYPE)) {
				// create models system
				createSystem(childSection, model, systemLevel, parent);
				// get all other defaults
			} else if (childSection.getName().contains(
					SimulinkSectionConstants.DEFAULT_SECTION_TYPE_POSTFIX)) {
				model.getDefaultSections().put(childSection.getName(), childSection);
			} else if (childSection.getName().equals(SimulinkSectionConstants.ARRAY_SECTION_TYPE)) {
				for (MDLSection configurationSection : childSection
						.getSubSections(SimulinkSectionConstants.SIMULINK_CONFIGSET)) {
					createConfigurations(model, configurationSection);
				}
			} else if (childSection.getName().equals(
					SimulinkSectionConstants.TEST_POINTED_SIGNAL_SECTION_TYPE)) {
				// TODO if needed
			}
		}

		// create stateflow
		if (stateflowSection != null) {
			MDLSection mSection = stateflowSection.getSubSections(
					SimulinkSectionConstants.SF_MACHINE_SECTION_TYPE).get(0);
			StateMachine sm = new StateMachine(mSection, model.nextID());
			model.getStateMachines().add(sm);
			Map<Integer, StateflowChart> charts = createStateflow(stateflowSection, sm, model);

			for (MDLSection instance : stateflowSection
					.getSubSections(SimulinkSectionConstants.SF_INSTANCE_SECTION_TYPE)) {
				// replace subsystem block with a chart block
				StateflowChart chart = charts.get(Integer.parseInt(instance
						.getParameter(SimulinkParameterNames.SF_CHART_PARAMETER)));
				blockFactory.replaceBlockByChartBlock(chart,
						instance.getParameter(SimulinkParameterNames.SF_NAME_PARAMETER), model);
			}
		}

		for (String path : path2libraryCharts.keySet()) {
			StateflowChart chart = path2libraryCharts.get(path);

			blockFactory.replaceBlockByChartBlock(chart, path, model);
		}

		// update block and signal sets
		model.getBlocks().addAll(model.getBlockMap().values());
		model.getSignalLines().addAll(model.getSignalMap().values());

		// get the port parameters
		busanalyzer = new BusMuxAnalyzer();
		if (matlabEvaluation) {
			MeMoMatlabManager.matlabPortEvaluation(model, busanalyzer);
		}

		for (Block b : model.getBlocks()) {
			b.getInPorts().addAll(b.getInPortsMap().values());
			b.getOutPorts().addAll(b.getOutPortsMap().values());
			b.getlConnPorts().addAll(b.getlConnPortsMap().values());
			b.getrConnPorts().addAll(b.getrConnPortsMap().values());
		}

		MeMoPlugin.out.println("[INFO] Building model " + model.getName() + " finished.");

		return model;
	}

	/**
	 * Creates for a given block all ports given by the "Ports" parameter stored
	 * in the parameter list. There could be only one trigger, one enable and one
	 * action port. If this constrain seemed to be violated, an exception is
	 * thrown.
	 *
	 * @see de.tu_berlin.pes.memo.conceptparser.model.ModelItem#getParameter()
	 *
	 * @param b
	 *           the block with missing ports
	 * @param m
	 *           the model to which the block belongs
	 * @throws Exception
	 */
	private void createPorts(Block b, Model m) throws Exception {
		// The Ports parameter has the form
		// [inPortsNumber, outPorstsNumber, enablePortsNumber,
		// triggerPortsNumber,
		// statePortNumber, LConnPortsNumber, RConnPortsNumber,
		// actionPortNumber]
		String portParameter = b.getParameter(SimulinkParameterNames.BLOCK_PORTS_PARAMETER);

		HashMap<Integer, MDLSection> outPortSections = new HashMap<Integer, MDLSection>();
		for (MDLSection port : b.getBlockSection().getSubSections(
				SimulinkSectionConstants.PORT_SECTION_TYPE)) {
			try {
				outPortSections.put(
						Integer.parseInt(port.getParameter(SimulinkParameterNames.PORT_NUMBER)), port);
			} catch (NumberFormatException e) {
				String err = "Port Section format not as expectetd: "
						+ SimulinkParameterNames.PORT_NUMBER + " = "
						+ port.getParameter(SimulinkParameterNames.PORT_NUMBER);
				MeMoPlugin.err.println(err);
				MeMoPlugin.logException(err, e);
			}
		}

		if (portParameter == null) {
			for (Integer i : outPortSections.keySet()) {
				Port p = new Port(m.nextID(), SimulinkPortConstants.OUT_PORT_PREFIX + i, i);
				p.addParameters(outPortSections.get(i));
				b.getOutPortsMap().put(i, p);
			}
			return;
		}

		String[] ports = portParameter.replace("[", "").replace("]", "").replace(" ", "")
				.split("\\D");
		int inPorts = 0;
		int outPorts = 0;
		int enablePorts = 0;
		int triggerPorts = 0;
		int statePorts = 0;
		int lConnPorts = 0;
		int rConnPorts = 0;
		int ifactionPorts = 0;

		if (((ports.length - 1) >= SimulinkPortConstants.IN_PORT_POSITION)
				&& !ports[SimulinkPortConstants.IN_PORT_POSITION].equals("")) { // happens
																										// if
																										// portParameter
																										// =
																										// ""
			inPorts = Integer.parseInt(ports[SimulinkPortConstants.IN_PORT_POSITION]);
			for (int i = 1; i <= inPorts; i++) {
				b.getInPortsMap().put(i,
						new Port(m.nextID(), SimulinkPortConstants.IN_PORT_PREFIX + i, i));
			}
		}
		if ((ports.length - 1) >= SimulinkPortConstants.OUT_PORT_POSITION) {
			outPorts = Integer.parseInt(ports[SimulinkPortConstants.OUT_PORT_POSITION]);
			for (int i = 1; i <= outPorts; i++) {
				Port p = new Port(m.nextID(), SimulinkPortConstants.OUT_PORT_PREFIX + i, i);
				p.addParameters(outPortSections.get(i));
				b.getOutPortsMap().put(i, p);
			}
		}

		if ((ports.length - 1) >= SimulinkPortConstants.ENABLE_PORT_POSITION) {
			enablePorts = Integer.parseInt(ports[SimulinkPortConstants.ENABLE_PORT_POSITION]);
			if (enablePorts > 0) {
				b.setEnablePort(new Port(m.nextID(), SimulinkPortConstants.ENABLE_PORT, 0));
			}
		}
		if ((ports.length - 1) >= SimulinkPortConstants.TRIGGER_PORT_POSITION) {
			triggerPorts = Integer.parseInt(ports[SimulinkPortConstants.TRIGGER_PORT_POSITION]);
			if (triggerPorts > 0) {
				b.setTriggerPort(new Port(m.nextID(), SimulinkPortConstants.TRIGGER_PORT, 0));
			}
		}
		if ((ports.length - 1) >= SimulinkPortConstants.STATE_PORT_POSITION) {
			statePorts = Integer.parseInt(ports[SimulinkPortConstants.STATE_PORT_POSITION]);
			if (statePorts > 0) {
				b.setStatePort(new Port(m.nextID(), SimulinkPortConstants.STATE_PORT, 0));
			}
		}
		if ((ports.length - 1) >= SimulinkPortConstants.L_CONN_PORT_POSITION) {
			lConnPorts = Integer.parseInt(ports[SimulinkPortConstants.L_CONN_PORT_POSITION]);
			for (int i = 1; i <= lConnPorts; i++) {
				b.getlConnPortsMap().put(i, new Port(m.nextID(), "LConn" + i, i));
			}
		}
		if ((ports.length - 1) >= SimulinkPortConstants.R_CONN_PORT_POSITION) {
			rConnPorts = Integer.parseInt(ports[SimulinkPortConstants.R_CONN_PORT_POSITION]);
			for (int i = 1; i <= rConnPorts; i++) {
				b.getrConnPortsMap().put(i, new Port(m.nextID(), "RConn" + i, i));
			}
		}
		if ((ports.length - 1) >= SimulinkPortConstants.IFACTION_PORT_POSITION) {
			ifactionPorts = Integer.parseInt(ports[SimulinkPortConstants.IFACTION_PORT_POSITION]);
			b.setIfactionPort(new Port(m.nextID(), SimulinkPortConstants.IFACTION_PORT, 0));
		}

		if ((enablePorts > 1) || (triggerPorts > 1) || (ifactionPorts > 1)) {
			Exception e = new Exception("Unerwartete Portanzahl!");
			throw e;
		}
	}

	/**
	 * Gets the destination port of a line. A destination port of a Line can be
	 * an in port, then it is represented by a natural number or it is an
	 * enanble, trigger or action port, then it is represneted by the name of the
	 * port type.
	 *
	 * @param b
	 *           the block to which the the sought port belongs
	 * @param portString
	 *           the string representing the port
	 * @param model
	 *           the model to which the block belongs
	 * @return
	 */
	private Port getDstPortFromString(Block b, String portString, Model model) {
		Port result;
		if (portString.matches("[0-9]+")) {
			result = b.getInPortsMap().get(Integer.parseInt(portString));
			if (result == null) {
				int portNr = Integer.parseInt(portString);
				result = new Port(model.nextID(), SimulinkPortConstants.IN_PORT_PREFIX + portString,
						portNr);
				b.getInPortsMap().put(portNr, result);
			}
			return result;
		} else if (portString.equals(SimulinkPortConstants.ENABLE_PORT)) {
			result = b.getEnablePort();
			if (result == null) {
				result = new Port(model.nextID(), portString, 0);
				b.setEnablePort(result);
			}
			return result;
		} else if (portString.equals(SimulinkPortConstants.TRIGGER_PORT)) {
			result = b.getTriggerPort();
			if (result == null) {
				result = new Port(model.nextID(), portString, 0);
				b.setTriggerPort(result);
			}
			return result;
			// state ports are only outgoing ports / source ports
			// }else if(portString.equals(SimulinkPortConstants.STATE_PORT)){
			// result = b.getStatePort();
			// if(result == null){
			// result = new Port(model.nextID(), portString);
			// b.setStatePort(result);
			// }
			// return result;
		} else if (portString.contains(SimulinkPortConstants.L_CONN_PORT_PREFIX)) {
			Integer portNumber = Integer.parseInt(portString.replace(
					SimulinkPortConstants.L_CONN_PORT_PREFIX, ""));
			result = b.getlConnPortsMap().get(portNumber);
			if (result == null) {
				result = new Port(model.nextID(), portString, portNumber);
				b.getlConnPortsMap().put(portNumber, result);
			}
			return result;
		} else if (portString.contains(SimulinkPortConstants.R_CONN_PORT_PREFIX)) {
			Integer portNumber = Integer.parseInt(portString.replace(
					SimulinkPortConstants.R_CONN_PORT_PREFIX, ""));
			result = b.getrConnPortsMap().get(portNumber);
			if (result == null) {
				result = new Port(model.nextID(), portString, portNumber);
				b.getrConnPortsMap().put(portNumber, result);
			}
			return result;
		} else if (portString.equals(SimulinkPortConstants.IFACTION_PORT)) {
			result = b.getIfactionPort();
			if (result == null) {
				result = new Port(model.nextID(), portString, 0);
				b.setIfactionPort(result);
			}
			return result;
		}
		return null;
	}

	/**
	 *
	 * Gets the source port of a line. A source port can be an out port, an state
	 * port or an L/RConn port. Out ports are represented by a natural number
	 *
	 * @param b
	 *           the block to which the the sought port belongs
	 * @param portString
	 *           the string representing the port
	 * @param model
	 *           the model to which the block belongs
	 * @return The Port found by the given String. Will create new Ports if not
	 *         found or returns null if the String is illegal.
	 */
	private Port getSrcPortFromString(Block b, String portString, Model model) {
		Port result;
		if (portString.matches("[0-9]+")) {
			result = b.getOutPortsMap().get(Integer.parseInt(portString));
			if (result == null) {
				int portNr = Integer.parseInt(portString);
				result = new Port(model.nextID(), SimulinkPortConstants.OUT_PORT_PREFIX + portString,
						portNr);
				b.getOutPortsMap().put(portNr, result);
			}
			return result;
		} else if (portString.equals(SimulinkPortConstants.STATE_PORT)) {
			result = b.getStatePort();
			if (result == null) {
				result = new Port(model.nextID(), portString, 0);
				b.setStatePort(result);
			}
			return result;
		} else if (portString.contains(SimulinkPortConstants.L_CONN_PORT_PREFIX)) {
			Integer portNumber = Integer.parseInt(portString.replace(
					SimulinkPortConstants.L_CONN_PORT_PREFIX, ""));
			result = b.getlConnPortsMap().get(portNumber);
			if (result == null) {
				result = new Port(model.nextID(), portString, portNumber);
				b.getlConnPortsMap().put(portNumber, result);
			}
			return result;
		} else if (portString.contains(SimulinkPortConstants.R_CONN_PORT_PREFIX)) {
			Integer portNumber = Integer.parseInt(portString.replace(
					SimulinkPortConstants.R_CONN_PORT_PREFIX, ""));
			result = b.getrConnPortsMap().get(portNumber);
			if (result == null) {
				result = new Port(model.nextID(), portString, portNumber);
				b.getrConnPortsMap().put(portNumber, result);
			}
			return result;
		}

		return null;
	}

	/**
	 * Builds the Stateflow part of the Simulink model.
	 *
	 * @since 28.1.2011
	 *
	 * @param stateflowSection
	 *           the detected stateflow part of the AST
	 * @param sm
	 *           the <code>StateflowMachine</code> which will be the root for all
	 *           the stateflow elements.
	 * @param model
	 *           the model to which the stateflow belongs
	 *
	 @return All parsed <code>StateflowCharts</code> mapped by mapped by their
	 *         id. Helpful to assign charts to <code>ChartBlocks</code> by the
	 *         instance section of the <code>stateflowSection</code>.
	 */
	private Map<Integer, StateflowChart> createStateflow(MDLSection stateflowSection,
			StateMachine sm, Model model) {
		// get charts an save them in hashmap for easier assignment of chart
		// elements
		Map<Integer, StateflowChart> charts = new HashMap<Integer, StateflowChart>();

		List<MDLSection> chartSections = stateflowSection
				.getSubSections(SimulinkSectionConstants.SF_CHART_SECTION_TYPE);
		for (MDLSection chartSection : chartSections) {
			StateflowChart chart = new StateflowChart(chartSection, model.nextID());
			sm.addStateflowChart(chart);
			charts.put(chart.getStateFlowId(), chart);
		}

		// collect states
		Map<Integer, StateflowItem> chartElements = new HashMap<Integer, StateflowItem>();
		List<MDLSection> stateSections = stateflowSection
				.getSubSections(SimulinkSectionConstants.SF_STATE_SECTION_TYPE);
		for (MDLSection stateSection : stateSections) {
			State state = new State(stateSection, model.nextID());
			charts.get(state.getChartId()).getStates().add(state);
			chartElements.put(state.getStateFlowId(), state);
		}

		// collect junctions
		List<MDLSection> junctionSections = stateflowSection
				.getSubSections(SimulinkSectionConstants.SF_JUNCTION_SECTION_TYPE);
		for (MDLSection junctionSection : junctionSections) {
			Junction junction = new Junction(junctionSection, model.nextID());
			charts.get(junction.getChartId()).getJunctions().add(junction);
			chartElements.put(junction.getStateFlowId(), junction);
		}

		// collect transitions
		List<MDLSection> transitionSections = stateflowSection
				.getSubSections(SimulinkSectionConstants.SF_TRANSITION_SECTION_TYPE);
		for (MDLSection transitionSection : transitionSections) {
			Transition transition = new Transition(transitionSection, model.nextID());
			charts.get(transition.getChartId()).getTransitions().add(transition);
			chartElements.put(transition.getStateFlowId(), transition);
			// reassign junctions/states an transition
			String src = transitionSection
					.getSubSections(SimulinkSectionConstants.SF_TRANSITION_SRC_SECTION_TYPE).get(0)
							.getParameter(StateflowParameterConstants.SF_ID_STRING);
			String dst = transitionSection
					.getSubSections(SimulinkSectionConstants.SF_TRANSITION_DST_SECTION_TYPE).get(0)
							.getParameter(StateflowParameterConstants.SF_ID_STRING);
			if (src != null) {
				StateflowItem srcItem = chartElements.get(Integer.parseInt(src));
				if (srcItem instanceof Junction) {
					((Junction) srcItem).getOutTransitions().add(transition);
				} else if (srcItem instanceof State) {
					((State) srcItem).getOutTransitions().add(transition);
				}
				transition.setSrc(srcItem);
			}
			if (dst != null) {
				StateflowItem dstItem = chartElements.get(Integer.parseInt(dst));
				if (dstItem instanceof Junction) {
					((Junction) dstItem).getInTransitions().add(transition);
				} else if (dstItem instanceof State) {
					((State) dstItem).getInTransitions().add(transition);
				}
				transition.setDst(dstItem);
			}
		}

		// update children of states.
		for (StateflowItem sfi : chartElements.values()) {
			int parentID;
			StateflowItem parent;
			if (sfi instanceof State) {
				parentID = ((State) sfi).getParentId();
				parent = chartElements.get(parentID);
				if ((parent != null) && (parent instanceof State)) {
					((State) parent).getChildStates().add((State) sfi);
				}
			} else if (sfi instanceof Junction) {
				parentID = ((Junction) sfi).getParentId();
				parent = chartElements.get(parentID);
				if ((parent != null) && (parent instanceof State)) {
					((State) parent).getChildJunctions().add((Junction) sfi);
				}
			} else if (sfi instanceof Transition) {
				parentID = ((Transition) sfi).getParentId();
				parent = chartElements.get(parentID);
				if ((parent != null) && (parent instanceof State)) {
					((State) parent).getChildTransitions().add((Transition) sfi);
				}
			}
		}

		// collect data
		List<MDLSection> dataSections = stateflowSection
				.getSubSections(SimulinkSectionConstants.SF_DATA_SECTION_TYPE);
		for (MDLSection dataSection : dataSections) {
			Data data = new Data(dataSection, model.nextID());
			int parentID = getParentID(data.getParameter().get(
					StateflowParameterConstants.SF_LINKNODE_STRING));
			getChart(parentID, charts, chartElements).getData().add(data);
			chartElements.put(data.getStateFlowId(), data);
			if (data.getParameter().get(StateflowParameterConstants.SCOPE)
					.equals(StateflowParameterConstants.LOCAL_DATA)) {
				StateflowItem item = chartElements.get(parentID);
				if (item != null) {
					// should be a state
					((State) item).getLocalData().add(data);
				} else {
					item = charts.get(parentID);
					// is a chart
					((StateflowChart) item).getLocalData().add(data);
				}
			}
		}

		// collect events
		List<MDLSection> eventSections = stateflowSection
				.getSubSections(SimulinkSectionConstants.SF_EVENT_SECTION_TYPE);
		for (MDLSection eventSection : eventSections) {
			Event event = new Event(eventSection, model.nextID());
			int parentID = getParentID(event.getParameter().get(
					StateflowParameterConstants.SF_LINKNODE_STRING));
			getChartFromParent(charts, chartElements, parentID).getEvents().add(event);
			chartElements.put(event.getStateFlowId(), event);
			if (event.getParameter().get(StateflowParameterConstants.SCOPE)
					.equals(StateflowParameterConstants.LOCAL_EVENT)) {
				StateflowItem item = chartElements.get(parentID);
				if (item != null) {
					// should be a state
					((State) item).getLocalEvents().add(event);
				} else {
					item = charts.get(parentID);
					// is a chart
					((StateflowChart) item).getLocalEvents().add(event);
				}
			}
		}

		model.getStateMachines().add(sm);

		return charts;
	}

	/**
	 * Get the Chart from a parent id. If the parent is not a chart, then it
	 * looks up the belonging chart of the parent.
	 *
	 * @param parentID
	 *           the parent of the object to which the chart shall be resolved
	 * @param charts
	 *           all known charts
	 * @param chartElements
	 *           all known chart elements
	 * @return the chart to which the the object with the parent with parentID
	 *         belongs
	 */
	private StateflowChart getChart(int parentID, Map<Integer, StateflowChart> charts,
			Map<Integer, StateflowItem> chartElements) {
		if (charts.get(parentID) != null) {
			return charts.get(parentID);
		} else {
			String chartID = chartElements.get(parentID).getParameter()
					.get(StateflowParameterConstants.SF_CHART_STRING);
			return charts.get(Integer.parseInt(chartID));
		}
	}

	private void parseProbalisticAttributes(Block b) {
		// parse probalistic information
		float prob_sum = 0.0f; // for checking, if all probilities together are
										// 1.0
		String prob = b.getParameter("AttributesFormatString");
		if (prob != null) {
			Pattern prob_pattern = Pattern.compile("(.+)@([0-9\\.%]+)+");
			prob = prob.replaceAll("\\\\n", "\n").replace("\\\n", "\n");
			String[] splits = prob.split("\n");
			for (String s : splits) {
				Matcher m = prob_pattern.matcher(s);
				if (m.matches() && (m.groupCount() == 2)) {
					String name = m.group(1);
					String probString = m.group(2);
					float probability = 1.0f;
					if (probString.contains("%")) {
						probString = probString.replaceAll("%", "");
						probability = Float.parseFloat(probString);
						probability /= 100.0f;
					} else {
						probability = Float.parseFloat(probString);
					}
					if (probabilityMapping.containsKey(b)) {
						probabilityMapping.get(b).put(name, probability);
					} else {
						probabilityMapping.put(b, new HashMap<String, Float>());
						probabilityMapping.get(b).put(name, probability);
					}
					prob_sum += probability;
				}
			}
			if (Math.abs(prob_sum - 1.0f) > 0.00000000001f) {
				MeMoPlugin.out.println("[WARNING] probabilities for Block " + b.getFullQualifiedName()
						+ " summed together are not 1.0, but " + prob_sum);
			}
		}
	}

	/**
	 * Builds the system part of the Simulink model.
	 *
	 * @since 28.1.2011
	 *
	 * @param systemSection
	 *           the detected system part of the AST
	 * @param topModel
	 *           the model to which the systemSection belongs
	 * @param systemLevel
	 *           the current layer of subsystems
	 * @param blockparent
	 *           the hash of the overlaying subsystem block
	 */
	private void createSystem(MDLSection systemSection, Model topModel, int systemLevel,
			ModelItem blockparent) {

		HashMap<Integer, Block> blockMap = topModel.getBlockMap();
		// collect block sections
		List<MDLSection> blockSections = systemSection
				.getSubSections(SimulinkSectionConstants.BLOCK_SECTION_TYPE);
		HashMap<String, Block> localName2Block = new HashMap<String, Block>();
		HashMap<Integer, Block> pmioWorklist = new HashMap<Integer, Block>();
		// ArrayList<Block> modelReferenceWorklist = new ArrayList<Block>();

		HashMap<Integer, Block>[] portBlocks = new HashMap[8];
		for (int i = 0; i < portBlocks.length; i++) {
			portBlocks[i] = new HashMap<Integer, Block>();
		}

		// create all Blocks
		for (MDLSection blockSection : blockSections) {
			Block b; // the block we will create

			if (blockparent == null) {
				blockparent = topModel;
			}

			// Store name, because reference blocks will be substituted but must be
			// available by their old name
			String blockName = blockSection.getParameter(SimulinkParameterNames.BLOCK_NAME_PARAMETER);

			if (SimulinkBlockConstants.REFERENCE_BLOCKTYPE.equals(blockSection
					.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
				MDLSection libBlockSection = getLibBlockSection(blockSection);
				b = blockFactory.createRefBlock(systemLevel + 1, blockparent, blockSection,
						libBlockSection, topModel, blockName);
				checkLibRefForStateflow(b, blockSection, topModel);
				b.setReference(true);
			} else if (SimulinkBlockConstants.MODEL_REFERENCE_BLOCKTYPE.equals(blockSection
					.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
				b = blockFactory.createBlock(systemLevel + 1, blockparent, blockSection, topModel,
						blockName);
				// modelReferenceWorklist.add(b);
				b.setReference(true);
			} else {
				b = blockFactory.createBlock(systemLevel + 1, blockparent, blockSection, topModel,
						blockName);
			}

			parseProbalisticAttributes(b);

			blockMap.put(b.hashCode(), b);
			localName2Block.put(b.getName(), b);

			// create Ports of port blocks
			if (SimulinkBlockConstants.INPORT_BLOCKTYPE.equals(blockSection
					.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
				b.getOutPortsMap().put(1,
						new Port(topModel.nextID(), SimulinkPortConstants.OUT_PORT_PREFIX + "1", 1));
			} else if (SimulinkBlockConstants.OUTPORT_BLOCKTYPE.equals(blockSection
					.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
				b.getInPortsMap().put(1,
						new Port(topModel.nextID(), SimulinkPortConstants.IN_PORT_PREFIX + "1", 1));
			} else if (SimulinkBlockConstants.LR_CON_BLOCKTYPE.equals(blockSection
					.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
				// Connections from and to PMIOPorts use RConn1
				b.getrConnPortsMap().put(1, new Port(topModel.nextID(), "RConn1", 1));
			}
			try {
				createPorts(b, topModel);
			} catch (Exception e) {
				e.printStackTrace(MeMoPlugin.err);
			}

			// handle port blocks with special care
			// -> forward lines from/to a subsystem to the corresponding port
			// block
			// but only IFF it is not the top level in hierarchy
			if (systemLevel > 0) {
				if (SimulinkBlockConstants.INPORT_BLOCKTYPE.equals(blockSection
						.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
					// reassignSignalToPortBlock(blockMap.get(parent), b);
					String port = b.getBlockSection().getParameter(
							SimulinkParameterNames.PORT_BLOCK_PORT_PARAMETER);
					int key = 1;
					if (port != null) { // in port 1 has no port parameter
						key = Integer.parseInt(port);
					}
					portBlocks[SimulinkPortConstants.IN_PORT_POSITION].put(key, b);
				} else if (SimulinkBlockConstants.OUTPORT_BLOCKTYPE.equals(blockSection
						.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
					// reassignSignalToPortBlock(blockMap.get(parent), b);
					String port = b.getBlockSection().getParameter(
							SimulinkParameterNames.PORT_BLOCK_PORT_PARAMETER);
					int key = 1;
					if (port != null) { // out port 1 has no port parameter
						key = Integer.parseInt(port);
					}
					portBlocks[SimulinkPortConstants.OUT_PORT_POSITION].put(key, b);
				} else if (SimulinkBlockConstants.ENABLEPORT_BLOCKTYPE.equals(blockSection
						.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
					// reassignSignalToPortBlock(blockMap.get(parent), b);
					portBlocks[SimulinkPortConstants.ENABLE_PORT_POSITION].put(1, b); // only
																											// one
																											// enable
																											// port
																											// possible
				} else if (SimulinkBlockConstants.TRIGGERPORT_BLOCKTYPE.equals(blockSection
						.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
					// reassignSignalToPortBlock(blockMap.get(parent), b);
					portBlocks[SimulinkPortConstants.TRIGGER_PORT_POSITION].put(1, b); // only
																												// one
																												// trigger
																												// port
																												// possible
					// } else if (blockSection.getParameter( // state port, not seen
					// as a block yet
					// SimulinkParameterNames.BLOCK_TYPE_PARAMETER).equals(
					// SimulinkBlockConstants.STATEPORT_BLOCKTYPE)) {
					// reassignSignalToPortBlock(blockMap.get(parent), b);
					// portBlocks[SimulinkPortConstants.STATE_PORT_POSITION].put(1,
					// b); // only one state port possible
				} else if (SimulinkBlockConstants.LR_CON_BLOCKTYPE.equals(blockSection
						.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
					// PMIOPorts are numbered, LCon/RCon ports are PMIOPorts with
					// the side parameter "left" or "right".the first PMIO with this
					// parameter
					// "right" is RCon1, the first with "left" is LCon1 and so on.
					// The PMIOPort number
					// isn't ordered left and right, so left and right can occur in
					// any order. To decide
					// which LCon or RCon number a port belongs to, you must know all
					// previous ports. This
					// is not guaranteed at this point, so collect the ports, until
					// all blocks of this subsystem
					// are known.
					pmioWorklist.put(Integer.parseInt(blockSection
							.getParameter(SimulinkParameterNames.PORT_BLOCK_PORT_PARAMETER)), b);
					// reassignSignalToPortBlock(blockMap.get(parent), b);
				} else if (SimulinkBlockConstants.ACTIONPORT_BLOCKTYPE.equals(blockSection
						.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER))) {
					// reassignSignalToPortBlock(blockMap.get(parent), b);
					portBlocks[SimulinkPortConstants.IFACTION_PORT_POSITION].put(1, b); // only
																												// one
																												// ifaction
																												// port
																												// possible
				}
			}
		} // end for

		if (systemLevel > 0) {
			pmioToLRCon(pmioWorklist, portBlocks);
			reassignSignalsToPortBlocks((Block) blockparent, portBlocks);
		}

		// collect lineSections
		List<MDLSection> lineSections = systemSection
				.getSubSections(SimulinkSectionConstants.LINE_SECTION_TYPE);
		Block src;
		Block dst;
		for (MDLSection lineSection : lineSections) {
			List<SignalLine> signals = new ArrayList<SignalLine>();
			String signalLineName = lineSection
					.getParameter(SimulinkParameterNames.SIGNAL_NAME_PARAMETER);
			String srcPortString = lineSection
					.getParameter(SimulinkParameterNames.SIGNAL_SRCPORT_PARAMETER);
			String dstPortString = lineSection
					.getParameter(SimulinkParameterNames.SIGNAL_DSTPORT_PARAMETER);
			src = localName2Block.get(lineSection
					.getParameter(SimulinkParameterNames.SIGNAL_SRCBLOCK_PARAMETER));
			dst = localName2Block.get(lineSection
					.getParameter(SimulinkParameterNames.SIGNAL_DSTBLOCK_PARAMETER));

			if ((dst == null) || (src == null)) { // if the destination or source
																// of
				// a signal line is null, it
				// could be a branch
				findBranches(topModel, signals, lineSection, localName2Block, systemLevel + 1);
			} else {

				Port srcPort = getSrcPortFromString(src, srcPortString, topModel);
				Port dstPort = getDstPortFromString(dst, dstPortString, topModel);

				SignalLine s = null;

				if (probabilityMapping.containsKey(src)
						&& probabilityMapping.get(src).containsKey(signalLineName)) {
					s = new ProbabilisticSignalLine(topModel.nextID(), systemLevel + 1, src, dst,
							srcPort, dstPort, signalLineName, probabilityMapping.get(src).get(
									signalLineName));
				} else {
					s = new SignalLine(topModel.nextID(), systemLevel + 1, src, dst, srcPort, dstPort,
							signalLineName);
				}

				signals.add(s);
			}

			for (SignalLine si : signals) {
				topModel.getSignalMap().put(si.hashCode(), si);
				si.getSrcBlock().getOutSignals().add(si);
				si.getDstBlock().getInSignals().add(si);
			}
		}

		// Perform subsystems, referenced models
		for (Block block : localName2Block.values()) {
			if (SimulinkBlockConstants.SUBSYSTEM_BLOCKTYPE.equals(block.getType())) {
				createSystem(
						block.getBlockSection().getFirstSubSection(
								SimulinkSectionConstants.SYSTEM_SECTION_TYPE), topModel, systemLevel + 1,
						block);
			} else if (SimulinkBlockConstants.MODEL_REFERENCE_BLOCKTYPE.equals(block.getType())) {
				createModelReference(block, topModel, systemLevel + 1);
			}
		}

	}

	/**
	 * A model reference is a whole simulink model included in an other one. This
	 * method starts the building of a referenced model and returns it.
	 * Guarantees that no id's will be doubled.
	 *
	 * @param refblock
	 *           The reference block that represents the included model. Will be
	 *           the parent of the new build model.
	 * @param model
	 *           The parent model of the referenced block and the key generator.
	 * @param systemLevel
	 *           The current depth in the model
	 * @return The new created referenced model
	 */
	private Model createModelReference(Block refblock, Model model, int systemLevel) {
		// TODO: Only one Statemachine for all references of one model?
		MDLSection blockSection = refblock.getBlockSection();
		String modelName = blockSection
				.getParameter(SimulinkParameterNames.MODEL_NAME_DIALOG_PARAMETER) != null ? blockSection
				.getParameter(SimulinkParameterNames.MODEL_NAME_DIALOG_PARAMETER) : blockSection
				.getParameter(SimulinkParameterNames.MODEL_NAME_REF_PARAMETER);
				MeMoPlugin.out.println("[INFO] Trying to replace model reference block "
				+ blockSection.getParameter(SimulinkParameterNames.BLOCK_NAME_PARAMETER));
				MDLSection mdlSection = MeMoParserManager.getRefModelAST(modelName);

		if (mdlSection != null) {

			ModelBuilder newBuilder = new ModelBuilder();

			Model newModel = newBuilder.buildModel(mdlSection, model.nextID(), false, systemLevel,
					refblock);
					model.setIdCounter(newModel.nextID() + 1); // don't use ids already
																		// used in the new model
																		// in the old one!
					newModel.setReference(true);

			for (Block block : newModel.getBlockMap().values()) {
						model.getBlockMap().put(block.getId(), block);
					}

			for (SignalLine sl : newModel.getSignalLines()) {
						model.getSignalMap().put(sl.getId(), sl);
					}

			model.getStateMachines().addAll(newModel.getStateMachines());

			return newModel;
				} else {
					MeMoPlugin.out.println("[WARNING] Model " + modelName + " not found");
					return null;
				}
	}

	// TODO: Multiple Libraries with same name! see C:\Program
	// Files\dSPACE\Matlab\Tl\blocklib\*\tl_autosar_lib.mdl
	/**
	 * Searches in the user given libraries for the referenced block of a
	 * reference block and delivers the found section.
	 *
	 * @param blockSection
	 *           The block section of the reference block
	 * @return The given block section if the library or the block in the library
	 *         is not found, the MDLSection of the library block otherwise.
	 */
	private MDLSection getLibBlockSection(MDLSection blockSection) {
		MeMoPlugin.out.println("[INFO] Trying to replace reference block "
				+ blockSection.getParameter(SimulinkParameterNames.BLOCK_NAME_PARAMETER));
		MDLSection result = getLibBlockSection(
				blockSection.getParameter(SimulinkParameterNames.BLOCK_SOURCE_BLOCK), blockSection);
		return result == null ? blockSection : result;
	}

	/**
	 * Searches in the user given libraries for the referenced block of a
	 * reference block and delivers the found section.
	 *
	 * @param referencePath
	 *           The reference path of the reference block
	 * @param blockSection
	 *           The reference block section, used for error messages
	 * @return the MDLSection of the library block or null if no library is
	 *         found.
	 */
	private MDLSection getLibBlockSection(String referencePath, MDLSection blockSection) {
		MDLSection result;
		referencePath = referencePath.replaceAll("\n","\\\\n");
		List<String> path = parseModelPath(referencePath); // split the path in
																			// its parts
		MDLSection library = MeMoParserManager.getLibraryAST(path.get(0)); // Get
																									// the
																									// parsed
																									// library
																									// mdl
																									// file
		MDLSection librarySection; // the library section of the mdl file
		String forwarding; // A library can contain a forwarding table that maps
									// old paths to new, sotre the new path

		if (library == null) {
			MeMoPlugin.out.println("[WARNING] Replacing of Reference Block "
					+ blockSection.getParameter(SimulinkParameterNames.BLOCK_NAME_PARAMETER)
					+ " failed, skip replacing!");
			return null;
		}

		forwarding = MeMoParserManager.getForwardingTable(library).get(referencePath); // get
																													// the
																													// new
																													// path,
																													// if
																													// there
																													// is
																													// a
																													// forwarding
		if (forwarding != null) { // forwarding found?
			MeMoPlugin.out.println("[INFO] Forwarding found: " + referencePath + " --> " + forwarding);
			result = getLibBlockSection(forwarding, blockSection); // just in case
																						// a
																						// forwarding
																						// could be
																						// forwarded:
																						// getLibSection
																						// with new
																						// path
			if (result != null) {
				return result;
			} else { // forwarding failed, do it without forwarding
				MeMoPlugin.out.println("[WARNING] Forwarding for " + referencePath
						+ " failed! Use most recent result!");
			}
		}

		librarySection = library.getFirstSubSection(SimulinkSectionConstants.LIBRARY_SECTION_TYPE); // get
																																	// the
																																	// library
																																	// section

		result = librarySection.getSubSections(SimulinkSectionConstants.SYSTEM_SECTION_TYPE).get(0); // get
																																	// the
																																	// system
																																	// section
																																	// where
																																	// the
																																	// blocks
																																	// are
																																	// actually
		for (int i = 1; i < path.size(); i++) { // follow the path
			MDLSection tmp = null;
			for (MDLSection block : result.getSubSections(SimulinkSectionConstants.BLOCK_SECTION_TYPE)) { // find
																																			// the
																																			// right
																																			// subsection
				if (block.getParameter(SimulinkParameterNames.BLOCK_NAME_PARAMETER).equals(path.get(i))) { // is
																																			// it
																																			// the
																																			// correct
																																			// subsection?
					if (block.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER).equals(
							SimulinkBlockConstants.SUBSYSTEM_BLOCKTYPE)
							&& (i < (path.size() - 1))) {
						tmp = block.getSubSections(SimulinkSectionConstants.SYSTEM_SECTION_TYPE).get(0); // in
																																	// a
																																	// subsystem
																																	// we
																																	// need
																																	// the
																																	// system
																																	// section,
																																	// if
																																	// we
																																	// want
																																	// to
																																	// go
																																	// deeper
					} else {
						tmp = block;
					}
					break;
				}
			}
			if (tmp == null) { // have found a match?
				MeMoPlugin.out.println("[WARNING] Replacing of Reference Block "
						+ blockSection.getParameter(SimulinkParameterNames.BLOCK_NAME_PARAMETER)
						+ " failed, Block " + path.get(i) + " of path " + path + " in Library "
						+ path.get(0) + " not found, skip replacing!");
				return null;
			} else {
				result = tmp;
			}

		}

		return result;
	}

	/**
	 * Checks at a given reference block if there exist an underlying
	 * StateflowBlock. if so, it generates the path to this (at this point not
	 * existing) block and stores the results in the libraryCharts map to replace
	 * this blocks later by an <code>ChartBlock</code>.
	 *
	 * @param block
	 *           The reference block
	 * @param blockSection
	 *           The blocksection of the reference block. It's not accessible by
	 *           <code>block..getBlockSection()</code> because it is replaced by
	 *           the library section for further processing.
	 * @param model
	 *           The model to which the blocks belong
	 */
	private void checkLibRefForStateflow(Block block, MDLSection blockSection, Model model) {
		String referencePath = blockSection.getParameter(SimulinkParameterNames.BLOCK_SOURCE_BLOCK);
		referencePath = referencePath.replaceAll("\n", "\\\\n");
		List<String> path = parseModelPath(referencePath); // split the path in
																			// its parts
		MDLSection library = MeMoParserManager.getLibraryAST(path.get(0)); // Get
																									// the
																									// parsed
																									// library
																									// mdl
																									// file
		String pathwoModelName = referencePath.substring(path.get(0).length() + 1);

		if ((library != null)
				&& !library.getSubSections(SimulinkSectionConstants.SF_SECTION_TYPE).isEmpty()) { // it
																																// exists
																																// a
																																// stateflow
																																// in
																																// the
																																// library
			ArrayList<MDLSection> instances = new ArrayList<MDLSection>();

			for (MDLSection instance : library // check all insatnces...
					.getFirstSubSection(SimulinkSectionConstants.SF_SECTION_TYPE).getSubSections(
							SimulinkSectionConstants.SF_INSTANCE_SECTION_TYPE)) {
				if (instance.getParameter(SimulinkParameterNames.SF_NAME_PARAMETER) // ...
																											// if
																											// they
																											// point
																											// to
																											// a
																											// block
																											// underlying
																											// the
																											// given
																											// one
						.startsWith(pathwoModelName)) {
					instances.add(instance); // collect these instances
				}
			}

			String currentPath = block.getFullQualifiedName(false); // get the path
																						// to the
																						// current
																						// block
			for (MDLSection instance : instances) { // now build for every instance
																	// the new path
				StateflowChart chart = getLibChart(
						instance.getParameter(SimulinkParameterNames.SF_CHART_PARAMETER), library, model); // get
																																		// the
																																		// corresponding
																																		// chart
				String chartPath = instance.getParameter(SimulinkParameterNames.SF_NAME_PARAMETER); // get
																																// the
																																// path
																																// were
																																// the
																																// instance
																																// points
																																// to
				String tailPath = chartPath.replaceFirst(pathwoModelName, ""); // cut
																									// off
																									// path
																									// part
																									// already
																									// gone
																									// by
																									// the
																									// referencing
				path2libraryCharts.put(currentPath + tailPath, chart); // build the
																							// whole
																							// path and
																							// store it
																							// and its
																							// chart
			}
		}
	}

	/**
	 * Get a chart by its id and the library it belongs to. Will build a new
	 * statflow if the machine belonging to the chart doesn't exist.
	 *
	 * @param queriedID
	 *           The id of the chart.
	 * @param library
	 *           The <code>MDLSection</code> of the library, to get the right
	 *           statemachine.
	 * @param model
	 *           The model to which the chart belongs.
	 * @return The found chart or null.
	 */
	private StateflowChart getLibChart(String queriedID, MDLSection library, Model model) {
		StateMachine machine = lib2machine.get(library); // get the right machine
																			// from map
		int chartId = Integer.parseInt(queriedID);

		if (machine == null) { // machine doesn't exist yet? -> build it!
			MDLSection sfSection = library
					.getFirstSubSection(SimulinkSectionConstants.SF_SECTION_TYPE);
			machine = new StateMachine(
					sfSection.getFirstSubSection(SimulinkSectionConstants.SF_MACHINE_SECTION_TYPE),
					model.nextID());
			createStateflow(sfSection, machine, model);
			lib2machine.put(library, machine); // store the new build machine for
															// reuse
		}

		for (StateflowChart chart : machine.getStateflowCharts()) { // search the
																						// machine for
																						// the chart
			if (chart.getStateFlowId() == chartId) {
				return chart;
			}
		}

		return null;
	}

	/**
	 * Splits a path with with path separator "/" in the individual parts of the
	 * path. Supports escaping of slash like "//".
	 *
	 * @param path
	 *           The path string with '/' as path separator
	 * @return The individual parts of the path as a list
	 */
	private List<String> parseModelPath(String path) {
		ArrayList<String> result = new ArrayList<String>();
		String pathpart = "";
		char c;
		for (int i = 0; i < path.length(); i++) {
			c = path.charAt(i);
			if (c != '/') {
				pathpart += c;
			} else {
				if (path.charAt(i + 1) == '/') {
					pathpart += "/";
					i++;
				} else {
					result.add(pathpart);
					pathpart = "";
				}
			}
		}
		result.add(pathpart);
		return result;
	}

	/**
	 * Adds the special pmioPort blocks at the right position with the right port
	 * number in the array. PMIOPorts are numbered from 1 to n, but they are
	 * discriminate by the value "left" and "right". The first left port is
	 * assigned to the first left port of the subsystem for right ports likewise,
	 * but left and right can occur in arbitrary order.
	 *
	 * @param pmioWorklist
	 *           All pmio ports of the subsystem.
	 * @param portBlocks
	 *           The array where the ports shall sorted.
	 */
	private void pmioToLRCon(HashMap<Integer, Block> pmioWorklist,
			HashMap<Integer, Block>[] portBlocks) {
		// Port count starts at 1
		for (int i = 1; i <= pmioWorklist.size(); i++) {
			Block port = pmioWorklist.get(i);
			if (port.getParameter("Side").equals("Left")) {
				portBlocks[SimulinkPortConstants.L_CONN_PORT_POSITION].put(
						portBlocks[SimulinkPortConstants.L_CONN_PORT_POSITION].size() + 1, port);
			} else {
				portBlocks[SimulinkPortConstants.R_CONN_PORT_POSITION].put(
						portBlocks[SimulinkPortConstants.R_CONN_PORT_POSITION].size() + 1, port);
			}
		}

	}

	/**
	 * Connects incoming and outgoing signals of a subsystem to the related
	 * incoming and outgoing port blocks.
	 *
	 * @param parentblock
	 *           The Subsystem with the in and outgoing signals.
	 * @param targets
	 *           All port blocks covered by this subsystem ordered by type and
	 *           port number. The array separates the port types, the hashmaps
	 *           cover the port numbers. Port number counting starts at 1.
	 */
	private void reassignSignalsToPortBlocks(Block parentblock, HashMap<Integer, Block>[] targets) {

		Block target;

		for (SignalLine si : parentblock.getInSignals()) {

			String destPortString = si.getDstPort().getName();
			// only one enable and trigger block are allowed, no further
			// checking required
			if (destPortString.equals(SimulinkPortConstants.ENABLE_PORT)) {
				target = targets[SimulinkPortConstants.ENABLE_PORT_POSITION].get(1);
				si.setDstBlock(target);
				target.setEnablePort(si.getDstPort());
				target.getInSignals().add(si);
			} else if (destPortString.equals(SimulinkPortConstants.TRIGGER_PORT)) {
				target = targets[SimulinkPortConstants.TRIGGER_PORT_POSITION].get(1);
				si.setDstBlock(target);
				target.setTriggerPort(si.getDstPort());
				target.getInSignals().add(si);
				// inports are numbered from 1 to n
			} else if (destPortString.contains(SimulinkPortConstants.IN_PORT_PREFIX)) {
				// inport block with port == null are related to port 1 by
				// default
				target = targets[SimulinkPortConstants.IN_PORT_POSITION].get(Integer
						.parseInt(destPortString.replace(SimulinkPortConstants.IN_PORT_PREFIX, "")));
				si.setDstBlock(target);
				target.getInPortsMap().put(si.getDstPort().getNumber(), si.getDstPort());
				target.getInSignals().add(si);
			} else if (destPortString.equals(SimulinkPortConstants.IFACTION_PORT)) {
				target = targets[SimulinkPortConstants.IFACTION_PORT_POSITION].get(1);
				si.setDstBlock(target);
				target.setIfactionPort(si.getDstPort());
				target.getInSignals().add(si);
				// LConn/RConn Ports can be src and dest
			} else if (destPortString.contains(SimulinkPortConstants.R_CONN_PORT_PREFIX)) {
				target = targets[SimulinkPortConstants.R_CONN_PORT_POSITION].get(Integer
						.parseInt(destPortString.replace(SimulinkPortConstants.R_CONN_PORT_PREFIX, "")));
				si.setDstBlock(target);
				target.getrConnPortsMap().put(2, si.getDstPort()); // PMIOPorts
				// are
				// connected
				// at the
				// same
				// layer via
				// RConn1
				// -> LConn1
				// seemed to
				// be free;
				// use
				// LConn2
				// just to
				// be on the
				// safe side
				// if LConn1
				// is used
				// after all
				target.getInSignals().add(si);
			} else if (destPortString.contains(SimulinkPortConstants.L_CONN_PORT_PREFIX)) {
				target = targets[SimulinkPortConstants.L_CONN_PORT_POSITION].get(Integer
						.parseInt(destPortString.replace(SimulinkPortConstants.L_CONN_PORT_PREFIX, "")));
				si.setDstBlock(target);
				target.getlConnPortsMap().put(2, si.getDstPort()); // PMIOPorts
				// are
				// connected
				// at the
				// same
				// layer via
				// RConn1
				// -> LConn1
				// seemed to
				// be free;
				// use
				// LConn2
				// just to
				// be on the
				// safe side
				// if LConn1
				// is used
				// after all
				target.getInSignals().add(si);
			}
		}

		// outgoing singnals only by outport, LConn and RConn blocks
		// same rules as by inport blocks
		for (SignalLine si : parentblock.getOutSignals()) {
			String srcPortString = si.getSrcPort().getName();
			if (srcPortString.contains(SimulinkPortConstants.OUT_PORT_PREFIX)) {
				target = targets[SimulinkPortConstants.OUT_PORT_POSITION].get(Integer
						.parseInt(srcPortString.replace(SimulinkPortConstants.OUT_PORT_PREFIX, "")));
				si.setSrcBlock(target);
				target.getOutPortsMap().put(si.getSrcPort().getNumber(), si.getSrcPort());
				target.getOutSignals().add(si);
				// LConn/RConn Ports can be src and dest
			} else if (srcPortString.contains(SimulinkPortConstants.R_CONN_PORT_PREFIX)) {
				target = targets[SimulinkPortConstants.R_CONN_PORT_POSITION].get(Integer
						.parseInt(srcPortString.replace(SimulinkPortConstants.R_CONN_PORT_PREFIX, "")));
				si.setSrcBlock(target);
				target.getrConnPortsMap().put(2, si.getSrcPort()); // PMIOPorts
				// are
				// connected
				// at the
				// same
				// layer via
				// RConn1
				// -> LConn1
				// seemed to
				// be free;
				// use
				// LConn2
				// just to
				// be on the
				// safe side
				// if LConn1
				// is used
				// after all
				target.getInSignals().add(si);
			} else if (srcPortString.contains(SimulinkPortConstants.L_CONN_PORT_PREFIX)) {
				target = targets[SimulinkPortConstants.L_CONN_PORT_POSITION].get(Integer
						.parseInt(srcPortString.replace(SimulinkPortConstants.L_CONN_PORT_PREFIX, "")));
				si.setSrcBlock(target);
				target.getlConnPortsMap().put(2, si.getSrcPort()); // PMIOPorts
				// are
				// connected
				// at the
				// same
				// layer via
				// RConn1
				// -> LConn1
				// seemed to
				// be free;
				// use
				// LConn2
				// just to
				// be on the
				// safe side
				// if LConn1
				// is used
				// after all
				target.getInSignals().add(si);
			} else if (srcPortString.contains(SimulinkPortConstants.STATE_PORT)) {
				MeMoPlugin.out
						.println("[WARNING] Subsystem with state port found! Never seen before! \n"
								+ "Implement the missing parts in " + ModelBuilder.class.getCanonicalName()
								+ "Method: reassignSignalsToPortBlocks(...)");
				// don't forget to implement the missing part in createSystem(...)
				// to get the state port block
			}
		}

	}

	/**
	 * Unfolds the branches of a line between blocks. A <code>SignalLine</code>
	 * will be created for every found source-destination relation.
	 *
	 * @since 28.1.2011
	 *
	 * @param model
	 *           the model that is currently build
	 * @param signals
	 *           all yet found lines
	 * @param src
	 *           the source Block from which the line comes
	 * @param dst
	 *           the destination Block from which the line comes
	 * @param lineSection
	 *           the line section with possible branches
	 * @param localName2Block
	 *           register of Blocks in this model (Pre-condition: all blocks of
	 *           the current level are added)
	 * @param lvl
	 *           the current layer of the system
	 * @param srcPort
	 *           the port where the line starts
	 * @param dstPort
	 *           the port where the line ends
	 */
	private void findBranches(Model model, List<SignalLine> signals, MDLSection lineSection,
			HashMap<String, Block> localName2Block, int lvl) {
		List<MDLSection> sources = new ArrayList<MDLSection>();
		List<MDLSection> destinations = new ArrayList<MDLSection>();

		List<MDLSection> worklist = new ArrayList<MDLSection>();
		worklist.add(lineSection);

		// get all sources and destinations
		while (!worklist.isEmpty()) {
			String srcID;
			String dstID;
			MDLSection section = worklist.remove(0);

			worklist.addAll(section.getSubSections(SimulinkSectionConstants.BRANCH_SECTION_TYPE));
			srcID = section.getParameter(SimulinkParameterNames.SIGNAL_SRCPORT_PARAMETER);
			dstID = section.getParameter(SimulinkParameterNames.SIGNAL_DSTPORT_PARAMETER);
			if (srcID != null) {
				sources.add(section);
			}
			if (dstID != null) {
				destinations.add(section);
			}
		}

		// create all Lines

		for (MDLSection sourceSection : sources) {
			for (MDLSection destSection : destinations) {
				Block srcBlock = localName2Block.get(sourceSection
						.getParameter(SimulinkParameterNames.SIGNAL_SRCBLOCK_PARAMETER));
				String srcPortString = sourceSection
						.getParameter(SimulinkParameterNames.SIGNAL_SRCPORT_PARAMETER);
				Block dstBlock = localName2Block.get(destSection
						.getParameter(SimulinkParameterNames.SIGNAL_DSTBLOCK_PARAMETER));
				String dstPortString = destSection
						.getParameter(SimulinkParameterNames.SIGNAL_DSTPORT_PARAMETER);

				String signalLineName = sourceSection
						.getParameter(SimulinkParameterNames.SIGNAL_NAME_PARAMETER);

				Port srcPort = getSrcPortFromString(srcBlock, srcPortString, model);
				Port dstPort = getDstPortFromString(dstBlock, dstPortString, model);

				SignalLine s = new SignalLine(model.nextID(), lvl, srcBlock, dstBlock, srcPort,
						dstPort, signalLineName);
				s.setIsBranched(true);
				signals.add(s);

			}
		}
	}

	/**
	 * Returns the id of the parent parsed out of a linkNode String. A linkNode
	 * String pattern looks like "[parentID anOtherNumber anOtherNumber]", so it
	 * returns just the parentID by splitting the String. Returns -1 if no ID
	 * found.
	 *
	 * @param linkNode
	 *           the linkNode parameter String
	 * @return the parent id defined by the linkNode or -1
	 */
	public int getParentID(String linkNode) {
		int parentID = -1;
		String[] split = linkNode.split("\\D");
		if (split.length > 1) {
			try {
				return Integer.parseInt(split[1]);
			} catch (NumberFormatException e) {
				MeMoPlugin.logException(e.toString(), e);
			}
		}
		return parentID;
	}

	/**
	 * Returns the statechart of an State given by its id.
	 *
	 * @param charts
	 *           the map of all available statecharts
	 * @param chartElements
	 *           the map of all statechart elements
	 * @param parentID
	 *           the id of the item to which the belonging chart shall be found
	 * @return the chart to which the item given be parentID belongs or null
	 */
	private StateflowChart getChartFromParent(Map<Integer, StateflowChart> charts,
			Map<Integer, StateflowItem> chartElements, int parentID) {
		StateflowChart result = null;
		result = charts.get(parentID);
		if (result == null) {
			// should be a state
			result = charts.get(((State) chartElements.get(parentID)).getChartId());
		}

		return result;
	}

	/**
	 * Creates a configuration and adds it to the given model. All subsections of
	 * the configuration section will be flattened an the parameters added. No
	 * renaming of the parameters like dot notation is used.
	 *
	 * @param model
	 *           the model to which the configuration shall belong
	 * @param configurationSection
	 *           the section where the configuration starts
	 */
	private void createConfigurations(Model model, MDLSection configurationSetSection) {
		UnmodifiableList<MDLSection> configurationSections = configurationSetSection.getSubSections()
				.get(0).getSubSections();
		for (MDLSection childSection : configurationSections) {
			Configuration config = new Configuration(model.nextID(), childSection);
			config.setParameter(getAllParametersRecursively(childSection,
					new HashMap<String, String>()));
			model.getConfigurations().add(config);
		}
	}

	/**
	 * Collects recursively all parameters of a section and its subsection.
	 * Doesn't rename the parameters.
	 *
	 * @param section
	 *           the section of which the parameters shall be collected
	 * @param map
	 *           the map where the parameters shall be collected, if null a new
	 *           map will be created
	 * @return returns the map with the collected parameters
	 */
	private Map<String, String> getAllParametersRecursively(MDLSection section,
			HashMap<String, String> map) {
		if (map == null) {
			map = new HashMap<String, String>();
		}

		for (String name : section.getParameterNames()) {
			map.put(name, section.getParameter(name));
		}

		for (MDLSection childSection : section.getSubSections()) {
			map.putAll(getAllParametersRecursively(childSection, map));
		}

		return map;
	}

	/**
	 * Connects Goto blocks and their corresponding From blocks with a Line.
	 *
	 * @param session
	 *           the session to work on the Database
	 * @param m
	 *           the model needed to generate fresh id's
	 */
	@SuppressWarnings("unchecked")
	public void reassignGotos(MeMoPersistenceManager persistence, Model m) {
		List<Block> gotoResult = new ArrayList<Block>();
		List<Block> fromResult = new ArrayList<Block>();

		// collect all Goto blocks an handle them by their tagVisibility
		gotoResult = persistence
				.getItemsByCriteria(Block.class, Restrictions.eq(MemoFieldNames.BLOCK_TYPE_FIELD,
						SimulinkBlockConstants.GOTO_BLOCKTYPE));

		for (Block b : gotoResult) {
			String tag = b.getParameter(SimulinkParameterNames.GOTO_TAG);
			// locals overwrites scope and global on the same level in the same
			// system part
			if (b.getParameter(SimulinkParameterNames.TAG_VISIBILITY).equals(
					SimulinkParameterValues.GOTO_TAG_LOCAL)) {
				fromResult = persistence.getItemsByCriteria(Block.class, Restrictions.eq(
						MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.FROM_BLOCKTYPE),
						Restrictions.eq(MemoFieldNames.BLOCK_PARENT_FIELD, b.getParent()));
				for (Block b2 : fromResult) {
					if (tag.equals(b2.getParameter(SimulinkParameterNames.GOTO_TAG))) {
						createGotoLine(b, b2, m);
					}
				}
				// scoped overwrites everything in the same system part from
				// the same level starting and the underlying levels
			} else if (b.getParameter(SimulinkParameterNames.TAG_VISIBILITY).equals(
					SimulinkParameterValues.GOTO_TAG_GLOBAL)) {
				fromResult = getFromsInScopeGlobal(b, persistence);
				for (Block b2 : fromResult) {
					if (tag.equals(b2.getParameter(SimulinkParameterNames.GOTO_TAG))) {
						createGotoLine(b, b2, m);
					}
				}
				// global is in the whole system reachable where it is not
				// overwritten
			} else if (b.getParameter(SimulinkParameterNames.TAG_VISIBILITY).equals(
					SimulinkParameterValues.GOTO_TAG_SCOPED)) {
				fromResult = getFromsInScopeScoped(b, persistence);
				for (Block b2 : fromResult) {
					if (tag.equals(b2.getParameter(SimulinkParameterNames.GOTO_TAG))) {
						createGotoLine(b, b2, m);
					}
				}
			}
		}
	}

	/**
	 * @param b
	 *           a From block with tagVisibility "global".
	 * @param session
	 *           The session to reach the Database
	 * @return a List of From blocks, which are in scope of the given goto block
	 */
	@SuppressWarnings("unchecked")
	// cast typesafe, because Criterions are typed
	private List<Block> getFromsInScopeGlobal(Block b, MeMoPersistenceManager persistence) {
		List<Block> result = persistence
				.getItemsByCriteria(Block.class, Restrictions.eq(MemoFieldNames.BLOCK_TYPE_FIELD,
						SimulinkBlockConstants.FROM_BLOCKTYPE)); // get all from
		// Blocks
		List<Block> killSet = new ArrayList<Block>(); // killSet will be over
		// approximated
		String tag = b.getParameter(SimulinkParameterNames.GOTO_TAG);

		// delete wrong tags. Not filtered in the Database query so
		// no HQL is needed
		for (Block b2 : result) {
			if (!tag.equals(b2.getParameter(SimulinkParameterNames.GOTO_TAG))) {
				killSet.add(b2);
			}
		}

		// delete overwritten by locals
		for (Block b2 : result) {
			List<Block> temp = persistence.getItemsByCriteria(Block.class, Restrictions.eq(
					MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.GOTO_BLOCKTYPE),
					Restrictions.eq(MemoFieldNames.BLOCK_LEVEL_FIELD, b2.getLevel()));
			for (Block tempB : temp) {
				if (tag.equals(tempB.getParameter(SimulinkParameterNames.GOTO_TAG))
						&& ((tempB.getLevel() == 1) || (tempB.getParent().getId() == b2.getParent()
								.getId())) // level = 1 => parent = null, b2 has the
												// same level
						&& tempB.getParameter(SimulinkParameterNames.TAG_VISIBILITY).equals(
								SimulinkParameterValues.GOTO_TAG_LOCAL)) {
					killSet.add(b2);
				}
			}
		}

		// delete overwritten by scope
		List<Block> temp = persistence
				.getItemsByCriteria(Block.class, Restrictions.eq(MemoFieldNames.BLOCK_TYPE_FIELD,
						SimulinkBlockConstants.GOTO_BLOCKTYPE));
		temp.remove(b); // don't remove Blocks in scope of the examined block!
		for (Block block : temp) {
			if (!block.getParameter(SimulinkParameterNames.GOTO_TAG).equals(tag)) {
				continue; // goto blocks without the same tag have no effect
			}
			if (block.getParameter(SimulinkParameterNames.TAG_VISIBILITY).equals(
					SimulinkParameterValues.GOTO_TAG_SCOPED)) { // we want only
																				// scoped goto
																				// blocks
				List<Block> subsystems;
				if (block.getParent() != null) { // block on top level?
					subsystems = persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_PARENT_FIELD, block.getParent()), Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.SUBSYSTEM_BLOCKTYPE)); // get
																																		// all
																																		// underlying
																																		// subsystems
					killSet.addAll(persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_PARENT_FIELD, block.getParent()), Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.FROM_BLOCKTYPE))); // all
																																// blocks
																																// in
																																// the
																																// same
																																// subsystem
																																// as
																																// the
																																// scoped
																																// block
																																// are
																																// in
																																// scope,
																																// overapproximates,
																																// get's
																																// blocks
																																// with
																																// wrong
																																// tag
				} else {
					subsystems = Collections.EMPTY_LIST;
					killSet.addAll(persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.FROM_BLOCKTYPE))); // scope
																																// on
																																// top
																																// level
																																// overwrites
																																// everything
				}
				while (!subsystems.isEmpty()) { // check all subsystems and the
															// subsystems of the subsystems
					Block currentSub = subsystems.remove(0);
					subsystems.addAll(persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_PARENT_FIELD, currentSub), Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.SUBSYSTEM_BLOCKTYPE))); // get
																																		// subsystems
																																		// of
																																		// the
																																		// subsystem
					killSet.addAll(persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_PARENT_FIELD, currentSub), Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.FROM_BLOCKTYPE)));
				}
			}
		}

		result.removeAll(killSet);

		return result;
	}

	/**
	 * @param b
	 *           a From block with tagVisibility "scoped".
	 * @param session
	 *           The session to reach the Database
	 * @return a List of From blocks, which are in scope of the given goto block
	 */
	@SuppressWarnings("unchecked")
	private List<Block> getFromsInScopeScoped(Block b, MeMoPersistenceManager persistence) {
		List<Block> result = persistence
				.getItemsByCriteria(Block.class, Restrictions.eq(MemoFieldNames.BLOCK_TYPE_FIELD,
						SimulinkBlockConstants.FROM_BLOCKTYPE), Restrictions.ge(
						MemoFieldNames.BLOCK_LEVEL_FIELD, b.getLevel())); // possible
		// scope
		// of
		// examined
		// block
		// b
		// only
		// downwards
		List<Block> killSet = new ArrayList<Block>(); // killSet will be over
		// approximated
		String tag = b.getParameter(SimulinkParameterNames.GOTO_TAG);

		// delete wrong tags
		// delete blocks without indirect parent = parent of b
		// Not filtered in the Database query so no HQL is needed
		for (Block b2 : result) {
			if (!tag.equals(b2.getParameter(SimulinkParameterNames.GOTO_TAG))) {
				killSet.add(b2);
			} else { // indirect child of parent of b?
				if (!(b.getParent() instanceof Block)) {
					break; // all in scope! Nothing to kill
				}
				if (!(b2.getParent() instanceof Block)) { // not in scope of b,
																		// because b2 is on top
																		// level, but b not
					killSet.add(b2);
					continue;
				}
				Block parent = b2;
				// search path upwards to b
				while (parent.getId() != b.getParent().getId()) {
					if (!(parent.getParent() instanceof Block)) { // no path found =>
																					// out of scope
						killSet.add(b2);
						break;
					}
					parent = (Block) parent.getParent();
				}
			}
		}

		// delete overwritten by locals
		for (Block b2 : result) {
			List<Block> temp = persistence.getItemsByCriteria(Block.class, Restrictions.eq(
					MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.GOTO_BLOCKTYPE),
					Restrictions.eq(MemoFieldNames.BLOCK_LEVEL_FIELD, b2.getLevel())); // scope
																												// only
																												// downwards
			for (Block tempB : temp) {
				if (tag.equals(tempB.getParameter(SimulinkParameterNames.GOTO_TAG))
						&& ((tempB.getLevel() == 1) || (tempB.getParent().getId() == b2.getParent()
								.getId())) // level = 1 => parent = null, b2 has the
												// same level
						&& tempB.getParameter(SimulinkParameterNames.TAG_VISIBILITY).equals(
								SimulinkParameterValues.GOTO_TAG_LOCAL)) {
					killSet.add(b2);
				}
			}
		}

		// delete overwritten by scope
		List<Block> temp = persistence
				.getItemsByCriteria(Block.class, Restrictions.eq(MemoFieldNames.BLOCK_TYPE_FIELD,
						SimulinkBlockConstants.GOTO_BLOCKTYPE), Restrictions.ge(
						MemoFieldNames.BLOCK_LEVEL_FIELD, b.getLevel())); // scope
		// only
		// downwards
		temp.remove(b); // don't remove Blocks in scope of the examined block!
		for (Block block : temp) {
			if (!block.getParameter(SimulinkParameterNames.GOTO_TAG).equals(tag)) {
				continue; // goto blocks without the same tag have no effect
			}
			if (block.getParameter(SimulinkParameterNames.TAG_VISIBILITY).equals(
					SimulinkParameterValues.GOTO_TAG_SCOPED)) { // we want only
																				// scoped goto
																				// blocks
				List<Block> subsystems;
				if (block.getParent() != null) {
					subsystems = persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_PARENT_FIELD, block.getParent()), Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.SUBSYSTEM_BLOCKTYPE)); // get
																																		// all
																																		// underlying
																																		// subsystems
					killSet.addAll(persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_PARENT_FIELD, block.getParent()), Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.FROM_BLOCKTYPE))); // all
																																// blocks
																																// in
																																// the
																																// same
																																// subsystem
																																// as
																																// the
																																// scoped
																																// block
																																// are
																																// in
																																// scope,
																																// overapproximates,
																																// get's
																																// blocks
																																// with
																																// wrong
																																// tag
				} else {
					subsystems = Collections.EMPTY_LIST;
					killSet.addAll(persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.FROM_BLOCKTYPE))); // scope
																																// on
																																// top
																																// level
																																// overwrites
																																// everything
				}
				while (!subsystems.isEmpty()) { // check all subsystems and the
															// subsystems of the subsystems
					Block currentSub = subsystems.remove(0);
					subsystems.addAll(persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_PARENT_FIELD, currentSub), Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.SUBSYSTEM_BLOCKTYPE)));
					killSet.addAll(persistence.getItemsByCriteria(Block.class, Restrictions.eq(
							MemoFieldNames.BLOCK_PARENT_FIELD, currentSub), Restrictions.eq(
							MemoFieldNames.BLOCK_TYPE_FIELD, SimulinkBlockConstants.FROM_BLOCKTYPE)));
				}
			}
		}

		result.removeAll(killSet);

		return result;
	}

	/**
	 * Creates a line between a goto and a from block
	 *
	 * @param gotoB
	 *           the source block of the line
	 * @param fromB
	 *           the destination block of the line
	 * @param m
	 *           the model to create new id's for creating new ports
	 */
	private void createGotoLine(Block gotoB, Block fromB, Model m) {
		Port srcPort = new Port(m.nextID(), SimulinkPortConstants.OUT_PORT_PREFIX + "1", 1);
		Port dstPort = new Port(m.nextID(), SimulinkPortConstants.IN_PORT_PREFIX + "1", 1);
		SignalLine line = new SignalLine(m.nextID(), -1, gotoB, fromB, srcPort, dstPort, "");

		gotoB.getOutPorts().add(srcPort);
		fromB.getInPorts().add(dstPort);

		gotoB.getOutSignals().add(line);
		fromB.getInSignals().add(line);

		m.getSignalLines().add(line);
	}

	/**
	 * Starts the analyze process of the bus mux system. Should be done after
	 * goto's are reassigned so the bus can be followed properly through
	 * goto/from blocks.
	 */
	public void busMuxAnlaysis() {
		busanalyzer.analyze();
	}

}
