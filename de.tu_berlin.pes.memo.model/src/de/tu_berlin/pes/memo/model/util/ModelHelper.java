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

import java.util.ArrayList;

import de.tu_berlin.pes.memo.MeMoException;
import de.tu_berlin.pes.memo.MeMoPlugin;

public class ModelHelper {

	/**
	 * Returns the id of the parent parsed out of a linkNode String. A linkNode
	 * String pattern looks like "[parentID anOtherNumber anOtherNumber]", so it
	 * returns just the parentID by splitting the String. Returns -1 if no ID
	 * found.
	 *
	 * @param linkNode
	 *           the linkNode parameter String
	 * @return the parent id defined by the linkNode or -1
	 */
	public static int getParentID(String linkNode) {
		int parentID = -1;
		String[] split = linkNode.split("\\D");
		if (split.length > 1) {
			try {
				return Integer.parseInt(split[1]);
			} catch (NumberFormatException e) {
				MeMoPlugin.logException(e.toString(), e);
			}
		}
		return parentID;
	}

	/**
	 * returns a one dimensional array for a parameter string *
	 * 
	 * @param array
	 *           the string representation of the array
	 * @return array
	 * @throws MeMoException
	 *
	 * @throws RuntimeException
	 *            if array is multidimensional!
	 */
	public static String[] paramStringToStringArray(String array) throws MeMoException {
		ArrayList<String> result = new ArrayList<String>();
		// remove [ and ] at beginning and end if(neccessary)
		String tmp = array;
		if (tmp.startsWith("[")) {
			tmp = array.substring(array.indexOf("[") + 1, array.lastIndexOf("]") - 1);
		}
		if (tmp.contains("[") || tmp.contains("]")) {
			throw new MeMoException(
					"Trying to cast a multi dimensional Array into a single dimensional array");
		}
		String[] elements = tmp.split(" ");
		for (String e : elements) {
			if (!e.isEmpty()) {
				result.add(e);
			}
		}
		return result.toArray(new String[result.size()]);
	}
}
