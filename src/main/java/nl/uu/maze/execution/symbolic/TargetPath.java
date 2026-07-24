package nl.uu.maze.execution.symbolic;

import nl.uu.maze.util.HCFG.HCFGPath;

public class TargetPath {
	
	public static enum TargetPathStatus { 
		HAS_NO_TARGET, 
		APPROACHING_TARGET,
		TARGET_COVERED,
		TARGET_PARTIALLY_COVERED } ;

    public TargetPathStatus status ;
	public HCFGPath targetpath ;
	
	public TargetPath() { 
		status = TargetPathStatus.HAS_NO_TARGET ;
	}
	
	public TargetPath(HCFGPath sigma) {
	   targetpath = sigma ;
	   status = null ;
	}
	
}
