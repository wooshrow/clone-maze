//
// SV-COMP property:
//    verification entry point: check()
//    * INVALID (has bug/s); should throw an assertion error
//    * should not throw run-time exception 
//

//
//This is a modification a file that is part of an SV-COMP verification task.
//Below is a copy of the original notice:
//

// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2013 Alexander von Rhein, University of Passau
// SPDX-FileCopyrightText: 2011-2021 The SV-Benchmarks Community
//
// SPDX-License-Identifier: Apache-2.0

package nl.uu.maze.benchmarks.svcomp.MinePump.spec1_5_product1;

import nl.uu.maze.benchmarks.svcomp.MinePump.spec1_5_product1.MinePumpSystem.Environment;
import nl.uu.maze.benchmarks.svcomp.MinePump.spec1_5_product1.MinePumpSystem.MinePump;

public class Actions {
	
	Environment env;
	  MinePump p;

	  boolean methAndRunningLastTime = false;
	  boolean switchedOnBeforeTS = false;

	  Actions() {
	    env = new Environment();
	    p = new MinePump(env);
	  }

	  void waterRise() {
	    env.waterRise();
	  }

	  void methaneChange() {
	    env.changeMethaneLevel();
	  }

	  void stopSystem() {
	    if (p.isSystemActive()) p.stopSystem();
	  }

	  void startSystem() {
	    if (!p.isSystemActive()) p.startSystem();
	  }

	  void timeShift() {

	    if (p.isSystemActive()) Specification5_1();

	    p.timeShift();

	    if (p.isSystemActive()) {
	      Specification1();
	      Specification2();
	      Specification3();
	      Specification4();
	      Specification5_2();
	    }
	  }

	  String getSystemState() {
	    return p.toString();
	  }

	  // Specification 1 methan is Critical and pumping leads to Error
	  void Specification1() {

	    Environment e = p.getEnv();

	    boolean b1 = e.isMethaneLevelCritical();
	    boolean b2 = p.isPumpRunning();

	    if (b1 && b2) {
	    	throw new AssertionError("SVCOMP injected bug found!") ;
	    }
	  }

	  // Specification 2: When the pump is running, and there is methane, then it is
	  // in switched off at most 1 timesteps.
	  void Specification2() {

	    Environment e = p.getEnv();

	    boolean b1 = e.isMethaneLevelCritical();
	    boolean b2 = p.isPumpRunning();

	    if (b1 && b2) {
	      if (methAndRunningLastTime) {
	    	  throw new AssertionError("SVCOMP injected bug found!") ;
	      } else {
	        methAndRunningLastTime = true;
	      }
	    } else {
	      methAndRunningLastTime = false;
	    }
	  }

	  // Specification 3: When the water is high and there is no methane, then the
	  // pump is on.
	  void Specification3() {

	    Environment e = p.getEnv();

	    boolean b1 = e.isMethaneLevelCritical();
	    boolean b2 = p.isPumpRunning();
	    boolean b3 = e.getWaterLevel() == Environment.WaterLevelEnum.high;

	    if (!b1 && b3 && !b2) {
	    	throw new AssertionError("SVCOMP injected bug found!") ;
	    }
	  }

	  // Specification 4: the pump is never on when the water level is low
	  void Specification4() {

	    Environment e = p.getEnv();

	    boolean b2 = p.isPumpRunning();
	    boolean b3 = e.getWaterLevel() == Environment.WaterLevelEnum.low;

	    if (b3 && b2) {
	    	throw new AssertionError("SVCOMP injected bug found!") ;
	    }
	  }

	  // Specification 5: The Pump is never switched on when the water is below the
	  // highWater sensor.
	  void Specification5_1() {
	    switchedOnBeforeTS = p.isPumpRunning();
	  }

	  // Specification 5: The Pump is never switched on when the water is below the
	  // highWater sensor.
	  void Specification5_2() {

	    Environment e = p.getEnv();

	    boolean b1 = p.isPumpRunning();
	    boolean b2 = e.getWaterLevel() != Environment.WaterLevelEnum.high;

	    if ((b2) && (b1 && !switchedOnBeforeTS)) {
	    	throw new AssertionError("SVCOMP injected bug found!") ;
	    }
	  }
	  
	  private static int cleanupTimeShifts = 2;
	  
	  static public boolean check(boolean[] seqs) {
		  
		  if (seqs == null || seqs.length % 4 != 0)
			  throw new IllegalArgumentException() ;

		  Actions a = new Actions();

		  int counter = 0;
		  while (counter < seqs.length) {
			  boolean action1 = seqs[counter] ;
			  boolean action2 = seqs[counter+1] ;
			  boolean action3 = seqs[counter+2] ;
			  boolean action4 = false;
			  if (!action3) action4 = seqs[counter+3] ;

			  if (action1) {
				  a.waterRise();
			  }

			  if (action2) {
				  a.methaneChange();
			  }

			  if (action3) {
				  a.startSystem();
			  } else if (action4) {
				  a.stopSystem();
			  }

			  a.timeShift();

			  counter = counter+4 ;

		  }

		  for (counter = 0; counter < cleanupTimeShifts; counter++) {
			  a.timeShift();
		  }
		  
		  return true ;

	  }


}
