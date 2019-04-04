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

package de.tu_berlin.pes.memo.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import de.tu_berlin.pes.memo.MeMoPlugin;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class MeMoPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public MeMoPreferences() {
		super(GRID);
		setPreferenceStore(MeMoPlugin.getDefault().getPreferenceStore());
		setDescription("A demonstration of a preference page implementation. Chnages can requiere a restart.");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common
	 * GUI blocks needed to manipulate various types of preferences. Each field
	 * editor knows how to save and restore itself.
	 */
	@Override
	public void createFieldEditors() {

		/* ----------------- MATLAB ---------------------- */
		addField(new StringFieldEditor(MeMoPreferenceConstants.PR_MATLAB_IP, "MATLAB IP:",
				getFieldEditorParent()));
		addField(new IntegerFieldEditor(MeMoPreferenceConstants.PR_MATLAB_PORT, "MATLAB Port:",
				getFieldEditorParent()));
		addField(new DirectoryFieldEditor(MeMoPreferenceConstants.PR_MATLAB_PATH, "MATLAB Path",
				getFieldEditorParent()));

		addField(new IntegerFieldEditor(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT, "Timeout:",
				getFieldEditorParent()));

		addField(new IntegerFieldEditor(MeMoPreferenceConstants.PR_MATLAB_TRIES,
				"Tries (if tiemout expires):", getFieldEditorParent()));

		/* -------------- Hibernate -------------- */
		addField(new BooleanFieldEditor(MeMoPreferenceConstants.HB_SHOW_SQL, "Show SQL Querries",
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(MeMoPreferenceConstants.HB_USE_UNICODE, "Use Unicode",
				getFieldEditorParent()));
		addField(new StringFieldEditor(MeMoPreferenceConstants.HB_CHARACTER_ENCODING,
				"Character Encoding:", getFieldEditorParent()));
		addField(new StringFieldEditor(MeMoPreferenceConstants.HB_CONNECTION, "Hibernate URI:",
				getFieldEditorParent()));
		addField(new StringFieldEditor(MeMoPreferenceConstants.HB_USER, "Hibernate User:",
				getFieldEditorParent()));

		StringFieldEditor hb_password = new StringFieldEditor(MeMoPreferenceConstants.HB_PASSWORD,
				"Hibernate Password:", getFieldEditorParent());
		hb_password.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(hb_password);

		addField(new StringFieldEditor(MeMoPreferenceConstants.HB_DRIVER, "Hibernate Driver:",
				getFieldEditorParent()));
		addField(new StringFieldEditor(MeMoPreferenceConstants.HB_TRANSACTION_FACTORY,
				"Hibernate Transaction Factory:", getFieldEditorParent()));
		addField(new StringFieldEditor(MeMoPreferenceConstants.HB_QUERY_FACTORY,
				"Hibernate Query Factory", getFieldEditorParent()));
		addField(new StringFieldEditor(MeMoPreferenceConstants.HB_DIALECT, "Hibernate Dialect",
				getFieldEditorParent()));

		addField(new BooleanFieldEditor(MeMoPreferenceConstants.CLOSE_MATLAB_WITHOUT_ASKING,
				"Close open Matlab instances at program termination without asking",
				getFieldEditorParent()));
		addField(new BooleanFieldEditor(MeMoPreferenceConstants.DEBUG_MODE, "Debug Mode",
				getFieldEditorParent()));

		/*-------------Boogie----------------*/
		// addField(new
		// DirectoryFieldEditor(MeMoPreferenceConstants.BG_BOOGIE_PATH,
		// "Boogie Path",
		// getFieldEditorParent()));
		// addField(new DirectoryFieldEditor(MeMoPreferenceConstants.BG_OUT_PATH,
		// "Boogie-files  Output-Path",
		// getFieldEditorParent()));
		// // addField(new
		// DirectoryFieldEditor(MeMoPreferenceConstants.BG_XML_PATH,
		// // "XML-Boogie Library Path",
		// // getFieldEditorParent()));
		// FileFieldEditor bffe = new
		// FileFieldEditor(MeMoPreferenceConstants.BG_XML_PATH,
		// "XML-Boogie Library Path",
		// getFieldEditorParent());
		// bffe.setFileExtensions(new String[] {"*.xml"});
		// addField(bffe);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	@Override
	public void init(IWorkbench workbench) {
	}

}