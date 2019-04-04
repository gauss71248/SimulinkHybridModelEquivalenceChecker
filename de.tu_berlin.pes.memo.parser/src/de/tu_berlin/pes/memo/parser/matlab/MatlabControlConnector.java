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
import java.io.IOException;
import java.net.URL;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;

import org.eclipse.core.runtime.FileLocator;

import com.modelengineers.jmbridge.lg.MatlabException;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.parser.MeMoParserPlugin;
import de.tu_berlin.pes.memo.parser.StandalonePreferenceStore;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

/**
 * An interface providing access to a running matlab instance. The interface
 * works with the matlab connector api.
 *
 * @author vseeker, Joachim Kuhnert, Alexander Reinicke
 *
 */
public class MatlabControlConnector implements MatlabConnector {

	private MatlabProxyFactory _proxyFactory = null;
	private MatlabProxy _proxy = null;

	private String _matlabPath;
	private boolean _debugActive = false;
	private Integer port = null;
	private Integer timeout = null;
	private String _matlabcontrolPath;
	private MatlabProxyFactoryOptions.Builder mpfOptionsBuilder = null;

	private String matlabExecutable;

	// private static MatlabConnector instance = null;

	private MatlabControlConnector() {
	};

	public void set_matlabPath(String _matlabPath) {
		this._matlabPath = _matlabPath;
	}
	
	public void setPort(Integer port) {
		this.port = port;
	}

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public MatlabControlConnector(boolean useExistingSession) {

		// System.out.println(File.separator);

		if (System.getProperty("os.name").startsWith("Windows")) {
			// includes: Windows 2000, Windows 95, Windows 98, Windows NT, Windows
			// Vista, Windows XP
			this.matlabExecutable = "matlab.exe";
		} else {
			// everything else
			this.matlabExecutable = "matlab";
		}

		refreshProperties();
		if(_matlabPath == null || port == null || timeout == null) {
			if(!StandalonePreferenceStore.getInstance().checkMatlab()) {
				throw new RuntimeException("Your DB configuration is incomplete");
			}
			_matlabPath = StandalonePreferenceStore.getInstance().getMatlabpath();
			port = StandalonePreferenceStore.getInstance().getPort();
			timeout = StandalonePreferenceStore.getInstance().getTimeout();
		}
		_matlabPath = _matlabPath.endsWith(File.separator) ? _matlabPath : _matlabPath
				+ File.separator;
		// MatlabProxyFactoryOptions.Builder mpfOptionsBuilder = new
		// MatlabProxyFactoryOptions.Builder();
		mpfOptionsBuilder = new MatlabProxyFactoryOptions.Builder();
		mpfOptionsBuilder.setPort(port);
		mpfOptionsBuilder.setProxyTimeout(timeout * 10000); /*
																			 * matlab control uses
																			 * ms
																			 */
		mpfOptionsBuilder.setMatlabLocation(_matlabPath + this.matlabExecutable);
		mpfOptionsBuilder.setUsePreviouslyControlledSession(useExistingSession);

		_proxyFactory = new MatlabProxyFactory(mpfOptionsBuilder.build());
	}

	// public MatlabConnector getMatlabConnector(){
	// if(instance == null) instance = new MatlabControlConnector();
	// return instance;
	// }

	/**
	 * Updates the matlab properties from the
	 * <code>MatlabConnectorProperties</code> class.
	 *
	 * @see de.tu_berlin.pes.memo.conceptparser.util.MatlabConnectorProperties
	 */
	@Override
	public void refreshProperties() {
		if(MeMoPlugin.getDefault() != null) {
			port = MeMoPlugin.getDefault().getPreferenceStore()
					.getInt(MeMoPreferenceConstants.PR_MATLAB_PORT);
			timeout = MeMoPlugin.getDefault().getPreferenceStore()
					.getInt(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT);
			_matlabcontrolPath = MeMoParserPlugin.getDefault().getJmbridgePath();
	   }
		String cp = System.getProperty("java.class.path");
		String test;
		try {
			if(MeMoPlugin.getDefault() != null) {
				test = new File(FileLocator.toFileURL(MeMoParserPlugin.getDefault().getBundle()
						.getEntry("/lib/matlabcontrol-4.1.0.jar")).getFile()).toString();
			} else {
				//TODO set URL here right
				test = StandalonePreferenceStore.getInstance().getMatlabcontrolpath();
			}
			System.out.println(test);
			if (!cp.contains(test)) {
				cp = cp + System.getProperty("path.separator") + test;
				System.setProperty("java.class.path", cp);
			}
		} catch (IOException e) {
			MeMoPlugin.logException(e.toString(), e);
		}
		if(MeMoPlugin.getDefault() != null) {
			_matlabPath = MeMoPlugin.getDefault().getPreferenceStore()
					.getString(MeMoPreferenceConstants.PR_MATLAB_PATH);
		}
	}

	/**
	 * Connect to a MatlabSession (if exist) or create new one if not
	 *
	 * @return true if Matlabconnection is established successfully
	 * @throws MatlabConnectionException
	 */

	@Override
	public boolean connect() throws MatlabConnectionException {
		boolean started = false;
		// try {
		if (_proxyFactory == null) {
			_proxyFactory = new MatlabProxyFactory(mpfOptionsBuilder.build());
		}
		if (_proxy == null) {
			_proxy = _proxyFactory.getProxy(); // requesting/getting a new proxy
			// will automatically start a
			// new ML-session, otherwise
			// connection to existing
			// ML-session will be
			// established
		}
		started = _proxy.isConnected();
		// } catch (Exception e) {
		// MeMoPlugin.err.println("Could not start matlab successfully.");
		// MeMoPlugin.logException(e.toString(), e);
		//
		// }
		return started;
	}

	/**
	 * Indicates whether a matlab connection is existing or not.
	 *
	 * @return true if the proxy is connected to matlab.
	 */
	@Override
	public boolean getIsConnected() {
		if (_proxy == null) {
			return false;
		}

		return _proxy.isConnected();
	}

	/**
	 * Release an existing matlab connection.
	 *
	 * @param disconnectAndClose
	 *           if false, matlab is not closed after disconnect
	 * @throws MatlabException
	 */
	@Override
	public void releaseConnection(boolean disconnectAndClose) throws MatlabInvocationException {
		if (disconnectAndClose) {
			_proxy.exit();
		}
		_proxy.disconnect();
		// else _proxy.disconnect();
		_proxy = null;
		// _proxyFactory = null;
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
		try {
			if (_proxy == null) {
				connect();
			}

			if (_debugActive) {
				MeMoPlugin.out.println("[DEBUG] Evaluate command:" + command);
			}

			if (!getIsConnected()) {
				MeMoPlugin.err.println("No matlab connection.");
				return false;
			}

			_proxy.eval(command);
		} catch (Exception e) {
			String err = "An internal matlab error occured while evaluating command: " + command;
			MeMoPlugin.logException(err, e);
			success = false;
		}

		return success;
	}

	/**
	 * provides access to Matlab-variables
	 */
	@Override
	public Object getVariable(String name) {
		try {
			return _proxy.getVariable(name);
		} catch (MatlabInvocationException e) {
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

}
