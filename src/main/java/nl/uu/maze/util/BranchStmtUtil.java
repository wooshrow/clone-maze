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


public class BranchStmtUtil {
    
    /** Convert a program path into branch history, represented as a list hashes of the branches. */
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
    
    /** Converts a branch taken to an integer representation */
    public static int getBranchHash(Stmt branchStmt, int branchIndex) {
        return branchStmt.hashCode() + 31 * branchIndex;
    }
    
    /**
     * Given a pair of stmts that forms a transition, return the hash representing
     * the transition.
     */
    @SuppressWarnings("unchecked")
	public static Integer getBranchHash(StmtGraph cfg, Stmt stmt, Stmt nextStmt, boolean onlyBranchStmt) {
    	List<Stmt> successors = cfg.getAllSuccessors(stmt) ;
    	if ((!onlyBranchStmt && successors.size() > 1) || stmt instanceof JIfStmt || stmt instanceof JSwitchStmt) {
    		int branchIndex = 0 ;
    		for (Stmt S : successors) {
    			if (S == nextStmt) { // deliberately using reference comparison
    				return getBranchHash(stmt,branchIndex) ;
    			}
    			branchIndex++ ;
    		}
    	}	
    	return null ;	
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
    
    /**
     * Get the stmt-stmt transition pair from a given branch-hash.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	static Pair<Stmt,Stmt> findBranch(StmtGraph cfg, int branchHash) {
    	List<Stmt> stmts = cfg.getStmts() ;
    	for(Stmt S : stmts) {
    		if (S instanceof JIfStmt || S instanceof JSwitchStmt) {
    			List<Stmt> successors = cfg.getAllSuccessors(S) ;
    			int N = successors.size() ;
    			for (int i = 0; i < N; i++) {
    	            if (getBranchHash(S,i) == branchHash) {
    	            	Pair<Stmt,Stmt> branch = new Pair<>(S, successors.get(i)) ;
     	                return branch ;
    	            }
    	        }
    		}
    	}
    	return null ;
    }


}
