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

import java.io.Serializable;

import org.conqat.lib.simulink.builder.MDLSection;

/**
 * One port of a block, where a <code>SignalLine</code> could be attached.
 * 
 * @author Robert Reicherdt, Joachim Kuhnert
 */
public class Port extends SimulinkItem implements Serializable{


	private static final long serialVersionUID = -277992484178445143L;
	
	/**
	 * @uml.property name="name"
	 */
	private String name;
	private int number = -1;

	/**
	 * Standard constructor, creates an empty port with id -1.
	 */
	public Port() {
		super(-1, null);
	}

	public Port(int id, String name, int nr) {
		super(id, null);
		this.name = name;
		number = nr;
	}

	/**
	 * Get the port name.
	 *
	 * @return The name of the port.
	 * @uml.property name="name"
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the port.
	 *
	 * @param name
	 *           The new name.
	 * @uml.property name="name"
	 */
	public void setName(String name) {
		this.name = name;
	}

	// @Override
	// public String toString() {
	// String rslt = this.getClass().getSimpleName() + ":";
	// for(Field f : this.getClass().getDeclaredFields()) {
	// try {
	// rslt += "(" + f.getName() + "," + f.get(this) + ")";
	// } catch (IllegalArgumentException e) {
	// MeMoPlugin.logException(e.toString(), e);
	// } catch (IllegalAccessException e) {
	// MeMoPlugin.logException(e.toString(), e);
	// }
	// }
	// return rslt;
	// }

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Add parameters to this port.
	 *
	 * @param portSection
	 */
	public void addParameters(MDLSection portSection) {

		if (portSection == null) {
			return;
		}

		for (String key : portSection.getParameterNames()) {
			String value = portSection.getParameter(key);
			getParameter().put(key, value);
		}
	}

	/**
	 * @return the number
	 */
	public int getNumber() {
		return number;
	}

	/**
	 * @param number
	 *           the number to set
	 */
	public void setNumber(int number) {
		this.number = number;
	}

}
