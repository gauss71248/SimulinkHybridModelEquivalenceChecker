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
//   (c) Copyright 2010-2012 PES Software Engineering for Embedded Systems, TU Berlin
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

package de.tu_berlin.pes.memo.parser.stateflow.datatypes;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents Expressions in relation with an operator. The operator
 * can be an unary or binary type.
 *
 * @author Joachim Kuhnert
 */
public class SubExpression extends Expression {

	/** The operator */
	Operator operat;
	/** All operands related to the operator */
	List<Expression> operands;

	public List<Expression> getOperands() {
		return operands;
	}

	/**
	 * Creates a expression with an unary operation
	 *
	 * @param op
	 *           The operator of the Expression
	 * @param operand
	 *           The Expression the operator affects
	 */
	public SubExpression(Operator op, Expression operand) {
		this.operat = op;
		ArrayList<Expression> list = new ArrayList<Expression>();
		list.add(operand);
		this.operands = list;
	}

	/**
	 * Creates a expression with an binary operation
	 *
	 * @param op
	 *           The operator of the Expression
	 * @param operand1
	 *           The first Expression the operator affects
	 * @param operand2
	 *           The second Expression the operator affects
	 */
	public SubExpression(Operator op, Expression operand1, Expression operand2) {
		this.operat = op;
		ArrayList<Expression> list = new ArrayList<Expression>();
		list.add(operand1);
		list.add(operand2);
		this.operands = list;
	}

	/**
	 * @return The first operand
	 */
	public Expression getLeft() {
		return operands.get(0);
	}

	/**
	 * @return the second operand
	 */
	public Expression getRight() {
		if (!isUnary()) {
			return operands.get(1);
		} else {
			return null;
		}
	}

	/**
	 * @return If more than one operand is given.
	 */
	public boolean isUnary() {
		return operands.size() == 1;
	}

	/**
	 * @return false, if the operator changes the value of one of its operands
	 */
	public boolean isNonModifing() {
		return (operat == Operator.EQEQ) || (operat == Operator.LTEQ) || (operat == Operator.GTEQ)
				|| (operat == Operator.LT) || (operat == Operator.GT) || (operat == Operator.AND)
				|| // FIXME find Parameter specifying if & is bit shift
				(operat == Operator.ANDAND) || (operat == Operator.OROR) || (operat == Operator.OR)
				|| (operat == Operator.NOT) || (operat == Operator.DOT);
	}

	/**
	 * @return The operator of the expression.
	 */
	public Operator getOperator() {
		return operat;
	};

	@Override
	public String toString() {
		String result = operat + "(";
		for (Expression exp : operands) {
			result += exp.toString() + ", ";
		}
		result = result.substring(0, result.length() - 2);
		result += ")";
		return result;
	}

}
