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

import java.io.File;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import de.tu_berlin.pes.memo.MeMoPlugin;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#
	 * initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = MeMoPlugin.getDefault().getPreferenceStore();

		store.setDefault(MeMoPreferenceConstants.PR_MATLAB_PORT, "5050");
		store.setDefault(MeMoPreferenceConstants.PR_MATLAB_IP, "localhost");
		store.setDefault(MeMoPreferenceConstants.PR_MATLAB_PATH,
				"C:\\Program Files\\MATLAB\\R2009b\\bin");
		store.setDefault(MeMoPreferenceConstants.PR_MATLAB_TIMEOUT, 5);
		store.setDefault(MeMoPreferenceConstants.PR_MATLAB_TRIES, 5);

		store.setDefault(MeMoPreferenceConstants.HB_CONNECTION,
				"jdbc:postgresql://localhost:5432/testdb1");
		store.setDefault(MeMoPreferenceConstants.HB_USER, "default");
		store.setDefault(MeMoPreferenceConstants.HB_PASSWORD, "default");
		store.setDefault(MeMoPreferenceConstants.HB_DRIVER, "org.postgresql.Driver");
		store.setDefault(MeMoPreferenceConstants.HB_TRANSACTION_FACTORY,
				"org.hibernate.transaction.JDBCTransactionFactory");
		store.setDefault(MeMoPreferenceConstants.HB_QUERY_FACTORY,
				"org.hibernate.hql.ast.ASTQueryTranslatorFactory");
		store.setDefault(MeMoPreferenceConstants.HB_DIALECT, "org.hibernate.dialect.ProgressDialect");
		store.setDefault(MeMoPreferenceConstants.HB_SHOW_SQL, "false");
		store.setDefault(MeMoPreferenceConstants.HB_CHARACTER_ENCODING, "ISO-8859-1");
		store.setDefault(MeMoPreferenceConstants.HB_USE_UNICODE, "true");

		store.setDefault(MeMoPreferenceConstants.AUTO_CREATE_DATABASES, false);
		store.setDefault(MeMoPreferenceConstants.LAST_MODEL_FILE_PATH,
				new File(".").getAbsolutePath());
		store.setDefault(MeMoPreferenceConstants.LAST_M_FILE_PATH, new File(".").getAbsolutePath());
		store.setDefault(MeMoPreferenceConstants.LAST_LIBRARY_FILE_PATH,
				new File(".").getAbsolutePath());
		store.setDefault(MeMoPreferenceConstants.STANDARD_LIBRARIES, "");

		store.setDefault(MeMoPreferenceConstants.DEBUG_MODE, false);
		store.setDefault(MeMoPreferenceConstants.CLOSE_MATLAB_WITHOUT_ASKING, false);

		store.setDefault(MeMoPreferenceConstants.SLICING_BUSRELOVING, false);
		store.setDefault(MeMoPreferenceConstants.SLICING_COMPREHENSIVE, false);
		store.setDefault(MeMoPreferenceConstants.SLICING_FORWARD, false);
		store.setDefault(MeMoPreferenceConstants.SLICING_ALL, false);
	}

}
