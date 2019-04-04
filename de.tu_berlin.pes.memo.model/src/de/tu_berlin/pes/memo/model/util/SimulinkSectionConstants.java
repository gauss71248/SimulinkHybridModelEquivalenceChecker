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

package de.tu_berlin.pes.memo.model.util;

public class SimulinkSectionConstants {
	/** MODEL ELEMENTS **/
	public static final String BLOCK_SECTION_TYPE = "Block";
	public static final String MODEL_SECTION_TYPE = "Model";
	public static final String SYSTEM_SECTION_TYPE = "System";
	public static final String LINE_SECTION_TYPE = "Line";
	public static final String BRANCH_SECTION_TYPE = "Branch";
	public static final String PORT_SECTION_TYPE = "Port";
	public static final String GRAPHICAL_INTERFACE_SECTION_TYPE = "GraphicalInterface";
	public static final String TEST_POINTED_SIGNAL_SECTION_TYPE = "TestPointedSignal";
	/** DEFAULTS **/
	public static final String DEFAULT_SECTION_TYPE_POSTFIX = "Defaults";
	public static final String BLOCKPARAMETERSDEFAULT_SECTION_TYPE = "BlockParameterDefaults";
	public static final String ANNOTATIONDEFAULTS_SECTION_TYPE = "AnnotationDefaults";
	public static final String BLOCKDEFAULTS_SECTION_TYPE = "BlockDefaults";
	public static final String LINEDEFAULTS_SECTION_TYPE = "LineDefaults";
	/** DOCUMENTATION **/
	public static final String ANNOTATION_SECTION_TYPE = "Annotation";

	/** DATATYPES **/
	public static final String ARRAY_SECTION_TYPE = "Array";
	public static final String LIST_SECTION_TYPE = "LIST";

	/** STATEFLOW **/
	public static final String SF_SECTION_TYPE = "Stateflow";
	public static final String SF_MACHINE_SECTION_TYPE = "machine";
	public static final String SF_CHART_SECTION_TYPE = "chart";
	public static final String SF_STATE_SECTION_TYPE = "state";
	public static final String SF_JUNCTION_SECTION_TYPE = "junction";
	public static final String SF_TRANSITION_SECTION_TYPE = "transition";
	public static final String SF_TRANSITION_SRC_SECTION_TYPE = "src";
	public static final String SF_TRANSITION_DST_SECTION_TYPE = "dst";
	public static final String SF_DATA_SECTION_TYPE = "data";
	public static final String SF_DATA_PROPS_SECTION_TYPE = "props";
	public static final String SF_DATA_TYPE_SECTION_TYPE = "type";
	public static final String SF_INSTANCE_SECTION_TYPE = "instance";
	// public static final String SF_TARGET_SECTION_TYPE = "target";
	public static final String SF_EVENT_SECTION_TYPE = "event";
	//public static final String SF_TRASITION_SRC_SECTION_TYPE = "src";
	//public static final String SF_TRASITION_DST_SECTION_TYPE = "dst";

	/** CONFIG SET **/
	public static final String SIMULINK_CONFIGSET = "Simulink.ConfigSet";

	/** DEFAULT **/
	public static final String BLOCK_PARAMETER_DEFAULTS_SECTION_TYPE = "BlockParameterDefaults";

	/** LIBRARY **/
	public static final String LIBRARY_SECTION_TYPE = "Library";
	
	/** OTHER **/
	public static final String OBJECT_SECTION_TYPE = "Object";
	public static final String MASK_SECTION_TYPE = "Simulink.Mask";
	public static final String MASK_PARAMETER_SECTION_TYPE = "Simulink.MaskParameter";

}
