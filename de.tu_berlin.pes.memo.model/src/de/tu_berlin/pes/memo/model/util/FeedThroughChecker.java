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

package de.tu_berlin.pes.memo.model.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;

import de.tu_berlin.pes.memo.model.impl.Block;
import de.tu_berlin.pes.memo.model.impl.Port;
import de.tu_berlin.pes.memo.model.impl.SignalLine;

public class FeedThroughChecker {

	public enum FeedthroughState {
		DIRECT, NONDIRECT, UNCERTAIN
	};

	private static String[] nonfeedthrough = { "Assertion", "ForIterator", "TransportDelay",
			"UnaryMinus", "UnitDelay", "WhileIterator" };
	private static HashSet<String> nonfeedthroughset = null;

	public static FeedthroughState isFeedthrough(Block startBlock, Block targetBlock) {

		if (nonfeedthroughset == null) {
			nonfeedthroughset = new HashSet<String>();
			for (String entry : nonfeedthrough) {
				nonfeedthroughset.add(entry);
			}
		}

		if (nonfeedthroughset.contains(targetBlock.getType())) {
			return FeedthroughState.NONDIRECT;
		}
		Block b = targetBlock;
		Port p = null;

		for (SignalLine s : targetBlock.getInSignals()) {
			if (s.getSrcBlock() == startBlock) {
				p = s.getDstPort();
			}
		}

		if (p == null) {
			return FeedthroughState.UNCERTAIN;
		}

		try {
			Class<FeedThroughChecker> cls = FeedThroughChecker.class;

			Method meth = cls.getDeclaredMethod("check" + b.getType().replace("-", ""), Block.class,
					Port.class);
			FeedthroughState returnvalue = (FeedthroughState) meth.invoke(null, b, p);
			return returnvalue;

		} catch (NoSuchMethodException e) {
			// The Method does not exist, the block has direct feedthrough
			return FeedthroughState.DIRECT;
		} catch (SecurityException e) {
			// schouldn't happen. Only private methods of this class are called.
			e.printStackTrace();
			return FeedthroughState.UNCERTAIN;
		} catch (IllegalArgumentException e) {
			// schouldn't happen. Only private methods of this class are called
			// with parameter Block and Port
			e.printStackTrace();
			return FeedthroughState.UNCERTAIN;
		} catch (IllegalAccessException e) {
			// schouldn't happen. Only private methods of this class are called.
			e.printStackTrace();
			return FeedthroughState.UNCERTAIN;
		} catch (InvocationTargetException e) {
			// Exception thrown in invoked method?
			if (e.getCause() != null) {
				e.getCause().printStackTrace();
			} else { // shouldn't happen, InvocationTargetException is thrown IFF
				// the underlying method throws an exception.
				e.printStackTrace();
			}
			return FeedthroughState.UNCERTAIN;
		}
	}

	// Only when the leading numerator coefficient is not equal to zero
	@SuppressWarnings("unused")
	private static FeedthroughState checkDiscreteFilter(Block b, Port p) {
		String numerator = b.getParameter("Numerator");
		numerator = numerator.substring(1, numerator.length() - 1); // remove []
		return !paramEqualsNull(numerator.split("\\x20")[0]) ? FeedthroughState.DIRECT
				: FeedthroughState.NONDIRECT;
	}

	// Only if D != 0
	@SuppressWarnings("unused")
	private static FeedthroughState checkDiscreteStateSpace(Block b, Port p) {
		return !paramEqualsNull(b.getParameter("D")) ? FeedthroughState.DIRECT
				: FeedthroughState.NONDIRECT;
	}

	// Only when the leading numerator coefficient is not equal to zero and the
	// numerator order equals the denominator order
	@SuppressWarnings("unused")
	private static FeedthroughState checkDiscreteTransferFcn(Block b, Port p) {
		String numerator = b.getParameter("Numerator");
		String denominator = b.getParameter("Denominator");

		numerator = numerator.substring(1, numerator.length() - 1); // remove []
		denominator = denominator.substring(1, denominator.length() - 1); // remove
		// []

		String[] numerators = numerator.split("\\x20");
		String[] denominators = denominator.split("\\x20");

		if (paramEqualsNull(numerators[0])) {
			return FeedthroughState.NONDIRECT;
		}
		// TODO: check order
		return FeedthroughState.DIRECT;
	}

	// Yes, if the number of zeros and poles are equal
	@SuppressWarnings("unused")
	private static FeedthroughState checkDiscreteZeroPole(Block b, Port p) {
		String zero = b.getParameter("Zeros");
		String pole = b.getParameter("Poles");

		zero = zero.substring(1, zero.length() - 1); // remove []
		pole = pole.substring(1, pole.length() - 1); // remove []

		String[] zeros = zero.split("\\x20");
		String[] poles = pole.split("\\x20");

		if (zeros.length == poles.length) {
			return FeedthroughState.DIRECT;
		}

		return FeedthroughState.NONDIRECT;
	}

	// Yes, of the reset and external initial condition source ports.
	// The input has direct feedthrough for every integration method except
	// Forward Euler and accumulation Forward Euler.
	@SuppressWarnings("unused")
	private static FeedthroughState checkDiscreteIntegrator(Block b, Port p) {
		if (p.getNumber() > 1) {
			return FeedthroughState.DIRECT;
		}

		if (b.getParameter("IntegratorMethod").equals("Integration: Forward Euler")
				|| b.getParameter("IntegratorMethod").equals("Accumulation: Forward Euler")) {
			return FeedthroughState.NONDIRECT;
		}

		return FeedthroughState.DIRECT;
	}

	// Yes, of the reset and external initial condition source ports
	@SuppressWarnings("unused")
	private static FeedthroughState checkIntegrator(Block b, Port p) {
		if (p.getNumber() < 1) {
			return FeedthroughState.DIRECT;
		}
		return FeedthroughState.NONDIRECT;
	}

	// Depends on the MATLAB S-function
	@SuppressWarnings("unused")
	private static FeedthroughState checkMSFunction(Block b, Port p) {
		return FeedthroughState.UNCERTAIN;
	}

	// No, except when you select Direct feedthrough of input during
	// linearization
	@SuppressWarnings("unused")
	private static FeedthroughState checkMemory(Block b, Port p) {
		return "on".equals(b.getParameter("LinearizeMemory")) ? FeedthroughState.DIRECT
				: FeedthroughState.NONDIRECT;
	}

	// If Single output/update function is enabled (the default),
	// a Model block is a direct feedthrough block regardless of the
	// structure of the referenced model.
	// If Single output/update function is disabled, a Model block
	// may or may not be a direct feedthrough block, depending on the
	// structure of the referenced model.
	@SuppressWarnings("unused")
	private static FeedthroughState checkModelReference(Block b, Port p) {
		// TODO: Get the model reference block?!
		return FeedthroughState.UNCERTAIN;
	}

	// No, for slow-to-fast transitions for which you select the Ensure data
	// integrity
	// during data transfer check box. Yes, otherwise.
	@SuppressWarnings("unused")
	private static FeedthroughState checkRateTransition(Block b, Port p) {

		if (!"on".equals(b.getParameter("Integrity"))) {
			return FeedthroughState.DIRECT;
		}

		return FeedthroughState.UNCERTAIN;
	}

	// Depends on contents of S-function
	@SuppressWarnings("unused")
	private static FeedthroughState checkSFunction(Block b, Port p) {
		return FeedthroughState.UNCERTAIN;
	}

	// Only if D != 0
	@SuppressWarnings("unused")
	private static FeedthroughState checkStateSpace(Block b, Port p) {
		return !paramEqualsNull(b.getParameter("D")) ? FeedthroughState.DIRECT
				: FeedthroughState.NONDIRECT;
	}

	// Only if the lengths of the Numerator coefficients and Denominator
	// coefficients parameters are equal
	@SuppressWarnings("unused")
	private static FeedthroughState checkTransferFcn(Block b, Port p) {
		String numerator = b.getParameter("Numerator");
		String denominator = b.getParameter("Denominator");

		numerator = numerator.substring(1, numerator.length() - 1); // remove []
		denominator = denominator.substring(1, denominator.length() - 1); // remove
		// []

		String[] numerators = numerator.split("\\x20");
		String[] denominators = denominator.split("\\x20");

		return numerators.length == denominators.length ? FeedthroughState.DIRECT
				: FeedthroughState.NONDIRECT;
	}

	// Yes, of the time delay (second) input
	@SuppressWarnings("unused")
	private static FeedthroughState checkVariableTransportDelay(Block b, Port p) {
		return p.getNumber() > 1 ? FeedthroughState.DIRECT : FeedthroughState.NONDIRECT;
	}

	// For all math operations except Ts and 1/Ts
	@SuppressWarnings("unused")
	private static FeedthroughState checkSampleTimeMath(Block b, Port p) {
		String operation = b.getParameter("TsampMathOp");
		return operation.equals("Ts Only") || operation.equals("1/Ts Only") ? FeedthroughState.NONDIRECT
				: FeedthroughState.DIRECT;
	}

	// Yes, if the number of zeros and poles are equal
	@SuppressWarnings("unused")
	private static FeedthroughState checkZeroPole(Block b, Port p) {
		String zero = b.getParameter("Zeros");
		String pole = b.getParameter("Poles");

		zero = zero.substring(1, zero.length() - 1); // remove []
		pole = pole.substring(1, pole.length() - 1); // remove []

		String[] zeros = zero.split("\\x20");
		String[] poles = pole.split("\\x20");

		if (zeros.length == poles.length) {
			return FeedthroughState.DIRECT;
		}

		return FeedthroughState.NONDIRECT;
	}

	@SuppressWarnings("unused")
	private static FeedthroughState checkSubSystem(Block b, Port p) {

		// TODO: Check\nDiscrete Gradient, Check \nDynamic Gap, Check \nDynamic
		// Range,
		// Check \nStatic Gap, Check \nStatic Range, Check Dynamic \nLower Bound,
		// Check Dynamic \nUpper Bound,
		// Check Input \nResolution, Check Static \nLower Bound, Check Static
		// \nUpper Bound

		// First-Order\nHold, Function-Call\nGenerator

		// PID Controller, PID Controller (2DOF)

		// Tapped Delay

		return FeedthroughState.UNCERTAIN;
	}

	private static boolean paramEqualsNull(String param) {
		return Double.parseDouble(param) == 0.0; // XXX: check double value by ==
		// ?
	}

}
