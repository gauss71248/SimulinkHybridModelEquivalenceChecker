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
import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class MeMoNewProjectWizard extends Wizard implements INewWizard, IExecutableExtension {

	private MeMoProjectWizardPage _pageOne;

	private IConfigurationElement _configurationElement;

	private final String WIZARD_NAME = "MeMo Project";
	private final String PAGE_NAME = "New MeMo Project";
	private final String WIZARD_DESC = "Create a new MeMo Project";

	public MeMoNewProjectWizard() {
		setWindowTitle(WIZARD_NAME);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// NOP

	}

	@Override
	public boolean performFinish() {
		String name = _pageOne.getProjectName();
		URI location = null;
		File slModel = new File(_pageOne.getModelLocation());
		File boogieBib = null;
		if (!_pageOne.useDefaults()) {
			location = _pageOne.getLocationURI();
		} // else location == null

		MeMoProjectSupport.createProject(name, location, slModel, boogieBib);

		BasicNewProjectResourceWizard.updatePerspective(_configurationElement);

		return true;
	}

	@Override
	public void addPages() {
		super.addPages();

		_pageOne = new MeMoProjectWizardPage(PAGE_NAME);

		_pageOne.setTitle(PAGE_NAME);
		_pageOne.setDescription(WIZARD_DESC);

		addPage(_pageOne);

	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {

		_configurationElement = config;

	}
}
