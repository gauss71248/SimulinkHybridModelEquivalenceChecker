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

/**
 * Used for ARRAY_SECTION_TYPE. Used for flattened configurations.
 * 
 * @author Robert Reicherdt, Joachim Kuhnert
 */
public class Configuration extends SimulinkItem {

	/**
	 * The name of the array entry
	 * 
	 * @uml.property name="name"
	 */
	private String name;

	/**
	 * Creates a new configuration
	 *
	 * @param id
	 *           the id this configuration should have
	 * @param configurationSection
	 *           the part of the AST where the configuration begins
	 */
	public Configuration(int id, MDLSection configurationSection) {
		super(id, configurationSection);
		this.name = configurationSection.getName();
	}

	/**
	 * Standard constructor creates an empty configuration with id -1.
	 */
	public Configuration() {
		super(-1, null);
	}

	/**
	 * Get the name of the configuration.
	 *
	 * @return The name
	 * @uml.property name="name"
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the configuration.
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
		return name;

	}

}