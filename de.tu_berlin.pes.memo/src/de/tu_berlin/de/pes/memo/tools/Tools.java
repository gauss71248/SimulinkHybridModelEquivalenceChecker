package de.tu_berlin.de.pes.memo.tools;

import java.util.HashSet;
import java.util.Set;

import de.tu_berlin.de.pes.memo.tools.sequence.Sequence;
import de.tu_berlin.de.pes.memo.tools.tree.TreeNode;
import de.tu_berlin.de.pes.memo.tools.tree.TreeNodeIter;
import de.tu_berlin.pes.memo.MeMoPlugin;
//import de.tu_berlin.pes.memo.slicing.pathconditions.sequence.Sequence;

/**
 * class containing tools needed across all CISMO packages
 *
 * @author marcus
 *
 */
public class Tools {
	/**
	 * returns the input string with enclosing ANSI terminal commands to color it
	 * according to the second parameter
	 *
	 * @param input_string
	 * @param color
	 *           the color of the return string
	 * @return the input string with enclosing ANSI terminal color commands for
	 *         supported colors
	 */
	static public String colorString(String input_string, String color) {

		// use ANSI terminal commands to color the output
		if (color.equals("red")) {
			return "\u001b[1;31m" + input_string + "\u001b[0m";
		} else if (color.equals("blue")) {
			return "\u001b[1;34m" + input_string + "\u001b[0m";
		} else if (color.equals("green")) {
			return "\u001b[1;32m" + input_string + "\u001b[0m";
		} else if (color.equals("yellow")) {
			return "\u001b[1;33m" + input_string + "\u001b[0m";
		} else {
			return input_string;
		}
	}
	
	static public Set<Sequence> treeToSequence(TreeNode tree) {
		
		Set<Sequence> resultSet = new HashSet<Sequence>();
		
		TreeNodeIter treeIterator = new TreeNodeIter(tree);
		
		while (treeIterator.hasNext()) {
			MeMoPlugin.out.println(treeIterator.next());
			
		}
		
		return resultSet;
	}
}
