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

package de.tu_berlin.pes.memo;

import java.io.PrintStream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import de.tu_berlin.pes.memo.MeMoPlugin.LogLevel;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

/**
 * The activator class controls the plug-in life cycle
 */
public class MeMoPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "de.tu_berlin.pes.memo";
	
	public enum LogLevel {
		info, warning, error, success, attention
	}

	public static PrintStream out = System.out;
	public static PrintStream err = System.err;
	private static MeMoLogInterface meMoLog;
	private static IProject currentProject;

	// The shared instance
	/**
	 * @uml.property name="plugin"
	 * @uml.associationEnd
	 */
	private static MeMoPlugin plugin;

	/**
	 * The constructor
	 */
	public MeMoPlugin() {
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
		MeMoOutput.init();
		this.getWorkbench().getActiveWorkbenchWindow().getSelectionService()
		.addSelectionListener(new ISelectionListener() {

			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (selection instanceof IStructuredSelection) {
					Object first = ((IStructuredSelection) selection).getFirstElement();
					if (first instanceof IResource) {
						IResource res = (IResource) first;
						IProject project = res.getProject();
						MeMoPlugin.setCurrentProject(project);
					}
				}

			}
		});

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
		// plugin = null; // Don't set to null. Race conditions!
		// Can cause nullpointer exceptions if stop()
		// in other plugins access getDefault().
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static MeMoPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *           the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	/**
	 * Set the new output stream.
	 *
	 * @param out
	 *           The new output stream.
	 */
	public static void setOut(PrintStream out) {
		MeMoPlugin.out = out;
	}

	/**
	 * Set the error stream.
	 *
	 * @param err
	 *           The new error stream.
	 */
	public static void setErr(PrintStream err) {
		MeMoPlugin.err = err;
	}

	/**
	 * Set the log for exception logging.
	 *
	 * @param meMoLog
	 *           The new exception logger.
	 */
	public static void setMeMoLog(MeMoLogInterface meMoLog) {
		MeMoPlugin.meMoLog = meMoLog;
	}

	/**
	 * Logs a exception.
	 *
	 * @param msg
	 *           The error message or description to log.
	 * @param e
	 *           The exception to log.
	 */
	public static void logException(String msg, Exception e) {
		if (meMoLog != null) {
			meMoLog.logException(getDefault().getLog(), PLUGIN_ID, msg, e);
		} else if(getDefault() != null) {
			getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, IStatus.OK, msg, e));
		}

		if (getDefault() != null && MeMoPlugin.getDefault().getPreferenceStore()
				.getBoolean(MeMoPreferenceConstants.DEBUG_MODE)) {
			e.printStackTrace(err);
		}
	}

	/**
	 * @return the currentProject
	 */
	public static IProject getCurrentProject() {
		return currentProject;
	}

	/**
	 * @param currentProject
	 *           the currentProject to set
	 */
	public static void setCurrentProject(IProject currentProject) {
		MeMoPlugin.currentProject = currentProject;
	}

	public static void warn(String message) {
		MeMoPlugin.out.println("[WARNING:]   " + message);

	}

	public static void log(Object obj, LogLevel level) {
		switch (level) {
		case info:
		default:
			MeMoPlugin.out.println(obj.toString());
		}
	}

}
