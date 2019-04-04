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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
//import java.util.ArrayList;
import java.util.Collection;
//import java.util.HashMap;
//import java.util.List;


import javax.swing.JOptionPane;

//import org.eclipse.core.runtime.CoreException;


import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.impl.Block;
import de.tu_berlin.pes.memo.model.impl.Model;
//import de.tu_berlin.pes.memo.model.impl.ReferenceBlock;
//import de.tu_berlin.pes.memo.model.impl.WSVariable;
//import de.tu_berlin.pes.memo.model.util.MemoFieldNames;
//import de.tu_berlin.pes.memo.model.util.SimulinkBlockConstants;
//import de.tu_berlin.pes.memo.model.util.SimulinkParameterNames;
//import de.tu_berlin.pes.memo.model.util.SimulinkParameterValues;
import de.tu_berlin.pes.memo.parser.BusMuxAnalyzer;
import de.tu_berlin.pes.memo.parser.MeMoParserManager;
import de.tu_berlin.pes.memo.parser.StandalonePreferenceStore;
//import de.tu_berlin.pes.memo.parser.parsewizard.MemoModelParseWizard;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

//import de.tu_berlin.pes.memo.project.ProjectUtil;

public class MeMoMatlabManager {

	private static MatlabConnector connector;

	private static boolean useJMBridgeConnector = false;

	/**
	 * Evaluates the given model in matlab to get the port parameters
	 *
	 * @param model
	 *           the model to evaluate
	 * @param busanalyzer
	 *           the Analyzer for the bus and mux system
	 * @param currentProject
	 *           the eclipse project that called this method (indirectly)
	 */
	public static void matlabPortEvaluation(Model model, BusMuxAnalyzer busanalyzer) {

		connect();
		try {
			eval("cd '" + (new File(MeMoParserManager.getOpenedFile())).getParent() + "'");
			loadMFiles(MeMoParserManager.getmFiles());
			if(MeMoParserManager.getLibFileMap() != null) {
				loadLibs(MeMoParserManager.getLibFileMap().values());
			}
			loadModelRefs(MeMoParserManager.getModelReferenceFilesMap().values());
			eval("load_system('" + MeMoParserManager.getOpenedFile() + "')");
			MatlabEvaluation eval = new MatlabEvaluation(model, MeMoParserManager
					.getModelReferenceFilesMap().keySet(), connector, true, busanalyzer,
					useJMBridgeConnector);
			eval.evaluate();
		} catch (Exception e) {
			
			MeMoPlugin.logException(e.toString(), e);
			throw e;
		}
		try {
			diconnect(false);
		} catch (Exception e) {
			MeMoPlugin.out.println("Error while closing Connection...");
			MeMoPlugin.logException(e.toString(), e);
		}
	}

	/**
	 * Load the m-Files for the currently selected model in matlab.
	 *
	 * @param collection
	 *           The files that shall be executed in matlab.
	 */
	public static void loadMFiles(Collection<File> files) {
		if(files == null) return;
		MeMoPlugin.out.println("[INFO] Loading M-Files in MATLAB...");
		for (File file : files) {
			eval("run '"
					+ file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf('.'))
					+ "';");
		}
	}

	/**
	 * Load the libraries for the currently selected model in matlab.
	 *
	 * @param collection
	 *           The files that shall be loaded as libraries.
	 */
	public static void loadLibs(Collection<File> files) {
		if(files == null) return;
		MeMoPlugin.out.println("[INFO] Loading Library-Files in MATLAB...");
		// MeMoParserManager.setLibFileMap(new HashMap<String, File>());
		for (File file : files) {
			eval("addpath('" + file.getParent() + "');");
			// runMatlabCommand("load_system('" +
			// file.getAbsolutePath().substring(0,
			// file.getAbsolutePath().lastIndexOf('.')) + "');",
			// con);
		}
	}

	/**
	 * Load files as model references.
	 *
	 * @param collection
	 *           The files that shall be loaded as model references.
	 */
	public static void loadModelRefs(Collection<File> collection) {
		MeMoPlugin.out.println("[INFO] Loading Model References in MATLAB...");
		for (File file : collection) {
			eval("load_system('"
					+ file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf('.'))
					+ "');");
		}
	}

	/**
	 *
	 *
	 * @param urlDescription
	 *           The URL. Currently only one URL Type will be processed: URLs to
	 *           open a system part. It must be in the pattern
	 *           "matlab: open_system('systemPartPath');"
	 * @param path
	 *           The path to the related model. Important to load the model if
	 *           not already open in matlab.
	 */
	public static void openSimulinkURL(URI uri, String path) {

		if ("matlab".equals(uri.getScheme()) && "open_system".equals(uri.getQuery())) {
			String systemName;
			String modelName;

			try {
				systemName = URLDecoder.decode(uri.getPath(), "UTF-8").replace("\\n", " ").substring(1);
			} catch (UnsupportedEncodingException e1) {
				systemName = uri.getPath().replace("\\n", " ").substring(1);
				MeMoPlugin.logException(e1.toString(), e1);
			} // replace new lines and cut leading slash
			modelName = systemName.substring(0, systemName.indexOf("/")); // extract
																								// model
																								// name
																								// from
																								// system
																								// name

			connect();

			connector.eval("cd '" + path + "'");

			connector.eval("load_system('" + modelName + "')"); // always load
																					// model, because
																					// user could
																					// close it
			connector.eval("open_system('" + systemName + "')");
		}
	}

	/**
	 * Creates a html String of the form
	 * {@literal <a href=\"matlab:/blockPath?open_system\">blockPath</a>} <br>
	 * <br>
	 * Used for html capable gui elements as the old MeMoSwingView.
	 *
	 * @param block
	 *           The block the URL shall be created for
	 * @return The URL-String
	 */
	public static String createBlockURLString(Block block) {
		try {
			String encodedName = URLEncoder.encode(
					block.getFullQualifiedName(true).replace("\\n", ""), "UTF-8");
			String result = "<a href=\"matlab:/" + encodedName + "?open_system\">"
					+ block.getFullQualifiedName() + "</a>";
			return result;

		} catch (UnsupportedEncodingException e) {
			MeMoPlugin.logException(e.toString(), e);
			return null;
		}
	}

	/**
	 * Shuts down a possible active Matlab connection. This Method needs some
	 * time, because it calls Thread.sleep(). Thats because releasing the
	 * connection needs some time so connect() can be called without errors after
	 * this call.
	 *
	 * @param askToCloseML
	 *           If matlab is open, it will ask to close matlab or closes it
	 *           without asking .
	 * @throws Exception
	 */
	public static void diconnect(boolean askToCloseML) throws Exception {
		// close open matlab instance to unbind port 5050
		if (connector == null) {
			return;
		}
		if (connector.getIsConnected()) {
			if (askToCloseML) {
				int answer = JOptionPane.showConfirmDialog(null,
						"Matlab Conection still open. Close Matlab?", "Open Matlab Connection",
						JOptionPane.YES_NO_OPTION);
				if (answer == JOptionPane.YES_OPTION) {
					connector.releaseConnection(true);
				} else {
					connector.releaseConnection(false);
				}
			} else {
				connector.releaseConnection(true);
			}
		}

		// release connection needs some time, so that connect() will work again
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			MeMoPlugin.logException(e.toString(), e);
		}
	}

	/**
	 * Open and connect to Matlab.
	 *
	 * @return If the task was successful.
	 */
	public static boolean connect() {

		boolean result = true;

		if (connector == null) {
			if (useJMBridgeConnector) {
				connector = new MatlabJMBridgeConnector();
			} else {
				connector = new MatlabControlConnector(true);
			}
		}

		if (!connector.getIsConnected()) {
			try {
				if (!connector.connect()) { // happens if user closes matlab
														// manually
					connector.releaseConnection(false);
					result = connector.connect();
				}
			} catch (Exception e) {
				result = false;
				MeMoPlugin.logException(e.toString(), e);
			}
		}

		return result;
	}

	/**
	 * Evaluates the command in matlab. Connects to matlab if necessary.
	 *
	 * @param command
	 *           The m-code to run in matlab.
	 * @return If the command was successful executed.
	 */
	public static boolean eval(String command) {
		int timeout = 5;
		int tries = 5;
		if(MeMoPlugin.getDefault() != null) {
			timeout = MeMoPlugin.getDefault().getPreferenceStore()
					.getInt(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT) * 1000;
			tries = MeMoPlugin.getDefault().getPreferenceStore()
					.getInt(MeMoPreferenceConstants.PR_MATLAB_TRIES);
		} else {
			timeout = StandalonePreferenceStore.getInstance().getTimeout();
		}

		connect();

		return RunMatlabCommand.runEvalCommand(command, connector, tries, timeout);
	}

	/**
	 * Gets a variable from the matlab (base) workspace. Connects to matlab if
	 * necessary.
	 *
	 * @param name
	 *           The name of the variable.
	 * @return The content of the variable. Null if not successfull.
	 */
	public static Object getVariable(String name) {
		int timeout = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT) * 1000;
		int tries = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TRIES);

		connect();

		RunMatlabCommand thread = RunMatlabCommand.runGetVarCommand(name, connector, tries, timeout);

		return thread.isSuccessful() ? thread.getResult() : null; // TODO: what,
																						// if it was
																						// not
																						// successful?
																						// discriminate
																						// between
																						// real "null"
																						// result and
																						// default
																						// return
																						// value
	}

}
