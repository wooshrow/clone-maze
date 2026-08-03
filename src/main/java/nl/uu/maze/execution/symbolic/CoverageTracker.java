package nl.uu.maze.execution.symbolic;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.uu.maze.execution.DSEController;
import nl.uu.maze.execution.EngineConfiguration;
import nl.uu.maze.util.BranchStmtUtil;
import nl.uu.maze.util.HCFG;
import nl.uu.maze.util.HCFG.HCFGPath;
import nl.uu.maze.util.IOUtils;

import java.io.IOException;
import java.nio.file.Paths;
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
    
    private EngineConfiguration engineConfig = EngineConfiguration.getInstance() ;

    public static CoverageTracker getInstance() {
        if (instance == null) {
            instance = new CoverageTracker();
        }
        return instance;
    }
    
    /**
     * The statements of CUT's targeted methods.
     */
    private Set<Stmt> targetStmts ;
    
    /**
     * The branches of CUT's targeted methods.
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
    
    /**
     * Tracked target methods and their high-level CFGs. 
     */
    Map<JavaSootMethod,HCFG> hcfgs = new IdentityHashMap<>() ; // use Identity Hash-map to use == instead of equals
    
    /**
     * Tracked target paths to cover.
     */
    Map<JavaSootMethod, List<HCFGPath>> targetPaths = new IdentityHashMap<>() ;
    
    /**
     * The target paths that are not covered yet, and are still considered as feasible
     * by the engine.
     */
    Map<JavaSootMethod, List<HCFGPath>> stillUncoveredTargetPaths = new IdentityHashMap<>() ;
    
    /**
     * Target paths that have been dropped by the engine, e.g. because it thinks they
     * are unfeasible.
     */
    Map<JavaSootMethod, List<HCFGPath>> droppedTargetPaths = new IdentityHashMap<>() ;
    
    
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
    
    /**
     * True, if the coverage information is just updated by a test, e.g. via {@link #registerCoveregeByTesting(SymbolicState, InstructionHistory)}.
     */
    private boolean dirty = false ;

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
    public void addTarget(JavaSootMethod method) {
    	
    	var cfg = method.getBody().getStmtGraph() ;
    	var stmts = method.getBody().getStmts() ;
    	targetStmts.addAll(stmts) ;
    	//System.out.println("    after add #targets=" + targetStmts.size() + ", #open=" + stillOpenTargets.size()) ;
    	
    	// adding branch-targets; we will only incluce branches from branching
    	// instructions as targets. In particular, exceptional jumps are not
    	// targeted in this implementation
    	int oldNumberOfTargetBranches = targetBranches.size() ;
    	for (Stmt S : stmts) {
    		var succs = cfg.getAllSuccessors(S) ;
    		for (var nextS : succs) {
    			Integer hash = BranchStmtUtil.getBranchHash(cfg,S,nextS,true) ; // only branching instructions
    			if (hash != null) {
    				targetBranches.add(hash) ;
    			}
    		}
    	}
    	int addedBranches = targetBranches.size() -  oldNumberOfTargetBranches ;
    	
    	HCFG hcfg = new HCFG(method) ;
    	hcfgs.put(method, hcfg) ;
    	
    	int k = EngineConfiguration.getInstance().pathLengthCoverage ;
    	int numOfTargetPaths = 0 ;
    	if (k==-1 || k>=1) {
    		var targets = hcfg.getMaxElementaryPaths2(k) ;
    		targetPaths.put(method, targets) ;
    		numOfTargetPaths = targets.size() ;
        	List<HCFGPath> targets__ = new LinkedList<>() ;
        	targets__.addAll(targets) ;
        	stillUncoveredTargetPaths.put(method, targets__) ;
        	
        	List<HCFGPath> dropped = new LinkedList<>() ;
        	droppedTargetPaths.put(method, dropped) ;
        	
        	
        	for (var nd : hcfg.nodes) {
        		if (HCFG.isExitNode(nd)) {
        			exitStmts.add(nd.label) ;
        		}
        		if (HCFG.isExceptionHandlerHead(nd)) {
        			exceptionHandlerHeads.add(nd.label) ;
        		}
        	}
    	}
    	
    	logger.info("Added " + method.getName() + " as a target, #stmts:" + method.getBody().getStmts().size()
    			+ ", #branches:" + addedBranches
    			+ (numOfTargetPaths > 0 ? ", #target-k-paths:" + numOfTargetPaths : "")) ;
    	// saving or printing information if configured to:
    	String outpath = engineConfig.outPath == null ? "" : engineConfig.outPath ;
    	if (outpath == null) outpath = "" ;
    	String classname = method.getDeclaringClassType().getClassName() ;
    	String methodname = method.getName() ;
    	
        switch (engineConfig.exportJimple) {
            case -1 :logger.info("Jimple code of " +  method.getName() + "\n" + method.getBody());  break ;
            case 1 : try {
            			String file = Paths.get(outpath, classname + "_" + methodname + ".jimple").toString() ;
            			IOUtils.saveTxtToFile(file, methodname + "\n" + method.getBody());
            		 }
            		 catch(Exception e) {
            			logger.error("Failing to save jimple of " + methodname);
            		 } 
            		 break ;
        }
        
        switch (engineConfig.exportHCFG) {
        	case -1 : logger.info("Dot file\n" + hcfg.asDot()) ; break ;
        	case 1 : try {
        				String file = Paths.get(outpath, classname + "_" + methodname + ".dot").toString() ;
        				hcfg.saveAsDot(file) ;
        			 }
        			 catch(Exception e) {
        				 logger.error("Failing to save HCFG of " + methodname);
        			 }
        			 break ;
        }
        
        if (engineConfig.exportTargetPaths != 0) {
        	String z = "" ;
        	if (numOfTargetPaths == 0) 
        		z = "has no target path" ;
        	else 
        		z = targetPathsToString(this.targetPaths.get(method)) ;
        	
        	switch (engineConfig.exportTargetPaths) {
        	case -1 : logger.info("Target paths:\n" + z) ; break ;
        	case 1 : try {
        				String file = Paths.get(outpath, classname + "_" + methodname + "-targetpaths.txt").toString() ;
        				IOUtils.saveTxtToFile(file,z) ;
         			 }
        			 catch(Exception e) {
        				 logger.error("Failing to save target paths of " + methodname);
        			 }
        			 break ;
            }
        }
    }
    
    private static String targetPathsToString(List<HCFGPath> paths) {
    	StringBuffer buf = new StringBuffer() ;
    	int i = 1 ;
    	for (var sigma : paths) {
    		if (i>1) buf.append("\n") ;
     		buf.append("   " + i + ":  " + sigma.toStringCompact()) ;
        	i++ ;
        }
    	return buf.toString() ;
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
    	
    	// register target paths covered by state.branchhistory and indirect-hist; 
    	// only relevant for k=-1 or k>=1:
    	int k = EngineConfiguration.getInstance().pathLengthCoverage ;
    	if (k == -1 || k >= 1) {

    		BiFunction<JavaSootMethod, List<Integer>,Integer> check = (method,sigma) -> {
    			if (hcfgs.get(method) == null) return 0 ;
    			var targets = this.stillUncoveredTargetPaths.get(method) ;
    			var dropped = this.droppedTargetPaths.get(method) ;
    			int count = 0 ;
    			List<HCFGPath> covered = targets.stream().filter(tau -> tau.coverBy(sigma) == 0).toList() ;
            	targets.removeAll(covered) ;
                count += covered.size() ;
    			// also check dropped-targets:
                covered = dropped.stream().filter(tau -> tau.coverBy(sigma) == 0).toList() ;
            	dropped.removeAll(covered) ;
                count += covered.size() ;
    			
            	return count ;
    		} ;
    		
    		var newcov = check.apply(state.getMethod(),state.getBranchHistory()) ;
            if (newcov > 0) hasNewCoverage = true ;
    		
    		// check indirect path-cov:
    		for (var indirectHist : state.getIndirectBranchHistories()) {
    			HCFG hcfg  = indirectHist.first() ;
    			var sigma = indirectHist.second() ;
    			newcov = check.apply(hcfg.method,sigma) ;
            	if (newcov > 0) hasNewCoverage = true ;
    		}
    		
    		logger.info("Registering a candidate test." 
    				+ (hasNewCoverage ? "+new-COV." : "")
    				+ (numberOfTargetPaths() > 0 ?
    						" Remaining #uncoverared-paths: " + numberOfStillUncoveredTargetPaths()
    						: "")
    				) ;
    	}
    	
    	if (hasNewCoverage) dirty = true ;
    	
    	return hasNewCoverage ;
    }
    
    /**
     * True, if the coverage information is just updated by a test via {@link #registerCoveregeByTesting(SymbolicState, InstructionHistory)}.
     * If it is false, it means that the coverage information has not changed
     * since there has not been a new test being registered. Note that this concerns
     * only test-coverage tracking information. The flag has no bearing towards 
     * exloration-coverage information stored in {@link #coveredStmts_byExpl}.
     */
    public boolean isDirty() {
    	return dirty ;
    }
    
    /**
     * Reset the flag {@link #dirty} to false (so... not dirty). 
     */
    public boolean cleanDirtyFlag() {
    	dirty = false ;
    	return dirty ;
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
    
    public List<HCFGPath> getStillUncoveredTargetPaths(JavaSootMethod method) {
    	return stillUncoveredTargetPaths.get(method) ;
    }
    
    public List<HCFGPath> getDroppedTargetPaths(JavaSootMethod method) {
    	return droppedTargetPaths.get(method) ;
    }
    
    
    public List<HCFGPath> whichTargetPathsAreCovered(List<HCFGPath> Z) {
    	List<HCFGPath> covered = new LinkedList<>() ;
    	for (var sigma : Z) {
    		boolean stillOpen = false ;
    		for (var U : stillUncoveredTargetPaths.values()) {
        		if (U.contains(sigma)) {
        			stillOpen = true ;
        			break ;
        		}
        	}
    		if (stillOpen) continue ;
    		for (var U : droppedTargetPaths.values()) {
        		if (U.contains(sigma)) {
        			stillOpen = true ;
        			break ;
        		}
        	}
    		if (! stillOpen) covered.add(sigma) ;
    	}
    	return covered ;
    }
    
    
    /**
     * Give the number of target paths that are still uncovered and are still considered as
     * feasible.
     */
    public int numberOfStillUncoveredTargetPaths() {
    	int m = 0 ;
    	for (var T : stillUncoveredTargetPaths.values()) {
    		m += T.size() ;
    	}
    	return m ;
    }
    
    /**
     * The number of target paths that were dropped e.g. because they were considered as 
     * unfeasible.
     */
    public int numberOfDroppedTargetPaths() {
    	int m = 0 ;
    	for (var T : droppedTargetPaths.values()) {
    		m += T.size() ;
    	}
    	return m ;
    }
    
    public HCFG getHCFG(JavaSootMethod method) {
    	return hcfgs.get(method) ;    	
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
    
    
    public List<HCFGPath> getAllTargetPaths() {
    	List<HCFGPath> targets = new LinkedList<>() ;
    	for (var T : targetPaths.values()) {
    		targets.addAll(T) ;
    	}
    	return targets ;
    }
    
    /**
     * Mark sigma as "unfeasible". This will remove sigma from the list of 
     * still-uncovered targets, and add it to the list of dropped targets.
     * Note that the engine does not typically check whether sigma is really
     * unfeasible, but it may consider it unfeasible based on some heuristics.
     */
    public boolean markTargetPathUnfeasible(HCFGPath sigma) {
    	for (var T : stillUncoveredTargetPaths.entrySet()) {
    		var targets = T.getValue() ;
    		var removed = targets.remove(sigma) ;
    		if (removed) {
    			// sigma found... remove it, and add it to the drop-list:
    			var method = T.getKey() ;
    			var dropped = droppedTargetPaths.get(method) ;
    			dropped.add(sigma) ;
    			dirty = true ;
    			return true ;
    		}
    	}
    	return false ;
    }
    
    /**
     * If engine EngineConfiguration is set to have pathLengthCoverage is set to 0 (default),
     * this is true when all target branches and stmts are covered.
     * Else, when the config if non-zero, this is true if if the number of still uncovered
     * target paths is 0. This does not count paths that were dropped (e.g. because considered
     * unfeasible).
     */
    public boolean allCoverageTargetsCompleted() {
    	if (engineConfig.pathLengthCoverage != 0) {
    		return this.numberOfStillUncoveredTargetPaths() == 0 ;
    	}
    	return this.numberOfStillUnCoveredBranches() == 0
    			&& this.numberOfStillUnCoveredStmts() == 0 ;
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
        droppedTargetPaths.clear();
        exitStmts.clear(); 
        exceptionHandlerHeads.clear(); 
    }
    
    public String showPathCoverageInfo() {
       	if (this.numberOfTargetPaths() == 0) 
    		return "no target paths were set" ;
 
       	StringBuffer buf = new StringBuffer() ;
    		
    	int k = 0 ;
    	for (var method : this.targetPaths.keySet()) {
    		if (k>0) buf.append("\n") ;
    		var targetted = this.targetPaths.get(method) ;
    		var uncovered = this.stillUncoveredTargetPaths.get(method) ;
    		var dropped = this.droppedTargetPaths.get(method) ;
    		List<HCFGPath> covered = targetted.stream()
    					.filter(sigma -> ! uncovered.contains(sigma) && ! dropped.contains(sigma))
    					.toList() ;
    		
    		buf.append("** method " + method.getName() + " " + covered.size() + "/" + targetted.size() + ". COVERED:\n") ;
    		buf.append(targetPathsToString(covered)) ;
    		buf.append("\n   MISSED: ") ;
    		if (uncovered.isEmpty())
    			buf.append("-") ;
    		else
    			buf.append("\n" + targetPathsToString(uncovered)) ;
    		buf.append("\n   DROPPED: ") ;
    		if (dropped.isEmpty())
    			buf.append("-") ;
    		else
    			buf.append("\n" + targetPathsToString(dropped)) ;
    	}
    	return buf.toString() ;
    }
    
    public void savePathCoverageInfo(String file) throws IOException {
    	IOUtils.saveTxtToFile(file, showPathCoverageInfo());
    }
    
    
    public void debugPrintCoveredPaths() {
    	System.out.println(">>>> debugPrintCoveredPaths") ;
    	for (var P : this.targetPaths.entrySet()) {
    		var missed = this.stillUncoveredTargetPaths.get(P.getKey()) ;
            var dropped = this.droppedTargetPaths.get(P.getKey()) ;
            System.out.println(">>>> method = " + P.getKey().getName() + ", COVERED:") ;
            int i = 1 ;
            for (var sigma : P.getValue()) {
            	if (missed.contains(sigma) || dropped.contains(sigma)) continue ;
                System.out.println("       " + i + ": " + sigma) ;
            	i++ ;
            }
            if (missed.size() == 0) {
            	System.out.println(">>>> method = " + P.getKey().getName() + ", MISSED: none") ;
            }
            else {
            	 System.out.println(">>>> method = " + P.getKey().getName() + ", MISSED:") ;
                 i = 1 ;
                 for (var sigma : missed) {
                     System.out.println("       " + i + ": " + sigma) ;
                 	i++ ;
                 }
            }
            if (dropped.size() == 0) {
            	System.out.println(">>>> method = " + P.getKey().getName() + ", DROPPED: none") ;
            }
            else {
            	System.out.println(">>>> method = " + P.getKey().getName() + ", DROPPED:") ;
                i = 1 ;
                for (var sigma : dropped) {
                    System.out.println("       " + i + ": " + sigma) ;
                	i++ ;
                }
            }
            
    	}
    }
}
