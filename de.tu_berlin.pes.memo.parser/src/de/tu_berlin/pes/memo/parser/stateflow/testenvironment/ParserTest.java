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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import de.tu_berlin.pes.memo.parser.stateflow.stateflowparser.StateflowStateParser;
import de.tu_berlin.pes.memo.parser.stateflow.stateflowparser.StateflowStateScanner;
import de.tu_berlin.pes.memo.parser.stateflow.stateflowparser.StateflowTransitionParser;
import de.tu_berlin.pes.memo.parser.stateflow.stateflowparser.StateflowTransitionScanner;

/**
 * Test environment for the generated cup parsers.
 */
public class ParserTest {

	// static void testStrings() {
	//
	// String[] testcases = { "O2_normal\nentry: fail_state[O2] = 0;exit: 1 = 2"
	// };
	//
	// System.out.println("RESULT: PARSED-STRINGS" + '\n'
	// + "-------------------------");
	//
	// for (int i = 0; i < testcases.length; i++) {
	// StateflowScanner scanner = new StateflowScanner(new StringReader(
	// testcases[i]));
	// scanner.setDebug(true);
	// StateflowParser parser = new StateflowParser(scanner);
	//
	// try {
	// System.out.println("TRY TO PARSE STRING: " + '\"'
	// + testcases[i] + '\"');
	// int iter = 0;
	// ArrayList<State> l = (ArrayList<State>) parser.parse().value;
	// for (State r : l) {
	// System.out.println("State:" + i + " Name: " + r.name);
	// iter++;
	// }
	// } catch (Exception e) {
	// System.err.println("Error parsing string: " + testcases[i]);
	// e.printStackTrace();
	// }
	// }
	// }

	static HashMap<Integer, String> testInputFile(String filename) {

		String strLine = null;
		int linecounter = 0;
		HashMap<Integer, String> errors = new HashMap<Integer, String>(); // Line
																								// ->
																								// String
																								// failed
																								// to
																								// parse

		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			while ((strLine = br.readLine()) != null) {
				linecounter++;

				if (strLine.startsWith("%")) {
					continue;
				}

				StateflowStateScanner sScanner = new StateflowStateScanner(new StringReader(strLine));
				sScanner.setDebug(true);

				StateflowTransitionScanner tScanner = new StateflowTransitionScanner(new StringReader(
						strLine));
				tScanner.setDebug(true);

				try {
					if (filename.contains("transitions")) {
						StateflowTransitionParser parser = new StateflowTransitionParser(tScanner);
						Object r = parser.parse().value;
						System.out.println("TRANSITION PARSED");
					} else {
						StateflowStateParser parser = new StateflowStateParser(sScanner);
						Object r = parser.parse().value;
						System.out.println("STATE PARSED");
					}
				} catch (java.lang.Exception e) {
					System.out.println("[ERROR] Parsing file " + filename + " in line: " + linecounter
							+ " --> \"" + strLine + "\"");
					errors.put(linecounter, strLine);
					// System.err.println("[ERROR] Parsing file " + filename +
					// " in line: " + linecounter + " --> \"" + strLine + "\"");
					// e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e1) {
			System.err.println("Error opening file: " + filename);
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return errors;
	}

	public static void main(String[] args) {

		System.out.println("RESULTS STATEFLOW-GRAMMAR" + '\n' + "-------------------------");

		// testStrings();

		// String testfile = "parserTestInput/statesActualCase.txt";
		String testfile = "parserTestInput/states.txt";
		// String testfile = "parserTestInput/transitionsActualCase.txt";
		// String testfile = "parserTestInput/transitions.txt";
		// String testfile = "parserTestInput/general_test.txt";

		System.out.println("PARSERRESULT - InputFile: " + testfile);

		HashMap<Integer, String> errors = testInputFile(testfile);

		if (!errors.isEmpty()) {
			System.out.println("Following " + errors.size() + " Strings failed to parse");
			for (String s : errors.values()) {
				System.out.println(s);
			}
		} else {
			System.out.println("Congrats! All Strings parsed!");
		}
	}
}
