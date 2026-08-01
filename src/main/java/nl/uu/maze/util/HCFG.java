package nl.uu.maze.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import sootup.core.graph.BasicBlock;
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
	 * Different types of the CFG nodes. A node is either BRANCHING or
	 * non-branching, represented by type BLOCK.
	 * 
	 * <p>A node can also be marked as START (the start of a method), or
	 * EXIT (the exit of a method), or ExcHANDLER_HEAD.
	 * 
	 * <p>an EXIT node is a single stmt, which is either a return or a throw.
	 * So, it can't be BRANCHING.
	 * 
	 * <p>An exception HEAD corresponded to the start of a catch-section. It always
	 * starts with var := @caughtexception, so it can't be BRANCHING nor EXIT.
	 * 
	 * <p>A BRANCHING node consists of a single stmt, which is either an if-stmt or
	 * a switch-stmt.
	 * 
	 * <p>A BLOCK can consist of multiple stmts. 
	 */
	public static enum HCFGNodeType { 
		START,  // the starting node of a HCFG
		EXIT,   // an exit node 
		BLOCK,  // a non-branching node 
		BRANCHING, 
		ExcHANDLER_HEAD // the start of an exception handler (catch-section)
	} ;
	
	/**
	 * Representing a target path in an HCFG that we aim to cover by a test. Such 
	 * a path is a sequence of edges in the HCFG.
	 */
	public static class HCFGPath {
		
		public String id ;
		public List<DiGraphEdge<Stmt,Stmt,HCFGNodeType,String>> path = new LinkedList<>() ;
		public List<Integer> encoded ;
		
		HCFGPath(List<DiGraphEdge<Stmt,Stmt,HCFGNodeType,String>> path) {
			this.path = path ;
			encoded = toHashList() ;
		}
		
		List<Integer> toHashList() {
			int N = path.size() ;
			List<Integer> z = new LinkedList<>() ;
			for (var E : path) {
				if (isBranchingNode(E.src)) {
					z.add(E.edgeId) ;
				}
				else {
					z.add(hashEncodingOfNonBranching(E.src)) ;
				}
			}
			// no need to know the last node, as the last edge simply determines it
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
		
		
		List<Integer> getIdsSequence() {
			List<Integer> seq = new LinkedList<>(path.stream().map(nd -> nd.src.id).toList()) ;
			seq.add(path.getLast().dest.id) ;
			return seq ;
		}
		
		public String toStringCompact() {
			String z = "" ;
			int k = 0 ;
			for (var E : path) {
				if (k > 0) {
					z += " -> " ;
				}
				var nd = E.src ;
				if (HCFG.isBranchingNode(nd)) {
					z += "[" + nd.id + "]" ;
				}
				else {
					z += nd.id ;
				}
				if (k==0 && HCFG.isExceptionHandlerHead(nd)) {
					z += "[EXC-head]" ;
				}
				k++ ;
			}
			var last = path.getLast().dest ;
			z += " -> " ;
			if (HCFG.isBranchingNode(last)) {
				z += "[" + last.id + "]" ;
			}
			else {
				z += last.id ;
			}
			if (isExitNode(last))
				z += " [EXIT]" ;
			if (last == path.getFirst().src) 
				z += " [CYCLIC]" ;
			
			return z ;
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
			z += " .... encoding: " + encoded ;
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
	 * to. An stmt belongs to a node, either if it is the statement that
	 * labels the node, or if there is a normal execution flow from the 
	 * statement, without passing a branching stmt.
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
		
		//var blocks = cfg.getBlocks() ;
		//System.out.println("### #blocks=" + blocks.size()) ;
		//for (var B : blocks) {
		//	BasicBlock B_ = (BasicBlock) B ;
		//	System.out.println("   block-start : " + B_.getHead() + ", block-end: " + B_.getTail()) ;
		//}
		
		// first lift the starting-stmt, other entry-stmts, exit stmts, and branching stmts
		// to nodes:
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
					newNode.properties.add(HCFGNodeType.BLOCK) ;
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
		
		// next, we'll lift stmts to which branching statements go to to nodes, if they have
		// not been lifted yet. This will also link the branching nodes to their successor-nodes.
		var branchingNodes = nodes.stream().filter(nd -> isBranchingNode(nd)).toList() ;
		for (var nd1 : branchingNodes) {
			Stmt stmt1 = nd1.label;
			var sucStmts = cfg.successors(stmt1);
			int branchIndex = 0;
			for (var stmt2 : sucStmts) {
				Stmt stmt2_ = (Stmt) stmt2;
				var nd2 = findNodeWithLabel(stmt2_);
				if (nd2 == null) {
					nd2 = this.addNode(stmt2_);
					nd2.properties.add(HCFGNodeType.BLOCK);
				}
				// add the edge from nd1 to nd2:
				var edge = this.addEdge(nd1.id, nd2.id, null);
				edge.label = stmt2_;
				edge.edgeId = BranchStmtUtil.getBranchHash(stmt1, branchIndex);
				branchIndex++;
			}
		}
		
		// next, we fill stmt2Node. This is the same as classifying which statements belong
		// to which nodes. At the same time we will also add out-edges from non-branching
		// nodes.
		for (var nd1 : nodes) {
			if (isBranchingNode(nd1) || isExitNode(nd1)) {
				stmt2Node.put(nd1.label, nd1) ;
				continue ;
			}
			Stmt stmt1 = nd1.label ;
			boolean reachingEndOfNode = false ;
			while (! reachingEndOfNode) {
				stmt2Node.put(stmt1,nd1) ;
				var sucStmts = cfg.successors(stmt1) ;
				if (sucStmts.size() != 1) {
					throw new Error("Should not happen!") ;
				}
				var stmt2 = (Stmt) sucStmts.getFirst() ;
				var nd2 = findNodeWithLabel(stmt2) ;
				if (nd2 != null) {
					// stmt2 is the start of node nd2, add an edge:
					var edge = this.addEdge(nd1.id, nd2.id,null) ;
					reachingEndOfNode = true ;
				}
				else {
					stmt1 = stmt2 ;
				}
			}
		}
		
		// renumber the ids so that they are tolopological:
		renumber() ;
	}
	
	
	
	
	private DiGraphNode<Stmt,HCFGNodeType> findNodeWithLabel(Stmt stmt) {
		for (var nd : nodes) {
			if (stmt == nd.label) {
				return nd ;
			}
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
			throw new IllegalArgumentException("target sigma has empty encoded-path") ;
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
		
		List<HCFGPath> paths = new LinkedList<>(this.getMaxElementaryPaths(k).stream().map(sigma -> new HCFGPath(sigma)).toList()) ;
		
		// we filter out paths whose encoding is empty, 
		// or a sub-sequence of the encoding of another path  --> well ... not needed anymore
		
		List<HCFGPath> paths2 = new LinkedList<>() ;
		
		while (! paths.isEmpty()) {
			var sigma = paths.remove(0) ;
			if (sigma.encoded.isEmpty()) continue ;
			//boolean maximal = ! paths.stream().anyMatch(tau -> tau != sigma && cover(tau.encoded,sigma.encoded)==0) ;
			//if (maximal)
			paths2.add(sigma) ;
		}
		
		// DEBUG
		/*
		List<Integer> exclude_ = new LinkedList<>() ;
		exclude_.add(0) ;
		exclude_.add(3) ;
		exclude_.add(4) ;
		
		List<Integer> exclude2_ = new LinkedList<>() ;
		exclude2_.add(0) ;
		exclude2_.add(1) ;
		exclude2_.add(2) ;
		
		List<Integer> only_ = new LinkedList<>() ;
		only_.add(7) ;
		only_.add(6) ;
		only_.add(3) ;
		only_.add(8) ;
		only_.add(1) ;
		only_.add(5) ;
		only_.add(2) ;
		only_.add(7) ;
		*/
		
		/*
		paths2 = new LinkedList<>(paths2.stream().filter(sigma -> 
			! (sigma.getIdsSequence().equals(exclude_)
				|| sigma.getIdsSequence().equals(exclude2_))).toList()
				)
				;
		*/
		
		/*
		paths2 = new LinkedList<>(paths2.stream().filter(sigma 
				-> sigma.getIdsSequence().equals(only_)).toList()) ;
		*/
		
		// sort the paths based on the id of the first node in the paths;
		// this will make it so that paths closer to the starting node will
		// appear first
		paths2.sort((p1,p2) -> {
			int c = Integer.compare(p1.path.getFirst().src.id, p2.path.getFirst().src.id) ;
			if (c!=0) return c ;
			return Integer.compare(p1.path.size(), p2.path.size()) ;
			}
		) ;
				
		return paths2 ;
	}
	
	/**
	 * Return the distance from the given stmt to a target HCFG node. 
	 * "Distance" here is the number of HCFG nodes in-between in the path
	 * from stmt to the target node. The target node itself is not counted.
	 * So, keep in mind that distance 0 does not necessarily mean that
	 * stmt is the same as nd.label, though it means it can reach nd.label
	 * without passing any branching statement in between,
	 * 
	 * <p>If the node is not reachable, -1 is returned.
	 */
	public int dist(Stmt stmt, DiGraphNode<Stmt,HCFGNodeType> nd) {
		if (stmt == nd.label) return 0 ;
		var nd0 = stmt2Node.get(stmt) ;
		if (nd0 == null)
			throw new IllegalArgumentException() ;
		return dist(nd0, nd) ;
	}
	
	public boolean isExceptionHandlerHead(Stmt stmt) {
		var nd = stmt2Node.get(stmt) ;
		return isExceptionHandlerHead(nd) && stmt == nd.label ;
	}
	
	public boolean isBranchingNode(Stmt stmt) {
		var nd = stmt2Node.get(stmt) ;
		return isBranchingNode(nd) && stmt == nd.label ;
	}
	
	public boolean isEdgeOutStmt(Stmt stmt) {
		for (var edges : this.successors.values()) {
			boolean found = edges.stream().anyMatch(E -> E.label == stmt) ;
			if (found) return true ;
		}
		return false ;
	}
	
	public boolean isHeadOfNonBranchingNode(Stmt stmt) {
		var nd = stmt2Node.get(stmt) ;
		return ! isBranchingNode(nd) && stmt == nd.label ;
	}
	
	/**
	 * Distance from the stmt to an exit node. Distance is defined as in
	 * {@link #dist(Stmt, DiGraphNode)}.
	 */
	public int distToExit(Stmt stmt) {
		Integer D = cachedDistanceToExit.get(stmt) ;
		if (D != null)
			return D ;
		D = -1 ;
		for (var nd : nodes) {
			if (isExitNode(nd)) {
				int dx = dist(stmt,nd) ;
				if (D < 0 || (dx>=0 && dx < D))
					D = dx ;
			}
		}
		cachedDistanceToExit.put(stmt, D) ;
		return D ;
	}
	
	/**
	 * The distance from the stmt to the head (first node) of the given target
	 * path.  Distance is defined as in {@link #dist(Stmt, DiGraphNode)}.
	 */
	public int distToPathHead(Stmt stmt, HCFGPath sigma) {
		var nd = sigma.path.getFirst().src ;
		return dist(stmt,nd) ;
	}
}
