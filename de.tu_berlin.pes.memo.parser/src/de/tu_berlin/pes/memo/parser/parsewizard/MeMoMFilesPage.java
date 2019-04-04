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

package de.tu_berlin.pes.memo.parser.parsewizard;

import java.io.File;
import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;
import de.tu_berlin.pes.memo.project.ProjectNature;

/**
 * The Page of the parse wizard to define model specific m-files.
 *
 * @author Robert Reicherdt, Joachim Kuhnert
 *
 */
public class MeMoMFilesPage extends WizardPage {

	private ListViewer fileList;
	private IProject project;
	private ArrayList<String> listContent;

	protected MeMoMFilesPage(IProject iProject) {
		super("page3");
		setTitle("Custom M-Files");
		setMessage("Add M-Files....");
		project = iProject;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.SCROLL_LINE);
		setControl(composite);

		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fileList = new ListViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		fileList.setContentProvider(new IStructuredContentProvider() {

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// NOP

			}

			@Override
			public void dispose() {
				// NOP

			}

			@Override
			public Object[] getElements(Object inputElement) {
				return (Object[]) inputElement;
			}
		});

		fileList.getControl().addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == SWT.DEL) {
					IStructuredSelection selection = (IStructuredSelection) fileList.getSelection();
					for (Object file : selection.toArray()) {
						listContent.remove(file);
					}

					fileList.setInput(listContent.toArray());

					setPageComplete(validatePage());
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {

			}
		});

		listContent = new ArrayList<String>();

		// load entries
		try {
			String all = project.getPersistentProperty(ProjectNature.MFILES);

			if (all != null) {
				String[] content = all.split(File.pathSeparator);
				for (String s : content) {
					if ((s != null) && !s.isEmpty()) {
						listContent.add(s);
					}
				}

			}
		} catch (CoreException e1) {
			MeMoPlugin.logException(e1.toString(), e1);
		}

		fileList.setInput(listContent.toArray());
		data.widthHint = 300;
		data.heightHint = 200;
		fileList.getList().setLayoutData(data);

		Button browseModelButton = new Button(composite, SWT.NONE);
		browseModelButton.setText("Browse...");
		browseModelButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell(), SWT.MULTI);
				fd.setText("Open");
				fd.setFilterPath(MeMoPlugin.getDefault().getPreferenceStore()
						.getString(MeMoPreferenceConstants.LAST_LIBRARY_FILE_PATH));
				String[] filterExt = { "*.m" };
				fd.setFilterExtensions(filterExt);
				fd.open();
				for (String file : fd.getFileNames()) {
					listContent.add(fd.getFilterPath() + File.separator + file);
				}

				MeMoPlugin.getDefault().getPreferenceStore()
						.setValue(MeMoPreferenceConstants.LAST_LIBRARY_FILE_PATH, fd.getFilterPath());

				fileList.setInput(listContent.toArray());

				setPageComplete(validatePage());
			}

		});
		data = new GridData();
		data.widthHint = 100;
		data.verticalAlignment = SWT.BOTTOM;
		browseModelButton.setLayoutData(data);

		setVisible(true);
		setPageComplete(validatePage());
	}

	/**
	 * Stores the files in the project so they need not be entried every time.
	 *
	 * @throws CoreException
	 */
	protected void storeFiles() throws CoreException {

		String filesString = "";
		Object[] files = (Object[]) fileList.getInput();
		for (Object file : files) {
			filesString += (String) file + File.pathSeparator;
		}

		project.setPersistentProperty(ProjectNature.MFILES, filesString);
	}

	/**
	 * Checks if all given paths are correct.
	 *
	 * @return true if everything is alright-
	 */
	private boolean validatePage() {
		Object[] files = (Object[]) fileList.getInput();
		for (Object file : files) {
			File f = new File((String) file);
			if (!f.exists()) {
				setErrorMessage("\"" + (String) file + "\" is not a valid File");
				return false;
			}
		}
		setErrorMessage(null);
		return true;
	}

}
