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

package de.tu_berlin.pes.memo.parser.stateflow.datatypes;

import java.util.ArrayList;

/**
 * @author reicherdt
 */
public class GraphicalFunction extends Expression {

	/**
	 * @uml.property name="signature"
	 * @uml.associationEnd
	 */
	Expression signature;
	ArrayList<Expression> returnvalues;
	ArrayList<Expression> arguments;

	public GraphicalFunction(ArrayList<Expression> returnvals, Function func) {
		this.signature = func.getIdentifier();
		this.returnvalues = returnvals;
		this.arguments = func.arguments;

		if (func.getClass().getSimpleName().equals("Function")) {
			;
		}
	}

}
