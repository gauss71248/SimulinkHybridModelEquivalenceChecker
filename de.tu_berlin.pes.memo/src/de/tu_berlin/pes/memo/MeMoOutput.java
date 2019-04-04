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

package de.tu_berlin.pes.memo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;

/**
 * Defines standard input and output streams for the project. Used to deliver
 * messages to the eclispe console.
 *
 * @author Joachim Kuhnert
 *
 */
public class MeMoOutput implements MeMoLogInterface {

	boolean debugThreadActive = false;
	static IOConsole myConsole = findConsole();
	static IOConsoleOutputStream console = myConsole.newOutputStream();
	public final static String CONSOLE_NAME = "Test";

	private static MeMoOutput instance = new MeMoOutput();

	public static void init() {
		instance.createOuputStreams();
		MeMoPlugin.setMeMoLog(instance);
		console.setActivateOnWrite(true);
	}

	private void printErr(final String msg) {
		System.err.print(msg);
	}

	private void createOuputStreams() {

		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				console.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				console.write(b, off, len);
			}

			@Override
			public void write(byte[] b) throws IOException {
				console.write(b, 0, b.length);
			}
		};

		OutputStream err = new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				String str = String.valueOf((char) b);
				printErr(str);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				String str = new String(b, off, len);
				printErr(str);
			}

			@Override
			public void write(byte[] b) throws IOException {
				console.write(b, 0, b.length);
			}
		};

		MeMoPlugin.setOut(new PrintStream(out, true));
		MeMoPlugin.setErr(new PrintStream(err, true));
	}

	@Override
	public void logException(ILog log, String PLUGIN_ID, String msg, Exception e) {
		log.log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, msg, e));
		e.printStackTrace(System.err);

	}

	public static MessageConsole findConsole() {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (IConsole element : existing) {
			if (CONSOLE_NAME.equals(element.getName())) {
				return (MessageConsole) element;
			}
		}
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(CONSOLE_NAME, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

}
