package nl.uu.maze.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.java.core.JavaSootMethod;

/**
 * Representing a high level control flow graph (HCFG) of a Java program; more precisely,
 * of the Jimple representation of a Java program. 
 * A node in the HCFG can be a starting node, an exit node, a branching node, 
 * or the head of an exception handler. 
 * An entry node represents the start of a program. An exit node represents a node
 * from where the program exits.
 * There can only be one entry node; but there can be multiple exit nodes.
 * 
 * <p>An exception handler head is the start of an exception handler code; it 
 * corresponds to a catch(e) S section.
 * 
 * <p> Any node in the HCFG can be only be one of the above mentioned types, 
 * except for branching and exit, that can also be an entry node. 
 * 
 * <p> An edge from node k to node n represents a normal flow of execution that
 * goes from node k to node n. We do not track exceptional flows (flow that is 
 * caused by a thrown exception). So exception-handler-head nodes will
 * structurally appear as nodes with no predecessor in the HCFG.
 * 
 * <p>Some properties of paths in this type of HCFG:
 * <ul>
 *     <li>exit node can only appear as the end of a path in a HCFG (it can't
 *         appear in any other position that the end of the path).
 *     <li>An exception handler head can only appear as the start of a path.
 *     <li>Nodes in the middle of a path (so, not the first, nor the last
 *         in the path) are branching nodes.
 * </ul>
 */
public class HighLevelCFG {
	
	/**
	 * Different types of the CFG nodes. They should be mutually exclusive, except
	 * for START, which can also be BRANCHING or EXIT.
	 * 
	 * <p>EXIT is either return or throw stmt. So, BRANCHING can't be EXIT.
	 * <p>An exception HEAD corresponded to the start of a catch-section. It always
	 * starts with var := @caughtexception, so it can't be BRANCHING nor EXIT.
	 */
	public static enum HCFGNodeType { 
		START,  // the starting node of a HCFG
		EXIT,   // an exit node 
		BRANCHING, 
		ExcHANDLER_HEAD // the start of an exception handler (catch-section)
		} ;
	
	public static class HCFGNode {
		public String id ;
		public Stmt stmt ;
		public List<HCFGNodeType> types = new LinkedList<>() ;
	
		public HCFGNode(Stmt stmt, HCFGNodeType nty) {
			this.stmt = stmt ;
			this.types.add(nty) ;
		}
		
		/**
		 * Return the hash of {@link #stmt}.
		 */
		@Override
	    public int hashCode() {
	        return stmt.hashCode() ;
	    }
		
		public boolean isBranching() {
			return types.contains(HCFGNodeType.BRANCHING) ;
		}
		
		public boolean isExceptionHandlerHead() {
			return types.contains(HCFGNodeType.ExcHANDLER_HEAD) ;
		}
		
		public boolean isExit() {
			return types.contains(HCFGNodeType.EXIT) ;
		}
		
		/**
		 * Return getBranchHash(this.stmt,-1). Used in some cases when we want to
		 * 'encode' the statement as if it is a branch, using -1 as a fake branch
		 * index.
		 */
		public int hashEncodingOfNonBranching() {
			if (isBranching())
				throw new IllegalArgumentException() ;
			return BranchStmtUtil.getBranchHash(this.stmt,-1) ;
		}
	}
	
	/**
	 * Representing an edge connecting two nodes in an HCFG. 
	 * 
	 * <p>A special case is when the destination node is null, then 
	 * the edge represents a single node, encoded as an edge.
	 */
	public static class HCFGEdge {
		public HCFGNode src ;
		public HCFGNode dest ;
		
		/**
		 * If [{@link #src} is a BRANCHING node, this stmt is the successor
		 * instruction of src.stmt that leads to the {@link #dest}.
		 * <p> If src is not a BRANCHING node, this.stmt is null.
		 */
		public Stmt stmt ;

		/**
		 * If [{@link #src} is a BRANCHING node, this is the hash that indentify
		 * the instruction-level edge from src.stmt to this.stmnt.
		 */
		public int edgeId ;
		
		public HCFGEdge(HCFGNode src, HCFGNode dest) {
			this.src = src ;
			this.dest = dest ;
		}
	}
	
	static public class HCFGPath {
		
		public String id ;
		private List<HCFGEdge> path = new LinkedList<>() ;
		private List<Integer> encoded ;
		
		HCFGPath(HighLevelCFG hcfg, List<HCFGEdge> path) {
			this.path = path ;
			encoded = toHashList(hcfg) ;
		}
		
		List<Integer> toHashList(HighLevelCFG hcfg) {
			int N = path.size() ;
			List<Integer> z = new LinkedList<>() ;
			for (var E : path) {
				if (E.src.isExceptionHandlerHead() || E.src.isExit()) {
					z.add(E.src.hashEncodingOfNonBranching()) ;
				}
				else if (E.src.isBranching()) {
					z.add(E.edgeId) ;
				}
			}
			return z ;
		}
		
		@Override
		public String toString() {
			String z = "" ;
			int k = 0 ;
			for (HCFGEdge E : path) {
				if (k>0) z += " --> ";
				z += E.src.id + " " + E.src.types  + " via " + E.stmt;
				k++ ;
			}
			var last = path.getLast() ;
			if (last.dest != null) {
				z += " --> " + last.dest.id + " " + last.dest.types;
				
			}
			z += " .... " + encoded ;
			return z ;
		}
		
		/**
		 * Check whether a given execution history covers this target path,
		 * or only partially covers the latter.
		 * 
		 * <p> return -1  : exec-hist does NOT cover this target path.
		 * <p> return 0   : exec-hist covers sigma. That is, this path is a subpath of hist.
		 * <p> return > 0 : exec-hist does not cover sigma, but may cover it if extended.
		 *                  That is, hist ends with a suffix tau which is a proper prefix
		 *                  of sigma. The returned value gives the remaining length of this 
		 *                  path that is still uncovered.
		 */
		public int coverBy(List<Integer> execHistory) {
			return cover(execHistory,encoded) ;
		}
	}
	
	
	public JavaSootMethod method ;
	public HCFGNode start ;
	/**
	 * Map every node k to its outgoing edges. Every edge connect k to
	 * another node n that is directly reachable from k.
	 */
	public Map<HCFGNode, List<HCFGEdge>> successors = new HashMap<>() ;
	public List<HCFGNode> nodes = new LinkedList<>();
		
	@SuppressWarnings("rawtypes")
	public HighLevelCFG(JavaSootMethod method) {
		this.method = method ;
		StmtGraph cfg = method.getBody().getStmtGraph() ;
		Stmt stmt0 = cfg.getStartingStmt() ;
		var stmts = method.getBody().getStmts() ;
		start = new HCFGNode(cfg.getStartingStmt(), HCFGNodeType.START) ; 
		for (Stmt stmt : stmts) {
			var sucs = cfg.successors(stmt) ;
			HCFGNode newNode = null ;
			if (sucs.size() > 1) {
				newNode = new HCFGNode(stmt, HCFGNodeType.BRANCHING) ;
			}
			else if (sucs.size() == 0) {
				newNode = new HCFGNode(stmt, HCFGNodeType.EXIT) ; 
			}
			if (stmt == stmt0) {
				// stmt is the starting stmt
				if (newNode == null) {
					newNode = new HCFGNode(stmt, HCFGNodeType.START) ; 
				}
				else {
					newNode.types.add(HCFGNodeType.START) ;
				}
				start = newNode ;
			}
			else if (cfg.getEntrypoints().contains(stmt)) {
				// stmt is not the start and is an "entry point", so it is the head
				// of an exception handler
				if (newNode == null) {
					newNode = new HCFGNode(stmt, HCFGNodeType.ExcHANDLER_HEAD) ;
				}
				else {
					newNode.types.add(HCFGNodeType.ExcHANDLER_HEAD) ;
				}
			}
			if (newNode != null) {
				int id = nodes.size() ;
				newNode.id = "" + id ;
				nodes.add(newNode) ;
			}
		}
		
		for (var nd1 : nodes) {
			List<HCFGEdge> outs = new LinkedList<>() ;
			successors.put(nd1, outs) ;
			if (nd1.types.contains(HCFGNodeType.EXIT)) 
				continue ;
			var sucStmts = cfg.successors(nd1.stmt) ;
			int k = 0 ;
			for (var stmt2 : sucStmts) {
				Stmt stmt2_ = (Stmt) stmt2 ;
				int branchHash = BranchStmtUtil.getBranchHash(nd1.stmt, k) ;
				for (var nd2 : nodes) {
					if (nd1 != nd2) {
						if (directReachable(cfg, stmt2_, nd2)) {
							HCFGEdge edge = new HCFGEdge(nd1,nd2) ;
							outs.add(edge) ;
							if (nd1.isBranching()) {
								edge.stmt = stmt2_ ;
								edge.edgeId = branchHash ;
								
							}
							break ;
						}
					}
				}
				k++ ;
			}
			
		}
		
	}
	
	/**
	 * Return true if node nd can be reaching from the statement srcStmt
	 * through normal execution, and without passing other nodes than nd.
	 * Else the function returns false.
	 */
	@SuppressWarnings("unchecked")
	private boolean directReachable(StmtGraph cfg, Stmt srcStmt, HCFGNode nd) {
		
		List<Stmt> stmtsOfNodes = nodes.stream().map(ndx -> ndx.stmt).toList() ;	
		Set<Stmt> visited = new HashSet<>() ;
		Queue<Stmt> worklist = new LinkedList<>() ;
		worklist.add(srcStmt) ;
		
		while (!worklist.isEmpty()) {
			var stmt = worklist.poll() ;
			if (visited.contains(stmt)) continue ;
			if (stmt == nd.stmt)
				// reaching nd --> so ... reachable from srcStmt
				return true ;
			visited.add(stmt) ;
			if (stmt != srcStmt && stmtsOfNodes.contains(stmt))
				// we visit another node, which is not nd --> don't continue
				continue ;
			
			// normal (non-exceptional) successors of stmt:
			var sucs = cfg.successors(stmt) ;	
			worklist.addAll(sucs.stream().map(z -> (Stmt) z).toList()) ;
		}
		return false ;
	}
	
	@Override
	public String toString() {
		String z = "HCFG of " + method.getName() ;
		for (HCFGNode nd : nodes) {
			z += "\n  nd " + nd.id + ": " + nd.stmt ;
			z += ", " + nd.types ;
			var outs = successors.get(nd) ;
			z += ", #out-edges=" + outs.size() ;
			if (outs.size() > 0) {
				for (var e : outs) {
					z += "\n    --> " 
							+ e.dest.id + ":" ;
					if (e.src.isBranching()) 
						z += " #" + e.edgeId ;
					z += " " + e.dest.stmt ;
				}
			}
		}
		return z ;
	}
	
	/**
	 * Save the HCFG as a dot-file for visualization. If fname is null, the dot
	 * graph will be printed to the console.
	 */
	public void saveAsDot(String fname) throws IOException {
		DotWriter dot = new DotWriter(method.getName()) ;
		for (var nd : nodes) {
			String label = "" + nd.stmt ;
			if (nd.types.contains(HCFGNodeType.START)) {
				dot.addStartNode(nd.id,label);
			}
			else if(nd.isExceptionHandlerHead()) {
				dot.addExeceptionHeadNode(nd.id, label);
			}
			else if (nd.isExit()) {
				dot.addExitNode(nd.id, label);
			}
			else {
				dot.addNode(nd.id, label);
			}
 		}
		for (var nd : nodes) {
			for (var E : successors.get(nd)) {
				dot.addEdge(E.src.id, E.dest.id);
			}
		}
		dot.close();
		if (fname == null) {
			System.out.println(dot.toString()) ;
		}
		else {
			dot.saveToFile(fname);
		}
	}
	
	public HCFGNode get(String id) {
		for (var nd : nodes) {
			if (nd.id.equals(id))
				return nd ;
		}
		return null ;
	}
	
	/**
	 * Given an execution history, representing a partial build up to a full
	 * test path, and a target path sigma to cover, this function checks 
	 * whether the given exec-hist covers or partially covers sigma.
	 * 
	 * <p> -1 : exec-hist does not cover sigma
	 * <p> 0  : exec-hist covers sigma. That is, sigma is a subpath of hist.
	 * <p> >0 : exec-hist does not cover sigma, but may cover it if extended.
     *          That is, hist ends with a suffix tau which is a proper prefix
	 *          of sigma. The returned value gives the remaining length of this 
	 *          path that is still uncovered.
	 */
	public static int cover(List<Integer> execHistory, List<Integer> sigma) {
		if (sigma.isEmpty())
			throw new IllegalArgumentException() ;
		if (execHistory == null || execHistory.isEmpty())
			return -1 ;
		int sigmaStart = sigma.get(0) ;
		int k = 0 ;
		for (var s0 : execHistory) {
			if (s0 == sigmaStart) {
				// match the start of sigma. s0 is the start of sigma
				int n = k ;
				for (var t : sigma) {
					if (n >= execHistory.size()) {
						// we have a partial match. That is, hist does not cover sigma,
						// but it can be extended to cover sigma.
						
						// calculate the remaining part of sigma that is still uncovered:
						int stillToCover = sigma.size() - (n - k) ;
						return stillToCover ;
						
					}
					int zz = execHistory.get(n) ;
					if (zz != t) {
						// no longer match
						break ;
					}
					n++ ;
				}
				int matchedPartOfSigma = n - k ;
				if (matchedPartOfSigma == sigma.size()) {
					// we have full match!
					return 0 ;
				}
				// else we don't have a match, yet. Continue with the next s0
			}
			k++ ;
		}
		return -1 ;
	}

	/**
	 * Check is sequence tau is a strict suffix of sigma. Both tau and sigma
	 * are assumed to be non-cyclic elementary paths.
	 * 
	 * <p>NOTE: deliberately using == to check equality.
	 */
	private static <T> boolean isStrictSuffixOf(List<T> tau, List<T> sigma) {
		
		if (tau.size() >= sigma.size()) return false;
		
		// so ... tau is shorter than sigma
		
		var x0 = tau.getFirst() ;
		int k = 0 ;
		for (var y : sigma) {
			if (y==x0) {
				// found start of tau in sigma
				break ;
			}
			k++ ;
		}
		if (sigma.size() - k < tau.size()) {
			// the remaining part of sigma is too short
			return false ;
		}
		for (var y : tau) {
			var x = sigma.get(k) ;
			if (x != y)
				return false ;
			k++ ;
		}
		return true ;
	}
	
	/**
	 * Return all maximal elementary paths of this HCFG, of length of up k.  The length
	 * of a path is defined as the number of edges passed by the path. So, for k=1
	 * this function will return, essentially, all edges in the HCFG. For k=1, it will
	 * return all the edge-pairs in the HCFG.
	 * 
	 * <p>For k=-1, the method will return all prime paths of the HCFG.
	 * 
	 * <p>An elementary path is a path where no node appears multiple times, except
	 * the first and the last nodes of the path; they can be the same (hence representing
	 * a cycle). A prime path is a maximal elementary path (it is not a subpath of
	 * another elementary path).
	 */
	public List<HCFGPath> getMaxElementaryPaths(int k) {
		
		if (k < -1)
			throw new IllegalArgumentException() ;
		
		if (k == 0)
			return new LinkedList<HCFGPath>() ;
		
		// initiialize T to contain singleton paths [e] where e
		// is an edge of this HCFG:
		List<List<HCFGEdge>> T = new LinkedList<>() ;
		for (var nd : nodes) {
			for (var E : successors.get(nd)) {
				List<HCFGEdge> sigma = new LinkedList<>() ;
				sigma.add(E) ;
				T.add(sigma) ;
			}
		}

		List<List<HCFGEdge>> cycles = new LinkedList<>() ;
		List<List<HCFGEdge>> alreadyRightMaximal = new LinkedList<>() ;

		
		if (k>0) k-- ;
		
		while (! T.isEmpty() &&  k != 0) {
			if (k>0) k-- ;
			
			// extends every sigma in T to the right, if it remains an elementary path:
			List<List<HCFGEdge>> T__ = new LinkedList<>() ;		
			for (var sigma : T) {	
				var last = sigma.getLast() ;
				if (last.dest.isExit()) {
					// sigma is right-maximal:
					alreadyRightMaximal.add(sigma) ;
					continue ;
				}
				// case that sigma can be extended on the right:
				for (var E2 : successors.get(last.dest)) {
					// make a new (shallow) copy of sigma:
					List<HCFGEdge> sigma__ = new LinkedList<>() ;
					sigma__.addAll(sigma) ;
					boolean stillElementaryPath = ! sigma__.stream().anyMatch(E1 -> E1.dest == E2.dest) ;
					if (stillElementaryPath) {
						sigma__.add(E2) ;
						if (E2.dest == sigma__.getFirst().src) {
							cycles.add(sigma__) ;
						}
						else {
							T__.add(sigma__) ;
						}
					}
				}
			}
			T = T__ ;
		}
		
		//System.out.println(">>> #T=" + T.size()) ;
		
		List<List<HCFGEdge>> Tfinal = new LinkedList<>() ;
		Tfinal.addAll(cycles) ;
		T.addAll(alreadyRightMaximal) ;
		
		// only add paths from T that are also left-maximal:
		for (var tau : T) {
			boolean maximal = ! T.stream().anyMatch(sigma -> tau != sigma && isStrictSuffixOf(tau,sigma)) ;
			if (maximal)
				Tfinal.add(tau) ;
		}
		
		// Tfinal now contain all maximal elementary paths of length <= k
		
		List<HCFGPath> targets = new LinkedList<>() ;
		for (var sigma : Tfinal) {
			targets.add(new HCFGPath(this,sigma)) ;
		}
		
		// return all the paths in tergets, but filter out the path whose encoded-path is
		// empty, e.g. the path [s,n] where s in the entry node. Such a path will always
		// be traversed, so no need to have it as a target.
				
		return targets.stream().filter(t -> ! t.encoded.isEmpty()).toList() ;
	}

}
