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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

public class MeMoProjectWizardPage extends WizardNewProjectCreationPage {

	Text sLModelText = null;
	Composite importGroup;

	public MeMoProjectWizardPage(String pageName) {
		super(pageName);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		Composite newComposite = new Composite((Composite) getControl(), SWT.None);
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(newComposite, IIDEHelpContextIds.NEW_PROJECT_WIZARD_PAGE);

		newComposite.setLayout(new GridLayout());
		newComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		createImportGroup(newComposite);

		setPageComplete(validatePage());
		// Show description on opening
		setErrorMessage(null);
		setMessage(null);
		setControl(newComposite);
	}

	private final void createImportGroup(Composite parent) {
		importGroup = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		importGroup.setLayout(layout);
		importGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		createSimulinkModelChooserPart(importGroup);

		// Label BBCLabel = new Label(importGroup, SWT.NONE);
		// BBCLabel.setText("Use default boogieBib");
		// BBCLabel.setFont(parent.getFont());

		// BBC = new BooleanFieldEditor("USE_DEFAULT_BOOGIEBIB",
		// "Use default boogieBib", parent);

		// Label BBLabel = new Label(importGroup, SWT.NONE);
		// SLMLabel.setText("boogieBib:");
		// SLMLabel.setFont(parent.getFont());

		// BB = new FileFieldEditor("sl_model", "Simulink Model", parent);
		// GridData BB_data = new GridData(GridData.FILL_HORIZONTAL);
		// data.widthHint = 250;// SIZING_TEXT_FIELD_WIDTH;

	}

	private void createSimulinkModelChooserPart(final Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText("Simulink Model:");

		sLModelText = new Text(parent, SWT.BORDER);
		sLModelText.setText("<specify model>");
		sLModelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button browseModelButton = new Button(parent, SWT.NONE);
		browseModelButton.setText("Browse...");
		browseModelButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(parent.getShell(), SWT.OPEN);
				fd.setText("Open");
				fd.setFilterPath(MeMoPlugin.getDefault().getPreferenceStore()
						.getString(MeMoPreferenceConstants.LAST_MODEL_FILE_PATH));

				// setting the filter to allow multiple extensions only works in
				// Windows,
				// for Linux, we do not use a filter
				if (System.getProperty("os.name").startsWith("Windows")) {
					// http://help.eclipse.org/indigo/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fswt%2Fwidgets%2FFileDialog.html
					// allow .mdl, .slx (and for now .xml) file types
					String[] filterExt = { "*.mdl; *.slx; *.xml" };
					fd.setFilterExtensions(filterExt);
				}
				String selected = fd.open();
				if (selected != null) {
					sLModelText.setText(selected);
					MeMoPlugin.getDefault().getPreferenceStore()
					.setValue(MeMoPreferenceConstants.LAST_MODEL_FILE_PATH, selected);
				}
				setPageComplete(validatePage());
			}

		});

	}

	@Override
	protected boolean validatePage() {
		boolean result = super.validatePage();
		if (sLModelText == null) {
			return false;
		}
		File f = new File(sLModelText.getText());
		return result && f.exists();

	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		importGroup.setVisible(visible);
	}

	public String getModelLocation() {
		return sLModelText.getText();
	}

}
