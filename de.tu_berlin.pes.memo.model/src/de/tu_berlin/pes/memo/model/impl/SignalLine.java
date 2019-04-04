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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class represents the link between two model items.
 * 
 * @author Robert Reichertd, Joachim Kuhnert
 */
public class SignalLine extends SimulinkItem {

	/**
	 * Name of the SignalLine
	 * 
	 * @uml.property name="name"
	 */
	private String name;
	/**
	 * source port of the source block where the <code>SignalLine</code> starts.
	 * 
	 * @uml.property name="srcPort"
	 * @uml.associationEnd
	 */
	private Port srcPort;
	/**
	 * Destination port of the block where the <code>SignalLine</code> ends.
	 * 
	 * @uml.property name="dstPort"
	 * @uml.associationEnd
	 */
	private Port dstPort;

	// /**
	// * The corresponding part of the AST.
	// */
	// private MDLSection lineSection;
	/**
	 * How deep is the link coverd by subsystems.
	 * 
	 * @uml.property name="lvl"
	 */
	private int lvl;
	/**
	 * The block where the <code>SignalLine</code> starts.
	 * 
	 * @uml.property name="srcBlock"
	 * @uml.associationEnd
	 */
	private Block srcBlock;
	/**
	 * The block where the <code>SignalLine</code> ends.
	 * 
	 * @uml.property name="dstBlock"
	 * @uml.associationEnd
	 */
	private Block dstBlock;

	/**
	 * Bus/Mux specific, empty if not part of Bus/Mux system
	 *
	 * The first signal line of all paths that ends at this signal line. It
	 * contains all signal sources ordered by the signal number. This is
	 * important to reconstruct the signal paths in correct order.
	 *
	 * In Fact this field is an AraryList<ArrayList<SignalLine>>, because e.g.
	 * switches can define different paths for the same signal on a line.
	 * Hibernate can't map nested collections, so the wrapper class is needed.
	 */
	private List<ConcurrentSignalLineOrigins> orderedSignalOrigins = new ArrayList<ConcurrentSignalLineOrigins>();

	/**
	 * Bus/Mux specific, empty if not part of Bus/Mux system
	 *
	 * The first signal line of all paths that cross this signal line.
	 */
	private Set<SignalLine> signalOrigins = new HashSet<SignalLine>();

	/**
	 * Bus/Mux specific, empty if not part of Bus/Mux system
	 *
	 * The last signal line of all paths that cross this signal line.
	 */
	private Set<SignalLine> signalDestinations = new HashSet<SignalLine>();

	/**
	 * True if this signalline is branched
	 * 
	 * @uml.property name="isBranch"
	 * @uml.associationEnd
	 */
	private boolean isBranched = false;

	/**
	 * creates a <code>SignalLine</code> with id -1.
	 */
	public SignalLine() {
		super(-1, null);
	}

	/**
	 * Creates a new <code>SignalLine</code> with the given parameters.
	 *
	 * @param id
	 *           the <code>ModelItem</code> id
	 * @param lineSection
	 *           the corresponding part of the AST
	 * @param lvl
	 *           the layer of subsystems
	 * @param src
	 *           the source block
	 * @param dst
	 *           the destination block
	 * @param srcPort
	 *           the port of the source block
	 * @param dstPort
	 *           the port of the destination block
	 */
	public SignalLine(int id, int lvl, Block src, Block dst, Port srcPort, Port dstPort,
			String signalName) {
		super(id, null);
		assert ((srcPort != null) && (dstPort != null));
		this.srcBlock = src;
		this.dstBlock = dst;
		this.lvl = lvl;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.name = signalName;
	}

	/**
	 * Get the name of the SignalLine.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the signalLine.
	 *
	 * @param name
	 *           The new name.
	 * @uml.property name="name"
	 */
	public void setName(String name) {
		this.name = name;
	}

	// @Override
	// public String toString() { // (to) extensive toString method
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
	public String toString() { // prints only the "SignalLine:$ID$"
		String result = this.getClass().getSimpleName() + ":" + getId();
		if ((name != null) && !name.isEmpty()) {
			result += ":" + name;
		}
		return result;
	}

	/**
	 * Get the origin port of the SIgnalLine.
	 *
	 * @return The source port.
	 * @uml.property name="srcPort"
	 */
	public Port getSrcPort() {
		return srcPort;
	}

	/**
	 * Set the origin port of the signal line.
	 *
	 * @param srcPort
	 *           The new source port
	 * @uml.property name="srcPort"
	 */
	public void setSrcPort(Port srcPort) {
		this.srcPort = srcPort;
	}

	/**
	 * Get the target port.
	 *
	 * @return The destination port.
	 * @uml.property name="dstPort"
	 */
	public Port getDstPort() {
		return dstPort;
	}

	/**
	 * Set the target Port.
	 *
	 * @param dstPort
	 *           The new destination port.
	 * @uml.property name="dstPort"
	 */
	public void setDstPort(Port dstPort) {
		this.dstPort = dstPort;
	}

	// public MDLSection getLineSection() {
	// return lineSection;
	// }
	//
	// public void setLineSection(MDLSection lineSection) {
	// this.lineSection = lineSection;
	// }

	/**
	 * Is this signal line branched in the original model?
	 *
	 * @return true if the signal line is branched.
	 * @uml.property name"isBranch"
	 */
	public boolean getIsBranched() {
		return isBranched;
	}

	/**
	 * Set if the signal line is branched.
	 *
	 * @param isBranched
	 *           The branch vlaue.
	 * @uml.property name"isBranch"
	 */
	public void setIsBranched(boolean isBranched) {
		this.isBranched = isBranched;
	}

	/**
	 * get the layer of the SignalLine.
	 *
	 * @return The level in the model hierarchy.
	 * @uml.property name="lvl"
	 */
	public int getLevel() {
		return lvl;
	}

	/**
	 * Set the layer.
	 *
	 * @param lvl
	 *           The new level in the model hierarchy.
	 * @uml.property name="lvl"
	 */
	public void setLvl(int lvl) {
		this.lvl = lvl;
	}

	/**
	 * Get the origin block.
	 *
	 * @return The source block.
	 * @uml.property name="srcBlock"
	 */
	public Block getSrcBlock() {
		return srcBlock;
	}

	/**
	 * Set the origin block.
	 *
	 * @param srcBlock
	 *           The new source block.
	 * @uml.property name="srcBlock"
	 */
	public void setSrcBlock(Block srcBlock) {
		this.srcBlock = srcBlock;
	}

	/**
	 * Get the target block.
	 *
	 * @return The destination Block.
	 * @uml.property name="dstBlock"
	 */
	public Block getDstBlock() {
		return dstBlock;
	}

	/**
	 * Set the target block.
	 *
	 * @param dstBlock
	 *           The new destiantion block.
	 * @uml.property name="dstBlock"
	 */
	public void setDstBlock(Block dstBlock) {
		this.dstBlock = dstBlock;
	}

	/**
	 * @return the signalDestinations
	 */
	public Set<SignalLine> getSignalDestinations() {
		return signalDestinations;
	}

	/**
	 * @param signalDestinations
	 *           the signalDestinations to set
	 */
	public void setSignalDestinations(Set<SignalLine> signalDestinations) {
		this.signalDestinations = signalDestinations;
	}

	/**
	 * @return the signalOrigins
	 */
	public Set<SignalLine> getSignalOrigins() {
		return signalOrigins;
	}

	/**
	 * @param signalOrigins
	 *           the signalOrigins to set
	 */
	public void setSignalOrigins(Set<SignalLine> signalOrigins) {
		this.signalOrigins = signalOrigins;
	}

	/**
	 * @return the orderedSignalOrigins
	 */
	public List<ConcurrentSignalLineOrigins> getOrderedSignalOrigins() {
		return orderedSignalOrigins;
	}

	/**
	 * @param orderedSignalOrigins
	 *           the orderedSignalOrigins to set
	 */
	public void setOrderedSignalOrigins(List<ConcurrentSignalLineOrigins> orderedSignalOrigins) {
		this.orderedSignalOrigins = orderedSignalOrigins;
	}

}
