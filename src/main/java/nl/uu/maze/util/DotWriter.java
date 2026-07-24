package nl.uu.maze.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * For creating simple Graphiz graph in the Dot notation.
 */
public class DotWriter {
	
	StringBuffer buf = new StringBuffer() ;
	
	
	private String quote(String s) {
		return "\"" + s + "\"" ;
	}
	public DotWriter(String graphName) { 
		buf.append("digraph " +  graphName + "{") ;
	}

	/**
	 * Node with styling of a start-node.
	 */
	public void addStartNode(String nodeId, String nodeLabel) {
		buf.append("\n   " + nodeId + "[shape=box, label=" + quote(nodeLabel) + "]") ;
	}
	
	/**
	 * A node with a different styling than standard node.
	 */
	public void addNode2(String nodeId, String nodeLabel) {
		buf.append("\n   " + nodeId + "[shape=box, color=red, label=" + quote(nodeLabel) + "]") ;
	}
	
	public void addNode(String nodeId, String nodeLabel) {
		buf.append("\n   " + nodeId + "[label=" + quote(nodeLabel) + "]") ;
	}

	/**
	 * Node with styling of an exit-node.
	 */
	public void addExitNode(String nodeId, String nodeLabel) {
		buf.append("\n   " + nodeId + "[style=bold, label=" + quote(nodeLabel) + "]") ;
	}

	public void addEdge(String srcId, String srcDest, String label) {
		buf.append("\n   " + srcId + " -> " + srcDest) ;
		if (label != null) {
			buf.append(" [label=" + quote(label) + "]") ;
		}
	}
	
	public void close() {
		buf.append("\n}") ;
	}
	
	@Override
	public String toString() {
		return buf.toString() ;
	}
	
	
	public void saveToFile(String fname) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
	    writer.write(buf.toString()); 
	    writer.close();
	}
}
