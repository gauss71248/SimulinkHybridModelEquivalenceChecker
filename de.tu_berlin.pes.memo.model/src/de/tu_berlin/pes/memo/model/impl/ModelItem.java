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
import java.util.HashMap;
import java.util.Map;

import org.conqat.lib.simulink.builder.MDLSection;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.util.SimulinkSectionConstants;

/**
 * The super class of all model parts.
 * 
 * @author Robert Reicherdt, Joachim Kuhnert
 */
public class ModelItem implements Serializable {

	/**
	 * All settings of an item, including the explicit fields of the class an the
	 * non field parameters.
	 * 
	 * @uml.property name="parameter"
	 */
	private Map<String, String> parameter = new HashMap<String, String>();

	/**
	 * The unique id of an item. Used as hash code!
	 * 
	 * @uml.property name="id"
	 */
	private int id = 0;

	/**
	 * Creates a new ModelItem
	 *
	 * @param id
	 *           The (unique) id of the ModelItem
	 * @param parameters
	 *           The model section containing the parameters of this item. Can be
	 *           null.
	 */
	public ModelItem(int id, MDLSection parameters) {
		this.setId(id);
		
		if (parameters != null) {
			Map<String, String> paramap = parameters.getParameterMapRecursively();
			for (String key : paramap.keySet()) {
				if (key.startsWith(SimulinkSectionConstants.SYSTEM_SECTION_TYPE + ".")) {
					continue; // Don't add Subsystem parameters
				}
				String value = paramap.get(key);
				getParameter().put(key, value);
			}
		}
	}

	@Override
	public int hashCode() {
		// use id as hash!
		return id;
	}

	/**
	 * @param parameter
	 *           the parameters to set
	 * @uml.property name="parameter"
	 */
	public void setParameter(Map<String, String> parameter) {
		this.parameter = parameter;
	}

	/**
	 * @return the parameters
	 * @uml.property name="parameter"
	 */
	public Map<String, String> getParameter() {
		return parameter;
	}

	/**
	 * @param id
	 *           the id to set
	 * @uml.property name="id"
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @return the id
	 * @uml.property name="id"
	 */
	public int getId() {
		return id;
	}

}
