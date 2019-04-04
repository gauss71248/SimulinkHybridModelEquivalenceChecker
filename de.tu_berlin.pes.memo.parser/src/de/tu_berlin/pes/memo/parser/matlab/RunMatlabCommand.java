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

import de.tu_berlin.pes.memo.MeMoPlugin;

/**
 * This class wrapped the execution of a MatlabConnector command in a thread, so
 * it does not block the entire program, if the jmbridge throws an exception in
 * Matlab. You could build a timeout around this thread or use
 * <code>waitForResult</code> or <code>executeCommand</code>. The thread starts
 * automatically, if a command is set. If the thread terminates successful it
 * stores the result, if there is one returned.
 * 
 * @author Joachim Kuhnert
 */
public class RunMatlabCommand extends Thread {

	/**
	 * The different matlab commands implemented.
	 */
	public static enum Command {
		/**
		 * @uml.property name="eval"
		 * @uml.associationEnd
		 */
		eval, /**
		 * @uml.property name="getVariable"
		 * @uml.associationEnd
		 */
		getVariable
	}

	/**
	 * Has the thread terminated successfully.
	 * 
	 * @uml.property name="successful"
	 */
	private boolean successful = false;
	/**
	 * Only set a command one time. If a command is set, the thread starts and
	 * the command shall not be changed.
	 */
	private boolean isCommandSet = false;
	/**
	 * Stores the matlab command type.
	 * 
	 * @uml.property name="commandToExecute"
	 * @uml.associationEnd
	 */
	private Command commandToExecute;
	/**
	 * Stores the parameter of the command.
	 */
	private String parameter;
	/**
	 * Stores the reuslt if one returned.
	 * 
	 * @uml.property name="result"
	 */
	private Object result = null;
	/**
	 * The time when the thread began to work
	 * 
	 * @uml.property name="startTime"
	 */
	private long startTime = Long.MAX_VALUE; // set to max to handle race
															// conditions in waitForResult
	/**
	 * The connection to MATLAB
	 * 
	 * @uml.property name="matlab"
	 * @uml.associationEnd
	 */
	private MatlabConnector matlab;

	// We need a MatlabConnector!
	@SuppressWarnings("unused")
	private RunMatlabCommand() {
	}

	/**
	 * Creates a new Thread to run a MatlabConector command
	 *
	 * @param matlab
	 *           the MatlabConector to execute the command
	 */
	public RunMatlabCommand(MatlabConnector matlab) {
		this.matlab = matlab;
		this.setDaemon(true);
	}

	/**
	 * @return If the command was successfully executed. It could be false if the
	 *         command returns false or if the command is not finished yet.
	 * @uml.property name="successful"
	 */
	public boolean isSuccessful() {
		return successful;
	}

	/**
	 * @return The result returned by the command, if it isn't an boolean result.
	 * @uml.property name="result"
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * Executes a command. Therefore it sets the command parameters and starts
	 * the thread.
	 *
	 * @param command
	 *           The command type
	 * @param param
	 *           The parameter of the command
	 * @return True, if the parameter is set and the thread is started, false if
	 *         a parameter is already set.
	 */
	public boolean setCommand(Command command, String param) {
		if (isCommandSet) {
			return false;
		}

		commandToExecute = command;
		parameter = param;
		successful = false;
		startTime = System.currentTimeMillis();
		this.start();
		return true;

	}

	/**
	 * Executes a command. Therefore it creates the thread, sets the command
	 * parameters, starts the thread and returns the thread object if it is
	 * successful or the timeout is reached.
	 *
	 * @param matlab
	 *           The MatlabConector to execute the command
	 * @param command
	 *           The command type
	 * @param param
	 *           The parameter of the command
	 * @param timeout
	 *           The maximal time to wait for the result in milliseconds
	 * @return The thread object used to run the command. If the command created
	 *         a result, it is stored in this object.
	 */
	public static RunMatlabCommand executeCommand(MatlabConnector matlab, Command command,
			String param, int timeout) {
		RunMatlabCommand thread = new RunMatlabCommand(matlab);
		thread.setCommand(command, param);
		thread.waitForResult(timeout);
		return thread;
	}

	@Override
	public void run() {
		try {
			successful = false;
			isCommandSet = true;
			switch (commandToExecute) {
				case eval:
					successful = matlab.eval(parameter);
					break;
				case getVariable:
					result = matlab.getVariable(parameter);
					successful = result == null ? false : true;
					break;
			}
		} catch (ThreadDeath e) {
		}
	}

	/**
	 * Executes con.eval(parameter) in a Thread. If the the command fails it
	 * tries it as often as given.
	 *
	 * @param parameter
	 *           The parameter of the command
	 * @param con
	 *           The MatlabConector to execute the command
	 * @param tries
	 *           The maximal count of tries
	 * @param timeout
	 *           The maximal time to wait for the result in milliseconds
	 * @return
	 */
	public static boolean runEvalCommand(String parameter, MatlabConnector con, int tries,
			int timeout) {
		// double starttime;
		RunMatlabCommand thread;
		while (tries > 0) {
			thread = executeCommand(con, RunMatlabCommand.Command.eval, parameter, timeout);
			tries--; // tries unbedingt runter z�hlen bevor das continue gemacht
						// wird!!!
			if (!thread.isSuccessful()) {
				// thread.stop(); ignore running thread
				continue;
			}
			if (thread.isSuccessful()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Executes con.getVariable(parameter) in a Thread. If the the command fails
	 * it tries it as often as given.
	 *
	 * @param parameter
	 *           The parameter of the command
	 * @param con
	 *           The MatlabConector to execute the command
	 * @param tries
	 *           The maximal count of tries
	 * @param timeout
	 *           The maximal time to wait for the result in milliseconds
	 * @return
	 */
	public static RunMatlabCommand runGetVarCommand(String parameter, MatlabConnector con,
			int tries, int timeout) {
		// double starttime;
		RunMatlabCommand thread = null;
		while (tries > 0) {
			thread = executeCommand(con, RunMatlabCommand.Command.getVariable, parameter, timeout);
			tries--;
			if (!thread.isSuccessful()) {
				// thread.stop(); ignore running thread
				continue;
			}

			if (thread.isSuccessful()) {
				return thread;
			}
		}

		return thread;

	}

	/**
	 * A timeout using the internal stored start time.
	 *
	 * @param timeout
	 */
	public void waitForResult(int timeout) {
		while (((System.currentTimeMillis() - startTime) < timeout) && !isSuccessful()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				MeMoPlugin.logException(e.toString(), e);
			}
		}
	}

	/**
	 * @return the time the thread starts running
	 * @uml.property name="startTime"
	 */
	public long getStartTime() {
		return startTime;
	}

}
