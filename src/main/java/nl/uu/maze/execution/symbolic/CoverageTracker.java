package nl.uu.maze.execution.symbolic;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.uu.maze.execution.DSEController;
import nl.uu.maze.util.BranchHistoryUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import sootup.core.jimple.common.stmt.Stmt;
import sootup.java.core.JavaSootMethod;

/**
 * Tracks the coverage of statements during symbolic execution.
 */
public class CoverageTracker {
	
	private static final Logger logger = LoggerFactory.getLogger(CoverageTracker.class);
	
    private static CoverageTracker instance;

    public static CoverageTracker getInstance() {
        if (instance == null) {
            instance = new CoverageTracker();
        }
        return instance;
    }
    
    /**
     * The statements of target methods under tests that we aim to cover. This refers
     * to coverage by generated tests. In particular, coverage simply by exploration
     * during the search for tests does not count. 
     */
    private Set<Stmt> targetStmts ;
    
    private Set<Integer> targetBranches ;
    
    /**
     * Targets among {@link #targetStmts} that are still not covered by generated tests.
     */
    private Set<Stmt> stillOpenStmtTargets ;
    
    private Set<Integer> stillOpenBranchTargets ;
    
    /**
     * This tracks the statements that have been visited/covered during exploration 
     * as MAZE searches for tests to generate. Note that this is different from coverage by 
     * actual tests (by the generated tests). A statement can be covered during the exploration, 
     * but it might remain uncovered by test, if no test is generated that execute that 
     * statement. This can happen if for example all program paths that lead out from 
     * that statement turn out to be infeasible.
     * 
     * <p>Tracking search-time/exploration-time coverage is relevant for some search strategies.
     */
    private final Set<Stmt> coveredStmts_byExpl;

    private CoverageTracker() {
        // Use identity hash map to avoid potentially expensive equals() calls on
        // statements (which are unique by reference, so reference equality suffices)
        coveredStmts_byExpl = Collections.newSetFromMap(new IdentityHashMap<>());
        targetStmts  = Collections.newSetFromMap(new IdentityHashMap<>());
        stillOpenStmtTargets  = Collections.newSetFromMap(new IdentityHashMap<>());
        
        targetBranches = new HashSet<>() ;
        stillOpenBranchTargets = new HashSet<>() ;
    }
    
    /**
     * Register coverage targets, given a target method.
     */
    public void addTargets(JavaSootMethod method) {
    	var cfg = method.getBody().getStmtGraph() ;
    	var stmts = method.getBody().getStmts() ;
    	//System.out.println("### BEFORE ADD #targets=" + targetStmts.size() + ", #open=" + stillOpenTargets.size()) ;
    	targetStmts.addAll(stmts) ;
    	stillOpenStmtTargets.addAll(stmts) ;
    	//System.out.println("    after add #targets=" + targetStmts.size() + ", #open=" + stillOpenTargets.size()) ;
    	
    	// adding branch-targets:
    	for (Stmt S : stmts) {
    		int N = cfg.getAllSuccessors(S).size();
    		if (N<2) continue ;
    		for (int i=0 ; i<N ; i++) {
    			int hash = BranchHistoryUtil.getBranchHash(S,i) ;
    			targetBranches.add(hash) ;
    			stillOpenBranchTargets.add(hash) ;
    		}
    	}
    	
    	System.out.println(">>> " + method.getName() +  ", #stmts:" + method.getBody().getStmts().size()) ;
        for (var stmt : method.getBody().getStmts()) {
        	System.out.println("       " + stmt.toString()) ;
        }
    	
    	//System.out.println(">>> " + method.getName() + " branches " + targetStmts.size() + " : " + stillOpenBranchTargets) ;
    	
    }

    /**
     * Marks a statement as covered by exploration during the search to come up with tests.
     * 
     * @return {@code true} if the statement was not covered before, {@code false}
     *         otherwise
     */
    public boolean registerStmtCovered_byExpl(SymbolicState state, Stmt stmt) {
        boolean  newlyCovered = coveredStmts_byExpl.add(stmt);
        return newlyCovered ;
    }

    /**
     * Checks whether a statement is covered by exploration.
     */
    public boolean isStmtCovered_byExpl(Stmt stmt) {
        return coveredStmts_byExpl.contains(stmt);
    }
    
    /**
     * Register statements executed during a test. A "test" here means a
     * (feasible) symbolic execution leading to a certain end-state (e.g
     * the state at the end of a method, or a state that throws an uncaught
     * exception). This end-state is given as input to this method. It also
     * contains information of branches passed during the execution that leads
     * to that state. From this information, we can reconstruct the sequence
     * of statements/instructions passed along the way.
     * 
     * <p>More precisely, the method register the statements and branches passed
     * for the purpose of coverage tracking.
     * 
     * <p>The method returns true if the test gives new coverage (either statement
     * of branch). 
     */
    public boolean registerPathCoveredByTesting(SymbolicState endstate) {
    	boolean hasNewCoverage = false ;
    	// register visited statements:
		try {
    		List<Stmt> visitedStmts = BranchHistoryUtil.getPathFromBranchHistory(
    			endstate.getBranchHistory2(),
    			endstate.getStmt()) ;
    		for (Stmt S : visitedStmts) {
    			//System.out.println(">>> visited " + endstate.getMethod().getName() + ": " +  S) ;
    			boolean changed = stillOpenStmtTargets.remove(S) ;
    			if (changed) hasNewCoverage = true ;
    		}
    	}
    	catch(Exception e) {
    		logger.error("Something went WRONG with history reconstruction: " + e) ;
    		//System.out.println("### something WENT WRONG with history reconstruction ") ;
    		//e.printStackTrace();
    		// something wrong with reconstructing the stmts-history, 
    		// should log this ...  
    	}
		// register visited branches:
		for (Integer b : endstate.getBranchHistory()) {
			//System.out.println(">>> visit branch " + b) ;
			boolean changed = stillOpenBranchTargets.remove(b) ;
			if (changed) hasNewCoverage = true ;
		}
    	return hasNewCoverage ;
    }
    
    
    /**
     * Get the number of target statements to cover by testing.
     */
    public int numberOfTargetStmts() {
    	return targetStmts.size() ;
    }
    
    public int numberOfTargetBranches() {
    	return targetBranches.size() ;
    }
    
    /**
     * Get the number of target statements that are still uncovered by testing.
     */
    public int numberOfStillUnCoveredStmts() {
    	return stillOpenStmtTargets.size() ;
    }
    
    public int numberOfStillUnCoveredBranches() {
    	return stillOpenBranchTargets.size() ;
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
        coveredStmts_byExpl.clear();
    }
}
