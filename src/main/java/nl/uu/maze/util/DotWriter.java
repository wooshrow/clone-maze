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
	
	public void addStartNode(String nodeId, String nodeLabel) {
		buf.append("\n   " + nodeId + "[shape=box, label=" + quote(nodeLabel) + "]") ;
	}
	
	public void addExeceptionHeadNode(String nodeId, String nodeLabel) {
		buf.append("\n   " + nodeId + "[shape=box, color=red, label=" + quote(nodeLabel) + "]") ;
	}
	
	public void addNode(String nodeId, String nodeLabel) {
		buf.append("\n   " + nodeId + "[label=" + quote(nodeLabel) + "]") ;
	}

	public void addExitNode(String nodeId, String nodeLabel) {
		buf.append("\n   " + nodeId + "[style=bold, label=" + quote(nodeLabel) + "]") ;
	}

	public void addEdge(String srcId, String srcDest) {
		buf.append("\n   " + srcId + " -> " + srcDest) ;
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
