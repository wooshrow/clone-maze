package nl.uu.maze.util;

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
 * of the Jimple representation of the Java program. 
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
public class HCFG extends DiGraph<Stmt,Stmt,nl.uu.maze.util.HCFG.HCFGNodeType,String>{
	
	
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
	
	/**
	 * Representing a target path in an HCFG that we aim to cover by a test.
	 */
	public static class HCFGPath {
		
		public String id ;
		private List<DiGraphEdge<Stmt,Stmt,HCFGNodeType,String>> path = new LinkedList<>() ;
		private List<Integer> encoded ;
		
		HCFGPath(List<DiGraphEdge<Stmt,Stmt,HCFGNodeType,String>> path) {
			this.path = path ;
			encoded = toHashList() ;
		}
		
		List<Integer> toHashList() {
			int N = path.size() ;
			List<Integer> z = new LinkedList<>() ;
			for (var E : path) {
				if (isExceptionHandlerHead(E.src) || isExitNode(E.src)) {
					z.add(hashEncodingOfNonBranching(E.src)) ;
				}
				else if (isBranchingNode(E.src)) {
					z.add(E.edgeId) ;
				}
			}
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
		
		@Override
		public String toString() {
			String z = "" ;
			int k = 0 ;
			for (var E : path) {
				if (k>0) z += " --> ";
				z += E.src.id + " " + E.src.properties  + " via " + E.label ;
				k++ ;
			}
			var last = path.getLast() ;
			if (last.dest != null) {
				z += " --> " + last.dest.id + " " + last.dest.properties;
				
			}
			z += " .... " + encoded ;
			return z ;
		}
	}
		
	
	public static boolean isStartNode(DiGraphNode<Stmt,HCFGNodeType> nd) {
		return nd.properties.contains(HCFGNodeType.START) ;
	}
	
	public static boolean isBranchingNode(DiGraphNode<Stmt,HCFGNodeType> nd) {
		return nd.properties.contains(HCFGNodeType.BRANCHING) ;
	}
		
	public static boolean isExceptionHandlerHead(DiGraphNode<Stmt,HCFGNodeType> nd) {
		return nd.properties.contains(HCFGNodeType.ExcHANDLER_HEAD) ;
	}
		
	public static boolean isExitNode(DiGraphNode<Stmt,HCFGNodeType> nd) {
		return nd.properties.contains(HCFGNodeType.EXIT) ;
	}	
	
	/**
	 * Return getBranchHash(this.stmt,-1). Used in some cases when we want to
	 * 'encode' the statement in the given non-branching node as if it is a branch, 
	 * using -1  as a fake branch index.
	 */
	public static int hashEncodingOfNonBranching(DiGraphNode<Stmt,HCFGNodeType> nd) {
		if (isBranchingNode(nd))
			throw new IllegalArgumentException() ;
		Stmt stmt = nd.label ;
		return BranchStmtUtil.getBranchHash(stmt,-1) ;
	}
	
	
	public JavaSootMethod method ;
	
	/**
	 * Mapping each Stmt in the {@link #method} to the HCFG node it belongs
	 * to. An stmt belows to a node, either if it is the statement that
	 * labels the node, or if there is a normal execution flow from the 
	 * statement, reaching the node, without passing other nodes in between.
	 */
	private Map<Stmt,DiGraphNode<Stmt,HCFGNodeType>> stmt2Node = new HashMap<>() ;
	
	/**
	 * Cached shortest distance to an exit node. Distance is given in terms
	 * of the number of HCFG nodes in-between passed to get to the target
	 * node.
	 */
	private Map<Stmt,Integer> cachedDistanceToExit = new HashMap<>() ;
	
	/**
	 * Construct a high level CFG of the given method.
	 */
	@SuppressWarnings("rawtypes")
	public HCFG(JavaSootMethod method) {
		super() ;
		this.method = method ;
		this.name = method.getName() ;
		StmtGraph cfg = method.getBody().getStmtGraph() ;
		Stmt stmt0 = cfg.getStartingStmt() ;
		var stmts = method.getBody().getStmts() ;
		for (Stmt stmt : stmts) {
			var sucs = cfg.successors(stmt) ;
			DiGraphNode<Stmt,HCFGNodeType> newNode = null ;
			if (sucs.size() > 1) {
				newNode = this.addNode(stmt) ;
				newNode.properties.add(HCFGNodeType.BRANCHING) ;	
			}
			else if (sucs.size() == 0) {
				newNode = this.addNode(stmt) ;
				newNode.properties.add(HCFGNodeType.EXIT) ;
			}
			if (stmt == stmt0) {
				// stmt is the starting stmt
				if (newNode == null) {
					newNode = this.addNode(stmt) ;
					newNode.properties.add(HCFGNodeType.START) ;
				}
				else {
					newNode.properties.add(HCFGNodeType.START) ;
				}
				start = newNode ;
			}
			else if (cfg.getEntrypoints().contains(stmt)) {
				// stmt is not the start and is an "entry point", so it is the head
				// of an exception handler
				if (newNode == null) {
					newNode = this.addNode(stmt) ;
					newNode.properties.add(HCFGNodeType.ExcHANDLER_HEAD) ;
				}
				else {
					newNode.properties.add(HCFGNodeType.ExcHANDLER_HEAD) ;
				}
			}
		}
		
		for (var nd1 : nodes) {
			if (isExitNode(nd1)) 
				continue ;
			Stmt stmt1 = nd1.label ;
			var sucStmts = cfg.successors(stmt1) ;
			int k = 0 ;
			for (var stmt2 : sucStmts) {
				Stmt stmt2_ = (Stmt) stmt2 ;
				int branchHash = BranchStmtUtil.getBranchHash(stmt1, k) ;
				for (var nd2 : nodes) {
					if (nd1 != nd2) {
						if (directReachable(cfg, stmt2_, nd2)) {
							var edge = this.addEdge(nd1.id, nd2.id,null) ;
							if (isBranchingNode(nd1)) {
								edge.label = stmt2_ ;
								edge.edgeId = branchHash ;
								
							}
							break ;
						}
					}
				}
				k++ ;
			}	
		}
		
		// fill in the stmt2node mapping:
		for (Stmt stmt : stmts) {
			for (var nd : nodes) {
				if (stmt == nd.label) {
					stmt2Node.put(stmt, nd) ;
					break ;
				}
				if (directReachable(cfg,stmt,nd)) {
					stmt2Node.put(stmt, nd) ;
					break ;
				}	
			}
		}
	}
	
	/**
	 * Return true if node nd can be reached from the statement srcStmt
	 * through normal execution, and without passing other HCFG nodes than nd.
	 * Else the function returns false.
	 */
	@SuppressWarnings("unchecked")
	private boolean directReachable(StmtGraph cfg, Stmt srcStmt, DiGraphNode<Stmt,HCFGNodeType> nd) {
		
		List<Stmt> stmtsOfNodes = nodes.stream().map(ndx -> ndx.label).toList() ;	
		Set<Stmt> visited = new HashSet<>() ;
		Queue<Stmt> worklist = new LinkedList<>() ;
		worklist.add(srcStmt) ;
		
		while (!worklist.isEmpty()) {
			var stmt = worklist.poll() ;
			if (visited.contains(stmt)) continue ;
			if (stmt == nd.label)
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
	 * Similar to {@link #getMaxElementaryPaths(int)}, but this one returns the 
	 * paths as a list of HCFGPath.
	 * 
	 * <p>Return all maximal elementary paths of this graph, of length of up k.  Paths are
	 * represented as sequences of edges. The length of a path is defined as the number 
	 * of edges passed by the path. 
	 * So, for k=1 this function will return, essentially, all edges in the graph. 
	 * For k=2, it will return all the edge-pairs in the HCFG.
	 * 
	 * <p>Special case: for k=-1, the method will return all prime paths of the HCFG.
	 * 
	 * <p>An elementary path is a path where no node appears multiple times, except
	 * the first and the last nodes of the path; they can be the same (hence representing
	 * a cycle). A prime path is a maximal elementary path (it is not a subpath of
	 * another elementary path).
	 */
	public List<HCFGPath> getMaxElementaryPaths2(int k) {
		var paths = this.getMaxElementaryPaths(k) ;
		return paths.stream().map(sigma -> new HCFGPath(sigma)).toList() ;
	}
	
	/**
	 * Return the distance from the given stmt to a target HCFG node. The distance
	 * is the number of HCFG nodes in-between to get to the target node.
	 * If the node is not reachable, -1 is returned.
	 */
	public int dist(Stmt stmt, DiGraphNode<Stmt,HCFGNodeType> nd) {
		var nd0 = stmt2Node.get(stmt) ;
		if (nd0 == null)
			throw new IllegalArgumentException() ;
		return dist(nd0, nd) ;
	}
	
	/**
	 * Distance from the stmt to an exit node. Distance is given in terms
	 * of the number of HCFG nodes in-between passed to get to the target
	 * node.
	 */
	public int distToExit(Stmt stmt) {
		Integer D = cachedDistanceToExit.get(stmt) ;
		if (D != null)
			return D ;
		for (var nd : nodes) {
			if (isExitNode(nd)) {
				int dx = dist(stmt,nd) ;
				if (dx >= 0) {
					if (D == null || dx < D) D = dx ;
				}
			}
		}
		if (D == null) return -1 ;
		return D ;
	}
	
	/**
	 * The distance from the stmt to the first node in the given target
	 * path.  Distance is given in terms of the number of HCFG nodes 
	 * in-between passed to get to the target node. If the node is not
	 * reachable, -1 is returned.
	 */
	public int distToPathHead(Stmt stmt, HCFGPath sigma) {
		var nd = sigma.path.getFirst().src ;
		return dist(stmt,nd) ;
	}
}
