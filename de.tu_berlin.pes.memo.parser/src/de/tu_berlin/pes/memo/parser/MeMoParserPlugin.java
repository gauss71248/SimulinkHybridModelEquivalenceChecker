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
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;

import matlabcontrol.PermissiveSecurityManager;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.parser.matlab.MeMoMatlabManager;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

/**
 * The activator class controls the plug-in life cycle
 */
public class MeMoParserPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "de_tu_berlin.pes.memo.parser";

	// The shared instance
	/**
	 * @uml.property name="plugin"
	 * @uml.associationEnd
	 */
	private static MeMoParserPlugin plugin;

	/**
	 * @uml.property name="jmbridgePath"
	 */
	private String jmbridgePath;

	/**
	 * The constructor
	 */
	public MeMoParserPlugin() {
		//System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
		//System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
	}

	/**
	 * @return
	 * @uml.property name="jmbridgePath"
	 */
	public String getJmbridgePath() {
		return jmbridgePath;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		URL u = FileLocator.toFileURL(getBundle().getEntry("/lib/jmbridge_dist/"));
		jmbridgePath = new File(u.getFile()).toString();

		// make RMI work
		System.setProperty(
				"java.rmi.server.codebase",
				FileLocator.toFileURL(
						MeMoParserPlugin.getDefault().getBundle()
						.getEntry("/lib/matlabcontrol-4.1.0.jar")).toString());
		System.setSecurityManager(new PermissiveSecurityManager());

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		if(MeMoPlugin.getDefault() != null) {
			MeMoMatlabManager.diconnect(!MeMoPlugin.getDefault().getPreferenceStore()
					.getBoolean(MeMoPreferenceConstants.CLOSE_MATLAB_WITHOUT_ASKING));
		}
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MeMoParserPlugin getDefault() {
		return plugin;
	}

}
