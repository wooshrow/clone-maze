//
// SV-COMP property:
//    verification entry point: check()
//    * should not give an assertion-error
//    * should not throw run-time exception
//
// This is a modification of an SV-COMP verification task of the same name.
// Below is a copy of the original notice:
// 

/**
 * Type : Functional Safety Expected Verdict : True Last modified by : Zafer Esen
 * <zafer.esen@it.uu.se> Date : 9 October 2019
 *
 * <p>Original license follows.
 */

/**
 * Copyright (c) 2011, Regents of the University of California All rights reserved.
 *
 * <p>Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * <p>1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * <p>2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * <p>3. Neither the name of the University of California, Berkeley nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * <p>THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Sudeep Juvekar <sjuvekar@cs.berkeley.edu>
 * @author Jacob Burnim <jburnim@cs.berkeley.edu>
 */


package nl.uu.maze.benchmarks.svcomp.algorithms;


public class BellmanFord_FunSat01 {

	static final int INFINITY = Integer.MAX_VALUE;

	/**
	 * The Bellman-Ford algorithm supposedly compute the single-source shortest paths in 
	 * a weighted digraph, efficiently handling graphs with negative edge weights.
	 * The version below only produce the distances of those shortest paths. 
     * 
     * D is an array describing weights between any pair (i,j) of nodes in a
     * directed graph. The graph is assumed to have, say, V number of nodes.
     * D is a single dimension array of size V*V.
	 */
	static int[] runBellmanFord(int N, int D[], int src) {
		// Initialize distances.
		int dist[] = new int[N];
		boolean infinite[] = new boolean[N];
		for (int i = 0; i < N; i++) { // V+1 branches
			dist[i] = INFINITY;
			infinite[i] = true;
		}
		dist[src] = 0;
		infinite[src] = false;

		// Keep relaxing edges until either:
		//  (1) No more edges need to be updated.
		//  (2) We have passed through the edges N times.
		int k;
		for (k = 0; k < N; k++) { // V+1 branches
			boolean relaxed = false;
			for (int i = 0; i < N; i++) { // V(V+1) branches
				//System.out.println("-- outer i=" +i);
				for (int j = 0; j < N; j++) { // V^2(V+1) branches
					if (i == j) continue; // V^3 branches
					if (!infinite[i]) { // V^2(V-1) branches
						//System.out.println("   ** j=" + j) ; 
						if (dist[j] > dist[i] + D[i * N + j]) { // V^2(V-1) branches
							dist[j] = dist[i] + D[i * N + j]; //  D[i * N + j] is the weight of edge from i to j
							//System.out.println("   ** i=" + i + ",j=" + j +", dist[" + j + "] = " + dist[j]) ;
							infinite[j] = false;
							relaxed = true;
						}
					}
				}
			}
			if (!relaxed) // V branches
				break;
		}

		// Check for negative-weight egdes.
		if (k == N) { // 1 branch
			// We relaxed during the N-th iteration, so there must be
			// a negative-weight cycle.
		}

		// Return the computed distances.
		return dist;
	}

	// D is an array of distances (weights) in a directed graph
	public static boolean check(int V, int[] D) {
		if(! (V > 0 && V < 46341)) {
			throw new IllegalArgumentException() ;
		}
		// enforce that V*V < Integer.MAX_VALUE
		
		if (D == null || D.length != V*V) {
			throw new IllegalArgumentException() ;
		}
		for (int i = 0; i < V; i++) {
			for (int j = 0; j < V; j++) {
				if (i == j) continue;
				int w = D[i * V + j] ;
				if ( !(w >= 0 && w < V))
					throw new IllegalArgumentException() ;
			}
		}

		int dist[] = runBellmanFord(V, D, 0);
		for (int d : dist) {
			//System.out.println(">>> " + d) ;
			// either there is no path to d from the source,
			// or it goes through at most V nodes
			if (! (d == INFINITY || d <= V) ) 
				throw new AssertionError("SVCOMP positive assertion violated!") ;
		}
		return true ;
	}

	
}
