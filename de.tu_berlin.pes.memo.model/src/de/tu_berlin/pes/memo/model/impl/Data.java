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

package de.tu_berlin.pes.memo.model.impl;

import java.lang.reflect.Field;

import org.conqat.lib.simulink.builder.MDLSection;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.util.StateflowParameterConstants;

/**
 * Represents for example input and output data for a statechart.
 * 
 * @author Robert Reicherdt, Joachim Kuhnert
 */
public class Data extends StateflowItem {

	/**
	 * the name of the Data. This name is used to refer at states and transitions
	 * to this data.
	 * 
	 * @uml.property name="name"
	 */
	private String name;

	/**
	 * The standard constructor creates an empty data with id = -1 and sfid = -1.
	 *
	 * @see StateflowItem#StateflowItem(int, int, MDLSection)
	 */
	public Data() {
		super(-1, -1, null);
	}

	/**
	 * Creates a new Data.
	 *
	 * @param section
	 *           The model section of the Data.
	 * @param id
	 *           The unique id for all model items.
	 */
	public Data(MDLSection section, int id) {
		super(id, Integer.parseInt(section.getParameter(StateflowParameterConstants.SF_ID_STRING)),
				section);
		this.name = this.getParameter().get(StateflowParameterConstants.SF_NAME_STRING);
	}

	/**
	 * Get the name of the data.
	 *
	 * @return The data name.
	 * @uml.property name="name"
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the data.
	 *
	 * @param name
	 *           The name to set.
	 * @uml.property name="name"
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		String rslt = this.getClass().getSimpleName() + ":";
		for (Field f : this.getClass().getDeclaredFields()) {
			try {
				rslt += "(" + f.getName() + "," + f.get(this) + ")";
			} catch (IllegalArgumentException e) {
				MeMoPlugin.logException(e.toString(), e);
			} catch (IllegalAccessException e) {
				MeMoPlugin.logException(e.toString(), e);
			}
		}
		return rslt;
	}

}
