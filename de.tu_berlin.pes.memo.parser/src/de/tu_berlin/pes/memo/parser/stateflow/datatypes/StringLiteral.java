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

package de.tu_berlin.pes.memo.parser.stateflow.datatypes;

/**
 * A String, recogniced by "value" or 'value'.
 *
 * @author Joachim Kuhnert
 */
public class StringLiteral extends Literal {

	/** The value of the string */
	java.lang.String value;

	/**
	 * Creates a new string literal.
	 *
	 * @param value
	 *           The value of the string
	 */
	public StringLiteral(java.lang.String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
