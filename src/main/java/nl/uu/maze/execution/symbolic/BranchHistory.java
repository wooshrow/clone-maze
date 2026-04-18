package nl.uu.maze.execution.symbolic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

/**
 * A list of branches passed by an execution. A branch will be represented by
 * a integer hash-value of the actual branching statement (and its outgoing
 * arrow/index).
 * The data structure also track when a method is entered, so we can infer back
 * to which method given tracked branches belong to.
 */
public class BranchHistory {
	
	static public class BranchHistoryItem { }
	
	static public class BranchItem extends BranchHistoryItem {
		public int branchHash ;
		BranchItem(int branchHash) { this.branchHash = branchHash ; }
		@Override
		public String toString() {
			return "" + branchHash ;
		}
	}

	static public class MethodEntryItem extends BranchHistoryItem {
		public JavaSootMethod method ;
		int numberOfBrachesBeforeThisEntry = 0 ;
		MethodEntryItem(JavaSootMethod method) { this.method = method ; }
		@Override
		public String toString() {
			return "Entering " + method.getName() + "/" + numberOfBrachesBeforeThisEntry ;
		}
	}
	
	private List<Integer> branchhistory = new ArrayList<>() ;
	private List<MethodEntryItem> methodEntryHistory = new LinkedList<>() ;
	
	public BranchHistory() {}
	
	public BranchHistory(BranchHistory h) {
		this.branchhistory = new ArrayList<>(h.branchhistory) ;
		this.methodEntryHistory = new LinkedList<>(h.methodEntryHistory) ;
	}
	
	public void clear() { 
		branchhistory.clear();
		methodEntryHistory.clear();
	}
	
	public void addBranch(int branchHash) {
		branchhistory.add(branchHash) ;
	}
	
	public void addMethodEntry(JavaSootMethod method) {
		MethodEntryItem entry = new MethodEntryItem(method) ;
		entry.numberOfBrachesBeforeThisEntry = branchhistory.size() ;
		methodEntryHistory.add(entry) ;
	}
	
	/**
	 * Return the recorded branch history, as a list of branches.
	 */
	public List<Integer> getBranchHistory() {
		return branchhistory ;
	}
	
	/**
	 * Return the recorded branch history, along with information on when methods
	 * are entered.
	 */
	public List<BranchHistoryItem> getContexedBranchHistory() {
		List<BranchHistoryItem> h = new LinkedList<>() ;
		int k = 0 ;
		for (var mentry : methodEntryHistory) {
			while (k < mentry.numberOfBrachesBeforeThisEntry && k < branchhistory.size()) {
				var branch = branchhistory.get(k) ;
				h.add(new BranchItem(branch)) ;
				k++ ;
			}
			h.add(mentry) ;
		}
		while ( k < branchhistory.size()) {
			var branch = branchhistory.get(k) ;
			h.add(new BranchItem(branch)) ;
			k++ ;
		}
		return h ;
	}
	
	@Override
	public String toString() {
		return "" +  getContexedBranchHistory() ;
	}

}
