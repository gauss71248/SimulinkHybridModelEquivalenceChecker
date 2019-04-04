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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipFile;

import org.conqat.lib.simulink.builder.MDLParser;
import org.conqat.lib.simulink.builder.MDLScanner;
import org.conqat.lib.simulink.builder.MDLSection;
import org.conqat.lib.simulink.builder.SimulinkModelBuildingException;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.util.SimulinkParameterNames;
import de.tu_berlin.pes.memo.model.util.SimulinkSectionConstants;
import de.tu_berlin.pes.memo.parser.slx.SLXParser;

/**
 * @author reicherdt
 */
public class MeMoParserManager {

	/**
	 * @uml.property name="openedFile"
	 */
	private static String openedFile; // The Model File path, where the directory
													// in Matlab is/will be changed to
	/**
	 * @uml.property name="mFiles"
	 */
	private static List<File> mFiles; // The List of given M-Files given from the
													// user/gui
	/**
	 * @uml.property name="libFile"
	 */
	private static HashMap<String, File> libFileMap; // All library files given
																		// from the user/gui
																		// mapped by their name
																		// for easy access
	private static HashMap<String, MDLSection> parsedLibFilesMap; // All yet
																						// parsed
																						// library
																						// files
	private static HashMap<MDLSection, HashMap<String, String>> forwardingTables;

	private static HashMap<String, MDLSection> parsedModelReferencesMap = new HashMap<String, MDLSection>();
	private static HashMap<String, File> modelReferenceFilesMap = new HashMap<String, File>();

	/**
	 * Parses a *.mdl-File and logs the process in "log.txt".
	 *
	 * @param file
	 *           the *.mdl-File
	 * @return the AST of the file
	 */
	public static MDLSection loadModelSectionFromFile(String file) {
		try {
			// selects if it's a *.slx, *.xml or *.mdl file
			String extension = file.substring(file.lastIndexOf(".") + 1).toLowerCase();
			if (extension.equals("mdl")) {
				// create the AST of a *.mdl-file
				MeMoPlugin.out.println("[INFO] Parsing *.mdl-File " + file);
				MDLSection section = (MDLSection) new MDLParser(new MDLScanner(
						new FileInputStream(file))).parse().value;
				openedFile = new File(file).getAbsolutePath();
				return section;
			} else if (extension.equals("xml")) {
				// create the AST of a *.xml-file
				MeMoPlugin.out.println("[INFO] Parsing *.xml-File " + file);
				MDLSection section = new SLXParser(new File(file)).parse();
				openedFile = new File(file).getAbsolutePath();
				return section;
			} else if (extension.equals("slx")) {
				// create the AST of a *.slx-file
				MeMoPlugin.out.println("[INFO] Parsing *.slx-File " + file);
				MDLSection section = new SLXParser(new ZipFile(file)).parse();
				openedFile = new File(file).getAbsolutePath();
				return section;
			}

		} catch (FileNotFoundException e) {
			MeMoPlugin.logException(e.toString(), e);
		} catch (SimulinkModelBuildingException e) {
			MeMoPlugin.logException(e.toString(), e);
		} catch (Exception e) {
			MeMoPlugin.logException(e.toString(), e);
		}
		return null;
	}

	/**
	 * Checks if a file has the given extension.
	 *
	 * @param file
	 *           The file to check.
	 * @param ext
	 *           The extension the file should have.
	 * @return true if the file extension and ext are the same ignoring case.
	 */
	private static boolean checkFileExtension(File file, String ext) {
		String name = file.getName().trim();
		int dotPos = name.lastIndexOf(".");
		String extension = name.substring(dotPos + 1);
		return extension.equalsIgnoreCase(ext);
	}

	/**
	 * Get the parsed (by CUP) Library from the library-files given by the user.
	 * If the library is not found, it returns null, otherwise the AST of the
	 * parsed library. If the library contaisn forwardings they will be
	 * accessable by {@link #getForwardingTable(MDLSection)}.
	 *
	 * @param libName
	 *           The name of the library.
	 * @return null or the parsed library.
	 */
	public static MDLSection getLibraryAST(String libName) {
		MDLSection ast = null;
		if(MeMoParserManager.parsedLibFilesMap != null) {
			ast = MeMoParserManager.parsedLibFilesMap.get(libName);
		}
		if (ast == null) {
			File libFile = null;
			if(MeMoParserManager.libFileMap != null) {
				 libFile = MeMoParserManager.libFileMap.get(libName);	
			}
			if (libFile == null) {
				MeMoPlugin.out.println("[ERROR] Library " + libName + " not found");
			} else {
				String filePath = MeMoParserPlugin.getDefault().getStateLocation().toOSString();
				// selects if it's a *.slx, *.xml or *.mdl file
				String extension = libFile.toString().substring(libFile.toString().lastIndexOf(".") + 1).toLowerCase();
				MeMoPlugin.out.println("[INFO] Parsing library " + libFile.getAbsolutePath());
				if (extension.equals("mdl")) {
					// create the AST of a *.mdl-file
					MeMoPlugin.out.println("[INFO] Parsing *.mdl-File " + filePath);
					try {
						ast = (MDLSection) new MDLParser(new MDLScanner(new FileInputStream(libFile)))
								.parse().value;
					} catch (FileNotFoundException e) {
						// File should be found, because files in libFileMap are checked
						MeMoPlugin.logException(e.toString(), e);
					} catch (SimulinkModelBuildingException e) {
						MeMoPlugin.logException(e.toString(), e);
					} catch (Exception e) {
						MeMoPlugin.logException(e.toString(), e);
					}
				} else if (extension.equals("xml")) {
				// create the AST of a *.xml-file
					MeMoPlugin.out.println("[INFO] Parsing *.xml-File " + libFile.toString());
					try {
						ast = new SLXParser(libFile).parse();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (extension.equals("slx")) {
				// create the AST of a *.slx-file
					MeMoPlugin.out.println("[INFO] Parsing *.slx-File " + libFile.toString());
					try {
						ast = new SLXParser(new ZipFile(libFile)).parse();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (ast.getFirstSubSection(SimulinkSectionConstants.LIBRARY_SECTION_TYPE) != null) {
					MeMoParserManager.parsedLibFilesMap.put(libName, ast);
					fillForwardingTable(ast);
					return ast;
				} else {
					MeMoPlugin.out.println("[ERROR] " + libFile + " is not the library " + libName);
				}
			}
		} else {
			return ast;
		}
		return null;
	}

	/**
	 * Parsed the forwarding table of a library and stores it in
	 * {@link #forwardingTables}.
	 *
	 * @param ast
	 *           The AST of the library.
	 */
	private static void fillForwardingTable(MDLSection ast) {
		HashMap<String, String> table = new HashMap<String, String>();
		forwardingTables.put(ast, table);
		String value = ast.getFirstSubSection(SimulinkSectionConstants.LIBRARY_SECTION_TYPE)
				.getParameter(SimulinkParameterNames.FORWARDING_TABLE_STRING);

		if (value == null) {
			return;
		}

		String[] items = value.split("\\|\\|__slOldName__\\|\\|");

		for (String str : items) {
			String[] pair = str.split("\\|\\|__slNewName__\\|\\|");

			if (pair.length != 2) {
				continue; // items[0] will be "", because ||__slOldName__|| is
								// leading ForwardingTableString
			}

			table.put(pair[0], pair[1]);
		}

	}

	/**
	 * @return the openedFile
	 * @uml.property name="openedFile"
	 */
	public static String getOpenedFile() {
		return openedFile;
	}

	/**
	 * @return the matlab files
	 * @uml.property name="mFiles"
	 */
	public static List<File> getmFiles() {
		return mFiles;
	}

	/**
	 * @param mFiles
	 *           the Matlab files to set
	 * @uml.property name="mFiles"
	 */
	public static void setmFiles(List<File> mFiles) {
		MeMoParserManager.mFiles = new LinkedList<File>();
		// if an m-file adds more libraries, the user should add them manually to
		// the tool
		for (File file : mFiles) {
			if (file.exists() && file.isFile() && checkFileExtension(file, "m")) {
				MeMoParserManager.mFiles.add(file);
			}
		}
	}

	/**
	 * @param libFiles
	 *           the library files and model references to set
	 */
	public static void setLibFiles(List<File> libFiles) {
		libFileMap = new HashMap<String, File>();
		parsedLibFilesMap = new HashMap<String, MDLSection>();
		forwardingTables = new HashMap<MDLSection, HashMap<String, String>>();
		parsedModelReferencesMap = new HashMap<String, MDLSection>();
		modelReferenceFilesMap = new HashMap<String, File>();
		String name = "";
		for (File file : libFiles) {
			if (file.exists() && (checkFileExtension(file, "mdl") || checkFileExtension(file, "slx"))) {
				if (file.isFile()) {
					name = file.getName().substring(0, file.getName().lastIndexOf('.'));
//					name = file.getName();
					MeMoParserManager.libFileMap.put(name, file);
				}
			} else if (file.exists() && file.isDirectory()) {
				addLibraryDirectory(file);
			}
		}
	}

	/**
	 * Adds all *.mdl Files of a folder and its sub directories as libraries or
	 * model references.
	 *
	 * @param directory
	 *           the folder containing the libraries
	 */
	private static void addLibraryDirectory(File directory) {
		String name = "";
		for (File file : directory.listFiles()) {
			if (file.exists() && (checkFileExtension(file, "mdl") || checkFileExtension(file, "slx"))) {
				if (file.isFile()) {
					name = file.getName().substring(0, file.getName().lastIndexOf('.'));
					MeMoParserManager.libFileMap.put(name, file);
				} else if (file.isDirectory()) {
					addLibraryDirectory(file);
				}
			}
		}

	}

	/**
	 * @return the libFileMap
	 * @uml.property name="libFile"
	 */
	public static HashMap<String, File> getLibFileMap() {
		return libFileMap;
	}

	/**
	 * @param libFileMap
	 *           the libFileMap to set
	 * @uml.property name="libFile"
	 */
	public static void setLibFileMap(HashMap<String, File> libFileMap) {
		MeMoParserManager.libFileMap = libFileMap;
	}

	/**
	 * Returns a map of forwardings for a libray. The HashMap contains
	 * MapElements of the form OldName=NewName.
	 *
	 * @param library
	 *           The Ast of the library.
	 * @return The forwarding map.
	 */
	public static HashMap<String, String> getForwardingTable(MDLSection library) {
		return forwardingTables.get(library);
	}

	/**
	 * Similar to libraries this method returns the AST for a model reference.
	 * First the method searches for the referenced model file among the user
	 * given library files. If this is unsuccessful it tries to find a mdl-file
	 * with the right name in the same directory as the main model.
	 *
	 * @param parameter
	 *           The name of the referenced model
	 * @return
	 */
	public static MDLSection getRefModelAST(String parameter) {
		MDLSection result = parsedModelReferencesMap.get(parameter);
		if (result == null && libFileMap != null) {
//			File newFile = libFileMap.get(parameter.substring(0, parameter.lastIndexOf('.')));
			File newFile = libFileMap.get(parameter);
			if (newFile == null) { // reference not found? try to guess
				File mdl = new File((new File(openedFile)).getParent() + "\\" + parameter + ".mdl");
				File slx = new File((new File(openedFile)).getParent() + "\\" + parameter + ".slx");
				if(slx.exists()) {
					newFile = slx;
				} else {
					newFile = mdl;
				}
			}
			if (newFile.exists()) {
				try {
					String oldOpenedFile = openedFile;
					result = loadModelSectionFromFile(newFile.getCanonicalPath());
					openedFile = oldOpenedFile;
					parsedModelReferencesMap.put(parameter, result);
					modelReferenceFilesMap.put(parameter, newFile);
				} catch (IOException e) {
					MeMoPlugin.logException(e.toString(), e);
				}
			}
		}
		return result;
	}

	/**
	 * get all parsed model references.
	 *
	 * @return
	 */
	public static HashMap<String, File> getModelReferenceFilesMap() {
		return modelReferenceFilesMap;
	}

	/**
	 * Set the map of parsed model references.
	 *
	 * @param modelReferenceFilesMap
	 */
	public static void setModelReferenceFilesMap(HashMap<String, File> modelReferenceFilesMap) {
		MeMoParserManager.modelReferenceFilesMap = modelReferenceFilesMap;
	}
}
