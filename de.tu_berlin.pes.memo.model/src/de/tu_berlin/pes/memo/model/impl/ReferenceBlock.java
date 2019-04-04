package de.tu_berlin.pes.memo.model.impl;

import org.conqat.lib.simulink.builder.MDLSection;

public class ReferenceBlock extends Block {

	private Block referencingBlock = null;

	/**
	 * Standard constructor. Creates an empty reference block with id -1.
	 */
	public ReferenceBlock() {
		super();
	}

	/**
	 * Creates a new block with the given parameters, especially with the given
	 * name. This is important if you substitute a reference block by the block
	 * of the library. Stores the old block that is overwritten by the referenced
	 * block so for examples mask parameters are still available.
	 *
	 * @param id
	 *           the item id
	 * @param level
	 *           how deep buried in subsystems
	 * @param parent
	 *           the overlaying subsystem block hash or 1 if block is on the
	 *           first layer (level = 1)
	 * @param blockSection
	 *           The part of the AST to which the block belongs
	 * @param blockName
	 *           The name the block will have, regardless of the parameter name
	 *           in the block section.
	 */
	public ReferenceBlock(int id, int level, ModelItem parent, Model parentModel,
			MDLSection blockSection, String blockName, Block referencing) {
		super(id, level, parent, parentModel, blockSection, blockName);
		setReferencingBlock(referencing);
	}

	/**
	 * @return the referencingBlock, the Block originally present in the model
	 *         before it was replaced.
	 */
	public Block getReferencingBlock() {
		return referencingBlock;
	}

	/**
	 * @param referencingBlock
	 *           the referencingBlock to set
	 */
	public void setReferencingBlock(Block referencingBlock) {
		this.referencingBlock = referencingBlock;
	}

}
