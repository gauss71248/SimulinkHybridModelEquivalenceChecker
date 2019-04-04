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

/*
 * see http://cvalcarcel.wordpress.com/2009/07/11/writing-an-eclipse-plug-in-part-2-creating-a-custom-project-in-eclipse-adding-to-the-new-project-wizard/
 * and following
 * these were used as tutorial
 */
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public class ProjectNature implements IProjectNature {

	public static final String NATURE_ID = "de.tu_berlin.pes.memo.project.projectNature";

	public static final QualifiedName ACTIVEDATABASE = new QualifiedName("active", "database");
	public static final QualifiedName DATABASES = new QualifiedName("all", "database");

	public static final String LIST_QUALIFIER = "lists";

	public static final QualifiedName CUSTOM_LIBRARIES = new QualifiedName(LIST_QUALIFIER,
			"CustomLibraries");

	public static final QualifiedName MFILES = new QualifiedName(LIST_QUALIFIER, "MFiles");

	public static final QualifiedName MODEL_REFERENCES = new QualifiedName(LIST_QUALIFIER,
			"ModelRefs");

	public static final String DOT_OUTPUT_PATH = "output/dot";
	public static final String SLICESCRIPT_OUTPUT_PATH = "output/slicescripts";

	public static final String BOOGIE_OUTPUT_PATH = "output/boogie";
	public static final String BOOGIE_INPUT_PATH = "input/boogie";

	@Override
	public void configure() throws CoreException {
		// NOP

	}

	@Override
	public void deconfigure() throws CoreException {
		// NOP

	}

	@Override
	public IProject getProject() {
		// NOP?
		return null;
	}

	@Override
	public void setProject(IProject project) {
		// NOP?

	}

}
