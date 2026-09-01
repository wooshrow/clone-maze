//
// SV-COMP property:
//    verification entry point: euler()
//    * INVALID (has bug/s); should throw an assertion error
//    * should not throw run-time exception 
//

//
//This is a modification of an SV-COMP verification task of the same name.
//Below is a copy of the original notice:
//

/*
 * Origin of the benchmark:
 *     repo: https://github.com/elizabethzhenliu/ScientificComputation
 *     branch: master
 *     root directory: .
 * The benchmark was taken from the repo: 8 October 2024
 */
// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2024 The SV-Benchmarks Community
//
// SPDX-License-Identifier: Apache-2.0

package nl.uu.maze.benchmarks.svcomp.float_nonlinear_calculation;


public class Euler {
	
	
	static double myDeriv(double function) {
		double derivative;
		// the function
		// original
		derivative = -9.8 - .002 * (Math.pow(function, 2) / .11);
		// replacing pow(f,2) with just f*f
		//derivative = -9.8 - .002 * (function*function / .11);
		
		// derivative = .1*.02*(1-function);
		return derivative;
	}

	public static boolean euler(double y) {
		double step = .1;
		double n = 10;
		for (int i = 0; i < n; i++) {
			y = y + step * myDeriv(y);
			//System.out.println(y);
		}
		if (y > myDeriv(y)) 
			throw new AssertionError("SVCOMP injected bug found!") ;
		boolean ok = true ;
		return ok ;	
	}

}
