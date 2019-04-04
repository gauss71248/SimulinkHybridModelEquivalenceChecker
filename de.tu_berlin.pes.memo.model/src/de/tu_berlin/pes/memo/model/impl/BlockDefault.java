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

import org.conqat.lib.simulink.builder.MDLSection;

import de.tu_berlin.pes.memo.model.util.SimulinkParameterNames;

/**
 * The default values for the parameters of a block type.
 *
 * @author Robert Reicherdt, Joachim Kuhnert
 */
public class BlockDefault extends SimulinkItem {

	/**
	 * The name of the block type these defaults belong to.
	 *
	 * @uml.property name="type"
	 */
	private String type = "";

	/**
	 * Standard constructor creates a BlockDefault with id -1.
	 */
	public BlockDefault() {
		super(-1, null);
	}

	/**
	 * Creates a BlockDefault.
	 *
	 * @param id
	 *           The unique id of model item.
	 * @param blockSection
	 *           The section of the block default containing the parameters.
	 */
	public BlockDefault(int id, MDLSection blockSection) {
		super(id, blockSection);
		this.setType(blockSection.getParameter(SimulinkParameterNames.BLOCK_TYPE_PARAMETER));
	}

	/**
	 * Creates a BlockDefault for the given type and with the given id.
	 *
	 * @param id
	 *           The unique id of model item.
	 * @param type
	 *           The name of the block type.
	 */
	public BlockDefault(int id, String type) {
		super(id, null);
		this.type = type;
	}

	/**
	 * Sets the block type name the defaults belong to.
	 *
	 * @param type
	 *           the type to set
	 * @uml.property name="type"
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * get the block type name the defaults belong to.
	 *
	 * @return the type
	 * @uml.property name="type"
	 */
	public String getType() {
		return type;
	}

}
