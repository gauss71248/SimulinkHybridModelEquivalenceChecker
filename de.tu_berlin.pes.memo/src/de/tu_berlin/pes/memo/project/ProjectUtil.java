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

package de.tu_berlin.pes.memo.project;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

public class ProjectUtil {

	public static List<File> getMFilesFromProject(IProject current) throws CoreException {
		String mFiles = current.getPersistentProperty(ProjectNature.MFILES);
		return fileListStringToFiles(mFiles);
	}

	public static List<File> getlibFilesFromProject(IProject current) throws CoreException {
		String customFiles = current.getPersistentProperty(ProjectNature.CUSTOM_LIBRARIES);
		String standardLibraries = MeMoPlugin.getDefault().getPreferenceStore()
				.getString(MeMoPreferenceConstants.STANDARD_LIBRARIES);

		String allLibFiles = "";

		if (standardLibraries != null) {
			allLibFiles += standardLibraries;
		}
		if (customFiles != null) {
			allLibFiles += customFiles;
		}

		return fileListStringToFiles(allLibFiles);
	}

	public static List<File> getRefFilesFromProject(IProject current) throws CoreException {
		String refFiles = current.getPersistentProperty(ProjectNature.MODEL_REFERENCES);
		return fileListStringToFiles(refFiles);
	}

	private static List<File> fileListStringToFiles(String files) {
		List<File> result = new LinkedList<File>();

		if (files != null) {
			String[] filePaths = files.split(File.pathSeparator);

			for (String f : filePaths) {
				if ((f == null) || f.trim().isEmpty()) {
					continue;
				}
				File temp = new File(f);
				if (!temp.exists()) {
					JOptionPane.showMessageDialog(null, f + " does not exist", "Error",
							JOptionPane.ERROR_MESSAGE);
				} else {
					result.add(temp);
				}
			}
		}

		return result;
	}

	public static File getModel(IProject current) throws CoreException {
		IFolder simFolder = current.getFolder("input/simulink/");
		IResource[] simFiles = simFolder.members();

		if (simFiles.length == 1) {
			return new File(simFiles[0].getLocation().toOSString());
		}

		MeMoPlugin.out.println("[Error] Multiple Model files found in \"input/simulink\"");

		return null;

	}

}
