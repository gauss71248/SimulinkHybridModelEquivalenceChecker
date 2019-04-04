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
import java.io.IOException;
import java.net.URI;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import de.tu_berlin.pes.memo.MeMoPlugin;

/*
 * see http://cvalcarcel.wordpress.com/2009/07/11/writing-an-eclipse-plug-in-part-2-creating-a-custom-project-in-eclipse-adding-to-the-new-project-wizard/
 * and following
 * these were used as tutorial
 */
public class MeMoProjectSupport {

	public static IProject createProject(String projectName, URI location, File slModel,
			File boogieBib) {

		if (projectName != null) {
			IProject project = createBaseProject(projectName, location);
			try {
				addNature(project);
				String[] paths = { "output/diffResults", "output/smoke", "output/boogie",
						"input/simulink", "input/boogie", ProjectNature.DOT_OUTPUT_PATH,
						ProjectNature.SLICESCRIPT_OUTPUT_PATH };
				addToProjectStructure(project, paths);
			} catch (CoreException e) {
				e.printStackTrace();
				project = null;
			}

			// create a link to the original model:
			try {
				String full = slModel.getCanonicalPath();
				String modelFileStr = slModel.getName();
				IPath slPathLocation = new Path(full);
				IFile link = project.getFile("input/simulink/" + modelFileStr);
				if (project.getWorkspace().validateLinkLocation(link, slPathLocation).isOK()) {
					link.createLink(slPathLocation, IResource.NONE, null);
				} else {
					// invalid location, throw an exception or warn user
				}
			} catch (IOException e) {
				MeMoPlugin.logException(e.toString(), e);
			} catch (CoreException e) {
				MeMoPlugin.logException(e.toString(), e);
			}

			return project;
		}
		return null;
	}

	private static IProject createBaseProject(String projectName, URI location) {

		IProject newProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);

		if (!newProject.exists()) {
			URI projectLocation = location;
			IProjectDescription desc = newProject.getWorkspace().newProjectDescription(
					newProject.getName());
			if ((location != null)
					&& ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(location)) {
				projectLocation = null;
			}

			desc.setLocationURI(projectLocation);
			try {
				newProject.create(desc, null);
				if (!newProject.isOpen()) {
					newProject.open(null);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		return newProject;
	}

	private static void addToProjectStructure(IProject newProject, String[] paths)
			throws CoreException {
		for (String path : paths) {
			IFolder etcFolders = newProject.getFolder(path);
			createFolder(etcFolders);
		}
	}

	private static void createFolder(IFolder folder) throws CoreException {
		IContainer parent = folder.getParent();
		if (parent instanceof IFolder) {
			createFolder((IFolder) parent);
		}
		if (!folder.exists()) {
			folder.create(false, true, null);
		}
	}

	private static void addNature(IProject project) throws CoreException {
		if (!project.hasNature(ProjectNature.NATURE_ID)) {
			IProjectDescription description = project.getDescription();
			String[] prevNatures = description.getNatureIds();
			String[] newNatures = new String[prevNatures.length + 1];
			System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			newNatures[prevNatures.length] = ProjectNature.NATURE_ID;
			description.setNatureIds(newNatures);

			IProgressMonitor monitor = null;
			project.setDescription(description, monitor);
		}
	}

}
