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

public class SimulinkPortConstants {

	public static final int IN_PORT_POSITION = 0;
	public static final int OUT_PORT_POSITION = 1;
	public static final int ENABLE_PORT_POSITION = 2;
	public static final int TRIGGER_PORT_POSITION = 3;
	public static final int STATE_PORT_POSITION = 4;
	public static final int L_CONN_PORT_POSITION = 5;
	public static final int R_CONN_PORT_POSITION = 6;
	public static final int IFACTION_PORT_POSITION = 7;

	public static final String IN_PORT_PREFIX = "in";
	public static final String OUT_PORT_PREFIX = "out";
	public static final String ENABLE_PORT = "enable";
	public static final String TRIGGER_PORT = "trigger";
	public static final String STATE_PORT = "state";
	public static final String L_CONN_PORT_PREFIX = "LConn";
	public static final String R_CONN_PORT_PREFIX = "RConn";
	public static final String IFACTION_PORT = "ifaction";

	public static final String PORT_HANDLES_PARAMETER = "PortHandles";
	public static final String PORT_CONNECTIVITY_PARAMETER = "PortConnectivity";
	public static final String COMPILED_PORT_WIDTHS_PARAMETER = "CompiledPortWidths";
	public static final String COMPILED_PORT_DIMENSIONS_PARAMETER = "CompiledPortDimensions";
	public static final String COMPILED_PORT_DATA_TYPES_PARAMETER = "CompiledPortDataTypes";
	public static final String COMPILED_PORT_COMPLEX_SIGNALS_PARAMETER = "CompiledPortComplexSignals";
	public static final String COMPILED_PORT_FRAME_DATA_PARAMETER = "CompiledPortFrameData";
	public static final String COMPILED_PORT_BUS_MODE_PARAMETER = "CompiledPortBusMode";

	// public static final String COMPILED_BUS_TYPE = "CompiledBusType";
	// public static final String SIGNAL_HIERARCHY = "SignalHierarchy";

}
