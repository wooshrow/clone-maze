package nl.uu.maze.execution.symbolic;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.uu.maze.execution.DSEController;
import nl.uu.maze.execution.EngineConfiguration;
import nl.uu.maze.util.BranchStmtUtil;
import nl.uu.maze.util.HCFG;
import nl.uu.maze.util.HCFG.HCFGPath;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
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
     * The statements of the class under test. 
     */
    private Set<Stmt> targetStmts ;
    
    /**
     * The branches of the class under test. 
     */
    private Set<Integer> targetBranches ;
    
    /**
     * This tracks all Jimple instructions that have been covered by test. These include
     * instructions that were not included as targets. When a test does not cover
     * a new target, but does cover a new instruction, the engine may decide to keep
     * that test.
     */
    private Set<Stmt> coveredStmts ;
    
    /**
     * This tracks all branches that have been covered by test. These include
     * branches that were not included as targets. When a test does not cover
     * a new target, but does cover a new branch, the engine may decide to keep
     * that test.
     */
    private Set<Integer> coveredBranches ;
    
    /**
     * This tracks the statements that have been visited/covered during exploration 
     * as MAZE searches for tests to generate. Note that this is different from coverage by 
     * actual tests (by the generated tests) {@link #coveredStmts}. A statement can be covered 
     * during the exploration, but remains uncovered by test if no test is generated 
     * that execute that statement. This can happen if for example all program paths 
     * that lead out from that statement turn out to be infeasible.
     */
    private Set<Stmt> coveredStmts_byExpl;
    
    
    Map<HCFG, List<HCFGPath>> targetPaths = new HashMap<>() ;
    Map<HCFG, List<HCFGPath>> stillUncoveredTargetPaths = new HashMap<>() ;
     
    /**
     * The statements (across the whole CUT) which are the head of an exception handler.
     * We store them so we can know when to record their visit into the branch history.
     */
    private Set<Stmt> exceptionHandlerHeads = new HashSet<>() ;
    
    /**
     * The statements (across the whole CUT) which are exit stmt of a method in the CUT.
     * We store them so we can know when to record their visit into the branch history.
     */
    private Set<Stmt> exitStmts = new HashSet<>() ;

    private CoverageTracker() {
        // Use identity hash map to avoid potentially expensive equals() calls on
        // statements (which are unique by reference, so reference equality suffices)
        targetStmts     = Collections.newSetFromMap(new IdentityHashMap<>()); 
        coveredStmts    = Collections.newSetFromMap(new IdentityHashMap<>()); 
        coveredStmts_byExpl = Collections.newSetFromMap(new IdentityHashMap<>());
        
        targetBranches  = new HashSet<>() ; 
        coveredBranches = new HashSet<>() ; 
    }
    
    /**
     * Register coverage targets, given a target method.
     */
    public void addTargets(JavaSootMethod method) {
    	var cfg = method.getBody().getStmtGraph() ;
    	var stmts = method.getBody().getStmts() ;
    	targetStmts.addAll(stmts) ;
    	//System.out.println("    after add #targets=" + targetStmts.size() + ", #open=" + stillOpenTargets.size()) ;
    	
    	// adding branch-targets; we will only incluce branches from branching
    	// instructions as targets. In particular, exceptional jumps are not
    	// targeted in this implementation
    	for (Stmt S : stmts) {
    		var succs = cfg.getAllSuccessors(S) ;
    		for (var nextS : succs) {
    			Integer hash = BranchStmtUtil.getBranchHash(cfg,S,nextS,true) ; // only branching instructions
    			if (hash != null) {
    				targetBranches.add(hash) ;
    			}
    		}
    	}
    	
    	HCFG hcfg = new HCFG(method) ;
    	System.out.println(">>> HCFG " + hcfg) ;
    	try {
    		hcfg.saveAsDot(null);
    	}
    	catch(Exception e) { }
    	// for now we'll target edge-pairs:
    	int k = EngineConfiguration.getInstance().pathLengthCoverage ;
    	if (k>=1) {
    		var targets = hcfg.getMaxElementaryPaths2(k) ;
    		targetPaths.put(hcfg, targets) ;
        	List<HCFGPath> targets__ = new LinkedList<>() ;
        	targets__.addAll(targets) ;
        	stillUncoveredTargetPaths.put(hcfg, targets__) ;
        	
        	for (var nd : hcfg.nodes) {
        		if (HCFG.isExitNode(nd)) {
        			exitStmts.add(nd.label) ;
        		}
        		if (HCFG.isExceptionHandlerHead(nd)) {
        			exceptionHandlerHeads.add(nd.label) ;
        		}
        	}
    	}
    	
    	
    	System.out.println(">>>> #targets = " + this.numberOfTargetPaths()) ;
        for (var T : targetPaths.entrySet()) {
        	System.out.println("   * " + T.getKey().method.getName() + ": " + T.getValue()) ;
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
    
    public boolean isStmtCovered(Stmt stmt) {
        return coveredStmts.contains(stmt);
    }
    
    public boolean isExitNode(Stmt stmt) {
    	return exitStmts.contains(stmt) ;
    }
    
    public boolean isExceptionHandlerHead(Stmt stmt) {
    	return exceptionHandlerHeads.contains(stmt) ;
    }

    
    /**
     * Determine which targets are covered by the given test-execution, and then
     * register the covered targets. The test execution is represented as
     * an instruction history (the list of Jimple instructions/stmt passed during
     * the test.
     * <p>The method returns true if the execution an item not covered before, and
     * else it returns false.
     */
    public boolean registerCoveregeByTesting(SymbolicState state, InstructionHistory ihist) {
    	
    	boolean hasNewCoverage = false ; 
    	Stmt prevStmt = null ;
    	StmtGraph<?> currentCfg = null ;
    	
    	// register passed stmts and branches in ihist:
    	for (var hi : ihist.getHistory()) {
    		if (hi instanceof InstructionHistory.MethodSwitchItem) {
    			prevStmt =  null ;
    			currentCfg = ((InstructionHistory.MethodSwitchItem) hi).method.getBody().getStmtGraph() ;
    			continue ;
    		}
    		var hi_ = (InstructionHistory.InstructionItem) hi ;
    		Stmt stmt = hi_.stmt ;
    		boolean changed = coveredStmts.add(stmt) ;
    		if (changed) hasNewCoverage = true ;
    		
    		if (prevStmt != null) {
    			Integer branch = BranchStmtUtil.getBranchHash(currentCfg,prevStmt,stmt,false) ;
    			if (branch != null) {
    				changed = coveredBranches.add(branch) ;
    				if (changed) hasNewCoverage = true ;
    			}
    		}
    		prevStmt = stmt ;
    	}
    	
    	// register target paths covered by state.branchhistory; only relevant for
    	// k>=1:
    	if (EngineConfiguration.getInstance().pathLengthCoverage >= 1) {
    		var sigma = state.getBranchHistory() ;
        	for (var Z : this.stillUncoveredTargetPaths.entrySet()) {
        		var targets = Z.getValue() ;
        		List<HCFGPath> covered = new LinkedList<>() ;
            	for (var tau : targets) {
        		   	if (tau.coverBy(sigma) == 0) {
        		   		covered.add(tau) ;
        		   	}
        		}
            	if (covered.size() > 0 && EngineConfiguration.getInstance().minimalisticTestSuite) {
            		hasNewCoverage = true ;
            	}
            	targets.removeAll(covered) ;
        	}
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
    
    public int numberOfTargetPaths() {
    	int n = 0 ;
    	for (var T : targetPaths.values()) {
    		n += T.size() ;
    	}
    	return n ;
    }
    
    public int numberOfStillUncoveredTargetPaths() {
    	int m = 0 ;
    	for (var T : stillUncoveredTargetPaths.values()) {
    		m += T.size() ;
    	}
    	return m ;
    }
    
    public HCFG getHCFG(JavaSootMethod method) {
    	for (var hcfg : this.targetPaths.keySet()) {
    		if (hcfg.method == method) {
    			return hcfg ;
    		}
    	}
    	return null ;
    }
    
    
    /**
     * Get the number of target statements that are still uncovered by testing.
     */
    public int numberOfStillUnCoveredStmts() {
    	return (int) targetStmts.stream().filter(stmt -> ! coveredStmts.contains(stmt)).count() ;
    }
    
    public int numberOfStillUnCoveredBranches() {
    	return (int) targetBranches.stream().filter(br -> ! coveredBranches.contains(br)).count() ;
    }
    
    public int numberOfCoveredUntargetedBrances() {
    	return (int) coveredBranches.stream().filter(br -> ! targetBranches.contains(br)).count() ;
    }
    
    
    public boolean allCoverageTargetsCompleted() {
    	return this.numberOfStillUnCoveredBranches() == 0
    			&& this.numberOfStillUnCoveredStmts() == 0
    			&& this.numberOfStillUncoveredTargetPaths() == 0 ;
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
        coveredStmts_byExpl.clear();
        coveredBranches.clear(); 
        stillUncoveredTargetPaths.clear(); 
        exitStmts.clear(); 
        exceptionHandlerHeads.clear(); 
    }
}
