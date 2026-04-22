package nl.uu.maze.util;

import nl.uu.maze.execution.symbolic.BranchHistory;
import nl.uu.maze.execution.symbolic.BranchHistory.BranchItem;
import nl.uu.maze.execution.symbolic.BranchHistory.MethodEntryItem;
import nl.uu.maze.execution.symbolic.SymbolicState;
import nl.uu.maze.search.SearchTarget;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.JIfStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.jimple.javabytecode.stmt.JSwitchStmt;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * This is contributed by MartV0, https://github.com/MartV0/maze
 */
public class BranchHistoryUtil {
    private final static Logger logger = LoggerFactory.getLogger(BranchHistoryUtil.class);

    /** Convert a program path into branch history */
    public static ArrayList<Integer> convertPathToBranchHistory(List<Stmt> path, StmtGraph<?> cfg){
        var history = new ArrayList<Integer>();
        for (int i = 0; i < path.size(); i++) {
            var stmt = path.get(i);
            var successors = cfg.getAllSuccessors(stmt);
            if (successors.size() > 1 && i < path.size() - 1) {
                int branchIndex = ListUtils.IndexOf(successors, path.get(i+1));
                if (branchIndex == -1) throw new java.lang.Error("Next item from path not found in list of successors");
                history.add(getBranchHash(stmt, branchIndex));
            }
        }
        return history;
    }
    
    public static void logHistory(SearchTarget state) {
        try {
            logger.info(getPathFromBranchHistory_Old(state.getBranchHistory(), state.getCFG(), state.getStmt()).toString());
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    /** Converts branch history for a given CFG to a list */
    static ArrayList<Stmt> getPathFromBranchHistory_Old(List<Integer> branch_history, StmtGraph<?> cfg, Stmt target) throws Exception {
    	var path = new ArrayList<Stmt>();
        var entry_points = cfg.getEntrypoints();
        if (entry_points.size() > 1) throw new Exception("More than one entry point");
        Stmt current_statement = entry_points.iterator().next();
        System.out.println("### #bhist " + branch_history.size()) ;
        System.out.println("### target: " + target) ;
        path.add(current_statement);
        int i = 0;     
        while (current_statement != null && (current_statement != target || i < branch_history.size())) {
            var successors = cfg.getAllSuccessors(current_statement);
            System.out.println("### i=" + i + ", cur-stmt " + current_statement + ", #sucs:" + successors.size()) ;
            switch (successors.size()) {
                case 0:
                	System.out.println("###0 path: " + path) ;
                	return path;
                case 1:
                    current_statement = successors.get(0);
                    break;
                default:
                    if (i >= branch_history.size()) { 
                    	System.out.println("###1 path: " + path) ;
                    	return path;
                    }
                    current_statement = findSuccesor(current_statement, branch_history.get(i++), successors);
                    break;
            }
            path.add(current_statement);
        }
        System.out.println("### path: " + path) ;
        return path;
    }
    
    /**
     * Reconstruct the sequence of passed statements/instructions from a given branch history.
     * This information is used to keep track the coverage of generated tests. MAZE tracks
     * the branches passed by a test (branch history), from which we can reconstruct the 
     * passed instructions. Rather than reconstructing, we could also simply track the 
     * sequence of instructions passed along a test, but this would cost some space. For now,
     * we don't do this and choose for the reconstruction approach. 
     * 
     * <p<There is however a subtlety in this reconstruction. MAZE's branch-history
     * does not track exceptional-branches (jumps as a result of throwing an exception).
     * It only tracks branches of an if- and switch-instructions. In other words, the
     * branch history may contain incomplete information to do correct a reconstruction.
     * The method below will do best-effort reconstruction.
     */
    public static ArrayList<Stmt> getPathFromBranchHistory(BranchHistory branch_history, Stmt target) {

    	var path = new ArrayList<Stmt>();
    	var tmpSuffix  = new ArrayList<Stmt>();
    	var hist = branch_history.getContexedBranchHistory() ;
    	StmtGraph<?> cfg = null ;
        Stmt current_statement = null ;
        //System.out.println("### #bhist " + branch_history.size()) ;
        //System.out.println("### target: " + target) ;
        
        while (true) {
        	if (cfg == null) {
        		path.addAll(tmpSuffix) ;
        		tmpSuffix.clear(); 
        		if (hist.isEmpty()) {
            		return path ;
        		}
        		var u = hist.removeFirst() ;
        		switch(u) {
        		   case MethodEntryItem mei -> cfg = mei.method.getBody().getStmtGraph() ;
          		   case BranchItem b -> {
          			   System.out.println(">>> mis-align-0") ;
        			   // Mis-alignment, cfg is null, so we don't know which to which cfg the branch b 
          			   // belongs to.
        			   // We then just return the path so far:
        			   return path ; }
        		   default -> { }
        		} ;
        		// load the next CFG from the branch-history:
        		current_statement = cfg.getStartingStmt() ; 
                tmpSuffix.add(current_statement);
                continue ;
        	}
        	
        	if (target == current_statement) {
        		path.addAll(tmpSuffix) ;
        		return path ;
        	}
        	
        	var successors = cfg.getAllSuccessors(current_statement);
        	
        	if (current_statement instanceof JIfStmt || current_statement instanceof JSwitchStmt) {
        		// these are the two branching instuctions tracked by MAZE branch history
        		if (hist.isEmpty()) { 
                	//System.out.println("###2 unrecorded branch! path: " + path + ", stmt " + current_statement) ;
        			// history has ended, we can return the path, we'll do this by
        			// clearing the cfg (the next iteration will then return the path):
        			cfg = null ;
        			continue ;
                }
        		// so branch-history is not empty, get the next item:
                var u = hist.removeFirst() ;
                if (u instanceof MethodEntryItem) {
                	// Mis-alignment!! The suffix-tmp ends in a branch, while recorded
                	// branch-history is at a transition to a new CFG.
                	// We drop the suffix then:
       			    System.out.println(">>> mis-align-1") ;
                	tmpSuffix.clear();
                	cfg = null ;
                	continue ;
                }
                // ok, so u is a branch:
                var b = (BranchItem) u ;
                
        		Stmt next_statement = findSuccesor(current_statement, b.branchHash, successors);
        		if (next_statement == null) {
        			// Mis-alignment!! The suffix-tmp ends in a branch, by the branch-history
        			// record a different branch
                	// We drop the suffix then:
       			   System.out.println(">>> mis-align-2") ;
        			tmpSuffix.clear();
        			// We'll then just leap to the recorded branch; we first need to find that branch
        			// in the CFG:
        			var branch = findBranch(cfg,b.branchHash) ;
        			if (branch == null) {
           			   System.out.println(">>> mis-align-2B") ;
        				// can't find the branch in the current CFG, we then just
        				// return the path found so far:
        				return path ;
        			}
        			// we continue the path by leaping to the branch (so there is a gap now
        			// in the path:
        			path.add(branch.first()) ;
        			current_statement = branch.second() ;
                    tmpSuffix.add(current_statement);
        			
        		}
        		else {
        			// the branch match, we can add the suffix-tmp to path:
        			path.addAll(tmpSuffix) ;
            		tmpSuffix.clear(); 
            		// and add the branch itself:
            		current_statement = next_statement ;
                    tmpSuffix.add(current_statement);
        		}
        	}
        	else {
        		// the current stmt is not a normal branch nor switch:
        		if (successors.size() == 0) {
        			// the current-stmt is the last instruction of the current cfg
        			//System.out.println("###0 path: " + path + ", stmt " + current_statement) ;
        			cfg = null ;
                	continue ;
        		}
        		else {
        			// assume the next stmt would be just the first one of the cfg-successor
            		// of the current stmt
        			// Note: if the current stmt is in a try-block, it may appear as having
        			// multiple successors, even if the stmt is not a branching smtm. The other
        			// successors are jump to the catch-handler/s.
        			current_statement = successors.get(0);
            		tmpSuffix.add(current_statement) ;
        		}
        	}
        }    	
    }

    public static List<Pair<Stmt,Stmt>> getStmtStmtListOfBranches(BranchHistory branchhistory) {
    	var h = branchhistory.getContexedBranchHistory() ;
    	List<Pair<Stmt,Stmt>> z = new LinkedList<>() ;
    	StmtGraph cfg = null ;
    	for (var bi : h) {
    		switch(bi) {
    		  case MethodEntryItem mei -> cfg = mei.method.getBody().getStmtGraph() ;
    		  case BranchItem b -> {
    			 var br = findBranch(cfg,b.branchHash) ;
    			 z.add(br) ;
    		  }
    		  default -> { }
    		}
    	}
    	return z ;
    }
    
    /** Converts a branch taken to an integer representation */
    public static int getBranchHash(Stmt branchStmt, int branchIndex) {
        return branchStmt.hashCode() + 31 * branchIndex;
    }
    
    /**
     * Given a statement/instruction, and a branch-id (a hash) of a branch that goes out
     * from that statement, this function returns the next statement at the end of the
     * branch.
     * <p>It returns null if the target statement cannot be found, e.g. if the branch-id
     * does not actually belong to a branch of the given stmt.
     */
    static Stmt findSuccesor(Stmt statement, int brachHash, List<Stmt> successors) {
        for (int i = 0; i < successors.size(); i++) {
            if (getBranchHash(statement, i) == brachHash) {
                return successors.get(i);
            }
        }
        System.out.println("### " + statement + ", target-br-hash=" + brachHash + ", successors: " + successors) ;
        for (int i = 0; i < successors.size(); i++) {
        	System.out.println("    br-hash:" + getBranchHash(statement, i)) ;
        }
        // no matching branch was found:
        return null ;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
	static Pair<Stmt,Stmt> findBranch(StmtGraph cfg, int brachHash) {
    	List<Stmt> stmts = cfg.getStmts() ;
    	for(Stmt S : stmts) {
    		if (S instanceof JIfStmt || S instanceof JSwitchStmt) {
    			List<Stmt> successors = cfg.getAllSuccessors(S) ;
    			int N = successors.size() ;
    			for (int i = 0; i < N; i++) {
    	            if (getBranchHash(S,i) == brachHash) {
    	            	Pair<Stmt,Stmt> branch = new Pair<>(S, successors.get(i)) ;
     	                return branch ;
    	            }
    	        }
    		}
    	}
    	return null ;
    }


}
