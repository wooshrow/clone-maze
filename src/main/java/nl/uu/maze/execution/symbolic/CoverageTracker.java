package nl.uu.maze.execution.symbolic;

import java.util.Set;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import sootup.core.jimple.common.stmt.Stmt;

/**
 * Tracks the coverage of statements during symbolic execution.
 */
public class CoverageTracker {
    private static CoverageTracker instance;

    public static CoverageTracker getInstance() {
        if (instance == null) {
            instance = new CoverageTracker();
        }
        return instance;
    }
    
    
    private Set<Stmt> targetStmts ;
    private Set<Stmt> stillOpenTargets ;
    
    /**
     * The statements that have been visited/covered.
     */
    private final Set<Stmt> coveredStmts;

    private CoverageTracker() {
        // Use identity hash map to avoid potentially expensive equals() calls on
        // statements (which are unique by reference, so reference equality suffices)
        coveredStmts = Collections.newSetFromMap(new IdentityHashMap<>());
        targetStmts  = Collections.newSetFromMap(new IdentityHashMap<>());
        stillOpenTargets  = Collections.newSetFromMap(new IdentityHashMap<>());
    }
    
    public void addTargets(List<Stmt> stmts) {
    	//System.out.println("### BEFORE ADD #targets=" + targetStmts.size() + ", #open=" + stillOpenTargets.size()) ;
    	targetStmts.addAll(stmts) ;
    	stillOpenTargets.addAll(stmts) ;
    	//System.out.println("    after add #targets=" + targetStmts.size() + ", #open=" + stillOpenTargets.size()) ;
    }

    /**
     * Marks a statement as covered.
     * 
     * @return {@code true} if the statement was not covered before, {@code false}
     *         otherwise
     */
    public boolean setCovered(SymbolicState state, Stmt stmt) {
    	stillOpenTargets.remove(stmt) ;
        boolean  newlyCovered = coveredStmts.add(stmt);
        return newlyCovered ;
    }

    /**
     * Checks whether a statement is covered.
     */
    public boolean isCovered(Stmt stmt) {
        return coveredStmts.contains(stmt);
    }
    
    
    public int getNumberOfTargetStmtsToCover() {
    	return targetStmts.size() ;
    }
    public int getNumberOfCoveredStmts() {
    	return coveredStmts.size() ;
    }
    
    public int getNumberOfStillUnCoveredTargetStmts() {
    	return stillOpenTargets.size() ;
    }

    /**
     * Resets the coverage tracker.
     * 
     * @apiNote This method need <b>not</b> be called between different methods
     *          under
     *          test for the same class, because test cases for one method can cover
     *          statements in another method as well!
     */
    public void reset() {
        coveredStmts.clear();
    }
}
