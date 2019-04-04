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
//   (c) Copyright 2010-2012 PES Software Engineering for Embedded Systems, TU Berlin
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

package de.tu_berlin.pes.memo.parser.stateflow.testenvironment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import de.tu_berlin.pes.memo.parser.stateflow.stateflowparser.StateflowStateParser;
import de.tu_berlin.pes.memo.parser.stateflow.stateflowparser.StateflowStateScanner;
import de.tu_berlin.pes.memo.parser.stateflow.stateflowparser.StateflowTransitionParser;
import de.tu_berlin.pes.memo.parser.stateflow.stateflowparser.StateflowTransitionScanner;

/**
 * Interface for the Parser,to parse user-specified files and to output not
 * parseable strings into a new created state_errorlist.txt and
 * transition_errorlist.txt
 */
public class ParserWrapper {

	public enum LogType {
		SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST
	}

	static Boolean debug = false;
	private static Logger logger = Logger.getLogger(ParserWrapper.class.getName());

	/**
	 * Extracts all states as Strings of a .mdl file (the labelStrings)
	 *
	 * @param file
	 *           contains the correct path including filename to the mdl file
	 */
	static ArrayList<String> readStates(String file) {

		ArrayList<String> states = new ArrayList<String>();

		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			Boolean in_state = false;
			Boolean in_substate = false;
			Boolean in_labelstring = false;
			Boolean supported_state = false;
			StringBuffer extracted_string = new StringBuffer();

			print('\n' + "READING STATES OF FILE: " + file + '\n' + "#############");

			while ((line = in.readLine()) != null) {

				if (in_state == true) {

					if (line.contains("labelString")) {
						in_labelstring = true;
						supported_state = true;
						extracted_string.append(line.substring(line.indexOf("\"") + 1,
								line.lastIndexOf("\"")));
						print("LabelString: " + extracted_string);
					} else if (line.contains("\"")) {
						if (in_labelstring) {
							String additional_label = line.substring(line.indexOf("\"") + 1,
									line.lastIndexOf("\""));
							extracted_string.append(additional_label);
							print("AdditionalLabelString: " + additional_label);
						}
					}
					// to filter still not supported types
					else if (line.contains("type")) {
						if (/* line.contains("FUNC_STATE") || */line.contains("GROUP_STATE")) {
							supported_state = false;
							print("not supported type: " + line);
						}
					} else if (line.contains("{")) {
						in_substate = true;
						in_labelstring = false;
					} else if (line.contains("}")) {
						if (in_substate == false) {
							in_state = false;

							if (supported_state && (extracted_string.length() != 0)) {
								states.add(extracted_string.toString());
							}
							extracted_string.setLength(0);
						} else {
							in_substate = false;
						}
					}
				} else {
					if (line.contains("state {")) {
						in_state = true;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		print('\n' + "STATE READYLIST: " + '\n' + "#############");
		printStringList(states);
		return states;

	}

	/**
	 * Extracts all transitions of a .mdl file
	 *
	 * @param file
	 *           contains the correct path including filename to the mdl file
	 */
	static ArrayList<String> readTransitions(String file) {

		ArrayList<String> transitions = new ArrayList<String>();

		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String line;
			Boolean in_transition = false;
			Boolean in_labelstring = false;
			Boolean supported_state = false;
			StringBuffer extracted_string = new StringBuffer();

			print('\n' + "READING TRANSITIONS OF FILE: " + file + '\n' + "#############");

			while ((line = in.readLine()) != null) {

				if (in_transition == true) {

					if (line.contains("labelString")) {
						in_labelstring = true;
						supported_state = true;
						extracted_string.append(line.substring(line.indexOf("\"") + 1,
								line.lastIndexOf("\"")));
						print("LabelString: " + extracted_string);
					} else if (line.contains("\"")) {
						if (in_labelstring) {
							String additional_label = line.substring(line.indexOf("\"") + 1,
									line.lastIndexOf("\""));
							extracted_string.append(additional_label);
							print("AdditionalLabelString: " + additional_label);
						}
					} else if (line.contains("}")) {

						in_labelstring = false;
						in_transition = false;
						if (extracted_string.length() != 0) {
							transitions.add(extracted_string.toString());
						}
						extracted_string.setLength(0);
					}
				} else {
					if (line.contains("transition {")) {
						in_transition = true;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		print('\n' + "TRANSITION READYLIST: " + '\n' + "#############");
		printStringList(transitions);
		return transitions;

	}

	/**
	 * Parses all Strings,contained in list.Because there are two grammars for
	 * states and transitions the type specifier controls the parsers that are
	 * used to parse the Strings in list
	 *
	 * @param list
	 *           the strings ArrayList<String> that should be parsed
	 * @param type
	 *           containing "state" or "transition" to parse the string,otherwise
	 *           nothing is done
	 */
	static ArrayList<String> parse(ArrayList<String> list, String type) {
		Iterator<String> iterator = list.iterator();

		if (type.contains("state")) {

			ArrayList<String> state_errorlist = new ArrayList<String>();

			while (iterator.hasNext()) {
				String current_line = iterator.next();
				try {
					StateflowStateScanner StateScanner = new StateflowStateScanner(new StringReader(
							current_line));
					StateflowStateParser StateParser = new StateflowStateParser(StateScanner);
					Object r = StateParser.parse().value;
				} catch (Exception e) {
					state_errorlist.add(current_line);
				}
			}
			return state_errorlist;
		} else {

			ArrayList<String> transition_errorlist = new ArrayList<String>();

			while (iterator.hasNext()) {
				String current_line = iterator.next();
				try {
					StateflowTransitionScanner TransitionScanner = new StateflowTransitionScanner(
							new StringReader(current_line));
					StateflowTransitionParser TransitionParser = new StateflowTransitionParser(
							TransitionScanner);
					Object r = TransitionParser.parse().value;
				} catch (Exception e) {
					transition_errorlist.add(current_line);
				}
			}
			return transition_errorlist;
		}
	}

	/**
	 * Writes the errorlists with type ArrayList<String> to the output file and
	 * checks if they are empty
	 *
	 * @param states
	 *           state_errorlist ArrayList<String> that should be written
	 * @param transitions
	 *           transition_errorlist ArrayList<String> that should be written
	 * @param filename
	 *           where to store the output file,including the full path
	 */
	static void writeToFile(ArrayList<String> states, ArrayList<String> transitions, String filename) {
		String notparseablestates = "NOT PARSEABLE STATES:";
		String notparseabletransitions = "NOT PARSEABLE TRANSITIONS:";
		ArrayList<String> list = new ArrayList<String>();

		if (!filename.endsWith(".txt")) {
			filename += ".txt";
		}

		if (states.size() == 0) {
			list.add("PARSED MDL FILE(S) CONTAIN NO STATE-PARSE ERRORS");
		} else {
			list.add(notparseablestates);
			list.addAll(states);
		}

		if (transitions.size() == 0) {
			list.add("PARSED MDL FILES CONTAIN NO TRANSITION-PARSE ERRORS");
		} else {
			list.add(notparseabletransitions);
			list.addAll(transitions);
		}

		printToFile(list, filename);
	}

	/**
	 * Writes a ArrayList<String> to an output file that is named via filename
	 *
	 * @param stringlist
	 *           the ArrayList that should be written
	 * @param filename
	 *           where to store the output file,including a relational path+the
	 *           name of the file f.e. foo/foo.mdl
	 */
	static void printToFile(ArrayList<String> stringlist, String filename) {

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			for (String str : stringlist) {
				out.write(str);
				out.newLine();
			}
			print("WROTE OUTPUT FILE TO: " + filename);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prints a ArrayList<String> to the console
	 *
	 * @param stringList
	 *           the ArrayList that should be printed
	 */
	static void printStringList(ArrayList<String> stringlist) {

		Iterator<String> iterator = stringlist.iterator();

		while (iterator.hasNext()) {
			print(iterator.next());
		}
	}

	static void print(String message) {
		if (debug) {
			System.out.println(message);
		}
	}

	static void printError(String message) {
		System.out.println(message);
	}

	static void setDebug(Boolean bool) {
		debug = bool;
	}

	/**
	 * Returns a StringList of all models located in a folder and its subfolders
	 *
	 * @param file
	 *           the folder (file) where the mdl (file)s are (is) situated
	 */
	public static ArrayList<String> findModels(File file) {
		ArrayList<String> rslt = new ArrayList<String>();

		if (file.isDirectory()) {
			File parent = file;
			for (File f : parent.listFiles()) {
				if (f.isDirectory()) {
					if (!f.isHidden() && (f.listFiles() != null)) {
						rslt.addAll(findModels(f));
					}
				} else {
					if (f.getPath().endsWith(".mdl")) {
						rslt.add(f.getPath());
					}
				}
			}
		} else {
			if (file.exists()) {
				rslt.add(file.getPath());
			} else {
				print("ERROR: FILE DOES NOT EXIST");
			}
		}

		return rslt;
	}

	/**
	 * Returns the folderpath of a file or a folder
	 *
	 * @param file
	 *           the file or folder that the path contains
	 */
	public static String getFileFolder(File file) {
		if (file.isDirectory()) {
			return file.getPath();
		} else {
			File parent = file.getParentFile();
			return parent.getPath();
		}
	}

	public static void log(String message, LogType logType) {
		switch (logType) {
			case SEVERE:
				logger.severe(message);
			case WARNING:
				logger.warning(message);
			case INFO:
				logger.info(message);
			case CONFIG:
				logger.config(message);
			case FINE:
				logger.fine(message);
			case FINER:
				logger.finer(message);
			case FINEST:
				logger.finest(message);
			default:
				logger.fine(message);
		}
	}

	public static void createLog(String filename) {
		try {
			FileHandler handler = new FileHandler(filename);
			SimpleFormatter formatter = new SimpleFormatter();
			handler.setFormatter(formatter);
			logger.addHandler(handler);
			logger.setLevel(Level.FINE);
		} catch (Exception e) {
			;
		}

	}

	/**
	 * Starts the parsing process. It can be executed with a gui (no args) or
	 * through console parameter (1 args). Each model is parsed separately.
	 * Occured ParsingErrors are stored in seperate errorlists and written to
	 * output files that are stored in the same path as the input files NOTE that
	 * the parse errors of each model are temporarly stored and cumulatively
	 * written to the output textfiles
	 *
	 * @param args
	 *           - points to the .mdl or a folder containing .mdl files
	 */
	public static void main(String[] args) {

		createLog("StateflowLabelParser.log");

		try {

			ArrayList<String> state_errorlist = new ArrayList<String>();
			ArrayList<String> transition_errorlist = new ArrayList<String>();

			if (args.length == 0) {
				log("STARTED GUI", LogType.FINEST);
				new WrapperGUI();
			} else if ((args.length == 1) || (args.length == 2)) {
				File thefile = new File(args[0]);
				ArrayList<String> model_list = findModels(thefile);

				if (model_list.isEmpty()) {
					log("NO MODELS TO PARSE.CHECK CORRECT MODEL PATH ARGUMENT.", LogType.SEVERE);
				} else {
					print("INCLUDED MODELS TO PARSE: ");
					printStringList(model_list);
					for (String model : model_list) {
						state_errorlist.addAll(parse(readStates(model), "state"));
						transition_errorlist.addAll(parse(readTransitions(model), "transition"));
					}

					if ((args.length == 2)) {
						writeToFile(state_errorlist, transition_errorlist, args[1]);
					} else {
						if (thefile.isDirectory()) {
							writeToFile(state_errorlist, transition_errorlist, (args[0] += "\\output.txt"));
						} else {
							writeToFile(state_errorlist, transition_errorlist, args[0]);
						}
					}
				}
			} else {
				log("ERROR: TOO MANY ARGUMENTS", LogType.SEVERE);
			}
			log("STATEFLOWPARSER EXIT", LogType.FINE);
		}

		catch (Exception e) {
			log(e.getMessage(), LogType.SEVERE);
		}
	}
}
