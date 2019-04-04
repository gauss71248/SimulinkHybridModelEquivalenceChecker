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
import java.util.HashMap;
import java.util.HashSet;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.impl.Block;
import de.tu_berlin.pes.memo.model.impl.ConcurrentSignalLineOrigins;
import de.tu_berlin.pes.memo.model.impl.Port;
import de.tu_berlin.pes.memo.model.impl.SignalLine;
import de.tu_berlin.pes.memo.model.util.SimulinkBlockConstants;
import de.tu_berlin.pes.memo.model.util.SimulinkParameterNames;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

/**
 * The BusAnalyzer tries to find for every <code>SignalLine</code> leaving a
 * bus/mux system the sources for the signals on this <code>SignalLine</code>.
 * Therefore the algorithm searches all paths from a source to target signal
 * line, whereby unneeded paths must be sorted out. If a signal is on the bus
 * but never selected, it is not of interest. Therefore the algorithm did a
 * backward analysis. If the paths are found, the predecessors of all
 * <code>SignalLines</code> in the bus/mux system and the outgoing
 * </code>SignalLines</code> are added to this <code>SignalLines</code>. The
 * algorithm works in the following way:
 *
 * <ol>
 * <li><b>Analyze all Bus Selectors iteratively:</b> A bus selector have two
 * parameters of interest: IputSignals containing a tree structure of signals.
 * It describes the bus structure, how the signals on the incoming
 * <code>SignalLine</code> are structured. OutputSignals, a path in dot
 * notation, describes which signals of the InpuSignals structure are selected.
 * <ol>
 * <li><b>Generate <code>SignalTree</code>:</b> Convert the "IputSignals" into a
 * tree structure suitable for the analysis.</li>
 * <li><b>Enrich SignalTree by path information:</b> Every node of the signal
 * tree stores the path of <code>SignalLines</code> which leads to the block in
 * the model where the children of the node are relevant.
 * <li><b>Analyze the OutputSignals Parameters:</b> The OutputSignals specifies
 * the pathway in the <code>SignalTree</code> to the selected signal. The
 * selected signal needn't be at a leaf of the <code>SignalTree</code>. If this
 * happens, all sub signals will be analyzed and returned.</li>
 * </ol>
 * If a mux/demux is a predecessor of an BusSelector, it is transparent for the
 * analysis, because the <code>SignalTree</code> respectively the InputSignals
 * parameter we get from MatLAB go through this block. Thats the reason why to
 * analyze BusSelectors first, there is not much interleaving with the
 * de-/muxes.</li>
 * <li><b>Analyze all unvisited BusCreators:</b> These BusCreators are not
 * followed by an BusSelector. Maybe they are used to create a mux even if
 * MathWorks discourages Bus/Mux mixtures. That is why these BusCreators are
 * considered as a mux.</li>
 * <li><b>Analyze all Demuxes:</b> The Demux assigns the muxed signals to its
 * outports. Where which signal is emitted depends on the number of outports and
 * the number on incoming signals.
 * <ol>
 * <li><b>Backtrack the incoming mux signal:</b> The incoming signal is a mux.
 * The algorithm follows this signal to its origins and has to handle blocks,
 * that interfere with the mux signal, e.g. the Selector block, that can change
 * the order of the muxed signals. We get a bundle of signals.</li>
 * <li><b>Assign the signals to the outputs:</b> The signals we get from the
 * previous step now must be mapped to the right output. If there are e.g 5
 * signals on the mux and 3 outports, the first two musst be assigned to the
 * first output, the second 2 to the second and the fifth signal to the third
 * output.</li>
 * </ol>
 * </li>
 * <li><b>Analyze all unvisited Muxes:</b> These Muxes are not followed by a
 * BusSelector or a Demux. Nevertheless they are part of a mux system they
 * create.</li>
 * </ol>
 *
 * After the algorithm finished, all found paths are used to add to every signal
 * line of every path its predecessors contained in the path.
 *
 * @author Joachim Kuhnert
 *
 */
public class BusMuxAnalyzer {

	// //// INNER CLASSES /////

	/**
	 * Only borders are points of interest: exit points of signals out of the
	 * Bus/Mux-System. At the beginning of the analyze process only SignalLines
	 * with only one signal on it will be considered as out of the bus/mux
	 * system: YES. To all other the MAY value will be assigned. If the analyze
	 * process encounters a SignalLine of an relevant Block while tracking back a
	 * signal, the state will be changed to NO.
	 *
	 * Remark: The algorithm as is currently, don't need the discrimination
	 * between YES and MAY.
	 */
	enum BorderAnalyzeState {
		YES, NO, MAY
	}

	/**
	 * A SignalTree represents the InputSignal parameter. This Parameter is a
	 * tree structure of signalnames. E.g. [signal1 [signal1.1, signal1.2
	 * [signal1.2.1 ...] ...] signal2 [...]... ]. The class implements this tree
	 * and enriches it by pathes of our SignalLines which represents the step
	 * between the current point and where the signal name is created, usually a
	 * BusCreator.
	 *
	 * @author Joachim Kuhnert
	 */
	private class SignalTree {
		/**
		 * The signal name of this tree. You found this tree under this key in the
		 * stringToChildMap of its parent.
		 */
		String value = "";

		/**
		 * Signal name to integer to associate port numbers to the name. The
		 * InputSignal parameter serves the signals in order.
		 */
		HashMap<String, Integer> stringToSignalNumberMap = new HashMap<String, Integer>();
		/**
		 * Signal names to children for easy access to the wanted subtree.
		 */
		HashMap<String, SignalTree> stringToChildMap = new HashMap<String, SignalTree>();
		/**
		 * Children ordered by signalNumber. Important to get signal paths in
		 * order for demux.
		 */
		ArrayList<SignalTree> children = new ArrayList<SignalTree>(); //
		/**
		 * The sequence of SignalLines in the simulink model to get to the point
		 * where the children of the tree become relevant.
		 */
		ArrayList<SignalLine> path;

		/**
		 * Creates an empty tree.
		 */
		SignalTree() {
		}

		/**
		 * Creates a new SignalTree of the given structure.
		 *
		 * @param inputSignalObject
		 *           The tree structure in an array representation.
		 */
		SignalTree(Object inputSignalObject) {
			if (inputSignalObject instanceof String) { // if the object is only one
																		// string it is a leaf
				value = (String) inputSignalObject; // stop recursion an give
																// current tree the string value
			} else { // a subtree of the pattern [signalName subTreeArray]
				Object[] castedInputSignalObject = (Object[]) inputSignalObject;
				Object[] subTreeArray = (Object[]) castedInputSignalObject[1]; // get
																									// the
																									// subTreeArray
				value = (String) castedInputSignalObject[0]; // get & set the signal
																			// name/value
				for (int i = 0; i < subTreeArray.length; i++) {
					SignalTree t = new SignalTree(subTreeArray[i]); // create the
																					// subtree
																					// recursively
					stringToSignalNumberMap.put(t.value, i + 1); // add the signal
																				// name to signal
																				// number relation
					stringToChildMap.put(t.value, t); // add the subtree
					children.add(t); // store the subtree/signal order for demuxing
				}
			}
		}

		@Override
		public String toString() {
			String result = value;
			result += pathToString(path);
			if (!stringToChildMap.isEmpty()) {
				result += "[";
				for (String key : stringToChildMap.keySet()) {
					String prefix = "(" + stringToSignalNumberMap.get(key) + ":"
							+ pathToString(stringToChildMap.get(key).path) + ")";
					result += stringToChildMap.get(key).toStringHelper(prefix);
				}
				result += "]";
			}
			return result;
		}

		/**
		 * Recursive function to generate string representation of the tree
		 * including the children.
		 *
		 * @param prefix
		 *           The string representation of the path and the port number
		 * @return The string resulting string
		 */
		private String toStringHelper(String prefix) {
			String result = value + prefix;
			if (!stringToChildMap.isEmpty()) {
				result += "[";
				for (String key : stringToChildMap.keySet()) {
					String newPrefix = "(" + stringToSignalNumberMap.get(key) + ":"
							+ pathToString(stringToChildMap.get(key).path) + "), ";
					result += stringToChildMap.get(key).toStringHelper(newPrefix);
				}
				result += "]";
			}
			return result;
		}

		/**
		 * Generates a string representation of a list of SignalLines.
		 *
		 * @param path
		 *           The list of SignalLines
		 * @return A String of the form <signaLineID1,...signalLineIDN,> or <?> if
		 *         path is null.
		 */
		private String pathToString(ArrayList<SignalLine> path) {
			String result = "<";

			if (path == null) {
				return "?";
			}

			for (SignalLine line : path) {
				result += line.getId();
				result += ",";
			}
			result += ">";

			return result;
		}

		/**
		 * Tests if a (sub)tree is a leaf.
		 *
		 * @return True, if the subtree has no children, else false.
		 */
		boolean isLeaf() {
			return stringToChildMap.isEmpty();
		}

		/**
		 * Copy the tree structure but without path information.
		 *
		 * @return
		 */
		public SignalTree blancCopy() {
			SignalTree result = new SignalTree();
			result.value = value;
			for (SignalTree subTree : children) {
				SignalTree newSubtree = subTree.blancCopy();
				result.children.add(newSubtree);
				result.stringToChildMap.put(newSubtree.value, newSubtree);
				result.stringToSignalNumberMap.put(newSubtree.value,
						stringToSignalNumberMap.get(newSubtree.value));
			}
			return result;
		}

	} // class SignalTree

	// ------------------------------//

	/**
	 * A wrapper for blocks storing temporary information in the analyzing
	 * process to track signal paths.
	 *
	 * @author Joachim Kuhnert
	 */
	private class AnalyzerNode {
		/**
		 * The wrapped block.
		 */
		Block block;
		/**
		 * The signal tree if wrapped block is a BusSelector.
		 */
		SignalTree signalTree;
		/**
		 * Is the node already analyzed? This could be, because the algorithm
		 * started at this node or visited this node during an other run. If this
		 * flag is true, the algorithm can extract information stored in the node
		 * and must not overwrite the most information.
		 */
		boolean isAlreadyUpdated = false;
		/**
		 * Is the stored information of an outgoing signal line part of the end
		 * result?
		 */
		HashMap<SignalLine, BorderAnalyzeState> outLineIsSystemBorder = new HashMap<SignalLine, BorderAnalyzeState>();
		/**
		 * Intermediate result of the analyzing process. The path from the single
		 * signal joining the bus/mux system to the outgoing SignalLine of this
		 * Block.
		 */
		HashMap<SignalLine, ArrayList<SignalLinePath>> signalPathList = new HashMap<SignalLine, ArrayList<SignalLinePath>>();

		ArrayList<SignalLinePath> lostSignalPaths = new ArrayList<SignalLinePath>();

		/**
		 * Creates a new AnalyzerNode with the block b wrapped.
		 *
		 * @param b
		 *           The Block to wrap.
		 */
		AnalyzerNode(Block b) {
			block = b;

			// for (Port port : block.getOutPorts()) {
			// outPortToLines.put(port, new ArrayList<ArrayList<SignalLine>>());
			// }

			for (SignalLine sl : block.getOutSignals()) { // initialize hash maps
				signalPathList.put(sl, new ArrayList<SignalLinePath>());
				outLineIsSystemBorder.put(sl, checkBorder(sl));
			}
		}

		/**
		 * Helps to initializes the border states. A SignalLine with only one
		 * signal on it, therefore the connected ports have a port width of 1,
		 * will be considered as out of the bus/mux system. All other signal lines
		 * will be initialized with MAY.
		 *
		 * @param sl
		 *           The signal line to check
		 * @return <code>yes</code> if the port width of the ports is 1,
		 *         <code>may</code> otherwise.
		 */
		private BorderAnalyzeState checkBorder(SignalLine sl) {
			BorderAnalyzeState result = BorderAnalyzeState.MAY;
			if (getPortWidth(sl.getSrcPort()) == 1) {
				result = BorderAnalyzeState.YES;
			}
			return result;
		}

		/**
		 * Returns if an AnalyzerNode has unclear border, therefore further
		 * analysis is necessary.
		 *
		 * Remark: The algorithm as is considers a MAY as an YES an don't do more
		 * analysis.
		 *
		 * @return
		 */
		public boolean containMays() {
			for (BorderAnalyzeState state : outLineIsSystemBorder.values()) {
				if (state.equals(BorderAnalyzeState.MAY)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() { // use the wrapped block id as hash!
			// use id as hash!
			return block.getId();
		}

		@Override
		public String toString() {
			// String result = "BlockName: \""
			// + MeMoMatlabManager.createBlockURLString(block) + "\"\n";
			String result = "BlockName: " + block.getFullQualifiedName() + "\n";
			result += "    Borders: " + bordersToString() + "\n";
			result += "    SignalTree: " + (signalTree == null ? "null" : signalTree) + "\n";
			result += "    SignalPathes:\n" + pathesToString();
			result += "    Lost Pathes:\n" + lostPathsToString();
			return result;
		}

		/**
		 * Lists the outgoing signal lines and it BorderStates. ToString part for
		 * the <code>outLineIsSystemBorder</code>.
		 *
		 * @return A String of the form
		 *         "SignalLine1=YES/NO/MAY,SignalLine2=YES/NO/MAY..."
		 */
		private String bordersToString() {
			String result = "";
			for (SignalLine key : outLineIsSystemBorder.keySet()) {
				result += key.getId() + "=" + outLineIsSystemBorder.get(key) + ", ";
			}
			return result;
		}

		/**
		 * Represents the stored paths, one path at one line for every signal on
		 * every signal line. ToString part for <code>signalPathList</code>.
		 *
		 * @return A String of the form
		 * 
		 *         <pre>
		 * outSignalLine1:
		 *         signalLinePath1.1, SignalLinePath1.2 ...
		 *         signalLinePath2.1, ...
		 * outSignalLine2:
		 *         ...
		 * </pre>
		 */
		private String pathesToString() {
			String result = "";

			for (SignalLine key : signalPathList.keySet()) {
				result += "        " + key.getId() + ":\n";
				for (SignalLinePath path : signalPathList.get(key)) {
					result += "            ";
					// for(SignalLine line : path.path) {
					// result += line.getId() + ",";
					// }
					result += path.toString();
					result += "\n";
				}
			}

			return result;
		}

		/**
		 * Lists all paths which are not further used.
		 *
		 * @return A String with all paths in its own line
		 */
		private String lostPathsToString() {
			String result = "";
			for (SignalLinePath path : lostSignalPaths) {
				result += "            ";
				result += path.toString();
				result += "\n";
			}
			return result;
		}

	} // class

	/**
	 * A class to represent the path of a signal and the signal width. Its for
	 * clarity and the signal width is important for demuxing of signals. At some
	 * blocks, e.g. Switch blocks there exist multiple possible origins for a
	 * specified signal after the block, so there are concurrent paths, but at a
	 * specific point in time only one path is valid.
	 *
	 * @author Joachim Kuhnert
	 */
	class SignalLinePath {
		int signalWidth; // the size of the signal on the path
		// if a switch is on the path, the path branches in two or more possible
		// paths
		ArrayList<ArrayList<SignalLine>> concurrentPaths = new ArrayList<ArrayList<SignalLine>>();

		/**
		 * Creates a simple SignalPath with just one signalLine on it.
		 *
		 * @param signalWidth
		 *           The size of the signal on this line
		 * @param line
		 *           the one and only signal line
		 */
		SignalLinePath(int signalWidth, SignalLine line) {
			ArrayList<SignalLine> tmp = new ArrayList<SignalLine>();
			tmp.add(line);
			concurrentPaths.add(tmp);
			this.signalWidth = signalWidth;
		}

		/**
		 * Creats a new signal path.
		 *
		 * @param signalWidth
		 *           the signal size.
		 * @param paths
		 *           the concurrent paths
		 */
		SignalLinePath(int signalWidth, ArrayList<ArrayList<SignalLine>> paths) {
			for (ArrayList<SignalLine> path : paths) {
				concurrentPaths.add(new ArrayList<SignalLine>(path));
			}
			this.signalWidth = signalWidth;
		}

		/**
		 * Appends the trail at the end of all concurrent paths. Does not change
		 * the original path object.
		 *
		 * @param newTrail
		 *           The new sub paths at the end
		 * @return the new created paths object
		 */
		SignalLinePath setTrail(ArrayList<SignalLine> newTrail) {
			SignalLinePath result = new SignalLinePath(signalWidth, concurrentPaths);
			for (ArrayList<SignalLine> path : result.concurrentPaths) {
				path.addAll(newTrail);
			}
			return result;
		}

		/**
		 * Appends a signal line as new end of all concurrent paths. Does not
		 * change the original path object.
		 *
		 * @param newTrail
		 *           The signal line to append
		 * @return The new created path object
		 */
		SignalLinePath setTrail(SignalLine newTrail) {
			SignalLinePath result = new SignalLinePath(signalWidth, concurrentPaths);
			for (ArrayList<SignalLine> path : result.concurrentPaths) {
				path.add(newTrail);
			}
			return result;
		}

		/**
		 * Checks if the given path is a subpath of any of the concurrent paths of
		 * this path object.
		 *
		 * @param path
		 * @return
		 */
		public boolean coverdBy(SignalLinePath path) {
			ArrayList<ArrayList<SignalLine>> longer = new ArrayList<ArrayList<SignalLine>>();
			ArrayList<ArrayList<SignalLine>> shorter = new ArrayList<ArrayList<SignalLine>>();
			longer.addAll(path.concurrentPaths);
			shorter.addAll(concurrentPaths);

			for (int i = longer.size() - 1; i >= 0; i--) {
				ArrayList<SignalLine> longPath = longer.get(i);
				for (int j = shorter.size() - 1; j >= 0; j--) {
					ArrayList<SignalLine> shortPath = shorter.get(j);
					if (shortPath.size() > longPath.size()) {
						continue;
					}

					boolean covered = true;
					for (int k = shortPath.size() - 1; k >= 0; k--) {
						if (!shortPath.get(k).equals(longPath.get(k))) {
							covered = false;
							break;
						}
					}

					if (covered) {
						shorter.remove(shortPath);
					}
				}
			}

			return shorter.isEmpty();
		}

		@Override
		public String toString() {
			String result = "";

			for (ArrayList<SignalLine> path : concurrentPaths) {
				for (SignalLine line : path) {
					result += line.getId() + ",";
				}
				if (path.size() > 0) {
					result = result.substring(0, result.length() - 1);
				}
				result += " || ";
			}
			if (concurrentPaths.size() > 0) {
				result = result.substring(0, result.length() - 4);
			}

			return result;
		}

	} // class

	// ///////////// PARAMETERS ////////////////

	/**
	 * All parameter objects of the bus selector blocks. Shall be filled by the
	 * MatlabPortEvaluation. Not mixed with demuxes so the analysis can simply
	 * iterate over all busSelectors first.
	 */
	private HashMap<AnalyzerNode, HashMap<String, Object>> busSelectorWorklist = new HashMap<AnalyzerNode, HashMap<String, Object>>();
	/**
	 * All parameter objects of the demux selector blocks. Shall be filled by the
	 * MatlabPortEvaluation. Not mixed with bus selectors, so the algorithm can
	 * simply iterate over the demuxes after the busSelectors.
	 */
	private HashMap<AnalyzerNode, HashMap<String, Object>> demuxWorklist = new HashMap<AnalyzerNode, HashMap<String, Object>>();
	/**
	 * Muxes are entry points of bus/mux systems. If a mux is not followed by a
	 * bus selector or demux, it will not automatically visited by the analysis
	 * an must be analyzed explicit.
	 */
	private HashSet<AnalyzerNode> muxes = new HashSet<AnalyzerNode>();
	/**
	 * Buscreators are entry points of bus/mux systems. If a bus creator is not
	 * followed by a bus selector or demux, it will not automatically visited by
	 * the analysis an must be analyzed explicit.
	 */
	private HashSet<AnalyzerNode> busCreators = new HashSet<AnalyzerNode>();
	/**
	 * A mapping from all relevant blocks to their AnalyzerNodes. Important to
	 * get the AnalyzerNode if the algorithm encounters a relevant block. It
	 * always encounters Blocks and not AnalyzerNodes because it follows the
	 * SignalLines.
	 */
	private HashMap<Block, AnalyzerNode> block2node = new HashMap<Block, AnalyzerNode>();

	// ///////////// CONSTRUCTOR ///////////////

	public BusMuxAnalyzer() {
	}

	// /////////////// METHODS /////////////////

	/**
	 * This method starts the analyzing process. Therefore blocks must be added
	 * to analyze by <code>addBusSelector</code>, <code>addDemux</code>,
	 * <code>addMux</code> and <code>addBusCreator</code>. All unadded blocks are
	 * maybe not analyzed at the and of the process.
	 */
	public void analyze() {
		try {

			if (!(busSelectorWorklist.isEmpty() && busCreators.isEmpty())) {
				MeMoPlugin.out.println("[INFO] Starting Analysis of Bus-System");
			}

			// Analyze busSelectors first. Demux analysis will use this
			// information.
			for (AnalyzerNode node : busSelectorWorklist.keySet()) {
				if (node.block.getType().equals(SimulinkBlockConstants.BUSSELECTOR_BLOCKTYPE)) {
					updateBusSelectorSignals(node);
				} else {
					updateBusAssignementSignals(node);
				}
			}

			// are there busCreators not visited yet? They will be analyzed now.
			if (!busCreators.isEmpty()) {
				MeMoPlugin.out
						.println("[INFO] Updating all unvisited BusCreators treating them as Mux");
			}
			for (AnalyzerNode node : busCreators) {
				updateMuxSignals(node); // treat all bus creators without a selector
												// as muxes.
			}

		} catch (Exception e) {
			MeMoPlugin.out.println("[ERROR] Analyzing of Bus System Failed!");
			MeMoPlugin.logException(e.toString(), e);
		}

		try {

			if (!demuxWorklist.isEmpty()) {
				MeMoPlugin.out.println("[INFO] Starting Analysis of Mux-System");
			}

			// Analyze all demuxes
			for (AnalyzerNode block : demuxWorklist.keySet()) {
				updateDemuxSignals(block);
			}

			// analyze all muxes not visited by the analyze process yet
			for (AnalyzerNode node : muxes) {
				updateMuxSignals(node); // update all unanalyzed muxes
			}

		} catch (Exception e) {
			MeMoPlugin.out.println("[ERROR] Analyzing of Mux System Failed!");
			MeMoPlugin.logException(e.toString(), e);
		}

		// Evaluate the result
		if (MeMoPlugin.getDefault() != null && MeMoPlugin.getDefault().getPreferenceStore()
				.getBoolean(MeMoPreferenceConstants.DEBUG_MODE)) {
			MeMoPlugin.out.println("[DEBUG] Result of the BusMuxAnalysis: \n");
		}
		for (AnalyzerNode node : block2node.values()) {
			if (MeMoPlugin.getDefault()!= null && MeMoPlugin.getDefault().getPreferenceStore()
					.getBoolean(MeMoPreferenceConstants.DEBUG_MODE)) {
				MeMoPlugin.out.println(node); // print the results of the analysis
			}

			for (SignalLine signalLine : node.outLineIsSystemBorder.keySet()) {
				// add to every signalLine its predecessor in the bus system.
				if (node.outLineIsSystemBorder.get(signalLine) != BorderAnalyzeState.NO) {
					for (SignalLinePath path : node.signalPathList.get(signalLine)) {
						ConcurrentSignalLineOrigins orderedOrigins = new ConcurrentSignalLineOrigins();
						for (ArrayList<SignalLine> singlePath : path.concurrentPaths) {
							SignalLine origin = singlePath.get(0);
							for (int i = singlePath.size() - 1; i >= 0; i--) {
								SignalLine actualSignalLine = singlePath.get(i);
								actualSignalLine.getSignalDestinations().add(signalLine);
								actualSignalLine.getSignalOrigins().add(origin);
							}
							orderedOrigins.concurrentOrigins.add(singlePath.get(0));
						}
						signalLine.getOrderedSignalOrigins().add(orderedOrigins);
					}
				}
			}

			// process lost signal paths
			// XXX: do something with the ordered Signal Origins?
			for (SignalLinePath path : node.lostSignalPaths) {
				ConcurrentSignalLineOrigins orderedOrigins = new ConcurrentSignalLineOrigins();
				for (ArrayList<SignalLine> singlePath : path.concurrentPaths) {
					SignalLine origin = singlePath.get(0);
					for (int i = singlePath.size() - 1; i >= 0; i--) {
						SignalLine actualSignalLine = singlePath.get(i);
						actualSignalLine.getSignalDestinations().add(
								singlePath.get(singlePath.size() - 1));
						actualSignalLine.getSignalOrigins().add(origin);
					}
					orderedOrigins.concurrentOrigins.add(singlePath.get(0));
				}
				// signalLine.getOrderedSignalOrigins().add(orderedOrigins);
			}

		}

	}

	/**
	 * Analyze a BusSelector node and allocates to every outgoing SignalLine the
	 * paths from the source SignalLine to it.
	 *
	 * @param node
	 *           The BusSelector to analyze.
	 */
	private void updateBusSelectorSignals(AnalyzerNode node) {
		// generate SignalTrees for further processing
		if (node.signalTree == null) { // don't overwrite existing signal trees
													// and already existing information
		// if(!node.isAlreadyUpdated) { // correct alternative for if condition
		// above?

			MeMoPlugin.out.println("        Analyzing BusSelector "
					+ node.block.getFullQualifiedName(false));

			SignalTree root = new SignalTree(); // root has no signal name value ->
															// create root tree manually
			HashMap<String, Object> parameterObjects = busSelectorWorklist.get(node);
			Object[] inputSignalObjects = (Object[]) parameterObjects
					.get(SimulinkParameterNames.BLOCK_INPUTSIGNALS_PARAMETER);

			for (int i = 0; i < inputSignalObjects.length; i++) {
				SignalTree tmp = new SignalTree(inputSignalObjects[i]);
				// tmp.signalNumber = i+1;
				// add the new tree to the root
				root.stringToChildMap.put(tmp.value, tmp);
				root.stringToSignalNumberMap.put(tmp.value, i + 1);
				root.children.add(tmp);
			}

			node.signalTree = root; // add the created signal tree to the analyzer
											// node

			fillTreePaths(root, getInSignalLineByInPortNumber(node.block, 1)); // fill
																										// the
																										// path
																										// properties
																										// of
																										// the
																										// signal
																										// tree

			analyzeBusSelectorPaths(node); // collect the path parts along the
														// signal tree to get the full path of
														// a signal

			node.isAlreadyUpdated = true;
		}

	}

	/**
	 * Analyze a BusSelector node and allocates to every outgoing SignalLine the
	 * paths from the source SignalLine to it.
	 *
	 * @param node
	 *           The BusSelector to analyze.
	 */
	private void updateBusAssignementSignals(AnalyzerNode node) {

		// generate SignalTrees for further processing
		if (node.signalTree == null) { // don't overwrite existing signal trees
													// and already existing information
		// if(!node.isAlreadyUpdated) { // correct alternative for if condition
		// above?

			MeMoPlugin.out.println("        Analyzing BusAssignement "
					+ node.block.getFullQualifiedName(false));

			SignalTree root = new SignalTree(); // root has no signal name value ->
															// create root tree manually
			HashMap<String, Object> parameterObjects = busSelectorWorklist.get(node);
			Object[] inputSignalObjects = (Object[]) parameterObjects
					.get(SimulinkParameterNames.BLOCK_INPUTSIGNALS_PARAMETER);

			for (int i = 0; i < inputSignalObjects.length; i++) {
				SignalTree tmp = new SignalTree(inputSignalObjects[i]);
				// add the new tree to the root
				root.stringToChildMap.put(tmp.value, tmp);
				root.stringToSignalNumberMap.put(tmp.value, i + 1);
				root.children.add(tmp);
			}

			node.signalTree = root; // add the created signal tree to the analyzer
											// node

			fillTreePaths(root, getInSignalLineByInPortNumber(node.block, 1)); // fill
																										// the
																										// path
																										// properties
																										// of
																										// the
																										// signal
																										// tree

			analyzeAssignmentPaths(node);

			node.isAlreadyUpdated = true;
		}

	}

	/**
	 * Fills the path properties of the signal tree and its subtrees with
	 * information. Every path belongs to one step down the tree.
	 *
	 * @param tree
	 *           The signal tree that has to be enriched with path information.
	 * @param currentLine
	 *           The first line to which the signal tree belongs.
	 */
	private void fillTreePaths(SignalTree tree, SignalLine currentLine) {
		Block previousBlock = currentLine.getSrcBlock();
		ArrayList<SignalLine> precessors = new ArrayList<SignalLine>(); // The
																								// predecessors
																								// are
																								// the
																								// SignalLines
																								// between
																								// to
																								// points

		precessors.add(currentLine);

		while (busPassThroughBlock(previousBlock) && !tree.isLeaf()) { // pass
																							// through
																							// nonrelevant
																							// blocks
			SignalLine tmp = previousBlock.getInSignals().toArray(new SignalLine[0])[0]; // a
																													// port
																													// only
																													// has
																													// one
																													// incoming
																													// signal
																													// line.
																													// Get
																													// this!
			precessors.add(0, tmp); // add the predecessor at the front
			previousBlock = tmp.getSrcBlock();
			currentLine = tmp;
		}

		tree.path = precessors;

		if (SimulinkBlockConstants.BUSSELECTOR_BLOCKTYPE.equals(previousBlock.getType())) { // new
																														// BusSelectors,
																														// new
																														// signal
																														// tree
																														// is
																														// actual.
			AnalyzerNode currentNode;
			currentNode = block2node.get(previousBlock);

			updateBusSelectorSignals(currentNode); // start analysis of the new bus
																// selector

			currentNode.outLineIsSystemBorder.put(currentLine, BorderAnalyzeState.NO); // the
																												// signal
																												// line
																												// on
																												// which
																												// we
																												// arrive
																												// at
																												// the
																												// bus
																												// selector
																												// is
																												// part
																												// of
																												// the
																												// bus
																												// system
		} else if (SimulinkBlockConstants.BUSASSIGNMENT_BLOCKTYPE.equals(previousBlock.getType())) {
			AnalyzerNode currentNode;
			currentNode = block2node.get(previousBlock);

			updateBusAssignementSignals(currentNode); // start analysis of the new
																	// bus selector

			currentNode.outLineIsSystemBorder.put(currentLine, BorderAnalyzeState.NO); // the
																												// signal
																												// line
																												// on
																												// which
																												// we
																												// arrive
																												// at
																												// the
																												// bus
																												// selector
																												// is
																												// part
																												// of
																												// the
																												// bus
																												// system
		} else if ((SimulinkBlockConstants.SWITCH_BLOCK_TYPE.equals(previousBlock.getType()) || SimulinkBlockConstants.MULTIPORT_SWITCH_BLOCK_TYPE
				.equals(previousBlock.getType()))
						|| SimulinkBlockConstants.MERGE_BLOCKTYPE.equals(previousBlock.getType())) {
			// NOP, stop filling here. If we reach a switch, we have multiple in
			// lines, which we must analyze
		} else {
			if (!tree.isLeaf()) { // commonly we are at a bus creator. Now we must
											// analyze all children of the signal tree
				for (String signalName : tree.stringToChildMap.keySet()) {
					SignalTree subTree = tree.stringToChildMap.get(signalName);
					SignalLine inLine = getInSignalLineByInPortNumber(previousBlock,
							tree.stringToSignalNumberMap.get(signalName)); // get the
																							// right
																							// incoming
																							// line of
																							// the new
																							// Block

					fillTreePaths(subTree, inLine); // call the current method to get
																// the path property of the
																// child filled
				}
			}

		} // if Busselector

	}

	/**
	 * Once the trees are build and their path properties are set, this path
	 * parts must be joined to get all paths that are selected by a BusSelector.
	 *
	 * @param node
	 *           The BusSelector node, already enriched with information by the
	 *           method <code>fillTreePaths</code>
	 */
	private void analyzeBusSelectorPaths(AnalyzerNode node) {
		int signalNumber = 0; // 0 is not a valid port, so Exceptions are thrown
										// if the algorithm not functions correct and the
										// error will be recognized.
		String allOutSignalObjects = (String) busSelectorWorklist.get(node).get(
				SimulinkParameterNames.BLOCK_OUTPUTSIGNALS_PARAMETER); // Looks like
																							// signal1.signalX...,signal1.signalY...,signalZ.
		String outSignalObject; // Holds one signal path, therefore the
										// allOutSignalObjects will be split at ",". Looks
										// like signalM.signalN.signalO...
		String[] outSignalSplit; // Partitions the outSignalObject into the
											// individual path parts. Split at ".", looks
											// like [signalM, signalN, signalO...].
		ArrayList<String> signalPartsList; // The outSignalSplit converted in an
														// ArrayList for more dynamic, because
														// the processed path parts will be
														// removed.
		ArrayList<SignalLinePath> pathes; // All paths found, the result of the
														// analysis.
		ArrayList<SignalLine> infix; // The tail of the current path, therefore
												// the path fraction found by the analysis
												// by now.

		if (!"on".equals(node.block.getParameter("OutputAsBus"))) { // Output as
																						// Bus means,
																						// there is
																						// only one
																						// out port
																						// for all
																						// Signals
																						// unlike one
																						// port for
																						// every
																						// selected
																						// signal
			for (SignalLine outSignalLine : node.block.getOutSignals()) {
				signalNumber = 0; // 0 is never correct.
				signalPartsList = new ArrayList<String>();
				infix = new ArrayList<SignalLine>();

				// Get the port number, where the outgoing SignalLine is connected
				// to.
				for (int tmpPortNr : node.block.getOutPortsMap().keySet()) {
					if (outSignalLine.getSrcPort().equals(node.block.getOutPortsMap().get(tmpPortNr))) {
						signalNumber = tmpPortNr;
						break;
					}
				}

				outSignalObject = allOutSignalObjects.split(",")[signalNumber - 1];
				outSignalSplit = outSignalObject.split("\\."); // Get the path parts
																				// of the
																				// signalObject.

				for (String element : outSignalSplit) { // Convert the Array to a
																		// List.
					signalPartsList.add(element);
				}

				// A path shall start at the signal line joining the bus and end at
				// the signal line leaving. So add the leaving signal line.
				infix.add(outSignalLine);

				pathes = getPathsFromTree(node.signalTree, signalPartsList, infix); // Get
																											// the
																											// whole
																											// paths.

				node.signalPathList.put(outSignalLine, pathes); // Add the found
																				// paths to the
																				// signalPathList
																				// ordered by the
																				// outgoing line.
			}

		} else { // Output as bus is enabled, we only have one out port.
			signalPartsList = new ArrayList<String>();
			String[] allOutSignalObjectsSplit = allOutSignalObjects.split(",");
			pathes = new ArrayList<SignalLinePath>();

			for (signalNumber = 1; signalNumber <= allOutSignalObjectsSplit.length; signalNumber++) { // Collect
																																	// the
																																	// paths
																																	// for
																																	// every
																																	// signalObject
																																	// .

				outSignalObject = allOutSignalObjectsSplit[signalNumber - 1];
				outSignalSplit = outSignalObject.split("\\."); // Get the path parts
																				// of the
																				// signalObject.

				for (String element : outSignalSplit) { // Convert the Array to a
																		// List.
					signalPartsList.add(element);
				}

				pathes.addAll(getPathsFromTree(node.signalTree, signalPartsList,
						new ArrayList<SignalLine>())); // Get the whole paths.
				// Add the out signal line infix later.
			}

			for (SignalLine outSignalLine : node.block.getOutSignals()) { // Add to
																								// every
																								// found
																								// path
																								// the
																								// outgoing
																								// signal
																								// lines
																								// as
																								// infix
				// and add them to the signalPathList ordered by the outgoing line.
				ArrayList<SignalLinePath> tmpPathList = new ArrayList<SignalLinePath>();
				for (SignalLinePath path : pathes) {
					tmpPathList.add(path.setTrail(outSignalLine));
				}
				node.signalPathList.put(outSignalLine, tmpPathList);
			}

		} // if else

		// get the lost paths
		// XXX: Double work for already calculated (and not lost) paths

		ArrayList<SignalLinePath> allPaths = getAllPathsFromTree2(node.signalTree,
				new ArrayList<SignalLine>());
		HashSet<SignalLinePath> usedPaths = new HashSet<SignalLinePath>();
		for (ArrayList<SignalLinePath> found : node.signalPathList.values()) {
			usedPaths.addAll(found);
		}

		// filter the used paths from allPaths
		for (SignalLinePath usedPath : usedPaths) {
			for (int i = allPaths.size() - 1; i >= 0; i--) {
				if (allPaths.get(i).coverdBy(usedPath)) {
					allPaths.remove(allPaths.get(i));
				}
			}
		}

		node.lostSignalPaths.addAll(allPaths);

	}

	/**
	 * Once the trees are build and their path properties are set, this path
	 * parts must be joined to get all paths that go through or are overwritten
	 * by the BusAssignment.
	 *
	 * @param node
	 *           The BusAssignmnet node, already enriched with information by the
	 *           method <code>fillTreePaths</code>
	 */
	private void analyzeAssignmentPaths(AnalyzerNode node) {
		ArrayList<SignalLinePath> pathes = new ArrayList<SignalLinePath>(); // All
																									// paths
																									// found,
																									// the
																									// result
																									// of
																									// the
																									// analysis.

		pathes.addAll(getPathsFromBusAssignement(node.signalTree, node, new ArrayList<String>(),
				new ArrayList<SignalLine>()));

		for (SignalLine outSignalLine : node.block.getOutSignals()) { // Add to
																							// every
																							// found
																							// path the
																							// outgoing
																							// signal
																							// lines as
																							// infix
			// and add them to the signalPathList ordered by the outgoing line.
			ArrayList<SignalLinePath> tmpPathList = new ArrayList<SignalLinePath>();
			for (SignalLinePath path : pathes) {
				tmpPathList.add(path.setTrail(outSignalLine));
			}
			node.signalPathList.put(outSignalLine, tmpPathList);
		}

		// get the lost paths
		// XXX: Double work for already calculated (and not lost) paths

		ArrayList<SignalLinePath> allPaths = getAllPathsFromTree2(node.signalTree,
				new ArrayList<SignalLine>());
		HashSet<SignalLinePath> usedPaths = new HashSet<SignalLinePath>();
		for (ArrayList<SignalLinePath> found : node.signalPathList.values()) {
			usedPaths.addAll(found);
		}

		// filter the used paths from allPaths
		for (SignalLinePath usedPath : usedPaths) {
			for (int i = allPaths.size() - 1; i >= 0; i--) {
				if (allPaths.get(i).coverdBy(usedPath)) {
					allPaths.remove(allPaths.get(i));
				}
			}
		}

		node.lostSignalPaths.addAll(allPaths);

	}

	/**
	 * The Signal tree has several subtrees identified by a belonging signal
	 * name.
	 *
	 * The signalPath is a list of signal names, that describes which subtree
	 * shall be selected and which sub-subtree and so on.
	 *
	 * This function follows the direction given by the path parts of the
	 * signalPath in the signal tree and collects the signal line path parts
	 * stored in the tree.
	 *
	 * @param tree
	 *           The information rich SignalTree.
	 * @param signalPath
	 *           The signal (tree) path that describe which signal in the tree
	 *           shall be selected
	 * @param infix
	 *           The result of the analysis to the current point.
	 * @return All found paths selected by the signalPath. Can be more than one
	 *         if a signal is selected that consists of multiple signals itself.
	 */
	private ArrayList<SignalLinePath> getPathsFromTree(SignalTree tree,
			ArrayList<String> signalPath, ArrayList<SignalLine> infix) {
		ArrayList<SignalLinePath> result;
		SignalLine outSignalLine = tree.path.get(0); // The path fraction stored
																	// in the tree doesn't need
																	// more analysis.
		// Get the first signal line (remember, we go backwards), that is where we
		// need to start.

		Block block = outSignalLine.getSrcBlock();

		AnalyzerNode blockNode = block2node.get(block);
		if (blockNode != null) {
			blockNode.outLineIsSystemBorder.put(outSignalLine, BorderAnalyzeState.NO);
		}

		infix.addAll(0, tree.path); // Add the signal fraction to temporally
												// result

		if (SimulinkBlockConstants.BUSSELECTOR_BLOCKTYPE.equals(block.getType())) {

			result = getPathsFromBusSelector(tree, signalPath, infix, outSignalLine); // Get
																												// the
																												// paths
																												// from
																												// the
																												// bus
																												// selector.

		} else if (SimulinkBlockConstants.BUSASSIGNMENT_BLOCKTYPE.equals(block.getType())) {
			AnalyzerNode busAssignement = block2node.get(outSignalLine.getSrcBlock());
			updateBusAssignementSignals(busAssignement);

			result = getPathsFromBusAssignement(tree, busAssignement, signalPath, infix);

		} else if (SimulinkBlockConstants.SWITCH_BLOCK_TYPE.equals(block.getType())
				|| SimulinkBlockConstants.MULTIPORT_SWITCH_BLOCK_TYPE.equals(block.getType())
				|| SimulinkBlockConstants.MERGE_BLOCKTYPE.equals(block.getType())) {

			result = new ArrayList<SignalLinePath>();
			ArrayList<ArrayList<SignalLinePath>> resultList = new ArrayList<ArrayList<SignalLinePath>>();

			Port portToSkip = null; // merge has no control port
			if (SimulinkBlockConstants.SWITCH_BLOCK_TYPE.equals(block.getType())) {
				portToSkip = outSignalLine.getSrcBlock().getInPortsMap().get(2);
			} else if (SimulinkBlockConstants.MULTIPORT_SWITCH_BLOCK_TYPE.equals(block.getType())) {
				portToSkip = outSignalLine.getSrcBlock().getInPortsMap().get(1);
			}

			// analyze the signals on every in port
			for (Port port : outSignalLine.getSrcBlock().getInPorts()) {
				ArrayList<SignalLine> newInfix = new ArrayList<SignalLine>(infix);

				if (port.equals(portToSkip)) {
					continue;
				}
				ArrayList<SignalLinePath> resultListItem;
				ArrayList<String> newSignalPath = new ArrayList<String>(signalPath);
				if (!newSignalPath.isEmpty()) {
					SignalTree newTree = tree.blancCopy();
					fillTreePaths(newTree, outSignalLine.getSrcBlock().getInSignalLineByInPort(port));
					resultListItem = getPathsFromTree(newTree, newSignalPath, newInfix);
				} else { // The signal path is empty, therefore no further selection
							// is specified -> get all possible paths left
					if (tree.isLeaf()) { // if the tree is leaf too, we are finished
						resultListItem = new ArrayList<SignalLinePath>();
						ArrayList<ArrayList<SignalLine>> paths = new ArrayList<ArrayList<SignalLine>>();
						paths.add(newInfix);
						resultListItem.add(new SignalLinePath(getPortWidth(infix.get(0).getDstPort()),
								paths)); // create new signal line path an add it to
											// result
					} else {
						resultListItem = getAllPathsFromTree(tree, newInfix); // Tree
																								// is
																								// not
																								// leaf:
																								// a
																								// signal
																								// of
																								// multiple
																								// signals
																								// were
																								// selected,
																								// disassemble
																								// it!
					}
				}
				resultList.add(resultListItem);
			}

			for (SignalLinePath path : resultList.get(0)) {
				for (int i = 1; i < resultList.size(); i++) {
					path.concurrentPaths.addAll(resultList.get(i).remove(0).concurrentPaths);
				}
				result.add(path);
			}

		} else { // not a bus selector

			if (!signalPath.isEmpty()) {
				SignalTree subTree = tree.stringToChildMap.get(signalPath.remove(0));
				result = getPathsFromTree(subTree, signalPath, infix);
			} else { // The signal path is empty, therefore no further selection is
						// specified -> get all possible paths left
				if (tree.isLeaf()) { // if the tree is leaf too, we are finished
					result = new ArrayList<SignalLinePath>();
					ArrayList<ArrayList<SignalLine>> paths = new ArrayList<ArrayList<SignalLine>>();
					paths.add(infix);
					result.add(new SignalLinePath(getPortWidth(infix.get(0).getDstPort()), paths)); // create
																																// new
																																// signal
																																// line
																																// path
																																// an
																																// add
																																// it
																																// to
																																// result
				} else {
					result = getAllPathsFromTree(tree, infix); // Tree is not leaf: a
																				// signal of
																				// multiple signals
																				// were selected,
																				// disassemble it!
				}
			}

			if (SimulinkBlockConstants.BUSCREATOR_BLOCKTYPE.equals(block.getType())) { // we
																												// reached
																												// an
																												// BusCreator
																												// node.
																												// Generate
																												// information
																												// for
																												// mux
																												// analysis

				AnalyzerNode node = blockNode;
				// AnalyzerNode node = block2node.get(outSignalLine.getSrcBlock());
				// blockNode.outLineIsSystemBorder.put(outSignalLine,
				// BorderAnalyzeState.NO); // the signal line on which we arrive at
				// the bus creator is part of the bus system

				if (!node.isAlreadyUpdated) { // If the node already was visited,
														// don't analyze it twice.

					MeMoPlugin.out.println("        Analyzing BusCreator block "
							+ node.block.getFullQualifiedName(false));

					for (SignalLine signalLine : node.block.getOutSignals()) {
						ArrayList<SignalLine> creatorPrefix = new ArrayList<SignalLine>();
						creatorPrefix.add(signalLine);
						ArrayList<SignalLinePath> creatorResult;
						creatorResult = getAllPathsFromTree(tree, creatorPrefix); // The
																										// current
																										// tree
																										// indicates
																										// which
																										// signals
																										// reach
																										// the
																										// bus
																										// selector.
																										// Get
																										// all
																										// of
																										// them.
						node.signalPathList.put(signalLine, creatorResult); // add the
																								// result
					}
					node.isAlreadyUpdated = true;
				}

			} // bus creator

		} // not a bus selector

		return result;
	}

	/**
	 * The BusSelector selects a subset of the signals that reach this bus
	 * selector. We reached the BusSelector from a following (downstream)
	 * BusSelector that only sees the selected subset. So the seen signal1 may
	 * has a different signal name for the new bus selector. This method handles
	 * this problem and gets the right paths.
	 *
	 * @param previousTree
	 *           The Tree with which we reached the BusSelector. Important
	 *           because it stores information about the signal we have to
	 *           select, because we select a signal out of a selection, so that
	 *           the names could be different.
	 * @param signalPath
	 *           The signal (tree) path that describe which signal in the tree
	 *           shall be selected.
	 * @param infix
	 *           The result of the analysis to the current point.
	 * @param outSignalLine
	 *           The signalLine on which we reached the bus selector so we can
	 *           determine which signal is wanted.
	 * @return All found paths selected by the signalPath. Can be more than one
	 *         if a signal is selected that consists of multiple signals itself.
	 */
	private ArrayList<SignalLinePath> getPathsFromBusSelector(SignalTree previousTree,
			ArrayList<String> signalPath, ArrayList<SignalLine> infix, SignalLine outSignalLine) {
		int signalNumber = 0; // 0 is never correct. Recognize algorithm errors by
										// NullpointerExceptions.
		AnalyzerNode actualNode = block2node.get(outSignalLine.getSrcBlock());
		String allOutSignalObjects = (String) busSelectorWorklist.get(actualNode).get(
				SimulinkParameterNames.BLOCK_OUTPUTSIGNALS_PARAMETER); // Looks like
																							// signal1.signalX...,signal1.signalY...,signalZ.
		String outSignalObject; // Holds one signal path, therefore the
										// allOutSignalObjects will be split at ",". Looks
										// like signalM.signalN.signalO...
		String[] outSignalSplit; // Partitions the outSignalObject into the
											// individual path parts. Split at ".", looks
											// like [signalM, signalN, signalO...].
		ArrayList<String> signalPartsList = new ArrayList<String>(); // The
																							// outSignalSplit
																							// converted
																							// in an
																							// ArrayList
																							// for more
																							// dynamic,
																							// because
																							// the
																							// processed
																							// path
																							// parts
																							// will be
																							// removed.
		ArrayList<SignalLinePath> result; // the collected paths

		if (signalPath.size() > 0) { // the path specifies a sub-signal of the
												// signal selected by this bus selector

			SignalTree newTree = actualNode.signalTree;

			if (!"on".equals(actualNode.block.getParameter("OutputAsBus"))) { // There
																									// is
																									// one
																									// port
																									// for
																									// every
																									// selected
																									// signal.
				// Which port number are we? Necessary to get the right signal
				// object for further processing.
				for (int signalNr : actualNode.block.getOutPortsMap().keySet()) {
					if (outSignalLine.getSrcPort().equals(
							actualNode.block.getOutPortsMap().get(signalNr))) {
						signalNumber = signalNr;
						break;
					}
				}
			} else { // output as bus in enabled
				if (allOutSignalObjects.split("\\,").length == 1) { // the selector
																						// selects
																						// exactly one
																						// signal.
																						// That's
																						// easy!
					signalNumber = 1;
				} else {
					signalNumber = previousTree.stringToSignalNumberMap.get(signalPath.remove(0)); // The
																																// selector
																																// selects
																																// multiple
																																// signals.
																																// Find
																																// out
																																// the
																																// number
																																// of
																																// ours.
					// Remove the first entry of the signal path but not in case
					// above,
					// because the selection is is published under a new signal name,
					// but if this signal has only one sub signal, the following bus
					// selector
					// collapsed it to one signal.
				}
			}

			outSignalObject = allOutSignalObjects.split(",")[signalNumber - 1];

			outSignalSplit = outSignalObject.split("\\."); // get the path parts of
																			// the signalObject

			for (String element : outSignalSplit) { // convert the Array to a List
				signalPartsList.add(element);
			}

			signalPartsList.addAll(signalPath); // add the sub signal selection

			result = getPathsFromTree(newTree, signalPartsList, infix); // get all
																							// the
																							// paths

		} else { // signal path is empty
			if (!"on".equals(actualNode.block.getParameter("OutputAsBus"))) {
				if (previousTree.isLeaf()) {
					// SignalTree newTree =
					// actualNode.signalTree.stringToChildMap.get(previousTree.value);
					// // Get the tree of the new node.
					// infix.addAll(0, actualNode.signalTree.path); // add the newly
					// found path fraction to the old path fraction. It's like 1-D
					// puzzling.
					// result = getPathesFromTree(newTree, signalPath, infix); // get
					// the signal line paths
					updateBusSelectorSignals(actualNode);
					SignalLine outgoing = infix.remove(0);
					actualNode.outLineIsSystemBorder.put(outgoing, BorderAnalyzeState.NO);
					ArrayList<SignalLinePath> paths = actualNode.signalPathList.get(outgoing);
					result = new ArrayList<SignalLinePath>();
					for (SignalLinePath path : paths) {
						result.add(path.setTrail(infix));
					}
				} else {
					// Tree not empty but path is. Possible e.g. in the calls from
					// getAllPathesFromTree
					SignalTree newTree = actualNode.signalTree; // Get the tree of
																				// the new node.
					result = getPathsFromTree(newTree, signalPath, infix); // get the
																								// signal
																								// line
																								// paths
				}
			} else { // output as bus in enabled and path is empty: Get all paths!
				result = new ArrayList<SignalLinePath>();
				String[] outSignalObjectSplit = allOutSignalObjects.split(",");
				for (signalNumber = 1; signalNumber <= outSignalObjectSplit.length; signalNumber++) { // guarantee
																																	// order
					ArrayList<SignalLine> newInfix = new ArrayList<SignalLine>();
					newInfix.addAll(infix);
					outSignalObject = outSignalObjectSplit[signalNumber - 1];
					outSignalSplit = outSignalObject.split("\\.");
					for (String element : outSignalSplit) { // convert the Array to a
																			// List
						signalPartsList.add(element);
					}
					SignalTree newTree = actualNode.signalTree;
					result.addAll(getPathsFromTree(newTree, signalPartsList, newInfix));
				}
			}
		}

		return result;

	}

	/**
	 * The BusAssignment overwrites a set of signals of a Bus on Port 1 with the
	 * specific signals on the other ports. The overwritten signals must be leafs
	 * of the tree structure in the bus. The syntax to specify which signals are
	 * overwritten is similar to the bus selector syntax. The first entry
	 * overwrites the specified signal with the value on port 2 the n-th entry
	 * uses the value on port n+1.
	 *
	 * @param previousTree
	 *           The Tree with which we reached the BusAssignment. Important
	 *           because it stores information about the signal we have to
	 *           select, because we select a signal out of a selection, so that
	 *           the names could be different.
	 * @param node
	 *           The BusAssignement node
	 * @param signalPath
	 *           he signal (tree) path that describe which signal in the tree
	 *           shall be selected.
	 * @param infix
	 *           The result of the analysis to the current point.
	 * @return All found paths selected by the signalPath, regarding assignments.
	 *         Can be more than one if a signal is selected that consists of
	 *         multiple signals itself.
	 */
	private ArrayList<SignalLinePath> getPathsFromBusAssignement(SignalTree previousTree,
			AnalyzerNode node, ArrayList<String> signalPath, ArrayList<SignalLine> infix) {
		ArrayList<SignalLinePath> result; // the collected paths
		String[] assignments = ((String) busSelectorWorklist.get(node).get(
				SimulinkParameterNames.ASSIGNED_SIGNALS)).split("\\,"); // the
																							// assignments
																							// specified

		if (signalPath.size() > 0) {

			String signalStr = ""; // the signalPath converted back to dot notation
											// to compare it with the assignments

			for (String str : signalPath) { // convert signalPath back to dot
														// notation
				signalStr += str + ".";
			}
			signalStr = signalStr.substring(0, signalStr.length() - 1); // cut of
																							// the
																							// trailing
																							// "."

			if (isSignalInAssignmentSet(signalStr, assignments)) { // is the
																						// selected
																						// signal or
																						// are parts
																						// of it
																						// assigned?
				result = getPathsWithAssignment(node, assignments, signalPath, signalStr, previousTree,
						infix);
			} else {
				ArrayList<SignalLine> newInfix = new ArrayList<SignalLine>();
				newInfix.addAll(infix);
				signalPath.remove(0);
				result = getPathsFromTree(node.signalTree, signalPath, newInfix);
			}

		} else { // all signals are selected, including signals with assignments
			result = new ArrayList<SignalLinePath>();
			for (SignalTree child : node.signalTree.children) { // generate
																					// signalPath for
																					// every child in
																					// order, so you
																					// can use
																					// getPathesWithAssignment
				if (isSignalInAssignmentSet(child.value, assignments)) { // is the
																							// generated
																							// signal
																							// or parts
																							// of it
																							// assigned?
					ArrayList<String> newSignalPath = new ArrayList<String>();
					newSignalPath.add(child.value);
					result.addAll(getPathsWithAssignment(node, assignments, newSignalPath, child.value,
							previousTree, infix));
				} else {
					ArrayList<SignalLine> newInfix = new ArrayList<SignalLine>();
					newInfix.addAll(infix);
					signalPath.remove(0);
					result.addAll(getAllPathsFromTree(node.signalTree, newInfix));
				}
			}
		}

		return result;
	}

	/**
	 * If we have not path given, that selects a specific signal, all signals are
	 * relevant. This is the case if a signal is selected, that contains multiple
	 * signals and this method is called to disassemble the signal into all
	 * paths. It's mainly just tree traversal.
	 *
	 * @param tree
	 *           The tree from which all inherent paths are wanted.
	 * @param infix
	 *           The already known result that has to be added to the paths.
	 * @return All found paths in the tree with the infix added.
	 */
	private ArrayList<SignalLinePath> getAllPathsFromTree(SignalTree tree,
			ArrayList<SignalLine> infix) {

		ArrayList<SignalLinePath> result = new ArrayList<SignalLinePath>();

		for (SignalTree subTree : tree.children) { // guarantee order
			ArrayList<SignalLine> newInfix = new ArrayList<SignalLine>();
			newInfix.addAll(infix);
			result.addAll(getPathsFromTree(subTree, new ArrayList<String>(), newInfix));
		}

		return result;
	}

	/**
	 * If we have not path given, that selects a specific signal, all signals are
	 * relevant. This is the case if a signal is selected, that contains multiple
	 * signals and this method is called to disassemble the signal into all
	 * paths. It's mainly just tree traversal.
	 *
	 * @param tree
	 *           The tree from which all inherent paths are wanted.
	 * @param infix
	 *           The already known result that has to be added to the paths.
	 * @return All found paths in the tree with the infix added.
	 */
	private ArrayList<SignalLinePath> getAllPathsFromTree2( // XXX: just afraid
																				// to change the
																				// method above, but
																				// should be the
																				// safer variant
			SignalTree tree, ArrayList<SignalLine> infix) {

		ArrayList<SignalLinePath> result = new ArrayList<SignalLinePath>();

		for (SignalTree subTree : tree.children) { // guarantee order
			ArrayList<String> newPath = new ArrayList<String>();
			ArrayList<SignalLine> newInfix = new ArrayList<SignalLine>();
			newPath.add(subTree.value);
			newInfix.addAll(infix);
			result.addAll(getPathsFromTree(tree, newPath, newInfix));
		}

		return result;
	}

	/**
	 * Get all paths from a bus assignment, if the selected signal leads to an
	 * assigned signal.
	 *
	 * @param assignmentNode
	 *           The node of the BusAssignment.
	 * @param assignments
	 *           All assignments specified.
	 * @param signalPath
	 *           The signal to select. Must not be empty!
	 * @param signalStr
	 *           The <code>signalParts</code> list in dot notation.
	 * @param previousTree
	 *           The Tree with which we reached the BusAssignment.
	 * @param infix
	 *           The already known result that has to be added to the paths.
	 * @return All found paths in the tree with the infix added.
	 */
	private ArrayList<SignalLinePath> getPathsWithAssignment(AnalyzerNode assignmentNode,
			String[] assignments, ArrayList<String> signalPath, String signalStr,
			SignalTree previousTree, ArrayList<SignalLine> infix) {
		// assert !signalPath.isEmpty();

		ArrayList<String> relevantAssignments = new ArrayList<String>(); // all
																								// assignments
																								// selected
																								// by
																								// the
																								// signalPath
		HashMap<String, SignalLine> assignedSignals = new HashMap<String, SignalLine>(); // mapping
																													// from
																													// assignment
																													// string
																													// to
																													// the
																													// assigned
																													// incoming
																													// signal
																													// line
		ArrayList<ArrayList<String>> generatedSignalPaths = new ArrayList<ArrayList<String>>(); // We
																																// will
																																// generate
																																// all
																																// sub
																																// signals
																																// of
																																// the
																																// current
																																// signal
																																// path
		SignalTree child = previousTree.stringToChildMap.get(signalPath.get(0)); // the
																											// child
																											// of
																											// the
																											// previousTree
																											// specified
																											// by
																											// the
																											// signal
																											// path
		ArrayList<SignalLinePath> result = new ArrayList<SignalLinePath>();

		for (int i = 0; i < assignments.length; i++) { // search for all
																		// assignments that are
																		// needed for the current
																		// signal path
			if (assignments[i].startsWith(signalStr)) {
				relevantAssignments.add(assignments[i]);
				assignedSignals.put(assignments[i],
						getInSignalLineByInPortNumber(assignmentNode.block, i + 2));
			}
		}

		// generate all relevant paths
		for (ArrayList<String> generatedSignalPath : generateSignalPaths(child,
				assignmentNode.signalTree.stringToChildMap.get(child.value))) {
			generatedSignalPath.addAll(0, signalPath.subList(1, signalPath.size()));
			generatedSignalPaths.add(generatedSignalPath);
		}

		for (ArrayList<String> generatedSignalPath : generatedSignalPaths) {

			// generate dot notation of current path
			String pathStr = "";
			for (String str : generatedSignalPath) {
				pathStr += str + ".";
			}
			if (!pathStr.isEmpty()) {
				pathStr = pathStr.substring(0, pathStr.length() - 1); // cut of
																						// trailing
																						// dot
			}

			if (assignedSignals.get(pathStr) != null) { // is it an overwritten
																		// signal?
				SignalLine line = assignedSignals.get(pathStr);

				if (getPortWidth(line.getDstPort()) > 1) { // we got a signal with
																			// more than one signal
																			// inherited?
					result = trackBackMuxSignal(line);
					// for (SignalLinePath linePath : result) {
					// linePath.path.addAll(infix);
					// }
					for (int i = 0; i < result.size(); i++) {
						SignalLinePath path = result.remove(i);
						result.add(i, path.setTrail(infix));
					}
				} else { // just a normal single signal
				// ArrayList<SignalLine> path = new ArrayList<SignalLine>();
				// path.addAll(infix);
				// path.add(0, line);
					ArrayList<ArrayList<SignalLine>> paths = new ArrayList<ArrayList<SignalLine>>();
					infix.add(0, line);
					paths.add(infix);
					result.add(new SignalLinePath(getPortWidth(infix.get(0).getDstPort()), paths));
				}
			} else { // not an assigned signal, just handle it the usual way
				ArrayList<SignalLine> newInfix = new ArrayList<SignalLine>();
				ArrayList<String> newSignalPath = new ArrayList<String>();
				newInfix.addAll(infix);
				newSignalPath.addAll(generatedSignalPath);
				result.addAll(getPathsFromTree(assignmentNode.signalTree, newSignalPath, newInfix));
			}
		}

		return result;
	}

	/**
	 * Checks if the path specified by <code>signalStr</code> is completely
	 * covered by a path of the assignments.
	 *
	 * @param signalStr
	 *           The path in dot notation that could relate to an assignment.
	 * @param assignments
	 *           The assignment paths in dot notation.
	 * @return True, if an assignment <code>assign</code> exists, so that
	 *         <code>assign.startsWith(signalStr)</code> returns true.
	 */
	private boolean isSignalInAssignmentSet(String signalStr, String[] assignments) {

		for (String ass : assignments) {
			if (ass.startsWith(signalStr)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Generates all signalPath that are included in both trees. Two trees are
	 * needed, because in the case, that a bus assignment that overwrites a
	 * single signal with a bus the signalTree of a following bus selector
	 * contains this sub tree but the signal tree of the bus assignment not. The
	 * signal tree of the bus assignment contains signals that are not selected.
	 * So we need the paths resulting by the overlapping parts of both trees.
	 *
	 * @param tree
	 *           The first tree.
	 * @param tree2
	 *           The second tree. If this tree is of the same structure as the
	 *           first, all paths will be generated.
	 * @return All paths, that are contained in both trees and lead to a leaf of
	 *         at least one tree.
	 */
	private ArrayList<ArrayList<String>> generateSignalPaths(SignalTree tree, SignalTree tree2) {
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();

		if (tree.isLeaf() || tree2.isLeaf()) { // all further processing would
															// lead to path, that is not
															// covered by both trees
			ArrayList<String> tmp = new ArrayList<String>();
			tmp.add(tree.value);
			result.add(tmp);
		} else {
			for (SignalTree subTree : tree.children) { // recursive processing of
																		// the children
				SignalTree subTree2 = tree2.stringToChildMap.get(subTree.value);
				for (ArrayList<String> tmp : generateSignalPaths(subTree, subTree2)) {
					tmp.add(0, tree.value);
					result.add(tmp);
				}
			}
		}

		return result;
	}

	// ------------- END OF BUSANALYSIS, BEGIN OF MUXANALYSIS ------------- //

	/**
	 * Analyze a demux node and allocates to every outgoing SignalLine the paths
	 * from the source SignalLine to it.
	 *
	 * @param node
	 *           The demux to analyze.
	 */
	private void updateDemuxSignals(AnalyzerNode node) {
		node.isAlreadyUpdated = true; // skip
		if (node.isAlreadyUpdated) {
			return; // don't analyze a block twice
		}

		MeMoPlugin.out.println("        Analyzing Demux block "
				+ node.block.getFullQualifiedName(false));

		// int inPortWidth =
		// getPortWidth((node.block.getInPorts().iterator().next())); // perhaps
		// as checksum? A demux has one input and x outputs
		int[] outPortWidths = new int[node.block.getOutPortsMap().keySet().size()]; // the
																												// width
																												// of
																												// all
																												// outports
		ArrayList<SignalLinePath> result; // the result of the analysis
		SignalLine actualSignal;

		for (int key : node.block.getOutPortsMap().keySet()) { // fill the
																					// outPortsWidths
																					// array with
																					// values
			Integer value = getPortWidth(node.block.getOutPortsMap().get(key));
			outPortWidths[key - 1] = value;
		}

		actualSignal = getInSignalLineByInPortNumber(node.block, 1); // demux has
																							// only one
																							// input

		result = trackBackMuxSignal(actualSignal); // get all the signals on the
																	// input signal line

		updateDemuxSignalPaths(node, result, outPortWidths); // assign the
																				// returned signals
																				// to the correct
																				// outputs

		node.isAlreadyUpdated = true;

	}

	/**
	 * Fills the signalPathList of the mux. The method just gets the incoming
	 * signals in order and assignes them to any outgoing signal.
	 *
	 * @param mux
	 *           The mux node to analyse.
	 */
	private void updateMuxSignals(AnalyzerNode mux) {
		mux.isAlreadyUpdated = true; // skip

		if (mux.isAlreadyUpdated) {
			return;
		}

		MeMoPlugin.out
				.println("        Analyzing Mux block " + mux.block.getFullQualifiedName(false));

		HashMap<Port, SignalLine> inPort2inSignal = new HashMap<Port, SignalLine>(); // in
																												// signal
																												// map
																												// for
																												// easy
																												// access
		ArrayList<SignalLinePath> result = new ArrayList<SignalLinePath>();

		for (SignalLine signal : mux.block.getInSignals()) { // initialize the in
																				// signal map
			inPort2inSignal.put(signal.getDstPort(), signal);
		}
		for (int key = 1; key <= mux.block.getInPortsMap().keySet().size(); key++) { // get
																												// the
																												// in
																												// ports
																												// in
																												// order
			Port port = mux.block.getInPortsMap().get(key);
			SignalLine inSignal = inPort2inSignal.get(port);

			if (getPortWidth(port) > 1) {
				result.addAll(trackBackMuxSignal(inSignal));
			} else {
				ArrayList<SignalLine> tmp = new ArrayList<SignalLine>();
				ArrayList<ArrayList<SignalLine>> tmps = new ArrayList<ArrayList<SignalLine>>();
				tmp.add(inSignal); // add the related in signal line.
				tmps.add(tmp);
				result.add(new SignalLinePath(getPortWidth(port), tmps));
			}
		}

		for (SignalLine signal : mux.block.getOutSignals()) { // assign the result
																				// to every out
																				// signal
			ArrayList<SignalLinePath> newPaths = new ArrayList<SignalLinePath>();
			for (SignalLinePath path : result) {
				newPaths.add(path.setTrail(signal));
			}
			mux.signalPathList.put(signal, newPaths);
		}

		mux.isAlreadyUpdated = true;

	}

	/**
	 * The demux splits the incoming mux signal and outputs the results to the
	 * output ports. The sum of the outport widths equals the inport width. The
	 * first outport selects the first n1 signals of the incoming signals, the
	 * second the following n2 and so on. Matlab calculates how great the ni's
	 * are dependent on the number of outports.
	 *
	 * @param demux
	 *           The demux block to update.
	 * @param result
	 *           All signals in the incoming mux signal.
	 * @param outPortWidths
	 *           The port widths of the outputs. Important to select the right
	 *           signals.
	 */
	private void updateDemuxSignalPaths(AnalyzerNode demux, ArrayList<SignalLinePath> result,
			int[] outPortWidths) {

		HashMap<SignalLinePath, Integer> pathMultipliers = new HashMap<SignalLinePath, Integer>(); // the
																																	// signals
																																	// in
																																	// the
																																	// result
																																	// list
																																	// have
																																	// different
																																	// widths

		for (SignalLinePath path : result) { // get all path widths
			int portWidth = path.signalWidth;
			pathMultipliers.put(path, portWidth);
		}

		for (int i = 0; i < demux.block.getOutPorts().size(); i++) { // get ports
																							// in order
			Port port = demux.block.getOutPortsMap().get(i + 1);
			int remainingOutPortWidth = getPortWidth(port); // the number of
																			// signals that still
																			// have to assigned to
																			// the current port
			while (remainingOutPortWidth > 0) {
				SignalLinePath signalPath = result.get(0); // get the first not yet
																			// consumed path
				int signalPathWidth = pathMultipliers.get(signalPath); // get the
																							// not yet
																							// consumed
																							// path
																							// size

				for (SignalLine signal : demux.block.getOutSignals()) { // add the
																							// current
																							// path to
																							// the
																							// port,
																							// associate
																							// it with
																							// every
																							// out
																							// signal
																							// line of
																							// the port
					if (port.equals(signal.getSrcPort())) {
						demux.signalPathList.get(signal).add(signalPath.setTrail(signal));
					}
				}

				if (signalPathWidth <= remainingOutPortWidth) {
					result.remove(0); // remove consumed fully paths
				} else {
					pathMultipliers.put(signalPath, signalPathWidth - remainingOutPortWidth); // reduce
																														// the
																														// path
																														// multiplier
																														// by
																														// the
																														// consumed
																														// number
				}

				remainingOutPortWidth -= signalPathWidth; // could be negative, that
																		// just means, that there
																		// is signal width left
																		// over for the next port

			}

		}

	}

	/**
	 * Follows a SignalLine with a mux signal to all of its origins.
	 *
	 * @param actualSignal
	 *           The signal line with a bus signal on it.
	 * @return All paths from the origin to actualSignal.
	 */
	private ArrayList<SignalLinePath> trackBackMuxSignal(SignalLine actualSignal) {
		ArrayList<SignalLinePath> result = new ArrayList<SignalLinePath>();
		ArrayList<SignalLine> infix = new ArrayList<SignalLine>(); // the already
																						// found path
																						// fraction
		Block previousBlock = actualSignal.getSrcBlock();

		while (muxPassThroughBlock(previousBlock) && (getPortWidth(actualSignal.getSrcPort()) > 1)) { // can
																																		// we
																																		// safely
																																		// pass
																																		// through
																																		// the
																																		// Block?
																																		// Do
																																		// it!
			infix.add(actualSignal);
			actualSignal = previousBlock.getInSignals().toArray(new SignalLine[0])[0]; // a
																												// port
																												// only
																												// has
																												// one
																												// incoming
																												// signal
																												// line.
																												// Get
																												// this!
			previousBlock = actualSignal.getSrcBlock();
		}

		if (SimulinkBlockConstants.BUSSELECTOR_BLOCKTYPE.equals(previousBlock.getType())) {

			AnalyzerNode prevBlockNode = block2node.get(previousBlock);
			ArrayList<SignalLinePath> signalPaths;

			updateBusSelectorSignals(prevBlockNode); // update if not already
																	// updated

			signalPaths = prevBlockNode.signalPathList.get(actualSignal); // will
																								// contain
																								// actualSignal,
																								// don't
																								// add
																								// it
																								// twice

			for (SignalLinePath path : signalPaths) { // concatenate the paths and
																	// the infix
				result.add(path.setTrail(infix)); // add the finished path to the
																// result
			}

			prevBlockNode.outLineIsSystemBorder.put(actualSignal, BorderAnalyzeState.NO); // we
																													// visit
																													// this
																													// signal
																													// line
																													// ->
																													// it's
																													// part
																													// of
																													// the
																													// mux
																													// system

		} else if (SimulinkBlockConstants.BUSCREATOR_BLOCKTYPE.equals(previousBlock.getType())) {

			AnalyzerNode prevBlockNode = block2node.get(previousBlock);
			ArrayList<SignalLinePath> signalPaths;

			updateMuxSignals(prevBlockNode); // block is only used to create mux or
														// it is already updated by the bus
														// analysis

			signalPaths = prevBlockNode.signalPathList.get(actualSignal); // get
																								// the
																								// paths
																								// out
																								// of
																								// updated
																								// node
																								// will
																								// contain
																								// actualSignal,
																								// don't
																								// add
																								// it
																								// twice

			for (SignalLinePath path : signalPaths) { // concatenate the paths and
																	// the infix
				result.add(path.setTrail(infix)); // add the finished path to the
																// result
			}

			prevBlockNode.outLineIsSystemBorder.put(actualSignal, BorderAnalyzeState.NO); // we
																													// visit
																													// this
																													// signal
																													// line
																													// ->
																													// it's
																													// part
																													// of
																													// the
																													// mux
																													// system

		} else if (SimulinkBlockConstants.DEMUX_BLOCKTYPE.equals(previousBlock.getType())) {

			AnalyzerNode demuxNode = block2node.get(previousBlock);
			ArrayList<SignalLinePath> signalPaths;

			updateDemuxSignals(demuxNode); // update the demux node if necessary

			signalPaths = demuxNode.signalPathList.get(actualSignal); // get the
																							// paths
																							// out of
																							// updated
																							// node
																							// will
																							// contain
																							// actualSignal,
																							// don't
																							// add it
																							// twice

			for (SignalLinePath path : signalPaths) { // concatenate the paths and
																	// the infix
				result.add(path.setTrail(infix)); // add the finished path to the
																// result
			}

			demuxNode.outLineIsSystemBorder.put(actualSignal, BorderAnalyzeState.NO); // we
																												// visit
																												// this
																												// signal
																												// line
																												// ->
																												// it's
																												// part
																												// of
																												// the
																												// mux
																												// system

		} else if (SimulinkBlockConstants.MUX_BLOCKTYPE.equals(previousBlock.getType())) {

			AnalyzerNode muxNode = block2node.get(previousBlock);
			ArrayList<SignalLinePath> signalPaths;

			updateMuxSignals(muxNode);

			signalPaths = muxNode.signalPathList.get(actualSignal);

			for (SignalLinePath path : signalPaths) { // concatenate the paths and
																	// the infix
				result.add(path.setTrail(infix)); // add the finished path to the
																// result
			}

			muxNode.outLineIsSystemBorder.put(actualSignal, BorderAnalyzeState.NO); // we
																											// visit
																											// this
																											// signal
																											// line
																											// ->
																											// it's
																											// part
																											// of
																											// the
																											// mux
																											// system

		} else if (SimulinkBlockConstants.SELECTOR_BLOCKTYPE.equals(previousBlock.getType())) {

			AnalyzerNode selectorNode = block2node.get(previousBlock);
			ArrayList<SignalLinePath> signalPaths;

			if (selectorNode == null) { // create new AnalyzerNode if one doesn't
													// exist for this block
				selectorNode = new AnalyzerNode(previousBlock);
				block2node.put(previousBlock, selectorNode);
			}

			updateSelectorSignals(selectorNode); // update selector node if
																// necessary

			signalPaths = selectorNode.signalPathList.get(actualSignal);

			for (SignalLinePath path : signalPaths) { // concatenate the paths and
																	// the infix
				result.add(path.setTrail(infix)); // add the finished path to the
																// result
			}

		} else if (SimulinkBlockConstants.BUSASSIGNMENT_BLOCKTYPE.equals(previousBlock.getType())) {

			AnalyzerNode prevBlockNode = block2node.get(previousBlock);
			ArrayList<SignalLinePath> signalPaths;

			updateBusAssignementSignals(prevBlockNode); // update if not already
																		// updated

			signalPaths = prevBlockNode.signalPathList.get(actualSignal); // will
																								// contain
																								// actualSignal,
																								// don't
																								// add
																								// it
																								// twice

			for (SignalLinePath path : signalPaths) { // concatenate the paths and
																	// the infix
				result.add(path.setTrail(infix)); // add the finished path to the
																// result
			}

			prevBlockNode.outLineIsSystemBorder.put(actualSignal, BorderAnalyzeState.NO); // we
																													// visit
																													// this
																													// signal
																													// line
																													// ->
																													// it's
																													// part
																													// of
																													// the
																													// mux
																													// system

		} else if (SimulinkBlockConstants.SWITCH_BLOCK_TYPE.equals(previousBlock.getType())
				|| SimulinkBlockConstants.MULTIPORT_SWITCH_BLOCK_TYPE.equals(previousBlock.getType())
				|| SimulinkBlockConstants.MERGE_BLOCKTYPE.equals(previousBlock.getType())) {

			ArrayList<ArrayList<SignalLinePath>> resultList = new ArrayList<ArrayList<SignalLinePath>>();

			infix.add(actualSignal);

			int portToSkip = -1; // merge has no control input
			if (SimulinkBlockConstants.SWITCH_BLOCK_TYPE.equals(previousBlock.getType())) {
				portToSkip = 2;
			} else if (SimulinkBlockConstants.MULTIPORT_SWITCH_BLOCK_TYPE.equals(previousBlock
					.getType())) {
				portToSkip = 1;
			}

			// analyze the signals on every in port
			for (int i = 1; i <= previousBlock.getInPortsMap().size(); i++) {

				if (i == portToSkip) {
					continue;
				}

				ArrayList<SignalLinePath> resultListItem;
				resultListItem = trackBackMuxSignal(getInSignalLineByInPortNumber(previousBlock, i));

				resultList.add(resultListItem);
			}

			for (SignalLinePath path : resultList.get(0)) {
				for (int i = 1; i < resultList.size(); i++) {
					path.concurrentPaths.addAll(resultList.get(i).remove(0).concurrentPaths);
				}

				for (ArrayList<SignalLine> signals : path.concurrentPaths) {
					signals.addAll(infix);
				}

				result.add(path);
			}

		} else { // e.g. Constant, Add; Assume that we can't go further and so
					// just add the actual signal line TODO: better? ->
					// trackBackMuxSignal
			ArrayList<ArrayList<SignalLine>> tmp = new ArrayList<ArrayList<SignalLine>>();
			infix.add(actualSignal);
			tmp.add(infix);
			result.add(new SignalLinePath(getPortWidth(infix.get(0).getDstPort()), tmp));
		}

		return result;
	}

	/**
	 * A Selector node forwards an assortment of it input signals. Thereby it can
	 * rearrange the signal order on the mux or eliminate some of the signals.
	 * The method analyzes this behavior.
	 *
	 * @param selectorNode
	 *           The analyzer node to analyze, containing a block of the Selector
	 *           type.
	 */
	private void updateSelectorSignals(AnalyzerNode selectorNode) {

		if (selectorNode.isAlreadyUpdated) {
			return;
		}

		MeMoPlugin.out.println("        Analyzing Selector block "
				+ selectorNode.block.getFullQualifiedName(false));

		SignalLine actualSignal = getInSignalLineByInPortNumber(selectorNode.block, 1); // the
																													// mux
																													// signal
																													// is
																													// on
																													// the
																													// first
																													// in
																													// port
																													// (even
																													// if
																													// there
																													// is
																													// an
																													// idx
																													// port)
		ArrayList<SignalLinePath> rawPathes = trackBackMuxSignal(actualSignal); // the
																										// incoming
																										// signal
																										// for
																										// the
																										// selection
																										// is
																										// a
																										// mux,
																										// get
																										// paths
																										// to
																										// sources
		ArrayList<SignalLinePath> pathes = new ArrayList<SignalLinePath>(); // all
																									// the
																									// incoming
																									// paths,
																									// multiplied
																									// by
																									// the
																									// path
																									// widths
		ArrayList<SignalLinePath> result = new ArrayList<SignalLinePath>(); // the
																									// output
																									// paths
																									// of
																									// the
																									// block

		for (SignalLinePath path : rawPathes) { // multiply the raw paths by their
																// paths widths
			for (int i = 0; i < path.signalWidth; i++) {
				pathes.add(new SignalLinePath(1, path.concurrentPaths));
			}
		}

		/**
		 * The Selector node has 5 options to specify the selection.
		 *
		 * 1. Select all: Do nothing, just forward the mux. 2. Index vector
		 * (dialog): The vector describes the output vector, its entries are the
		 * signal numbers of the incoming mux. e.g. [1 5 2] means, that the fifth
		 * incoming signal will be second on the output, the originally second
		 * will be third. 3. Index vector (port): The Vector incoming on the idx
		 * port specifies the selection. Therefore it can change at runtime so we
		 * can't analyze it. 4. Starting index (dialog): It is specified how many
		 * signals are selected in order by a given index to start and a number,
		 * the output size, specifying how many signals from the starting index
		 * are selected in order. Therefore the selected signals are starting
		 * index to starting index + range. 5. Starting index (port): The starting
		 * index is specified by the scalar incoming at idx port. Therefore it can
		 * change during runtime so we can't analyze it.
		 */
		if ("Index vector (dialog)".equals(selectorNode.block.getParameter("IndexOptions"))) { // a
																															// vector
																															// to
																															// specify
																															// the
																															// selection
																															// is
																															// given
			String indices; // the vector
			String[] indicesSplit; // the vector entries

			// parse the permutation
			indices = selectorNode.block.getParameter(SimulinkParameterNames.SELECTORBLOCK_INDICES);
			indices = indices.substring(1, indices.length() - 1); // remove "[" and
																					// "]";
			indicesSplit = indices.split("\\x20"); // split at the blanks

			for (String element : indicesSplit) { // get the selected paths in the
																// right order
				int signalNr = Integer.parseInt(element) - 1;
				result.add(pathes.get(signalNr));
			}

		} else if ("Starting index (dialog)".equals(selectorNode.block.getParameter("IndexOptions"))) {
			int startingIndex = Integer.parseInt( // the beginning of the selction
																// range
					selectorNode.block.getParameter(SimulinkParameterNames.SELECTORBLOCK_INDICES));
			int outPutSize = Integer.parseInt( // the size of the selction range
					selectorNode.block.getParameter("OutputSizes"));

			for (int i = startingIndex; i < (startingIndex + outPutSize); i++) { // get
																										// all
																										// the
																										// selected
																										// paths
																										// in
																										// order
				result.add(pathes.get(i - 1));
			}

		} else if ("Select all".equals(selectorNode.block.getParameter("IndexOptions"))) { // just
																														// pass
																														// through
			result = rawPathes;

		} else { // path routing variable at runtime, can't say where signal flows
			// just add actual signal line (the incoming one) as the only one
			// incoming paths
			result.add(new SignalLinePath(getPortWidth(actualSignal.getDstPort()), actualSignal));
		}

		// add the result for every outgoing signal line
		for (SignalLine signal : selectorNode.block.getOutSignals()) {
			ArrayList<SignalLinePath> newPaths = new ArrayList<SignalLinePath>();
			for (SignalLinePath path : result) {
				newPaths.add(path.setTrail(signal));
			}
			selectorNode.signalPathList.put(signal, newPaths);
		}

		selectorNode.isAlreadyUpdated = true;

	}

	/**
	 * Uses a blacklist to determine if the algorithm can safely pass through a
	 * given block.
	 *
	 * @param block
	 *           The block to check.
	 * @return False, if the <code>block</code> has more than on inport or its
	 *         block type is in the blacklist.
	 */
	private boolean muxPassThroughBlock(Block block) {
		String type = block.getType();

		if (block.getInPorts().size() != 1) {
			return false; // which port shall we take to track back signal?
								// e.g.Constant, Add
		}

		// blacklist; some of this blocks will ctahced by the if above, but are
		// here as documentation
		return !(SimulinkBlockConstants.BUSSELECTOR_BLOCKTYPE.equals(type)
				|| SimulinkBlockConstants.BUSCREATOR_BLOCKTYPE.equals(type)
				|| SimulinkBlockConstants.MUX_BLOCKTYPE.equals(type)
				|| SimulinkBlockConstants.DEMUX_BLOCKTYPE.equals(type)
				|| SimulinkBlockConstants.SELECTOR_BLOCKTYPE.equals(type)
				|| SimulinkBlockConstants.BUSASSIGNMENT_BLOCKTYPE.equals(type)
				|| type.equals(SimulinkBlockConstants.SWITCH_BLOCK_TYPE)
				|| type.equals(SimulinkBlockConstants.MULTIPORT_SWITCH_BLOCK_TYPE) || type
					.equals(SimulinkBlockConstants.MERGE_BLOCKTYPE));
	}

	/**
	 * Uses a blacklist to determine if the algorithm can safely pass through a
	 * given block.
	 *
	 * @param block
	 *           The block to check.
	 * @return False, if the <code>block</code> has more than on inport or its
	 *         block type is in the blacklist.
	 */
	private boolean busPassThroughBlock(Block block) {
		// return muxPassThrough(block);
		String type = block.getType();

		if (block.getInPorts().size() != 1) {
			return false; // which port shall we take to track back signal?
								// e.g.Constant, Add
		}

		// blacklist; some of this blocks will ctahced by the if above, but are
		// here as documentation
		return // muxPassThroughBlock(block) &&
		!(SimulinkBlockConstants.BUSSELECTOR_BLOCKTYPE.equals(type)
				|| SimulinkBlockConstants.BUSCREATOR_BLOCKTYPE.equals(type)
				|| SimulinkBlockConstants.MUX_BLOCKTYPE.equals(type)
				|| SimulinkBlockConstants.DEMUX_BLOCKTYPE.equals(type)
				|| SimulinkBlockConstants.BUSASSIGNMENT_BLOCKTYPE.equals(type)
				|| type.equals(SimulinkBlockConstants.SWITCH_BLOCK_TYPE)
				|| type.equals(SimulinkBlockConstants.MULTIPORT_SWITCH_BLOCK_TYPE) || type
					.equals(SimulinkBlockConstants.MERGE_BLOCKTYPE));
	}

	/**
	 * There should be only one signal line at every port. To get the incoming
	 * signal line of port specified by its port number, call this method.
	 *
	 * @param block
	 *           The target block of the wanted signal line.
	 * @param inPortNumber
	 *           The number of the input port, where the signal line belongs to.
	 * @return The signal line with <code>block</code> as target block and the
	 *         port given by <code>inPortNumber</code> as target port. Returns
	 *         null, if no signal line is found.
	 */
	private SignalLine getInSignalLineByInPortNumber(Block block, int inPortNumber) {
		Port port = block.getInPortsMap().get(inPortNumber);
		for (SignalLine s : block.getInSignals()) {
			if (s.getDstPort().equals(port)) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Returns the size of a port, the width of the input/output signal.
	 *
	 * @param port
	 *           The port its width we want to know.
	 * @return The port width.
	 */
	private int getPortWidth(Port port) {
		String value = port.getParameter().get(SimulinkParameterNames.COMPILED_PORT_WIDTHS)
				.replace(".0", "");
		return Integer.parseInt(value);
	}

	/**
	 * Add a BusSelector to analyze. It is important to add it manually because
	 * the "InputSignals" parameter and the "OutputSignals" parameter are needed
	 * for the analysis.
	 *
	 * @param block
	 *           The block that shall be added.
	 * @param parameters
	 *           The block parameters.
	 */
	public void addBusSelector(Block block, HashMap<String, Object> parameters) {
		AnalyzerNode node = new AnalyzerNode(block);
		busSelectorWorklist.put(node, parameters);
		block2node.put(block, node);
	}

	/**
	 * Adds a Demux block to analyze. The parameters are not needed currently.
	 *
	 * @param block
	 *           The Demux that shall be added.
	 * @param parameters
	 *           The Demux parameters.
	 */
	public void addDemux(Block block, HashMap<String, Object> parameters) {
		AnalyzerNode node = new AnalyzerNode(block);
		demuxWorklist.put(node, parameters);
		block2node.put(block, node);
	}

	/**
	 * Adds a Mux block to analyze.
	 *
	 * @param block
	 *           The Mux to add.
	 */
	public void addMux(Block block) {
		AnalyzerNode node = new AnalyzerNode(block);
		block2node.put(block, node);
		muxes.add(node);

	}

	/**
	 * Adds a BusCreator block to analyze.
	 *
	 * @param block
	 *           The BusCreator to add.
	 */
	public void addBusCreator(Block block) {
		AnalyzerNode node = new AnalyzerNode(block);
		block2node.put(block, node);
		busCreators.add(node);

	}

}
