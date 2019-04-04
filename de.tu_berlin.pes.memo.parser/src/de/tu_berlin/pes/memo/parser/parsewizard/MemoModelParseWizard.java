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
import java.util.Collection;
//import java.util.LinkedList;
//import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.parser.MeMoParserManager;
import de.tu_berlin.pes.memo.parser.persistence.MeMoPersistenceManager;
import de.tu_berlin.pes.memo.parser.persistence.views.ActiveDBView;
//import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;
import de.tu_berlin.pes.memo.project.ProjectNature;
import de.tu_berlin.pes.memo.project.ProjectUtil;
//import javax.swing.JOptionPane;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

/**
 * Wizard to parse a mdl-file.
 *
 * @author Robetr Reicherdt, Joachim Kuhnert
 */
public class MemoModelParseWizard extends Wizard {
	private IFile selectedFile;
	private MeMoParseStandardParametersPage standardParameters;
	private MeMoCustomLibrariesPage customLibraries;
	private MeMoMFilesPage mFiles;

	/**
	 * Create the parse wizard.
	 *
	 * @param selectedFile
	 *           The File to parse
	 */
	public MemoModelParseWizard(IFile selectedFile) {
		this.selectedFile = selectedFile;
		standardParameters = new MeMoParseStandardParametersPage();
		customLibraries = new MeMoCustomLibrariesPage(selectedFile.getProject());
		mFiles = new MeMoMFilesPage(selectedFile.getProject());

		setWindowTitle("Parse a Model");
		addPage(standardParameters);
		addPage(customLibraries);
		addPage(mFiles);
	}

	@Override
	public boolean performFinish() {
		final MeMoPersistenceManager persistanceManager = MeMoPersistenceManager.getInstance();
		final IProject current = selectedFile.getProject();

		try {

			standardParameters.storeFiles();
			customLibraries.storeFiles();
			mFiles.storeFiles();

			MeMoParserManager.setmFiles(ProjectUtil.getMFilesFromProject(current));
			MeMoParserManager.setLibFiles(ProjectUtil.getlibFilesFromProject(current));
			final String fName = selectedFile.getLocation().toOSString();

			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					persistanceManager.saveModelToDB(MeMoParserManager.loadModelSectionFromFile(fName), standardParameters.getMatlabEvaluationStatus(), true, standardParameters.getRandomizeStatus());
					try {
						storeModelRefs(MeMoParserManager.getModelReferenceFilesMap().values());
					} catch (CoreException e1) {
						MeMoPlugin.logException(e1.toString(), e1);
					}

					try {
						addNewDBName(persistanceManager.getDatabaseName(), selectedFile.getProject());
					} catch (CoreException e) {
						MeMoPlugin.logException(e.toString(), e);
					}
				}
			});

			t.start();

			return true;

		} catch (CoreException e) {
			MeMoPlugin.logException(e.toString(), e);
		}

		return false;
	}

	/**
	 * Add the new database to the project properties.
	 *
	 * @param databaseName
	 *           The name of the new created database
	 * @param project
	 *           The project the database belongs to.
	 * @throws CoreException
	 */
	private void addNewDBName(String databaseName, IProject project) throws CoreException {
		boolean newName = true;
		String allStrings = project.getPersistentProperty(ProjectNature.DATABASES);
		if (allStrings == null) {
			allStrings = "";
		}

		String[] content = allStrings.split(File.pathSeparator);
		for (String s : content) {
			if ((s != null) && !s.isEmpty() && s.equals(databaseName)) {
				newName = false;
				break;
			}
		}

		if (newName) {
			allStrings += databaseName + File.pathSeparator;
		}

		selectedFile.getProject().setPersistentProperty(ProjectNature.DATABASES, allStrings);
		selectedFile.getProject().setPersistentProperty(ProjectNature.ACTIVEDATABASE, databaseName);

		ActiveDBView.update();

	}

	/**
	 * Store the model references. The model files of referenced models are
	 * hidden among the library files and are identified while parsing. To get
	 * the model references later without new paring, store them in the project.
	 *
	 * @param files
	 * @throws CoreException
	 */
	private void storeModelRefs(Collection<File> files) throws CoreException {
		String filesString = "";
		for (File file : files) {
			filesString += file.getAbsolutePath() + File.pathSeparator;
		}

		selectedFile.getProject().setPersistentProperty(ProjectNature.MODEL_REFERENCES, filesString);

	}

}
