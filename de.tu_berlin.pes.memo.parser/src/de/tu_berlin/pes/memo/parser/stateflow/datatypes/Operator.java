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

/**
 * These Operators are used by the parser. Thats why some symbols appear
 * multiple times. So can "*" be the multiplication operator or the pointer
 * operator.
 *
 * @author Joachim Kuhnert
 */
public enum Operator {
	/* Arithmetic Operators */
	PLUS, // +
	MINUS, // -
	MULT, // *
	DIV, // /
	MOD, // %

	/* Logical Operators */
	AND, // &
	OR, // |
	NOT, // ~ or !

	/* binary Operators */
	OROR, // ||
	ANDAND, // &&
	XOR, // ^
	LSHIFT, // <<
	RSHIFT, // >>

	/* unary Operators */
	UMINUS, // -exp
	UPLUS, // +exp
	PREINC, // ++exp
	POSTINC, // exp++
	PREDEC, // --exp
	POSTDEC, // exp--

	/* Comparsion Operators */
	EQEQ, // ==
	NOTEQ, // != or ~=
	LT, // <
	GT, // >
	LTEQ, // <=
	GTEQ, // >=

	/* Assignment Operators */
	ASS, // =
	PLUSASS, // +=
	MINUSASS, // -=
	MULTASS, // *=
	DIVASS, // /=
	MODASS, // %=
	ORASS, // |=
	ANDASS, // &=
	XORASS, // ^=

	/* Other */
	ADDR, // &identifier
	POINTER, // *identifier
	ARROW, // ->
	DOT // id.id
}
