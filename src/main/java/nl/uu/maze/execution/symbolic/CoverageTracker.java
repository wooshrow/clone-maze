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

import sootup.core.graph.StmtGraph;
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
     * This tracks all covered branches, which may include branches which are not
     * targeted. When a test does not produce new targeted coverage, but still
     * produce new untargeted coverage, the engine may decide to still generate 
     * that test. 
     */
    private Set<Integer> coveredRawBranches ;
    
    /**
     * This tracks the statements that have been visited/covered during exploration 
     * as MAZE searches for tests to generate. Note that this is different from coverage by 
     * actual tests (by the generated tests) {@link #targetStmts}. A statement can be covered 
     * during the exploration, but remains uncovered by test if no test is generated 
     * that execute that statement. This can happen if for example all program paths 
     * that lead out from that statement turn out to be infeasible.
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
        coveredRawBranches = new HashSet<>() ;
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
    	
    	// adding branch-targets; we will only incluce branches from branching
    	// instructions as targets. In particular, exceptional jumps are not
    	// targeted in this implementation
    	for (Stmt S : stmts) {
    		var succs = cfg.getAllSuccessors(S) ;
    		for (var nextS : succs) {
    			Integer hash = BranchHistoryUtil.getBranchHash(cfg,S,nextS,true) ; // only branching instructions
    			if (hash != null) {
    				targetBranches.add(hash) ;
        			stillOpenBranchTargets.add(hash) ;
    			}
    		}
    	}
    	
    	/*
    	System.out.println(method.getSignature());
	    System.out.println(method.getBody());
    	System.out.println(">>> " + method.getName() +  ", #stmts:" + method.getBody().getStmts().size()) ;
        for (var stmt : method.getBody().getStmts()) {
        	System.out.println("       " + stmt.toString()) ;
        }
        */
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
    
    
    public boolean registerPathCoveredByTesting(InstructionHistory ihist) {
    	
    	boolean hasNewCoverage = false ; 
    	Stmt prevStmt = null ;
    	StmtGraph<?> currentCfg = null ;
    	
    	for (var hi : ihist.getHistory()) {
    		if (hi instanceof InstructionHistory.MethodSwitchItem) {
    			prevStmt =  null ;
    			currentCfg = ((InstructionHistory.MethodSwitchItem) hi).method.getBody().getStmtGraph() ;
    			continue ;
    		}
    		var hi_ = (InstructionHistory.InstructionItem) hi ;
    		Stmt stmt = hi_.stmt ;
    		boolean changed = stillOpenStmtTargets.remove(stmt) ;
    		if (changed) hasNewCoverage = true ;
    		
    		if (prevStmt != null) {
    			Integer branch = BranchHistoryUtil.getBranchHash(currentCfg,prevStmt,stmt,false) ;
    			if (branch != null) {
    				changed = stillOpenBranchTargets.remove(branch) ;
    				if (changed) hasNewCoverage = true ;
    				changed = coveredRawBranches.add(branch) ;
    				if (changed) hasNewCoverage = true ;
    			}
    		}
    		prevStmt = stmt ;
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
    
    public int numberOfCoveredUntargetedBrances() {
    	return (int) coveredRawBranches.stream().filter(br -> ! targetBranches.contains(br)).count() ;
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
