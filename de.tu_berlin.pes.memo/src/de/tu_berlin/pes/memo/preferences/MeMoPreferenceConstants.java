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

/**
 * Constant definitions for plug-in preferences
 */
public class MeMoPreferenceConstants {

	public static final String PR_MATLAB_PORT = "matlab.port";
	public static final String PR_MATLAB_IP = "matlab.ip";
	public static final String PR_MATLAB_PATH = "matlab.path.execute";
	public static final String PR_MATLAB_TIMEOUT = "matlab.connection.timeout";
	public static final String PR_VERSION = "matlab.version";
	public static final String PR_MATLAB_TRIES = "matlab.tries";

	public static final String HB_CONNECTION = "hibernate.connection.url";
	public static final String HB_USER = "hibernate.connection.username";
	public static final String HB_PASSWORD = "hibernate.connection.password";
	public static final String HB_DRIVER = "hibernate.connection.driver_class";
	public static final String HB_TRANSACTION_FACTORY = "hibernate.transaction.factory_class";
	public static final String HB_QUERY_FACTORY = "hibernate.query.factory_class";
	public static final String HB_DIALECT = "hibernate.dialect";
	public static final String HB_SHOW_SQL = "hibernate.show_sql";
	public static final String HB_CHARACTER_ENCODING = "hibernate.connection.characterEncoding";
	public static final String HB_USE_UNICODE = "hibernate.connection.useUnicode";

	public static final String LAST_MODEL_FILE_PATH = "system.lastModelFilePath";
	public static final String LAST_M_FILE_PATH = "system.lastMFilePath";
	public static final String LAST_LIBRARY_FILE_PATH = "system.lastLibFilePath";
	public static final String AUTO_CREATE_DATABASES = "system.autoCreateDatabases";
	public static final String DEBUG_MODE = "system.debug";
	public static final String CLOSE_MATLAB_WITHOUT_ASKING = "system.alwaysCloseMatlab";

	public static final String BG_BOOGIE_PATH = "boogie.path";
	public static final String BG_XML_PATH = "xml.path";
	public static final String BG_OUT_PATH = "bg_out.path";

	// Standard libraries
	public static final String STANDARD_LIBRARIES = "system.standardLibraries";

	public static final String SLICING_FORWARD = "slicing.forward";
	public static final String SLICING_BUSRELOVING = "slicing.busresolving";
	public static final String SLICING_COMPREHENSIVE = "slicing.comprehensive";
	public static final String SLICING_ALL = "slicing.sliceall";
	public static final String SLICING_COLORING = "slicing.coloring";
	public static final String SLICING_ENABLE_COLORING = "slicing.enableColoring";
	public static final String SLICING_SELECTED_COLOR = "slicing.selectedColor";
	public static final String SLICING_RESET_COLOR = "slicing.resetColor";
	public static final String SLICING_PROBABILISTIC = "slicing.probabilistic";
	public static final String SLICING_ENABLE_PROBABILISTIC = "slicing.enableProbabilistic";

}
