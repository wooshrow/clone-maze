package nl.uu.maze.execution.symbolic;

import nl.uu.maze.util.HCFG.HCFGPath;

/**
 * Representing a target path over {@link} a high level CFG ({@link nl.uu.maze.util.HCFG})
 * that a symbolic execution seeks to cover. 
 */
public class TargetPath {
	
	public static enum TargetPathStatus { 
		TARGET_COVERED,
		TARGET_PARTIALLY_COVERED,
		APPROACHING_TARGET,
		HAS_NO_TARGET } 

	/**
	 * The status of the {@link #targetpath}:
	 * 
	 * <ol>
	 * <li>TARGET_COVERED : its is already covered by a symbolic execution that is under consideration.
	 * 
	 * <li>TARGET_PARTIALLY_COVERED: when the symbolic execution under consideration ends in some prefix of
	 *                       the target path, but not yet completely covers the target path.
	 *                       
	 * <li>APPROACHING_TARGET: when the execution under consideration has not yet reach the head of the
	 *                       target path, but still has a possibility to reach it.
	 *                       
	 * <li>HAS_NO_TARGET: when last state s of the execution under consideration belongs to a method m,
	 *                    but there is no target path in m that is reachable from s.                                      
	 * </ol>
	 */
    public TargetPathStatus status ;
    
    /**
     * The target path to go after.
     */
	public HCFGPath targetpath ;
	
	/**
	 * Some estimated distance towards reaching the target path, if its head has
	 * not reached yet, or distance towards the current method's exit if the 
	 * target path has been covered, or if there is no target path reachable 
	 * from the current state.
	 */
	public float hdist = Float.POSITIVE_INFINITY ;
	
	public TargetPath() { 
		status = TargetPathStatus.HAS_NO_TARGET ;
	}
	
	public TargetPath(HCFGPath sigma) {
	   targetpath = sigma ;
	   status = null ;
	}
	
	
	/**
	 * Create a new instance of this class, which is a shallow copy of the given T.
	 * The field {@link #targetpath} is shallow-copied.
	 */
	public TargetPath(TargetPath T) {
		this() ;
		targetpath = T.targetpath ;
		status = T.status ;
		hdist = T.hdist ;
	}
}
