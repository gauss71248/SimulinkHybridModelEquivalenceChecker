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

package de.tu_berlin.pes.memo.parser;

import java.util.HashMap;

import org.conqat.lib.simulink.builder.MDLSection;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.impl.Block;
import de.tu_berlin.pes.memo.model.impl.BlockDefault;
import de.tu_berlin.pes.memo.model.impl.ChartBlock;
import de.tu_berlin.pes.memo.model.impl.Model;
import de.tu_berlin.pes.memo.model.impl.ModelItem;
import de.tu_berlin.pes.memo.model.impl.ReferenceBlock;
import de.tu_berlin.pes.memo.model.impl.SignalLine;
import de.tu_berlin.pes.memo.model.impl.StateflowChart;

/**
 * This class creates Blocks and assigns to each Block the default properties of
 * its type. The default properties are stored in the BlockDefault object. For
 * each block type exist exactly one BlockDefault object.
 * 
 * @author Joachim Kuhnert
 */
public class BlockFactory {

	/**
	 * @uml.property name="instance"
	 * @uml.associationEnd
	 */
	private static BlockFactory instance = new BlockFactory();

	// private BlockFactory(){}
	//
	// /**
	// * @return
	// * @uml.property name="instance"
	// */
	// public static BlockFactory getInstance(){
	// return instance;
	// }

	private HashMap<String, HashMap<String, BlockDefault>> modelname2defaults = new HashMap<String, HashMap<String, BlockDefault>>();

	/**
	 * Adds new default parameters for a block type to the block factory. If
	 * default parameters already known, the parameter values will be updated to
	 * the given values of the MDLSection.
	 *
	 * @param type
	 *           the block type its defaults shall be updated
	 * @param defaultSection
	 *           the section where the defaults are defined
	 * @param model
	 *           the related model, important to generate new id's
	 */
	public void addDefaults(String modelName, String type, MDLSection defaultSection, Model model) {
		HashMap<String, BlockDefault> defaults = modelname2defaults.get(modelName);
		if (defaults == null) {
			defaults = new HashMap<String, BlockDefault>();
			modelname2defaults.put(modelName, defaults);
		}
		if (defaults.get(type) == null) {
			defaults.put(type, new BlockDefault(model.nextID(), defaultSection));
		} else {
			BlockDefault defaultBlock = defaults.get(type);
			for (String key : defaultSection.getParameterNames()) {
				String value = defaultSection.getParameter(key);
				defaultBlock.getParameter().put(key, value);
			}
		}
	}

	/**
	 * Creates a new block with the given parameters.
	 *
	 * @param level
	 *           how deep the block is buried in subsystems
	 * @param parent
	 *           the overlaying subsystem block hash or 0 if block is on the
	 *           first layer (level = 0)
	 * @param blockSection
	 *           The part of the AST to which the block belongs
	 * @param model
	 *           the related model, important to generate new id's
	 * @return The created Block
	 */
	public Block createBlock(int level, ModelItem parent, MDLSection blockSection, Model model) {
		Block result = new Block(model.nextID(), level, parent, model, blockSection);
		String blockModelName = result.getModelName();
		HashMap<String, BlockDefault> defaults = modelname2defaults.get(blockModelName);
		if (defaults == null) {
			defaults = new HashMap<String, BlockDefault>();
			modelname2defaults.put(blockModelName, defaults);
		}
		if (defaults.get(result.getType()) == null) {
			defaults.put(result.getType(), new BlockDefault(model.nextID(), result.getType()));
		}
		result.setDefaultParamters(defaults.get(result.getType()));

		return result;
	}

	/**
	 * Creates a new block with the given parameters, especially with the given
	 * name. This is important if you substitute a reference block by the block
	 * of the library.
	 *
	 * @param level
	 *           how deep buried in subsystems
	 * @param parent
	 *           the overlaying subsystem block hash or 0 if block is on the
	 *           first layer (level = 0)
	 * @param blockSection
	 *           The part of the AST to which the block belongs
	 * @param model
	 *           the related model, important to generate new id's
	 * @param blockName
	 *           The name the block will have, regardless of the parameter name
	 *           in the block section.
	 * @return The created Block
	 */
	public Block createBlock(int level, ModelItem parent, MDLSection blockSection, Model model,
			String blockName) {
		Block result = new Block(model.nextID(), level, parent, model, blockSection, blockName);
		String blockModelName = result.getModelName();
		HashMap<String, BlockDefault> defaults = modelname2defaults.get(blockModelName);
		if (defaults == null) {
			defaults = new HashMap<String, BlockDefault>();
			modelname2defaults.put(blockModelName, defaults);
		}
		if (defaults.get(result.getType()) == null) {
			defaults.put(result.getType(), new BlockDefault(model.nextID(), result.getType()));
		}
		result.setDefaultParamters(defaults.get(result.getType()));

		return result;
	}

	/**
	 * Creates a new block with the given parameters. Will return a a
	 * ReferenceBlock that replaces the block that references but stores the
	 * original Block for accessing old parameters.
	 *
	 * @param level
	 *           how deep buried in subsystems
	 * @param parent
	 *           the overlaying subsystem block hash or 0 if block is on the
	 *           first layer (level = 0)
	 * @param blockSection
	 *           The part of the AST to which the block belongs
	 * @param libBlockSection
	 *           The same as the block section, only from the AST oft the library
	 * @param model
	 *           the related model, important to generate new id's
	 * @param blockName
	 *           The name the block will have, regardless of the parameter name
	 *           in the block section.
	 * @return The created Block
	 */
	public ReferenceBlock createRefBlock(int level, ModelItem parent, MDLSection blockSection,
			MDLSection libBlockSection, Model model, String blockName) {
		Block original = createBlock(level, parent, blockSection, model, blockName);
		ReferenceBlock refBlock = new ReferenceBlock(model.nextID(), level, parent, model,
				libBlockSection, blockName, original);

		String blockModelName = refBlock.getModelName();
		HashMap<String, BlockDefault> defaults = modelname2defaults.get(blockModelName);
		if (defaults == null) {
			defaults = new HashMap<String, BlockDefault>();
			modelname2defaults.put(blockModelName, defaults);
		}
		if (defaults.get(refBlock.getType()) == null) {
			defaults.put(refBlock.getType(), new BlockDefault(model.nextID(), refBlock.getType()));
		}
		refBlock.setDefaultParamters(defaults.get(refBlock.getType()));

		return refBlock;
	}

	/**
	 * Replaces a common block by a chartblock
	 *
	 * @param chart
	 *           the corresponding chart
	 * @param path
	 *           the path of the block that shall be replaced
	 * @param model
	 *           the corresponding model
	 * @return The new chartBlock
	 */
	public ChartBlock replaceBlockByChartBlock(StateflowChart chart, String path, Model model) {

		Block correspondingBlock = model.getBlockByPath(path, false);

		ChartBlock cb = new ChartBlock();

		if (correspondingBlock == null) {
			MeMoPlugin.err.println("[Error] Corresponding block " + path + " to chart "
					+ chart.getName() + " not found");
			return cb;
		}

		// copy data
		cb.setId(correspondingBlock.getId());
		cb.setLevel(correspondingBlock.getLevel());
		cb.setParent(correspondingBlock.getParent());
		cb.setBlockSection(correspondingBlock.getBlockSection());
		cb.setName(correspondingBlock.getName());

		cb.setType(correspondingBlock.getType());

		cb.setInPortsMap(correspondingBlock.getInPortsMap());
		cb.setOutPortsMap(correspondingBlock.getOutPortsMap());
		cb.setlConnPortsMap(correspondingBlock.getlConnPortsMap());
		cb.setrConnPortsMap(correspondingBlock.getrConnPortsMap());

		cb.setEnablePort(correspondingBlock.getEnablePort());
		cb.setTriggerPort(correspondingBlock.getTriggerPort());
		cb.setStatePort(correspondingBlock.getStatePort());
		cb.setIfactionPort(correspondingBlock.getIfactionPort());

		cb.setDefaultParamters(correspondingBlock.getDefaultParamters());

		cb.setParameter(correspondingBlock.getParameter());

		cb.setParentModel(correspondingBlock.getParentModel());

		// reassign lines
		cb.setInSignals(correspondingBlock.getInSignals());
		cb.setOutSignals(correspondingBlock.getOutSignals());

		for (SignalLine l : correspondingBlock.getInSignals()) {
			if (l.getDstBlock() == correspondingBlock) {
				l.setDstBlock(cb);
			}
		}

		for (SignalLine l : correspondingBlock.getOutSignals()) {
			if (l.getSrcBlock() == correspondingBlock) {
				l.setSrcBlock(cb);
			}
		}

		// replace old block
		model.getBlockMap().remove(correspondingBlock.hashCode());
		model.getBlockMap().put(cb.hashCode(), cb);

		// replace parent references, there are a few, for example the s-function
		// block
		for (Block child : model.getBlockMap().values()) {
			if (child.getParent() == correspondingBlock) {
				child.setParent(cb);
			}
		}

		// add the new data
		cb.setChart(chart);
		chart.getChartBlocks().add(cb);

		return cb;

	}

	/**
	 * If a new model shall be parsed, old id's of the BlockDefault's can
	 * conflict with new generated id's and the stored default values may be not
	 * correct. This deletes all stored defaults.
	 */
	public void clear() {
		modelname2defaults = new HashMap<String, HashMap<String, BlockDefault>>();
	}

}
