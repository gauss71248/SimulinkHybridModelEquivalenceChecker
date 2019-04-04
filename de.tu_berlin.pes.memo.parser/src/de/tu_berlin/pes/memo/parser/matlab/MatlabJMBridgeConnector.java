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
//		Alexander Reinicke
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

import com.modelengineers.jmbridge.api.MatlabAPI;
import com.modelengineers.jmbridge.api.MatlabConnection;
import com.modelengineers.jmbridge.lg.MatlabException;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.parser.MeMoParserPlugin;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

/**
 * A interface providing access to a running matlab instance. The interface
 * works with the jmbridge connector api.
 *
 * @author vseeker, Joachim Kuhnert
 *
 */
public class MatlabJMBridgeConnector implements MatlabConnector {

	private MatlabAPI _interface;
	private MatlabConnection _conn = null;

	private String _matlabPath;
	private String _connectorPath;
	private boolean _debugActive = false;
	private String _version;
	private boolean _useVersion;

	private String ipAddress = "";
	private int port;
	private int timeout;

	// private static MatlabConnector instance = null;

	public MatlabJMBridgeConnector() {
		_interface = null;
		refreshProperties();
	}

	// public MatlabConnector getMatlabConnector(){
	// if(instance == null) instance = new MatlabJMBridgeConnector();
	// return instance;
	// }

	/**
	 * Starts and connects to a Matlab-Instance
	 */
	@Override
	public boolean connect() {

		return establishMatlabConnection() && startMatlabInstance(_useVersion);
	}

	/**
	 * Indicates whether a matlab connection is existing or not.
	 *
	 * @return true if the interface is connected to matlab.
	 */
	@Override
	public boolean getIsConnected() {
		if (_conn == null) {
			return false;
		}

		return _conn.isConnected();
	}

	/**
	 * Updates the matlab properties from the
	 * <code>MatlabConnectorProperties</code> class.
	 *
	 * @see de.tu_berlin.pes.memo.conceptparser.util.MatlabConnectorProperties
	 */
	@Override
	public void refreshProperties() {
		ipAddress = MeMoPlugin.getDefault().getPreferenceStore()
				.getString(MeMoPreferenceConstants.PR_MATLAB_IP);
		port = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_PORT);
		timeout = MeMoPlugin.getDefault().getPreferenceStore()
				.getInt(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT);
		_connectorPath = MeMoParserPlugin.getDefault().getJmbridgePath().toString();
		_matlabPath = MeMoPlugin.getDefault().getPreferenceStore()
				.getString(MeMoPreferenceConstants.PR_MATLAB_PATH);
		_version = "";
		_useVersion = false;
	}

	/**
	 * Starts a new Matlab instance
	 * 
	 * @return true if successful.
	 */
	public boolean startMatlabInstance(boolean useVersion) {
		boolean started = false;
		try {

			_matlabPath = _matlabPath.endsWith("\\") ? _matlabPath : _matlabPath + "\\";
			// _connectorPath = _connectorPath.endsWith("\\") ? _connectorPath :
			// _connectorPath + "\\";

			if (timeout < 100) {
				timeout *= 1000;
			}
			started = (useVersion) ? _conn.startMatlabByVersionAndConnect(_version, _connectorPath,
					timeout) : _conn.startMatlabByPathAndConnect(_matlabPath, _connectorPath, timeout);
		} catch (Exception e) {
			String err = "Could not start matlab successfully.";
			MeMoPlugin.err.println(err);
			MeMoPlugin.logException(err, e);
		}
		return started;
	}

	/**
	 * Establishes a connection to a running matlab instance.
	 *
	 * @return true if a connection process could be executed successfully.
	 */
	public boolean establishMatlabConnection() {
		boolean started = false;
		try {
			if (_conn == null) {
				_conn = new MatlabConnection(ipAddress, port);
			}

			started = _conn.isConnected();
		} catch (Exception e) {
			String err = "Could not start and connect to matlab successfully.";
			MeMoPlugin.err.println(err);
			MeMoPlugin.logException(err, e);
		}

		return started;
	}

	/**
	 * Release an existing matlab connection.
	 *
	 * @param disconnectAndClose
	 *           if false, matlab is not closed after disconnect
	 * @throws MatlabException
	 */
	@Override
	public void releaseConnection(boolean disconnectAndClose) throws MatlabException {
		if (disconnectAndClose) {
			_conn.closeMatlab();
		}
		_conn.disconnect();
	}

	/**
	 * Evaluates a given matlab command. There are no return values expected for
	 * this command.
	 *
	 * @param command
	 *           the command to be evaluated.
	 */
	@Override
	public boolean eval(String command) {
		boolean success = true;

		if (_interface == null) {
			_interface = new MatlabAPI(_conn);
		}

		if (_debugActive) {
			MeMoPlugin.out.println("[DEBUG] Evaluate command:" + command);
		}

		if (!getIsConnected()) {
			MeMoPlugin.err.println("No matlab connection.");
			return false;
		}

		try {
			_interface.evalInMatlab(command, 0);
		} catch (MatlabException e) {
			String err = "An internal matlab error occured while evaluating command: " + command;
			MeMoPlugin.err.println(err);
			MeMoPlugin.logException(err, e);
			success = false;
		}

		return success;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.tu_berlin.pes.memo.parser.matlab.MatlabConnector#getVariable(java.lang
	 * .String)
	 */
	@Override
	public Object[] getVariable(String name) {
		try {
			return _interface.getVariable(name);
		} catch (MatlabException e) {
			String err = "An internal matlab error occured while evaluating command: get var " + name;
			MeMoPlugin.err.println(err);
			MeMoPlugin.logException(err, e);
		}
		return null;
	}

	/**
	 * Activates or deactivates the debug mode for the matlab interface.
	 *
	 * @param activateDebug
	 *           state to be set for the debug mode.
	 */
	@Override
	public void debug(boolean activateDebug) {
		_debugActive = activateDebug;

	}

	// /**
	// *
	// * Evaluates a matlab command with return values.
	// *
	// * @param command
	// * the command to be evaluated
	// * @param returnCount
	// * the number of expected return values
	// * @return the return value object
	// */
	// public Object returningEval(String command, int returnCount) {
	// Object[] result = null;
	// if(_interface == null)
	// _interface = new MatlabAPI(_conn);
	//
	// if(_debugActive)
	// MeMoPlugin.out.print("[DEBUG] Evaluate command:" + command);
	//
	// if(!getIsConnected()){
	// MeMoPlugin.err.println("No matlab connection.");
	// return false;
	// }
	//
	// try {
	// result = _interface.evalInMatlab( command, returnCount);
	// } catch (MatlabException e) {
	// MeMoPlugin.err.println("An internal matlab error occured while evaluating command: "+
	// command);
	// MeMoPlugin.logException(e.toString(), e);
	// }
	//
	// return result[0];
	// }
	//
	//
	// /**
	// * Disconnects and closes MATLAB
	// */
	// public void close() {
	// try {
	// _conn.disconnect();
	// _conn.closeMatlab();
	// } catch (MatlabException e) {
	// MeMoPlugin.logException(e.toString(), e);
	// }
	// _conn = null;
	// _interface = null;
	//
	// }
	//
	// /**
	// * @return the MatlabAPI of this connector
	// */
	// public MatlabAPI getMatlabAPI() {
	// return _interface;
	// }
	//
	// /**
	// * @return the SimulinkAPI
	// */
	// public SimulinkAPI getSLAPI() {
	// if(_conn.getSimulinkAPI() == null)
	// new SimulinkAPI(_conn);
	//
	// return _conn.getSimulinkAPI();
	// }
	//
	// public void addMatlabInterfacePacketListener(
	// MatlabConnectionPacketListener matlabConnectionPacketListener) {
	// _conn.addMatlabInterfacePacketListener(matlabConnectionPacketListener);

	// }

}
