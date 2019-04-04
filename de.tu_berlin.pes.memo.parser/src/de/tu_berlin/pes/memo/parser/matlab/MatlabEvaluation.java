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

package de.tu_berlin.pes.memo.parser.matlab;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.impl.Block;
import de.tu_berlin.pes.memo.model.impl.Configuration;
import de.tu_berlin.pes.memo.model.impl.Model;
import de.tu_berlin.pes.memo.model.impl.ModelItem;
import de.tu_berlin.pes.memo.model.impl.Port;
import de.tu_berlin.pes.memo.model.impl.ReferenceBlock;
import de.tu_berlin.pes.memo.model.impl.SignalLine;
import de.tu_berlin.pes.memo.model.impl.WSVariable;
import de.tu_berlin.pes.memo.model.util.SimulinkBlockConstants;
import de.tu_berlin.pes.memo.model.util.SimulinkParameterNames;
import de.tu_berlin.pes.memo.model.util.SimulinkPortConstants;
import de.tu_berlin.pes.memo.parser.BusMuxAnalyzer;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

/**
 * Checks the given block parameter for updates via matlab.
 *
 * @author vseeker, Joachim Kuhnert, Alexander Reinicke
 */
public class MatlabEvaluation {

	/**
	 * @uml.property name="_model"
	 * @uml.associationEnd
	 */
	private Model _model;
	private HashMap<Integer, Block> _blocks;
	private HashMap<Integer, SignalLine> _signalLines;
	private String _modelName;
	/**
	 * @uml.property name="_matlab"
	 * @uml.associationEnd
	 */
	private MatlabConnector _matlab;
	private boolean _postCompile;
	private boolean _compileSuccess = false;
	private BusMuxAnalyzer busanalyzer = null;
	private Collection<String> modelRefs;
	private static boolean jmBridgeBehaviour = false;
	private static final String jmBridgeVarName = "params_mesJMBridge";
	//private IProject project;
	private String tmpVarParam = "MeMo.tempVarNames";

	/**
	 * Creates a matlab evaluation process with the given parameter.
	 *
	 * @param model
	 *           the model to be evaluated.
	 * @param matlab
	 *           the matlab interface. A connection must have already been
	 *           established.
	 * @param postCompile
	 *           true if a second evaluation shall be executed after a model
	 *           compile.
	 */
	public MatlabEvaluation(Model model, Collection<String> modelRefs, MatlabConnector matlab,
			boolean postCompile, BusMuxAnalyzer busAnalyzer, boolean jmBridge) {

		_model = model;
		_blocks = model.getBlockMap();
		_signalLines = model.getSignalMap();
		_modelName = model.getName();
		_matlab = matlab;
		_postCompile = postCompile;
		busanalyzer = busAnalyzer;
		jmBridgeBehaviour = jmBridge;
		this.modelRefs = modelRefs;
	}

	/**
	 * Performs the evaluation of this evaluation process.
	 */
	public void evaluate() {
		// avoid nullpointer exceptions
		if (busanalyzer == null) {
			busanalyzer = new BusMuxAnalyzer();
		}
		MeMoPlugin.out.println("[INFO] Performing Matlab parameter update...");
		internalEvaluate();
		MeMoPlugin.out.println("[INFO] Matlab parameter update done.");
	}

	protected void internalEvaluate() {

		if (_postCompile) {
			MeMoPlugin.out.println("[INFO] Compiling Simulink model " + _model.getName() + "...");
			_compileSuccess = _matlab.eval(_modelName + "([],[],[],'compile')");
			for (String refName : modelRefs) {
				MeMoPlugin.out.println("[INFO] Compiling Referenced Model " + refName);
				if (!_matlab.eval(refName + "([],[],[],'compile')")) {
					MeMoPlugin.out
							.println("[WARNING] Compiling faild! Trying to set \"SimulationMode\" to \"normal\"");
					_matlab.eval("MemoStatus = get_param('" + refName + "', 'SimulationStatus')");
					Object status = _matlab.getVariable("MemoStatus");
					if ("paused".equals(status)) {
						continue;
					}
					_matlab.eval("set_param('" + refName + "','SimulationMode','normal');");
					if (!_matlab.eval(refName + "([],[],[],'compile')")) {
						MeMoPlugin.out.println("[ERROR] Compiling failed!");
						_matlab.eval(_modelName + "('term')");
						for (String refName2 : modelRefs) {
							if (refName2 == refName) {
								return; // Don't stop uncompiled model ref and following
											// (yet not compiled)
							}
							_matlab.eval(refName2 + "([],[],[],'term')");
						}
					}
				}
			}

			try {
				if (!_compileSuccess) {
					MeMoPlugin.out
					.println("[Warning] Compile Failed. Trying update port parameters without compiling");
					updateParameterFromMatlab();
					return;
				}

				MeMoPlugin.out.println("[INFO] Updating port parameter via matlab after compile...");
				updateParameterFromMatlab();
			} catch (Exception e) {
				MeMoPlugin.out.println("[ERROR] Parameter update not successful");
				MeMoPlugin.logException("Parameterupdate failed", e);
			}

			_matlab.eval(_modelName + "('term')");
			for (String refName : modelRefs) {
				_matlab.eval(refName + "([],[],[],'term')");
			}
		}
	}

	/**
	 * Updates all registered parameter container with the corresponding
	 * parameter in matlab or adds new, if matlab has more.
	 *
	 */
	private void updateParameterFromMatlab() {
		List<Block> redo = new ArrayList<Block>();
		List<SignalLine> sredo = new ArrayList<SignalLine>();
		int blockcnt = 0;
		int signalcnt = 0;

		getWorkspaceVariables();

		for (Block block : _blocks.values()) {

			Object[] parameterValueMap = collectBlockParameter(block);

			if (parameterValueMap.length < 1) {
				MeMoPlugin.out.println("[ERROR] Can not handle " + block.getFullQualifiedName(true)
						+ " id " + block.getId() + " type " + block.getType());
				redo.add(block);
				continue;
			}
			blockcnt++;

			if ((blockcnt % 50) == 0) {
				MeMoPlugin.out.println("\t processing block " + blockcnt + " of "
						+ _blocks.values().size());
			}
			addBlockParameter(block, parameterValueMap);
		}
		for (Block undone : redo) {
			MeMoPlugin.out.println("\t redoing block: " + undone);
			Object[] parameterValueMap = collectBlockParameter(undone);
			addBlockParameter(undone, parameterValueMap);
		}
		for (SignalLine signal : _signalLines.values()) {
			Object[] parameterValueMap = collectSignalParameter(signal);

			if (parameterValueMap.length < 1) {
				MeMoPlugin.out.println("[Error] Can not handle "
						+ signal.getSrcBlock().getFullQualifiedName(true) + "//" + signal.getId()
						+ " id " + signal.getId());
				sredo.add(signal);
				continue;
			}
			signalcnt++;

			if ((signalcnt % 50) == 0) {
				MeMoPlugin.out.println("\t processing signal line " + signalcnt + " of "
						+ _signalLines.values().size());
			}
			addSignalParameter(signal, parameterValueMap);
		}
		for (SignalLine undone : sredo) {
			MeMoPlugin.out.println("\t redoing signal line: " + undone);
			Object[] parameterValueMap = collectSignalParameter(undone);
			addSignalParameter(undone, parameterValueMap);
		}

		// changes original values -> do it last
		// calculateVariables();
		//replaceVariablesByValue();

	}

	/**
	 * Examine the MATLab workspace an get information about the variables in the
	 * workspace and store them in the model.
	 */
	private void getWorkspaceVariables() {
		Object[] result = null;
		int tries = 0;
		int timeout = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT) * 1000;
		int maxTries = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TRIES);
		String whosVarName = "whosVar"; // generateMatlabVarname("whosVar"); //
													// Generate var name, if "whos" already
													// taken?
		RunMatlabCommand thread;

		// get a List of variable parameters (including variable name)
		while ((result == null) && (tries < maxTries)) {

			tries++;

			if (tries > 1) {
				MeMoPlugin.out.println("[INFO] Error detected getting workspace variables"
						+ " , try again! Try: " + tries);
				thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.eval,
						"clear " + whosVarName, timeout);

				if (!thread.isSuccessful()) {
					// ignore running thread
					continue;
				}
			}

			thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.eval,
					whosVarName + " = whos;", timeout);

			if (!thread.isSuccessful()) {
				// ignore running thread
				continue;
			}

			thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.getVariable,
					whosVarName, timeout);

			if (!thread.isSuccessful()) {
				// ignore running thread
				continue;
			}

			result = (Object[]) thread.getResult();

			if ((result != null) && (result.length > 0) && jmBridgeBehaviour) { // JMBridge
																										// encapsulates
																										// results
																										// in
																										// an
																										// extra
																										// Object
																										// array
				result = (Object[]) result[0];
			}
		}

		thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.eval, "clear "
				+ whosVarName, timeout);

		// create found variables
		String[] paramNames = (String[]) result[0];
		Object[] vars = (Object[]) result[1];

		for (Object paramValues : vars) {
			WSVariable var = new WSVariable(_model.nextID());

			fillVariable(var, paramNames, paramValues, _matlab);

			_model.getWorkspaceVariables().add(var);
		}

	}

	public static void fillVariable(WSVariable var, String[] paramNames, Object paramValues,
			MatlabConnector matlab) {
		int timeout = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT) * 1000;
		int maxTries = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TRIES);

		for (int i = 0; i < paramNames.length; i++) {
			addParamToWSVariable(var, paramNames[i], ((Object[]) paramValues)[i]);
		}

		String varName = var.getParameter().get(SimulinkParameterNames.VAR_NAME);

		if (jmBridgeVarName.equals(varName)) { // don't read the jmBrigde
															// Variable, it's not part of the
															// model
			return;
		}

		var.setName(varName);

		int tries = 0;

		// get the value of the variable
		boolean gotResult = false;
		while (!gotResult && (tries < maxTries)) {

			tries++;

			if (tries > 1) {
				MeMoPlugin.out.println("[INFO] Error detected getting workspace variable value"
						+ varName + " , try again! Try: " + tries);
				// thread = RunMatlabCommand.executeCommand(_matlab,
				// RunMatlabCommand.Command.eval,
				// "clear " + whosVarName, timeout);
				//
				// if (!thread.isSuccessful()) {
				// // ignore running thread
				// tries++;
				// continue;
				// }
			}

			RunMatlabCommand thread = RunMatlabCommand.executeCommand(matlab,
					RunMatlabCommand.Command.getVariable, varName, timeout);

			if (!thread.isSuccessful()) {
				// ignore running thread
				continue;
			}

			gotResult = true;
			var.setValue(ReturnFormatter.formatResult(jmBridgeBehaviour ? ((Object[]) thread
					.getResult())[0] : thread.getResult()));
		}
	}

	/**
	 * Fills the given variable with the given parameter. Therefore it converts
	 * the parameter object into a String.
	 *
	 * @param var
	 *           The variable to which the parameter belongs
	 * @param name
	 *           The name of the parameter
	 * @param obj
	 *           The parameter value
	 */
	private static void addParamToWSVariable(WSVariable var, String name, Object obj) {
		if (obj instanceof String) { // name, class, nesting.function
			var.getParameter().put(name, (String) obj);
		} else if (obj instanceof double[]) { // bytes, size
			var.getParameter().put(name, ReturnFormatter.formatResult(obj));
		} else if (obj instanceof boolean[]) { // global, sparse, complex,
															// nesting.level, persistent
			var.getParameter().put(name, ReturnFormatter.formatResult(obj));
		} else if (obj instanceof Object[]) {
			if (((Object[]) obj).length == 2) { // nesting
				String[] paramNames = (String[]) ((Object[]) obj)[0];
				Object paramValues = ((Object[]) ((Object[]) obj)[1])[0];
				for (int i = 0; i < paramNames.length; i++) {
					addParamToWSVariable(var, name + "." + paramNames[i], ((Object[]) paramValues)[i]);
				}
			} else { // fall back case, shouldn't happen
				var.getParameter().put(name, ReturnFormatter.formatResult(obj));
			}
		}
	}

	/**
	 * Collects parameter with corresponding values for the given block id on the
	 * matlab site and returns the result.
	 *
	 * @param blockId
	 *           the block to be analyzed
	 * @return parameter name and parameter value map, where the parameter names
	 *         are in the first and the values in the second half of the array.
	 */
	private Object[] collectBlockParameter(Block block) {
		Object[] result = null;
		int tries = 0;
		// long starttime;
		int timeout = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT) * 1000;
		int maxTries = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TRIES);
		RunMatlabCommand thread;
		String blockName = (block.getFullQualifiedName(true)).replace("\\n", " ").replace("'", "''");

		while ((result == null) && (tries < maxTries)) {

			if (tries > 0) {
				MeMoPlugin.out.println("[INFO] Error detected updating block "
						+ block.getFullQualifiedName(true) + " , try again! Try: " + tries);
			}

			tries++;

			String script = "MeMo_block_params = get_param('"
					+ blockName
					+ "', 'ObjectParameters');"
					+ "MeMo_params_asstring =  fieldnames(MeMo_block_params); "
					+ "MeMo_params_asstring = char(MeMo_params_asstring); "
					+ "MeMo_nParams = size(MeMo_params_asstring, 1); "
					+ "MeMo_test = cell(MeMo_nParams,2); "
					+ "for MeMo_d = 1:MeMo_nParams "
					+ "MeMo_temp_str = strtrim(MeMo_params_asstring(MeMo_d,:)); "
					+ "MeMo_temp_obj = get_param( '"
					+ blockName
					+ "', MeMo_temp_str ); "
					+ "MeMo_test{MeMo_d,1} = MeMo_temp_str; "
					+ "if (isa(MeMo_temp_obj, 'Capabilities') || isa(MeMo_temp_obj, 'Simulink.RunTimeBlock') || isa(MeMo_temp_obj, 'RuntimeObject') || isa(MeMo_temp_obj, 'Simulink.Mask')) "
					+ "MeMo_test{MeMo_d,2} = 'null'; "
					+ "elseif strcmp(MeMo_temp_str, 'MaskWSVariables') == 1 "
					+ "MeMo_test{MeMo_d,2} = 'null'; "
					+ "MeMo_f = 1; "
					+ "MeMo_test{MeMo_nParams+1,1} = '"
					+ tmpVarParam
					+ "'; "
					+ "for MeMo_e = 1:size(MeMo_temp_obj,2) "
					+ "MeMo_f = MeMo_f+1;"
					+ "MeMo_test{MeMo_nParams+1,2} = strcat(MeMo_test{MeMo_nParams+1,2}, MeMo_temp_obj(MeMo_e).Name, ';'); "
					+ "MeMo_test{MeMo_nParams+MeMo_f,1} = MeMo_temp_obj(MeMo_e).Name;"
					+ "if isa(MeMo_temp_obj(MeMo_e).Value, 'Simulink.NumericType') "
					+ "MeMo_test{MeMo_nParams+MeMo_f,2} = strcat(" + "'fixdt(',"
					+ "num2str(MeMo_temp_obj(MeMo_e).Value.getsigned),',',"
					+ "num2str(MeMo_temp_obj(MeMo_e).Value.getwordlength), ',',"
					+ "num2str(MeMo_temp_obj(MeMo_e).Value.getfractionlength),')');" + "else "
					+ "MeMo_test{MeMo_nParams+MeMo_f,2} = MeMo_temp_obj(MeMo_e).Value; " + "end; "
					+ "end; " + "elseif isstruct(MeMo_temp_obj) "
					+ "MeMo_test{MeMo_d,2} = struct2cell(MeMo_temp_obj); " + "else "
					+ "MeMo_test{MeMo_d,2} = MeMo_temp_obj; " + "end; " + "end ";

			thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.eval, script,
					timeout);

			if (!thread.isSuccessful()) {
				// thread.stop(); ignore running thread
				continue;
			}

			thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.getVariable,
					"MeMo_test", timeout);

			if (!thread.isSuccessful()) {
				// thread.stop(); ignore running thread
				continue;
			}
			result = (Object[]) thread.getResult();

			thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.eval,
					"clear MeMo_test;" + "clear MeMo_temp_str;" + "clear MeMo_temp_obj;"
							+ "clear MeMo_params_asstring;" + "clear MeMo_nParams;"
							+ "clear MeMo_block_params" + "clear MeMo_e;" + "clear MeMo_d;"
							+ "clear MeMo_f;", timeout);

			if (!thread.isSuccessful()) {
				// thread.stop(); ignore running thread
				result = null;
				continue;
			}
		}

		// Avoid NullpointerExceptions
		if ((result != null) && (result.length > 0)) {

			if (jmBridgeBehaviour) { // JMBridge encapsulates results in an extra
												// Object array
				result = (Object[]) result[0];
			}

			if (result[0] instanceof String) {
				return result;
			} else {
				return (Object[]) result[0];
			}
		} else {
			MeMoPlugin.out
			.println("[DEBUG] Unexpected result in MatlabPortEvaluation.collectBlockParameter!");
			return new Object[0];
		}
	}

	private Object[] collectSignalParameter(SignalLine signal) {
		Object[] result = null;
		Object[] tmp = null;
		Object[] tmpP1 = null; // begin //for composing return-value when branch
										// exists
		Object[] tmpP2 = null; // end
		Object[] tmpP3 = null; // middle
		boolean met = false; // is signal-line totally "pointed out" via matlab so
									// far?
		Block srcBlock = checkForSubsys(signal.getSrcBlock(), signal);
		Block dstBlock = checkForSubsys(signal.getDstBlock(), signal); // use
																							// dstPort
																							// for
																							// evaluation
		Port srcPort = signal.getSrcPort(); // signals without srcPort (on
														// matlabside only!) are candidates
														// for being branch-signals
		Port dstPort = signal.getDstPort();
		String sSrcPort = convertPortName(srcPort); // converted String-Name of
																	// Port
		String sDstPort = convertPortName(dstPort); // converted String-Name of
																	// that Port
		String srcBlockName = (srcBlock.getFullQualifiedName(true)).replace("\\n", " ").replace("'",
				"''");
		String dstBlockName = (dstBlock.getFullQualifiedName(true)).replace("\\n", " ").replace("'",
				"''");
		String signalName = (srcBlockName + ":" + sSrcPort + "---(" + signal.getId() + ")---"
				+ dstBlockName + ":" + sDstPort);

		if (sDstPort == null) { // check which should never be true
			MeMoPlugin.out.println("A faulty Port called " + dstPort + " is stored in:" + signalName);
			return null;
		}
		if (srcPort == null) { // check which should never be true
			MeMoPlugin.out.println("A faulty Port called " + srcPort + " is stored in:" + signalName);
			return null;
		}

		if (!signal.getIsBranched()) {
			tmp = threadedMatlabInvocation(dstBlockName, sDstPort, signalName, false);
			tmpP2 = convertToPointArray(tmp[1]);
		} else { // now...it's a branch!

			tmp = threadedMatlabInvocation(srcBlockName, sSrcPort, signalName, false);
			tmpP1 = convertToPointArray(tmp[1]);
			tmp = threadedMatlabInvocation(dstBlockName, sDstPort, signalName, false);
			tmpP2 = convertToPointArray(tmp[1]);

			while (!met) {
				if (checkLinesMeet(tmpP1, tmpP2)) {// check if the line-parts "meet"
					met = true;
				} else {
					tmp = threadedMatlabInvocation(null, null, signalName, true);
					tmpP3 = convertToPointArray(tmp[1]);
					tmpP2 = concatPArrays(tmpP3, tmpP2);
				}
			}
			tmpP2 = concatPArrays(tmpP1, tmpP2);
		}

		result = new Object[2];
		result[0] = new String("Points");
		result[1] = tmpP2;

		return result;
	}

	/**
	 * @author Alexander Reinicke
	 * @param inBlock
	 * @param signal
	 * @return
	 */
	private Block checkForSubsys(Block inBlock, SignalLine signal) {
		ModelItem tmp = null;
		if (
		// (
		(inBlock.getParent() != null)
				&& (inBlock.getLevel() > 1)
				&& (inBlock.getLevel() != signal.getLevel())
				// )
				&& ((inBlock.getType().equals(SimulinkBlockConstants.INPORT_BLOCKTYPE))
						|| (inBlock.getType().equals(SimulinkBlockConstants.OUTPORT_BLOCKTYPE))
						|| (inBlock.getType().equals(SimulinkBlockConstants.ENABLEPORT_BLOCKTYPE))
						|| (inBlock.getType().equals(SimulinkBlockConstants.TRIGGERPORT_BLOCKTYPE))
						|| (inBlock.getType().equals(SimulinkBlockConstants.LR_CON_BLOCKTYPE)) || (inBlock
							.getType().equals(SimulinkBlockConstants.ACTIONPORT_BLOCKTYPE)))) { // TODO:
																														// which
																														// other
																														// PortBlocks
																														// exist:
																														// ?StatePort?
			tmp = inBlock.getParent();
		} else {
			tmp = inBlock;
		}

		if (tmp instanceof Block) {
			return (Block) tmp; // surplus: ensure that the parent is a Block (and
										// not ModelItem aka System)
		} else {
			return inBlock;
		}
	}

	// converts the portname to a string accessable in Matlab; Helper for
	// collectSignalParameter
	private String convertPortName(Port myPort) {
		String tmp = myPort.getName();
		if (tmp.contains("out")) {
			return ("Outport(" + tmp.substring(3) + ")"); // determine if is Out or
																			// In and correct
																			// portnumber
		} else if (tmp.contains("in")) {
			return ("Inport(" + tmp.substring(2) + ")");
		} else if (tmp.contains("enable")) {
			return ("Enable");
		} else if (tmp.contains("trigger")) {
			return ("Trigger");
		} else if (tmp.contains("state")) {
			return ("State");
		} else if (tmp.contains("LConn")) {
			return ("LConn(" + tmp.substring(5) + ")");
		} else if (tmp.contains("RConn")) {
			return ("RConn(" + tmp.substring(5) + ")");
		} else if (tmp.contains("ifaction")) {
			return ("Ifaction");
		} else {
			return null;
		}
	}

	private Object[] concatPArrays(Object[] first, Object[] second) {
		Object[] result = null;
		double[] tmp = null;
		int lFirst = first.length;
		int lSecond = second.length;
		result = new Object[(lFirst + lSecond) - 1];
		for (int i = 0; i < lFirst; i++) {
			tmp = (double[]) first[i];
			result[i] = tmp.clone();
		}
		lFirst--; // override last point of first to avoid doubling
		for (int i = 0; i < lSecond; i++) {
			tmp = (double[]) second[i];
			result[lFirst + i] = tmp.clone();
		}
		return result;
	}

	// creates an Array that holds the points as paired elements
	private Object[] convertToPointArray(Object toConvertRaw) {

		double[] toConvert;

		if (toConvertRaw instanceof double[]) {
			toConvert = (double[]) toConvertRaw;
		} else {
			toConvert = new double[0];
		}

		double[] tmp = new double[2];
		int length = toConvert.length;
		Object[] result = new Object[length / 2]; // The pairs in this list are
																// [i,i+n], where
																// n=pointList.length()/2;
		for (int i = 0; i < (length / 2); i++) {
			tmp[0] = toConvert[i];
			tmp[1] = toConvert[i + (length / 2)];
			result[i] = tmp.clone();
		}
		return result;
	}

	// checks if two lines are connected (endPoint of first is beginPoint of
	// second)
	private boolean checkLinesMeet(Object[] first, Object[] second) {
		double[] endPoint = (double[]) first[first.length - 1];
		double[] beginPoint = (double[]) second[0];
		return ((endPoint[0] == beginPoint[0]) && (endPoint[1] == beginPoint[1]));
	}

	// parametrized helper for collectSignalParameter due the occurrence of
	// (multi)branches
	private Object[] threadedMatlabInvocation(String block, String port, String signalName,
			boolean branchChild) {
		Object[] result = null;
		int tries = 0;
		int timeout = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT) * 1000;
		int maxTries = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TRIES);
		RunMatlabCommand thread;

		while ((result == null) && (tries < maxTries)) {

			if (tries > 0) {
				MeMoPlugin.out.println("[INFO] Error detected updating signal " + signalName
						+ " , try again! Try: " + tries);
			}

			tries++;

			if (!branchChild) {
				thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.eval,
						"debugStr = '" + block + ":" + port + "';" + "lineHandle = get_param('" + block
								+ "', 'LineHandles');" + "lastLineHandle = lineHandle." + port + ";"
								+ "linePoints = get_param(lastLineHandle, 'Points');", timeout);

				if (!thread.isSuccessful()) {
					continue;
				}
			} else {// branchChild = true
				thread = RunMatlabCommand.executeCommand(
						_matlab, // TODO: avoid access to a parent or child with
									// handle-ID "-1";
						RunMatlabCommand.Command.eval,
						"lineHandle = get_param(lastLineHandle,'LineParent');"
								+ "lastLineHandle = lineHandle(1);"
								+ "linePoints = get_param(lastLineHandle,'Points');", timeout);

				if (!thread.isSuccessful()) {
					continue;
				}
			}
			thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.eval,
					"lineCell=cell(1,2);" + "lineCell{1,1}='Points';" + "lineCell{1,2}=linePoints;",
					timeout);

			if (!thread.isSuccessful()) {
				continue;
			}

			thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.getVariable,
					"lineCell", timeout);

			if (!thread.isSuccessful()) {
				continue;
			}
			result = (Object[]) thread.getResult();

			thread = RunMatlabCommand.executeCommand(_matlab, RunMatlabCommand.Command.eval,
					"clear linePoints; clear lineHandle; clear lineCell;", timeout); // DON'T
																											// clear
																											// all!
																											// lastLineHandle
																											// is
																											// needed
																											// for
																											// branches

			if (!thread.isSuccessful()) {
				result = null;
				continue;
			}
		}

		// Avoid NullpointerExceptions
		if ((result != null) && (result.length > 0)) {

			if ((result != null) && (result.length > 0) && jmBridgeBehaviour) { // JMBridge
																										// encapsulates
																										// results
																										// in
																										// an
																										// extra
																										// Object
																										// array
				result = (Object[]) result[0];
			}

			if (result[0] instanceof String) {
				return result;
			} else {
				return (Object[]) result[0];
			}
		} else {
			MeMoPlugin.out
			.println("[DEBUG] Unexpected result in MatlabPortEvaluation.collectSignalParameter!");
			return new Object[0];
		}
	}

	/**
	 * Adds the signal Points results from matlab to the registered parameter
	 * container or updates existing ones.
	 *
	 * @param signal
	 *           the current signal to be evaluated
	 * @param parmeterValueMap
	 *           the matlab access result
	 */
	private void addSignalParameter(SignalLine signal, Object[] parameterValueMap) {
		if (parameterValueMap[1] instanceof Object[]) {
			Object[] pointArray = (Object[]) parameterValueMap[1];
			int pointCnt = pointArray.length;
			for (int i = 0; i < pointCnt; i++) {
				double[] tmp = (double[]) pointArray[i];
				signal.getParameter().put("Point" + (i + 1), tmp[0] + ":" + tmp[1]);
			}
		} else {
			MeMoPlugin.out.println(parameterValueMap[1]);
		}
		return;
	}

	/**
	 * Adds the block parameter results from matlab to the registered parameter
	 * container or updates existing ones.
	 *
	 * @param block
	 *           the current block to be evaluated
	 * @param parmeterValueMap
	 *           the matlab access result
	 */
	private void addBlockParameter(Block block, Object[] parameterValueMap) {

		HashMap<String, String> notPortParameters = new HashMap<String, String>();
		HashMap<String, Object> notPortParameterObjects = new HashMap<String, Object>();
		HashMap<String, String> portParameters = new HashMap<String, String>();
		HashMap<String, Object> portParameterObjects = new HashMap<String, Object>();

		int valofst = parameterValueMap.length / 2;

		for (int i = 0; i < valofst; i++) {

			String paraName = (String) parameterValueMap[i];
			String simulinkValue = ReturnFormatter.formatResult(parameterValueMap[i + valofst]);

			if (isEmptyParameterValue(simulinkValue)) {
				simulinkValue = "[]"; // represent empty values a uniform way
			}

			if (paraName.contains("Port")) {
				portParameters.put(paraName, simulinkValue);
				portParameterObjects.put(paraName, parameterValueMap[i + valofst]);
				block.getParameter().put(paraName, simulinkValue);
			} else {
				notPortParameters.put(paraName, simulinkValue);
				notPortParameterObjects.put(paraName, parameterValueMap[i + valofst]);
			}

		}

		/* update block parameters */
		for (String key : notPortParameters.keySet()) {
			block.getParameter().put(key, notPortParameters.get(key));
		}

		/* update port parameters */
		// [in, out, enable, trigger, state, LConn, RConn, action]
		double[] ports = ((double[]) portParameterObjects
				.get(SimulinkParameterNames.BLOCK_PORTS_PARAMETER));
		double numberOfInports = ports[SimulinkPortConstants.IN_PORT_POSITION];
		double numberOfOutports = ports[SimulinkPortConstants.OUT_PORT_POSITION];
		boolean enablePort = (ports[SimulinkPortConstants.ENABLE_PORT_POSITION]) > 0;
		boolean triggerPort = (ports[SimulinkPortConstants.TRIGGER_PORT_POSITION]) > 0;
		boolean statePort = (ports[SimulinkPortConstants.STATE_PORT_POSITION]) > 0;
		double numberOfLConnPorts = ports[SimulinkPortConstants.L_CONN_PORT_POSITION];
		double numberOfRConnPorts = ports[SimulinkPortConstants.R_CONN_PORT_POSITION];
		boolean actionPort = (ports[SimulinkPortConstants.IFACTION_PORT_POSITION]) > 0;

		// in Ports
		for (int i = 1; i <= numberOfInports; i++) {
			Port port = block.getInPortsMap().get(i);
			if (port == null) {
				MeMoPlugin.out.println("[Warning] " + block.getFullQualifiedName(false)
						+ ": In port not found, create new Port");
				port = new Port(_model.nextID(), SimulinkPortConstants.IN_PORT_PREFIX + i, i);
				block.getInPortsMap().put(i, port);
			}

			for (String key : portParameterObjects.keySet()) {
				updatePortParameter(port, portParameters, portParameterObjects, key, i, 1);
			}
		}

		// out ports
		for (int i = 1; i <= numberOfOutports; i++) {
			Port port = block.getOutPortsMap().get(i);
			if (port == null) {
				MeMoPlugin.out.println("[Warning] " + block.getFullQualifiedName()
						+ ": Out port not found, create new Port");
				port = new Port(_model.nextID(), SimulinkPortConstants.OUT_PORT_PREFIX + i, i);
				block.getOutPortsMap().put(i, port);
			}

			for (String key : portParameterObjects.keySet()) {
				updatePortParameter(port, portParameters, portParameterObjects, key, i, 2);
			}
		}

		// enable Ports
		if (enablePort) {
			Port port = block.getEnablePort();
			if (port == null) {
				MeMoPlugin.out.println("[Warning] " + block.getFullQualifiedName()
						+ ": Enable port not found, create new Port");
				port = new Port(_model.nextID(), SimulinkPortConstants.ENABLE_PORT, 0);
				block.setEnablePort(port);
			}
			for (String key : portParameterObjects.keySet()) {
				updatePortParameter(port, portParameters, portParameterObjects, key, 1, 3);
			}
		}

		// trigger Ports
		if (triggerPort) {
			Port port = block.getTriggerPort();
			if (port == null) {
				MeMoPlugin.out.println("[Warning] " + block.getFullQualifiedName()
						+ ": Trigger port not found, create new Port");
				port = new Port(_model.nextID(), SimulinkPortConstants.TRIGGER_PORT, 0);
				block.setTriggerPort(port);
			}
			for (String key : portParameterObjects.keySet()) {
				updatePortParameter(port, portParameters, portParameterObjects, key, 1, 4);
			}
		}

		// state Ports
		if (statePort) {
			Port port = block.getStatePort();
			if (port == null) {
				MeMoPlugin.out.println("[Warning] " + block.getFullQualifiedName()
						+ ": State port not found, create new Port");
				port = new Port(_model.nextID(), SimulinkPortConstants.STATE_PORT, 0);
				block.setStatePort(port);
			}
			for (String key : portParameterObjects.keySet()) {
				updatePortParameter(port, portParameters, portParameterObjects, key, 1, 5);
			}
		}

		// LConn Ports
		for (int i = 1; i <= numberOfLConnPorts; i++) {
			Port port = block.getlConnPortsMap().get(i);
			if (port == null) {
				MeMoPlugin.out.println("[Warning] " + block.getFullQualifiedName()
						+ ": LConn port not found, create new Port");
				port = new Port(_model.nextID(), "LConn" + i, i);
				block.getlConnPortsMap().put(i, port);
			}

			for (String key : portParameterObjects.keySet()) {
				updatePortParameter(port, portParameters, portParameterObjects, key, i, 6);
			}
		}

		// RConn Ports
		for (int i = 1; i <= numberOfRConnPorts; i++) {
			Port port = block.getrConnPortsMap().get(i);
			if (port == null) {
				MeMoPlugin.out.println("[Warning] " + block.getFullQualifiedName()
						+ ": LConn port not found, create new Port");
				port = new Port(_model.nextID(), "RConn" + i, i);
				block.getrConnPortsMap().put(i, port);
			}

			for (String key : portParameterObjects.keySet()) {
				updatePortParameter(port, portParameters, portParameterObjects, key, i, 7);
			}
		}

		// action Ports
		if (actionPort) {
			Port port = block.getIfactionPort();
			if (port == null) {
				MeMoPlugin.out.println("[Warning] " + block.getFullQualifiedName()
						+ ": action port not found, create new Port");
				port = new Port(_model.nextID(), SimulinkPortConstants.IFACTION_PORT, 0);
				block.setIfactionPort(port);
			}
			for (String key : portParameterObjects.keySet()) {
				updatePortParameter(port, portParameters, portParameterObjects, key, 1, 8);
			}
		}

		// if (_compileSuccess) {
		if (SimulinkBlockConstants.BUSSELECTOR_BLOCKTYPE.equals(block.getType())) {
			busanalyzer.addBusSelector(block, notPortParameterObjects);
		} else if (SimulinkBlockConstants.DEMUX_BLOCKTYPE.equals(block.getType())) {
			busanalyzer.addDemux(block, notPortParameterObjects);
		} else if (SimulinkBlockConstants.MUX_BLOCKTYPE.equals(block.getType())) {
			busanalyzer.addMux(block);
		} else if (SimulinkBlockConstants.BUSCREATOR_BLOCKTYPE.equals(block.getType())) {
			busanalyzer.addBusCreator(block);
		} else if (SimulinkBlockConstants.BUSASSIGNMENT_BLOCKTYPE.equals(block.getType())) {
			busanalyzer.addBusSelector(block, notPortParameterObjects);
		}
		// }

		return;
	}

	/**
	 * Updates all parameters of a port.
	 *
	 * @param port
	 *           The port that shall be updated
	 * @param parameters
	 *           a List of String representations of the parameters
	 * @param parameterObjects
	 *           the parameters delivered by MATLAB
	 * @param key
	 *           the name of the parameter that shall be updated
	 * @param portNumber
	 *           the number of the port
	 * @param type
	 *           the position where in the MATLAb port object information can be
	 *           found. The order of the port types in an arrays is
	 *           "[in, out, enable, trigger, other, LConn, RConn, ifAction]"
	 *           COUNTING STARTS AT 1!
	 */
	private void updatePortParameter(Port port, HashMap<String, String> parameters,
			HashMap<String, Object> parameterObjects, String key, int portNumber, int type) {

		Object value = parameterObjects.get(key);

		// Handle the different data structures
		if (value instanceof String) {
			port.getParameter().put(key, (String) value);
		} else if (key.equals(SimulinkPortConstants.PORT_CONNECTIVITY_PARAMETER)) {
			int offset = (portNumber - 1) * 6;

			Object[] object = (Object[]) parameterObjects.get(key);
			port.getParameter().put(SimulinkPortConstants.PORT_CONNECTIVITY_PARAMETER,
					(String) object[offset]);

			for (int i = 1; i < 6; i++) {
				double[] doubleValues = (double[]) object[i + offset];
				if (doubleValues == null) {
					// ignore known null pointers, throw exception for unknown
					if (((i == 4) || (i == 5))
							&& ((type == (SimulinkPortConstants.L_CONN_PORT_POSITION + 1)) || (type == (SimulinkPortConstants.R_CONN_PORT_POSITION + 1)))) {
						continue;
					} else {
						MeMoPlugin.out.println("[DEBUG] PortConnectivity" + i + " not found for Port "
								+ port.getName());
					}
				}
				for (int j = 0; j < doubleValues.length; j++) {
					port.getParameter().put(
							SimulinkPortConstants.PORT_CONNECTIVITY_PARAMETER + i + "." + j,
							Double.toString(doubleValues[j]));
				}
			}

		} else if (key.equals(SimulinkPortConstants.PORT_HANDLES_PARAMETER)) {
			Object[] object = (Object[]) parameterObjects.get(key);
			double[] portParameters = (double[]) object[type - 1];
			String stringValue = "";
			if (portParameters.length > (portNumber - 1)) {
				stringValue = Double.toString(portParameters[portNumber - 1]);
			}
			port.getParameter().put(SimulinkPortConstants.PORT_HANDLES_PARAMETER, stringValue);

		} else if (key.equals(SimulinkPortConstants.COMPILED_PORT_BUS_MODE_PARAMETER)) {
			Object[] object = (Object[]) parameterObjects.get(key);
			double[] portParameters = (double[]) object[type - 1];
			String stringValue = "";
			if (portParameters.length > (portNumber - 1)) {
				stringValue = Double.toString(portParameters[portNumber - 1]);
			}
			port.getParameter().put(SimulinkPortConstants.COMPILED_PORT_BUS_MODE_PARAMETER,
					stringValue);

		} else if (key.equals(SimulinkPortConstants.COMPILED_PORT_COMPLEX_SIGNALS_PARAMETER)) {
			Object[] object = (Object[]) parameterObjects.get(key);
			double[] portParameters = (double[]) object[type - 1];
			String stringValue = "";
			if (portParameters.length > (portNumber - 1)) {
				stringValue = Double.toString(portParameters[portNumber - 1]);
			}
			port.getParameter().put(SimulinkPortConstants.COMPILED_PORT_COMPLEX_SIGNALS_PARAMETER,
					stringValue);

		} else if (key.equals(SimulinkPortConstants.COMPILED_PORT_DATA_TYPES_PARAMETER)) {
			Object[] object = (Object[]) parameterObjects.get(key);
			if ((type == (SimulinkPortConstants.L_CONN_PORT_POSITION + 1))
					|| (type == (SimulinkPortConstants.R_CONN_PORT_POSITION + 1))) {
				double[] portParameters = (double[]) object[type - 1];
				port.getParameter().put(SimulinkPortConstants.COMPILED_PORT_DATA_TYPES_PARAMETER,
						Double.toString(portParameters[portNumber - 1]));
			} else {
				String[] portParameters = (String[]) object[type - 1];
				port.getParameter().put(SimulinkPortConstants.COMPILED_PORT_DATA_TYPES_PARAMETER,
						portParameters[portNumber - 1]);
			}

		} else if (key.equals(SimulinkPortConstants.COMPILED_PORT_DIMENSIONS_PARAMETER)) {
			Object[] object = (Object[]) parameterObjects.get(key);
			double[] portParameters = (double[]) object[type - 1]; // Array that
			// could
			// have
			// three
			// forms, if
			// existing:
			// "[m1 n1 m2 n2... mn nn]"
			// a lis of
			// two
			// dimensional
			// signal
			// with the
			// given
			// sizes m n
			// "[-2 SignalCount Signal1m Signal1n Signal2m Signal2n ... SignalSignalCountm SignalSignalCountn]"
			// e.g. bus.
			// Starting
			// with -2
			// because
			// -1 means
			// a signal
			// size with
			// any
			// dimension
			if (Math.abs(portParameters[0] + 2) < 0.1) {
				port.getParameter().put(SimulinkPortConstants.COMPILED_PORT_DIMENSIONS_PARAMETER,
						ReturnFormatter.formatResult(portParameters));
			} else {
				String stringValue1 = "";
				String stringValue2 = "";
				if (portParameters.length > ((portNumber - 1) * 2)) {
					stringValue1 = Double.toString(portParameters[(portNumber - 1) * 2]);
				}
				if (portParameters.length > (((portNumber - 1) * 2) + 1)) {
					stringValue2 = Double.toString(portParameters[((portNumber - 1) * 2) + 1]);
				}

				port.getParameter().put(SimulinkPortConstants.COMPILED_PORT_DIMENSIONS_PARAMETER,
						"[" + stringValue1 + " " + stringValue2 + "]");
			}

		} else if (key.equals(SimulinkPortConstants.COMPILED_PORT_FRAME_DATA_PARAMETER)) {
			Object[] object = (Object[]) parameterObjects.get(key);
			double[] portParameters = (double[]) object[type - 1];
			String stringValue = "";
			if (portParameters.length > (portNumber - 1)) {
				stringValue = Double.toString(portParameters[portNumber - 1]);
			}
			port.getParameter().put(SimulinkPortConstants.COMPILED_PORT_FRAME_DATA_PARAMETER,
					stringValue);

		} else if (key.equals(SimulinkPortConstants.COMPILED_PORT_WIDTHS_PARAMETER)) {
			Object[] object = (Object[]) parameterObjects.get(key);
			double[] portParameters = (double[]) object[type - 1];
			String stringValue = "";
			if (portParameters.length > (portNumber - 1)) {
				stringValue = Double.toString(portParameters[portNumber - 1]);
			}
			port.getParameter().put(SimulinkPortConstants.COMPILED_PORT_WIDTHS_PARAMETER, stringValue);

			// Store string representation for all other parameters
		} else if (!key.equals(SimulinkParameterNames.BLOCK_PORTS_PARAMETER)) {
			port.getParameter().put(key, parameters.get(key));
		}
	}

	private void replaceVariablesByValue() {
		HashMap<String, String> variable2value;
		HashMap<String, String> root = getVariableToValueMap();
		HashMap<Block, HashMap<String, String>> block2VarMap = new HashMap<Block, HashMap<String, String>>();
		int blockcnt = 0;
		Set<String> keyList;

		MeMoPlugin.out.println("[INFO] Replacing Variables");

		// update model configuration parameters
		for (Configuration c : _model.getConfigurations()) {
			keyList = c.getParameter().keySet();
			for (String key : keyList) {
				String value = c.getParameter().get(key);
				if (root.containsKey(value)) {
					c.getParameter().put(key, root.get(value));
				}
			}
		}

		for (Block currentBlock : _model.getBlocks()) {
			List<Block> blockPath;

			blockcnt++;

			if (block2VarMap.containsKey(currentBlock)) {
				continue; // if block already updated, all blocks above are updated
			}

			variable2value = root;

			blockPath = currentBlock.getPathOfBlocks();

			if ((blockcnt % 50) == 0) {
				MeMoPlugin.out.println("\t processing block " + blockcnt + " of "
						+ _blocks.values().size());
			}

			blockPath = currentBlock.getPathOfBlocks();
			while (!blockPath.isEmpty() && block2VarMap.containsKey(blockPath.get(0))) { // fast
																													// forward
																													// to
																													// first
																													// not
																													// updated
																													// block
				variable2value = block2VarMap.get(blockPath.get(0));
				blockPath.remove(0);
			}

			// to update the current block, all blocks on path needed to be updated
			// before
			for (Block pathBlock : blockPath) {
				String variablesString;
				String[] variables = null;

				// a fresh map for every block, so nothing will be overwritten
				variable2value = new HashMap<String, String>(variable2value);
				block2VarMap.put(pathBlock, variable2value);

				// Filter before processing. Not all Blocks on path need an update.
				if (// pathBlock instanceof ReferenceBlock ||
				pathBlock.getParameter(SimulinkParameterNames.MASK_VARIABLES) != null) {

					variablesString = pathBlock.getParameter(tmpVarParam);

					if (variablesString != null) {
						variables = variablesString.split(";");

						// get variables defined in this mask
						for (String variable : variables) {
							variable2value.put(variable, formatVarValue(pathBlock.getParameter(variable)));
						} // for

					} // fi maskVariables

				} // fi analyze

				// substitute variables in parameters
				keyList = pathBlock.getParameter().keySet();
				for (String key : keyList) {
					String value = pathBlock.getParameter().get(key);
					if (variable2value.containsKey(value)) {
						pathBlock.getParameter().put(key, variable2value.get(value));
					}
				}

				if (pathBlock instanceof ReferenceBlock) {
					// substitute parameters in original block
					Block refBlock = ((ReferenceBlock) pathBlock).getReferencingBlock();
					keyList = refBlock.getParameter().keySet();
					for (String key : keyList) {
						String value = refBlock.getParameter().get(key);
						if (variable2value.containsKey(value)) {
							refBlock.getParameter().put(key, variable2value.get(value));
						}
					}
				}

				// pathBlock.getParameter().remove(tmpVarParam);

			} // for pathBlock

		} // for currentBlock
	}

	private HashMap<String, String> getVariableToValueMap() {
		HashMap<String, String> result = new HashMap<String, String>();

		for (WSVariable var : _model.getWorkspaceVariables()) {
			result.put(var.getName(), formatVarValue(var.getValue()));
		}

		return result;
	}

	private String formatVarValue(String value) {
		if (value == null) {
			return "[]";
		}
		String result = value;

		// if single value -> don't use array representation
		if (value.startsWith("[") && value.endsWith("]")) {
			if (value.split("\\x20").length == 3) { // 2 blanks: [ value ]
				result = value.substring(1, value.length() - 1).trim();
			} else if (value.split("\\x20").length == 2) { // 1 blank: "[ ]",
																			// convert to []
				result = "[]";
			}
		}

		if (result.isEmpty()) {
			result = "[]";
		}

		return result;
	}

	/**
	 * Returns true if the given parameter value is an empty string or an empty
	 * array. An empty array looks like this: "[ ]"
	 *
	 * @param value
	 *           the parameter value to be checked.
	 * @return true if the given parameter value is an empty string or an empty
	 *         array
	 */
	private boolean isEmptyParameterValue(Object value) {
		if (value instanceof Object[]) {
			return ((Object[]) value).length == 0;
		} else if (value instanceof String) {
			String v = (String) value;
			boolean isEmptyString = v.trim().isEmpty();

			String temp = v.replace('[', ' ');
			temp = temp.replace(']', ' ');
			boolean isEmptyArray = temp.trim().isEmpty();

			return isEmptyString || isEmptyArray;
		} else {
			return true;
		}
	}

	/**
	 * @return the matlab connection
	 */
	public MatlabConnector get_matlabConection() {
		return _matlab;
	}

	// private String generateMatlabVarname(String wantedName) {
	// String result = wantedName;
	// int timeout = MeMoPlugin.getDefault().getPreferenceStore()
	// .getInt(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT) * 1000;
	// int maxTries = MeMoPlugin.getDefault().getPreferenceStore()
	// .getInt(MeMoPreferenceConstants.PR_MATLAB_TRIES);
	// int tries = 1;
	//
	// RunMatlabCommand thread = RunMatlabCommand.executeCommand(_matlab,
	// RunMatlabCommand.Command.getVariable,
	// result, timeout);
	//
	//
	// while (!thread.isSuccessful() || thread.getResult() != null) {
	// tries++;
	// if (tries >= maxTries) return wantedName + Math.random(); // no tries
	// left? Hope for the best
	// if (!thread.isSuccessful()) {
	// thread = RunMatlabCommand.executeCommand(_matlab,
	// RunMatlabCommand.Command.getVariable,
	// result, timeout);
	// } else {
	// result = wantedName + tries;
	// thread = RunMatlabCommand.executeCommand(_matlab,
	// RunMatlabCommand.Command.getVariable,
	// result, timeout);
	// }
	// }
	//
	// return result;
	// }

}
