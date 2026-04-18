package nl.uu.maze.util;

import nl.uu.maze.execution.symbolic.BranchHistory;
import nl.uu.maze.execution.symbolic.BranchHistory.BranchItem;
import nl.uu.maze.execution.symbolic.BranchHistory.MethodEntryItem;
import nl.uu.maze.search.SearchTarget;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.Stmt;
import java.util.ArrayList;
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
    public static ArrayList<Stmt> getPathFromBranchHistory_Old(List<Integer> branch_history, StmtGraph<?> cfg, Stmt target) throws Exception {
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
     * Reconstruct the sequence of passed statements from a given branch history.
     */
    public static ArrayList<Stmt> getPathFromBranchHistory(BranchHistory branch_history, Stmt target) throws Exception {
    	var path = new ArrayList<Stmt>();
    	var hist = branch_history.getContexedBranchHistory() ;
    	StmtGraph<?> cfg = null ;
        Stmt current_statement = null ;
        //System.out.println("### #bhist " + branch_history.size()) ;
        //System.out.println("### target: " + target) ;
        
        while (true) {
        	if (cfg == null) {
        		if (hist.isEmpty()) return path ;
        		cfg = ((MethodEntryItem) hist.removeFirst()).method.getBody().getStmtGraph() ;
            	var entry_points = cfg.getEntrypoints();
                if (entry_points.size() > 1) 
                	throw new Exception("More than one entry point");
                current_statement = entry_points.iterator().next();
                path.add(current_statement);
                continue ;
        	}
        	
            var successors = cfg.getAllSuccessors(current_statement);
            //System.out.println("### cur-stmt " + current_statement + ", #sucs:" + successors.size()) ;
            switch (successors.size()) {
                case 0:
                	//System.out.println("###0 path: " + path + ", stmt " + current_statement) ;
                	cfg = null ;
                	continue ;
                case 1:
                	//System.out.println("###1 path: " + path + ", stmt " + current_statement) ;
                    current_statement = successors.get(0);
                    break;
                default:
                	// sucs > 1, so a branching stmt ;
                    if (hist.isEmpty()) { 
                    	//System.out.println("###2 unrecorded branch! path: " + path + ", stmt " + current_statement) ;
                    	return path;
                    }
                    var u = hist.removeFirst() ;
                    switch (u) {
                       case  MethodEntryItem mei :
                    	  //System.out.println("###2b unrecorded branch! path: " + path) ;
                    	  //System.out.println("      " + branch_history) ;
                    	  return path;
                       case BranchItem b:
                    	  current_statement = findSuccesor(current_statement, b.branchHash, successors);
                    default :
                    }
            }
            path.add(current_statement);
        }    	
    }

    static Stmt findSuccesor(Stmt statement, int brachHash, List<Stmt> successors) throws Exception {
        for (int i = 0; i < successors.size(); i++) {
            if (getBranchHash(statement, i) == brachHash) {
                return successors.get(i);
            }
        }
        throw new Exception("Method findSuccesor: no matching successor statement");
    }

    /** Converts a branch taken to an integer representation */
    public static int getBranchHash(Stmt branchStmt, int branchIndex) {
        return branchStmt.hashCode() + 31 * branchIndex;
    }
}
